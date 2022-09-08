package com.admob;

import android.app.Activity;
import android.os.Handler;
import android.widget.Toast;

import com.BaseApplication;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class GAD {
    private static final long REWORDS_COUNT = 6;
    public static String KEY_NO_AD = "noad";

    public static String UID_BANNER = "ca-app-pub-2342479172622730/7933482993";
    public static String UID_COVER = "ca-app-pub-2342479172622730/1230427553";
    public static String UID_SPLASH = "ca-app-pub-2342479172622730/2681156316";
    public static String UID_REWARD = "ca-app-pub-2342479172622730/8240604753";

    public static Handler handler = new Handler();
    private static long oldTime = 0;

    private static AppOpenManager appOpenManager;

    public static void init(BaseApplication context) {
        MobileAds.initialize(context, initializationStatus -> {
            ADRewarded.load(context);
            AppOpenManager.init(context);
        });

    }

    public static AdView createBanner(Activity act) {
        return ADBanner.createAdView(act);
    }

    public static void showbannerT(Activity ctx) {
        if (isNoAd()) {
            return;
        }
        ADBanner.addFloatingBanner(ctx, 1);
    }

    public static void showbannerB(Activity ctx) {
        if (isNoAd()) {
            return;
        }
        ADBanner.addFloatingBanner(ctx, 0);
    }

    public static void showCover(Activity activity) {
        if (isNoAd()) {
            return;
        }
        ADCover.show(activity, true);
    }

    public static void loadBanner(AdView adView) {
        // Create an ad request. Check logcat output for the hashed device ID to
        // get test ads on a physical device.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

    public static boolean loadReward(Activity activity) {
        return ADRewarded.loadReward(activity);
    }

    public static void showReward(Activity activity, Runnable runnable) {
        ADRewarded.show(activity, runnable);
    }

    public static void showReward(Activity activity) {
        if (canLoadRewardAd()) {
            ADRewarded.show(activity);
        } else {
            Toast.makeText(BaseApplication.context, "今日奖励机会已用完,请明天再试!", Toast.LENGTH_LONG).show();
        }
    }

    private static boolean canLoadRewardAd() {
        long currentTimes = ADRewarded.getTodayRewardAdCount();
        if (currentTimes > REWORDS_COUNT) {
            return false;
        }
        return true;
    }

    public static boolean isNoAd() {
        return DataStoreUtils.SP_TRUE.equals(DataStoreUtils.readLocalInfo(BaseApplication.context, KEY_NO_AD));
    }

    public static void setNoAd() {
        DataStoreUtils.saveLocalInfo(BaseApplication.context, KEY_NO_AD, DataStoreUtils.SP_TRUE);
    }

    // 是否可免费消息
    public static boolean canFreeConsumer(long amount) {
        return ADRewarded.consumerAmount(amount);
    }
}
