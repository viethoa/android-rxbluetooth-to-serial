package com.viethoa.rxbluetoothserial.dialogchoosedevice;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.viethoa.rxbluetoothserial.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by VietHoa on 23/10/2016.
 */
public class DialogChooseDevice extends Dialog implements AdapterView.OnItemClickListener {

    private Context mContext;
    private ListView lvDevices;
    private TextView tvTitle;

    private String mTitle;
    private DialogChooseDeviceListener mListener;
    private List<BluetoothDevice> mDeviceList;
    private boolean isShowAddress;

    public DialogChooseDevice(Context context) {
        super(context, R.style.Window_DialogStyle);
        mContext = context;
        isShowAddress = true;

        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.dialog_chooses_device);

        tvTitle = (TextView) findViewById(R.id.tv_title);
        lvDevices = (ListView) findViewById(R.id.lv_devices);
    }

    //----------------------------------------------------------------------------------------------
    // Properties
    //----------------------------------------------------------------------------------------------

    public void setOnDeviceSelectedListener(DialogChooseDeviceListener listener) {
        mListener = listener;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setDevices(Set<BluetoothDevice> devices) {
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

    @Override
    public void setTitle(int resId) {
        mTitle = mContext.getString(resId);
    }

    @Override
    public void show() {
        if (!TextUtils.isEmpty(mTitle)) {
            tvTitle.setText(mTitle);
        }

        BluetoothDeviceAdapter adapter = new BluetoothDeviceAdapter(mContext, mDeviceList, isShowAddress);
        lvDevices.setAdapter(adapter);
        super.show();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (mDeviceList == null || mDeviceList.size() <= 0) {
            return;
        }
        if (position < 0 || position >= mDeviceList.size()) {
            return;
        }
        if (mListener == null) {
            return;
        }

        mListener.onBluetoothDeviceSelected(mDeviceList.get(position));
    }
}
