package com.viethoa.rxbluetoothserial.spp;

import android.bluetooth.BluetoothDevice;

import com.viethoa.rxbluetoothserial.BluetoothSerialState;

/**
 * Created by VietHoa on 24/10/2016.
 */

public interface SPPServiceListener {

    /**
     * Bluetooth connection state have changed.
     */
    void onMessageStateChange(@BluetoothSerialState int state);

    /**
     * Receive data from bluetooth device.
     */
    void onMessageRead(byte[] bufferRead, String message);

    /**
     * Send data to bluetooth device such as command or something like that.
     */
    void onMessageWrite(byte[] bufferWrite, String message);

    /**
     * That notify what device we a connected for.
     */
    void onDeviceInfo(BluetoothDevice device);
}
