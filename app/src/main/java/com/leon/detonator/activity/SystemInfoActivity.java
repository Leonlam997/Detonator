package com.leon.detonator.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.leon.detonator.R;
import com.leon.detonator.adapter.SystemInfoAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.EnterpriseUserBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.SystemInfoBean;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SystemInfoActivity extends BaseActivity {
    private int keyCount = 0;
    private List<SystemInfoBean> infoBeans;
    private boolean displaySIM = false;
    private SystemInfoAdapter infoAdapter;
    private BaseApplication myApp;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 1:
                    serialPortUtil.sendCmd("", SerialCommand.CODE_VERSION, 0);
                    myHandler.sendEmptyMessageDelayed(2, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
                case 2:
                    serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0, 0xFF, 0x16);
                    break;
            }
            return false;
        }
    });

    private final Runnable bufferRunnable = () -> {
        byte[] received = myReceiveListener.getRcvData();
        if (received[0] == SerialCommand.INITIAL_FINISHED) {
            myHandler.sendEmptyMessage(1);
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
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_info);

        setTitle(R.string.settings_info);
        myApp = (BaseApplication) getApplication();
        infoBeans = new ArrayList<>();
        SystemInfoBean bean = new SystemInfoBean();
        bean.setTitle(getString(R.string.system_info_login_user));
        List<EnterpriseUserBean.ResultBean.PageListBean> listBeans = myApp.readUserList();
        LocalSettingBean setting = BaseApplication.readSettings();
        if (listBeans != null)
            for (EnterpriseUserBean.ResultBean.PageListBean b : listBeans)
                if (b.getUserID() == setting.getUserID()) {
                    bean.setSubtitle(b.getName());
                }
        infoBeans.add(bean);
        bean = new SystemInfoBean();
        bean.setTitle(getString(R.string.system_info_device_code));
        bean.setSubtitle(setting.getExploderID());
        infoBeans.add(bean);
        bean = new SystemInfoBean();
        bean.setTitle(getString(R.string.system_info_version));
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            bean.setSubtitle(packageInfo.versionName);
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(SystemInfoActivity.this, bufferRunnable);
            myReceiveListener.setSingleConnect(true);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
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

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        if (myReceiveListener != null) {
            myReceiveListener.setStartAutoDetect(false);
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
        if (null != serialPortUtil) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        super.onDestroy();
    }
}
