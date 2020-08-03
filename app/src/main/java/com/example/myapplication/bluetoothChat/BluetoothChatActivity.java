package com.example.myapplication.bluetoothChat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BluetoothChatActivity extends AppCompatActivity {
    private final int REQUEST_BT_ENABLE = 1;
    private final int REQUEST_PERMISSION = 2;
    private Button btnScan, btnStopScan, btnWrite;
    private TextView scanState, connectState, readData, writeData;
    private EditText edWrite;
    private ListView btItems, chatBox;
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> mConversationArrayAdapter;
    private Toast mToast;
    private String deviceAddress, deviceName;
    private BroadcastReceiver receiver;
    private Handler handler;
    private BluetoothDevice btDevice;
    private BluetoothChatService chatService;
    private StringBuffer mOutStringBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client2);

        findViews();
        initBt();

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBt();
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = edWrite.getText().toString();
                sendMessage(msg);
            }
        });

        mConversationArrayAdapter = new ArrayAdapter<>(this, R.layout.message);
        chatBox.setAdapter(mConversationArrayAdapter);
    }

    private void sendMessage(String message) {
        // 先確認 bluetoothChatService 是否仍連線
        if (chatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            showToast("You are not connected to a device");
            return;
        }

        // 確認用戶是否有寫入訊息
        if (message.length() > 0) {
            // 將寫入之 String 轉成 bytes，再傳至 BluetoothChatService
            byte[] send = message.getBytes();
            chatService.write(send);

            // 清空 string buffer & 輸入欄位
            mOutStringBuffer.setLength(0);
            edWrite.setText("");
        }
    }

    private void scanBt() {
        showLog("scanBt - 打開藍牙可見性");
        // 打開藍牙可見性
        Intent btDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        btDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        showToast("打開藍芽可見性");
        startActivity(btDiscoverableIntent);

        // 查找其他藍牙設備
        showLog("scanBt - 查找其他藍牙設備");
        BluetoothAdapter.getDefaultAdapter().startDiscovery();

        showLog("scanBt - 獲取已配對裝置");
        // 獲取已配對裝置
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<Map<String, String>> items = new ArrayList<Map<String,String >>();
        if (pairedDevices.size() > 0) {
            devices.addAll(pairedDevices);
            showLog("scanBt - 獲取已配對裝置" + pairedDevices.toString());
            showToast("獲取已配對裝置");

            for (BluetoothDevice device : pairedDevices) {
                Map<String, String> item = new HashMap<String, String>();
                if (device.getName()!= null) {
                    item.put("name", device.getName());
                } else {
                    item.put("name", "unknown");
                }
                item.put("mac", device.getAddress());
                items.add(item);
            }
        }

        // 獲取未配對裝置
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        showToast("藍芽打開");
                        break;

                    case BluetoothAdapter.STATE_ON:
                        showToast("藍芽關閉");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        showToast("藍芽正在打開");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        showToast("藍芽正在關閉");
                        break;

                    default:
                        showToast("藍芽狀態：其他");
                        break;
                }
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // 找到一個添加一個
                    devices.add(device);
                    Map<String, String> item = new HashMap<String, String>();
                    if (device.getName()!= null) {
                        item.put("name", device.getName());
                    } else {
                        item.put("name", "unknown");
                    }

                    item.put("mac", device.getAddress());
                    items.add(item);
                    showToast("獲取未配對裝置");
                    showLog("scanBt - 獲取未配對裝置" + device.toString());
                }

                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                    showLog("scanBt - 結束掃描" + items.toString());
                    setDevicesList(items);
                }
            }
        };

        // 監聽廣播，註釋掉的二行可以縮寫為下面的一行
        // IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        // registerReceiver(receiver, filter);
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

    }

    private void setDevicesList(List<Map<String, String>> items) {
        showLog("set list.");
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                items,
                R.layout.item_bt_devices,
                new String[]{"name", "mac"},
                new int[]{R.id.name, R.id.mac});

        btItems.setAdapter(adapter);
        btItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 關閉掃描，因掃描非常耗資源，故已確認要連接哪個設備後就可關掉
                if(bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                }

                deviceAddress = items.get(position).get("mac");
                deviceName = items.get(position).get("name");
                for (BluetoothDevice device : devices) {
                    if (device.getAddress().equals(deviceAddress)) {
                        device.createBond();
                    }
                }

                showLog("name : " + deviceName + ", mac : " + deviceAddress);

                // 取得mac
                btDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                // 連接設備
                chatService.connect(btDevice);

            }
        });
        connectState.setText(bluetoothAdapter.getBondedDevices().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        if(chatService != null) {
            chatService.stop();
        }
    }

    private void openBt() {
        showLog("確認是否開啟藍牙");
        // 開啟藍芽
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                // 若未開啟藍芽則自動開啟，但此法不建議
                // bluetoothAdapter.enable();

                // 官方推薦 - 引導用戶開啟藍芽功能
                showLog("引導用戶開啟藍芽");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_BT_ENABLE);
            }
        } else {
            showToast("該裝置不支援藍芽");
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            connectState.setText("connecting...");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            connectState.setText("not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    mConversationArrayAdapter.add(deviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    showToast("Connected to " + deviceName);
                    break;
                case Constants.MESSAGE_TOAST:
                    showToast(msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };

    private void initBt() {
        checkLocationPermissions();
        showLog("確認危險權限");
        openBt();
        // 初始化 BluetoothChatService
        chatService = new BluetoothChatService(this, mHandler);

        // 初始化 StringBuffer
        mOutStringBuffer = new StringBuffer();
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void showLog(String text) {
        Log.i("TAG", text);
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
                showToast("未開啟權限無法使用");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
        } else {
            showToast("尚未開啟藍芽，無法使用藍芽功能");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (chatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (chatService.getState() == BluetoothChatService.STATE_NONE) {   // 初始狀態
                // Start the Bluetooth chat services
                chatService.start();
                showLog("onResume - chatService start.");
            }
        }
    }

    private void findViews() {
        btnScan = findViewById(R.id.btn_scan);
        btnStopScan = findViewById(R.id.btn_stop_scan);
        btItems = findViewById(R.id.lv);
        btnWrite = findViewById(R.id.btn_write);
        scanState = findViewById(R.id.tv_scan_state);
        connectState = findViewById(R.id.tv_connect_state);
        readData = findViewById(R.id.tv_read_data);
        writeData = findViewById(R.id.tv_write_data);
        chatBox = findViewById(R.id.chatBox);
        edWrite = findViewById(R.id.et_write);
    }
}
