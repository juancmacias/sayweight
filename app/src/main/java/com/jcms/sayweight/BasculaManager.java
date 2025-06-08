package com.jcms.sayweight;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.UUID;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.util.UUID;

public class BasculaManager {

    private final Context context;
    private final TextView textView;
    private static final UUID UUID_PESO = UUID.fromString("0000fed0-494c-4f47-4943-544543480000"); // Sustituye con el UUID real
    private static final UUID CCCD_UUID = UUID.fromString("0000fed1-494c-4f47-4943-544543480000");

    public BasculaManager(Context context, TextView textView) {
        this.context = context;
        this.textView = textView;
    }

    public void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        device.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Conectado. Descubriendo servicios...");
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BLE", "Desconectado");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            for (BluetoothGattService service : gatt.getServices()) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    if (UUID_PESO.equals(characteristic.getUuid())) {
                        Log.d("BLE", "Característica de peso encontrada: " + UUID_PESO);
                        enableNotifications(gatt, characteristic);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            float peso = parseWeight(data);
            Log.d("BLE", "Dato recibido: " + peso);
            ((MainActivity) context).runOnUiThread(() -> textView.setText(String.format("Peso: %.2f kg", peso)));
        }

        private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            boolean success = gatt.setCharacteristicNotification(characteristic, true);
            if (success) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                    Log.i("BLE", "Notificaciones habilitadas");
                } else {
                    Log.e("BLE", "Descriptor CCCD no encontrado");
                }
            } else {
                Log.e("BLE", "No se pudo activar notificación");
            }
        }
    };

    private float parseWeight(byte[] data) {
        if (data == null || data.length < 2) return 0f;
        int raw = (data[1] << 8) | (data[0] & 0xFF);
        return raw / 100.0f;
    }
}
