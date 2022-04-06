package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.KeyUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

public class CheckLineActivity extends BaseActivity {
    private final int DETECT_SUCCESS = 1,
            DETECT_CONTINUE = 2,
            DETECT_FAIL = 3,
            DETECT_INITIAL = 4,
            DETECT_ADDRESS = 6,
            DETECT_DELAY = 7,
            DETECT_SHORT = 8,
            DETECT_RESEND = 9,
            DETECT_VOLTAGE = 10;
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private final int initVoltage = 2790;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private SoundPool soundPool;
    private MyButton btnDetect, btnStatus;
    private TextView tvTube, tvDelayTime;
    private boolean startReceive = false, drawWaveform = false;
    private int resendCount = 0, delayTime, soundSuccess, soundFail, soundAlert, changeMode, keyCount = 0, cmdType;
    private String tempAddress;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_line);

        setTitle(R.string.single_check_detonator);

        myApp = (BaseApplication) getApplication();
        tvTube = findViewById(R.id.tv_tube);
        tvTube.setText("--");
        tvDelayTime = findViewById(R.id.tv_delay);
        tvDelayTime.setText(R.string.no_delay_time);
        btnDetect = findViewById(R.id.btn_detect);
        btnDetect.setOnClickListener(view -> {
            cmdType = 1;
            detectStatusHandler.sendEmptyMessage(DETECT_CONTINUE);
        });
        btnDetect.requestFocus();
        btnStatus = findViewById(R.id.btn_status);
        if (!newLG) {
            btnStatus.setOnClickListener(v -> {
                cmdType = 3;
                detectStatusHandler.sendEmptyMessage(DETECT_CONTINUE);
            });
        } else
            btnStatus.setVisibility(View.GONE);

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(CheckLineActivity.this, bufferRunnable);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            enableButton(false);
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(CheckLineActivity.this, R.string.message_open_module_fail);
            finish();
        }
        initSound();
    }

    private final Handler detectStatusHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_INITIAL:
                    startReceive = false;
                    enableButton(true);
                    break;
                case DETECT_SUCCESS: //检测成功
                    detectStatusHandler.removeMessages(DETECT_RESEND);
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    myReceiveListener.setRcvData("");
                    enableButton(true);
                    break;
                case DETECT_CONTINUE: //开始检测
                    detectStatusHandler.removeCallbacksAndMessages(null);
                    enableButton(false);
                    startReceive = newLG;
                    tempAddress = "";
                    tvTube.setText("--");
                    tvDelayTime.setText(R.string.no_delay_time);
                    resendCount = 0;
                    if (newLG) {
                        changeMode = 4;
                        detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                    } else {
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + initVoltage + "###");
                        detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.BOOST_TIME);
                    }
//                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_VOLTAGE, ConstantUtils.BOOT_TIME);
                    break;
                case DETECT_FAIL: //检测失败
                    detectStatusHandler.removeMessages(DETECT_RESEND);
                    myApp.playSoundVibrate(soundPool, soundFail);
                    enableButton(true);
                    tempAddress = "";
//                    SemiProductDialog myDialog = new SemiProductDialog(CheckLineActivity.this);
//                    myDialog.setStyle(4);
//                    myDialog.setTitleId(R.string.dialog_unqualified);
//                    myDialog.setCode((String) msg.obj);
//                    myDialog.setAutoClose(false);
//                    myDialog.setCanceledOnTouchOutside(false);
//                    myDialog.show();
                    break;
                case DETECT_ADDRESS:
                    tvTube.setText(tempAddress);
                    break;
                case DETECT_DELAY:
                    tvDelayTime.setText(String.format(Locale.CHINA, "%dms", delayTime));
                    break;
                case DETECT_SHORT:
                    detectStatusHandler.removeMessages(DETECT_RESEND);
                    if (myReceiveListener != null) {
                        myReceiveListener.closeAllHandler();
                        myReceiveListener = null;
                    }
                    if (serialPortUtil != null) {
                        serialPortUtil.closeSerialPort();
                        serialPortUtil = null;
                    }
                    myApp.playSoundVibrate(soundPool, soundAlert);
                    new AlertDialog.Builder(CheckLineActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_short_circuit)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .create().show();
                    break;
                case DETECT_VOLTAGE:
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + initVoltage + "###");
                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.BOOST_TIME);
                    break;
                case DETECT_RESEND:
                    startReceive = true;
                    myReceiveListener.setRcvData("");
                    if (newLG)
                        switch (changeMode) {
                            case 1:
                                resendCount = 0;
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                                detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 2:
                                resendCount = 0;
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_INITIAL_VOLTAGE, ConstantUtils.DEFAULT_SINGLE_WORK_VOLTAGE, ConstantUtils.DEFAULT_SINGLE_WORK_VOLTAGE));
                                detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 4:
                                resendCount = 0;
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 1));
                                detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 6:
                                resendCount = 0;
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_SIGNAL_WAVEFORM, 1));
                                detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 8:
                                resendCount = 0;
                                serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_SN, 0);
                                detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, 1500);
                                return false;
                            case 9:
                                myApp.myToast(CheckLineActivity.this, R.string.message_waveform_data_error);
                                detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                                return false;
                        }
                    if (resendCount >= ConstantUtils.RESEND_TIMES) {
                        myApp.myToast(CheckLineActivity.this, R.string.message_detonator_not_detected);
                        detectStatusHandler.sendEmptyMessage(DETECT_FAIL);
                        return false;
                    }
                    switch (cmdType) {
                        case 1:
                            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_SN, 0);
                            break;
                        case 2:
                            serialPortUtil.sendCmd(tempAddress, SerialCommand.ACTION_TYPE.GET_DELAY, 0);
                            break;
                        case 3:
                            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_TEST_TIMES, 0);
                            break;
                    }
                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                    resendCount++;
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            String received = myReceiveListener.getRcvData();
//            myApp.myToast(CheckLineActivity.this, received);
            if (received.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                if (startReceive) {
                    myApp.myToast(CheckLineActivity.this, R.string.message_capacity_large_current);
                    detectStatusHandler.sendEmptyMessage(DETECT_FAIL);
                } else {
                    detectStatusHandler.sendEmptyMessage(DETECT_SHORT);
                }
            } else if (received.contains(SerialCommand.INITIAL_FAIL)) {
                myApp.myToast(CheckLineActivity.this, R.string.message_open_module_fail);
                finish();
            } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                myReceiveListener.setRcvData("");
                if (newLG) {
                    startReceive = true;
                    changeMode = 1;
                    detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                } else {
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + initVoltage + "###");
                    detectStatusHandler.sendEmptyMessage(DETECT_INITIAL);
                }
            } else if (startReceive) {
                if (newLG) {
                    if (received.contains(SerialCommand.AT_CMD_RESPOND)) {
                        switch (changeMode) {
                            case 1:
                                changeMode++;
                                detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                                break;
                            case 2:
                                changeMode++;
                                detectStatusHandler.removeMessages(DETECT_RESEND);
                                detectStatusHandler.sendEmptyMessageDelayed(DETECT_INITIAL, ConstantUtils.INITIAL_VOLTAGE_DELAY);
                                break;
                            case 4:
                                if (drawWaveform)
                                    changeMode = 8;
                                else
                                    changeMode++;
                                detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                                break;
                            case 6:
                                changeMode = 4;
                                detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                                break;
                        }
                        myReceiveListener.setRcvData("");
                    } else if (received.startsWith(SerialCommand.DATA_PREFIX) && received.length() >= 10 && serialPortUtil.checkData(received)) {
                        tempAddress = received.substring(6, received.length() - 2);
                        if (1 == cmdType) {
                            char character = (char) (int) Integer.valueOf(tempAddress.substring(4, 6), 16);
                            if (character == 0xff)
                                character = 'F';
                            tempAddress = tempAddress.substring(0, 4) + tempAddress.substring(6, 9) + character + tempAddress.substring(9);
                            myReceiveListener.setRcvData("");
                            resendCount = 0;
                            cmdType = 2;
                            detectStatusHandler.removeMessages(DETECT_RESEND);
                            detectStatusHandler.sendEmptyMessage(DETECT_ADDRESS);
                            detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                        } else {
                            detectStatusHandler.removeMessages(DETECT_RESEND);
                            delayTime = Integer.parseInt(tempAddress, 16);
                            detectStatusHandler.sendEmptyMessage(DETECT_DELAY);
                            detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                        }
                    } else if (drawWaveform) {
                        if (received.length() == 2048) {
                            Intent intent = new Intent(CheckLineActivity.this, WaveformActivity.class);
                            intent.putExtra(KeyUtils.KEY_WAVEFORM_DATA, received);
                            startActivity(intent);
                            detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                        } else {
                            myApp.myToast(CheckLineActivity.this, received.length() + "");
                            changeMode = 9;
                        }
                    }
                } else {
                    String confirm;
                    switch (cmdType) {
                        case 1:
                            confirm = SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_SN);
                            assert confirm != null;
                            if (received.contains(confirm)) {
                                received = received.substring(received.indexOf(confirm) + confirm.length());
//                        myApp.myToast(CheckLineActivity.this, received);
                                if (received.length() >= 16) {
                                    tempAddress = received.substring(0, 14);
                                    detectStatusHandler.removeMessages(DETECT_RESEND);
                                    if (tempAddress.startsWith("FF")) {
                                        //sendMsg(DETECT_FAIL, R.string.message_detonator_no_code);
                                        tempAddress = "FFFFFFFFFFFFFF";
                                        myReceiveListener.setRcvData("");
                                        detectStatusHandler.sendEmptyMessage(DETECT_ADDRESS);
                                        resendCount = 0;
                                    } else {
//                                sendMsg(DETECT_MESSAGE, tempAddress);
                                        try {
                                            int checkSum = 0;
                                            for (int j = 0; j < tempAddress.length(); j += 2) {
                                                byte a = (byte) Integer.parseInt(tempAddress.substring(j, j + 2), 16);
                                                checkSum += a;
                                            }
                                            if (String.format("%02X", checkSum).endsWith(received.substring(14, 16))) {
                                                char character = (char) (int) Integer.valueOf(tempAddress.substring(12), 16);
                                                tempAddress = tempAddress.substring(0, 7) + character + tempAddress.substring(7, 12);
                                                myReceiveListener.setRcvData("");
                                                detectStatusHandler.removeMessages(DETECT_RESEND);
                                                detectStatusHandler.sendEmptyMessage(DETECT_ADDRESS);
                                                resendCount = 0;
                                                cmdType = 2;
                                            }
                                            detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                                        } catch (Exception e) {
                                            BaseApplication.writeErrorLog(e);
                                        }
                                    }
                                }
                            }
                            break;
                        case 2:
                            confirm = SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.GET_DELAY);
                            assert confirm != null;
                            if (received.contains(confirm)) {
                                detectStatusHandler.removeMessages(DETECT_RESEND);
                                received = received.substring(received.indexOf(confirm) + confirm.length());
                                if (received.length() > 4)
                                    received = received.substring(0, 4);
                                delayTime = Integer.parseInt(received, 16);
                                detectStatusHandler.sendEmptyMessage(DETECT_DELAY);
                                detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                            }
                            break;
                        case 3:
                            confirm = SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_TEST_TIMES);
                            assert confirm != null;
                            if (received.contains(confirm)) {
                                detectStatusHandler.removeMessages(DETECT_RESEND);
                                try {
                                    int times = Integer.parseInt(received.substring(received.indexOf(confirm) + confirm.length()), 16);
                                    switch (times) {
                                        case 2:
                                            myApp.myToast(CheckLineActivity.this, R.string.message_status_charge_only);
                                            break;
                                        case 3:
                                            myApp.myToast(CheckLineActivity.this, R.string.message_status_charge_explode);
                                            break;
                                        case 4:
                                            myApp.myToast(CheckLineActivity.this, R.string.message_status_explode_only);
                                            break;
                                        default:
                                            myApp.myToast(CheckLineActivity.this, "数据："+times);
                                            break;
                                    }
                                } catch (Exception e) {
                                    myApp.myToast(CheckLineActivity.this, R.string.message_status_error);
                                }
                                detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                            }
                            break;
                    }
                }
            }
        }
    };

    private void enableButton(boolean enable) {
        startReceive = !enable;
        setProgressVisibility(!enable);
        btnDetect.setEnabled(enable);
        btnStatus.setEnabled(enable);
        myReceiveListener.setMaxCurrent(enable ? ConstantUtils.MAXIMUM_CURRENT : 500);
        myReceiveListener.setStartAutoDetect(enable);
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                cmdType = 1;
                changeMode = 0;
                detectStatusHandler.sendEmptyMessage(DETECT_CONTINUE);
                keyCount = 0;
                break;
            case KeyEvent.KEYCODE_STAR:
                if (btnDetect.isEnabled()) {
                    if (keyCount < 2)
                        keyCount++;
                    else if (keyCount > 3)
                        keyCount++;
                    else if (keyCount != 2)
                        keyCount = 0;
                    if (keyCount == 6) {
                        keyCount = 0;
                        drawWaveform = true;
                        changeMode = 6;
                        enableButton(false);
                        detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                    }
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (!newLG) {
                    cmdType = 3;
                    detectStatusHandler.sendEmptyMessage(DETECT_CONTINUE);
                }
                keyCount = 0;
                break;
            case KeyEvent.KEYCODE_0:
                if (keyCount == 2)
                    keyCount++;
                break;
            case KeyEvent.KEYCODE_7:
                if (keyCount == 3)
                    keyCount++;
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDestroy() {
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        detectStatusHandler.removeCallbacksAndMessages(null);
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
