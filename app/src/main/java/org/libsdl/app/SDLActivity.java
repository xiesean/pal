package org.libsdl.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.admob.DataStoreUtils;
import com.admob.GAD;

import org.sean.pal95.R;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


/**
 * SDL Activity
 */
public class SDLActivity extends Activity {
    private static final String TAG = SDLActivity.class.getName();
    // Keep track of the paused state
    public static boolean mIsPaused;
    public static boolean mIsPausedMusic = false;

    public static boolean neverShowReward;
    // Main components
    public static SDLActivity mSingleton;
    // Messages from the SDLMain thread
    static int COMMAND_CHANGE_TITLE = 1;
    private static SDLSurface mSurface;
    // This is what SDL runs in. It invokes SDL_main(), eventually
    private static Thread mSDLThread;
    // Audio
    private static Thread mAudioThread;
    private static AudioTrack mAudioTrack;
    // EGL private objects
    private static EGLContext mEGLContext;
    private static EGLSurface mEGLSurface;
    private static EGLDisplay mEGLDisplay;
    private static EGLConfig mEGLConfig;
    private static int mGLMajor, mGLMinor;

    private static void vibrate(View view) {
        if (SettingsActivity.isFeedback()) {
            int flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, flags);
        }
    }

    private static final Runnable m_runableShowJS = new Runnable() {
        public void run() {
            if (mSingleton.m_joystick == null) {
                mSingleton.m_joystick = new Joystick(mSingleton);
                FrameLayout.LayoutParams paramsJS = new FrameLayout.LayoutParams(
                        Joystick.JOYSTICK_WIDTH * mSingleton.m_density / 160,
                        Joystick.JOYSTICK_HEIGHT * mSingleton.m_density / 160);
                paramsJS.bottomMargin = 50;
                paramsJS.leftMargin = 100;
                paramsJS.gravity = Gravity.BOTTOM | Gravity.LEFT;
                mSingleton.addContentView(mSingleton.m_joystick, paramsJS);
            }
            mSingleton.m_joystick.setVisibility(View.VISIBLE);
        }
    };
    private static final Runnable m_runableHideJS = new Runnable() {
        public void run() {
            mSingleton.m_joystick.setVisibility(View.INVISIBLE);
        }
    };
    private static final Runnable m_runableShowBack = new Runnable() {
        public void run() {
            if (mSingleton.m_btnBack == null) {
                mSingleton.m_btnBack = new Button(mSingleton);
                mSingleton.m_btnBack.setBackgroundResource(R.drawable.ic_mine);
                mSingleton.m_btnBack.getBackground().setAlpha(128);
                FrameLayout.LayoutParams paramsBack = new FrameLayout.LayoutParams(
                        40 * mSingleton.m_density / 160, 40 * mSingleton.m_density / 160);
                paramsBack.topMargin = 20;
                paramsBack.leftMargin = 20;
                paramsBack.gravity = Gravity.TOP | Gravity.LEFT;

                mSingleton.addContentView(mSingleton.m_btnBack, paramsBack);
                mSingleton.m_btnBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        vibrate(v);
                        SDLActivity.nativeBack();
                    }
                });
            }

            mSingleton.m_btnBack.setVisibility(View.VISIBLE);
        }
    };
    private static final Runnable m_runableHideBack = new Runnable() {
        public void run() {
            mSingleton.m_btnBack.setVisibility(View.INVISIBLE);
        }
    };
    private static final Runnable m_runableShowSearch = new Runnable() {
        public void run() {
            if (mSingleton.m_btnSearch == null) {
                mSingleton.m_btnSearch = new Button(mSingleton);
                mSingleton.m_btnSearch.setBackgroundResource(R.drawable.ic_search);
                mSingleton.m_btnSearch.getBackground().setAlpha(128);
                FrameLayout.LayoutParams paramsSearch = new FrameLayout.LayoutParams(
                        50 * mSingleton.m_density / 160, 50 * mSingleton.m_density / 160);
                paramsSearch.bottomMargin = (Math.min(SDLActivity.width, SDLActivity.height)) / 4;
                paramsSearch.rightMargin = 140;
                paramsSearch.gravity = Gravity.BOTTOM | Gravity.RIGHT;

                mSingleton.addContentView(mSingleton.m_btnSearch, paramsSearch);
                mSingleton.m_btnSearch.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        KeyUtils.pressKey(KeyEvent.KEYCODE_SPACE, () -> mSingleton.runOnUiThread(SDLActivity::nativeSearch));
                        vibrate(v);
                    }
                });
            }
            mSingleton.m_btnSearch.setVisibility(View.VISIBLE);
        }
    };
    private static final Runnable m_runableHideSearch = new Runnable() {
        public void run() {
            mSingleton.m_btnSearch.setVisibility(View.INVISIBLE);
        }
    };
    // Audio
    private static Object buf;

    // Load the .so
    static {
        System.loadLibrary("SDL2");
        //System.loadLibrary("SDL2_image");
        //System.loadLibrary("SDL2_mixer");
        System.loadLibrary("SDL_ttf");
        System.loadLibrary("sdlpal");
    }

    public Joystick m_joystick;
    public Button m_btnBack;
    public Button m_btnSearch;
    public boolean m_isJoystickShow = true;
    public int m_density;
    static int width;
    static int height;
    // Handler for the messages
    Handler commandHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 == COMMAND_CHANGE_TITLE) {
                setTitle((String) msg.obj);
            }
        }
    };

    public static void showJoystick() {
        if (!mSingleton.m_isJoystickShow) {
            return;
        }
        Log.i(TAG, "showJoystick");
        mSingleton.runOnUiThread(m_runableShowJS);
    }

    public static void hideJoystick() {
        Log.i(TAG, "hideJoystick");
        if (mSingleton.m_joystick != null) {
            mSingleton.runOnUiThread(m_runableHideJS);
        }
    }

    public static void showBackButton() {
        mSingleton.runOnUiThread(m_runableShowBack);
    }

    public static void hideBackButton() {
        if (mSingleton.m_btnBack != null) {
            //mSingleton.runOnUiThread(m_runableHideBack);
        }
    }

    public static void showSearchButton() {
        mSingleton.runOnUiThread(m_runableShowSearch);

    }

    public static void hideSearchButton() {
//        if (mSingleton.m_btnSearch != null) {
//            mSingleton.runOnUiThread(m_runableHideSearch);
//        }
    }

    public static native void nativeSetGameDir(String dir);

    // C functions we call
    public static native void nativeSetSize(int width, int height);

    public static native void nativeInit();

    public static native void nativeQuit();

    public static native void nativePause();

    public static native void nativeResume();

    public static native void nativePauseGame();

    public static native void nativeResumeGame();

    public static native void onNativeResize(int x, int y, int format);

    public static native void onNativeKeyDown(int keycode);

    public static native void onNativeKeyUp(int keycode);

    public static native void onNativeTouch(int touchDevId, int pointerFingerId,
                                            int action, float x,
                                            float y, float p);

    public static native void onNativeAccel(float x, float y, float z);

    public static native void nativeRunAudioThread();

    public static native void nativeDirChange(int dir);

    public static native void nativeSearch();

    public static native void nativeBack();

    public static native void nativeHack(int type, int value);

    public static native void nativeChangeBattleMode(int isClassic);

    public static native void nativeChangeJoystick(int use);

    public static native int nativeGetFlyMode();

    // Java functions called from C
    public static boolean createGLContext(int majorVersion, int minorVersion) {
        return initEGL(majorVersion, minorVersion);
    }

    public static void flipBuffers() {
        if (!SDLActivity.mIsPaused) {
            flipEGL();
        }
    }

    public static void setActivityTitle(String title) {
        // Called from SDLMain() thread and can't directly affect the view
        mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
    }

    public static Context getContext() {
        return mSingleton;
    }

    public static void startApp() {
        // Start up the C app thread
        if (mSDLThread == null) {
            mSDLThread = new Thread(new SDLMain(), "SDLThread");
            mSDLThread.start();
        } else {
            /*
             * Some Android variants may send multiple surfaceChanged events, so we don't need to resume every time
             * every time we get one of those events, only if it comes after surfaceDestroyed
             */
            if (mIsPaused) {
                //           	SDLActivity.createEGLSurface();
                SDLActivity.nativeResume();
                SDLActivity.mIsPaused = false;
            }
        }
    }

    // EGL functions
    public static boolean initEGL(int majorVersion, int minorVersion) {
        if (SDLActivity.mEGLDisplay == null) {
            //Log.v("SDL", "Starting up OpenGL ES " + majorVersion + "." + minorVersion);

            try {
                EGL10 egl = (EGL10) EGLContext.getEGL();

                EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

                int[] version = new int[2];
                egl.eglInitialize(dpy, version);

                int EGL_OPENGL_ES_BIT = 1;
                int EGL_OPENGL_ES2_BIT = 4;
                int renderableType = 0;
                if (majorVersion == 2) {
                    renderableType = EGL_OPENGL_ES2_BIT;
                } else if (majorVersion == 1) {
                    renderableType = EGL_OPENGL_ES_BIT;
                }
                int[] configSpec = {
                        //EGL10.EGL_DEPTH_SIZE,   16,
                        EGL10.EGL_RENDERABLE_TYPE, renderableType,
                        EGL10.EGL_NONE
                };
                EGLConfig[] configs = new EGLConfig[1];
                int[] num_config = new int[1];
                if (!egl.eglChooseConfig(dpy, configSpec, configs, 1, num_config) || num_config[0] == 0) {
                    Log.e("SDL", "No EGL config available");
                    return false;
                }
                EGLConfig config = configs[0];

                /*int EGL_CONTEXT_CLIENT_VERSION=0x3098;
                int contextAttrs[] = new int[] { EGL_CONTEXT_CLIENT_VERSION, majorVersion, EGL10.EGL_NONE };
                EGLContext ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, contextAttrs);

                if (ctx == EGL10.EGL_NO_CONTEXT) {
                    Log.e("SDL", "Couldn't create context");
                    return false;
                }
                SDLActivity.mEGLContext = ctx;*/
                SDLActivity.mEGLDisplay = dpy;
                SDLActivity.mEGLConfig = config;
                SDLActivity.mGLMajor = majorVersion;
                SDLActivity.mGLMinor = minorVersion;

                SDLActivity.createEGLSurface();
            } catch (Exception e) {
                Log.v("SDL", e + "");
                for (StackTraceElement s : e.getStackTrace()) {
                    Log.v("SDL", s.toString());
                }
            }
        } else SDLActivity.createEGLSurface();

        return true;
    }

    public static String getGameDir() {
        return mSingleton.getExternalFilesDir("").getAbsolutePath();
    }

    public static boolean createEGLContext() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] contextAttrs = new int[]{EGL_CONTEXT_CLIENT_VERSION, SDLActivity.mGLMajor, EGL10.EGL_NONE};
        SDLActivity.mEGLContext = egl.eglCreateContext(SDLActivity.mEGLDisplay, SDLActivity.mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttrs);
        if (SDLActivity.mEGLContext == EGL10.EGL_NO_CONTEXT) {
            Log.e("SDL", "Couldn't create context");
            return false;
        }
        return true;
    }

    public static boolean createEGLSurface() {
        if (SDLActivity.mEGLDisplay != null && SDLActivity.mEGLConfig != null) {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            if (SDLActivity.mEGLContext == null) createEGLContext();

//            Log.v("SDL", "Creating new EGL Surface");
            EGLSurface surface = egl.eglCreateWindowSurface(SDLActivity.mEGLDisplay, SDLActivity.mEGLConfig, SDLActivity.mSurface, null);
            if (surface == EGL10.EGL_NO_SURFACE) {
                Log.e("SDL", "Couldn't create surface");
                return false;
            }

            if (egl.eglGetCurrentContext() != SDLActivity.mEGLContext) {
                if (!egl.eglMakeCurrent(SDLActivity.mEGLDisplay, surface, surface, SDLActivity.mEGLContext)) {
                    Log.e("SDL", "Old EGL Context doesnt work, trying with a new one");
                    // TODO: Notify the user via a message that the old context could not be restored, and that textures need to be manually restored.
                    createEGLContext();
                    if (!egl.eglMakeCurrent(SDLActivity.mEGLDisplay, surface, surface, SDLActivity.mEGLContext)) {
                        Log.e("SDL", "Failed making EGL Context current");
                        return false;
                    }
                }
            }
            SDLActivity.mEGLSurface = surface;
            return true;
        }
        return false;
    }

    // EGL buffer flip
    public static void flipEGL() {
        try {
            EGL10 egl = (EGL10) EGLContext.getEGL();

            egl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null);

            // drawing here

            egl.eglWaitGL();

            egl.eglSwapBuffers(SDLActivity.mEGLDisplay, SDLActivity.mEGLSurface);


        } catch (Exception e) {
            Log.v("SDL", "flipEGL(): " + e);
            for (StackTraceElement s : e.getStackTrace()) {
                Log.v("SDL", s.toString());
            }
        }
    }

    public static Object audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

        //       Log.v("SDL", "SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + ((float)sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);

        audioStartThread();

//        Log.v("SDL", "SDL audio: got " + ((mAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + ((float)mAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        if (is16Bit) {
            buf = new short[desiredFrames * (isStereo ? 2 : 1)];
        } else {
            buf = new byte[desiredFrames * (isStereo ? 2 : 1)];
        }
        return buf;
    }

    public static void audioStartThread() {
        mAudioThread = new Thread(new Runnable() {
            public void run() {
                mAudioTrack.play();
                nativeRunAudioThread();
            }
        });

        // I'd take REALTIME if I could get it!
        mAudioThread.setPriority(Thread.MAX_PRIORITY);
        mAudioThread.start();
    }

    public static void audioWriteShortBuffer(short[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w("SDL", "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    public static void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w("SDL", "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    public static void audioQuit() {
        if (mAudioThread != null) {
            try {
                mAudioThread.join();
            } catch (Exception e) {
                Log.v("SDL", "Problem stopping audio thread: " + e);
            }
            mAudioThread = null;

            //Log.v("SDL", "Finished waiting for audio thread");
        }

        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    // Setup
    protected void onCreate(Bundle savedInstanceState) {
        //       Log.v("SDL", "onCreate()");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        width = getResources().getDisplayMetrics().widthPixels;
        height = getResources().getDisplayMetrics().heightPixels;
        m_density = getResources().getDisplayMetrics().densityDpi;
        // So we can call stuff from static callbacks
        mSingleton = this;

        // Keep track of the paused state
        mIsPaused = false;

        // Set up the surface
        mSurface = new SDLSurface(getApplication());
        setContentView(mSurface);

        // 辅助功能
        Button funHelp = new Button(this);
        funHelp.setBackgroundResource(R.drawable.ic_fun);
        funHelp.getBackground().setAlpha(128);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                40 * m_density / 160, 40 * m_density / 160);
        params.topMargin = 20;
        params.rightMargin = 20 * m_density / 160;
        params.gravity = Gravity.TOP | Gravity.RIGHT;

        addContentView(funHelp, params);
        funHelp.setOnClickListener(v -> {
            vibrate(v);
            startActivity(new Intent(SDLActivity.this, SettingsActivity.class));
        });

        // 攻略
        Button glHelp = new Button(this);
        glHelp.setBackgroundResource(R.drawable.ic_gl);
        glHelp.getBackground().setAlpha(128);
        FrameLayout.LayoutParams glParams = new FrameLayout.LayoutParams(
                40 * m_density / 160, 40 * m_density / 160);
        glParams.topMargin = 20;
        glParams.rightMargin = 100 * m_density / 160;
        glParams.gravity = Gravity.TOP | Gravity.RIGHT;

        addContentView(glHelp, glParams);
        glHelp.setOnClickListener(v -> {
            vibrate(v);
            startActivity(new Intent(SDLActivity.this, GlActivity.class));
        });

        //        GAD.showbannerT(this);
    }

    public static void rewordAd(int exp, int money) {
        Log.e(TAG, "Exp : " + exp + " Money : " + money);
        mSingleton.runOnUiThread(new Runnable() {
            public void run() {
                if (neverShowReward) {
                    return;
                }
                boolean isLoad = GAD.loadReward(mSingleton);
                if (!isLoad) {
                    return;
                }
                int p = (int) (Math.random() * 10);
                if (p < 1) {
                    return;
                }

                String msg = "观看广告可以获得三倍经验和三倍金币奖励";
                AlertDialog dialog = new AlertDialog.Builder(mSingleton)
                        .setCancelable(false)
                        .setTitle("三倍奖励")
                        .setMessage(msg)
                        .setPositiveButton("不再显示", (dialog1, which) -> {
                            neverShowReward = true;
                        })
                        .setNeutralButton("观看", (dialog1, which) -> {
                            dialog1.cancel();
                            // 经验加成
                            GAD.showReward(SDLActivity.mSingleton, () -> {
                                nativeHack(3, exp * 2);
                                nativeHack(4, money * 2);
                            });
                        })
                        .setNegativeButton("取消",
                                (dialog1, which) -> dialog1.cancel())
                        .create();
                dialog.show();
            }
        });
    }

    public void onClickAd() {
    }

    public void onFailedReceiveAd() {
    }

    public void onReceiveAd() {
    }

    public void onCloseMogoDialog() {
    }

    public void onCloseAd() {
    }

    public void onRequestAd() {
    }

    public void onRealClickAd() {
    }

    // Events
    protected void onPause() {
        SDLActivity.nativePauseGame();

        if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.pause();
            mIsPausedMusic = true;
        }
        super.onPause();
        // Don't call SDLActivity.nativePause(); here, it will be called by SDLSurface::surfaceDestroyed
        neverShowReward = false;
    }

    protected void onResume() {
        //       Log.v("SDL", "onResume()");
        SDLActivity.nativeResumeGame();

        m_isJoystickShow = DataStoreUtils.readLongValue(SDLActivity.this, DataStoreUtils.SP_JOYSTICK, 1) == 1;
        nativeChangeJoystick(m_isJoystickShow ? 1 : 0);

        if (mIsPausedMusic && mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
            mAudioTrack.play();
        }
        super.onResume();
        // Don't call SDLActivity.nativeResume(); here, it will be called via SDLSurface::surfaceChanged->SDLActivity::startApp

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Send a quit message to the application
        SDLActivity.nativeQuit();

        System.exit(0);
    }

    // Send a message from the SDLMain thread
    void sendCommand(int command, Object data) {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage(msg);
    }
}

/**
 * Simple nativeInit() runnable
 */
class SDLMain implements Runnable {
    public void run() {
        // Runs SDL_main()
        SDLActivity.nativeInit();

        //Log.v("SDL", "SDL thread terminated");
    }
}


/**
 * SDLSurface. This is what we draw on, so we need to know when it's created
 * in order to do anything useful.
 * <p>
 * Because of this, that's where we set up the SDL thread
 */
class SDLSurface extends SurfaceView implements SurfaceHolder.Callback,
        View.OnKeyListener, View.OnTouchListener, SensorEventListener {

    // Sensors
    private static SensorManager mSensorManager;
    private int m_lastKeyPressed = 0;
    private float m_lastMoveX = 0;
    private float m_lastMoveY = 0;

    // Startup
    @SuppressLint("WrongConstant")
    public SDLSurface(Context context) {
        super(context);
        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        setOnKeyListener(this);
        setOnTouchListener(this);

        mSensorManager = (SensorManager) context.getSystemService("sensor");
    }

    // Called when we have a valid drawing surface
    public void surfaceCreated(SurfaceHolder holder) {
//        Log.v("SDL", "surfaceCreated()");
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
        enableSensor(Sensor.TYPE_ACCELEROMETER, true);
    }

    // Called when we lose the surface
    public void surfaceDestroyed(SurfaceHolder holder) {
//        Log.v("SDL", "surfaceDestroyed()");
        if (!SDLActivity.mIsPaused) {
            SDLActivity.mIsPaused = true;
            SDLActivity.nativePause();
        }
        enableSensor(Sensor.TYPE_ACCELEROMETER, false);
    }

    // Called when the surface is resized
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height) {
        //       Log.v("SDL", "surfaceChanged()");

        int sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565 by default
        switch (format) {
            case PixelFormat.A_8:
                Log.v("SDL", "pixel format A_8");
                break;
            case PixelFormat.LA_88:
                Log.v("SDL", "pixel format LA_88");
                break;
            case PixelFormat.L_8:
                Log.v("SDL", "pixel format L_8");
                break;
            case PixelFormat.RGBA_4444:
                Log.v("SDL", "pixel format RGBA_4444");
                sdlFormat = 0x85421002; // SDL_PIXELFORMAT_RGBA4444
                break;
            case PixelFormat.RGBA_5551:
                Log.v("SDL", "pixel format RGBA_5551");
                sdlFormat = 0x85441002; // SDL_PIXELFORMAT_RGBA5551
                break;
            case PixelFormat.RGBA_8888:
                Log.v("SDL", "pixel format RGBA_8888");
                sdlFormat = 0x86462004; // SDL_PIXELFORMAT_RGBA8888
                break;
            case PixelFormat.RGBX_8888:
                Log.v("SDL", "pixel format RGBX_8888");
                sdlFormat = 0x86262004; // SDL_PIXELFORMAT_RGBX8888
                break;
            case PixelFormat.RGB_332:
                Log.v("SDL", "pixel format RGB_332");
                sdlFormat = 0x84110801; // SDL_PIXELFORMAT_RGB332
                break;
            case PixelFormat.RGB_565:
                Log.v("SDL", "pixel format RGB_565");
                sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565
                break;
            case PixelFormat.RGB_888:
                Log.v("SDL", "pixel format RGB_888");
                // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
                sdlFormat = 0x86161804; // SDL_PIXELFORMAT_RGB888
                break;
            default:
                Log.v("SDL", "pixel format unknown " + format);
                break;
        }
        SDLActivity.onNativeResize(width, height, sdlFormat);
        SDLActivity.nativeSetSize(width, height);
        //       Log.v("SDL", "Window size:" + width + "x"+height);

        SDLActivity.startApp();
    }

    // unused
    public void onDraw(Canvas canvas) {
    }

    // Key events
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return super.onKeyDown(keyCode, event);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                return super.onKeyUp(keyCode, event);
            }
        }

        // 这些按键不需要repeat
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (m_lastKeyPressed == keyCode &&
                    (KeyEvent.KEYCODE_MENU == keyCode
                            || KeyEvent.KEYCODE_BACK == keyCode
                            || KeyEvent.KEYCODE_PLUS == keyCode
                            || KeyEvent.KEYCODE_MINUS == keyCode)) {
                return true;
            }

            m_lastKeyPressed = keyCode;
            //           Log.v("SDL", "key down: " + keyCode);
            SDLActivity.onNativeKeyDown(keyCode);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            //           Log.v("SDL", "key up: " + keyCode);
            m_lastKeyPressed = 0;

            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK: {
                    Dialog dialog = new AlertDialog.Builder(SDLActivity.mSingleton).setTitle("PAL").setMessage("是否退出游戏？")
                            .setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            SDLActivity.mSingleton.finish();
                                            dialog.cancel();
                                        }
                                    }).setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    }).create();// 创建按钮
                    dialog.show();
                    return true;
                }
                case KeyEvent.KEYCODE_HOME:
                    // 此处无法截获
                    break;
            }
            SDLActivity.onNativeKeyUp(keyCode);
            return true;
        }

        return false;
    }

    // Touch events
    public boolean onTouch(View v, MotionEvent event) {
        {
            final int touchDevId = event.getDeviceId();
            final int pointerCount = event.getPointerCount();
            // touchId, pointerId, action, x, y, pressure
            int actionPointerIndex = event.getActionIndex();
            int pointerFingerId = event.getPointerId(actionPointerIndex);
            int action = event.getActionMasked();

            float x = event.getX(actionPointerIndex);
            float y = event.getY(actionPointerIndex);
            float p = event.getPressure(actionPointerIndex);

//             if (true) {
            if (action == MotionEvent.ACTION_MOVE) {
                if (Math.abs(m_lastMoveX - x) < 0.01 && (Math.abs(m_lastMoveY - y) < 0.01)) {
                    return true;
                }
                m_lastMoveX = x;
                m_lastMoveY = y;
            }

            if (action == MotionEvent.ACTION_MOVE && pointerCount > 1) {
                // TODO send motion to every pointer if its position has
                // changed since prev event.
                for (int i = 0; i < pointerCount; i++) {
                    pointerFingerId = event.getPointerId(i);
                    x = event.getX(i);
                    y = event.getY(i);
                    p = event.getPressure(i);
                    SDLActivity.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p);
                }
            } else {
                SDLActivity.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p);
            }
        }
        return true;
    }

    // Sensor events
    public void enableSensor(int sensortype, boolean enabled) {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if (enabled) {
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(sensortype),
                    SensorManager.SENSOR_DELAY_GAME, null);
        } else {
            mSensorManager.unregisterListener(this,
                    mSensorManager.getDefaultSensor(sensortype));
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            SDLActivity.onNativeAccel(event.values[0] / SensorManager.GRAVITY_EARTH,
                    event.values[1] / SensorManager.GRAVITY_EARTH,
                    event.values[2] / SensorManager.GRAVITY_EARTH);
        }
    }

}

