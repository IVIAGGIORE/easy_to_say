package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import  android.os.CountDownTimer ;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    final int ArduinoData = 1;
    final String LOG_TAG = "myLogs";
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static String MacAddress = "98:DA:50:00:38:03"; // MAC-адрес БТ модуля
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectedThred MyThred = null;
    public TextView mytext;
    Button b1, b2, b3, b4;
    EditText tb1;
    Handler h;
    CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mytext = (TextView) findViewById(R.id.txtrobot);

        if (btAdapter != null){
            if (btAdapter.isEnabled()){
                mytext.setText("Bluetooth включен. Все отлично.");
            }else
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

        }else
        {
            MyError("Fatal Error", "Bluetooth ОТСУТСТВУЕТ");
        }

        b1 = (Button) findViewById(R.id.b1);
        b2 = (Button) findViewById(R.id.b2);
        b3 = (Button) findViewById(R.id.b3);
        b4 = (Button) findViewById(R.id.b4);
        tb1= (EditText) findViewById(R.id.plain_text_input);

        b1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                tb1.setText("");
                MyThred.sendByte((byte)1);
            }
        });

        b2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MyThred.sendByte((byte)0);
            }
        });

        b3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (tb1.length() != 1) return;
                byte temp = EncodeW1251(tb1.getText().toString())[0];
                tb1.setText("");
                //if (temp < 48) return;
                MyThred.sendByte(temp);
            }
        });

        b4.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MyThred.sendByte((byte)2);
            }
        });

        timer = new CountDownTimer(2000, 100) {

            public void onTick(long millisUntilFinished) {
                //mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                //tb1.setText("");
                mytext.setText("Нет данных");
            }
        };


        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case ArduinoData:
                        byte[] readBuf = (byte[]) msg.obj;
                        //String strIncom = new String(readBuf, 0, msg.arg1);
                        String strIncom = DecodeW1251(readBuf).substring(0, msg.arg1);
                        mytext.setText("Данные от Arduino");
                        if (strIncom.equals(".")) tb1.setText("");
                        else tb1.setText(tb1.getText() + strIncom);
                        timer.start();
                        break;
                }
            };
        };

    }

    public static byte[] EncodeW1251(CharSequence string) {
        try {
            ByteBuffer bytes = Charset.forName("windows-1251").newEncoder().encode(CharBuffer.wrap(string));
            byte[] bytesCopy = new byte[bytes.limit()];
            System.arraycopy(bytes.array(), 0, bytesCopy, 0, bytes.limit());
            return bytesCopy;
        }
        catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Encoding failed", e);
        }
    }
    public static String DecodeW1251(byte[] bytearray) {
        try {
            return Charset.forName("windows-1251").newDecoder().decode(ByteBuffer.wrap(bytearray)).toString();
        }
        catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Encoding failed", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        BluetoothDevice device = btAdapter.getRemoteDevice(MacAddress);
        Log.d(LOG_TAG, "***Получили удаленный Device***"+device.getName());

        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            Log.d(LOG_TAG, "...Создали сокет...");
        } catch (IOException e) {
            MyError("Fatal Error", "В onResume() Не могу создать сокет: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();
        Log.d(LOG_TAG, "***Отменили поиск других устройств***");

        Log.d(LOG_TAG, "***Соединяемся...***");
        try {
            btSocket.connect();
            Log.d(LOG_TAG, "***Соединение успешно установлено***");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                MyError("Fatal Error", "В onResume() не могу закрыть сокет" + e2.getMessage() + ".");
            }
        }

        MyThred = new ConnectedThred(btSocket);
        MyThred.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(LOG_TAG, "...In onPause()...");

        if (MyThred.status_OutStrem() != null) {
            MyThred.cancel();
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            MyError("Fatal Error", "В onPause() Не могу закрыть сокет" + e2.getMessage() + ".");
        }
    }

    private void MyError(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }


    //Отдельный поток для передачи данных
    private class ConnectedThred extends Thread{
        private final BluetoothSocket copyBtSocket;
        private final OutputStream OutStrem;
        private final InputStream InStrem;

        public ConnectedThred(BluetoothSocket socket){
            copyBtSocket = socket;
            OutputStream tmpOut = null;
            InputStream tmpIn = null;
            try{
                tmpOut = socket.getOutputStream();
                tmpIn = socket.getInputStream();
            } catch (IOException e){}

            OutStrem = tmpOut;
            InStrem = tmpIn;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try{
                    bytes = InStrem.read(buffer);
                    h.obtainMessage(ArduinoData, bytes, -1, buffer).sendToTarget();
                }catch(IOException e){break;}

            }

        }

        public void sendData(String message) {
            byte[] msgBuffer = message.getBytes();
            Log.d(LOG_TAG, "***Отправляем данные: " + message + "***"  );

            try {
                OutStrem.write(msgBuffer);
            } catch (IOException e) {}
        }

        public void sendByte(byte msgBuffer) {
            Log.d(LOG_TAG, "***Отправляем данные: " + msgBuffer+ "***"  );
            try {
                OutStrem.write(msgBuffer);
            } catch (IOException e) {}
        }

        public void cancel(){
            try {
                copyBtSocket.close();
            }catch(IOException e){}
        }

        public Object status_OutStrem(){
            if (OutStrem == null){return null;
            }else{return OutStrem;}
        }
    }
}