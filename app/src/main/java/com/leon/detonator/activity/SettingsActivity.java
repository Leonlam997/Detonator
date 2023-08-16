package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ListView;

import com.leon.detonator.R;
import com.leon.detonator.adapter.SettingsAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.SettingsBean;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {
    private List<SettingsBean> list;
    private SettingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.txt_settings);
        initData();
        ListView lvSettings = findViewById(R.id.lv_settings);
        adapter = new SettingsAdapter(this, list);
        lvSettings.setAdapter(adapter);
        lvSettings.setOnItemClickListener((parent, view, position, id) -> launchWhich(position));
        lvSettings.requestFocus();
    }

    private void launchWhich(int which) {
        Intent intent = new Intent();
        BaseApplication.writeFile(list.get(which).getTitle());
        switch (BaseApplication.settings.getServerHost()) {
            case 2:
                if (which == 5 || which == 6)
                    which += 4;
                else if (which > 6)
                    which -= 2;
                break;
            case 0:
            case 3:
                if (which == 5)
                    which = 9;
                else if (which > 5)
                    which -= 1;
        }
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
                intent.setClass(SettingsActivity.this, ServerSelectActivity.class);
                startActivity(intent);
                break;
            case 5:
                intent.setClass(SettingsActivity.this, UpdateAppActivity.class);
                startActivity(intent);
                break;
            case 6:
                BaseApplication.customDialog(new AlertDialog.Builder(this, R.style.AlertDialog)
                        .setTitle(R.string.progress_title)
                        .setMessage(R.string.dialog_exit_delete)
                        .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
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
                        .setNegativeButton(R.string.button_cancel, null)
                        .show(), true);
                break;
            case 7:
                intent.setClass(SettingsActivity.this, SystemInfoActivity.class);
                startActivity(intent);
                break;
            case 8:
                intent = new Intent();
                ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.fingerprint.FingerprintEnrollIntroduction");
                intent.setComponent(componentName);
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
                break;
            case 10:
            case 9:
                intent.setClass(SettingsActivity.this, InfoListActivity.class);
                intent.putExtra(KeyUtils.KEY_INFO_TYPE, which == 10 ? ConstantUtils.INFO_BLASTER : 2 == BaseApplication.settings.getServerHost() ? ConstantUtils.INFO_PROJECT : ConstantUtils.INFO_ENTERPRISE);
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
        else if (list.size() > 10 && keyCode == KeyEvent.KEYCODE_STAR)
            launchWhich(10);
        return super.onKeyUp(keyCode, event);
    }

    private void initData() {
        list = new ArrayList<>();
        final int[] iconRes = {R.mipmap.ic_settings_wifi,
                R.mipmap.ic_settings_bt,
                R.mipmap.ic_settings_disp,
                R.mipmap.ic_settings_sound,
                R.mipmap.ic_settings_server,
                R.mipmap.ic_settings_upgrade,
                R.mipmap.ic_settings_clear,
                R.mipmap.ic_settings_info,
                R.mipmap.ic_settings_finger};
        final int[] menuTextID = {R.string.settings_wifi,
                R.string.settings_bt,
                R.string.settings_display,
                R.string.settings_sound,
                R.string.settings_server,
                R.string.settings_upgrade,
                R.string.settings_clear,
                R.string.settings_info,
                R.string.settings_finger};
        boolean[] subMenu = {false, false, true, true, true, true, true, false, true, true};
        for (int i = 0; i < iconRes.length; i++) {
            SettingsBean bean = new SettingsBean();
            bean.setIcon(iconRes[i]);
            bean.setTitle((i + 1) + ". " + getString(menuTextID[i]));
            bean.setMore(subMenu[i]);
            bean.setCheckBox(i < 2);
            list.add(bean);
        }
    }

    @Override
    protected void onResume() {
        new Handler(message -> {
            list.get(4).setSubtitle(ConstantUtils.UPLOAD_HOST[BaseApplication.settings.getServerHost()][0]);
            switch (BaseApplication.settings.getServerHost()) {
                case 2:
                    if (list.size() == 9 || list.size() == 10) {
                        SettingsBean bean = new SettingsBean();
                        if (list.size() == 9) {
                            bean.setIcon(R.mipmap.ic_settings_enterprise);
                            bean.setTitle("6. " + getString(R.string.settings_enterprise));
                            bean.setMore(true);
                            list.add(5, bean);
                            bean = new SettingsBean();
                        }
                        bean.setIcon(R.mipmap.ic_settings_blaster);
                        bean.setTitle("7. " + getString(R.string.settings_blaster));
                        bean.setMore(true);
                        list.add(6, bean);
                        for (int i = 7; i < list.size(); i++) {
                            list.get(i).setTitle((i == 9 ? 0 : i == 10 ? "*" : (i + 1)) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 3);
                        }
                    }
                    BaiSeInfoBean baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                    list.get(5).setSubtitle(baiSeInfoBean == null ? "" : String.format("%s %s", getString(R.string.enterprise_name), baiSeInfoBean.getBurstOrgName()));
                    BaiSeBlasterBean baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                    list.get(6).setSubtitle(baiSeBlasterBean == null ? "" : String.format("%s %s", getString(R.string.detector_name), baiSeBlasterBean.getData().getProjectCode()));
                    break;
                case 0:
                case 3:
                    if (list.size() == 9) {
                        SettingsBean bean = new SettingsBean();
                        bean.setIcon(R.mipmap.ic_settings_enterprise);
                        bean.setTitle("6. " + getString(R.string.settings_enterprise));
                        bean.setMore(true);
                        list.add(5, bean);
                        for (int i = 6; i < list.size(); i++) {
                            list.get(i).setTitle((i == 9 ? 0 : (i + 1)) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 3);
                        }
                    }
                    EnterpriseBean enterpriseBean = DbUtil.getCurrentEnterprise();
                    list.get(5).setSubtitle(enterpriseBean == null ? "" : String.format("%s %s", getString(R.string.enterprise_code), enterpriseBean.getCode()));
                    if (list.size() == 11) {
                        list.remove(6);
                        for (int i = 6; i < list.size(); i++) {
                            list.get(i).setTitle((i == 9 ? 0 : (i + 1)) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 3);
                        }
                    }
                    break;
                default:
                    if (list.size() == 10 || list.size() == 11) {
                        if (list.size() == 11)
                            list.remove(6);
                        list.remove(5);
                        for (int i = 5; i < list.size(); i++) {
                            list.get(i).setTitle((i + 1) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 3);
                        }
                    }
            }
            adapter.updateList(list);
            return false;
        }).sendEmptyMessage(1);
        super.onResume();
    }
}
