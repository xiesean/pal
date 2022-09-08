package com.admob;

import android.content.Context;
import android.content.SharedPreferences;

public class DataStoreUtils {

    public static final String DEFAULT_VALUE = "";
    public static final String VALUE_TRUE = "1";
    public static final String VALUE_FALSE = "0";
    /**
     * 通用 true
     */
    public static final String SP_TRUE = "true";
    /**
     * 通用 false
     */
    public static final String SP_FALSE = "false";
    private static final String FILE_NAME = "tg";

    public static final String SP_JOYSTICK = "spj";

    public static SharedPreferences share;

    // 保存本地信息
    public static void saveLocalInfo(Context ctx, String name, String value) {
        if (share == null) {
            share = ctx.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        }
        if (share != null) {
            share.edit().putString(name, value).commit();
        }
        share = null;
    }

    // 读取本地信息
    public static String readLocalInfo(Context ctx, String name) {
        if (share == null) {
            share = ctx.getSharedPreferences(FILE_NAME, 0);
        }
        if (share != null) {
            return share.getString(name, DEFAULT_VALUE);
        }
        share = null;
        return DEFAULT_VALUE;
    }

    public static void saveLongValue(Context context, String name, long value) {
        saveLocalInfo(context, name, String.valueOf(value));
    }

    public static long readLongValue(Context context, String name, long defaultV) {
        try {
            return Long.parseLong(readLocalInfo(context, name));
        } catch (Exception e) {
            return defaultV;
        }
    }

    public static void setOnceValue(Context ctx, String fileName, String key, String value) {
        if (share == null) {
            share = ctx.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        }
        if (share != null) {
            share.edit().putString(key, value).commit();
        }
        share = null;
    }
}
