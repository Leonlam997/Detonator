package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.R;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class CheckLineActivity extends BaseActivity {
    private final int DETECT_SUCCESS = 1;
    private final int DETECT_CONTINUE = 2;
    private final int DETECT_FAIL = 3;
    private final int DETECT_INITIAL = 4;
    private final int DETECT_NEXT_STEP = 5;
    private final int DETECT_ADDRESS = 6;
    private final int DETECT_DELAY = 7;
    private final int DETECT_SHORT = 8;
    private final int DETECT_SEND_COMMAND = 9;
    private final int STEP_CHECK_CONFIG = 1;
    private final int STEP_CLEAR_STATUS = 2;
    private final int STEP_SCAN = 3;
    private final int STEP_READ_SHELL = 4;
    private final int STEP_READ_FIELD = 5;
    private final int STEP_SCAN_CODE = 6;
    private final int STEP_END = 7;
    private BaseApplication myApp;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private SoundPool soundPool;
    private MyButton btnDetect;
    private MyButton btnStatus;
    private TextView tvTube;
    private TextView tvDelayTime;
    private TextView tvLock;
    private String tempAddress;
    private int delayTime;
    private int soundSuccess;
    private int soundFail;
    private int soundAlert;
    private int flowStep;

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_INITIAL:
                    enableButton(true);
                    break;
                case DETECT_SUCCESS: //检测成功
                    myHandler.removeCallbacksAndMessages(null);
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    enableButton(true);
                    break;
                case DETECT_CONTINUE: //开始检测
                    myHandler.removeCallbacksAndMessages(null);
                    enableButton(false);
                    tempAddress = "";
                    tvTube.setText("--");
                    tvDelayTime.setText(R.string.no_delay_time);
                    myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
//                    myHandler.sendEmptyMessageDelayed(DETECT_VOLTAGE, ConstantUtils.BOOT_TIME);
                    break;
                case DETECT_FAIL: //检测失败
                    myHandler.removeCallbacksAndMessages(null);
                    myApp.playSoundVibrate(soundPool, soundFail);
                    Map<Integer, Integer> failCode = new HashMap<Integer, Integer>() {
                        {
                            put(STEP_SCAN, R.string.message_detonator_not_detected);
                            put(STEP_READ_SHELL, R.string.message_detonator_read_shell_error);
                            put(STEP_READ_FIELD, R.string.message_detonator_read_field_error);
                            put(STEP_CHECK_CONFIG, R.string.message_detect_error);
                            put(STEP_CLEAR_STATUS, R.string.message_detonator_write_error);
                            put(STEP_SCAN_CODE, R.string.message_scan_timeout);
                        }
                    };
                    Integer i = failCode.get(flowStep);
                    myApp.myToast(CheckLineActivity.this, null == i ? R.string.message_detonator_not_detected : i);
                    enableButton(true);
                    tempAddress = "";
                    break;
                case DETECT_ADDRESS:
                    tvTube.setText(tempAddress);
                    break;
                case DETECT_DELAY:
                    tvDelayTime.setText(String.format(Locale.CHINA, "%dms", delayTime));
                    break;
                case DETECT_SHORT:
                    myHandler.removeMessages(DETECT_SEND_COMMAND);
                    if (myReceiveListener != null) {
                        myReceiveListener.closeAllHandler();
                        myReceiveListener = null;
                    }
                    if (serialPortUtil != null) {
                        serialPortUtil.closeSerialPort();
                        serialPortUtil = null;
                    }
                    myApp.playSoundVibrate(soundPool, soundAlert);
                    BaseApplication.customDialog(new AlertDialog.Builder(CheckLineActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_short_circuit)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .show());
                    break;
                case DETECT_NEXT_STEP:
                    switch (flowStep) {
                        case STEP_CHECK_CONFIG:
                            flowStep = STEP_CLEAR_STATUS;
                            break;
                        case STEP_CLEAR_STATUS:
                            flowStep = STEP_SCAN;
                            break;
                        case STEP_SCAN:
                            flowStep = STEP_READ_SHELL;
                            break;
                        case STEP_READ_SHELL:
                            flowStep = STEP_READ_FIELD;
                            break;
                    }
                case DETECT_SEND_COMMAND:
                    switch (flowStep) {
                        case STEP_CHECK_CONFIG:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SINGLE_READ_CONFIG, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_CLEAR_STATUS:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CLEAR_READ_STATUS, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_SCAN:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_UID, ConstantUtils.UID_LEN);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_READ_SHELL:
                            serialPortUtil.sendCmd(tempAddress, SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_READ_FIELD:
                            serialPortUtil.sendCmd(tempAddress, SerialCommand.CODE_READ_FIELD, ConstantUtils.UID_LEN, 0, 0, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_READ_FIELD_CMD_TIMEOUT);
                            break;
                        case STEP_SCAN_CODE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                            myHandler.sendEmptyMessageDelayed(DETECT_FAIL, ConstantUtils.SCAN_CODE_TIME);
                            break;
                    }
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
            byte[] received = myReceiveListener.getRcvData();
//            myApp.myToast(CheckLineActivity.this, received);
            if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                myHandler.sendEmptyMessage(DETECT_SHORT);
            } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                myHandler.sendEmptyMessage(DETECT_INITIAL);
            } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                myApp.myToast(CheckLineActivity.this, R.string.message_open_module_fail);
                finish();
            } else {
                myHandler.removeMessages(DETECT_SEND_COMMAND);
                myHandler.removeMessages(DETECT_NEXT_STEP);
                if (received.length > SerialCommand.CODE_CHAR_AT + 1 && 0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                    switch (flowStep) {
                        case STEP_SCAN:
                            if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                            DetonatorInfoBean bean = new DetonatorInfoBean(new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9)),
                                    (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 11]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 12]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 13]),//Delay
                                    (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 10]),//Number
                                    (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 14]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 15]),//Hole
                                    Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 16]), true);//Status
                            tempAddress = bean.getAddress();
                            runOnUiThread(() -> tvLock.setVisibility((bean.getInside() & SerialCommand.MASK_STATUS_LOCK) == 0 ? View.VISIBLE : View.INVISIBLE));
                            if (!Pattern.matches(ConstantUtils.UID_PATTERN, tempAddress)) {
                                myHandler.sendEmptyMessage(DETECT_FAIL);
                                return;
                            }
                            break;
                        case STEP_READ_SHELL:
                            tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                            if (!Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                                myHandler.sendEmptyMessage(DETECT_FAIL);
                                return;
                            }
                            myHandler.sendEmptyMessage(DETECT_ADDRESS);
                            break;
                        case STEP_READ_FIELD:
                            delayTime = (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 4]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 5]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 6]);
                            myHandler.sendEmptyMessage(DETECT_DELAY);
                            flowStep = STEP_END;
                            myHandler.sendEmptyMessage(DETECT_SUCCESS);
                            return;
                        case STEP_SCAN_CODE:
                            flowStep = STEP_END;
                            tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                            if (Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                                myHandler.sendEmptyMessage(DETECT_ADDRESS);
                                myHandler.sendEmptyMessage(DETECT_SUCCESS);
                            } else
                                myHandler.sendEmptyMessage(DETECT_FAIL);
                            return;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                } else {
                    myHandler.sendEmptyMessage(DETECT_FAIL);
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
        tvLock = findViewById(R.id.tv_lock);
        tvLock.setVisibility(View.INVISIBLE);
        btnDetect = findViewById(R.id.btn_detect);
        btnDetect.setOnClickListener(view -> {
            flowStep = STEP_CHECK_CONFIG;
            myHandler.sendEmptyMessage(DETECT_CONTINUE);
        });
        btnDetect.requestFocus();
        btnStatus = findViewById(R.id.btn_status);
        btnStatus.setVisibility(View.GONE);

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(CheckLineActivity.this, bufferRunnable);
            myReceiveListener.setSingleConnect(true);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            enableButton(false);
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(CheckLineActivity.this, R.string.message_open_module_fail);
            finish();
        }
        initSound();
    }

    private void enableButton(boolean enable) {
        setProgressVisibility(!enable);
        btnDetect.setEnabled(enable);
        btnStatus.setEnabled(enable);
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
        if (btnDetect.isEnabled())
            if (keyCode == KeyEvent.KEYCODE_1) {
                flowStep = STEP_CHECK_CONFIG;
                myHandler.sendEmptyMessage(DETECT_CONTINUE);
            } else if (keyCode == KeyEvent.KEYCODE_B) {
                flowStep = STEP_SCAN_CODE;
                myHandler.sendEmptyMessage(DETECT_CONTINUE);
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
