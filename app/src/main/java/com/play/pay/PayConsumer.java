package com.play.pay;

import android.widget.Toast;

import androidx.core.util.Consumer;

import com.BaseApplication;
import com.admob.DataStoreUtils;
import com.admob.GAD;
import com.util.FileUtil;

import org.libsdl.app.SDLActivity;
import org.libsdl.app.SplashScreenActivity;

public class PayConsumer implements Consumer<String> {
    public static String KEY_STATE = "STATE_";
    public static String fileName = "";

    @Override
    public void accept(String s) {
        if (s == null) {
            return;
        }
        switch (s) {
            case Goods.ITEM_FLY:
                if (fileName != null && fileName.trim().length() > 0) {
                    loadState(fileName);
                    fileName = null;
                }
                break;
            case Goods.ITEM_LEVEL_UP:
                levelUp();
                break;
            case Goods.ITEM_MONEY:
                addMoney();
                break;
            case Goods.ITEM_NO_AD:
                GAD.setNoAd();
                Toast.makeText(BaseApplication.context, "已升级为专业版，马上去使用吧", Toast.LENGTH_LONG).show();
                break;
        }
    }

    public static void loadState(String file) {
        DataStoreUtils.saveLocalInfo(BaseApplication.context, KEY_STATE + file, DataStoreUtils.VALUE_TRUE);
        FileUtil.decodeFromAsses(BaseApplication.context, "state/" + file, SplashScreenActivity.getGamePath() + "1.rpg");
        Toast.makeText(BaseApplication.context, "载入存档1,即可跳对应主线", Toast.LENGTH_LONG).show();
    }

    public static void levelUp() {
        // 人物升级
        SDLActivity.nativeHack(1, 0);
        Toast.makeText(BaseApplication.context, "人物升级成功", Toast.LENGTH_LONG).show();
    }

    public static void addMoney() {
        // 加金钱
        SDLActivity.nativeHack(2, 0);
        Toast.makeText(BaseApplication.context, "加金钱成功", Toast.LENGTH_LONG).show();
    }
}
