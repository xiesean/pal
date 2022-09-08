package com.play.pay;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.BaseApplication;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class PayUtil {
    private static final String TAG = "PayUtil";
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    // 支付结果集合
    private static final Map<String, String> purchasesMap = new HashMap<>();
    // SKU集合
    private static final Map<String, SkuDetails> skuMap = new HashMap<>();
    // 是否初始化完成
    private static boolean isInit = false;
    private static Consumer<String> consumer;

    // 消耗成功监听
    private static AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

        }
    };

    // 支付监听
    private static PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        // To be implemented in a later section.
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Toast.makeText(BaseApplication.context, "支付取消", Toast.LENGTH_LONG).show();
        } else {
            // Handle any other error codes.
            Toast.makeText(BaseApplication.context, "支付失败请重试", Toast.LENGTH_LONG).show();
        }
    };

    private static BillingClient billingClient = BillingClient.newBuilder(BaseApplication.context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build();

    // 交易确认
    private static void handlePurchase(Purchase purchase) {
        // Purchase retrieved from BillingClient#queryPurchases or your PurchasesUpdatedListener.
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            // 消耗品
            ConsumeParams consumeParams =
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
            purchasesMap.put(purchase.getPurchaseToken(), purchase.getSku());
            ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Handle the success of the consume operation.
                    // 交易完成， 给用户提供商品
                    consumer.accept(purchasesMap.get(purchaseToken));
                    //Toast.makeText(BaseApplication.context, "交易完成", Toast.LENGTH_LONG).show();
                }
            };

            billingClient.consumeAsync(consumeParams, listener);

//            // 非消耗品
//            if (!purchase.isAcknowledged()) {
//                AcknowledgePurchaseParams acknowledgePurchaseParams =
//                        AcknowledgePurchaseParams.newBuilder()
//                                .setPurchaseToken(purchase.getPurchaseToken())
//                                .build();
//                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
//            }


        }
    }

    public static void init(Consumer<String> consumer) {
        if (consumer == null) {
            Toast.makeText(BaseApplication.context, "未初始化", Toast.LENGTH_LONG).show();
            return;
        }
        PayUtil.consumer = consumer;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.i(TAG, billingResult.getResponseCode() + ":" + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    querySkus();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                executor.execute(() -> {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (count.getAndIncrement() < 10) {
                        init(consumer);
                    }
                });
            }
        });


    }

    private static void querySkus() {
        // 展示可供购买的商品
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(Goods.skus).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult, skuDetailsList) -> {
                    // Process the result.
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (SkuDetails details : skuDetailsList) {
                            skuMap.put(details.getSku(), details);
                        }
                        isInit = true;
                        Log.i(TAG, Arrays.deepToString(skuDetailsList.toArray()));
                    }
                });
    }

    public static void startPay(Activity activity, String sku) {
        startPay(activity, sku, null);
    }

    public static void startPay(Activity activity, String sku, String ext) {
        if (skuMap.get(sku) == null) {
            init(PayUtil.consumer);
        }

        Future<Boolean> future = executor.submit(() -> isInit);
        try {
            if (!future.get(30, TimeUnit.SECONDS)) {
                // 查询sku失败
                Toast.makeText(activity, "支付失败，请重试！", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 查询sku失败
            Toast.makeText(activity, "支付失败，请重试！", Toast.LENGTH_LONG).show();
            return;
        }

        SkuDetails skuDetails = skuMap.get(sku);
        if (skuDetails == null) {
            Toast.makeText(activity, "支付失败，请重试！", Toast.LENGTH_LONG).show();
            return;
        }
        // 查询是否有购买未消耗
        Purchase.PurchasesResult result = billingClient.queryPurchases(sku);
        if (result != null && result.getPurchasesList() != null && !result.getPurchasesList().isEmpty()) {
            for (Purchase purchase : result.getPurchasesList()) {
                handlePurchase(purchase);
            }
            return;
        }
        //启动购买流程
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
        // Handle the result.
        if (BillingClient.BillingResponseCode.OK == responseCode) {
            PayConsumer.fileName = ext;
            // 响应成功
        } else {
            Toast.makeText(activity, "支付失败，请重试！", Toast.LENGTH_LONG).show();
        }
    }
}
