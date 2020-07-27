package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_PERMISSION_FOR_BLUETOOTH = 100;
    private final int REQUEST_PERMISSION_FOR_CAMERA = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnS = findViewById(R.id.btn_classic_server);
        Button btnC = findViewById(R.id.btn_ble);
        Button btnBTC = findViewById(R.id.btn_classic_client);

        btnBTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                startActivity(intent);
            }
        });

        btnS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ServerActivity.class);
                startActivity(intent);
            }
        });

        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BleActivity.class);
                startActivity(intent);
            }
        });

        //checkPermission();


    }



    private void checkPermission() {
//        int permission_location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
//        int permission_blueTooth = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
//        int permission_blueToothAdmin = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);
//
//        if (permission_location == PackageManager.PERMISSION_GRANTED && permission_blueTooth == PackageManager.FEATURE_BLUETOOTH && permission_blueToothAdmin == PackageManager.FEATURE_BLUETOOTH_LE)

        int permission_camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission_camera == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent();
            intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_PERMISSION_FOR_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 楊查收到的權限是否和自訂的回傳常數相同，若相同代表使用者允許
        if (requestCode == REQUEST_PERMISSION_FOR_CAMERA) {
            // 若取得使用著
        }
    }
}
