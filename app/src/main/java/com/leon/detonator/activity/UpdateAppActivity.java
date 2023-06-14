package com.leon.detonator.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.download.DownloadService;
import com.leon.detonator.util.FilePath;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

public class UpdateAppActivity extends BaseActivity {
    public static final int UPDATE_HAS_NEW = 100;
    public static final int UPDATE_NO_NEW = 101;
    private final int UPDATE_PROGRESS = 4;
    private final int UPDATE_DOWNLOADING = 5;
    private final int UPDATE_DOWNLOAD_FAIL = 6;
    private TextView tvHints;
    private TextView tvPercentage;
    private ProgressBar pbDownload;
    private MyButton btnUpdate;
    private MyButton btnVersion;
    private UpdateVersionBean versionBean;
    private BaseApplication myApp;

    private final Handler refreshUI = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case UPDATE_HAS_NEW:
                    versionBean = (UpdateVersionBean) msg.obj;
                    if (versionBean.getVersion() != null) {
                        String[] version = versionBean.getVersion().split("\\.");
                        if (version.length == 3) {
                            try {
                                int code = Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2]);
                                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.split("\\.");
                                if (version.length == 3 && code > Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2])) {
                                    tvHints.setText(String.format(Locale.getDefault(), getString(R.string.update_found_new_version), versionBean.getVersion()));
                                    btnUpdate.setEnabled(true);
                                } else
                                    tvHints.setText(String.format(Locale.getDefault(), getString(R.string.update_is_new_version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        }
                    } else {
                        tvHints.setText(R.string.message_check_network);
                        myApp.myToast(UpdateAppActivity.this, R.string.message_update_get_version_error);
                    }
                    setProgressVisibility(false);
                    break;
                case UPDATE_NO_NEW:
                    try {
                        tvHints.setText(String.format(Locale.getDefault(), getString(R.string.update_is_new_version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                    setProgressVisibility(false);
                    break;
                case UPDATE_DOWNLOADING:
                    btnUpdate.setEnabled(false);
                    setProgressVisibility(true);
                    tvHints.setText(R.string.update_downloading);
                    break;
                case UPDATE_DOWNLOAD_FAIL:
                    setProgressVisibility(false);
                    tvHints.setText(R.string.update_download_fail);
                    break;
                case UPDATE_PROGRESS:
                    if (msg.arg1 < 100)
                        pbDownload.setProgress(msg.arg1);
                    else {
                        btnVersion.setEnabled(true);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(new File(String.format(Locale.getDefault(), FilePath.FILE_UPDATE_APK, versionBean.getVersion()))), "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", msg.arg1));
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_app);

        setTitle(R.string.settings_upgrade);
        setProgressVisibility(true);
        myApp = (BaseApplication) getApplication();
        tvHints = findViewById(R.id.tv_hints);
        tvPercentage = findViewById(R.id.tv_percentage);
        pbDownload = findViewById(R.id.pb_download);
        btnUpdate = findViewById(R.id.btn_update);
        btnVersion = findViewById(R.id.btn_version);
        pbDownload.setMax(100);
        tvPercentage.setText("0%");

        tvHints.setText(R.string.update_checking_version);
        File file = new File(FilePath.FILE_UPDATE_PATH);
        if ((!file.exists() && !file.mkdir()) || (file.exists() && !file.isDirectory() && file.delete() && !file.mkdir()))
            myApp.myToast(UpdateAppActivity.this, R.string.message_create_folder_fail);
        File[] files = new File(FilePath.FILE_UPDATE_PATH + "/").listFiles();
        btnVersion.setEnabled(files != null && files.length > 0);
        myApp.getVersion(refreshUI);
        btnUpdate.setEnabled(false);
        btnUpdate.setOnClickListener(v -> downloadApp());

        findViewById(R.id.btn_version).setOnClickListener(v -> startActivity(new Intent(UpdateAppActivity.this, VersionManageActivity.class)));
    }

    private void downloadApp() {
        if (BaseApplication.isNetSystemUsable(UpdateAppActivity.this)) {
            refreshUI.obtainMessage(UPDATE_DOWNLOADING).sendToTarget();
            try {
                new DownloadTask().start();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
                refreshUI.obtainMessage(UPDATE_DOWNLOAD_FAIL).sendToTarget();
            }
        } else
            myApp.myToast(UpdateAppActivity.this, R.string.message_check_network);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                if (btnUpdate.isEnabled())
                    downloadApp();
                break;
            case KeyEvent.KEYCODE_2:
                startActivity(new Intent(UpdateAppActivity.this, VersionManageActivity.class));
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private class DownloadTask extends Thread {
        private DownloadService loader;

        /**
         * 退出下载
         */
        public void exit() {
            if (loader != null) {
                loader.exit();
            }
        }

        @Override
        public void run() {
            try {
                String fileName = String.format(Locale.getDefault(), FilePath.FILE_UPDATE_APK, versionBean.getVersion());
                File file = new File(fileName);
                if (file.exists() && !file.delete()) {
                    myApp.myToast(UpdateAppActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delete_file_fail), file.getName()));
                }
                loader = new DownloadService(getApplicationContext(), versionBean.getUrl(), fileName);
                loader.download(true, size -> {
                    int fileSize = loader.getFileSize();
                    refreshUI.obtainMessage(UPDATE_PROGRESS, (int) (size * 100f / fileSize), fileSize).sendToTarget();
                });
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
                refreshUI.obtainMessage(UPDATE_DOWNLOAD_FAIL).sendToTarget();
            }
        }
    }
}
