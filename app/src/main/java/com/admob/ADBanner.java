package com.admob;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class ADBanner {

    public static final int POSITION_TOP = 1;
    public static final int POSITION_BOTTOM = 2;
    /**
     * 广告Layout
     */
    private static RelativeLayout adLayout;

    public static void addFloatingBanner(final Activity act, int position) {
        addFloatingBanner(act, position, null);
    }

    public static void addFloatingBanner(final Activity act, int position, ViewGroup viewGroup) {
        adLayout = new RelativeLayout(act);
        RelativeLayout.LayoutParams parentLayputParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (position == 1) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        }
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        AdView adView = createAdView(act);
        adLayout.setVisibility(View.VISIBLE);
        adLayout.addView(adView, layoutParams);

        if (viewGroup != null) {
            viewGroup.addView(adLayout, parentLayputParams);
        } else {
            act.addContentView(adLayout, parentLayputParams);
        }
        loadBanner(adView);
    }

    public static AdView createAdView(Activity act) {
        AdView adView = new AdView(act);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(GAD.UID_BANNER);
        return adView;
    }

    private static void loadBanner(AdView adView) {
        if (GAD.isNoAd()) {
            return;
        }
        // Create an ad request. Check logcat output for the hashed device ID to
        // get test ads on a physical device.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

}
