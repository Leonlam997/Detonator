package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ListView;

import com.leon.detonator.adapter.SettingsAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.SettingsBean;
import com.leon.detonator.R;
import com.leon.detonator.util.FilePath;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {
    private List<SettingsBean> list;
    private SettingsAdapter adapter;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle(R.string.txt_settings);
        myApp = (BaseApplication) getApplication();
        initData();
        ListView lvSettings = findViewById(R.id.lv_settings);
        adapter = new SettingsAdapter(this, list);
        lvSettings.setAdapter(adapter);
        lvSettings.setOnItemClickListener((parent, view, position, id) -> launchWhich(position));
        lvSettings.requestFocus();
    }

    private void launchWhich(int which) {
        Intent intent = new Intent();
        BaseApplication.writeFile(list.get(which).getMenuText());
        switch (which) {
            case 0:
                intent.setClass(SettingsActivity.this, WifiActivity.class);
                startActivity(intent);
                break;
            case 1:
                intent.setClass(SettingsActivity.this, BluetoothActivity.class);
                startActivity(intent);
                break;
            case 2:
                intent.setClass(SettingsActivity.this, DisplaySettingsActivity.class);
                startActivity(intent);
                break;
            case 3:
                intent.setClass(SettingsActivity.this, SoundSettingsActivity.class);
                startActivity(intent);
                break;
            case 4:
                intent = new Intent();
                ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.fingerprint.FingerprintEnrollIntroduction");
                intent.setComponent(componentName);
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
                break;
            case 5:
                intent.setClass(SettingsActivity.this, ServerSelectActivity.class);
                startActivity(intent);
                break;
            case 6:
                intent.setClass(SettingsActivity.this, 2 == BaseApplication.readSettings().getServerHost() ? BaiSeDataActivity.class : EnterpriseActivity.class);
                startActivity(intent);
                break;
            case 7:
                intent.setClass(SettingsActivity.this, UpdateAppActivity.class);
                startActivity(intent);
                break;
            case 8:
                BaseApplication.customDialog(new AlertDialog.Builder(this, R.style.AlertDialog)
                        .setTitle(R.string.progress_title)
                        .setMessage(R.string.dialog_exit_delete)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which1) -> {
                            try {
                                File[] files = new File(FilePath.APP_PATH + "/").listFiles();
                                if (null != files)
                                    for (File file : files) {
                                        if ((file.getName().endsWith("lst") || file.getName().endsWith("log")) && !file.delete())
                                            myApp.myToast(SettingsActivity.this, R.string.message_delete_fail);
                                    }
                                myApp.myToast(SettingsActivity.this, R.string.message_delete_success);
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show());
                break;
            case 9:
                intent.setClass(SettingsActivity.this, SystemInfoActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
            launchWhich(keyCode == KeyEvent.KEYCODE_0 ? 9 : keyCode - KeyEvent.KEYCODE_1);
        return super.onKeyUp(keyCode, event);
    }

    private void initData() {
        list = new ArrayList<>();
        final int[] iconRes = {R.mipmap.ic_settings_wifi,
                R.mipmap.ic_settings_bt,
                R.mipmap.ic_settings_disp,
                R.mipmap.ic_settings_sound,
                R.mipmap.ic_settings_finger,
                R.mipmap.ic_settings_server,
                R.mipmap.ic_settings_enterprise,
                R.mipmap.ic_settings_upgrade,
                R.mipmap.ic_settings_clear,
                R.mipmap.ic_settings_info};
        final int[] menuTextID = {R.string.settings_wifi,
                R.string.settings_bt,
                R.string.settings_display,
                R.string.settings_sound,
                R.string.settings_finger,
                R.string.settings_server,
                R.string.settings_enterprise,
                R.string.settings_upgrade,
                R.string.settings_clear,
                R.string.settings_info};
        boolean[] subMenu = {false, false, true, true, true, true, true, true, false, true, true};
        for (int i = 0; i < iconRes.length; i++) {
            SettingsBean bean = new SettingsBean();
            bean.setIcon(iconRes[i]);
            bean.setMenuText((i == 9 ? 0 : (i + 1)) + ". " + getString(menuTextID[i]));
            bean.setSubMenu(subMenu[i]);
            bean.setCheckBox(i < 2);
            list.add(bean);
        }
    }

    @Override
    protected void onResume() {
        new Handler(message -> {
            if (null != adapter)
                adapter.notifyDataSetChanged();
            return false;
        }).sendEmptyMessage(1);
        super.onResume();
    }
}
