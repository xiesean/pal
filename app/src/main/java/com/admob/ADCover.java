package com.admob;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.util.LogUtil;


public class ADCover {
    private static final String TAG = ADCover.class.getName();

    private static InterstitialAd mInterstitialAd;

    @SuppressLint("SimpleDateFormat")
    public static void show(final Activity activity, boolean alwaysShowAd) {

        if (GAD.isNoAd()) {
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(activity, GAD.UID_COVER, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                mInterstitialAd = interstitialAd;
                LogUtil.i(TAG,"onAdLoaded");
                show(activity);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                // Handle the error
                LogUtil.i(TAG,"LoadAdError:%s", loadAdError.getMessage());
                mInterstitialAd = null;
            }
        });

    }

    static void show(Activity activity) {
        if (mInterstitialAd == null) {
            return;
        }
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                LogUtil.d("The ad was dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when fullscreen content failed to show.
                LogUtil.d("The ad failed to show.%s", adError.getMessage());
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                // Make sure to set your reference to null so you don't
                // show it a second time.
                mInterstitialAd = null;
                LogUtil.d("The ad was shown.");
            }
        });
        try {
            if (!activity.isFinishing()) {
                mInterstitialAd.show(activity);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
