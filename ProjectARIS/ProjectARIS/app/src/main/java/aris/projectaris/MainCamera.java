package aris.projectaris;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.util.List;

import android.os.Vibrator;
import android.content.Context;

//THIS IS FROM SAMPLE
//IT PERFORMS CONTINOUS SCANNING
//AND DISPLAYS THE BARCODE AND THE SOURCE IMAGE,
//WHEN A BARCODE IS SCANNER

public class MainCamera extends Activity {

    private static final String TAG = MainCamera.class.getSimpleName();
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

            //vibrate with the starwars theme
            //v.vibrate(new long[]{0, 500, 110, 500, 110, 450, 110, 200, 110, 170, 40, 450, 110, 200, 110, 170, 40, 500}, -1);
            augment(findViewById(android.R.id.content));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            //nothing
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
        arIntent.putExtra("key", qrContent); //Optional parameters
        MainCamera.this.startActivity(arIntent);
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}