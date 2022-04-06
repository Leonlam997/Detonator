package com.leon.detonator.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.PermissionRequest;

import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Bean.UpdateVersionBean;
import com.leon.detonator.R;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.KeyUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private int keyCount = 0, launchType;
    private LocalSettingBean settingBean;
    private UpdateVersionBean versionBean;
    private BaseApplication myApp;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myApp = (BaseApplication) getApplication();
        setTitle(R.string.app_name, myApp.isTunnel() ? R.string.tunnel : R.string.open_air);
        setBackButtonVisibility(false);
        setProgressVisibility(false);

        findViewById(R.id.btn_delay).setOnClickListener(this);
        findViewById(R.id.btn_authorize).setOnClickListener(this);
        findViewById(R.id.btn_log).setOnClickListener(this);
        findViewById(R.id.btn_control).setOnClickListener(this);
        findViewById(R.id.btn_settings).setOnClickListener(this);
        findViewById(R.id.btn_cooperate).setOnClickListener(this);
        findViewById(R.id.tv_delay).setOnClickListener(this);
        findViewById(R.id.tv_auth).setOnClickListener(this);
        findViewById(R.id.tv_log).setOnClickListener(this);
        findViewById(R.id.tv_control).setOnClickListener(this);
        findViewById(R.id.tv_settings).setOnClickListener(this);
        findViewById(R.id.tv_cooperate).setOnClickListener(this);
        findViewById(R.id.btn_cooperate).setEnabled(false);
        new Thread(() -> myApp.uploadExplodeList()).start();
        LocalSettingBean bean = BaseApplication.readSettings();
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
            String im = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            if (null == bean.getIMEI() || (null != bean.getIMEI() && null != im && !im.trim().isEmpty() && !bean.getIMEI().equals(im))) {
                bean.setRegistered(false);
                myApp.saveSettings(bean);
            }
        }
        keyCount = 0;
        initSettings();
        new GetVersion().execute();
    }

    private void initSettings() {
        settingBean = BaseApplication.readSettings();
        versionBean = new UpdateVersionBean();
        findViewById(R.id.btn_authorize).setEnabled(0 == settingBean.getServerHost());
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
            case R.id.btn_log:
            case R.id.tv_log:
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

    private void launchActivity(int num) {
        keyCount = 0;
        if (2 == num && 0 != settingBean.getServerHost())
            return;
        if (num >= 0 && num <= 5) {
            Intent intent = new Intent();
            Class<?>[] menuActivities = {DelayScheduleActivity.class,
                    DetonateStep1Activity.class,
                    AuthorizationListActivity.class,
                    ExplosionRecordActivity.class,
                    DetonateStep1Activity.class,
                    SettingsActivity.class,
            };
            if (4 == num) {
                if (!findViewById(R.id.btn_cooperate).isEnabled()) {
                    return;
                }
                intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, true);
            }
            intent.setClass(MainActivity.this, menuActivities[num]);
            startActivity(intent);
            keyCount = 0;
        }
    }

    @Override
    protected void onResume() {
        initSettings();
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_STAR:
                if (keyCount == 5 && launchType <= 4) {
                    keyCount = 0;
                    Intent intent = new Intent();
                    Class<?>[] menuActivities = {VoltageTestActivity.class,
                            SemiProductActivity.class,
                            settingBean.isNewLG() ? PropsSettingsActivity.class : SerialTestActivity.class,
                            WriteSNActivity.class,
                            ChargeActivity.class};
                    intent.setClass(MainActivity.this, menuActivities[launchType]);
                    startActivity(intent);
                    keyCount = 0;
                } else if (keyCount == 0 || keyCount == 1 || keyCount == 4) {
                    keyCount++;
                } else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_0:
                if (keyCount == 2)
                    keyCount++;
                else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_5:
                if (keyCount == 3) {
                    launchType = 0;
                    keyCount++;
                } else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_6:
                if (keyCount == 3) {
                    launchType = 1;
                    keyCount++;
                } else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_7:
                if (keyCount == 3) {
                    launchType = 2;
                    keyCount++;
                } else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_8:
                if (keyCount == 3) {
                    launchType = 3;
                    keyCount++;
                } else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_9:
                if (keyCount == 3) {
                    launchType = 4;
                    keyCount++;
                } else
                    keyCount = 0;
                break;
            case KeyEvent.KEYCODE_POUND:
                if (keyCount == 2) {
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

    @SuppressLint("StaticFieldLeak")
    private class GetVersion extends AsyncTask<Void, Integer, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(ConstantUtils.VERSION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
                    InputStream is = connection.getInputStream();

                    int len;
                    byte[] buf = new byte[1024];
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((len = is.read(buf)) != -1) {
                        String s = new String(buf, 0, len);
                        stringBuilder.append(s);
                    }
                    versionBean = new Gson().fromJson(stringBuilder.toString(), UpdateVersionBean.class);
                    is.close();
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            return "success";
        }

        private boolean newVersion() {
            if (versionBean.getVersion() != null) {
                String[] version = versionBean.getVersion().split("\\.");
                if (version.length == 3) {
                    try {
                        int code = Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2]);
                        version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.split("\\.");
                        if (version.length == 3 && code > Integer.parseInt(version[0]) * 1000 * 1000 + Integer.parseInt(version[1]) * 1000 + Integer.parseInt(version[2])) {
                            return true;
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(String result) {
            if (newVersion()) {
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.progress_title)
                        .setMessage(String.format(Locale.CHINA, getResources().getString(R.string.dialog_found_new_version), versionBean.getVersion()))
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> startActivity(new Intent(MainActivity.this, UpdateAppActivity.class)))
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create().show());
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }
}
