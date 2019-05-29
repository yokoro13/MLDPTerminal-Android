package com.i14yokoro.mldpterminal

import android.app.Activity
import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.ListView

class MldpBluetoothScanActivity : ListActivity() {
    private var scanStopHandler: Handler? = null

    private var bleService: MldpBluetoothService? = null
    private var bleDeviceListAdapter: DeviceListAdapter? = null
    private var areScanning: Boolean = false
    private var alwaysConnectCheckBox: CheckBox? = null

    private val bleServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            val binder = service as MldpBluetoothService.LocalBinder
            bleService = binder.service
            scanStart()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bleService = null
        }
    }

    private val bleServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (MldpBluetoothService.ACTION_BLE_SCAN_RESULT == action) {
                Log.d(TAG, "Scan scan result received")
                val device = BleDevice(intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_ADDRESS),
                        intent.getStringExtra(MldpBluetoothService.INTENT_EXTRA_SERVICE_NAME))
                if (device.name.contains("RN")) {
                    bleDeviceListAdapter!!.addDevice(device)
                    bleDeviceListAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    private val stopScan = Runnable { this.scanStop() }


    public override fun onCreate(savedInstanceState: Bundle?) {
        // requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_list_screen)//Show the screen
        // setProgressBarIndeterminate(true)
        // setProgressBarIndeterminateVisibility(true)
        alwaysConnectCheckBox = findViewById<View>(R.id.alwaysConnectCheckBox) as CheckBox

        val bleServiceIntent = Intent(this, MldpBluetoothService::class.java)
        this.bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE)
        scanStopHandler = Handler()
    }

    override fun onResume() {
        super.onResume()
        bleDeviceListAdapter = DeviceListAdapter(this, R.layout.scan_list_item)
        listAdapter = bleDeviceListAdapter
        if (bleService != null) {
            scanStart()
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(MldpBluetoothService.ACTION_BLE_SCAN_RESULT)
        registerReceiver(bleServiceReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        if (bleService != null) {
            scanStopHandler!!.removeCallbacks(stopScan)
            scanStop()
        }
        unregisterReceiver(bleServiceReceiver)
    }

    public override fun onStop() {
        super.onStop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        unbindService(bleServiceConnection)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scan_activity_menu, menu)
        menu.findItem(R.id.menu_scan).isVisible = !areScanning
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> scanStart()
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return true
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = bleDeviceListAdapter!!.getDevice(position)
        scanStopHandler!!.removeCallbacks(stopScan)
        scanStop()
        val intent = Intent()
        intent.putExtra(INTENT_EXTRA_SCAN_AUTO_CONNECT, alwaysConnectCheckBox!!.isChecked)
        intent.putExtra(INTENT_EXTRA_SCAN_NAME, device.name)
        intent.putExtra(INTENT_EXTRA_SCAN_ADDRESS, device.address)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun scanStart() {
        if (!areScanning) {
            if (bleService!!.isBluetoothRadioEnabled) {
                bleDeviceListAdapter!!.clear()
                areScanning = true
                // setProgressBarIndeterminateVisibility(true)
                invalidateOptionsMenu()
                bleService!!.scanStart()
                scanStopHandler!!.postDelayed(stopScan, SCAN_TIME)
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT)
                Log.d(TAG, "Requesting user to enable Bluetooth radio")
            }
        }
    }

    private fun scanStop() {
        if (areScanning) {
            bleService!!.scanStop()
            areScanning = false
            // setProgressBarIndeterminateVisibility(false)
            invalidateOptionsMenu()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode == REQ_CODE_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                scanStart()
            } else {
                onBackPressed()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    companion object {
        private const val TAG = "debug***"

        const val INTENT_EXTRA_SCAN_ADDRESS = "BLE_SCAN_DEVICE_ADDRESS"
        const val INTENT_EXTRA_SCAN_NAME = "BLE_SCAN_DEVICE_NAME"
        const val INTENT_EXTRA_SCAN_AUTO_CONNECT = "BLE_SCAN_AUTO_CONNECT"
        private const val REQ_CODE_ENABLE_BT = 2

        private const val SCAN_TIME: Long = 10000
    }
}