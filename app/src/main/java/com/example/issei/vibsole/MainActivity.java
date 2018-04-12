package com.example.issei.vibsole;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter;
    final int REQUEST_ENABLE_BT = 1;
    final String APP_NAME = "Vibsole";

    private boolean btEnabled;

    private HashMap<String,BluetoothGatt> connectedDevices = new HashMap<>();
    private HashMap<String,BluetoothDevice> connectingDevices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(APP_NAME,"onCreate");

        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        if (btAdapter == null || !btAdapter.isEnabled()){
            btEnabled = false;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            btEnabled = true;
            scanLeDevice();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_ENABLE_BT:
                switch (resultCode){
                    case RESULT_OK:
                        btEnabled = true;
                        Log.i(APP_NAME,"Bluetooth is enabled.");
                        scanLeDevice();
                        break;
                    case RESULT_CANCELED:
                        btEnabled = false;
                        Log.e(APP_NAME,"Bluetooth cannot be enabled because user cancelled the request");
                }
        }
    }

    BluetoothLeScanner bleScanner;

    private void scanLeDevice(){
        ArrayList<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter bleFilter = new ScanFilter.Builder()
                .setDeviceName("MyESP32")
                .build();
        scanFilters.add(bleFilter);

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

        Log.i(APP_NAME,"BLE Device Scan started...");
        bleScanner = btAdapter.getBluetoothLeScanner();
        bleScanner.startScan(scanFilters,scanSettings,bleScanCallback);
//        bleScanner.startScan(bleScanCallback);
    }

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String text = result.getDevice().getAddress();
            Log.i(APP_NAME,"BLE Device found: " + text);

            if (!connectingDevices.containsKey(result.getDevice().getAddress()) & !connectedDevices.containsKey(result.getDevice().getAddress())){

                switch (result.getDevice().getBondState()){
                    case BluetoothDevice.BOND_BONDED:
                        Log.i(APP_NAME,"Connecting: " + result.getDevice().getAddress());

                        if (connectingDevices.size()<1){
                            connectingDevices.put(result.getDevice().getAddress(),result.getDevice());
                            result.getDevice().connectGatt(getApplicationContext(),false,new BleGattCallback());
                        }

                        break;

                    case BluetoothDevice.BOND_BONDING:
                        break;

                    case BluetoothDevice.BOND_NONE:
                        Log.i(APP_NAME,"Pairing started.");
                        result.getDevice().createBond();
                        break;

                    default:
                        break;
                }


            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private class BleGattCallback extends BluetoothGattCallback{
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            String text="";
            switch (newState){
                case BluetoothGatt.STATE_CONNECTED:
                    text = "connected";

                    connectingDevices.remove(gatt.getDevice().getAddress());
                    connectedDevices.put(gatt.getDevice().getAddress(),gatt);

                    connectedDevices.get(gatt.getDevice().getAddress()).discoverServices();

                    if (connectedDevices.size()<2){
                        scanLeDevice();
                    } else if (connectedDevices.size() == 2){
//                        for (String key : connectedDevices.keySet()){
//                            connectedDevices.get(key).discoverServices();
//                        }
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    text = "disconnected";

                    if (connectedDevices.containsKey(gatt.getDevice().getAddress())){
                        connectedDevices.remove(gatt.getDevice().getAddress());
                    }

                    if (connectingDevices.containsKey(gatt.getDevice().getAddress())){
                        connectingDevices.remove(gatt.getDevice().getAddress());
                    }

                    if (connectedDevices.size()<2){
                        scanLeDevice();
                    }

                    break;
            }

            Log.i(APP_NAME,"Connection state of "+ gatt.getDevice().getAddress() +" has changed: " + text);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(APP_NAME,"Service discovered: " + gatt.getDevice().getAddress()+ ": " + gatt.getServices().toString());

            gatt.setCharacteristicNotification(gatt.getService(UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")).getCharacteristic(UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")),true);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(APP_NAME,"Characteristic read: " + gatt.getDevice().getAddress());
            Log.i(APP_NAME, characteristic.getStringValue(0));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(APP_NAME,"Characteristic write: " + gatt.getDevice().getAddress());
            Log.i(APP_NAME, characteristic.getStringValue(0));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.i(APP_NAME,"Characteristic has changed: " + gatt.getDevice().getAddress());
            Log.i(APP_NAME, characteristic.getStringValue(0));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (String key : connectedDevices.keySet()){
            connectedDevices.get(key).close();
            connectedDevices.remove(key);
        }
    }
}
