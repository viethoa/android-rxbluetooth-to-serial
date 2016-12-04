package com.viethoa.rxbluetoothserial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

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

    public BluetoothSerial(Context context, BluetoothSerialListener listener) {
        mAdapter = getAdapter(context);
        mListener = listener;
    }

    //----------------------------------------------------------------------------------------------
    // Settings
    //----------------------------------------------------------------------------------------------

    private BluetoothAdapter getAdapter(Context context) {
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

    private boolean checkBluetooth() {
        if (mAdapter == null) {
            mListener.onBluetoothNotSupported();
            return false;
        } else {
            if (!mAdapter.isEnabled()) {
                mListener.onRequestTurnOnBluetooth();
                return false;
            } else {
                return true;
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Properties
    //----------------------------------------------------------------------------------------------

    public void setup() {
        if (!checkBluetooth()) {
            return;
        }

        if (mPairedDevices == null) {
            Log.d(TAG, "Create mPairedDevices");
            mPairedDevices = mAdapter.getBondedDevices();
        }
        if (mService == null) {
            Log.d(TAG, "Create mService");
            mService = new SPPService(this);
        }
    }

    public void startDiscover() {
        if (mAdapter != null) {
            mAdapter.startDiscovery();
        }
    }

    public void stopDiscover() {
        if (mAdapter != null) {
            mAdapter.cancelDiscovery();
        }
    }

    public void connect(android.bluetooth.BluetoothDevice device) {
        // Always stop discover because it will slow down connection
        stopDiscover();

        if (mService != null) {
            mService.resetConnection();
            mService.connect(device);
        }
    }

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

    public void write(byte[] data) {
        if (mService.getState() == BluetoothSerialState.CONNECTED) {
            mService.write(data);
        }
    }

    public void write(String data, boolean crlf) {
        write(data.getBytes(Charset.forName("ISO-8859-1")));
        if (crlf) {
            write(CRLF);
        }
    }

    public void write(String data) {
        write(data.getBytes(Charset.forName("ISO-8859-1")));
    }

    public void writeln(String data) {
        write(data.getBytes(Charset.forName("ISO-8859-1")));
        write(CRLF);
    }

    public void stop() {
        if (mService != null) {
            mService.disconnect();
        }
        if (mAdapter != null) {
            mAdapter.cancelDiscovery();
        }

        mConnectedDevice = null;
    }

    //----------------------------------------------------------------------------------------------
    // Info properties
    //----------------------------------------------------------------------------------------------

    public int getState() {
        return mService.getState();
    }

    public boolean isBluetoothEnabled() {
        return mAdapter.isEnabled();
    }

    public boolean isConnected() {
        if (mService == null) {
            return false;
        }

        return (mService.getState() == BluetoothSerialState.CONNECTED);
    }

    public Set<android.bluetooth.BluetoothDevice> getPairedDevices() {
        return mPairedDevices;
    }

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

    public String getConnectedDeviceName() {
        if (mConnectedDevice == null) {
            return null;
        }
        return mConnectedDevice.getName();
    }

    public String getConnectedDeviceAddress() {
        if (mConnectedDevice == null) {
            return null;
        }
        return mConnectedDevice.getAddress();
    }

    //----------------------------------------------------------------------------------------------
    // SPP Service listener: will notify about connection changed
    //----------------------------------------------------------------------------------------------

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
