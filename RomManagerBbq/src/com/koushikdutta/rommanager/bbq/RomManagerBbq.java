package com.koushikdutta.rommanager.bbq;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

public class RomManagerBbq extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Helper.beginDownload("http://qrcode.kaywa.com/img.php?s=8&d=" + Helper.getSafeDeviceId(this), this, true, getFilesDir().getAbsolutePath() + "/qrcode.png", new DownloadContext() {
            @Override
            public void onDownloadFailed() {
                Toast.makeText(RomManagerBbq.this, "Fail!", Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onDownloadComplete(String filename) {
                Bitmap bmp = BitmapFactory.decodeFile(getFilesDir().getAbsolutePath() + "/qrcode.png");
                ImageView i = new ImageView(RomManagerBbq.this);
                i.setImageBitmap(bmp);
                setContentView(i);
            }
            
            @Override
            public boolean getDestroyed() {
                return mDestroyed;
            }
        });
        setContentView(R.layout.main);
    }
    
    boolean mDestroyed = false;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
    }
}