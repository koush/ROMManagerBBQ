package com.koushikdutta.rommanager.bbq;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Helper {
    public final static String SDCARD_PATH = "/sdcard";//Environment.getExternalStorageDirectory().getAbsolutePath();
    public final static String BASE_PATH = SDCARD_PATH +"/clockworkmod";    private static final String LOGTAG = "RomManagerBbq";
    public final static String DOWNLOAD_PATH = BASE_PATH + "/download";

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (path.delete());
    }

    
    static public String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
        }
        catch (Exception e) {
            return null;
        }
    }
    
    static public String getSafeDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String ret = digest(tm.getDeviceId());
        return ret;
    }
    
    public static File computeFilePath(URL downloadUrl) {
        final String query = downloadUrl.getQuery();
        String path = Helper.DOWNLOAD_PATH + "/" + downloadUrl.getHost() + downloadUrl.getPath();
        if (query != null && !query.equals(""))
            path += "/" + query;
        path = path.replace('?', '/').replace('=', '/').replace('&', '/');
        final File filePath = new File(path);
        return filePath;
    }
    
    

    static public void beginDownload(final String url, final Activity activity, final boolean allowCached, final DownloadContext downloadContext)
    {
        beginDownload(new String[] { url }, activity, allowCached, null, downloadContext);
    }
    
    static public void beginDownload(final String url, final Activity activity, final boolean allowCached, final String destinationFile, final DownloadContext downloadContext)
    {
        beginDownload(new String[] { url }, activity, allowCached, destinationFile, downloadContext);
    }
    
    static public void beginDownload(final String[] urls, final Activity activity, final boolean allowCached, final String destinationFile, final DownloadContext downloadContext)
    {
        final Handler mHandler = new Handler();
        Thread thread = new Thread() {
            @Override
            public void run() {
                int step = 100 / urls.length; 
                for (int i = 0; i < urls.length; i++) {
                    String url = urls[i];
                    final int start = step * i;
                    int end = start + step;
                    try {
                        URL downloadUrl = new URL(url);
                        final File filePath = destinationFile == null ? computeFilePath(downloadUrl) : new File(destinationFile);
                        
                        if (!filePath.exists() || !allowCached) {
                            Helper.deleteDirectory(filePath);
                            mHandler.post(new Runnable() {
                                public void run() {
                                    if (activity == null)
                                        return;
                                    activity.setProgressBarVisibility(true);
                                    activity.setProgress(start);
                                }
                            });
                            URLConnection conn = downloadUrl.openConnection();
                            final int totalSize = conn.getContentLength();
                            mHandler.post(new Runnable() {
                                public void run() {
                                    if (activity == null)
                                        return;
                                    activity.setProgressBarIndeterminate(totalSize == -1);
                                }
                            });
                            Log.i(LOGTAG, "Downloading: " + downloadUrl);
                            DataInputStream input = new DataInputStream(conn.getInputStream());
                            final File tmpFilePath = new File(filePath.toString() + ".tmp");
                            filePath.getParentFile().mkdirs();
                            FileOutputStream fout = new FileOutputStream(tmpFilePath);
                            byte[] buffer = new byte[200000];
                            int read = 0;
                            int total = 0;
                            int lastUpdate = 0;
                            while ((read = input.read(buffer)) != -1) {
                                total += read;
                                final double fraction = (double) total / (double) totalSize;
                                final int progress = (int)((fraction * (end - start) + start) * 100);
                                if (totalSize != -1 && total > lastUpdate + 50000) {
                                    if (null != downloadContext && downloadContext.getDestroyed())
                                        return;
                                    lastUpdate = total;
                                    mHandler.post(new Runnable() {
                                        public void run() {
                                            if (activity == null)
                                                return;
                                            activity.setProgress(progress);
                                        }
                                    });
                                }
                                fout.write(buffer, 0, read);
                            }
                            fout.close();
                            input.close();
                            tmpFilePath.renameTo(filePath);
                            Log.i(LOGTAG, "Download complete.");
                        }
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (null != downloadContext && downloadContext.getDestroyed())
                                    return;
                                downloadContext.onDownloadComplete(filePath.toString());
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (null != downloadContext && downloadContext.getDestroyed())
                                    return;
                                downloadContext.onDownloadFailed();
                            }
                        });
                    } finally {
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (activity == null)
                                    return;
                                activity.setProgressBarVisibility(false);
                            }
                        });
                    }
                }
            }
        };
        thread.start();
    }
}
