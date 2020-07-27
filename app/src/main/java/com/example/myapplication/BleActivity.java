package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.util.List;
import java.util.UUID;


public class BleActivity extends AppCompatActivity {
    private final int REQUEST_BT_ENABLE = 1;
    private final int REQUEST_PERMISSION = 2;
    private int permission_coarse, permission_fine, permission_bt, permission_bt_admin;
    private Button btnScan, btnStopScan, btnConnect, btnDisconnect, btnRead, btnWrite, btnNotify, btnDisNotify, btnIndicate, btnDisIndicate;
    private EditText edtWrite;
    private TextView scanState, connectState, readData, writeData, notifyData, mac, indecateData;
    private BleDevice device;
    private UUID uuid_service, uuid_chara;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);


        findViews();
        initBle();

        // 掃描
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBle();
            }
        });

        // 停止掃描
        btnStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScan();
                scanState.setText("停止掃描");
            }
        });

        // 連接
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectBle();
            }
        });

        // 斷開連接
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectBle();
            }
        });

        // 讀
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleRead();
            }
        });

        // 寫
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleWrite();
            }
        });

        // Notify
        btnNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleNotify();
            }
        });
    }

    // 初始化
    private void initBle() {
        Log.i("TAG", "initBle");
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)     //重連次數以及間隔時間
            //    .setSplitWriteNum(20)   //拆分寫入數
            //    .setConnectOverTime(10000)    //連接超時時間
                .setOperateTimeout(5000);    //操作超時時間
    }

    private void checkBle() {
        Log.i("TAG", "click btn");
        // check 是否支援藍芽
        if (!BleManager.getInstance().isSupportBle()) {
            Toast.makeText(this, "您的手機不支援藍芽", Toast.LENGTH_LONG).show();
        }

        // check 藍芽是否打開, 若無就引導用戶打開
        if (!BleManager.getInstance().isBlueEnable()) {
            // 直接打開藍芽，但此方法是異步，打開藍芽需要時間，故調用此方法後藍芽不會立刻處於開啟狀態
            // 若使用此方法後緊接著就需要進行掃描，建議先查詢藍芽是否處於開啟狀態，未開啟先引導用戶等待至開啟成功
            //BleManager.getInstance().enableBluetooth();

            // 通過 startActivityForResult 引導界面，引導用戶打開藍芽
            new AlertDialog.Builder(this)
                    .setTitle("藍芽未開啟")
                    .setMessage("使用本APP需開啟藍芽，是否前往開啟？")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(BleActivity.this, "不開藍芽 = 功能 GG.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(intent, REQUEST_BT_ENABLE);
                        }
                    })
                    .show();

        } else {
            checkPermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_BT_ENABLE) {
            if (BleManager.getInstance().isBlueEnable()) {
                checkPermissions();
            } else {
                Toast.makeText(this, "您的藍芽未開啟,無法使用功能", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkPermissions() {
        Log.i("TAG", "check permissions");
        permission_coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        permission_fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        permission_bt = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        permission_bt_admin = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);
        if (permission_coarse == PackageManager.PERMISSION_GRANTED && permission_fine == PackageManager.PERMISSION_GRANTED && permission_bt == PackageManager.PERMISSION_GRANTED && permission_bt_admin == PackageManager.PERMISSION_GRANTED) {
            scan();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("TAG", "on Request permissionResult");
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                scan();
            }else {
                Toast.makeText(this, "未開啟權限無法使用", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void scan() {
        Log.i("TAG", "scan");
        // 設定掃描規則
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
              //  .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
              //  .setDeviceName(true, "GATT Server")         // 只扫描指定广播名的设备，可选
             //   .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
             //   .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 掃描超時時間，可自行設定，預設10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);

        // 開始掃描
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                //搜索完成，列出所有掃描到的設備，亦可能為空
                Log.i("TAG", "scan_finished. list" + scanResultList.toString());
                //stopScan();

            }

            @Override
            public void onScanStarted(boolean success) {
                //开始搜索
                Log.i("TAG", "scan_onScanStarted");
                scanState.setText("開始搜索");
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                Log.i("TAG", "scan_onScanning");
                // bleDevice：搜索到的设备，內含以下訊息：
                    // String getName()：廣播名稱
                    // String getMac()：Mac地址
                    // byte[] getScanRecord()：所攜帶的廣播數據
                    // int getRssi() ：信號強度
                // 後續進行 bleDevice 連接、斷開、判斷 device 狀態、讀寫操作時皆會用到

                Log.i("TAG", "scaning : " + bleDevice);
                // 如果搜尋到設備後就停止搜尋
                if (bleDevice != null) {
                    stopScan();
                    mac.setText(bleDevice.getName());
                    device = bleDevice;
                }
            }
        });
    }

    private void connectBle() {
        Log.i("TAG", "gatt connection");
        BleManager.getInstance().connect(device, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.i("TAG", "開始進行連接");
                Toast.makeText(BleActivity.this, "開始連接", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Log.i("TAG", "連接不成功 :" + bleDevice + exception);
                Toast.makeText(BleActivity.this, "連接不成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.i("TAG", "連接成功");
                connectGatt(bleDevice);
                device = bleDevice;

                connectState.setText("連接成功，uuid = " + uuid_service);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                Log.i("TAG", "連接斷開");
                // 斷線就關閉釋放資源，避免資源滿載造成之後無法連線
                gatt.disconnect();
                gatt.close();
                Toast.makeText(BleActivity.this, "連接斷開", Toast.LENGTH_LONG).show();
                // 監控設備連接狀態，一旦連接斷開，可指定對 bleDevice 進行重連。但建議斷開和重連之間最好間隔一段時間，否則有可能出現長時間連接不上的情況
            }
        });
    }

    private void disconnectBle() {
        BleManager.getInstance().disconnect(device);
        connectState.setText("斷開連接！");
        Log.i("TAG", "連接斷開");
    }

    private void connectGatt(BleDevice bleDevice) {
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
        List<BluetoothGattService> serviceList = gatt.getServices();
        for (BluetoothGattService service : serviceList) {
            uuid_service = service.getUuid();

            List<BluetoothGattCharacteristic > characteristicList = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristicList) {
                uuid_chara = characteristic.getUuid();
            }
            Log.i("TAG_uuid", "chara ：" + characteristicList.get(0).getUuid().toString());
        }
        Log.i("TAG_uuid", "service：" + serviceList.toString());
    }

    private void stopScan() {
        Log.i("TAG", "stop scan");
        scanState.setText("停止掃描");
        // 如果處於掃描狀態，就停止掃描
        if (BleManager.getInstance().getScanSate() == BleScanState.STATE_SCANNING) {
            BleManager.getInstance().cancelScan();
        }
    }

    private void bleRead() {
        BleManager.getInstance().read(
                device,
                uuid_service.toString(),
                uuid_chara.toString(),
                new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {
                        readData.setText(new String(data));
                        Log.i("TAG", "read_data ");
                    }

                    @Override
                    public void onReadFailure(BleException exception) {
                        readData.setText("on Read Fail.");
                    }
                }
        );
    }

    private void bleWrite() {
        BleManager.getInstance().write(
                device,
                uuid_service.toString(),
                uuid_chara.toString(),
                edtWrite.getText().toString().getBytes(),
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        // 若寫入位元超過20 byte，預設會自動分包傳送，可透過 onWriteSuccess的回傳參數檢視傳送進度
                        writeData.setText("寫入 - current：" + current + ", total：" + total + ", justWrite：" + new String(justWrite));
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        writeData.setText("寫入不成功");
                    }
                }
        );
    }

    private void bleNotify() {
        BleManager.getInstance().notify(
                device,
                uuid_service.toString(),
                uuid_chara.toString(),
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        Log.i("TAG", "打開通知操作成功");
                        Toast.makeText(BleActivity.this, "打開通知操作成功", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        Log.i("TAG", "打開通知操作失敗");
                        Toast.makeText(BleActivity.this, "打開通知操作失敗", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        Log.i("TAG", "打開通知後，device 發過來的數據將在這裡出現");
                        indecateData.setText(new String(data));
                    }
                }
        );
    }

    private void findViews() {
        btnScan = findViewById(R.id.btn_scan);
        btnStopScan = findViewById(R.id.btn_stop_scan);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnRead = findViewById(R.id.btn_read);
        btnWrite = findViewById(R.id.btn_write);
        btnNotify = findViewById(R.id.btn_notify);
        btnDisNotify = findViewById(R.id.btn_disable_notify);
        edtWrite = findViewById(R.id.et_write);
        scanState = findViewById(R.id.tv_scan_state);
        connectState = findViewById(R.id.tv_connect_state);
        readData = findViewById(R.id.tv_read_data);
        writeData = findViewById(R.id.tv_write_data);
        notifyData = findViewById(R.id.tv_notify_data);
        mac = findViewById(R.id.mac);
        btnIndicate = findViewById(R.id.btn_indicate);
        btnDisIndicate = findViewById(R.id.btn_disable_indicate);
        indecateData = findViewById(R.id.tv_indicate_data);
    }
}
