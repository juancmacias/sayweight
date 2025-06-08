package com.jcms.sayweight;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class BlueTooth {
    public interface BlueToothPhysicalDevice {
        void parseResponse(byte[] rd, BluetoothGattCharacteristic characteristic);

        boolean isCompatibleService(BluetoothGattService service);

        boolean isCompatiblePrimary(BluetoothGattCharacteristic characteristic);

        boolean isCompatibleSecondary(BluetoothGattCharacteristic characteristic);

        boolean isCompatibleControl(BluetoothGattCharacteristic characteristic);

        void enableDevice();

        void syncSequence();

        void sendCommand(org.json.JSONObject command);
    }

    private final Context context;
    private BluetoothAdapter bluetoothAdapter;

    public BlueTooth(Context context) {
        this.context = context;
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
    }

    public void scanAndConnect(String deviceName, BlueToothPhysicalDevice device) {
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (result.getDevice().getName() != null &&
                        result.getDevice().getName().equalsIgnoreCase(deviceName)) {
                    Toast.makeText(context, "Conectando:" + result.getDevice(), Toast.LENGTH_LONG).show();
                    scanner.stopScan(this);
                    connectToDevice(result.getDevice(), device);
                }
            }
        };
        scanner.startScan(callback);
    }

    private void connectToDevice(BluetoothDevice device, BlueToothPhysicalDevice deviceLogic) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (deviceLogic.isCompatibleService(service)) {
                        for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                            if (deviceLogic.isCompatiblePrimary(ch)) {
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                gatt.setCharacteristicNotification(ch, true);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                deviceLogic.parseResponse(data, characteristic);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    gatt.discoverServices();
                }
            }
        });
    }
}
