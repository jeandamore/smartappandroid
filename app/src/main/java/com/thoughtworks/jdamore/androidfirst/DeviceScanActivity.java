/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.jdamore.androidfirst;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity {

    private final static String TAG = DisplayEmoticonActivity.class.getSimpleName();

    //    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothDevice mLeDevice;
    private ArrayList<BluetoothDevice> mLeDevices;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

//    private ImageView incrementingBoxView;
//    private AnimationDrawable myAnimationDrawable;

    public static RelativeLayout connectingLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setTitle(R.string.title_devices);
        mLeDevices = new ArrayList<BluetoothDevice>();
        mHandler = new Handler();

        setContentView(R.layout.connecting);

        connectingLayout = (RelativeLayout) findViewById(R.id.connecting);
        setOnTouchListenerForLayout(connectingLayout);

        Typeface futuraFont = Typeface.createFromAsset(getAssets(),
                "fonts/Futura.ttc");
        Typeface myriadFont = Typeface.createFromAsset(getAssets(), "fonts/MyriadPro_Regular.otf");

        TextView smartCapTextView = (TextView) findViewById(R.id.smartCapText);
        TextView connectingText = (TextView) findViewById(R.id.connectingText);
        smartCapTextView.setTypeface(futuraFont);
        connectingText.setTypeface(myriadFont);

//        incrementingBoxView = (ImageView) findViewById(R.id.incrementingBoxView);
//        incrementingBoxView.setVisibility(View.VISIBLE);
//        incrementalHorizontalLoading();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        //mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        UUID fatigueMonitorUUID = UUID.fromString(SampleGattAttributes.FATIGUE_LEVEL_SERVICE);
        UUID[] serviceUUIDs = { fatigueMonitorUUID };
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    finish();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(serviceUUIDs, mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            finish();
        }
    }

    public void connectToFatigueDevice() {
        if (mLeDevices == null) return;
        final Intent intent = new Intent(this, DisplayEmoticonActivity.class);
        intent.putExtra(DisplayEmoticonActivity.EXTRAS_DEVICE_NAME, mLeDevices.get(0).getName());
        intent.putExtra(DisplayEmoticonActivity.EXTRAS_DEVICE_ADDRESS, mLeDevices.get(0).getAddress());
        //Log.d(TAG, "address22222" + mLeDevice.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "42940236 mLeDevices.add(device)");
                    mLeDevices.clear();
                    mLeDevices.add(device);

                    connectToFatigueDevice();
                }
            });

        }
    };

//    public void incrementalHorizontalLoading() {
//        myAnimationDrawable = (AnimationDrawable) incrementingBoxView.getDrawable();
//
//        myAnimationDrawable.start();
//    }

    private void setOnTouchListenerForLayout(RelativeLayout layout) {
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                finish();
                return true;//always return true to consume event
            }
        });
    }
}