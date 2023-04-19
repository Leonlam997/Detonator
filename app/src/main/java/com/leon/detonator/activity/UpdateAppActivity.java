package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class UpdateAppActivity extends BaseActivity {
    private final int UPDATE_HAS_NEW = 1;
    private final int UPDATE_NOT_NEED = 2;
    private final int UPDATE_ERROR = 3;
    private final int UPDATE_PROGRESS = 4;
    private final int UPDATE_DOWNLOADING = 5;
    private final int UPDATE_DOWNLOAD_FAIL = 6;
    private TextView tvHints, tvPercentage;
    private ProgressBar pbDownload;
    private MyButton btnUpdate, btnVersion;
    private UpdateVersionBean versionBean;
    private BaseApplication myApp;
    private long fileSize;

    private Handler refreshUI = new Handler(new Handler.Callback() {
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
                                    tvHints.setText(String.format(Locale.CHINA, getString(R.string.update_found_new_version), versionBean.getVersion()));
                                    btnUpdate.setEnabled(true);
                                    setProgressVisibility(false);
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        }
                    }
                    break;
                case UPDATE_NOT_NEED:
                    try {
                        tvHints.setText(String.format(Locale.CHINA, getString(R.string.update_is_new_version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
                        setProgressVisibility(false);
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                        myApp.myToast(UpdateAppActivity.this, R.string.message_update_get_version_error);
                    }
                    break;
                case UPDATE_ERROR:
                    setProgressVisibility(false);
                    tvHints.setText(R.string.message_check_network);
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
                    if (msg.arg1 <= 100) {
                        pbDownload.setProgress(msg.arg1);
                        tvPercentage.setText(String.format(Locale.CHINA, "%d%%", msg.arg1));
                    }
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
        if ((!file.exists() && !file.mkdir()) || (file.exists() && !file.isDirectory() && file.delete() && !file.mkdir())) {
            myApp.myToast(UpdateAppActivity.this, R.string.message_create_folder_fail);
        }
        File[] files = new File(FilePath.FILE_UPDATE_PATH + "/").listFiles();
        btnVersion.setEnabled(files != null && files.length > 0);
        myApp.getVersion(refreshUI);
        btnUpdate.setEnabled(false);
        btnUpdate.setOnClickListener(v -> downloadApp());

        findViewById(R.id.btn_version).setOnClickListener(v -> startActivity(new Intent(UpdateAppActivity.this, VersionManageActivity.class)));
    }

    private void downloadApp() {
        Message msg = refreshUI.obtainMessage(UPDATE_DOWNLOADING);
        refreshUI.sendMessage(msg);
        new DownloadTask().execute(versionBean.getUrl());
//        if (BaseApplication.isNetSystemUsable(UpdateAppActivity.this))
//            new Thread(() -> {
//                OkHttpUtils.get()
//                        .url(versionBean.getUrl())
//                        .build().execute(new FileCallBack(FilePath.FILE_UPDATE_PATH, versionBean.getVersion() + ".apk") {
//                            private float lastProgress = -1;
//
//                            @Override
//                            public void onError(Call call, Exception e, int i) {
//                                BaseApplication.writeErrorLog(e);
//                                btnVersion.setEnabled(true);
//                                refreshUI.sendEmptyMessage(UPDATE_DOWNLOAD_FAIL);
//                            }
//
//                            @Override
//                            public void inProgress(float progress, long total, int id) {
//                                if (progress > lastProgress + 0.01) {
//                                    fileSize = total;
//                                    refreshUI.removeMessages(UPDATE_PROGRESS);
//                                    refreshUI.obtainMessage(UPDATE_PROGRESS, (int) (progress * 100), (int) total).sendToTarget();
//                                    lastProgress = progress;
//                                }
//                                super.inProgress(progress, total, id);
//                            }
//
//                            @Override
//                            public void onResponse(File file, int i) {
//                                //安装应用
//                                if (file.length() == fileSize) {
//                                    btnVersion.setEnabled(true);
//                                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
//                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    startActivity(intent);
//                                } else
//                                    refreshUI.sendEmptyMessage(UPDATE_DOWNLOAD_FAIL);
//                            }
//                        });
//            }).start();
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

    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private PowerManager.WakeLock mWakeLock;

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            File file;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                //设置请求方式（一般提交数据使用POST，获取数据使用GET）
                connection.setRequestMethod("GET");
                //设置请求时间
                connection.setConnectTimeout(5000);
                // expect HTTP 200 OK, so we don't mistakenly save error
                // report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    myApp.myToast(UpdateAppActivity.this, connection.getResponseCode() + " " + connection.getResponseMessage());
                    return "Server returned HTTP "
                            + connection.getResponseCode() + " "
                            + connection.getResponseMessage();
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                file = new File(String.format(Locale.CHINA, FilePath.FILE_UPDATE_APK, versionBean.getVersion()));
                if (file.exists() && !file.delete()) {
                    myApp.myToast(UpdateAppActivity.this, String.format(Locale.CHINA, getString(R.string.message_delete_file_fail), file.getName()));
                }
                myApp.myToast(UpdateAppActivity.this, String.format(Locale.CHINA, getString(R.string.message_update_file_size), fileLength));
                input = connection.getInputStream();
                output = new FileOutputStream(file);
                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
//                myApp.myToast(UpdateAppActivity.this, "您未打开SD卡权限！");
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            Message msg = refreshUI.obtainMessage(UPDATE_PROGRESS);
            msg.arg1 = progress[0];
            refreshUI.sendMessage(msg);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (result != null) {
                refreshUI.sendEmptyMessage(UPDATE_DOWNLOAD_FAIL);
            } else {
                //安装应用
                btnVersion.setEnabled(true);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(String.format(Locale.CHINA, FilePath.FILE_UPDATE_APK, versionBean.getVersion()))), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }
}
