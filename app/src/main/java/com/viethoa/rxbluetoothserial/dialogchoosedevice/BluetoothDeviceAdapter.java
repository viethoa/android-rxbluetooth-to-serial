package com.viethoa.rxbluetoothserial.dialogchoosedevice;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.viethoa.rxbluetoothserial.R;
import com.viethoa.rxbluetoothserial.base.BaseArrayAdapter;

import java.util.List;

/**
 * Created by VietHoa on 23/10/2016.
 */
public class BluetoothDeviceAdapter extends BaseArrayAdapter<BluetoothDevice> {

    private final boolean mShowAddress;

    BluetoothDeviceAdapter(Context context, List<BluetoothDevice> data, boolean mShowAddress) {
        super(context, data);
        this.mShowAddress = mShowAddress;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_device_layout, parent, false);
            ViewHolder holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.bind(getItem(position));
        return convertView;
    }

    private class ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;

        ViewHolder(View itemView) {
            tvDeviceName = (TextView) itemView.findViewById(R.id.tv_device_name);
            tvDeviceAddress = (TextView) itemView.findViewById(R.id.tv_device_address);
        }

        void bind(BluetoothDevice device) {
            if (device == null) {
                return;
            }

            if (!TextUtils.isEmpty(device.getName())) {
                tvDeviceName.setText(device.getName());
            }
            if (mShowAddress && !TextUtils.isEmpty(device.getAddress())) {
                tvDeviceAddress.setText(device.getAddress());
            }
        }
    }
}
