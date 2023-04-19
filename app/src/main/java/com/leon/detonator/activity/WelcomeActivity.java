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

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        RelativeLayout rlWelcome = findViewById(R.id.rlWelcome);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.tv_version)).setText(String.format(Locale.CHINA, getString(R.string.version_number), packageInfo.versionName));
            LocalSettingBean bean = BaseApplication.readSettings();
            if (null != bean.getExploderID()) {
                ((TextView) findViewById(R.id.tv_exploder)).setText(String.format(Locale.CHINA, getString(R.string.device_code), bean.getExploderID()));
            }
        } catch (PackageManager.NameNotFoundException e) {
            BaseApplication.writeErrorLog(e);
        }
        if (BaseApplication.isNetSystemUsable(this)) {
            BaseApplication myApp = (BaseApplication) getApplication();
            myApp.uploadExplodeList();
        }
        rlWelcome.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, BaseApplication.isRemote() ? MainActivity.class : LoginActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        LocalSettingBean bean = BaseApplication.readSettings();
        if (null != bean.getExploderID())
            runOnUiThread(() -> ((TextView) findViewById(R.id.tv_exploder)).setText(String.format(Locale.CHINA, getString(R.string.device_code), bean.getExploderID())));
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

    @Override
    protected void onDestroy() {
        /*
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
        */
        super.onDestroy();
    }
}