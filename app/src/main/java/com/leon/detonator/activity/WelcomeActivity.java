package com.leon.detonator.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.CheckRegister;
import com.leon.detonator.base.UploadExplodeList;
import com.leon.detonator.bean.LocalSettingBean;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        BaseApplication myApp = (BaseApplication) getApplication();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        RelativeLayout rlWelcome = findViewById(R.id.rlWelcome);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.tv_version)).setText(String.format(Locale.getDefault(), getString(R.string.version_number), packageInfo.versionName));
            LocalSettingBean bean = BaseApplication.readSettings();
            if (BaseApplication.isNetSystemUsable(this)) {
                if (bean == null || !bean.isRegistered()) {
                    myApp.registerExploder();
                    new CheckRegister() {
                        @Override
                        public void onError() {
                        }

                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> ((TextView) findViewById(R.id.tv_exploder)).setText(String.format(Locale.getDefault(), getString(R.string.device_code), bean.getExploderID())));
                        }
                    }.setActivity(this).start();
                } else if (null != bean.getExploderID() || (null != bean.getExploderID() && bean.getExploderID().isEmpty())) {
                    ((TextView) findViewById(R.id.tv_exploder)).setText(String.format(Locale.getDefault(), getString(R.string.device_code), bean.getExploderID()));
                }
                if (UploadExplodeList.isNotUploading())
                    new UploadExplodeList(myApp).start();
            }
            BaseApplication.writeFile(getString(R.string.app_name) + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            BaseApplication.writeErrorLog(e);
        }
        rlWelcome.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, BaseApplication.isRemote() ? MainActivity.class : LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        LocalSettingBean bean = BaseApplication.readSettings();
        if (null != bean && null != bean.getExploderID())
            runOnUiThread(() -> ((TextView) findViewById(R.id.tv_exploder)).setText(String.format(Locale.getDefault(), getString(R.string.device_code), bean.getExploderID())));
        super.onResume();
    }

    @Override
    public void finish() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}