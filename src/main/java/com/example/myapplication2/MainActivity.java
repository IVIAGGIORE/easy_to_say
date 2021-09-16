package com.example.myapplication2;
import java.io.IOException;
import java.util.Set;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 0;
    final String LOG_TAG = "myLogs";
    public BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket = null;
    // MAC-адрес Bluetooth модуля
    private static String MacAdress = "98:DA:50:00:38:03";
    public TextView mytext;
    Button SoundСonnection;
    Handler bluetoothIn;
    BluetoothAdapter myBluetooth = null;
    String address = null, name = null;
    Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mytext = (TextView) findViewById(R.id.txtrobot);

        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {
                mytext.setText("Bluetooth включен. Все отлично.");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

        } else {
            MyError("Fatal Error", "Bluetooth ОТСУТСТВУЕТ");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "***Пытаемся соединиться***");
        // Получаем удаленное устройство по его MAC адресу
        BluetoothDevice device = btAdapter.getRemoteDevice(MacAdress);
        mytext.setText("***Получили device = " + device.getName() + "***");
    }

    private void MyError(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
}