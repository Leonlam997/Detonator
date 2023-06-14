package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.dialog.SemiProductDialog;
import com.leon.detonator.R;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class SemiProductActivity extends BaseActivity {
    private final int DETECT_FINISH = 1;
    private final int DETECT_SEND_COMMAND = 2;
    private final int DETECT_NEXT_STEP = 3;
    private final int STEP_WRITE_CONFIG = 1;
    private final int STEP_WRITE_UID = 2;
    private final int STEP_WRITE_PSW = 3;
    private final int STEP_WRITE_SHELL = 4;
    private final int STEP_CHECK_CONFIG = 5;
    private final int STEP_CLEAR_STATUS = 6;
    private final int STEP_SCAN = 7;
    private final int STEP_READ_SHELL = 8;
    private final int STEP_READ_BRIDGE = 9;
    private final int STEP_READ_CAPACITY = 10;
    private final int STEP_LOCK = 11;
    private final int STEP_END = 12;
    private final int STEP_FINISHED = 13;
    private final int STEP_SCAN_CODE = 14;
    private SerialPortUtil serialPortUtil;
    private int flowStep;
    private int soundSuccess;
    private int soundFail;
    private int soundAlert;
    private TextView textViewCurrent;
    private TextView textViewVoltage;
    private TextView textViewCode;
    private CheckBox cbWrite;
    private CheckBox cbLock;
    private MyButton btnScan;
    private MyButton btnManual;
    private SoundPool soundPool;
    private String uID;
    private long timeCounter;
    private BaseApplication myApp;
    private SerialDataReceiveListener myReceiveListener;
    private SemiProductDialog myDialog = null;

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case DETECT_FINISH:
                    enabledButton(true);
                    if (flowStep != STEP_FINISHED) {
                        if (myDialog == null || !myDialog.isShowing()) {
                            myDialog = new SemiProductDialog(SemiProductActivity.this);
                            if (STEP_END == flowStep) {
                                myApp.playSoundVibrate(soundPool, soundSuccess);
                                myDialog.setStyle(3);
                                myDialog.setTitleId(R.string.dialog_qualified);
                                BaseApplication.writeFile(getString(R.string.dialog_qualified));
                                myDialog.setCode("");
                                if (!textViewCode.getText().toString().isEmpty() && cbWrite.isChecked())
                                    textViewCode.setText(String.format(Locale.CHINA, "%s%05d", textViewCode.getText().toString().substring(0, 8), Long.parseLong(textViewCode.getText().toString().substring(8)) + 1));
                            } else {
                                Map<Integer, String> failCode = new HashMap<Integer, String>() {
                                    {
                                        put(STEP_SCAN, getString(R.string.semi_error_not_detect));
                                        put(STEP_READ_SHELL, getString(R.string.semi_error_read));
                                        put(STEP_READ_CAPACITY, getString(R.string.semi_error_capacity));
                                        put(STEP_READ_BRIDGE, getString(R.string.semi_error_bridge));
                                        put(STEP_CHECK_CONFIG, getString(R.string.semi_error_version));
                                        put(STEP_CLEAR_STATUS, getString(R.string.semi_error_data));
                                        put(STEP_WRITE_CONFIG, getString(R.string.semi_error_write));
                                        put(STEP_LOCK, getString(R.string.semi_error_lock));
                                    }
                                };
                                myDialog.setAutoClose(false);
                                myDialog.setStyle(2);
                                myApp.playSoundVibrate(soundPool, soundFail);
                                myDialog.setCanceledOnTouchOutside(false);
                                myDialog.setTitleId(R.string.dialog_unqualified);
                                myDialog.setSubtitleId(R.string.dialog_unqualified_hint);
                                myDialog.setCode(failCode.get(flowStep));
                                BaseApplication.writeFile(failCode.get(flowStep));
                            }
                            myDialog.show();
                        }
                    } else
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                    flowStep = STEP_END;
                    break;
                case DETECT_NEXT_STEP:
                    switch (flowStep) {
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
                        case STEP_CHECK_CONFIG:
                            flowStep = STEP_CLEAR_STATUS;
                            break;
                        case STEP_CLEAR_STATUS:
                            flowStep = STEP_SCAN;
                            break;
                        case STEP_SCAN:
                            if (uID == null || uID.isEmpty()) {
                                myHandler.sendEmptyMessage(DETECT_FINISH);
                                return false;
                            } else
                                flowStep = STEP_READ_SHELL;
                            break;
                        case STEP_READ_SHELL:
                            flowStep = STEP_READ_BRIDGE;
                            break;
                        case STEP_READ_BRIDGE:
                            flowStep = STEP_READ_CAPACITY;
                            break;
                        case STEP_READ_CAPACITY:
                            flowStep = STEP_LOCK;
                            break;
                    }
                case DETECT_SEND_COMMAND:
                    myHandler.removeMessages(DETECT_SEND_COMMAND);
                    if (flowStep != STEP_SCAN_CODE)
                        myApp.myToast(SemiProductActivity.this, String.format(Locale.getDefault(), getString(R.string.semi_step), flowStep));
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
                            serialPortUtil.sendCmd(uID, SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_READ_BRIDGE:
                            serialPortUtil.sendCmd(uID, SerialCommand.CODE_BRIDGE_RESISTANCE, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_READ_CAPACITY:
                            serialPortUtil.sendCmd(uID, SerialCommand.CODE_CAPACITOR, ConstantUtils.CHECK_CAPACITY_LEVEL);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.CHECK_CAPACITY_TIME);
                            break;
                        case STEP_SCAN_CODE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_SCAN_TIMEOUT);
                            break;
                        case STEP_WRITE_CONFIG:
                            serialPortUtil.sendCmd(textViewCode.getText().toString(), SerialCommand.CODE_SINGLE_WRITE_CONFIG, ConstantUtils.UID_LEN, ConstantUtils.PSW_LEN, ConstantUtils.DETONATOR_VERSION);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_WRITE_UID:
                            serialPortUtil.sendCmd(textViewCode.getText().toString(), SerialCommand.CODE_WRITE_UID, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_WRITE_PSW:
                            serialPortUtil.sendCmd(ConstantUtils.EXPLODE_PSW, SerialCommand.CODE_WRITE_PSW, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_WRITE_SHELL:
                            serialPortUtil.sendCmd(textViewCode.getText().toString(), SerialCommand.CODE_WRITE_SHELL, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_LOCK:
                            serialPortUtil.sendCmd(textViewCode.getText().toString(), SerialCommand.CODE_LOCK, 0);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                    }
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semi_product);
        setTitle(R.string.semi_product_title);

        myApp = (BaseApplication) getApplication();
        BaseApplication.writeFile(getString(R.string.semi_product_title));
        cbWrite = findViewById(R.id.cb_write);
        cbLock = findViewById(R.id.cb_lock);
        try {
            initSound();
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(this, () -> {
                byte[] received = myReceiveListener.getRcvData();
                if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                    if (serialPortUtil != null) {
                        serialPortUtil.closeSerialPort();
                        serialPortUtil = null;
                    }
                    runOnUiThread(() -> new AlertDialog.Builder(SemiProductActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_short_circuit)
                            .setCancelable(false)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .show());
                    myApp.playSoundVibrate(soundPool, soundAlert);
                } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                    flowStep = STEP_FINISHED;
                    myHandler.sendEmptyMessage(DETECT_FINISH);
                } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                    myApp.myToast(SemiProductActivity.this, R.string.message_open_module_fail);
                    finish();
                } else if (received.length > SerialCommand.CODE_CHAR_AT + 1) {
                    if (received[SerialCommand.CODE_CHAR_AT] == SerialCommand.CODE_MEASURE_VALUE) {
                        runOnUiThread(() -> {
                            try {
                                textViewVoltage.setText(String.format(Locale.getDefault(), "%.2f", Float.intBitsToFloat((Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 5]) << 24)
                                        + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 4]) << 16)
                                        + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 3]) << 8)
                                        + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 2]))));
                                textViewCurrent.setText(String.format(Locale.getDefault(), "%.2f", Float.intBitsToFloat((Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]) << 24)
                                        + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 8]) << 16)
                                        + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 7]) << 8)
                                        + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 6]))));
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        });
                    } else {
                        myHandler.removeMessages(DETECT_SEND_COMMAND);
                        myHandler.removeMessages(DETECT_NEXT_STEP);
                        if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                            switch (flowStep) {
                                case STEP_SCAN:
                                    uID = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9));
                                    break;
                                case STEP_READ_SHELL:
                                    runOnUiThread(() -> {
                                        try {
                                            textViewCode.setText(new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15)));
                                        } catch (Exception e) {
                                            BaseApplication.writeErrorLog(e);
                                        }
                                    });
                                    break;
                                case STEP_READ_CAPACITY:
                                    try {
                                        final String cap = String.format(Locale.getDefault(), "电容：%.2fuF(%dms)",
                                                Float.intBitsToFloat((Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 5]) << 24)
                                                        + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 4]) << 16)
                                                        + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 3]) << 8)
                                                        + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 2])), System.currentTimeMillis() - timeCounter);
                                        myApp.myToast(SemiProductActivity.this, cap);
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                    if (cbLock.isEnabled() && cbLock.isChecked())
                                        break;
                                case STEP_LOCK:
                                    flowStep = STEP_END;
                                    myHandler.sendEmptyMessage(DETECT_FINISH);
                                    return;
                                case STEP_SCAN_CODE:
                                    flowStep = STEP_FINISHED;
                                    if (received.length < 20)
                                        myApp.myToast(SemiProductActivity.this, R.string.message_scan_timeout);
                                    else {
                                        final String add = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                                        BaseApplication.writeFile(add);
                                        textViewCode.setText(add);
                                    }
                                    myHandler.sendEmptyMessage(DETECT_FINISH);
                                    return;
                            }
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                        } else {
                            myHandler.sendEmptyMessage(DETECT_FINISH);
                        }
                    }
                }
            });
            myReceiveListener.setSemiTest(true);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);

            textViewCurrent = findViewById(R.id.tv_current);
            textViewVoltage = findViewById(R.id.tv_voltage);
            textViewCode = findViewById(R.id.tv_code);
            List<String> listType = new ArrayList<>();
            listType.add(getString(R.string.semi_bridge_type1));
            listType.add(getString(R.string.semi_bridge_type2));
            ArrayAdapter<String> adapterType = new ArrayAdapter<>(this, R.layout.layout_spinner_item, listType);
            adapterType.setDropDownViewResource(R.layout.layout_spinner_item);
            ((Spinner) findViewById(R.id.spinner)).setAdapter(adapterType);
            ((Spinner) findViewById(R.id.spinner)).setSelection(0, true);
            findViewById(R.id.spinner).setEnabled(false);
            btnScan = findViewById(R.id.btn_scan);
            btnScan.setOnClickListener(view -> doScan());
            findViewById(R.id.btn_test).setOnClickListener(view -> doTest());
            btnManual = findViewById(R.id.btn_manual);
            btnManual.setOnClickListener(view -> doManual());
            cbWrite.setOnCheckedChangeListener((buttonView, isChecked) -> {
                btnManual.setEnabled(isChecked);
                btnScan.setEnabled(isChecked);
                cbLock.setEnabled(isChecked);
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }

    }

    private void doManual() {
        if (btnManual.isEnabled()) {
            final View inputCodeView = LayoutInflater.from(SemiProductActivity.this).inflate(R.layout.layout_edit_dialog, null);
            final EditText code = inputCodeView.findViewById(R.id.et_dialog);
            code.setHint(R.string.offline_hint);
            code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(13)});
            code.setKeyListener(new NumberKeyListener() {
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
            inputCodeView.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
            new AlertDialog.Builder(SemiProductActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_manual_input)
                    .setView(inputCodeView)
                    .setPositiveButton(R.string.btn_confirm, (dialogInterface, ii) -> {
                        if (Pattern.matches(ConstantUtils.SHELL_PATTERN, code.getText())) {
                            runOnUiThread(() -> textViewCode.setText(code.getText()));
                        } else {
                            myApp.myToast(SemiProductActivity.this, R.string.message_detonator_input_error);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create().show();
        }
    }

    private void doTest() {
        if (cbWrite.isChecked() && textViewCode.getText().length() != 13) {
            myApp.myToast(SemiProductActivity.this, R.string.dialog_scan_first);
        } else if (findViewById(R.id.btn_test).isEnabled()) {
            BaseApplication.writeFile(getString(R.string.button_self_test));
            enabledButton(false);
            timeCounter = System.currentTimeMillis();
            flowStep = cbWrite.isChecked() ? STEP_WRITE_CONFIG : STEP_CLEAR_STATUS;
            myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
        }
    }

    private void doScan() {
        if (findViewById(R.id.btn_scan).isEnabled()) {
            BaseApplication.writeFile(getString(R.string.button_scan));
            myHandler.removeCallbacksAndMessages(null);
            enabledButton(false);
            flowStep = STEP_SCAN_CODE;
            myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
        }
    }

    private void enabledButton(boolean enabled) {
        setProgressVisibility(!enabled);
        myReceiveListener.setStartAutoDetect(enabled);
        findViewById(R.id.btn_test).setEnabled(enabled);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                doTest();
                break;
            case KeyEvent.KEYCODE_2:
                doScan();
                break;
            case KeyEvent.KEYCODE_3:
                doManual();
                break;
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
            soundAlert = soundPool.load(this, R.raw.alert, 1);
            if (0 == soundAlert)
                myApp.myToast(this, R.string.message_media_load_error);
        } else
            myApp.myToast(this, R.string.message_media_init_error);
    }

    @Override
    protected void onDestroy() {
        if (null != myReceiveListener) {
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
        if (null != myHandler) {
            myHandler.removeCallbacksAndMessages(null);
            myHandler = null;
        }
        if (null != serialPortUtil) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }
}
