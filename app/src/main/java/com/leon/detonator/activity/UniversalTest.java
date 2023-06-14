package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.kfree.comm.system.ScanQrControl;
import com.kfree.comm.utils.Utils;
import com.kfree.comm.utils.UtilsByte;
import com.kfree.expd.ExpdDevMgr;
import com.kfree.expd.OnOpenSerialPortListener;
import com.kfree.expd.OnSerialPortDataListener;
import com.kfree.expd.Status;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;

import java.io.File;

public class UniversalTest extends BaseActivity implements View.OnClickListener {
    private final String TAG = "ExpDev";

    private TextView mtv_show;

    private ExpdDevMgr mExpDevMgr;
    private ScanQrControl mScanner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_test);

        mtv_show = findViewById(R.id.tv_show);
        mExpDevMgr = new ExpdDevMgr(this);
        setTitle(R.string.hide_universal_test);

        mExpDevMgr.setExpdDevEventCb(new ExpdDevMgr.IOnExpdDevEventCb() {
            @Override
            public void onSafeSwitchStChg(boolean isNowOn) {
                showMes("接收事件 安全开关打开:" + isNowOn);
            }

            @Override
            public void onBackcoverStChg(boolean isNowOpen) {
                showMes("接收事件 电池后盖打开:" + isNowOpen);
            }
        });

        ((ToggleButton) findViewById(R.id.toggleExplorderPower)).setChecked(mExpDevMgr.isExPowerOn());
        ((ToggleButton) findViewById(R.id.toggleExplorderPower)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mExpDevMgr.exPowerOn();
            } else {
                mExpDevMgr.exPowerOff();
            }
        });

        ((ToggleButton) findViewById(R.id.toggleExplorderUart)).setChecked(mExpDevMgr.isSerialPortOpen());
        ((ToggleButton) findViewById(R.id.toggleExplorderUart)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mExpDevMgr.openSerialPort(new OnOpenSerialPortListener() {
                    @Override
                    public void onSuccess(File portDevFile) {
                        showMes("打开串口成功:" + portDevFile.toString());
                    }

                    @Override
                    public void onFail(File portDevFile, Status errMessage) {
                        showMes("打开串口失败:" + portDevFile.toString());
                    }
                }, new OnSerialPortDataListener() {
                    @Override
                    public void onDataReceived(byte[] buffer) {
                        showMes("收到:" + UtilsByte.byteBufferToHexString(buffer));
                    }

                    @Override
                    public void onDataSent(byte[] buffer) {
                        showMes("发送:" + UtilsByte.byteBufferToHexString(buffer));
                    }
                }, 115200);
            } else {
                mExpDevMgr.closeSerialPort();
            }
        });

        // 创建扫描头操作对象，并注册回调
        mScanner = new ScanQrControl(this);
        mScanner.registerScanCb(new ScanQrControl.IScan() {
            @Override
            public void onScanStart(int timeoutSec) {
                showMes("start scanning...");
            }

            @Override
            public void onScanResult(boolean isSuccess, String scanResultStr) {
                showMes("ScanResult:" + isSuccess + "|" + scanResultStr);
            }
        });

        ((ToggleButton) findViewById(R.id.togglePsamPw)).setChecked(mExpDevMgr.isPsamReaderPwOn());
        ((ToggleButton) findViewById(R.id.togglePsamPw)).setOnCheckedChangeListener((buttonView, isChecked) -> mExpDevMgr.setPsamReaderPw(isChecked));

        ((ToggleButton) findViewById(R.id.toggle12V)).setChecked(mExpDevMgr.is12VEnable());
        ((ToggleButton) findViewById(R.id.toggle12V)).setOnCheckedChangeListener((buttonView, isChecked) -> mExpDevMgr.set12VEnable(isChecked));
    }

    private void showMes(String mes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mtv_show.setText(mes);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        ((TextView) findViewById(R.id.tv_key_mes)).setText("keycode:" + keyCode + "\r\n" + event.toString());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    private int mSelectGpioN = 1;
    private int mSelectGpioValue = 1;

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_get_code: {
                mExpDevMgr.sendBytes(new byte[]{(byte) 0x1E, (byte) 0x02, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x75, (byte) 0x2E});
            }
            break;
            case R.id.btn_setIO: {
                Utils.showListSelectDialog(this, "选择GPIO", new String[]{"1", "2", "3"}, new Utils.ISelectDialogListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String itemStr) {
                        mSelectGpioN = which + 1;

                        Utils.showListSelectDialog(UniversalTest.this, "选择高低", new String[]{"0", "1"}, new Utils.ISelectDialogListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, String itemStr) {
                                mSelectGpioValue = which;

                                showMes("setExGpio " + mSelectGpioN + "," + mSelectGpioValue + "=" + mExpDevMgr.setExGpio(mSelectGpioN, mSelectGpioValue));
                            }
                        });
                    }
                });
            }
            break;
            case R.id.btn_getIO: {
                showMes("getExGpioStatus " + mSelectGpioN + "=" + mExpDevMgr.getExGpioStatus(mSelectGpioN));
            }
            break;
            case R.id.btn_getManfact: {
                showMes(mExpDevMgr.getManufacture());
            }
            break;
            case R.id.btn_writelog: {
                mExpDevMgr.writeAuditRecord("测试写入日志\n");
            }
            break;
            case R.id.btn_bc_scan: {
                mScanner.startScan();
            }
            break;
            case R.id.btn_reset_psam: {
                mExpDevMgr.resetPsamReader();
            }
            break;
            case R.id.btn_safe_sw: {
                showMes("安全开关:" + mExpDevMgr.isSafeSwitchOpen());
            }
            break;
            case R.id.btn_hw_ver: {
                showMes("硬件版本:" + Utils.getHwVerStr(this) + " | " + Utils.getHwVerV1x(this));
            }
            break;
            case R.id.btn_switch_keyboard: {
                mExpDevMgr.setKeyForNumber(!mExpDevMgr.getKeyForNumber());
                showMes("当前键为:" + (mExpDevMgr.getKeyForNumber() ? "数字键盘" : "方向键盘"));
            }
            break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanner.unregisterScanCb();
        mExpDevMgr.deInit();
    }
}