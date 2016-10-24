package com.viethoa.rxbluetoothserial.listeners;

import android.bluetooth.BluetoothDevice;

/**
 * Created by VietHoa on 24/10/2016.
 */

public interface DialogChooseDeviceListener {

    /**
     * User have choose a device from the list.
     */
    void onBluetoothDeviceSelected(BluetoothDevice device);
}
