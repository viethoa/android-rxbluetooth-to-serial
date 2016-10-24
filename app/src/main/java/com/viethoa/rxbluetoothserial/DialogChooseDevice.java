package com.viethoa.rxbluetoothserial;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.viethoa.rxbluetoothserial.listeners.DialogChooseDeviceListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by VietHoa on 23/10/2016.
 */
public class DialogChooseDevice {

    private Context mContext;
    private DialogChooseDeviceListener mListener;
    private Set<BluetoothDevice> mDeviceSet;
    private List<BluetoothDevice> mDeviceList;
    private boolean isShowAddress;
    private String mTitle;

    public DialogChooseDevice(Context context) {
        mContext = context;
        isShowAddress = true;
    }

    public void setOnDeviceSelectedListener(DialogChooseDeviceListener listener) {
        mListener = listener;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setTitle(int resId) {
        mTitle = mContext.getString(resId);
    }

    public void setDevices(Set<BluetoothDevice> devices) {
        mDeviceSet = devices;
        if (devices == null || devices.size() <= 0) {
            return;
        }

        mDeviceList = new ArrayList<>();
        for (BluetoothDevice d : devices) {
            mDeviceList.add(d);
        }
    }

    public void shouldShowAddress(boolean showAddress) {
        isShowAddress = showAddress;
    }

    public void show() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(mTitle)
                .setAdapter(new BluetoothDeviceAdapter(mContext, mDeviceList, isShowAddress), null)
                .create();

        final ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mListener.onBluetoothDeviceSelected(mDeviceList.get(position));
                    dialog.cancel();
                }
            });
        }

        dialog.show();
    }

}
