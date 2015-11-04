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

package com.thoughtworks.jdamore.bluetooth4LE;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DeviceScanActivity extends Activity {

    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLeScanner;
    private ScanCallback mScanCallback;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private boolean mScanning;
    private Handler mHandler;
    private ArrayList<BluetoothDevice> mLeDevices;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public static RelativeLayout connectingLayout;
    List<String> serviceUUIDs;
    String fatigueMonitorUUIDString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        fatigueMonitorUUIDString = SampleGattAttributes.FATIGUE_LEVEL_SERVICE;
        serviceUUIDs = new ArrayList<>();
        serviceUUIDs.add(fatigueMonitorUUIDString);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        initScanCallback();

        scanLeDevice(true);
    }

    private void initScanCallback(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initCallbackLollipop();
        } else {
            initScanCallbackSupport();
        }
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
    }

    private void scanLeDevice(final boolean enable) {
        UUID fatigueMonitorUUID = UUID.fromString(SampleGattAttributes.FATIGUE_LEVEL_SERVICE);
        UUID[] serviceUUIDs = { fatigueMonitorUUID };
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                    mScanning = false;
                    finish();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            startScanWithServices(serviceUUIDs);
        } else {
            stopScan();
            mScanning = false;
            finish();
        }
    }

    private void initScanCallbackSupport(){
        if(mLeScanCallback != null) return;
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "22222 mLeDevices.add(device)");
                        mLeDevices.clear();
                        mLeDevices.add(device);
                        connectToFatigueDevice();
                    }
                });
            }
        };
    }

    public void connectToFatigueDevice() {
        if (mLeDevices == null) return;
        final Intent intent = new Intent(this, DisplayEmoticonActivity.class);
        intent.putExtra(DisplayEmoticonActivity.EXTRAS_DEVICE_NAME, mLeDevices.get(0).getName());
        intent.putExtra(DisplayEmoticonActivity.EXTRAS_DEVICE_ADDRESS, mLeDevices.get(0).getAddress());
        Log.d(TAG, "address22222" + mLeDevices.get(0).getName() + mLeDevices.get(0).getAddress());
        if (mScanning) {
            stopScan();
            mScanning = false;
        }
        startActivity(intent);
    }

    private void setOnTouchListenerForLayout(RelativeLayout layout) {
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                finish();
                return true;//always return true to consume event
            }
        });
    }

    public void startScanWithServices(UUID[] uuidFilters){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanLollipop(fatigueMonitorUUIDString);
        }else{
            scanSupport(uuidFilters);
        }

        if(BuildConfig.DEBUG) Log.d(TAG, "BLE Scan started");
    }

    private void scanSupport(UUID[] uuidFilters){
        if(mLeScanCallback == null)
            initScanCallbackSupport();

        //start scan
        boolean success = mBluetoothAdapter.startLeScan(uuidFilters, mLeScanCallback);
//        boolean success = mBluetoothAdapter.startLeScan(mLeScanCallback);

        //check scan success
        if(!success) {
            if(BuildConfig.DEBUG) Log.d(TAG, "BLE Scan failed");
        }
    }

    public void stopScan(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Stop scan and flush pending scan
            mBluetoothAdapter.getBluetoothLeScanner().flushPendingScanResults(mScanCallback);
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        }else{
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        if(BuildConfig.DEBUG) Log.d(TAG, "BLE Scan stopped");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initCallbackLollipop(){
        if(mScanCallback != null) return;
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                Log.d(TAG, "mLeDevices.add(device)");
                mLeDevices.clear();
                mLeDevices.add(result.getDevice());
                connectToFatigueDevice();
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(TAG, "Batch scan results: ");
                for (ScanResult result: results){
                    Log.d(TAG, "Batch scan result: " + result.toString());
                    //Do whatever you want
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(TAG, "Scan failed");
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scanLollipop(String uuidString){
        if(mScanCallback == null) initCallbackLollipop();

        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(uuidString))).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // or BALANCED previously
                .setReportDelay(0)
                .build();
        if(mBluetoothAdapter == null) {
            Log.d(TAG, "mBluetoothAdapterNull 3456");
        }
        mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, mScanCallback);
    }
}