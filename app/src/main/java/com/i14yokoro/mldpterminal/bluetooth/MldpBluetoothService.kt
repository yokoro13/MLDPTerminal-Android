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

package com.i14yokoro.mldpterminal.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log

import java.util.LinkedList
import java.util.UUID

class MldpBluetoothService : Service() {
    private val descriptorWriteQueue = LinkedList<BluetoothGattDescriptor>()
    private val characteristicWriteQueue = LinkedList<BluetoothGattCharacteristic>()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var mldpDataCharacteristic: BluetoothGattCharacteristic? = null
    private var transparentRxDataCharacteristic: BluetoothGattCharacteristic? = null

    private var connectionAttemptCountdown = 0

    private val bleGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectionAttemptCountdown = 0
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        val intent = Intent(ACTION_BLE_CONNECTED)
                        sendBroadcast(intent)
                        Log.i(TAG, "Connected to BLE device")
                        descriptorWriteQueue.clear()
                        characteristicWriteQueue.clear()
                        bluetoothGatt!!.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        val intent = Intent(ACTION_BLE_DISCONNECTED)
                        sendBroadcast(intent)
                        Log.i(TAG, "Disconnected from BLE device")
                    }
                } else {
                    if (connectionAttemptCountdown-- > 0) {
                        gatt.connect()
                        Log.d(TAG, "Connection attempt failed, trying again")
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        val intent = Intent(ACTION_BLE_DISCONNECTED)
                        sendBroadcast(intent)
                        Log.i(TAG, "Unexpectedly disconnected from BLE device")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                transparentRxDataCharacteristic = null
                mldpDataCharacteristic = transparentRxDataCharacteristic
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val gattServices = gatt.services
                    if (gattServices == null) {
                        Log.d(TAG, "No BLE services found")
                        return
                    }
                    searchService(gattServices)
                } else {
                    Log.w(TAG, "Failed service discovery with status: $status")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            }

        }

        private fun searchService(gattServices: List<BluetoothGattService>) {
            var uuid: UUID
            for (gattService in gattServices) {
                uuid = gattService.uuid
                if (uuid == UUID_MLDP_PRIVATE_SERVICE || uuid == UUID_TANSPARENT_PRIVATE_SERVICE) {
                    val gattCharacteristics = gattService.characteristics
                    searchCharacteristic(gattCharacteristics)
                    break
                }
            }
            if (mldpDataCharacteristic == null || transparentRxDataCharacteristic == null) {
                Log.d(TAG, "Did not find MLDP or Transparent service")
            }
        }

        private fun searchCharacteristic(gattCharacteristics: List<BluetoothGattCharacteristic>) {
            var uuid: UUID
            for (gattCharacteristic in gattCharacteristics) {
                uuid = gattCharacteristic.uuid
                if (uuid == UUID_TRANSPARENT_RX_PRIVATE_CHAR) {
                    transparentRxDataCharacteristic = gattCharacteristic
                    val characteristicProperties = gattCharacteristic.properties
                    if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
                        gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    }
                    Log.d(TAG, "Found Transparent service Rx characteristics")
                }
                if (uuid == UUID_MLDP_DATA_PRIVATE_CHAR || uuid == UUID_TRANSPARENT_TX_PRIVATE_CHAR) {
                    mldpDataCharacteristic = gattCharacteristic
                    val characteristicProperties = gattCharacteristic.properties
                    if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                        bluetoothGatt!!.setCharacteristicNotification(gattCharacteristic, true)
                        val descriptor = gattCharacteristic.getDescriptor(UUID_CHAR_NOTIFICATION_DESCRIPTOR)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        descriptorWriteQueue.add(descriptor)
                        if (descriptorWriteQueue.size == 1) {
                            bluetoothGatt!!.writeDescriptor(descriptor)
                        }
                    }
                    if (characteristicProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
                        gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    }

                    Log.d(TAG, "Found MLDP service and characteristics")
                }
            }
        }

        //MLDPから文字を受けとったことをActivityに通知
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            try {
                if (UUID_MLDP_DATA_PRIVATE_CHAR == characteristic.uuid || UUID_TRANSPARENT_TX_PRIVATE_CHAR == characteristic.uuid) {
                    val dataValue = characteristic.getStringValue(0)
                    //byte[] dataValue = characteristic.getValue();
                    Log.d(TAG, "New notification or indication")
                    val intent = Intent(ACTION_BLE_DATA_RECEIVED)
                    intent.putExtra(INTENT_EXTRA_SERVICE_DATA, dataValue)
                    sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            }

        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Error writing GATT characteristic with status: $status")
                }
                characteristicWriteQueue.remove()
                if (characteristicWriteQueue.size > 0) {
                    bluetoothGatt!!.writeCharacteristic(characteristicWriteQueue.element())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            }

        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Error writing GATT descriptor with status: $status")
                }
                descriptorWriteQueue.remove()
                if (descriptorWriteQueue.size > 0) {
                    bluetoothGatt!!.writeDescriptor(descriptorWriteQueue.element())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            }

        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {}

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {}

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {}

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {}
    }

    val isBluetoothRadioEnabled: Boolean
        get() {
            try {
                if (bluetoothAdapter != null) {
                    if (bluetoothAdapter!!.isEnabled) {
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            }

            return false
        }

    // 新しいデバイスが見つかったとき呼ばれる
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "onScanResult")
            super.onScanResult(callbackType, result)
            if (Build.VERSION.SDK_INT >= 21) {
                val intent = Intent(ACTION_BLE_SCAN_RESULT)
                intent.putExtra(INTENT_EXTRA_SERVICE_ADDRESS, result.device.address)
                intent.putExtra(INTENT_EXTRA_SERVICE_NAME, result.device.name)
                sendBroadcast(intent)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed Error!")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return LocalBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            Log.d(TAG, "set BluetoothManager")
            Log.d(TAG, "set BluetoothAdapter")
            bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                Log.d(TAG, "BluetoothAdapter is null")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

    }

    override fun onDestroy() {
        try {
            if (bluetoothGatt != null) {
                bluetoothGatt!!.close()
                bluetoothGatt = null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        val service: MldpBluetoothService
            get() = this@MldpBluetoothService
    }

    fun scanStart() {
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
        if (bluetoothLeScanner != null) {
            Log.d(TAG, "bluetoothLeScanner is not null")
        }
        try {
            if (Build.VERSION.SDK_INT >= 21) { //Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "scan start in serviceActivity")

                bluetoothLeScanner!!.startScan(bleScanCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

    }


    fun scanStop() {
        try {
            bluetoothLeScanner!!.stopScan(bleScanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

    }

    // 接続処理
    fun connect(address: String?): Boolean {
        try {
            if (bluetoothAdapter == null || address == null) {
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address")
                return false
            }

            val bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
            if (bluetoothDevice == null) {
                Log.w(TAG, "Unable to connect because device was not found")
                return false
            }
            if (bluetoothGatt != null) {
                bluetoothGatt!!.close()
            }
            connectionAttemptCountdown = 3
            bluetoothGatt = bluetoothDevice.connectGatt(this, false, bleGattCallback)
            Log.i(TAG, "Attempting to create a new Bluetooth connection")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
            return false
        }

    }

    // 切断処理
    fun disconnect() {
        try {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized")
                return
            }
            connectionAttemptCountdown = 0
            bluetoothGatt!!.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

    }

    // MLDPに文字列を送信
    fun writeMLDP(string: String) {
        try {
            val writeDataCharacteristic: BluetoothGattCharacteristic? = if (mldpDataCharacteristic != null) {
                mldpDataCharacteristic
            } else {
                transparentRxDataCharacteristic
            }
            if (bluetoothAdapter == null || bluetoothGatt == null || writeDataCharacteristic == null) {
                Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected")
                return
            }
            writeDataCharacteristic.setValue(string)
            characteristicWriteQueue.add(writeDataCharacteristic)
            if (characteristicWriteQueue.size == 1) {
                if (!bluetoothGatt!!.writeCharacteristic(writeDataCharacteristic)) {
                    Log.w(TAG, "Failed to write characteristic")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

    }

    // byte型の送信
    fun writeMLDP(byteValues: ByteArray) {
        try {
            val writeDataCharacteristic: BluetoothGattCharacteristic? = if (mldpDataCharacteristic != null) {
                mldpDataCharacteristic
            } else {
                transparentRxDataCharacteristic
            }
            if (bluetoothAdapter == null || bluetoothGatt == null || writeDataCharacteristic == null) {
                Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected")
                return
            }
            writeDataCharacteristic.value = byteValues
            characteristicWriteQueue.add(writeDataCharacteristic)
            if (characteristicWriteQueue.size == 1) {
                if (!bluetoothGatt!!.writeCharacteristic(writeDataCharacteristic)) {
                    Log.w(TAG, "Failed to write characteristic")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Oops, exception caught in " + e.stackTrace[0].methodName + ": " + e.message)
        }

    }

    companion object {

        private const val TAG = "debug***"

        const val INTENT_EXTRA_SERVICE_ADDRESS = "BLE_SERVICE_DEVICE_ADDRESS"
        const val INTENT_EXTRA_SERVICE_NAME = "BLE_SERVICE_DEVICE_NAME"
        const val INTENT_EXTRA_SERVICE_DATA = "BLE_SERVICE_DATA"

        const val ACTION_BLE_REQ_ENABLE_BT = "ACTION_BLE_REQ_ENABLE_BT"
        const val ACTION_BLE_SCAN_RESULT = "ACTION_BLE_SCAN_RESULT"
        const val ACTION_BLE_CONNECTED = "ACTION_BLE_CONNECTED"
        const val ACTION_BLE_DISCONNECTED = "ACTION_BLE_DISCONNECTED"
        const val ACTION_BLE_DATA_RECEIVED = "ACTION_BLE_DATA_RECEIVED"

        private val UUID_MLDP_PRIVATE_SERVICE = UUID.fromString("00035b03-58e6-07dd-021a-08123a000300")
        private val UUID_MLDP_DATA_PRIVATE_CHAR = UUID.fromString("00035b03-58e6-07dd-021a-08123a000301")

        private val UUID_TANSPARENT_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
        private val UUID_TRANSPARENT_TX_PRIVATE_CHAR = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
        private val UUID_TRANSPARENT_RX_PRIVATE_CHAR = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3")

        private val UUID_CHAR_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
