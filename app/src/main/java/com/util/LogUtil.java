package com.util;

import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class LogUtil {
    private static final String TAG = LogUtil.class.getName();

    public static void i(String msg, Object... args) {
        Log.i(TAG, String.format(msg, args));
    }

    public static void iTag(String tag, String msg, Object... args) {
        Log.i(tag, String.format(msg, args));
    }

    public static void d(String msg, Object... args) {
        Log.i(TAG, String.format(msg, args));
    }

    public static void dTag(String tag, String msg, Object... args) {
        Log.i(tag, String.format(msg, args));
    }

    public static void e(String msg, Object... args) {
        Log.i(TAG, String.format(msg, args));
    }

    public static void eTag(String tag, String msg, Object... args) {
        Log.i(tag, String.format(msg, args));
    }

    public static void e(Object msg) {
        e(TAG, msg);
    }

    public static void error(String tag, Object msg) {
        if (msg == null) {
            return;
        }
        if (msg instanceof Collection) {
            Log.e(tag, "" + Arrays.deepToString(((Collection<?>) msg).toArray()));
        } else if (msg.getClass().isArray()) {
            Log.e(tag, "" + Arrays.deepToString((Object[]) msg));
        } else if (msg instanceof Map) {
            Map map = (Map) msg;
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                Log.e(tag, "key : " + key + " ; value : " + value);
            }
        } else {
            Log.e(tag, "" + msg);
        }
    }

    public static void printStackTrace() {

        StackTraceElement[] stackElements = new Throwable().getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                LogUtil.e("printStackTrace", "" + stackElements[i]);
            }
        }
    }
}
