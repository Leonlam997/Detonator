package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Dialog.MyProgressDialog;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;

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
                    new AlertDialog.Builder(ChargeActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_short_circuit)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .create().show();
                    break;
                case DETECT_INITIAL:
                    if (null != pDialog && pDialog.isShowing()) {
                        BaseApplication.releaseWakeLock(ChargeActivity.this);
                        pDialog.dismiss();
                    }
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    enabledButton(true);
                    break;
                case DETECT_CHARGE:
                    if (chargeCmdCount++ <= 1) {
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.INSTANT_OPEN_CAPACITOR, 0);
                        detectStatusHandler.sendEmptyMessageDelayed(DETECT_CHARGE, 1000);
                    } else {
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
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
    private int timeCount, elapseTime, dac;
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
    private float workVoltage;
    private EditText etTime, etVoltage;
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
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "1600###");
                    chargeCmdCount = 0;
                    pDialog = new MyProgressDialog(this);
                    pDialog.setInverseBackgroundForced(false);
                    pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pDialog.setCanceledOnTouchOutside(false);
                    pDialog.setTitle(R.string.progress_title);
                    pDialog.setMessage(getResources().getString(R.string.progress_charging));
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
                String received = myReceiveListener.getRcvData();
                if (received.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                    detectStatusHandler.sendEmptyMessage(DETECT_SHORT);
                } else if (received.contains(SerialCommand.INITIAL_FAIL)) {
                    myApp.myToast(ChargeActivity.this, R.string.message_open_module_fail);
                    finish();
                } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                    myReceiveListener.setRcvData("");
                    detectStatusHandler.sendEmptyMessage(DETECT_INITIAL);
                } else if (received.startsWith("V")) {
                    try {
                        float v = Integer.parseInt(received.substring(1)) / 100.0f;
                        if (Math.abs(v - workVoltage) < 0.1f) {
                            detectStatusHandler.sendEmptyMessage(DETECT_CHANGE_FINISHED);
                            myReceiveListener.setFeedback(false);
                            settingBean.getDacMap().put(workVoltage, dac);
                            myApp.saveSettings(settingBean);
                        } else {
                            dac += (v > workVoltage ? 100 : -100) * (Math.abs(v - workVoltage) - 0.01f);
                            if (dac < 50 || dac > 3000) {
                                dac = 94 + (int) ((24 - workVoltage) * 124);
                            }
                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
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
        myReceiveListener.setFeedback(true);
        if (null == settingBean.getDacMap())
            settingBean.setDacMap(new HashMap<>());
        Integer v = settingBean.getDacMap().get(vol);
        if (null != v) {
            dac = v;
        } else {
            dac = 94 + (int) ((24 - vol) * 124);
        }
        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
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