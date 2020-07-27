package com.example.myapplication.thread;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import static android.content.ContentValues.TAG;

// 在server端的子執行緒中發起連線
public class AcceptThread extends Thread {
    private final static UUID MY_UUID = UUID.fromString("02001101-0001-1000-8080-00805F9BA9BA");
    private final BluetoothServerSocket mmServerSocket;
    public BluetoothAdapter bluetoothAdapter;

    public AcceptThread() {
        BluetoothServerSocket tmp = null;
        try {
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
// IGNORE
        }
        mmServerSocket = tmp;
    }
    public void run() {
        BluetoothSocket socket = null;
// Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
// If a connection was accepted
            if (socket != null) {
// Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
                mmServerSocket.close();
                break;
            }
        }
    }
    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel () {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
// IGNORE
        }
    }
}