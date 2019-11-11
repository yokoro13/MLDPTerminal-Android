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

package com.i14yokoro.mldpterminal.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class MldpBluetoothService extends Service {

    private final static String TAG = "debug***";

    public static final String INTENT_EXTRA_SERVICE_ADDRESS = "BLE_SERVICE_DEVICE_ADDRESS";
    public static final String INTENT_EXTRA_SERVICE_NAME = "BLE_SERVICE_DEVICE_NAME";
    public static final String INTENT_EXTRA_SERVICE_DATA = "BLE_SERVICE_DATA";

    public final static String ACTION_BLE_REQ_ENABLE_BT = "ACTION_BLE_REQ_ENABLE_BT";
    public final static String ACTION_BLE_SCAN_RESULT = "ACTION_BLE_SCAN_RESULT";
    public final static String ACTION_BLE_CONNECTED = "ACTION_BLE_CONNECTED";
    public final static String ACTION_BLE_DISCONNECTED = "ACTION_BLE_DISCONNECTED";
    public final static String ACTION_BLE_DATA_RECEIVED = "ACTION_BLE_DATA_RECEIVED";

    private final static UUID UUID_MLDP_PRIVATE_SERVICE = UUID.fromString("00035b03-58e6-07dd-021a-08123a000300");
    private final static UUID UUID_MLDP_DATA_PRIVATE_CHAR = UUID.fromString("00035b03-58e6-07dd-021a-08123a000301");

    private final static UUID UUID_TANSPARENT_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    private final static UUID UUID_TRANSPARENT_TX_PRIVATE_CHAR = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");
    private final static UUID UUID_TRANSPARENT_RX_PRIVATE_CHAR = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3");

    private final static UUID UUID_CHAR_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private final Queue<BluetoothGattCharacteristic> characteristicWriteQueue = new LinkedList<>();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic mldpDataCharacteristic, transparentRxDataCharacteristic;

    private int connectionAttemptCountdown = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate(){
        super.onCreate();
        try{
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            Log.d(TAG,"set BluetoothManager");
            if(bluetoothManager == null){
                Log.d(TAG, "BluetoothManager is null");
            }
            else {
                Log.d(TAG,"set BluetoothAdapter");
                bluetoothAdapter = bluetoothManager.getAdapter();
                if(bluetoothAdapter == null){
                    Log.d(TAG, "BluetoothAdapter is null");
                }
            }
        }
        catch (Exception e){
            Log.d(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    @Override
    public void onDestroy(){
        try{
            if(bluetoothGatt != null){
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
        }
        catch (Exception e){
            Log.d(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        super.onDestroy();
    }

    public class LocalBinder extends Binder{
        public MldpBluetoothService getService(){
            return MldpBluetoothService.this;
        }
    }

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectionAttemptCountdown = 0;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        final Intent intent = new Intent(ACTION_BLE_CONNECTED);
                        sendBroadcast(intent);
                        Log.i(TAG, "Connected to BLE device");
                        descriptorWriteQueue.clear();
                        characteristicWriteQueue.clear();
                        bluetoothGatt.discoverServices();
                    }
                    else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        final Intent intent = new Intent(ACTION_BLE_DISCONNECTED);
                        sendBroadcast(intent);
                        Log.i(TAG, "Disconnected from BLE device");
                    }
                }
                else {
                    if (connectionAttemptCountdown-- > 0) {
                        gatt.connect();
                        Log.d(TAG, "Connection attempt failed, trying again");
                    }
                    else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        final Intent intent = new Intent(ACTION_BLE_DISCONNECTED);
                        sendBroadcast(intent);
                        Log.i(TAG, "Unexpectedly disconnected from BLE device");
                    }
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                mldpDataCharacteristic = transparentRxDataCharacteristic = null;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    List<BluetoothGattService> gattServices = gatt.getServices();
                    if (gattServices == null) {
                        Log.d(TAG, "No BLE services found");
                        return;
                    }
                    searchService(gattServices);
                }
                else {
                    Log.w(TAG, "Failed service discovery with status: " + status);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        private void searchService(List<BluetoothGattService> gattServices){
            UUID uuid;
            for (BluetoothGattService gattService : gattServices) {
                uuid = gattService.getUuid();
                if (uuid.equals(UUID_MLDP_PRIVATE_SERVICE) || uuid.equals(UUID_TANSPARENT_PRIVATE_SERVICE)) {
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    searchCharacteristic(gattCharacteristics);
                    break;
                }
            }
            if(mldpDataCharacteristic == null || transparentRxDataCharacteristic == null) {
                Log.d(TAG, "Did not find MLDP or Transparent service");
            }
        }

        private void searchCharacteristic(List<BluetoothGattCharacteristic> gattCharacteristics){
            UUID uuid;
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid();
                if (uuid.equals(UUID_TRANSPARENT_RX_PRIVATE_CHAR)) {
                    transparentRxDataCharacteristic = gattCharacteristic;
                    final int characteristicProperties = gattCharacteristic.getProperties();
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }
                    Log.d(TAG, "Found Transparent service Rx characteristics");
                }
                if (uuid.equals(UUID_MLDP_DATA_PRIVATE_CHAR) || uuid.equals(UUID_TRANSPARENT_TX_PRIVATE_CHAR)) {
                    mldpDataCharacteristic = gattCharacteristic;
                    final int characteristicProperties = gattCharacteristic.getProperties();
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) {
                        bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID_CHAR_NOTIFICATION_DESCRIPTOR);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        descriptorWriteQueue.add(descriptor);
                        if(descriptorWriteQueue.size() == 1) {
                            bluetoothGatt.writeDescriptor(descriptor);
                        }
                    }
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }

                    Log.d(TAG, "Found MLDP service and characteristics");
                }
            }
        }

        //MLDPから文字を受けとったことをActivityに通知
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                if (UUID_MLDP_DATA_PRIVATE_CHAR.equals(characteristic.getUuid()) || UUID_TRANSPARENT_TX_PRIVATE_CHAR.equals(characteristic.getUuid())) {
                    String dataValue = characteristic.getStringValue(0);
                    //byte[] dataValue = characteristic.getValue();
                    Log.d(TAG, "New notification or indication");
                    final Intent intent = new Intent(ACTION_BLE_DATA_RECEIVED);
                    intent.putExtra(INTENT_EXTRA_SERVICE_DATA, dataValue);
                    sendBroadcast(intent);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Error writing GATT characteristic with status: " + status);
                }
                characteristicWriteQueue.remove();
                if(characteristicWriteQueue.size() > 0) {
                    bluetoothGatt.writeCharacteristic(characteristicWriteQueue.element());
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Error writing GATT descriptor with status: " + status);
                }
                descriptorWriteQueue.remove();
                if(descriptorWriteQueue.size() > 0) {
                    bluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }
    };

    public boolean isBluetoothRadioEnabled() {
        try {
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled()) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return false;
    }

    public void scanStart() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if(bluetoothLeScanner != null){
            Log.d(TAG,"bluetoothLeScanner is not null");
        }
        try {
            if (Build.VERSION.SDK_INT >= 21) { //Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "scan start in serviceActivity");

                bluetoothLeScanner.startScan(bleScanCallback);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }


    public void scanStop() {
        try {
            bluetoothLeScanner.stopScan(bleScanCallback);
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // 接続処理
    public boolean connect(final String address) {
        try {
            if (bluetoothAdapter == null || address == null) {
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address");
                return false;
            }

            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            if (bluetoothDevice == null) {
                Log.w(TAG, "Unable to connect because device was not found");
                return false;
            }
            if (bluetoothGatt != null) {
                bluetoothGatt.close();
            }
            connectionAttemptCountdown = 3;
            bluetoothGatt = bluetoothDevice.connectGatt(this, false, bleGattCallback);
            Log.i(TAG, "Attempting to create a new Bluetooth connection");
            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            return false;
        }
    }

    // 切断処理
    public void disconnect() {
        try {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            connectionAttemptCountdown = 0;
            bluetoothGatt.disconnect();
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // MLDPに文字列を送信
    public void writeMLDP(String string) {
        try {
            BluetoothGattCharacteristic writeDataCharacteristic;
            if (mldpDataCharacteristic != null) {
                writeDataCharacteristic = mldpDataCharacteristic;
            }
            else {
                writeDataCharacteristic = transparentRxDataCharacteristic;
            }
            if (bluetoothAdapter == null || bluetoothGatt == null || writeDataCharacteristic == null) {
                Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected");
                return;
            }
            writeDataCharacteristic.setValue(string);
            characteristicWriteQueue.add(writeDataCharacteristic);
            if(characteristicWriteQueue.size() == 1){
                if (!bluetoothGatt.writeCharacteristic(writeDataCharacteristic)) {
                    Log.w(TAG, "Failed to write characteristic");
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // byte型の送信
    public void writeMLDP(byte[] byteValues) {
        try {
            BluetoothGattCharacteristic writeDataCharacteristic;
            if (mldpDataCharacteristic != null) {
                writeDataCharacteristic = mldpDataCharacteristic;
            }
            else {
                writeDataCharacteristic = transparentRxDataCharacteristic;
            }
            if (bluetoothAdapter == null || bluetoothGatt == null || writeDataCharacteristic == null) {
                Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected");
                return;
            }
            writeDataCharacteristic.setValue(byteValues);
            characteristicWriteQueue.add(writeDataCharacteristic);
            if(characteristicWriteQueue.size() == 1){
                if (!bluetoothGatt.writeCharacteristic(writeDataCharacteristic)) {
                    Log.w(TAG, "Failed to write characteristic");
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // 新しいデバイスが見つかったとき呼ばれる
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG,"onScanResult");
            super.onScanResult(callbackType, result);
            if (Build.VERSION.SDK_INT >= 21) {
                final Intent intent = new Intent(ACTION_BLE_SCAN_RESULT);
                intent.putExtra(INTENT_EXTRA_SERVICE_ADDRESS, result.getDevice().getAddress());
                intent.putExtra(INTENT_EXTRA_SERVICE_NAME, result.getDevice().getName());
                sendBroadcast(intent);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed Error!");
        }
    };
}
