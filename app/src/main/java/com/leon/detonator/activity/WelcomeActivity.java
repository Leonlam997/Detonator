package com.leon.detonator.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        RelativeLayout rlWelcome = findViewById(R.id.rlWelcome);
        try {
            if (!hasShortcut()) {
                Intent addIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.app_name);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, R.mipmap.ic_launcher);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(WelcomeActivity.this, WelcomeActivity.class));
                sendBroadcast(addIntent);
            }
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.tv_version)).setText(String.format(Locale.CHINA, getResources().getString(R.string.version_number), packageInfo.versionName));
            LocalSettingBean bean = BaseApplication.readSettings();
            if (null != bean.getExploderID()) {
                ((TextView) findViewById(R.id.tv_exploder)).setText(String.format(Locale.CHINA, getResources().getString(R.string.device_code), bean.getExploderID()));
            }
            BaseApplication.writeFile(getString(R.string.app_name) + packageInfo.versionName);
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
            finish();
        });
    }

    private boolean hasShortcut() {
        final ContentResolver cr = getContentResolver();
        final Uri CONTENT_URI = Uri.parse("content://com.android.launcher.settings/favorites?notify=true");
        try {
            Cursor c = cr.query(CONTENT_URI, new String[]{"title", "iconResource"}, "title=?",
                    new String[]{getString(R.string.app_name)}, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onKeyUp(keyCode, event);
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