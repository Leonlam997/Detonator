package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Dialog.SemiProductDialog;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

public class CheckLineActivity extends BaseActivity {
    private final int DETECT_SUCCESS = 1,
            DETECT_CONTINUE = 2,
            DETECT_FAIL = 3,
            DETECT_INITIAL = 4,
            DETECT_MESSAGE = 5,
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
    private MyButton btnDetect;
    private TextView tvTube, tvDelayTime;
    private boolean getDelayTime = false, startReceive = false;
    private int resendCount = 0, delayTime, soundSuccess, soundFail, soundAlert;
    private String tempAddress;
    private BaseApplication myApp;
    private final Handler detectStatusHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_INITIAL:
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
                    startReceive = false;
                    tempAddress = "";
                    tvTube.setText("--");
                    tvDelayTime.setText(R.string.no_delay_time);
                    resendCount = 0;
                    getDelayTime = false;
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + initVoltage + "###");
                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.BOOST_TIME);
//                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_VOLTAGE, ConstantUtils.BOOT_TIME);
                    break;
                case DETECT_FAIL: //检测失败
                    detectStatusHandler.removeMessages(DETECT_RESEND);
                    myApp.playSoundVibrate(soundPool, soundFail);
                    enableButton(true);
                    tempAddress = "";
                    SemiProductDialog myDialog = new SemiProductDialog(CheckLineActivity.this);
                    myDialog.setStyle(4);
                    myDialog.setTitleId(R.string.dialog_unqualified);
                    myDialog.setCode((String) msg.obj);
                    myDialog.setAutoClose(false);
                    myDialog.setCanceledOnTouchOutside(false);
                    myDialog.show();
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
                    if (!getDelayTime) {
                        if (resendCount >= ConstantUtils.RESEND_TIMES) {
                            sendMsg(DETECT_FAIL, R.string.message_detonator_not_detected);
                            break;
                        } else {
                            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_SN, 0);
                        }
                    } else {
                        if (resendCount >= ConstantUtils.RESEND_TIMES) {
                            resendCount = 0;
                            getDelayTime = false;
                            sendMsg(DETECT_FAIL, R.string.message_detonator_not_detected);
                            break;
                        } else {
                            serialPortUtil.sendCmd(tempAddress, SerialCommand.ACTION_TYPE.GET_DELAY, 0);
                        }
                    }
                    detectStatusHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.RESEND_CMD_TIMEOUT);
                    resendCount++;
                    break;
                default:
                    myApp.myToast(CheckLineActivity.this, (String) msg.obj);
                    break;
            }
            return false;
        }
    });
    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            String received = myReceiveListener.getRcvData();
//            sendMsg(DETECT_MESSAGE, received);
            if (received.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                if (startReceive) {
                    sendMsg(DETECT_FAIL, R.string.message_capacity_large_current);
                } else {
                    detectStatusHandler.sendEmptyMessage(DETECT_SHORT);
                }
            } else if (received.contains(SerialCommand.INITIAL_FAIL)) {
                myApp.myToast(CheckLineActivity.this, R.string.message_open_module_fail);
                finish();
            } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                myReceiveListener.setRcvData("");
                detectStatusHandler.sendEmptyMessage(DETECT_INITIAL);
                if (newLG)
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.SET_VOLTAGE, 90);
                else
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + initVoltage + "###");
            } else if (startReceive) {
                if (!getDelayTime) {
                    String confirm = newLG ? SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_SN) : SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_SN);
                    assert confirm != null;
                    if (received.contains(confirm)) {
                        received = received.substring(received.indexOf(confirm) + confirm.length() - (newLG ? 2 : 0));
                        if (newLG && received.contains(SerialCommand.STRING_DATA_END))
                            received = received.substring(0, received.length() - SerialCommand.STRING_DATA_END.length());
//                        myApp.myToast(CheckLineActivity.this, received);
                        if (received.length() >= (newLG ? 20 : 16)) {
                            tempAddress = received.substring(0, newLG ? 18 : 14);
                            detectStatusHandler.removeMessages(DETECT_RESEND);
                            if (!newLG && tempAddress.startsWith("FF")) {
                                //sendMsg(DETECT_FAIL, R.string.message_detonator_no_code);
                                tempAddress = "FFFFFFFFFFFFFF";
                                myReceiveListener.setRcvData("");
                                detectStatusHandler.sendEmptyMessage(DETECT_ADDRESS);
                                resendCount = 0;
                            } else {
//                                sendMsg(DETECT_MESSAGE, tempAddress);
                                try {
                                    int checkSum = 0;
                                    StringBuilder temp = new StringBuilder();
                                    for (int j = 0; j < tempAddress.length(); j += 2) {
                                        byte a = (byte) Integer.parseInt(tempAddress.substring(j, j + 2), 16);
                                        checkSum += a;
                                        if (newLG && j >= 4) {
                                            a ^= SerialCommand.XOR_DATA;
                                            temp.append(String.format("%02X", a));
                                        }
                                    }
                                    if (newLG)
                                        tempAddress = temp.toString();
                                    if (String.format("%02X", checkSum).endsWith(received.substring(newLG ? 18 : 14, newLG ? 20 : 16))) {
                                        char character = (char) (int) Integer.valueOf(tempAddress.substring(12), 16);
                                        tempAddress = tempAddress.substring(0, 7) + character + tempAddress.substring(7, 12);
                                        myReceiveListener.setRcvData("");
                                        detectStatusHandler.removeMessages(DETECT_RESEND);
                                        detectStatusHandler.sendEmptyMessage(DETECT_ADDRESS);
                                        resendCount = 0;
                                        getDelayTime = true;
                                    }
                                    detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                                } catch (Exception e) {
                                    BaseApplication.writeErrorLog(e);
                                }
                            }
                        }
                    } else if (received.contains(SerialCommand.RESPOND_FAIL)) {
                        detectStatusHandler.removeMessages(DETECT_RESEND);
                        myReceiveListener.setRcvData("");
                        if (getDelayTime)
                            sendMsg(DETECT_FAIL, R.string.message_network_timeout);
                        else
                            sendMsg(DETECT_FAIL, R.string.message_detonator_not_detected);
                    }
                } else {
                    String confirm = newLG ? SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.GET_DELAY) : SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.GET_DELAY);
                    assert confirm != null;
                    if (received.contains(confirm)) {
                        detectStatusHandler.removeMessages(DETECT_RESEND);
                        if (newLG) {
                            if (received.contains(SerialCommand.STRING_DATA_END) && received.lastIndexOf(confirm) + 2 < received.lastIndexOf(SerialCommand.STRING_DATA_END))
                                received = received.substring(received.lastIndexOf(confirm) + 2, received.lastIndexOf(SerialCommand.STRING_DATA_END));
                            else
                                received = received.substring(received.lastIndexOf(confirm) + 2);
                            int checkSum = 0;
                            for (int i = 0; i < received.length() - 2; i += 2) {
                                checkSum += Integer.parseInt(received.substring(i, i + 2), 16);
                            }
                            if (String.format("%02X", checkSum).endsWith(received.substring(received.length() - 2))) {
                                byte a = (byte) Integer.parseInt(received.substring(4, 6), 16),
                                        b = (byte) Integer.parseInt(received.substring(6, 8), 16);
                                a ^= SerialCommand.XOR_DATA;
                                b ^= SerialCommand.XOR_DATA;
                                delayTime = (a & 0xFF) + (b & 0xFF) * 0x100;
                                if (delayTime >= ConstantUtils.PRESET_DELAY)
                                    delayTime -= ConstantUtils.PRESET_DELAY;

                                detectStatusHandler.sendEmptyMessage(DETECT_DELAY);
                                detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                            } else
                                detectStatusHandler.sendEmptyMessage(DETECT_RESEND);
                        } else {
                            received = received.substring(received.indexOf(confirm) + confirm.length());
                            if (received.length() > 4)
                                received = received.substring(0, 4);
                            delayTime = Integer.parseInt(received, 16);
                            detectStatusHandler.sendEmptyMessage(DETECT_DELAY);
                            detectStatusHandler.sendEmptyMessage(DETECT_SUCCESS);
                        }
                    }
                }
            }
        }
    };

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
        btnDetect.setOnClickListener(view -> detectStatusHandler.sendEmptyMessage(DETECT_CONTINUE));
        btnDetect.requestFocus();

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(CheckLineActivity.this, bufferRunnable);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            enableButton(false);
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            sendMsg(DETECT_MESSAGE, R.string.message_open_module_fail);
            finish();
        }
        initSound();
    }


    private void enableButton(boolean enable) {
        startReceive = !enable;
        setProgressVisibility(!enable);
        btnDetect.setEnabled(enable);
        myReceiveListener.setMaxCurrent(enable ? ConstantUtils.MAXIMUM_CURRENT : 500);
        myReceiveListener.setStartAutoDetect(enable);
    }

    private void sendMsg(int what, @StringRes int hint) {
        Message msg = detectStatusHandler.obtainMessage(what);
        msg.obj = getResources().getString(hint);
        detectStatusHandler.sendMessage(msg);
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
        if (KeyEvent.KEYCODE_1 == keyCode)
            detectStatusHandler.sendEmptyMessage(DETECT_CONTINUE);
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
