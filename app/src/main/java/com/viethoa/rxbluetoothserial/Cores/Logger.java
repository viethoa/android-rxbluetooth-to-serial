package com.viethoa.rxbluetoothserial.cores;

import android.support.compat.BuildConfig;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by VietHoa on 23/10/2016.
 */
public class Logger {

    public static void i(String tag, String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.i(tag, message);
        }
    }

    public static void e(String tag, Exception ex) {
        if (ex == null) {
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.e(tag, ex.getMessage());
        }
    }

    public static void e(String tag, String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.e(tag, message);
        }
    }

    public static void d(String tag, String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void v(String tag, String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (BuildConfig.DEBUG) {
            Log.v(tag, message);
        }
    }
}
