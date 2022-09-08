package com.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    public static String getTodayYMDStry() {
        return getDateStr(new Date(), null);
    }

    public static String getYMDStr(Date date) {
        return getDateStr(date, null);
    }

    public static String getDateStr(Date date, String format) {
        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }
}
