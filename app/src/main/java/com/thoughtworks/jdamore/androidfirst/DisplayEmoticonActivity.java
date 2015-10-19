package com.thoughtworks.jdamore.androidfirst;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DisplayEmoticonActivity extends Activity {
    private final static String TAG = DisplayEmoticonActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public static FrameLayout currentLayout;
    public static RelativeLayout connectingLayout;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "22222 Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d(TAG, "22222 onServiceConnected");
            mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "22222 mBluetoothLeService.connect");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //mBluetoothLeService.disconnect();
            Log.d(TAG, "22222 mBluetoothLeService.disconnect");
            mBluetoothLeService = null;
            finish();
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "22222 broadcastreceiverCalled");
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "22222 ACTION_GATT_DISCONNECTED");

                finish();
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.d(TAG, "22222 ACTION_GATT_SERVICES_DISCOVERED");
                setGattServices(mBluetoothLeService.getSupportedGattServices());
                readFatigueLevelCharacteristic();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "22222 ACTION_DATA_AVAILABLE");
                setView(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void readFatigueLevelCharacteristic() {
        if (mGattCharacteristics != null) {
            // Connects to the Fatigue Service characteristic.
            final BluetoothGattCharacteristic characteristic = getGattCharacteristic(SampleGattAttributes.FATIGUE_LEVEL, mGattCharacteristics);

            final int charaProp = characteristic.getProperties();
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, true);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "22222 onCreate called");

        setContentView(R.layout.connecting2);
        connectingLayout = (RelativeLayout) findViewById(R.id.connecting2);
        setOnTouchListenerForLayout(connectingLayout);

        Typeface futuraFont = Typeface.createFromAsset(getAssets(),
                "fonts/Futura.ttc");
        Typeface myriadFont = Typeface.createFromAsset(getAssets(), "fonts/MyriadPro_Regular.otf");

        TextView smartCapTextView = (TextView) findViewById(R.id.smartCapText);
        TextView connectingText = (TextView) findViewById(R.id.connectingText);
        smartCapTextView.setTypeface(futuraFont);
        connectingText.setTypeface(myriadFont);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Log.d(TAG, "22222 Intentaddress" + mDeviceAddress);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "22222 onResume called + registerReceiver");

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "22222 Connect request result=" + result);
            Log.d(TAG, "22222 mBluetoothLeService not null");
        } else {
            Log.d(TAG, "22222 mBluetoothLeService null");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "22222 onPause called + unregisterReceiver");
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "22222 onDestroy close called + unbindService mBluetoothLeService = null");
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void setView(String data) {
        if(data != null) {
            switch (data) {
                //Hexadecimal 0002
                case "1":
                    setContentView(R.layout.happy_face);
                    currentLayout = (FrameLayout) findViewById(R.id.happy_face);
                    break;
                case "2":
                    setContentView(R.layout.normal_face);
                    currentLayout = (FrameLayout) findViewById(R.id.normal_face);
                    break;
                case "3":
                    setContentView(R.layout.sleepier_face);
                    currentLayout = (FrameLayout) findViewById(R.id.sleepier_face);
                    break;
                case "4":
                    setContentView(R.layout.tired_face);
                    currentLayout = (FrameLayout) findViewById(R.id.tired_face);
                    final MediaPlayer mpBeep = MediaPlayer.create(getApplicationContext(), R.raw.screaming_goat);
                    final MediaPlayer mpVoice = MediaPlayer.create(getApplicationContext(), R.raw.voice);
                    mpBeep.setVolume(0.8f, 0.8f);
                    mpBeep.setNextMediaPlayer(mpVoice);
                    mpVoice.setVolume(1.0f, 1.0f);
                    mpVoice.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mpBeep.release();
                            mpVoice.release();
                        }
                    });
                    mpBeep.start();
                    break;
                case "5":
                    setContentView(R.layout.no_cap_connected);
                    currentLayout = (FrameLayout) findViewById(R.id.no_cap_connected);
                    break;
                case "6":
                    setContentView(R.layout.poorly_fitted_cap);
                    currentLayout = (FrameLayout) findViewById(R.id.poorly_fitted_cap);
                    break;
                case "7":
                    setContentView(R.layout.no_comms);
                    currentLayout = (FrameLayout) findViewById(R.id.no_comms);
                    break;
                case "8":
                    setContentView(R.layout.offline);
                    currentLayout = (FrameLayout) findViewById(R.id.offline);
                    break;
                case "9":
                    setContentView(R.layout.unassigned);
                    currentLayout = (FrameLayout) findViewById(R.id.unassigned);
                    break;
                //Hexadecimal 000A
                case "10":
                    setContentView(R.layout.cap_off);
                    currentLayout = (FrameLayout) findViewById(R.id.cap_off);
                    break;
                case "11":
                    setContentView(R.layout.dial1);
                    currentLayout = (FrameLayout) findViewById(R.id.dial1);
                    break;
                case "12":
                    setContentView(R.layout.dial2);
                    currentLayout = (FrameLayout) findViewById(R.id.dial1);
                    break;
                default:
                    finish();
                    break;
            }
            if(currentLayout != null) setOnTouchListenerForLayout(currentLayout);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void setGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private BluetoothGattCharacteristic getGattCharacteristic(String characteristicUUID, ArrayList<ArrayList<BluetoothGattCharacteristic>> characteristicList) {
        for(int i = 0; i < characteristicList.size(); i++) {
            BluetoothGattCharacteristic currentCharacteristic = characteristicList.get(i).get(0);
            if(currentCharacteristic.getUuid().toString().equals(characteristicUUID)) {
                return currentCharacteristic;
            }
        }
        return null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void setOnTouchListenerForLayout(FrameLayout layout) {
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Log.d(TAG, "onTouch yay 22222");
                finish();
                return true;//always return true to consume event
            }
        });
    }

    private void setOnTouchListenerForLayout(RelativeLayout layout) {
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Log.d(TAG, "onTouch yay 22222");
                finish();
                return true;//always return true to consume event
            }
        });
    }
}
