package com.leon.detonator.activity;

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

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public class WriteSNActivity extends BaseActivity {
    private final int HANDLER_SUCCESS = 1;
    private final int HANDLER_FAIL = 2;
    private final int HANDLER_RESEND = 3;
    private final int HANDLER_SCAN = 4;
    private final int HANDLER_NEXT_STEP = 5;
    private final int STEP_WRITE_CONFIG = 1;
    private final int STEP_WRITE_UID = 2;
    private final int STEP_WRITE_PSW = 3;
    private final int STEP_WRITE_SHELL = 4;
    private final int STEP_READ_SHELL = 5;
    private final int STEP_WRITE_DELAY = 6;
    private final int STEP_SCAN_CODE = 7;
    private final int STEP_WRITE_CLOCK = 8;
    private final int STEP_LOCK = 9;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private EditText etNumber;
    private EditText etDelay;
    private EditText etPeriod;
    private int flowStep;
    private int soundSuccess;
    private int soundFail;
    private Button btnWrite;
    private Button btnDelay;
    private boolean startReceive;
    private LocalSettingBean settings;
    private String tempAddress;
    private SoundPool soundPool;
    private BaseApplication myApp;

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case HANDLER_SCAN:
                    etNumber.setText(tempAddress);
                case HANDLER_FAIL:
                case HANDLER_SUCCESS:
                    if (msg.what == HANDLER_FAIL)
                        myApp.playSoundVibrate(soundPool, soundFail);
                    else
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                    myHandler.removeCallbacksAndMessages(null);
                    enabledButton(true);
                    break;
                case HANDLER_NEXT_STEP:
                    switch (flowStep) {
                        case STEP_WRITE_CLOCK:
                            flowStep = STEP_WRITE_CONFIG;
                            break;
                        case STEP_WRITE_CONFIG:
                            flowStep = STEP_WRITE_UID;
                            break;
                        case STEP_WRITE_UID:
                            flowStep = STEP_WRITE_PSW;
                            break;
                        case STEP_WRITE_PSW:
                            flowStep = STEP_WRITE_SHELL;
                            break;
                        case STEP_WRITE_SHELL:
                            flowStep = STEP_READ_SHELL;
                            break;
                        case STEP_READ_SHELL:
                            flowStep = STEP_LOCK;
                            break;
                    }
                case HANDLER_RESEND:
                    startReceive = true;
                    myHandler.removeMessages(HANDLER_RESEND);
                    switch (flowStep) {
                        case STEP_WRITE_CLOCK:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_WRITE_CLOCK, 0);
                            break;
                        case STEP_WRITE_CONFIG:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_SINGLE_WRITE_CONFIG, ConstantUtils.UID_LEN, ConstantUtils.PSW_LEN, ConstantUtils.DETONATOR_VERSION);
                            break;
                        case STEP_WRITE_UID:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_WRITE_UID, 0);
                            break;
                        case STEP_WRITE_PSW:
                            serialPortUtil.sendCmd(ConstantUtils.EXPLODE_PSW, SerialCommand.CODE_WRITE_PSW, 0);
                            break;
                        case STEP_WRITE_SHELL:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_WRITE_SHELL, 0);
                            break;
                        case STEP_READ_SHELL:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                            break;
                        case STEP_LOCK:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_LOCK, 0);
                            break;
                        case STEP_WRITE_DELAY:
                            serialPortUtil.sendCmd(etNumber.getText().toString(), SerialCommand.CODE_SINGLE_WRITE_FIELD, 0, Integer.parseInt(etDelay.getText().toString()), 0);
                            break;
                        case STEP_SCAN_CODE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                            myHandler.sendEmptyMessageDelayed(HANDLER_RESEND, ConstantUtils.RESEND_SCAN_TIMEOUT);
                            return false;
                    }
                    myHandler.sendEmptyMessageDelayed(HANDLER_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
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
        startReceive = false;

        initData();
        initSound();
        BaseApplication.writeFile(getString(R.string.write_number));
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(WriteSNActivity.this, bufferRunnable);
            myReceiveListener.setSingleConnect(true);
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

        findViewById(R.id.btn_increase).setOnClickListener(v -> etNumber.setText(String.format(Locale.getDefault(), "%s%05d", etNumber.getText().toString().substring(0, 8), (Long.parseLong(etNumber.getText().toString().substring(8)) + 1) % 100000)));
        findViewById(R.id.btn_decrease).setOnClickListener(v -> etNumber.setText(String.format(Locale.getDefault(), "%s%05d", etNumber.getText().toString().substring(0, 8), (Long.parseLong(etNumber.getText().toString().substring(8)) - 1) % 100000)));
        //btnWrite.setEnabled(false);
        btnWrite.setOnClickListener(v -> {
            if (!Pattern.matches(ConstantUtils.SHELL_PATTERN, etNumber.getText().toString().toUpperCase())) {
                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_input_error);
            } else {
                etNumber.setText(etNumber.getText().toString().toUpperCase());
                enabledButton(false);
                myHandler.removeCallbacksAndMessages(null);
                flowStep = STEP_WRITE_CLOCK;
                myHandler.sendEmptyMessage(HANDLER_RESEND);
                BaseApplication.writeFile(getString(R.string.write_number) + ", " + etNumber.getText().toString());
            }
        });

        btnDelay.setOnClickListener(v -> {
            if (etDelay.getText().toString().isEmpty()) {
                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_delay_input_error);
            } else {
                try {
                    if (Integer.parseInt(etDelay.getText().toString()) >= 0) {
                        enabledButton(false);
                        flowStep = STEP_WRITE_DELAY;
                        myHandler.removeCallbacksAndMessages(null);
                        myHandler.sendEmptyMessage(HANDLER_RESEND);
                        BaseApplication.writeFile(getString(R.string.button_set_delay) + ", " + etDelay.getText().toString());
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
            byte[] received = myReceiveListener.getRcvData();

            if (received[0] == SerialCommand.INITIAL_FINISHED) {
                myHandler.sendEmptyMessage(HANDLER_SUCCESS);
            } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                myApp.myToast(WriteSNActivity.this, R.string.message_open_module_fail);
                finish();
            } else {
                if (startReceive) {
                    myHandler.removeMessages(HANDLER_RESEND);
                    if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                        switch (flowStep) {
                            case STEP_READ_SHELL:
                                if (!etNumber.getText().toString().equals(new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15)))) {
                                    BaseApplication.writeFile(getString(R.string.message_detonator_read_shell_error));
                                    myHandler.sendEmptyMessage(HANDLER_FAIL);
                                    return;
                                }
                                break;
                            case STEP_LOCK:
                            case STEP_WRITE_DELAY:
                                myApp.myToast(WriteSNActivity.this, R.string.message_detonator_write_success);
                                runOnUiThread(() -> {
                                    try {
                                        etNumber.setText(String.format(Locale.getDefault(), "%s%06d", etNumber.getText().toString().substring(0, 7), Integer.parseInt(etNumber.getText().toString().substring(7)) + 1));
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                });
                                myHandler.sendEmptyMessage(HANDLER_SUCCESS);
                                return;
                            case STEP_SCAN_CODE:
                                if (received.length < 9) {
                                    BaseApplication.writeFile(getString(R.string.message_scan_timeout));
                                    myHandler.sendEmptyMessage(HANDLER_FAIL);
                                } else {
                                    tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                                    myHandler.sendEmptyMessage(HANDLER_SCAN);
                                }
                                return;
                        }
                        myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                    } else {
                        BaseApplication.writeFile(getString(R.string.message_detonator_not_detected));
                        myHandler.sendEmptyMessage(HANDLER_FAIL);
                    }
                }
            }
        }
    };

    private void enabledButton(boolean enabled) {
        setProgressVisibility(!enabled);
        myReceiveListener.setStartAutoDetect(enabled);
        findViewById(R.id.btn_increase).setEnabled(enabled);
        findViewById(R.id.btn_decrease).setEnabled(enabled);
        btnWrite.setEnabled(enabled);
        btnDelay.setEnabled(enabled);
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
            BaseApplication.writeFile(getString(R.string.button_scan));
            myReceiveListener.setStartAutoDetect(false);
            setProgressVisibility(true);
            flowStep = STEP_SCAN_CODE;
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
        myApp.saveBean(settings);
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
