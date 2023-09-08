package com.leon.detonator.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.widget.ListView;

import androidx.core.content.ContextCompat;

import com.leon.detonator.R;
import com.leon.detonator.adapter.SystemInfoAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.SystemInfoBean;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SystemInfoActivity extends BaseActivity {
    private List<SystemInfoBean> infoBeans;
    private SystemInfoAdapter infoAdapter;
    private SerialPortUtil serialPortUtil;
    private DataReceiveListener myReceiveListener;
    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case 1:
                serialPortUtil.sendCmd("", SerialCommand.CODE_VERSION, 0);
                break;
            case 2:
                if (myReceiveListener != null)
                    myReceiveListener.closeAllHandler();
                break;
            case DataReceiveListener.HANDLER_RECEIVED_DATA:
                byte[] received = (byte[]) msg.obj;
                if (received != null && received.length > 0)
                    if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                        myApp.shortCircuit(SystemInfoActivity.this, msg.getTarget());
                    } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                        msg.getTarget().sendEmptyMessage(1);
                    } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                        myApp.myToast(SystemInfoActivity.this, R.string.message_open_module_fail);
                    } else if (received.length > SerialCommand.CODE_CHAR_AT + 1 && 0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                        if (received[SerialCommand.CODE_CHAR_AT] == SerialCommand.CODE_VERSION) {
                            SystemInfoBean bean = new SystemInfoBean();
                            bean.setTitle(getString(R.string.system_info_board_date));
                            bean.setSubtitle(String.format(Locale.getDefault(), "20%02x-%02x-%02x %02x:%02x",
                                    received[SerialCommand.CODE_CHAR_AT + 3], received[SerialCommand.CODE_CHAR_AT + 4], received[SerialCommand.CODE_CHAR_AT + 5],
                                    received[SerialCommand.CODE_CHAR_AT + 6], received[SerialCommand.CODE_CHAR_AT + 7]));
                            infoBeans.add(bean);
                            bean = new SystemInfoBean();
                            bean.setTitle(getString(R.string.system_info_board_version));
                            bean.setSubtitle(String.format(Locale.getDefault(), "%x.%2x", received[SerialCommand.CODE_CHAR_AT + 8], received[SerialCommand.CODE_CHAR_AT + 9]));
                            infoBeans.add(bean);
                            infoAdapter.updateList(infoBeans);
                            msg.getTarget().sendEmptyMessage(2);
                        }
                    }
                break;
        }
        return false;
    });

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_info);

        setTitle(R.string.settings_info);
        myApp = (BaseApplication) getApplication();
        infoBeans = new ArrayList<>();
        SystemInfoBean bean = new SystemInfoBean();
        bean.setTitle(getString(R.string.system_info_device_code));
        bean.setSubtitle(BaseApplication.settings.getExploderID());
        infoBeans.add(bean);
        bean = new SystemInfoBean();
        bean.setTitle(getString(R.string.system_info_version));
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            bean.setSubtitle(packageInfo.versionName);
            serialPortUtil = SerialPortUtil.getInstance(this);
            myReceiveListener = DataReceiveListener.getInstance(SystemInfoActivity.this, myHandler);
            myReceiveListener.setSingleConnect(true);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        infoBeans.add(bean);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(SystemInfoActivity.this, Manifest.permission.READ_PHONE_STATE)
                && null != telephonyManager) {
            try {
                if (telephonyManager.getDeviceId() != null) {
                    bean = new SystemInfoBean();
                    bean.setTitle(getString(R.string.system_info_imei));
                    bean.setSubtitle(telephonyManager.getDeviceId());
                    infoBeans.add(bean);
                }
                if (null != telephonyManager.getSimSerialNumber()) {
                    bean = new SystemInfoBean();
                    bean.setTitle(getString(R.string.system_info_iccid));
                    bean.setSubtitle(telephonyManager.getSimSerialNumber());
                    infoBeans.add(bean);
                }
                if (null != telephonyManager.getLine1Number()) {
                    bean = new SystemInfoBean();
                    bean.setTitle(getString(R.string.system_info_number));
                    bean.setSubtitle(telephonyManager.getLine1Number());
                    infoBeans.add(bean);
                }
            } catch (Exception e) {
                myApp.myToast(this, R.string.message_acquire_phone_info_error);
                BaseApplication.writeErrorLog(e);
            }
        } else {
            myApp.myToast(this, R.string.message_acquire_phone_info_error);
        }

        ListView listView = findViewById(R.id.lv_info);
        infoAdapter = new SystemInfoAdapter(this, infoBeans);
        listView.setAdapter(infoAdapter);
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        if (myReceiveListener != null)
            myReceiveListener.closeAllHandler();
        super.onDestroy();
    }
}
