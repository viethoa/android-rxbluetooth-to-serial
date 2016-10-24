package com.viethoa.rxbluetoothserial;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.viethoa.rxbluetoothserial.Cores.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by VietHoa on 23/10/2016.
 */
class SPPService {

    private static final String TAG = SPPService.class.getSimpleName();
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @BluetoothSerialState
    private int mCurrentState;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    SPPService(Handler handler) {
        mHandler = handler;
        mCurrentState = BluetoothSerialState.DISCONNECTED;
    }

    //----------------------------------------------------------------------------------------------
    // Properties
    //----------------------------------------------------------------------------------------------

    synchronized void connect(BluetoothDevice device) {
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

    synchronized void disconnect() {
        Logger.d(TAG, "disconnect");
        resetThreads();
        setState(BluetoothSerialState.DISCONNECTED);
    }

    synchronized void write(byte[] data) {
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

    synchronized int getState() {
        return mCurrentState;
    }

    //----------------------------------------------------------------------------------------------
    // Settings
    //----------------------------------------------------------------------------------------------

    private synchronized void setState(int state) {
        Logger.d(TAG, "setState() " + mCurrentState + " -> " + state);

        mCurrentState = state;
        mHandler.obtainMessage(BluetoothSerial.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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
    // Threads
    //----------------------------------------------------------------------------------------------

    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        ConnectThread(BluetoothDevice device) {
            BluetoothSocket socket = null;

            try {
                Logger.d(TAG, String.format("Connect to device: %s", device));
                socket = device.createRfcommSocketToServiceRecord(UUID_SPP);
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(UUID_SPP);
                } catch (IOException ex) {
                    Logger.e(TAG, ex.getMessage());
                }
            }

            this.mDevice = device;
            this.mSocket = socket;
        }

        public void run() {
            if (mDevice == null || mSocket == null) {
                cancel();
                disconnect();
                return;
            }

            try {
                mSocket.connect();
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
                cancel();
                disconnect();
                return;
            }

            synchronized (SPPService.this) {
                mConnectThread = null;
            }

            connected(mSocket, mDevice);
        }

        synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
            Logger.d(TAG, String.format("Connected to %s", device));

            resetThreads();
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();

            Message msg = mHandler.obtainMessage(BluetoothSerial.MESSAGE_DEVICE_INFO);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothSerial.KEY_DEVICE_NAME, device.getName());
            bundle.putString(BluetoothSerial.KEY_DEVICE_ADDRESS, device.getAddress());
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            setState(BluetoothSerialState.CONNECTED);
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
            byte[] data = new byte[2048];
            int bytes;

            while (true) {
                try {
                    bytes = mInputStream.read(data);
                    String messageStr = new String(data, 0, bytes, Charset.forName("ISO-8859-1"));
                    Message messageObj = mHandler.obtainMessage(BluetoothSerial.MESSAGE_READ, bytes, -1,
                            messageStr.getBytes(Charset.forName("ISO-8859-1")));
                    messageObj.sendToTarget();
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
                mHandler.obtainMessage(BluetoothSerial.MESSAGE_WRITE, -1, -1, data).sendToTarget();
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
