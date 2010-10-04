package com.koushikdutta.rommanager.nomnom;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class RomManagerNomNom extends Activity {
    private final String LOGTAG = "RomManagerNomNom";
    private static final int REQUEST_QR_CODE = 1001;
    private final static String BASE_URL = "https://licenseservice.appspot.com";
    private static final String AUTH_URL = BASE_URL + "/_ah/login";
    Handler mHandler = new Handler();
    static String cookie = null;
    
    private void getCookie(String authToken) throws URISyntaxException, ClientProtocolException, IOException {
        Log.i(LOGTAG, "getting cookie");
        // Get ACSID cookie
        DefaultHttpClient client = new DefaultHttpClient();
        String continueURL = BASE_URL;
        URI uri = new URI(AUTH_URL + "?continue=" +
                URLEncoder.encode(continueURL, "UTF-8") +
                "&auth=" + authToken);
        HttpGet method = new HttpGet(uri);
        final HttpParams getParams = new BasicHttpParams();
        HttpClientParams.setRedirecting(getParams, false);  // continue is not used
        method.setParams(getParams);

        HttpResponse res = client.execute(method);
        Header[] headers = res.getHeaders("Set-Cookie");
        if (res.getStatusLine().getStatusCode() != 302 ||
                headers.length == 0) {
            Log.i(LOGTAG, "failure getting cookie: " + res.getStatusLine().getStatusCode() + " " + res.getStatusLine().getReasonPhrase());
            return;
        }

        for (Header header: headers) {
            if (header.getValue().indexOf("ACSID=") >=0) {
                // let's parse it
                String value = header.getValue();
                String[] pairs = value.split(";");
                cookie = pairs[0];
            }
        }
        
        Log.i(LOGTAG, "Cookie: " + cookie);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (cookie == null) {
            final String[] accounts = SafeAccountHelper.getGoogleAccounts(this);
            AlertDialog.Builder builder = new Builder(RomManagerNomNom.this);
            builder.setItems(accounts, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, final int which) {
                    Log.i(LOGTAG, accounts[which]);
                    Bundle bundle = SafeAccountHelper.tryAuth(RomManagerNomNom.this, accounts[which]);
                    try {
                        if (bundle == null)
                            throw new Exception();
                        final String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        if (authToken == null) {
                            Log.i(LOGTAG, "no auth token yet");
                            Intent authIntent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                            if (authIntent == null)
                                throw new Exception();
                            startActivity(authIntent);
                            mHandler.postDelayed(new Runnable() {
                                public void run() {
                                    Bundle bundle = SafeAccountHelper.tryAuth(RomManagerNomNom.this, accounts[which]);
                                    final String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                                    Log.i(LOGTAG, "AuthToken: " + authToken);
                                    new Thread() {
                                        public void run() {
                                            try {
                                                getCookie(authToken);
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        };
                                    }.start();
                                }
                            }, 5000);
                        }
                        else {
                            new Thread() {
                                public void run() {
                                    try {
                                        getCookie(authToken);
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                };
                            }.start();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.create().show();
        }
        
        Button button = new Button(this);
        button.setText("NomNom!");
        button.setTextSize(40);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    startActivityForResult(intent, REQUEST_QR_CODE);
                }
                catch (Exception ex)
                {
                    Toast.makeText(RomManagerNomNom.this, "NomNom failed. Do you have QR Code by Zxing installed?", Toast.LENGTH_LONG).show();
                }
            }
        });
        setContentView(button);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_QR_CODE && resultCode == RESULT_OK) {
            try {
                String safeId = data.getStringExtra("SCAN_RESULT");
                // Make POST request
                DefaultHttpClient client = new DefaultHttpClient();
                URI uri = new URI("https://licenseservice.appspot.com/purchase");
                HttpPost post = new HttpPost(uri);
                ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("seller", "koushik_dutta@yahoo.com"));
                params.add(new BasicNameValuePair("deviceId", safeId));
                params.add(new BasicNameValuePair("productId", "ROM Manager"));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
                post.setEntity(entity);
                post.setHeader("Cookie", cookie);
                post.setHeader("X-Same-Domain", "1");  // XSRF
                HttpResponse res = client.execute(post);
                Log.i(LOGTAG, "Status code from register: " + res.getStatusLine().getStatusCode());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    boolean mDestroyed = false;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
    }
}