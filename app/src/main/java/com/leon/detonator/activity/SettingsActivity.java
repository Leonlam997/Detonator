package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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
    private BaseApplication myApp;
    private long lastClickTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle(R.string.text_settings);
        myApp = (BaseApplication) getApplication();
        initData();
        ListView lvSettings = findViewById(R.id.lv_settings);
        adapter = new SettingsAdapter(this, list);
        lvSettings.setAdapter(adapter);
        lvSettings.setOnItemClickListener((parent, view, position, id) -> launchWhich(position));
        lvSettings.requestFocus();
    }

    private void launchWhich(int which) {
        if (System.currentTimeMillis() - lastClickTime > ConstantUtils.FAST_CLICK_DELAY_TIME) {
            switch (BaseApplication.settings.getServerHost()) {
                case 2:
                    if (which == 2 || which == 3)
                        which += 3;
                    else if (which > 3)
                        which -= 2;
                    break;
                case 0:
                case 3:
                    if (which == 2)
                        which = 5;
                    else if (which > 2)
                        which -= 1;
            }
            lastClickTime = System.currentTimeMillis();
            Intent intent = new Intent();
            BaseApplication.writeFile(list.get(which).getTitle());
            switch (which) {
                case 0:
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                    break;
                case 1:
                    intent.setClass(SettingsActivity.this, ServerSelectActivity.class);
                    startActivity(intent);
                    break;
                case 2:
                    intent.setClass(SettingsActivity.this, UpdateAppActivity.class);
                    startActivity(intent);
                    break;
                case 3:
                    new AlertDialog.Builder(this, R.style.AlertDialog)
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
                            .show();
                    break;
                case 4:
                    intent.setClass(SettingsActivity.this, SystemInfoActivity.class);
                    startActivity(intent);
                    break;
                case 5:
                    intent.setClass(SettingsActivity.this, InfoListActivity.class);
                    intent.putExtra(KeyUtils.KEY_INFO_TYPE, 2 == BaseApplication.settings.getServerHost() ? ConstantUtils.INFO_PROJECT : ConstantUtils.INFO_ENTERPRISE);
                    startActivity(intent);
                    break;
                case 6:
                    if (2 == BaseApplication.settings.getServerHost()) {
                        intent.setClass(SettingsActivity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_BLASTER);
                        startActivity(intent);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_7)
            launchWhich(keyCode - KeyEvent.KEYCODE_1);
        return super.onKeyUp(keyCode, event);
    }

    private void initData() {
        list = new ArrayList<>();
        final int[] iconRes = {R.mipmap.ic_settings_disp,
                R.mipmap.ic_settings_server,
                R.mipmap.ic_settings_upgrade,
                R.mipmap.ic_settings_clear,
                R.mipmap.ic_settings_info};
        final int[] menuTextID = {R.string.settings_local,
                R.string.settings_server,
                R.string.settings_upgrade,
                R.string.settings_clear,
                R.string.settings_info};
        boolean[] subMenu = {true, true, true, false, true};
        for (int i = 0; i < iconRes.length; i++) {
            SettingsBean bean = new SettingsBean();
            bean.setIcon(iconRes[i]);
            bean.setTitle((i + 1) + ". " + getString(menuTextID[i]));
            bean.setMore(subMenu[i]);
            list.add(bean);
        }
    }

    @Override
    protected void onResume() {
        new Handler(message -> {
            list.get(1).setSubtitle(ConstantUtils.UPLOAD_HOST[BaseApplication.settings.getServerHost()][0]);
            switch (BaseApplication.settings.getServerHost()) {
                case 2:
                    if (list.size() == 5 || list.size() == 6) {
                        SettingsBean bean = new SettingsBean();
                        if (list.size() == 5) {
                            bean.setIcon(R.mipmap.ic_settings_enterprise);
                            bean.setTitle("3. " + getString(R.string.settings_enterprise));
                            bean.setMore(true);
                            list.add(2, bean);
                            bean = new SettingsBean();
                        }
                        bean.setIcon(R.mipmap.ic_settings_blaster);
                        bean.setTitle("4. " + getString(R.string.settings_blaster));
                        bean.setMore(true);
                        list.add(3, bean);
                        for (int i = 4; i < list.size(); i++) {
                            list.get(i).setTitle((i + 1) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 2);
                        }
                    }
                    BaiSeInfoBean baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                    list.get(2).setSubtitle(baiSeInfoBean == null ? "" : String.format("%s %s", getString(R.string.enterprise_name), baiSeInfoBean.getBurstOrgName()));
                    BaiSeBlasterBean baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                    list.get(3).setSubtitle(baiSeBlasterBean == null ? "" : String.format("%s %s", getString(R.string.detector_name), baiSeBlasterBean.getData().getProjectCode()));
                    break;
                case 0:
                case 3:
                    if (list.size() == 5) {
                        SettingsBean bean = new SettingsBean();
                        bean.setIcon(R.mipmap.ic_settings_enterprise);
                        bean.setTitle("3. " + getString(R.string.settings_enterprise));
                        bean.setMore(true);
                        list.add(2, bean);
                        for (int i = 3; i < list.size(); i++) {
                            list.get(i).setTitle((i + 1) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 2);
                        }
                    }
                    EnterpriseBean enterpriseBean = DbUtil.getCurrentEnterprise();
                    list.get(2).setSubtitle(enterpriseBean == null ? "" : String.format("%s %s", getString(R.string.enterprise_code), enterpriseBean.getCode()));
                    if (list.size() == 7) {
                        list.remove(3);
                        for (int i = 3; i < list.size(); i++) {
                            list.get(i).setTitle((i + 1) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 2);
                        }
                    }
                    break;
                default:
                    if (list.size() == 6 || list.size() == 7) {
                        if (list.size() == 7)
                            list.remove(3);
                        list.remove(2);
                        for (int i = 2; i < list.size(); i++) {
                            list.get(i).setTitle((i + 1) + list.get(i).getTitle().substring(1));
                            list.get(i).setMore(i != list.size() - 2);
                        }
                    }
            }
            if (null != adapter) {
                adapter.notifyDataSetChanged();
            }
            return false;
        }).sendEmptyMessage(1);
        super.onResume();
    }
}
