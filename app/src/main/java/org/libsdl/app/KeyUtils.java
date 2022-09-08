package org.libsdl.app;

import android.app.Instrumentation;
import android.util.Log;
import android.view.KeyEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyUtils {
    private static final String TAG = KeyUtils.class.getName();
    static ExecutorService executor = Executors.newSingleThreadExecutor();

    static Instrumentation inst = new Instrumentation();

    public static void pressKey(int key) {
        pressKey(key, null);
    }

    public static void pressKey(int key, Runnable runnable) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(key);
                } catch (Throwable e) {
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }
        });
    }

    public static void touchKey(int key, boolean isUp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "isUp: " + isUp);
                inst.sendKeySync(new KeyEvent(isUp ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN, key));
            }
        });
    }
}
