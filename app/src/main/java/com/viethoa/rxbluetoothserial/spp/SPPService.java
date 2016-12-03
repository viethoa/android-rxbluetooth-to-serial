package com.viethoa.rxbluetoothserial.spp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.viethoa.rxbluetoothserial.BluetoothSerialState;
import com.viethoa.rxbluetoothserial.cores.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by VietHoa on 23/10/2016.
 */
public class SPPService {

    private static final String TAG = SPPService.class.getSimpleName();
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @BluetoothSerialState
    private int mCurrentState;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private SPPServiceListener mSppServiceListener;

    public SPPService(SPPServiceListener listener) {
        mSppServiceListener = listener;
        mCurrentState = BluetoothSerialState.DISCONNECTED;
    }

    //----------------------------------------------------------------------------------------------
    // Properties
    //----------------------------------------------------------------------------------------------

    public synchronized void resetConnection() {
        Log.d(TAG, "resetConnection()");
        resetThreads();
    }

    public synchronized void connect(BluetoothDevice device) {
        Logger.d(TAG, String.format("connect to device: %s", device));
        if (mCurrentState == BluetoothSerialState.CONNECTING) {
            resetConnectThread();
        }
        if (mCurrentState == BluetoothSerialState.CONNECTED) {
            resetConnectedThread();
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(BluetoothSerialState.CONNECTING);
    }

    public synchronized void disconnect() {
        Logger.d(TAG, "disconnect");
        resetThreads();
        setState(BluetoothSerialState.DISCONNECTED);
    }

    public synchronized void write(byte[] data) {
        ConnectedThread connectedThread = null;
        synchronized (this) {
            if (mCurrentState == BluetoothSerialState.CONNECTED) {
                connectedThread = mConnectedThread;
            }
        }
        if (connectedThread != null) {
            connectedThread.write(data);
        }
    }

    public synchronized int getState() {
        return mCurrentState;
    }

    //----------------------------------------------------------------------------------------------
    // Settings
    //----------------------------------------------------------------------------------------------

    private synchronized void setState(@BluetoothSerialState int state) {
        Logger.d(TAG, "setState() " + mCurrentState + " -> " + state);

        mCurrentState = state;
        mSppServiceListener.onMessageStateChange(state);
    }

    private synchronized void resetThreads() {
        resetConnectThread();
        resetConnectedThread();
    }

    private synchronized void resetConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private synchronized void resetConnectedThread() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Connect threads
    //----------------------------------------------------------------------------------------------

    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "ConnectThread(" + device + ")");

            BluetoothSocket tempSocket = null;
            try {
                Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                tempSocket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));
            } catch (Exception e1) {
                Log.e(TAG, "Failed to create a socket with reflection!");
                try {
                    tempSocket = device.createRfcommSocketToServiceRecord(UUID_SPP);
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to create a secure socket!");
                    try {
                        tempSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                    } catch (Exception e3) {
                        Log.e(TAG, "Failed to create an insecure socket!");
                    }
                }
            }

            this.mDevice = device;
            this.mSocket = tempSocket;
        }

        public void run() {
            if (mDevice == null || mSocket == null) {
                cancel();
                disconnect();
                return;
            }

            try {
                mSocket.connect();
            } catch (Exception e) {
                Log.e(TAG, "" + e.getMessage());
                try {
                    Log.d(TAG, "trying to reconnect again");
                    mSocket.connect();
                } catch (Exception ex) {
                    cancel();
                    disconnect();
                    return;
                }
            }

            connected(mSocket, mDevice);
        }

        synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
            Logger.d(TAG, String.format("Connected to %s", device));

            resetConnectedThread();
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();

            setState(BluetoothSerialState.CONNECTED);
            mSppServiceListener.onDeviceInfo(device);
        }

        void cancel() {
            Logger.d(TAG, "ConnectThread -> cancel");

            try {
                if (mSocket != null) {
                    mSocket.close();
                }
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        ConnectedThread(BluetoothSocket socket) {
            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;

            try {
                tempInputStream = socket.getInputStream();
                tempOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }

            this.mSocket = socket;
            this.mInputStream = tempInputStream;
            this.mOutputStream = tempOutputStream;
        }

        public void run() {
            byte[] data = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mInputStream.read(data);
                    String message = new String(data, 0, bytes, Charset.forName("ISO-8859-1"));
                    mSppServiceListener.onMessageRead(data, message);
                } catch (IOException e) {
                    Logger.e(TAG, e.getMessage());
                    cancel();
                    disconnect();
                    break;
                }
            }
        }

        void write(byte[] data) {
            if (data == null) {
                return;
            }

            try {
                mOutputStream.write(data);
                String message = new String(data, 0, data.length, Charset.forName("ISO-8859-1"));
                mSppServiceListener.onMessageWrite(data, message);
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }

        void cancel() {
            Logger.d(TAG, "ConnectedThread -> cancel");
            try {
                if (mSocket != null) {
                    mSocket.close();
                }
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }

}
