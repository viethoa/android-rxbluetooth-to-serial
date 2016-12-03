package com.viethoa.rxbluetoothserial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.viethoa.rxbluetoothserial.cores.Logger;
import com.viethoa.rxbluetoothserial.dialogchoosedevice.DialogChooseDevice;
import com.viethoa.rxbluetoothserial.dialogchoosedevice.DialogChooseDeviceListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BluetoothSerialListener,
        DialogChooseDeviceListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    private Button btnScan;
    private TextView tvBluetoothConnection;
    private BluetoothSerial bluetoothSerial;

    private ProgressDialog dialogScanning;
    private ArrayList<BluetoothDevice> mDeviceList;
    private DialogChooseDevice dialogChooseDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect state text view.
        tvBluetoothConnection = (TextView) findViewById(R.id.tv_bluetooth_status);

        // Scan button
        btnScan = (Button) findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothSerial.startDiscover();
            }
        });

        // Choose device dialog
        dialogChooseDevice = new DialogChooseDevice(this);
        dialogChooseDevice.setListener(this);

        // Scan dialog
        dialogScanning = new ProgressDialog(this);

        // Bluetooth to serial
        bluetoothSerial = new BluetoothSerial(this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothSerial.setup();

        // Scan receiver
        IntentFilter scanFilter = new IntentFilter();
        scanFilter.addAction(BluetoothDevice.ACTION_FOUND);
        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothScanReceiver, scanFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(bluetoothScanReceiver);
    }

    @Override
    public void onBluetoothDeviceSelected(BluetoothDevice device) {
        bluetoothSerial.connect(device);
    }

    //----------------------------------------------------------------------------------------------
    // Discover events
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    bluetoothSerial.setup();
                } else {
                    onRequestTurnOnBluetooth();
                }
                break;
        }
    }

    private final BroadcastReceiver bluetoothScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mDeviceList = new ArrayList<>();
                dialogScanning.show();

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                dialogScanning.dismiss();

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Logger.d(TAG, "Found device " + device.getName());
                mDeviceList.add(device);
            }
        }
    };

    //----------------------------------------------------------------------------------------------
    // Bluetooth listener events
    //----------------------------------------------------------------------------------------------

    @Override
    public void onBluetoothNotSupported() {
        Toast.makeText(this, "bluetooth not supported", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestTurnOnBluetooth() {
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
    }

    @Override
    public void onBluetoothDeviceDisconnected() {
        tvBluetoothConnection.setText("Disconnected");
    }

    @Override
    public void onConnectingBluetoothDevice() {
        tvBluetoothConnection.setText("Connecting");
    }

    @Override
    public void onBluetoothDeviceConnected(BluetoothDevice device) {
        if (device != null) {
            tvBluetoothConnection.setText(String.format("Connected %s", device.getAddress()));
        }
    }

    @Override
    public void onBluetoothSerialRead(byte[] byteMessage, String message) {
        if (!TextUtils.isEmpty(message)) {
            Logger.d(TAG, String.format("Message read: %s", message));
        }
    }

    @Override
    public void onBluetoothSerialWrite(byte[] bytesMesage, String message) {
        if (!TextUtils.isEmpty(message)) {
            Logger.d(TAG, String.format("Message wrote: %s", message));
        }
    }
}
