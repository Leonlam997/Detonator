package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.R;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ChargeActivity extends BaseActivity {
    private final int DETECT_SHORT = 1,
            DETECT_INITIAL = 2,
            DETECT_CHARGE = 3,
            DETECT_VOLTAGE = 4,
            DETECT_CHANGE_FINISHED = 5;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private SoundPool soundPool;
    private MyProgressDialog pDialog;
    private BaseApplication myApp;
    private int soundSuccess, soundFail, soundAlert, chargeCmdCount;
    private int timeCount, elapseTime, dac;
    private final Handler detectStatusHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case DETECT_SHORT:
                    if (myReceiveListener != null) {
                        myReceiveListener.closeAllHandler();
                        myReceiveListener = null;
                    }
                    if (serialPortUtil != null) {
                        serialPortUtil.closeSerialPort();
                        serialPortUtil = null;
                    }
                    myApp.playSoundVibrate(soundPool, soundAlert);
                    BaseApplication.customDialog(new AlertDialog.Builder(ChargeActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_short_circuit)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .show(), true);
                    break;
                case DETECT_INITIAL:
                    if (null != pDialog && pDialog.isShowing()) {
                        BaseApplication.releaseWakeLock(ChargeActivity.this);
                        pDialog.dismiss();
                    }
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    enabledButton(true);
                    break;
                case DETECT_CHARGE:
                    if (chargeCmdCount++ <= 1) {
                        detectStatusHandler.sendEmptyMessageDelayed(DETECT_CHARGE, 1000);
                    } else {
                        detectStatusHandler.sendEmptyMessageDelayed(DETECT_VOLTAGE, ConstantUtils.BOOST_TIME);
                    }
                    break;
                case DETECT_VOLTAGE:
                    myReceiveListener.setStartAutoDetect(true);
                    break;
                case DETECT_CHANGE_FINISHED:
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    enabledButton(true);
                    break;
            }
            return false;
        }
    });
    private float workVoltage;
    private EditText etTime, etVoltage;
    private final Handler progressHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            elapseTime += 200;
            if (elapseTime < timeCount - 200) {
                pDialog.setProgress(100 * elapseTime / timeCount);
            }
            if (elapseTime > timeCount) {
                detectStatusHandler.sendEmptyMessage(DETECT_INITIAL);
            } else {
                progressHandler.sendEmptyMessageDelayed(1, 200);
            }
            return false;
        }
    });
    private LocalSettingBean settingBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge);

        setTitle(R.string.det_capacity);

        myApp = (BaseApplication) getApplication();
        initSound();
        settingBean = BaseApplication.readSettings();
        etTime = findViewById(R.id.et_time);
        etVoltage = findViewById(R.id.et_voltage);
        dac = 660;
        workVoltage = 21;
        findViewById(R.id.btn_charge).setOnClickListener(v -> {
            try {
                float elapse;
                if (etTime.getText().toString().isEmpty())
                    elapse = 5;
                else
                    elapse = Float.parseFloat(etTime.getText().toString());
                if (elapse > 0 && elapse <= 60) {
                    timeCount = (int) (elapse * 60000);
                    enabledButton(false);
                    chargeCmdCount = 0;
                    pDialog = new MyProgressDialog(this);
                    pDialog.setInverseBackgroundForced(false);
                    pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pDialog.setCanceledOnTouchOutside(false);
                    pDialog.setTitle(R.string.progress_title);
                    pDialog.setMessage(getString(R.string.progress_charging));
                    pDialog.setMax(100);
                    pDialog.setProgress(0);
                    pDialog.setOnCancelListener(dialog -> {
                        elapseTime = 0;
                        enabledButton(true);
                    });
                    elapseTime = 0;
                    pDialog.show();
                    progressHandler.sendEmptyMessageDelayed(1, 100);
                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_CHARGE, ConstantUtils.BOOST_TIME);
                    BaseApplication.acquireWakeLock(this);
                }
            } catch (Exception e) {
                myApp.myToast(ChargeActivity.this, R.string.message_detonator_time_input_error);
                etTime.requestFocus();
            }
        });
        findViewById(R.id.btn_voltage).setOnClickListener(v -> {
            enabledButton(false);
            if (etVoltage.getText().toString().isEmpty()) {
                myApp.myToast(ChargeActivity.this, R.string.message_input_voltage_error);
                etVoltage.requestFocus();
            } else
                try {
                    workVoltage = Float.parseFloat(etVoltage.getText().toString());
                    if (workVoltage < 0 || workVoltage > 24) {
                        myApp.myToast(ChargeActivity.this, R.string.message_input_voltage_error);
                        etVoltage.requestFocus();
                    } else
                        startChangeVoltage(Float.parseFloat(etVoltage.getText().toString()));
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                    myApp.myToast(ChargeActivity.this, R.string.message_input_voltage_error);
                    etVoltage.requestFocus();
                }
        });

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(this, () -> {
                byte[] received = myReceiveListener.getRcvData();
                if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                    detectStatusHandler.sendEmptyMessage(DETECT_SHORT);
                } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                    detectStatusHandler.sendEmptyMessage(DETECT_INITIAL);
                } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                    myApp.myToast(ChargeActivity.this, R.string.message_open_module_fail);
                    finish();
                }
            });
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            enabledButton(false);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void enabledButton(boolean enabled) {
        setProgressVisibility(!enabled);
        findViewById(R.id.btn_charge).setEnabled(enabled);
        findViewById(R.id.btn_voltage).setEnabled(enabled);
        myReceiveListener.setStartAutoDetect(enabled);
    }

    private void startChangeVoltage(float vol) {
        if (null == settingBean.getDacMap())
            settingBean.setDacMap(new HashMap<>());
        Integer v = settingBean.getDacMap().get(vol);
        if (null != v) {
            dac = v;
        } else {
            dac = 94 + (int) ((24 - vol) * 124);
        }
        myReceiveListener.setStartAutoDetect(true);
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
            if (0 == soundSuccess)
                myApp.myToast(this, R.string.message_media_load_error);
            soundFail = soundPool.load(this, R.raw.fail, 1);
            if (0 == soundFail)
                myApp.myToast(this, R.string.message_media_load_error);
            soundAlert = soundPool.load(this, R.raw.alert, 1);
            if (0 == soundAlert)
                myApp.myToast(this, R.string.message_media_load_error);
        } else
            myApp.myToast(this, R.string.message_media_init_error);
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeMessages(1);
        if (null != myReceiveListener)
            myReceiveListener.closeAllHandler();
        if (null != serialPortUtil)
            serialPortUtil.closeSerialPort();
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        if (null != pDialog && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        super.onDestroy();
    }


}