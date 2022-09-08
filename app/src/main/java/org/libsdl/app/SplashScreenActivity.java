package org.libsdl.app;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.BaseApplication;
import com.ad.admob.AdvertisingIdClient;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.AriaConfig;
import com.arialyy.aria.core.download.DownloadTaskListener;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.NetUtils;
import com.util.ZipUtil;

import org.sean.pal95.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SplashScreenActivity extends AppCompatActivity implements DownloadTaskListener {
    public static final String[] DOWNLOAD_URL = new String[]{
            "https://raw.githubusercontent.com/hsoftxl/data/main/sdlpal.zip"
    };
    public static final String tip = "正在下载游戏数据...  ";
    private static final AtomicInteger atom = new AtomicInteger(0);
    private static final String TAG = SplashScreenActivity.class.getSimpleName();
    public static byte m_dataVersion = 1;
    public static String gameDir;
    public static String gameData;
    private static int index = 0;
    private final int PERMISSION = 123;
    private long downloadTaskId;

    private AlertDialog downloadingDialog;
    private AlertDialog zipDialog;
    private AlertDialog tipDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);
        initPermission();

        gameDir = getGamePath();
        gameData = gameDir + "data.zip";

        SDLActivity.nativeSetGameDir(gameDir);

        dataPre();
        if (!isNeedUpdateData()) {
            startGame();
        } else {
            Aria.download(this).register();
            showTipDialog();
        }
        AdvertisingIdClient.printGoogleAdId(this);
    }

    private void startGame() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startActivity(new Intent(SplashScreenActivity.this, SDLActivity.class));
                finish();
            }
        }.start();
    }

    private AlertDialog showDialog(String content, String posButton, DialogInterface.OnClickListener listener) {
        cancelDialogs();
        if (isFinishing()) {
            return null;
        }
        AlertDialog dialog = new AlertDialog.Builder(SplashScreenActivity.this)
                .setCancelable(false)
                .setMessage(content)
                .setTitle(R.string.data_download)
                .setPositiveButton(posButton, listener)
                .setNegativeButton("取消",
                        (dialog12, which) -> {
                            dialog12.cancel();
                            finish();
                        })
                .create();
        showDig(dialog);
        return dialog;
    }

    private void showTipDialog() {
        if (isFinishing()) {
            return;
        }
        cancelDialogs();
        tipDialog = showDialog("因版权问题，本软件不提供游戏数据包，需要您下载百游官方提供的游戏数据包，请确认！",
                "确定",
                (dialog1, whichButton) -> {
                    if (!NetUtils.isConnected(AriaConfig.getInstance().getAPP())) {
                        ALog.e(TAG, "启动任务失败，网络未连接");
                        showNetDialog();
                        return;
                    }
                    download(getUrlIndex());
                    showWaitingDialog();
                });
    }

    private void showNetDialog() {
        cancelDialogs();
        if (isFinishing()) {
            return;
        }
        tipDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("网络不可用,请先开启网络... ")
                .setNegativeButton("取消",
                        (dialog12, which) -> {
                            dialog12.cancel();
                            finish();
                        })
                .setPositiveButton("重试",
                        (dialog12, which) -> {
                            if (NetUtils.isConnected(AriaConfig.getInstance().getAPP())) {
                                download(getUrlIndex());
                                showWaitingDialog();
                            } else {
                                showNetDialog();
                            }
                        })
                .create();
        showDig(tipDialog);
    }

    private int getUrlIndex() {
        return index;
    }

    private void showWaitingDialog() {
        cancelDialogs();
        if (isFinishing()) {
            return;
        }
        tipDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("正在等待下载... ")
                .setNegativeButton("取消",
                        (dialog12, which) -> {
                            dialog12.cancel();
                            finish();
                        })
                .create();
        showDig(tipDialog);
    }

    private void showDownloadDialog() {
        if (isFinishing()) {
            return;
        }
        cancelDialogs();
        downloadingDialog = showDialog(tip, getUrlIndex() == 0 ? "国外分流" : "国内分流", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Aria.download(this).load(downloadTaskId).stop();
                download(getNextIndex());
                showDownloadDialog();
                runOnUiThread(() -> {
                    Toast.makeText(SplashScreenActivity.this, "切换成功", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void download(int index) {
        downloadTaskId = Aria.download(this)
                .load(DOWNLOAD_URL[index])     //读取下载地址
                .setFilePath(gameData) //设置文件保存的完整路径
                .ignoreFilePathOccupy()
                .create();   //创建并启动下载
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), PERMISSION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Arrays.asList(permissions).contains(WRITE_EXTERNAL_STORAGE)) {
                        showTipDialog();
                    }
                    //initGameData();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(SplashScreenActivity.this, "无权限不能进行游戏!!!!!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(SplashScreenActivity.this, "无权限不能进行游戏!!!!!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }


    public static String getGamePath() {
        return BaseApplication.context.getExternalFilesDir(null).getAbsolutePath() + "/sdlpal/";
    }

    public static String getGameFile(String name) {
        return getGamePath() + name;
    }

    public boolean isNeedUpdateData() {
        try {
            InputStream checkFile = new FileInputStream(getGameFile("dataflag.cfg"));
            int data = checkFile.read();
            checkFile.close();
            if (data < m_dataVersion) {
                return true;
            }
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void initGameData() {
        Thread thread = new Thread(() -> {
            updateData();
            runOnUiThread(() -> {
                cancelDialogs();
                startActivity(new Intent(SplashScreenActivity.this, SDLActivity.class));
                finish();
            });
        });
        thread.start();
    }

    public void updateData() {
        AssetManager amgr = getAssets();
        boolean updateOk = true;
        try {
            ZipUtil.UnZipFolder(gameData, gameDir);
        } catch (Exception e) {
            e.printStackTrace();
            updateOk = false;
        }

        // 更新成功，写入新的版本号
        if (updateOk) {
            try {
                File checkFile = new File(getGameFile("dataflag.cfg"));
                if (!checkFile.exists()) {
                    checkFile.createNewFile();
                }
                OutputStream output = new FileOutputStream(checkFile);
                output.write(m_dataVersion);
                output.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void dataPre() {
        File file = new File(gameDir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    public void onWait(DownloadTask task) {
        Log.i(TAG, "onWait.......");
        runOnUiThread(this::showWaitingDialog);
    }

    @Override
    public void onPre(DownloadTask task) {
        Log.i(TAG, "onPre.......");
    }

    @Override
    public void onTaskPre(DownloadTask task) {
        Log.i(TAG, "onTaskPre.......");

    }

    @Override
    public void onTaskResume(DownloadTask task) {
        Log.i(TAG, "onTaskResume.......");
        runOnUiThread(this::showDownloadDialog);
    }

    @Override
    public void onTaskStart(DownloadTask task) {
        Log.i(TAG, "onTaskStart.......");
        runOnUiThread(this::showDownloadDialog);
    }

    @Override
    public void onTaskStop(DownloadTask task) {
        Log.i(TAG, "onTaskStop.......");

        runOnUiThread(() -> tipDialog = showDialog("下载失败...",
                "重试",
                (dialog1, whichButton) -> {
                    download(getUrlIndex());
                    showDownloadDialog();
                }));
    }

    @Override
    public void onTaskCancel(DownloadTask task) {
        Log.i(TAG, "onTaskCancel.......");
        runOnUiThread(() -> tipDialog = showDialog("下载失败...",
                "重试",
                (dialog1, whichButton) -> {
                    download(getUrlIndex());
                    showDownloadDialog();
                }));
    }

    @Override
    public void onTaskFail(DownloadTask task, Exception e) {
        Log.i(TAG, "onTaskFail.......");
        runOnUiThread(() -> tipDialog = showDialog("下载失败...",
                "重试",
                (dialog1, whichButton) -> {
                    download(getUrlIndex());
                    showDownloadDialog();
                }));
    }

    @Override
    public void onTaskComplete(DownloadTask task) {
        Log.i(TAG, "onTaskComplete.......");
        runOnUiThread(() -> {
            cancelDialogs();
            if (isFinishing()) {
                return;
            }
            zipDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("解压数据中... ")
                    .setNegativeButton("取消",
                            (dialog12, which) -> {
                                dialog12.cancel();
                                finish();
                            })
                    .create();
            showDig(zipDialog);
        });
        initGameData();
    }

    private void showDig(AlertDialog dig) {
        if (dig != null && !dig.isShowing()) {
            if (getMainLooper().getThread() == Thread.currentThread()) {
                dig.show();
            } else {
                runOnUiThread(dig::show);
            }
        }
    }

    @Override
    public void onTaskRunning(DownloadTask task) {
        Log.i(TAG, "onTaskRunning.......");

        if (task.getKey().equals(DOWNLOAD_URL)) {
            //    可以通过url判断是否是指定任务的回调
        }
        int p = task.getPercent();  //任务进度百分比
        String speed = task.getConvertSpeed();  //转换单位后的下载速度，单位转换需要在配置文件中打开
        long speed1 = task.getSpeed(); //原始byte长度速度
        Log.i("Download", "p:" + p + " speed:" + speed + "  speed1:" + speed1);
        if (downloadingDialog != null && downloadingDialog.isShowing()) {
            downloadingDialog.setMessage(tip + speed);
        }
    }

    @Override
    public void onNoSupportBreakPoint(DownloadTask task) {
        Log.i(TAG, "onNoSupportBreakPoint.......");

    }

    private int getNextIndex() {
        return index = (atom.addAndGet(1) % DOWNLOAD_URL.length);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void cancelDialogs() {
        if (tipDialog != null && tipDialog.isShowing()) {
            tipDialog.cancel();
        }
        if (downloadingDialog != null && downloadingDialog.isShowing()) {
            downloadingDialog.cancel();
        }
        if (zipDialog != null && zipDialog.isShowing()) {
            zipDialog.cancel();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelDialogs();
    }
}
