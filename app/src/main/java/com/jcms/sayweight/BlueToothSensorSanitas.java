package com.jcms.sayweight;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;



import org.json.JSONObject;

public class BlueToothSensorSanitas implements BlueTooth.BlueToothPhysicalDevice {
    private static final String LOGTAG = BlueToothSensorSanitas.class.getSimpleName();

    private final BlueTooth parent;
    private BluetoothDataListener dataListener;

    public interface BluetoothDataListener {
        void onWeightRead(float weight);
        void onConnectionFailed(String reason);
    }

    public BlueToothSensorSanitas(BlueTooth parent) {
        this.parent = parent;
    }

    public void setBluetoothDataListener(BluetoothDataListener listener) {
        this.dataListener = listener;
    }

    public boolean isCompatibleService(BluetoothGattService service) {
        return service.getUuid().toString().equals("0000fed0-494c-4f47-4943-544543480000");
    }

    public boolean isCompatiblePrimary(BluetoothGattCharacteristic characteristic) {
        return characteristic.getUuid().toString().equals("0000fed1-494c-4f47-4943-544543480000");
    }

    public boolean isCompatibleSecondary(BluetoothGattCharacteristic characteristic) {
        return characteristic.getUuid().toString().equals("0000fed2-494c-4f47-4943-544543480000");
    }

    public boolean isCompatibleControl(BluetoothGattCharacteristic characteristic) {
        return false;
    }

    public void enableDevice() {
        //parent.noFireOnWrite = true;
    }

    public void syncSequence() {
        startSyncSequence();
    }

    @Override
    public void sendCommand(JSONObject command) {

    }


    public void parseResponse(byte[] rd, BluetoothGattCharacteristic characteristic) {
        if (rd.length >= 13) {
            int weightRaw = ((rd[11] & 0xFF) << 8) | (rd[12] & 0xFF);
            float weight = weightRaw / 100.0f;

            Log.d(LOGTAG, "Peso recibido: " + weight + " kg");
            if (dataListener != null) {
                dataListener.onWeightRead(weight);
            }
        } else {
            Log.w(LOGTAG, "Paquete recibido muy corto, ignorado.");
        }
    }

    private JSONObject syncStatus;

    private void startSyncSequence() {
        // Sync sequence logic here (already provided in previous code)
    }



    private byte[] getStepHistoryData(int day) {
        byte[] data = new byte[1];
        data[0] = (byte) day;
        return data;
    }

    private byte[] getSleepHistoryData(int position) {
        byte[] data = new byte[2];
        data[0] = (byte) (0x80 + (position & 0x7f));
        data[1] = (byte) ((position >> 7) & 0xff);
        return data;
    }
}
