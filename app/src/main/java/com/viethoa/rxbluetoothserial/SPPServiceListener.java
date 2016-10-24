package com.viethoa.rxbluetoothserial;

/**
 * Created by VietHoa on 24/10/2016.
 */

public interface SPPServiceListener {

    void onMessageStateChange(@BluetoothSerialState int state);

    void onMessageRead(byte[] bufferRead, String message);

    void onMessageWrite(byte[] bufferWrite, String message);

    void onDeviceInfo(String deviceName, String deviceAddress);
}
