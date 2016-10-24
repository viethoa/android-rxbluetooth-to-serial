package com.viethoa.rxbluetoothserial.base;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by VietHoa on 23/06/15.
 */
public class BaseArrayAdapter<T> extends BaseAdapter {

    protected Context mContext;
    protected List<T> mDataArray;

    public BaseArrayAdapter(Context context, List<T> data) {
        this.mContext = context;
        this.mDataArray = data;
    }

    public void refreshData(ArrayList<T> data) {
        mDataArray = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mDataArray == null)
            return 0;
        return mDataArray.size();
    }

    @Override
    public T getItem(int i) {
        if (mDataArray == null)
            return null;
        if (i < 0 || i >= mDataArray.size())
            return null;

        return mDataArray.get(i);
    }

    @Override
    public long getItemId(int i) {
        if (mDataArray == null)
            return 0;
        if (i >= mDataArray.size())
            return 0;

        return mDataArray.get(i).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        return null;
    }
}
