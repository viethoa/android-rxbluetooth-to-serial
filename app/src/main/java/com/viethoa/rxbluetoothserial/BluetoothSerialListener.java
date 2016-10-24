package com.viethoa.rxbluetoothserial;

import android.bluetooth.BluetoothDevice;

/**
 * Created by VietHoa on 23/10/2016.
 */
public interface BluetoothSerialListener {

    /**
     * Bluetooth adapter is not present on this device.
     */
    void onBluetoothNotSupported();

    /**
     * This device's Bluetooth adapter is turned off.
     */
    void onBluetoothDisabled();

    /**
     * Disconnected from a remote Bluetooth device.
     */
    void onBluetoothDeviceDisconnected();

    /**
     * Connecting to a remote Bluetooth device.
     */
    void onConnectingBluetoothDevice();

    /**
     * Connected to a remote Bluetooth device.
     */
    void onBluetoothDeviceConnected(BluetoothDevice device);

    /**
     * Specified message is read from the serial port.
     */
    void onBluetoothSerialRead(byte[] byteMessage, String message);

    /**
     * Specified message is written to the serial port.
     */
    void onBluetoothSerialWrite(byte[] bytesMesage, String message);

}
