package com.leon.detonator.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.ListView;

import androidx.core.content.ContextCompat;

import com.leon.detonator.Adapter.SystemInfoAdapter;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.EnterpriseUserBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Bean.SystemInfoBean;
import com.leon.detonator.R;

import java.util.ArrayList;
import java.util.List;

public class SystemInfoActivity extends BaseActivity {
    private int keyCount = 0;
    private List<SystemInfoBean> infoBeans;
    private boolean displaySIM = false;
    private SystemInfoAdapter infoAdapter;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_info);

        setTitle(R.string.settings_info);
        myApp = (BaseApplication) getApplication();
        infoBeans = new ArrayList<>();
        SystemInfoBean bean = new SystemInfoBean();
        bean.setTitle(getResources().getString(R.string.system_info_login_user));
        List<EnterpriseUserBean.ResultBean.PageListBean> listBeans = myApp.readUserList();
        LocalSettingBean setting = BaseApplication.readSettings();
        if (listBeans != null)
            for (EnterpriseUserBean.ResultBean.PageListBean b : listBeans)
                if (b.getUserID() == setting.getUserID()) {
                    bean.setSubtitle(b.getName());
                }
        infoBeans.add(bean);
        bean = new SystemInfoBean();
        bean.setTitle(getResources().getString(R.string.system_info_device_code));
        bean.setSubtitle(setting.getExploderID());
        infoBeans.add(bean);
        bean = new SystemInfoBean();
        bean.setTitle(getResources().getString(R.string.system_info_version));
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            bean.setSubtitle(packageInfo.versionName);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        infoBeans.add(bean);

        //SystemInfoAdapter adapter = new SystemInfoAdapter(this,infoBeans);
        ListView listView = findViewById(R.id.lv_info);
        infoAdapter = new SystemInfoAdapter(this, infoBeans);
        listView.setAdapter(infoAdapter);
    }

    @SuppressLint("HardwareIds")
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!displaySIM) {
            if (keyCode == KeyEvent.KEYCODE_POUND) {
                keyCount++;
                if (keyCount > 4) {
                    displaySIM = true;
                    SystemInfoBean bean;
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(SystemInfoActivity.this, Manifest.permission.READ_PHONE_STATE)
                            && null != telephonyManager) {
                        try {
                            if (telephonyManager.getDeviceId() != null) {
                                bean = new SystemInfoBean();
                                bean.setTitle(getResources().getString(R.string.system_info_imei));
                                bean.setSubtitle(telephonyManager.getDeviceId());
                                infoBeans.add(bean);
                            }
                            if (null != telephonyManager.getSimSerialNumber()) {
                                bean = new SystemInfoBean();
                                bean.setTitle(getResources().getString(R.string.system_info_iccid));
                                bean.setSubtitle(telephonyManager.getSimSerialNumber());
                                infoBeans.add(bean);
                            }
                            if (null != telephonyManager.getLine1Number()) {
                                bean = new SystemInfoBean();
                                bean.setTitle(getResources().getString(R.string.system_info_number));
                                bean.setSubtitle(telephonyManager.getLine1Number());
                                infoBeans.add(bean);
                            }
                            infoAdapter.updateList(infoBeans);
                        } catch (Exception e) {
                            myApp.myToast(this, R.string.message_acquire_phone_info_error);
                            BaseApplication.writeErrorLog(e);
                        }
                    } else {
                        myApp.myToast(this, R.string.message_acquire_phone_info_error);
                    }
                }
            } else
                keyCount = 0;
        }
        return super.onKeyUp(keyCode, event);
    }
}
