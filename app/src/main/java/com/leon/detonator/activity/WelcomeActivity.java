package com.leon.detonator.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.database.DbUtil;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    private TextView tvExploder;
    private final Handler myHandler = new Handler(msg -> {
        if (msg.what == BaseApplication.HANDLER_REGISTER_SUCCESS)
            tvExploder.setText(String.format(Locale.getDefault(), getString(R.string.device_code), BaseApplication.settings.getExploderID()));
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        BaseApplication myApp = (BaseApplication) getApplication();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        RelativeLayout rlWelcome = findViewById(R.id.rlWelcome);
        tvExploder = findViewById(R.id.tv_exploder);
        try {
            DbUtil.initHelper(this);
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.tv_version)).setText(String.format(Locale.getDefault(), getString(R.string.version_number), packageInfo.versionName));
            new Thread(() -> {
                if (BaseApplication.isNetSystemUsable(this) && BaseApplication.isNetPingUsable()) {
                    if (BaseApplication.settings == null || !BaseApplication.settings.isRegistered())
                        myApp.registerExploder(myHandler);
                    else if (null != BaseApplication.settings.getExploderID() || (null != BaseApplication.settings.getExploderID() && BaseApplication.settings.getExploderID().isEmpty()))
                        runOnUiThread(() -> tvExploder.setText(String.format(Locale.getDefault(), getString(R.string.device_code), BaseApplication.settings.getExploderID())));
                }
            }).start();
            BaseApplication.writeFile(getString(R.string.app_name) + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            BaseApplication.writeErrorLog(e);
        }
        rlWelcome.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, BaseApplication.isRemote ? MainActivity.class : LoginActivity.class)));
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        if (null != BaseApplication.settings && null != BaseApplication.settings.getExploderID() && tvExploder.getText().toString().isEmpty())
            tvExploder.setText(String.format(Locale.getDefault(), getString(R.string.device_code), BaseApplication.settings.getExploderID()));
        super.onResume();
    }

    @Override
    public void finish() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}