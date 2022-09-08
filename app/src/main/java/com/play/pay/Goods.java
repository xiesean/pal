package com.play.pay;

import java.util.ArrayList;
import java.util.List;

public final class Goods {
    // 加钱
    public static  final String ITEM_MONEY = "money_add_10000";
    // 存档
    public static  final String ITEM_FLY = "mod_fly";
    //去广告
    public static  final String ITEM_NO_AD = "no_ad";
    // 升级
    public static  final String ITEM_LEVEL_UP = "level_up";

    public static  final List<String> skus = new ArrayList<>();

    static {
        skus.add(ITEM_MONEY);
        skus.add(ITEM_FLY);
        skus.add(ITEM_NO_AD);
        skus.add(ITEM_LEVEL_UP);
    }
}
