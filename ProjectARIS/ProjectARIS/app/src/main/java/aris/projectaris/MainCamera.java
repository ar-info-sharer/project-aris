package aris.projectaris;

import android.app.Activity;
import android.content.Intent;
//import android.graphics.Camera;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.ResultPoint;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.SourceData;
import com.journeyapps.barcodescanner.camera.PreviewCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import android.os.Vibrator;
import android.content.Context;

import android.hardware.Camera;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.PictureCallback;
import android.widget.Toast;

public class MainCamera extends Activity {

    private static final String LOGTAG = MainCamera.class.getSimpleName();
    private CompoundBarcodeView barcodeView;

    private String qrContent;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() != null) {
                //pause scan
                barcodeView.pause();

                barcodeView.setStatusText(result.getText());
                qrContent = result.getText();
            }

            //Added preview of scanned barcode
            ImageView imageView = (ImageView)findViewById(R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));

            //Vibrate on scan
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500); //Vibrate for 500 milliseconds

            //vibrate with the star wars theme
            //v.vibrate(new long[]{0, 500, 110, 500, 110, 450, 110, 200, 110, 170, 40, 450, 110, 200, 110, 170, 40, 500}, -1);
            //augment(findViewById(android.R.id.content));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            //nothing here
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        qrContent = "";

        setContentView(R.layout.activity_main_camera);

        barcodeView = (CompoundBarcodeView) findViewById(R.id.barcode_scanner);
        barcodeView.decodeContinuous(callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void augment(View view) {
        Intent arIntent = new Intent(MainCamera.this, ARCameraActivity.class);
        arIntent.putExtra("qrcontent", qrContent); //Optional parameters
        MainCamera.this.startActivity(arIntent);
    }

    public void save(View view) {
        barcodeView.getBarcodeView().getCameraInstance().requestPreview(new PreviewCallback() {
            @Override
            public void onPreview(SourceData sourceData) {
                try {
                    sourceData.setCropRect(new Rect(0,0,sourceData.getDataHeight(), sourceData.getDataWidth()));
                    Bitmap bmp = sourceData.getBitmap();

                    File dir = new File(Environment.getExternalStorageDirectory().getPath().concat("/ProjectARIS"));
                    dir.mkdirs();

                    String filepath = dir.getPath().concat(String.format("/%d.jpg", System.currentTimeMillis()));
                    OutputStream stream = new FileOutputStream(filepath);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    //Make a Toast with cream cheese
                    Toast bagel = Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_SHORT);
                    bagel.show();

                } catch (IllegalArgumentException e) {
                    Log.e(LOGTAG, "Error saving Image");
                    Toast bagel = Toast.makeText(getApplicationContext(), "Error. Could Not Save.", Toast.LENGTH_SHORT);
                    bagel.show();
                } catch (FileNotFoundException e) {
                    Log.e(LOGTAG, e.getMessage());
                } catch (RuntimeException e) {
                    Log.e(LOGTAG, "upsi. Internal message was: ".concat(e.getMessage()));
                }
            }
        });
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}