package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends AppCompatActivity {
    private final int REQUEST_BT_ENABLE = 1;
    private final int REQUEST_PERMISSION = 2;
    private Button btnScan, btnStopScan, btnPairing, btnStopPairing, btnWrite;
    private TextView scanState, pairingState, readData, writeData, tv;
    private EditText edWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        findViews();
        initBt();

    }

    private void initBt() {
        checkLocationPermissions();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                // 若未開啟藍芽則自動開啟，但此法不建議
                // bluetoothAdapter.enable();

                // 引導用戶開啟藍芽功能
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_BT_ENABLE);
            }
        } else {
            Toast.makeText(this, "該裝置不支援藍芽", Toast.LENGTH_LONG).show();
        }
    }



    private void checkLocationPermissions() {
        int permission_coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permission_fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int permission_bt = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        int permission_bt_admin = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);
        if (permission_coarse == PackageManager.PERMISSION_GRANTED && permission_fine == PackageManager.PERMISSION_GRANTED && permission_bt == PackageManager.PERMISSION_GRANTED && permission_bt_admin == PackageManager.PERMISSION_GRANTED) {

        } else {
            // 若未取得權限，系統跳出 dialog 請用戶開啟，透過 onRequestPermissionsResult 確認用戶是否給予權限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 如果這是有關 ble 的危險權限（因上方有給回傳參數REQUEST_PERMISSION）
        if (requestCode == REQUEST_PERMISSION) {
            // 確認是否取得權限
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "未開啟權限無法使用", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_BT_ENABLE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "尚未開啟藍芽，無法使用藍芽功能", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void findViews() {
        btnScan = findViewById(R.id.btn_scan);
        btnStopScan = findViewById(R.id.btn_stop_scan);
        btnPairing = findViewById(R.id.btn_pairing);
        btnStopPairing = findViewById(R.id.btn_stopPairing);
        btnWrite = findViewById(R.id.btn_write);
        scanState = findViewById(R.id.tv_scan_state);
        pairingState = findViewById(R.id.tv_pairing_state);
        readData = findViewById(R.id.tv_read_data);
        writeData = findViewById(R.id.tv_write_data);
        tv = findViewById(R.id.tv);
        edWrite = findViewById(R.id.et_write);
    }
}
