package com.viethoa.rxbluetoothserial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.viethoa.rxbluetoothserial.spp.SPPServiceListener;
import com.viethoa.rxbluetoothserial.spp.SPPService;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Created by VietHoa on 23/10/2016.
 */
public class BluetoothSerial implements SPPServiceListener {

    private static final String TAG = BluetoothSerial.class.getSimpleName();
    private static final byte[] CRLF = {0x0D, 0x0A};

    private BluetoothAdapter mAdapter;
    private Set<android.bluetooth.BluetoothDevice> mPairedDevices;

    private SPPService mService;
    private BluetoothDevice mConnectedDevice;
    private BluetoothSerialListener mListener;

    /**
     * Init
     */
    public BluetoothSerial(Context context, BluetoothSerialListener listener) {
        mAdapter = getAdapter(context);
        mListener = listener;
    }

    static BluetoothAdapter getAdapter(Context context) {
        BluetoothAdapter bluetoothAdapter = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null)
                bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }

    /**
     * Discover bluetooth devices
     */
    public void startDiscover() {
        if (mAdapter != null) {
            mAdapter.startDiscovery();
        }
    }

    /**
     * Stop discover bluetooth devices
     */
    public void stopDiscover() {
        if (mAdapter != null && mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    /**
     * Check the presence of a Bluetooth adapter on this device and set up the Bluetooth Serial Port Profile (SPP) service.
     */
    public void setup() {
        if (!checkBluetooth()) {
            return;
        }

        Log.d(TAG, "Create SPPService");
        mPairedDevices = mAdapter.getBondedDevices();
        if (mService == null) {
            mService = new SPPService(this);
        }
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     */
    public boolean isBluetoothEnabled() {
        return mAdapter.isEnabled();
    }

    public boolean checkBluetooth() {
        if (mAdapter == null) {
            mListener.onBluetoothNotSupported();
            return false;
        } else {
            if (!mAdapter.isEnabled()) {
                mListener.onBluetoothDisabled();
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Open a Bluetooth serial port and get ready to establish a connection with a remote device.
     */
    //public void start() {
    //   if (mService != null && mService.getState() == STATE_DISCONNECTED) {
    //       mService.disconnect();
    //   }
    //}

    /**
     * Connect to a remote Bluetooth device with the specified MAC address.
     *
     * @param address The MAC address of a remote Bluetooth device.
     */
    public void connect(String address) {
        try {
            android.bluetooth.BluetoothDevice device = mAdapter.getRemoteDevice(address);
            if (device == null) {
                mListener.onBluetoothDeviceDisconnected();
                return;
            }

            mListener.onConnectingBluetoothDevice();
            if (device.getBondState() != android.bluetooth.BluetoothDevice.BOND_BONDED) {
                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
            } else {
                connect(device);
            }
        } catch (Exception e) {
            Log.e(TAG, "BluetoothDevice not found!");
        }
    }

    /**
     * Connect to a remote Bluetooth device.
     *
     * @param device A remote Bluetooth device.
     */
    public void connect(android.bluetooth.BluetoothDevice device) {
        if (mService != null) {
            mService.connect(device);
        }
    }

    /**
     * Write the specified bytes to the Bluetooth serial port.
     *
     * @param data The data to be written.
     */
    public void write(byte[] data) {
        if (mService.getState() == BluetoothSerialState.CONNECTED) {
            mService.write(data);
        }
    }

    /**
     * Write the specified bytes to the Bluetooth serial port.
     *
     * @param data The data to be written.
     * @param crlf Set true to end the data with a newline (\r\n).
     */
    public void write(String data, boolean crlf) {
        write(data.getBytes(Charset.forName("ISO-8859-1")));
        if (crlf) {
            write(CRLF);
        }
    }

    /**
     * Write the specified string to the Bluetooth serial port.
     *
     * @param data The data to be written.
     */
    public void write(String data) {
        write(data.getBytes(Charset.forName("ISO-8859-1")));
    }

    /**
     * Write the specified string ended with a new line (\r\n) to the Bluetooth serial port.
     *
     * @param data The data to be written.
     */
    public void writeln(String data) {
        write(data.getBytes(Charset.forName("ISO-8859-1")));
        write(CRLF);
    }

    /**
     * Disconnect from the remote Bluetooth device and close the active Bluetooth serial port.
     */
    public void stop() {
        if (mService != null) {
            mService.disconnect();
        }
        if (mAdapter != null) {
            mAdapter.cancelDiscovery();
        }

        mConnectedDevice = null;
    }

    /**
     * Get the current state of the Bluetooth serial port.
     *
     * @return the current state
     */
    public int getState() {
        return mService.getState();
    }

    /**
     * Return true if a connection to a remote Bluetooth device is established.
     *
     * @return true if connected to a device
     */
    public boolean isConnected() {
        if (mService == null) {
            return false;
        }

        return (mService.getState() == BluetoothSerialState.CONNECTED);
    }

    /**
     * Get connected remote bluetooth device name.
     */
    public String getConnectedDeviceName() {
        if (mConnectedDevice == null) {
            return null;
        }
        return mConnectedDevice.getName();
    }

    /**
     * Get the MAC address of the connected remote Bluetooth device.
     */
    public String getConnectedDeviceAddress() {
        if (mConnectedDevice == null) {
            return null;
        }
        return mConnectedDevice.getAddress();
    }

    /**
     * Get the paired Bluetooth devices of this device.
     */
    public Set<android.bluetooth.BluetoothDevice> getPairedDevices() {
        return mPairedDevices;
    }

    /**
     * Get the names of the paired Bluetooth devices of this device.
     */
    public String[] getPairedDevicesName() {
        if (mPairedDevices != null) {
            String[] name = new String[mPairedDevices.size()];
            int i = 0;
            for (android.bluetooth.BluetoothDevice d : mPairedDevices) {
                name[i] = d.getName();
                i++;
            }
            return name;
        }
        return null;
    }

    /**
     * Get the MAC addresses of the paired Bluetooth devices of this device.
     */
    public String[] getPairedDevicesAddress() {
        if (mPairedDevices != null) {
            String[] address = new String[mPairedDevices.size()];
            int i = 0;
            for (android.bluetooth.BluetoothDevice d : mPairedDevices) {
                address[i] = d.getAddress();
                i++;
            }
            return address;
        }
        return null;
    }

    /**
     * SPP Service listener: will notify about connection changed
     */
    @Override
    public void onMessageStateChange(@BluetoothSerialState int state) {
        switch (state) {
            case BluetoothSerialState.CONNECTED:
                mListener.onBluetoothDeviceConnected(mConnectedDevice);
                break;
            case BluetoothSerialState.CONNECTING:
                mListener.onConnectingBluetoothDevice();
                break;
            case BluetoothSerialState.DISCONNECTED:
                mListener.onBluetoothDeviceDisconnected();
                break;
        }
    }

    @Override
    public void onMessageWrite(byte[] bufferWrite, String message) {
        String messageWrite = new String(bufferWrite);
        mListener.onBluetoothSerialWrite(bufferWrite, messageWrite);
    }

    @Override
    public void onMessageRead(byte[] bufferRead, String message) {
        String messageRead = new String(bufferRead);
        mListener.onBluetoothSerialRead(bufferRead, messageRead);
    }

    @Override
    public void onDeviceInfo(BluetoothDevice device) {
        mConnectedDevice = device;
    }
}
