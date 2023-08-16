package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.SynchronizeExplodeRecord;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.util.FilePath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Locale;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private String[] title;
    private boolean dialogShowing;
    private int keyCount;
    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case UpdateAppActivity.UPDATE_HAS_NEW:
                UpdateVersionBean versionBean = (UpdateVersionBean) msg.obj;
                if (versionBean.getVersion() != null) {
                    String[] version = versionBean.getVersion().split("\\.");
                    if (version.length == 3) {
                        try {
                            int code = Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2]);
                            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.split("\\.");
                            if (version.length == 3 && code > Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2]) && !dialogShowing) {
                                dialogShowing = true;
                                BaseApplication.customDialog(new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog).setTitle(R.string.progress_title)
                                        .setMessage(String.format(Locale.getDefault(), getString(R.string.dialog_found_new_version), versionBean.getVersion()))
                                        .setPositiveButton(R.string.button_confirm, (dialog, which) -> startActivity(new Intent(MainActivity.this, UpdateAppActivity.class)))
                                        .setNegativeButton(R.string.button_cancel, null)
                                        .setOnDismissListener(dialog -> dialogShowing = false).show(), true);
                            }
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                    }
                }
                break;
            case 1:
                final Handler handler = msg.getTarget();
                handler.removeMessages(1);
                new Thread(() -> {
                    File file = new File(FilePath.FILE_SERIAL_LOG);
                    if (file.exists() && file.length() > 3 * 1024 * 1024)
                        trimFile(FilePath.FILE_SERIAL_LOG);
                    file = new File(FilePath.FILE_DEBUG_LOG);
                    if (file.exists() && file.length() > 3 * 1024 * 1024)
                        trimFile(FilePath.FILE_DEBUG_LOG);
                    if (BaseApplication.isNetSystemUsable(MainActivity.this) && BaseApplication.isNetPingUsable()) {
                        if (!SynchronizeExplodeRecord.uploading)
                            new SynchronizeExplodeRecord(myApp).start();
                        if (BaseApplication.settings != null) {
                            if (!BaseApplication.settings.isUploadedLog())
                                myApp.uploadLog(handler);
                            if (BaseApplication.settings.isUpdateHint())
                                myApp.getVersion(handler);
                        }
                    }
                    handler.sendEmptyMessageDelayed(1, 60 * 1000);
                }).start();
                break;
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name, BaseApplication.isRemote ? R.string.mode_wireless : BaseApplication.isTunnel ? R.string.mode_tunnel : R.string.mode_open_air);
        setProgressVisibility(false);
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
                getString(R.string.button_check_detonator),
                getString(R.string.txt_settings)
        };
        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setText(String.format(Locale.getDefault(), "%d.%s", i + 1, title[i]));
            textViews[i].setOnClickListener(this);
        }
        keyCount = 0;
        @SuppressLint("HardwareIds") String im = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        if (null == BaseApplication.settings.getIMEI() || (null != BaseApplication.settings.getIMEI() && null != im && !im.trim().isEmpty() && !BaseApplication.settings.getIMEI().equals(im))) {
            BaseApplication.settings.setRegistered(false);
            myApp.saveSettings();
        }
        myHandler.sendEmptyMessage(1);
    }

    private void initSettings() {
        findViewById(R.id.tv_auth).setEnabled(0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost());
    }

    private void trimFile(String fileName) {
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
                    BaseApplication.writeFile(String.format(getString(R.string.message_delete_file_fail), tempFile));
            } else
                BaseApplication.writeFile(String.format(getString(R.string.message_delete_file_fail), fileName));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_delay:
                launchActivity(0);
                break;
            case R.id.tv_control:
                launchActivity(1);
                break;
            case R.id.tv_auth:
                launchActivity(2);
                break;
            case R.id.tv_records:
                launchActivity(3);
                break;
            case R.id.tv_cooperate:
                launchActivity(4);
                break;
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
        super.onDestroy();
    }

    private void launchActivity(int num) {
        keyCount = 0;
        if (2 == num && 0 != BaseApplication.settings.getServerHost() && 3 != BaseApplication.settings.getServerHost())
            return;
        if (num >= 0 && num <= 6) {
            Intent intent = new Intent();
            final Class<?>[] menuActivities = {SchemeActivity.class,
                    DetonateStep1Activity.class,
                    AuthorizationListActivity.class,
                    ExplosionRecordActivity.class,
                    CheckLineActivity.class,
                    SettingsActivity.class,
                    HideActivity.class
            };
            intent.setClass(MainActivity.this, menuActivities[num]);
            BaseApplication.writeFile(num == 6 ? getString(R.string.hide_test_title) : title[num]);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        myHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    protected void onResume() {
        BaseApplication.writeFile(getString(BaseApplication.isTunnel ? R.string.mode_tunnel : R.string.mode_open_air));
        initSettings();
        myHandler.sendEmptyMessage(1);
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_STAR:
                if (keyCount == 0 || keyCount == 2)
                    keyCount++;
                else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_POUND:
                if (keyCount == 1)
                    keyCount++;
                else if (keyCount == 3)
                    launchActivity(6);
                else
                    keyCount = 0;

                break;
            default:
                if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_6)
                    launchActivity(keyCode - KeyEvent.KEYCODE_1);
        }
        return super.onKeyUp(keyCode, event);
    }
}
