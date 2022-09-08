package com;

import android.app.Application;
import android.content.Context;

import com.admob.AppOpenManager;
import com.admob.GAD;
import com.play.pay.PayConsumer;
import com.play.pay.PayUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2015/10/21.
 */
public class BaseApplication extends Application {

    public static Context context;


    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        GAD.init(this);
        PayUtil.init(new PayConsumer());
    }
}
