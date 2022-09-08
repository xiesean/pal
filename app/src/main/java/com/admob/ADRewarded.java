package com.admob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.BaseApplication;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.util.DateUtil;

public class ADRewarded {
    private static final String TAG = "ADRewarded";
    private static RewardedInterstitialAd rewardedInterstitialAd;
    private static final String AD_EARN = "ad_earn";
    private static boolean isLoading = false;
    private static boolean isTip = false;
    public static String KEY_CAN_LOAD__REWARDS_AD = "c_r_ad";

    public static boolean loadReward(Activity activity) {
        if (rewardedInterstitialAd != null) {
            return true;
        } else {
            load(activity);
            return false;
        }
    }

    public static void load(Context context) {
        if (GAD.isNoAd()) {
            return;
        }
        if (isLoading) {
            return;
        }
        isLoading = true;
        RewardedInterstitialAd.load(context, GAD.UID_REWARD, new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedInterstitialAd ad) {
                rewardedInterstitialAd = ad;
                isLoading = false;
                if (isTip) {
                    Toast.makeText(BaseApplication.context, "广告已就绪，可以获取免费功能了！", Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, "onAdLoaded");
                rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    /** Called when the ad failed to show full screen content. */
                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.i(TAG, "onAdFailedToShowFullScreenContent");
                    }

                    /** Called when ad showed the full screen content. */
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.i(TAG, "onAdShowedFullScreenContent");
                    }

                    /** Called when full screen content is dismissed. */
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.i(TAG, "onAdDismissedFullScreenContent");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "onAdFailedToLoad:" + loadAdError);
                isLoading = false;
            }
        });
    }

    /**
     * 进入已用次数
     */
    public static long getTodayRewardAdCount() {
        return DataStoreUtils.readLongValue(BaseApplication.context, getTodayRewardAdCountKey(), 0);
    }

    /**
     * 进入次数KEY
     */
    private static String getTodayRewardAdCountKey() {
        return KEY_CAN_LOAD__REWARDS_AD + "_" + DateUtil.getTodayYMDStry();
    }

    /**
     * 增加进入次数
     */
    private static void addTodayRewardCount() {
        DataStoreUtils.saveLongValue(BaseApplication.context, getTodayRewardAdCountKey(), getTodayRewardAdCount() + 1);
    }

    // 展示广告, 按需奖励
    public static void show(Activity activity, Runnable runnable) {
        if (GAD.isNoAd()) {
            return;
        }
        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.show(activity, rewardItem -> {
                Log.i(TAG, "onUserEarnedReward");
                runnable.run();
                rewardedInterstitialAd = null;
            });
        } else {
            if (!isLoading) {
                load(activity);
                isTip = false;
            }
        }
    }

    public static void show(Activity activity) {
        if (GAD.isNoAd()) {
            return;
        }
        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.show(activity, rewardItem -> {
                Log.i(TAG, "onUserEarnedReward");
                int count = Math.max(((int) (Math.random() * 10)) / 3 + 1, 3) * rewardItem.getAmount();
                DataStoreUtils.saveLongValue(BaseApplication.context, AD_EARN, getAmount() + count);
                Toast.makeText(activity, "奖励已发放！+" + count, Toast.LENGTH_LONG).show();
                addTodayRewardCount();
                rewardedInterstitialAd = null;
            });
        } else {
            Toast.makeText(activity, "广告准备中，请稍后再试！", Toast.LENGTH_LONG).show();
            if (!isLoading) {
                load(activity);
                isTip = true;
            }
        }
    }

    // 当前奖励数
    public static long getAmount() {
        return DataStoreUtils.readLongValue(BaseApplication.context, AD_EARN, 0);
    }

    // 消费奖励
    public static boolean consumerAmount(long count) {
        long amount = getAmount();
        if (amount >= count) {
            amount = amount - count;
        } else {
            return false;
        }
        DataStoreUtils.saveLongValue(BaseApplication.context, AD_EARN, amount);
        return true;
    }
}
