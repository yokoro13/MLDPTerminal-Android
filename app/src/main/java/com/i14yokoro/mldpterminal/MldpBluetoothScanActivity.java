/*
 * Copyright (C) 2015 Microchip Technology Inc. and its subsidiaries.  You may use this software and any derivatives
 * exclusively with Microchip products.
 *
 * THIS SOFTWARE IS SUPPLIED BY MICROCHIP "AS IS".  NO WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY, APPLY TO THIS
 * SOFTWARE, INCLUDING ANY IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY, AND FITNESS FOR A PARTICULAR
 * PURPOSE, OR ITS INTERACTION WITH MICROCHIP PRODUCTS, COMBINATION WITH ANY OTHER PRODUCTS, OR USE IN ANY APPLICATION.
 *
 * IN NO EVENT WILL MICROCHIP BE LIABLE FOR ANY INDIRECT, SPECIAL, PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSS, DAMAGE,
 * COST OR EXPENSE OF ANY KIND WHATSOEVER RELATED TO THE SOFTWARE, HOWEVER CAUSED, EVEN IF MICROCHIP HAS BEEN ADVISED OF
 * THE POSSIBILITY OR THE DAMAGES ARE FORESEEABLE.  TO THE FULLEST EXTENT ALLOWED BY LAW, MICROCHIP'S TOTAL LIABILITY ON
 * ALL CLAIMS IN ANY WAY RELATED TO THIS SOFTWARE WILL NOT EXCEED THE AMOUNT OF FEES, IF ANY, THAT YOU HAVE PAID
 * DIRECTLY TO MICROCHIP FOR THIS SOFTWARE.
 *
 * MICROCHIP PROVIDES THIS SOFTWARE CONDITIONALLY UPON YOUR ACCEPTANCE OF THESE TERMS.
 */

package com.i14yokoro.mldpterminal;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ListView;

import com.i14yokoro.tecterminal.R;

/**
 * Activity for scanning and displaying available Bluetooth LE devices
 */
public class MldpBluetoothScanActivity extends ListActivity {
	private final static String TAG = "debug***";

    public static final String INTENT_EXTRA_SCAN_ADDRESS = "BLE_SCAN_DEVICE_ADDRESS";
    public static final String INTENT_EXTRA_SCAN_NAME = "BLE_SCAN_DEVICE_NAME";
    public static final String INTENT_EXTRA_SCAN_AUTO_CONNECT = "BLE_SCAN_AUTO_CONNECT";
    private static final int REQ_CODE_ENABLE_BT = 2;

    private static final long SCAN_TIME = 10000;
    private Handler scanStopHandler;

    private MldpBluetoothService bleService;
    private DeviceListAdapter bleDeviceListAdapter;
    private boolean areScanning;
    private CheckBox alwaysConnectCheckBox;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_list_screen);//Show the screen
        setProgressBarIndeterminate(true);
        setProgressBarIndeterminateVisibility(true);
        alwaysConnectCheckBox = (CheckBox) findViewById(R.id.alwaysConnectCheckBox);

        Intent bleServiceIntent = new Intent(this, MldpBluetoothService.class);
        this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);
        scanStopHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
		bleDeviceListAdapter = new DeviceListAdapter(this, R.layout.scan_list_item);
        setListAdapter(bleDeviceListAdapter);
        if(bleService != null) {
            scanStart();
        }

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MldpBluetoothService.ACTION_BLE_SCAN_RESULT);
        registerReceiver (bleServiceReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bleService != null) {
            scanStopHandler.removeCallbacks(stopScan);
            scanStop();
        }
        unregisterReceiver(bleServiceReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
            unbindService(bleServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_activity_menu, menu);
        if (areScanning) {
            menu.findItem(R.id.menu_scan).setVisible(false);
        } else {
            menu.findItem(R.id.menu_scan).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_scan:
                scanStart();
	            break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }

    private final ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MldpBluetoothService.LocalBinder binder = (MldpBluetoothService.LocalBinder) service;
            bleService = binder.getService();
            scanStart();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MldpBluetoothService.ACTION_BLE_SCAN_RESULT.equals(action)) {
                Log.d(TAG, "Scan scan result received");
                final BleDevice device = new BleDevice(intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_ADDRESS), intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_NAME)); //Create new item to hold name and address
                if (device.getName() != null && device.getName().contains("RN")) {
                    bleDeviceListAdapter.addDevice(device);
                    bleDeviceListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BleDevice device = bleDeviceListAdapter.getDevice(position);
        scanStopHandler.removeCallbacks(stopScan);
        scanStop();
        final Intent intent = new Intent();
        if (device == null) {
            setResult(Activity.RESULT_CANCELED, intent);
        }
        else {
            intent.putExtra(INTENT_EXTRA_SCAN_AUTO_CONNECT, alwaysConnectCheckBox.isChecked());
            intent.putExtra(INTENT_EXTRA_SCAN_NAME, device.getName());
            intent.putExtra(INTENT_EXTRA_SCAN_ADDRESS, device.getAddress());
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void scanStart() {
        if (!areScanning) {
            if (bleService.isBluetoothRadioEnabled()) {
                bleDeviceListAdapter.clear();
                areScanning = true;
                setProgressBarIndeterminateVisibility(true);
                invalidateOptionsMenu();
                bleService.scanStart();
                scanStopHandler.postDelayed(stopScan, SCAN_TIME);
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);
                Log.d(TAG, "Requesting user to enable Bluetooth radio");
            }
        }
    }

    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            scanStop();
        }
    };

    private void scanStop() {
        if (areScanning) {
            bleService.scanStop();
            areScanning = false;
            setProgressBarIndeterminateVisibility(false);
            invalidateOptionsMenu();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQ_CODE_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                scanStart();
            }
            else {
                onBackPressed();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }
}