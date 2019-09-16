package com.example.bluetoothapp;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT=1;
    ListView lv_paired_devices;
    Set<BluetoothDevice> set_pairedDevices;
    ArrayAdapter adapter_paired_devices;
    BluetoothAdapter bluetoothAdapter;
    public ConnectThread connectThread;
    public ConnectedThread connectedThread;
    public ImageView imageView;
    public ProgressBar progressBar;
    public View shadeView;
    public LinearLayout workLinearLayout;

    ListView unpairedDevicesView;
    BluetoothDeviceAdapter bluetoothDeviceAdapter;
    private static List<BluetoothDevice> unpairedDevicesList = new ArrayList<>();

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int MESSAGE_READ=0;
    public static final int MESSAGE_WRITE=1;
    public static final int CONNECTING=2;
    public static final int CONNECTED=3;
    public static final int NO_SOCKET_FOUND=4;


    String bluetooth_message="00";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(getApplicationContext(),"Discover started",Toast.LENGTH_LONG).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Toast.makeText(getApplicationContext(),"Discover finished " + unpairedDevicesList.size(),Toast.LENGTH_LONG).show();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()!=null) {
                    unpairedDevicesList.add(device);

                    bluetoothDeviceAdapter.notifyDataSetChanged();
                }
                //Toast.show("Found device " + device.getName());
            }
        }
    };



    @SuppressLint("HandlerLeak")
    Handler mHandler=new Handler()
    {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what){
                case MESSAGE_READ:

                    byte[] readbuf=(byte[])msg_type.obj;
                    String string_recieved=new String(readbuf);
                    //Toast.makeText(getApplicationContext(),"MSG RECEIVED",Toast.LENGTH_SHORT).show();
                    //do some task based on recieved string

                    break;
                case MESSAGE_WRITE:

                    if(msg_type.obj!=null){
                        connectedThread=new ConnectedThread((BluetoothSocket)msg_type.obj);
                        connectedThread.start();
                        connectedThread.write(bluetooth_message.getBytes());
                        connectedThread.size = -1;

                    }
                    break;

                case CONNECTED:

                    Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                    //sendImageConfirmation();
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(),"Connecting...",Toast.LENGTH_SHORT).show();
                    break;

                case NO_SOCKET_FOUND:
                    Toast.makeText(getApplicationContext(),"No socket found",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void sendImageConfirmation(View v){
        if(connectedThread != null) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pepe);
            //Bitmap bmp = Bitmap.createBitmap(500,500,Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            bmp.recycle();
            Log.d("Here", "I am in connecte handler");
            getData(byteArray);
        }
        else{

            Toast.makeText(getApplicationContext(),"No device connected",Toast.LENGTH_SHORT).show();
        }
    }

    public void getData(final byte[] dataArray) {
        final int size = dataArray.length;
        Log.d("MainActivityDataPerr","Size in bytes: " + size);
        if(size > 800){
            Thread tSender = new Thread(new Runnable() {
                ConnectedThread connectedThread;

                @Override
                public void run() {
                    int responseTemp = size;
                    ByteArrayOutputStream bosTemp = new ByteArrayOutputStream();
                    try
                    {
                        ObjectOutputStream oos1 = new ObjectOutputStream(bosTemp);
                        oos1.writeObject(responseTemp);
                        oos1.flush();
                    }catch (Exception e) { Log.d("MainActivityDataPerr",""+e); }

                    if (connectedThread != null) {
                        Log.d("MainActivityDataPerr","I've sended the size!");
                        connectedThread.write(bosTemp.toByteArray());
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    byte[] tempMsg;
                    int a = size / 800;
                    if (size % 800 != 0) {
                        a++;
                    }
                    Log.d("Chunck", "I am in size: " + size);
                    int p = 0;
                    while(a>0){

                        tempMsg = new byte[800];
                        int i = 0;
                        //Log.d("Chunck", "----------------------------------------------------------------" + (connectedThread == null));
                        while(i<800){
                            if(p<size) {
                                tempMsg[i] = dataArray[p];

                            }
                            i++;
                            p++;
                        }
                        //Log.d("Chunck", "Chunk number " + a + " ; ");
                        //Log.d("MainActivityDataPerr","Size is: " + a + " || " + tempMsg[0] + " || " + p);

                        connectedThread.write(tempMsg);
                        //connectedThread.write(tempMsg);

                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        a--;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Message sent!",Toast.LENGTH_LONG).show();
                        }
                    });

                }

                public Runnable init(ConnectedThread connectedThread){
                    this.connectedThread=connectedThread;
                    return this;
                }
            }.init(connectedThread));

            tSender.start();
        }
        else{
            if (connectedThread != null) {
                connectedThread.write(dataArray);
            }
        }
    }




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.loadingScreen);
        shadeView = findViewById(R.id.shadeView);
        workLinearLayout = findViewById(R.id.workLayout);
        getSupportActionBar().hide();
        getPermissions();
        imageView = findViewById(R.id.imageViewTemp);
        unpairedDevicesView = findViewById(R.id.unpairdeList);
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(unpairedDevicesList,this);

        unpairedDevicesView.setAdapter(bluetoothDeviceAdapter);
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);

        unpairedDevicesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice tempDevice = unpairedDevicesList.get(position);
                tempDevice.createBond();
            }
        });

        initialize_layout();
        initialize_bluetooth();
        start_accepting_connection();
        initialize_clicks();

    }

    public void refreshList(View v)
    {
        initialize_layout();
        initialize_bluetooth();
    }

    private void getPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if(SDK_INT > 8){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    public void becomeVisible(View v){
        Intent turnDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(turnDiscoverable, 0);
    }

    public void findDevices(View v){

        //unpairedDevicesView = findViewById(R.id.unpairdeList);
        unpairedDevicesList= new ArrayList<>();
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(unpairedDevicesList,this);

        unpairedDevicesView.setAdapter(bluetoothDeviceAdapter);
        bluetoothAdapter.startDiscovery();


    }

    public void start_accepting_connection()
    {
        //call this on button click as suited by you

        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
        Toast.makeText(getApplicationContext(),"accepting",Toast.LENGTH_SHORT).show();
    }
    public void initialize_clicks()
    {
        lv_paired_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Object[] objects = set_pairedDevices.toArray();
                BluetoothDevice device = (BluetoothDevice) objects[position];

                connectThread = new ConnectThread(device);
                connectThread.start();

                Toast.makeText(getApplicationContext(),"device choosen "+device.getName(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void initialize_layout()
    {
        lv_paired_devices = (ListView)findViewById(R.id.lv_paired_devices);
        adapter_paired_devices = new ArrayAdapter(getApplicationContext(),R.layout.support_simple_spinner_dropdown_item);
        lv_paired_devices.setAdapter(adapter_paired_devices);
    }

    public void initialize_bluetooth()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            finish();
        }

        //Add these permisions before
//        <uses-permission android:name="android.permission.BLUETOOTH" />
//        <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
//        <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
//        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        else {
            set_pairedDevices = bluetoothAdapter.getBondedDevices();

            if (set_pairedDevices.size() > 0) {

                for (BluetoothDevice device : set_pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address

                    adapter_paired_devices.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    }


    public class AcceptThread extends Thread
    {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("NAME",MY_UUID);
            } catch (IOException e) { }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null)
                {
                    // Do work to manage the connection (in a separate thread)
                    mHandler.obtainMessage(CONNECTED).sendToTarget();
                    // Do work to manage the connection (in a separate thread)
                    bluetooth_message = "Initial message";
                    mHandler.obtainMessage(MESSAGE_WRITE,socket).sendToTarget();
                }
            }
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mHandler.obtainMessage(CONNECTING).sendToTarget();

                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            bluetooth_message = "Initial message";
            mHandler.obtainMessage(MESSAGE_WRITE,mmSocket).sendToTarget();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public int size;
        byte[] tempMsg = new byte[0];

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            //size = -1;
        }

        public void run() {

            Log.d("Erros5", "I AM INITIED+++++++++++++++++++++++++++++++\n");
            byte[] buffer = new byte[800];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    //Log.d("Erros5", "GOT SMTH+++++++++++++++++++++++++++++++\n");
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    try {
                        if(size ==-1) {
                            ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
                            Log.d("Erros5", "1\n");
                            ObjectInputStream ois = new ObjectInputStream(bis);
                            Log.d("Erros5", "2\n");
                            size = (int) ois.readObject();
                            int a = size / 800;
                            if (size % 800 != 0) {
                                a++;
                            }
                            size = a;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.VISIBLE);
                                    shadeView.setVisibility(View.VISIBLE);
                                    workLinearLayout.setEnabled(false);
                                }
                            });

                            //Log.d("Erros5", " size is " + size);
                        }
                        else {

                            //Log.d("Inside", "size: " + size + " || " + bytes);
                            if(bytes!=0) {
                                byte[] tempAnswer = new byte[bytes + tempMsg.length];
                                int count = 0;

                                for (int i = 0; i < tempMsg.length; i++) {
                                    tempAnswer[i] = tempMsg[i];
                                    count++;
                                }

                                for (int j = 0; j < bytes; j++) {
                                    tempAnswer[count++] = buffer[j];

                                }
                                //Log.d("Inside", "Chunk done!");
                                tempMsg = tempAnswer;

                                size--;
                                //Log.d("Erros5", bytes + " || " + tempMsg.length);
                            }
                            if(size == 0) {

                                final Bitmap bm = BitmapFactory.decodeByteArray(tempMsg,0,tempMsg.length);
                                Log.d("Erros5", "1\n " + (bm==null));
                                //ByteArrayInputStream bis = new ByteArrayInputStream(tempMsg);
                                //Log.d("Erros5", "1\n");
                                //ObjectInputStream ois = new ObjectInputStream(bis);
                                //Log.d("Erros5", "2\n");
                                //final Bitmap model = (Bitmap) ois.readObject();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        progressBar.setVisibility(View.INVISIBLE);
                                        shadeView.setVisibility(View.INVISIBLE);
                                        workLinearLayout.setEnabled(true);
                                        imageView.setVisibility(View.VISIBLE);
                                        imageView.setImageBitmap(bm);
                                    }
                                });
                                Log.d("Erros5", "I GOT BITMAP+++++++++++++++++++++++++++");
                                tempMsg = new byte[0];
                                size--;
                            }
                        }

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }



        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            //Log.d("Erros5", "Sent+++++++++++++++++++++++++++++++\n");
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("Erros5", "ERORO SENT____________________________________\n");
                e.printStackTrace();
            }
        }


        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}