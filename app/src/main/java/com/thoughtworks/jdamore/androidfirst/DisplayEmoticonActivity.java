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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

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
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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
            Log.d(TAG, "broadcastreceiverCalled");
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                if(DeviceScanActivity.ismScanning()) {
//                    setContentView(R.layout.connecting);
//                    connectingLayout = (RelativeLayout) findViewById(R.id.connecting);
//                    setOnTouchListenerForLayout(connectingLayout);
//                } else {
//                    finish();
//                }
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                finish();
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                setGattServices(mBluetoothLeService.getSupportedGattServices());
                readFatigueLevelCharacteristic();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
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

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void setView(String data) {
        if(data != null) {
            switch (data) {
                //Hexadecimal 0002
                case "2":
                    setContentView(R.layout.happy_face);
                    currentLayout = (FrameLayout) findViewById(R.id.happy_face);
                    break;
                case "3":
                    setContentView(R.layout.normal_face);
                    currentLayout = (FrameLayout) findViewById(R.id.normal_face);
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
                    break;
                case "6":
                    setContentView(R.layout.poorly_fitted_cap);
                    break;
                case "7":
                    setContentView(R.layout.no_comms);
                    break;
                case "8":
                    setContentView(R.layout.offline);
                    break;
                case "9":
                    setContentView(R.layout.unassigned);
                    break;
                //Hexadecimal 000A
                case "10":
                    setContentView(R.layout.cap_off);
                    break;
                default:
                    finish();
                    break;
            }
        }
        setOnTouchListenerForLayout(currentLayout);
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

    private void setOnTouchListenerForLayout(RelativeLayout layout) {
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Log.d(TAG, "onTouch yay");
                finish();
                return true;//always return true to consume event
            }
        });
    }

    private void setOnTouchListenerForLayout(FrameLayout layout) {
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Log.d(TAG, "onTouch yay");
                finish();
                return true;//always return true to consume event
            }
        });
    }
}
