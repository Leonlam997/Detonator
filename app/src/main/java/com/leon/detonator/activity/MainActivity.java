package com.leon.detonator.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.SynchronizeExplodeRecord;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Locale;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private CheckBox cbChange;
    private String[] title;
    private boolean dialogShowing;
    private long lastClickTime;
    private int keyCount = 0;

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
                                        .setOnDismissListener(dialog -> dialogShowing = false).show());
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

        myApp = (BaseApplication) getApplication();
        setTitle(R.string.app_name);
        setTitleClickListener(v -> hideTest());
        setBackButtonVisibility(false);
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
                getString(R.string.check_detonator),
                getString(R.string.text_settings)
        };
        int[] color = new int[]{R.color.colorMainBg1, R.color.colorMainBg2, R.color.colorMainBg3,
                R.color.colorMainBg4, R.color.colorMainBg5, R.color.colorMainBg6};
        for (int i = 0; i < textViews.length; i++) {
            GradientDrawable drawable = (GradientDrawable) AppCompatResources.getDrawable(this, R.drawable.shape_main_item);
            if (drawable != null) {
                drawable.setColor(getColor(color[i]));
                textViews[i].setBackground(drawable);
            }
            textViews[i].setText(String.format(Locale.getDefault(), "%d. %s", i + 1, title[i]));
            textViews[i].setOnClickListener(this);
        }
        findViewById(R.id.tv_open_air).setOnClickListener(this);
        findViewById(R.id.tv_tunnel).setOnClickListener(this);
        cbChange = findViewById(R.id.cb_mode);
        cbChange.setOnCheckedChangeListener((compoundButton, b) -> {
            if ((b && BaseApplication.settings.isTunnel()) || (!b && !BaseApplication.settings.isTunnel())) {
                BaseApplication.settings.setTunnel(!BaseApplication.settings.isTunnel());
                BaseApplication.saveSettings();
                setBarColor(BaseApplication.settings.isTunnel());
            }
        });
        keyCount = 0;
        initSettings();
        cbChange.setChecked(!BaseApplication.settings.isTunnel());
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
            @SuppressLint("HardwareIds") String im = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            if (null == BaseApplication.settings.getIMEI() || (null != BaseApplication.settings.getIMEI() && null != im && !im.trim().isEmpty() && !BaseApplication.settings.getIMEI().equals(im))) {
                BaseApplication.settings.setRegistered(false);
                BaseApplication.saveSettings();
            }
        }
        myHandler.sendEmptyMessage(1);
    }

    private void hideTest() {
        if (++keyCount > ConstantUtils.HIDE_TEST_COUNT) {
            keyCount = 0;
            startActivity(new Intent(MainActivity.this, HideTestActivity.class));
        }
    }

    private void initSettings() {
        runOnUiThread(() -> findViewById(R.id.tv_auth).setEnabled(0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()));
    }

    private void trimFile(String fileName) {
        new Thread(() -> {
            try {
                File tempFile = new File(FilePath.FILE_TEMP_LOG);
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                long i = br.skip(new File(fileName).length() - 2 * 1024 * 1024);
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
        if (System.currentTimeMillis() - lastClickTime > ConstantUtils.FAST_CLICK_DELAY_TIME) {
            lastClickTime = System.currentTimeMillis();
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
                case R.id.tv_open_air:
                    cbChange.setChecked(true);
                    break;
                case R.id.tv_tunnel:
                    cbChange.setChecked(false);
                default:
                    break;
            }
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
        if (num >= 0 && num <= 5) {
            Intent intent = new Intent();
            Class<?>[] menuActivities = {SchemeActivity.class,
                    DetonateStep1Activity.class,
                    AuthorizationListActivity.class,
                    ExplosionRecordActivity.class,
                    CheckLineActivity.class,
                    SettingsActivity.class,
            };
            intent.setClass(MainActivity.this, menuActivities[num]);
            BaseApplication.writeFile(title[num]);
            startActivity(intent);
            keyCount = 0;
        }
    }

    @Override
    public void finish() {
    }

    @Override
    protected void onPause() {
        myHandler.removeCallbacksAndMessages(null);
        keyCount = 0;
        super.onPause();
    }

    @Override
    protected void onResume() {
        BaseApplication.writeFile(getString(BaseApplication.settings.isTunnel() ? R.string.mode_tunnel : R.string.mode_open_air));
        setBarColor(BaseApplication.settings.isTunnel());
        initSettings();
        myHandler.sendEmptyMessage(1);
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
                launchActivity(keyCode - KeyEvent.KEYCODE_1);
                break;
            case KeyEvent.KEYCODE_F1:
                cbChange.setChecked(true);
                break;
            case KeyEvent.KEYCODE_F2:
                cbChange.setChecked(false);
                break;
            case KeyEvent.KEYCODE_MENU:
                hideTest();
                return super.onKeyUp(keyCode, event);
        }
        keyCount = 0;
        return super.onKeyUp(keyCode, event);
    }
}
