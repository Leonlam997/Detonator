package com.leon.detonator.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Locale;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private int keyCount = 0, launchType;
    private LocalSettingBean settingBean;
    private BaseApplication myApp;
    private String[] title;
    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == 1) {
                UpdateVersionBean versionBean = (UpdateVersionBean) message.obj;
                if (versionBean.getVersion() != null) {
                    String[] version = versionBean.getVersion().split("\\.");
                    if (version.length == 3) {
                        try {
                            int code = Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2]);
                            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.split("\\.");
                            if (version.length == 3 && code > Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2])) {
                                BaseApplication.customDialog(new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog).setTitle(R.string.progress_title)
                                        .setMessage(String.format(Locale.CHINA, getResources().getString(R.string.dialog_found_new_version), versionBean.getVersion()))
                                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> startActivity(new Intent(MainActivity.this, UpdateAppActivity.class)))
                                        .setNegativeButton(R.string.btn_cancel, null).show());
                            }
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                    }
                }
            } else if (message.what == 2) {
                File file = new File(FilePath.FILE_SERIAL_LOG);
                if (file.exists() && file.length() > 2 * 1024 * 1024)
                    trimFile(FilePath.FILE_SERIAL_LOG);
                file = new File(FilePath.FILE_DEBUG_LOG);
                if (file.exists() && file.length() > 2 * 1024 * 1024)
                    trimFile(FilePath.FILE_DEBUG_LOG);
                if (!myApp.isUploading() && BaseApplication.isNetSystemUsable(MainActivity.this)) {
                    new Thread(() -> {
                        myApp.uploadExplodeList();
                        if (!settingBean.isUploadedLog()) {
                            myApp.uploadLog(FilePath.FILE_SERIAL_LOG);
                            myApp.uploadLog(FilePath.FILE_DEBUG_LOG);
                        }
                    }).start();
                    myApp.getVersion(myHandler);
                }
                myHandler.sendEmptyMessageDelayed(2, 60 * 1000);
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myApp = (BaseApplication) getApplication();
        setTitle(R.string.app_name, BaseApplication.isRemote() ? R.string.mode_wireless : myApp.isTunnel() ? R.string.mode_tunnel : R.string.mode_open_air);
        setBackButtonVisibility(false);
        setProgressVisibility(false);

        findViewById(R.id.btn_delay).setOnClickListener(this);
        findViewById(R.id.btn_authorize).setOnClickListener(this);
        findViewById(R.id.btn_records).setOnClickListener(this);
        findViewById(R.id.btn_control).setOnClickListener(this);
        findViewById(R.id.btn_settings).setOnClickListener(this);
        findViewById(R.id.btn_cooperate).setOnClickListener(this);
        findViewById(R.id.btn_cooperate).setEnabled(false);
        TextView[] textViews = new TextView[]{
                findViewById(R.id.tv_delay),
                findViewById(R.id.tv_control),
                findViewById(R.id.tv_auth),
                findViewById(R.id.tv_records),
                findViewById(R.id.tv_cooperate),
                findViewById(R.id.tv_settings)
        };
        title = new String[]{
                getString(R.string.delay_scheme),
                getString(R.string.detonate_ctrl),
                getString(R.string.detonate_auth),
                getString(R.string.detonate_rec),
                getString(R.string.detonate_cooperate),
                getString(R.string.txt_settings)
        };
        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setText(String.format(Locale.CHINA, "%d.%s", i + 1, title[i]));
            textViews[i].setOnClickListener(this);
        }
        keyCount = 0;
        initSettings();
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
            @SuppressLint("HardwareIds") String im = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            if (null == settingBean.getIMEI() || (null != settingBean.getIMEI() && null != im && !im.trim().isEmpty() && !settingBean.getIMEI().equals(im))) {
                settingBean.setRegistered(false);
                myApp.saveBean(settingBean);
            }
        }
        myHandler.sendEmptyMessage(2);
    }

    private void initSettings() {
        settingBean = BaseApplication.readSettings();
        findViewById(R.id.btn_authorize).setEnabled(0 == settingBean.getServerHost());
    }

    private void trimFile(String fileName) {
        new Thread(() -> {
            try {
                File tempFile = new File(FilePath.FILE_TEMP_LOG);
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                long i = br.skip(new File(fileName).length() - 1024 * 1024);
                if (i > 0) {
                    String read;
                    BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, false));
                    while ((read = br.readLine()) != null) bw.write(read + "\n");
                    bw.flush();
                    bw.close();
                }
                br.close();
                if (new File(fileName).delete()) {
                    if (!tempFile.renameTo(new File(fileName)))
                        myApp.myToast(MainActivity.this, R.string.message_delete_fail);
                } else
                    myApp.myToast(MainActivity.this, R.string.message_delete_fail);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }).start();
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_delay:
            case R.id.tv_delay:
                launchActivity(0);
                break;
            case R.id.btn_control:
            case R.id.tv_control:
                launchActivity(1);
                break;
            case R.id.btn_authorize:
            case R.id.tv_auth:
                launchActivity(2);
                break;
            case R.id.btn_records:
            case R.id.tv_records:
                launchActivity(3);
                break;
            case R.id.btn_cooperate:
            case R.id.tv_cooperate:
                launchActivity(4);
                break;
            case R.id.btn_settings:
            case R.id.tv_settings:
                launchActivity(5);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        myHandler = null;
        super.onDestroy();
    }

    private void launchActivity(int num) {
        keyCount = 0;
        if (2 == num && 0 != settingBean.getServerHost()) return;
        if (num >= 0 && num <= 5) {
            Intent intent = new Intent();
            Class<?>[] menuActivities = {DelayScheduleActivity.class,
                    DetonateStep1Activity.class,
                    AuthorizationListActivity.class,
                    ExplosionRecordActivity.class,
                    CheckLineActivity.class,
                    SettingsActivity.class,
            };
            if (4 == num) {
                if (!findViewById(R.id.btn_cooperate).isEnabled()) {
                    return;
                }
                intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, true);
            }
            intent.setClass(MainActivity.this, menuActivities[num]);
            BaseApplication.writeFile(title[num]);
            startActivity(intent);
            keyCount = 0;
        }
    }

    @Override
    protected void onPause() {
        myHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    protected void onResume() {
        BaseApplication.writeFile(getString(myApp.isTunnel() ? R.string.mode_tunnel : R.string.mode_open_air));
        initSettings();
        myHandler.sendEmptyMessage(2);
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_STAR:
                if (keyCount == 5) {
                    keyCount = 0;
                    if (launchType == 2) {
                        settingBean.setRegistered(false);
                        myApp.saveBean(settingBean);
                        if (BaseApplication.isNetSystemUsable(this)) {
                            myApp.registerExploder();
                            myApp.myToast(MainActivity.this, R.string.message_register_detonator);
                        } else {
                            myApp.myToast(MainActivity.this, R.string.message_check_network);
                        }
                    } else if (launchType <= 1) {
                        Intent intent = new Intent();
                        Class<?>[] menuActivities = {SemiProductActivity.class, WriteSNActivity.class};
                        intent.setClass(MainActivity.this, menuActivities[launchType]);
                        startActivity(intent);
                    }
                } else if (keyCount == 0 || keyCount == 1 || keyCount == 4) {
                    keyCount++;
                } else keyCount = 0;
                break;
            case KeyEvent.KEYCODE_0:
                if (keyCount == 2) keyCount++;
                else keyCount = 0;
                break;
            case KeyEvent.KEYCODE_6:
                if (keyCount == 3) {
                    launchType = 0;
                    keyCount++;
                } else keyCount = 0;
                break;
            case KeyEvent.KEYCODE_8:
                if (keyCount == 3) {
                    launchType = 1;
                    keyCount++;
                } else keyCount = 0;
                break;
            case KeyEvent.KEYCODE_9:
                if (keyCount == 3) {
                    launchType = 2;
                    keyCount++;
                } else keyCount = 0;
                break;
            case KeyEvent.KEYCODE_POUND:
                if (keyCount == 2 && !myApp.isUploading() && BaseApplication.isNetSystemUsable(this)) {
                    myApp.myToast(MainActivity.this, R.string.message_upload_log);
                    myApp.uploadLog(FilePath.FILE_SERIAL_LOG);
                    myApp.uploadLog(FilePath.FILE_DEBUG_LOG);
                }
            default:
                keyCount = 0;
        }
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_6 && keyCount == 0) {
            launchActivity(keyCode - KeyEvent.KEYCODE_1);
        }
        return super.onKeyUp(keyCode, event);
    }
}
