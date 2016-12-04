package com.viethoa.rxbluetoothserial.serialportprofile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.viethoa.rxbluetoothserial.BluetoothSerialState;
import com.viethoa.rxbluetoothserial.cores.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
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
    private int currentState;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private WeakReference<SPPServiceListener> sppServiceListener;

    public SPPService(SPPServiceListener listener) {
        sppServiceListener = new WeakReference<>(listener);
        currentState = BluetoothSerialState.DISCONNECTED;
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

        resetThreads();
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(BluetoothSerialState.CONNECTING);
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Logger.d(TAG, String.format("Connected to %s", device));

        resetConnectedThread();
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(BluetoothSerialState.CONNECTED);
        if (sppServiceListener != null && sppServiceListener.get() != null) {
            sppServiceListener.get().onDeviceInfo(device);
        }
    }

    public synchronized void disconnect() {
        Logger.d(TAG, "disconnect");
        resetThreads();
        setState(BluetoothSerialState.DISCONNECTED);
    }

    public synchronized void write(byte[] data) {
        ConnectedThread connectedThread = null;
        synchronized (this) {
            if (currentState == BluetoothSerialState.CONNECTED) {
                connectedThread = this.connectedThread;
            }
        }
        if (connectedThread != null) {
            connectedThread.write(data);
        }
    }

    public synchronized int getState() {
        return currentState;
    }

    //----------------------------------------------------------------------------------------------
    // Settings
    //----------------------------------------------------------------------------------------------

    private synchronized void setState(@BluetoothSerialState int state) {
        Logger.d(TAG, "setState() " + currentState + " -> " + state);

        currentState = state;
        if (sppServiceListener != null && sppServiceListener.get() != null) {
            sppServiceListener.get().onMessageStateChange(state);
        }
    }

    private synchronized void resetThreads() {
        resetConnectThread();
        resetConnectedThread();
    }

    private synchronized void resetConnectThread() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private synchronized void resetConnectedThread() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Connect threads
    //----------------------------------------------------------------------------------------------

    private class ConnectThread extends Thread {

        private final BluetoothSocket socket;
        private final BluetoothDevice device;

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

            this.device = device;
            this.socket = tempSocket;
        }

        public void run() {
            if (device == null || socket == null) {
                cancel();
                disconnect();
                return;
            }

            try {
                socket.connect();
            } catch (Exception e) {
                Log.e(TAG, "" + e.getMessage());
                try {
                    Log.d(TAG, "trying to reconnect again");
                    socket.connect();
                } catch (Exception ex) {
                    cancel();
                    disconnect();
                    return;
                }
            }

            connected(socket, device);
        }

        void cancel() {
            Logger.d(TAG, "ConnectThread -> cancel");

            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        ConnectedThread(BluetoothSocket socket) {
            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;

            try {
                tempInputStream = socket.getInputStream();
                tempOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }

            this.socket = socket;
            this.inputStream = tempInputStream;
            this.outputStream = tempOutputStream;
        }

        public void run() {
            byte[] data = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(data);
                    String message = new String(data, 0, bytes, Charset.forName("ISO-8859-1"));
                    sendReadMessage(data, message);
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
                outputStream.write(data);
                String message = new String(data, 0, data.length, Charset.forName("ISO-8859-1"));
                sendWroteMessage(data, message);
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }

        private void sendReadMessage(byte[] data, String message) {
            if (sppServiceListener != null && sppServiceListener.get() != null) {
                sppServiceListener.get().onMessageRead(data, message);
            }
        }

        private void sendWroteMessage(byte[] data, String message) {
            if (sppServiceListener != null && sppServiceListener.get() != null) {
                sppServiceListener.get().onMessageWrite(data, message);
            }
        }

        void cancel() {
            Logger.d(TAG, "ConnectedThread -> cancel");
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }

}
