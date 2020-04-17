package com.example.covid;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PandamicService extends Service {

    private static final String TAG = "MyService";
    UUID uuid = UUID.fromString("b39c07ce-7c62-11ea-bc55-0242ac130003");
    String NAME = "pandamicapp";
    private static Set<BluetoothDevice> selectedDevices;
    private boolean isAcceptAlive = false;
    private boolean isBTPermissionGranted = false;
    private boolean isLocationPermissionGranted = false;
    private boolean isServiceWorking = false;
    private BluetoothAdapter mBluetoothAdapter;
    ConnectedThread mConnectedThread;
    Handler mHandler;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    public PandamicService() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.i(TAG, "handleMessage");

            }
        };

    }
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        registerReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //  periodicUpdate.scheduleNext();
        createNotificationChannel(getString(R.string.monitor_started));
        boolean sendMessage = false;
        if (intent != null && intent.hasExtra("sendmessage")) {
            sendMessage = intent.getBooleanExtra("sendmessage", false);
        }
        if (sendMessage && wantToUpdate() && isPositive()) {
            sendMessage(null);
        }

        //if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            periodicUpdate.post();
       // }

        return super.onStartCommand(intent, flags, startId);
    }

    private void stopMonitor() {
        Log.d(TAG, "onClick: enabling/disabling bluetooth.");
        disableBT();
        isServiceWorking = false;
        periodicUpdate.cancelFuture();
        stopSelf();
    }

    private void sendAlert() {
        connectDevice(null);
    }

    private void startServer() {
        if (!mBluetoothAdapter.isEnabled()) {
         //   Toast.makeText(this, "Click on Start Monitor", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Click on Start Monitor");
        } else {
            AcceptThread accept = new AcceptThread();
            accept.start();
        }
    }

    private void registerReceivers() {
        try {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver4, filter);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);

            IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(mBroadcastReceiver2, intentFilter);

            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

            IntentFilter discoverDevicesPairIntent = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
            registerReceiver(mBroadcastReceiver5, discoverDevicesPairIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermissions() {
       // createNotificationChannel(getString(R.string.permission_checking));
        checkBTPermissions();
        checkLocationPermissions();
    }

    private void checkBTPermissions() {
        boolean isBluetoothSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (isBluetoothSupported) {
            enableBT();
            enableDisableDiscoverable(null);
            //btnDiscover();
        }
    }

    private void checkLocationPermissions() {
     /*   int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            //send notification
            //  this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }*/
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isLocationPermissionGranted = false;
            Intent enableBTIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            enableBTIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBTIntent);
        } else {
            isLocationPermissionGranted = true;
        }
    }


    public void enableBT() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Does not have Bluetooth capabilities", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            createNotificationChannel("Enable Bluetooth");
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBTIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBTIntent);
        }
    }

    public void disableBT() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Does not have Bluetooth capabilities", Toast.LENGTH_SHORT).show();
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();
/*
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);*/
        }
        //stopSelf();
    }

    public void enableDisableDiscoverable(View view) {

        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
        if (mBluetoothAdapter.isEnabled() && !mBluetoothAdapter.isDiscovering()) {
            createNotificationChannel("Make Bluetooth discoverable ");
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1000);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(discoverableIntent);
        }

    }

    public void btnDiscover() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Does not have Bluetooth capabilities", Toast.LENGTH_SHORT).show();
        } else {
           /* if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(MyService.this, "Click on Start Monitor", Toast.LENGTH_SHORT).show();
            } else {*/
            if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "btnDiscover: Canceling discovery.");
            }


            //check permissions
            // if start monitor is on and BT or location is off or not discoverable then send alert to switch on
            checkPermissions();

            mBluetoothAdapter.startDiscovery();
            mBTDevices.clear();
            EventBus.getDefault().post(mBTDevices);
        }
    }


    public void connectDevice(View v) {
        //getDiscoverable
        //Pair
        //connect
        //send message
        if (!mBluetoothAdapter.isEnabled()) {
          //  Toast.makeText(this, "Click on Start Monitor", Toast.LENGTH_SHORT).show();
        } else {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            ArrayList<BluetoothDevice> list = new ArrayList<>();
            if (selectedDevices != null && !selectedDevices.isEmpty()) {
                list = new ArrayList<>(selectedDevices);
            } else {
                //enable after testing
                //list = mBTDevices;
               //Testing
                list = new ArrayList<>(pairedDevices);

            }
            if (list != null) {
                for (BluetoothDevice device : list) {
                    Log.i(TAG, "Devices selected or paired :: " + device.getName() + " " + device.getAddress());
                    if (pairedDevices == null || !pairedDevices.contains(device)) {
                        startPairing(device);
                    } else {
                        Log.e(TAG, "" + pairedDevices.size());
                        if (pairedDevices.size() > 0) {
                            connectOperation(device);
                        }
                    }
                }
                if(selectedDevices != null)
                selectedDevices.clear();
            }
        }/*else {
                if (pairedDevices == null)
                    Toast.makeText(this, "Pair device before connect and send message ", Toast.LENGTH_SHORT).show();

                if (selectedDevices == null)
                    Toast.makeText(this, "Please select device before connection ", Toast.LENGTH_SHORT).show();
            }*/
    }
    //}
    /*Set<BluetoothDevice> selectedDevices = new HashSet<>();*/

    @Subscribe
    public void getSelectedDevices(Set<BluetoothDevice> devices) {
        selectedDevices = devices;
        connectDevice(null);
    }

    private void startPairing(BluetoothDevice device) {
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            // mBTDevices.get(i).createBond();
            try {
                pairDevice(device);
                // connect(device);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            Log.d("pairDevice()", "Start Pairing...");

        }
    }

    private void pairDevice(BluetoothDevice device) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (device != null) {
            Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
            Method createBondMethod = class1.getMethod("createBond");
            Boolean returnValue = (Boolean) createBondMethod.invoke(device);
            Log.d("pairDevice()", "Pairing booleanValue ::    " + returnValue.booleanValue());
            Log.d("pairDevice()", "Pairing finished.");
        }
    }


    private void connected(BluetoothSocket mmSocket, boolean client) {
        Log.d(TAG, "connected: Starting.");

        if (mmSocket != null) {
            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new ConnectedThread(mmSocket, client);
            mConnectedThread.start();
        }
    }

    private void connectOperation(BluetoothDevice device) {
        Log.e(TAG, "" + device);
        ConnectThread connect = new ConnectThread(device, uuid);
        connect.start();

    }

    public void sendMessage(View v) {
        Log.e(TAG, "sendMessage");
        if (!mBluetoothAdapter.isEnabled()) {
            // Toast.makeText(this, "Click on Start Monitor", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "sendMessage");
        } else {
            if (mConnectedThread != null) {
                byte[] bytes = getResources().getString(R.string.pandamic_positive_alert).getBytes(Charset.defaultCharset());
                mConnectedThread.write(bytes);
            } else {
                //  Toast.makeText(this, "Please Accept or Connect to other device with Bluetooth", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Please Accept or Connect to other device with Bluetooth");
            }
        }
    }


    public void replyMessage(View v) {
        if (mBluetoothAdapter.isEnabled()) {
            //Toast.makeText(this, "Click on Start Monitor", Toast.LENGTH_SHORT).show();
            if (mConnectedThread != null) {
                byte[] bytes = getResources().getString(R.string.appreciate_message).getBytes(Charset.defaultCharset());
                mConnectedThread.write(bytes);
            } else {
             //   Toast.makeText(this, "Please Accept or Connect to other device with Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }


    class MyRunnable implements Runnable {
        Handler handler;

        public MyRunnable() {
            handler = new Handler();
        }

        @Override
        public void run() {
            if (!hasStoppedMonitor()) {
                doStuff();
                scheduleNext();
            } else {
                //stopMonitor();
                cancelFuture();
            }
        }

        void doStuff() {
            isServiceWorking = true;
            Log.i(TAG, "doStuff START");
            //        EventBus.getDefault().post(mBTDevices);
           /* if(1 != 0)
                return;*/
            //run task after every 10 min
            btnDiscover();
            //register broadcast receivers
            registerReceivers();
            //start Accept thread ------isAcceptAlive = false
            startServer();
            //connect to all discoverables which are paired
            //request pairing for unpaired devices and then connect and send alert
            sendAlert();
            //on click of stop monitor stop service and unregister receivers and update preference
            //stopMonitor();
            Log.i(TAG, "doStuff END");
        }

        void scheduleNext() {
            Log.i(TAG, "scheduleNext  START");
            cancelFuture();
            handler.postDelayed(periodicUpdate, 5 * 60 * 1000); //milliseconds);
            Log.i(TAG, "scheduleNext  END");
        }

        void post() {
            cancelFuture();
            Log.i(TAG, "post  START");
            handler.post(periodicUpdate); //milliseconds);
            Log.i(TAG, "post  END");

        }

        void cancelFuture() {
            handler.removeCallbacks(this, null);
        }

    }

    SharedPreferences sharedPref;

    private boolean hasStoppedMonitor() {
        Log.i(TAG, "hasStoppedMonitor  Start");
        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int isMonitor = sharedPref.getInt(getString(R.string.monitor), 0);
        Log.i(TAG, "hasStoppedMonitor  " + isMonitor);
        return isMonitor == 1 ? false : true;
    }

    private MyRunnable periodicUpdate = new MyRunnable();


    @Subscribe
    public void listMessage(ArrayList<String> list) {
        Log.d("EventsBus", " recievedMessage " + list);
        //selectedDevices = list;
        //connectDevice(null);
        if (isPositive() && wantToUpdate()) {
            sendMessage(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, " onDestroy");
        createNotificationChannel("Service stopped");
        periodicUpdate.cancelFuture();
        isServiceWorking = false;
        mBluetoothAdapter = null;
        mBTDevices = null;
        mHandler.removeCallbacks(null);
        mHandler = null;
        EventBus.getDefault().unregister(this);
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
            unregisterReceiver(mBroadcastReceiver5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String status = "Bluetooth status changed ";
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        status = status + "STATE OFF";
                        Log.d(TAG, "mBroadcastReceiver1 :: onReceive: STATE OFF");
                        isBTPermissionGranted = false;
                        createNotificationChannel("StopSpreadCoVID19 App need bluetooth service to get notification");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        createNotificationChannel("StopSpreadCoVID19 App need bluetooth service to get notification");
                        status = status + "STATE TURNING OFF";
                        isBTPermissionGranted = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        createNotificationChannel(status);
                        isBTPermissionGranted = true;
                        status = status + "STATE ON";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        status = status + "STATE TURNING ON";
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        createNotificationChannel(status);
                        isBTPermissionGranted = true;
                        break;
                }

               // Toast.makeText(context, status, Toast.LENGTH_SHORT).show();

            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                String status = "Scan mode changed ";
                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        status = status + " Discoverability Enabled.";

                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        createNotificationChannel(status);
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        status = status + " Discoverability Disabled. Able to receive connections.";
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        // status = status + " Discoverability Disabled. Not able to receive connections";
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        createNotificationChannel("StopSpreadCoVID19 App need bluetooth service to get notification");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        status = status + " Connecting....";
                        createNotificationChannel(status);
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        status = status + " Connected";
                        createNotificationChannel(status);
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

                //Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
            }
        }
    };


    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver3 :: onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (!mBTDevices.contains(device)) {
                    // New device added.
                    //Send alert to all about COVID positive
                    mBTDevices.add(device);
                    EventBus.getDefault().post(mBTDevices);
                }
                Log.d(TAG, "mBroadcastReceiver3 :: onReceive: " + device.getName() + ": " + device.getAddress());
//                EventBus.getDefault().post(mBTDevices);

                /*mDeviceListAdapter.setDevices(mBTDevices);
                mDeviceListAdapter.notifyDataSetChanged();*/
                String status = ": " + device.getAddress();

                if (device.getName() != null && !device.getName().isEmpty())
                    status += device.getName();
                //Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
                //createNotificationChannel(status);
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }

                createNotificationChannel("Devices Bond state change " + mDevice.getName() + " " + mDevice.getBondState());
                //Toast.makeText(context, "Devices Bond state change " + mDevice.getName() + " " + mDevice.getBondState(), Toast.LENGTH_SHORT).show();
            }

        }
    };

    /**
     * Broadcast Receiver for act on pairing request
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver5 :: onReceive: ACTION FOUND.");

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Toast.makeText(context, "Paired with " + dev.getName(), Toast.LENGTH_SHORT).show();
                createNotificationChannel("Paired with " + dev.getName());
                connectOperation(dev);

            }
        }
    };

    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, uuid);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + uuid);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            mmServerSocket = tmp;
        }

        public void run() {
            isAcceptAlive = true;
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");
                if (mmServerSocket != null) {
                    socket = mmServerSocket.accept();
                    createNotificationChannel("Accept call successful ");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PandamicService.this, "Accept call successful", Toast.LENGTH_SHORT).show();
                            /*onConnectionSuccess();*/

                        }
                    });
                    Log.d(TAG, "run: RFCOM server socket accepted connection.");
                }
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            //talk about this is in the 3rd
            if (socket != null) {

                connected(socket,false);
            }

            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {

            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
                isAcceptAlive = false;
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }

    }

    private class ConnectedThread extends Thread {
        private boolean isClient = false;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, boolean client) {
            Log.d(TAG, "ConnectedThread: Starting.");
            isClient = client;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                //Toast.makeText(PandamicService.this, "Error in Receiving message ", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()
            if (isPositive() && wantToUpdate()) {
                sendMessage(null);
            }
            // Keep listening to the InputStream until an exception occurs
            while (true  /*!hasStoppedMonitor() && wantToUpdate()*/) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    final String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (incomingMessage != null && !incomingMessage.isEmpty()) {
                                /*mResponse.setText(incomingMessage);*/
                                //send notification
                                createNotificationChannel(incomingMessage);
                                if (incomingMessage.equals(getString(R.string.pandamic_positive_alert))) {
                                    //replyMessage(null);
                                    byte[] bytes = getResources().getString(R.string.appreciate_message).getBytes(Charset.defaultCharset());
                                    write(bytes);
                                }
                            }
                        }
                    });
                    //   break;
                    // SendHelloMessage("Start communication");

                } catch (IOException e) {
                    e.printStackTrace();
                    if (!isClient) {
                        try {
                            mmSocket.close();
                            mmOutStream.close();
                            cancel();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        // break;
                    /*runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(PandamicActivity.this, "Error in Receiving message ", Toast.LENGTH_SHORT).show();
                        }
                    });*/
                    }
                }
                if (isClient) {
                    try {
                        mmSocket.close();
                        mmOutStream.close();
                        cancel();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }


        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e) {
            }
        }
    }

    private boolean wantToUpdate() {
        //  Log.i(TAG, "wantToUpdate   " + sharedPref.getInt(getString(R.string.wanttoupdate), 0));
        //   return sharedPref.getInt(getString(R.string.wanttoupdate), 0) == 1 ? true : false;
        return true;
    }

    private boolean isPositive() {
        Log.i(TAG, "isPositive   " + sharedPref.getInt(getString(R.string.positive), 0));
        return sharedPref.getInt(getString(R.string.positive), 0) == 1 ? true : false;
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
        }

        public void run() {
            BluetoothSocket tmp = null;
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //   Toast.makeText(MyService.this, "Connect failed.Please try again ", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            mmSocket = tmp;
            int attempt = 0;
            boolean isConnectionSuccessful = false;
            // Make a connection to the BluetoothSocket
            while (attempt < 3 && !isConnectionSuccessful) {
                attempt++;
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();
                    createNotificationChannel("Connect call successful");

                    isConnectionSuccessful = true;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PandamicService.this, "Connect call successful", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                   /* if (mHandler != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MyService.this, "Connect failed.Please try again ", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }*/
                }
            }
            isConnectionSuccessful = false;
            connected(mmSocket,true);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    private void createNotificationChannel(String message) {
        Log.i(TAG, "createNotificationChannel " + message);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = message;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("pandamic", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Intent intent = new Intent(this, PandamicActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("pandamic")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message))
                    .setAutoCancel(true);
            notificationManager.notify(1, builder.build());
        }
    }
}

