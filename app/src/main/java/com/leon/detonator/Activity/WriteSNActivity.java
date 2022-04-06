package com.leon.detonator.Activity;

import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;

import java.io.FileWriter;
import java.util.Locale;
import java.util.Objects;

public class WriteSNActivity extends BaseActivity {
    private final int detectVoltage = 2800;
    private final int HANDLER_SUCCESS = 1,
            HANDLER_FAIL = 2,
            HANDLER_RESEND = 3,
            HANDLER_SCAN = 4;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private EditText etNumber, etDelay, etPeriod;
    private TextView tvAuto;
    private int resendCount, soundSuccess, soundFail, changeMode;
    private Button btnWrite, btnDelay;
    private boolean setDelay, scanMode, startReceive;
    private LocalSettingBean settings;
    private String tempAddress;
    private SoundPool soundPool;
    private BaseApplication myApp;

    private void analyzeCode(String received) {
        myReceiveListener.setRcvData("");
        if (!settings.isNewLG())
            received = received.substring(0, received.indexOf(SerialCommand.RESPOND_SUCCESS));
        else {
            received = received.split("\\+")[1];
            received = received.substring(received.indexOf(SerialCommand.SCAN_RESPOND) + SerialCommand.SCAN_RESPOND.length());
        }
        received = received.replace("\1", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "");
        if (received.length() == 13) {
            tempAddress = received;
            myApp.playSoundVibrate(soundPool, soundSuccess);
            myHandler.sendEmptyMessage(HANDLER_SCAN);
        } else {
            myHandler.sendEmptyMessage(HANDLER_RESEND);
        }
    }

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case HANDLER_SCAN:
                    etNumber.setText(tempAddress);
                case HANDLER_FAIL:
                case HANDLER_SUCCESS:
                    myReceiveListener.setRcvData("");
                    scanMode = false;
                    myReceiveListener.setScanMode(false);
                    if (msg.what == HANDLER_FAIL)
                        myApp.playSoundVibrate(soundPool, soundFail);
                    else
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                    myHandler.removeCallbacksAndMessages(null);
                    enabledButton(true);
                    break;
                case HANDLER_RESEND:
                    startReceive = true;
                    myHandler.removeMessages(HANDLER_RESEND);
                    myReceiveListener.setRcvData("");
                    resendCount++;
                    if (scanMode) {
                        if (resendCount >= ConstantUtils.RESEND_TIMES) {
                            myApp.myToast(WriteSNActivity.this, R.string.message_scan_timeout);
                            myHandler.sendEmptyMessage(HANDLER_FAIL);
                        } else {
                            serialPortUtil.sendCmd(SerialCommand.CMD_SCAN);
                        }
                        myHandler.sendEmptyMessageDelayed(HANDLER_RESEND, ConstantUtils.SCAN_TIMEOUT);
                    } else {
                        if (4 == changeMode) {
                            resendCount = 0;
                            serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 1));
                        } else if (setDelay) {
                            if (resendCount >= ConstantUtils.RESEND_TIMES) {
                                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_not_detected);
                                myHandler.sendEmptyMessage(HANDLER_FAIL);
                            } else {
                                serialPortUtil.sendCmd(etNumber.getText().toString(), settings.isNewLG() ? SerialCommand.ACTION_TYPE.SHORT_CMD2_SET_DELAY : SerialCommand.ACTION_TYPE.SET_DELAY, Integer.parseInt(etDelay.getText().toString()));
                            }
                        } else {
                            if (resendCount >= ConstantUtils.RESEND_TIMES) {
                                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_not_detected);
                                myHandler.sendEmptyMessage(HANDLER_FAIL);
                            } else {
                                serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.ACTION_TYPE.WRITE_SN, 0);
                            }
                        }
                        myHandler.sendEmptyMessageDelayed(HANDLER_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                    }
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_sn);

        setTitle(R.string.write_number);

        myApp = (BaseApplication) getApplication();
        etNumber = findViewById(R.id.etSerialNumber);
        etDelay = findViewById(R.id.etDelay);
        etPeriod = findViewById(R.id.etPeriod);
        btnWrite = findViewById(R.id.btn_write);
        btnDelay = findViewById(R.id.btn_delay);
        tvAuto = findViewById(R.id.tv_autoHints);
        setDelay = false;
        startReceive = false;

        initData();
        initSound();
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(WriteSNActivity.this, bufferRunnable);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        enabledButton(false);
        etNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(13)});
        etNumber.setKeyListener(new NumberKeyListener() {
            @NonNull
            @Override
            protected char[] getAcceptedChars() {
                return ConstantUtils.INPUT_DETONATOR_ACCEPT.toCharArray();
            }

            @Override
            public int getInputType() {
                return InputType.TYPE_TEXT_VARIATION_PASSWORD;
            }
        });

        findViewById(R.id.btn_increase).setOnClickListener(v -> etNumber.setText(String.format(Locale.CHINA, "%s%05d", etNumber.getText().toString().substring(0, 8), Long.parseLong(etNumber.getText().toString().substring(8)) + 1)));
        findViewById(R.id.btn_decrease).setOnClickListener(v -> etNumber.setText(String.format(Locale.CHINA, "%s%05d", etNumber.getText().toString().substring(0, 8), Long.parseLong(etNumber.getText().toString().substring(8)) - 1)));
        //btnWrite.setEnabled(false);
        btnWrite.setOnClickListener(v -> {
            if (etNumber.getText().length() != 13) {
                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_input_error);
            } else {
                setDelay = false;
                enabledButton(false);
                myHandler.removeCallbacksAndMessages(null);
                if (settings.isNewLG())
                    changeMode = 4;
                myHandler.sendEmptyMessage(HANDLER_RESEND);
            }
        });

        btnDelay.setOnClickListener(v -> {
            if (etDelay.getText().toString().isEmpty()) {
                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_delay_input_error);
            } else {
                try {
                    if (Integer.parseInt(etDelay.getText().toString()) >= 0) {
                        setDelay = true;
                        enabledButton(false);
                        myHandler.removeCallbacksAndMessages(null);
                        if (settings.isNewLG())
                            changeMode = 4;
                        myHandler.sendEmptyMessage(HANDLER_RESEND);
                    }
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                    myApp.myToast(WriteSNActivity.this, R.string.message_detonator_delay_input_error);
                }
            }
        });
    }

    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            String received = myReceiveListener.getRcvData();
            if (received.contains(SerialCommand.INITIAL_FAIL)) {
                myApp.myToast(WriteSNActivity.this, R.string.message_open_module_fail);
                finish();
            } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                myReceiveListener.setRcvData("");
                if (!settings.isNewLG()) {
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + detectVoltage + "###");
                    myHandler.sendEmptyMessage(HANDLER_SUCCESS);
                } else {
                    changeMode = 1;
                    serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                }
            } else {
                //sendMsg(2, received);
                if (settings.isNewLG()) {
                    if (received.contains(SerialCommand.SCAN_RESPOND)) {
                        if (scanMode && received.length() > 10)
                            analyzeCode(received);
                    } else if (received.contains(SerialCommand.AT_CMD_RESPOND)) {
                        switch (changeMode) {
                            case 1:
                                changeMode++;
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_INITIAL_VOLTAGE, ConstantUtils.DEFAULT_SINGLE_WORK_VOLTAGE, ConstantUtils.DEFAULT_SINGLE_WORK_VOLTAGE));
                                break;
                            case 2:
                                changeMode++;
                                myHandler.sendEmptyMessageDelayed(HANDLER_SUCCESS, ConstantUtils.INITIAL_VOLTAGE_DELAY);
                                break;
                            case 4:
                                changeMode++;
                                myHandler.sendEmptyMessage(HANDLER_RESEND);
                                break;
                        }
                        myReceiveListener.setRcvData("");
                    } else if (received.startsWith(SerialCommand.DATA_PREFIX)) {
                        myHandler.sendEmptyMessage(HANDLER_SUCCESS);
                    }
                } else if (scanMode) {
                    myHandler.removeMessages(HANDLER_RESEND);
                    if (received.contains(SerialCommand.RESPOND_SUCCESS)) {
                        analyzeCode(received);
                    }
                } else if (startReceive) {
                    if (setDelay) {
                        if (received.contains(Objects.requireNonNull(settings.isNewLG() ? SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.SET_DELAY) :
                                SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.SET_DELAY)))) {
                            myHandler.sendEmptyMessage(HANDLER_SUCCESS);
                        }
                    } else {
                        if (received.contains(Objects.requireNonNull(settings.isNewLG() ? SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.WRITE_SN) :
                                SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.WRITE_SN)))) {
                            myHandler.sendEmptyMessage(HANDLER_SUCCESS);
                        }
                    }
                }
            }
        }
    };

    private void enabledButton(boolean enabled) {
        resendCount = 0;
        myReceiveListener.setRcvData("");
        setProgressVisibility(!enabled);
        myReceiveListener.setStartAutoDetect(enabled);
        findViewById(R.id.btn_increase).setEnabled(enabled);
        findViewById(R.id.btn_decrease).setEnabled(enabled);
        btnWrite.setEnabled(enabled);
        etNumber.setEnabled(enabled);
        etDelay.setEnabled(enabled);
    }

    private void initData() {
        settings = BaseApplication.readSettings();
        if (settings.getSerialNum() != null && !settings.getSerialNum().isEmpty())
            etNumber.setText(settings.getSerialNum());
        if (settings.getDelayTime() != null && !settings.getDelayTime().isEmpty())
            etDelay.setText(settings.getDelayTime());
        if (settings.getDelayPeriod() != null && !settings.getDelayPeriod().isEmpty())
            etPeriod.setText(settings.getDelayPeriod());
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_B) {
            myReceiveListener.setStartAutoDetect(false);
            setProgressVisibility(true);
            myReceiveListener.setScanMode(true);
            scanMode = true;
            resendCount = 0;
            //btnWrite.setEnabled(false);
            myHandler.removeCallbacksAndMessages(null);
            myHandler.sendEmptyMessage(HANDLER_RESEND);
        }
        return super.onKeyUp(keyCode, event);
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
        } else
            myApp.myToast(this, R.string.message_media_init_error);
    }

    @Override
    public void onDestroy() {
        settings.setSerialNum(etNumber.getText().toString());
        settings.setDelayTime(etDelay.getText().toString());
        settings.setDelayPeriod(etPeriod.getText().toString());
        myApp.saveSettings(settings);
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.release();
            soundPool = null;
        }
        try {
            FileWriter fw = new FileWriter(FilePath.FILE_LOCAL_SETTINGS);
            fw.append(new Gson().toJson(settings));
            fw.close();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        if (null != myHandler) {
            myHandler.removeCallbacksAndMessages(null);
            myHandler = null;
        }
        myReceiveListener.closeAllHandler();
        myReceiveListener = null;
        if (serialPortUtil != null)
            serialPortUtil.closeSerialPort();
        super.onDestroy();
    }


}
