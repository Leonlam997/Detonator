package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Dialog.SemiProductDialog;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SemiProductActivity extends BaseActivity {
    private final int REFRESH_VOLTAGE = 1,
            REFRESH_CURRENT = 2,
            REFRESH_CODE = 3,
            START_SCAN = 4,
            CMD_TIMEOUT = 5,
            GET_RESULT = 6,
            SCAN_TIMEOUT = 7,
            RESTORE_VOLTAGE = 8;
    private final int FAIL_NO_RESPOND = 199,
            FAIL_LARGE_CURRENT = 198,
            FAIL_SMALL_CURRENT = 197,
            FAIL_DETECT_LARGE_CURRENT = 196,
            FAIL_WRITE_ERROR = 195,
            FAIL_CAPACITY_ERROR = 194,
            FAIL_BRIDGE_BROKE = 193,
            FAIL_DATA_ERROR = 192,
            FAIL_BRIDGE_SHORT = 191,
            FAIL_CAPACITY_FULL = 190,
            FAIL_TIME_OUT = 189,
            FAIL_LOW_VOLTAGE = 188;
    private final Map<Integer, String> failCode = new HashMap<Integer, String>() {
        {
            put(FAIL_NO_RESPOND, "检测不到！");
            put(FAIL_LARGE_CURRENT, "大电流！");
            put(FAIL_SMALL_CURRENT, "小电流！");
            put(FAIL_DETECT_LARGE_CURRENT, "大电流！");
            put(FAIL_WRITE_ERROR, "写码错误！");
            put(FAIL_CAPACITY_ERROR, "电容损坏！");
            put(FAIL_BRIDGE_BROKE, "桥丝断开！");
            put(FAIL_DATA_ERROR, "数据错误！");
            put(FAIL_BRIDGE_SHORT, "桥丝短路！");
            put(FAIL_CAPACITY_FULL, "电容有电！");
            put(FAIL_TIME_OUT, "命令超时！");
            put(FAIL_LOW_VOLTAGE, "输出低压！");
        }
    };
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private int defaultVoltage = newLG ? 80 : 2790;
    private SerialPortUtil serialPortUtil;
    private int detonatorType, flowStep, readCurrentCount, writeSNCount, ldoVoltage, boostVoltage = 1950, chargeTime = newLG ? 1000 : 200, soundSuccess, soundFail, soundAlert;
    private SerialDataReceiveListener serialListener;
    private boolean autoDetect = false, scanCode = false, finishWriting, autoStart = false;
    private TextView textViewCurrent, textViewVoltage, textViewCode;
    private SoundPool soundPool;
    private long timeCounter;
    private float lastCurrent;
    private BaseApplication myApp;

    private void changeMode(boolean auto, boolean scan) {
        readCurrentCount = 0;
        autoDetect = auto;
        scanCode = scan;
        serialListener.setStartAutoDetect(auto);
        serialListener.setFeedback(auto);
        serialListener.setScanMode(scan);
    }

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case REFRESH_VOLTAGE:
                    textViewVoltage.setText((String) message.obj);
                    break;
                case REFRESH_CURRENT:
                    textViewCurrent.setText((String) message.obj);
                    break;
                case REFRESH_CODE:
                    if (null != message.obj && ((String) message.obj).length() == 13) {
                        textViewCode.setText(((String) message.obj));
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                    } else {
                        textViewCode.setText("");
                    }
                    break;
                case START_SCAN:
                    startScan();
                    break;
                case CMD_TIMEOUT:
                    if (flowStep > (newLG ? 12 : 11))
                        finishedDetect(FAIL_BRIDGE_BROKE);
                    else if (flowStep == (newLG ? 3 : 4) && !finishWriting && writeSNCount > 1)
                        finishedDetect(FAIL_WRITE_ERROR);
                    else
                        startFlow();
                    break;
                case SCAN_TIMEOUT:
                    changeMode(true, false);
                    enabledButton(true);
                    myApp.myToast(SemiProductActivity.this, R.string.message_scan_timeout);
                    break;
                case RESTORE_VOLTAGE:
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                    //changeMode(true, false);
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.BOOST_TIME);
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
        try {
            initSound();
            serialPortUtil = SerialPortUtil.getInstance();
            serialListener = new SerialDataReceiveListener(this, () -> {
                String data = serialListener.getRcvData();
                if (data.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                    if (serialPortUtil != null) {
                        serialPortUtil.closeSerialPort();
                        serialPortUtil = null;
                    }
                    runOnUiThread(() -> new AlertDialog.Builder(SemiProductActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_short_circuit)
                            .setCancelable(false)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .create().show());
                    myApp.playSoundVibrate(soundPool, soundAlert);
                } else if (data.contains(SerialCommand.INITIAL_FAIL)) {
                    myApp.myToast(SemiProductActivity.this, R.string.message_open_module_fail);
                    finish();
                } else if (data.contains(SerialCommand.INITIAL_FINISHED)) {
                    serialListener.setRcvData("");
                } else if (data.equals(SerialCommand.RESPOND_CONNECTED)) {
                    if (flowStep == 0) {
                        myHandler.removeMessages(START_SCAN);
                        myHandler.removeMessages(CMD_TIMEOUT);
                        enabledButton(false);
                        flowStep = 1;
                        startFlow();
                        autoStart = true;
                    }
                } else if (autoDetect) {
                    Message m = myHandler.obtainMessage();
                    data = data.replace("Fail!", "");
                    if (data.length() > 1 && data.startsWith("V")) {
                        m.what = REFRESH_VOLTAGE;
                        try {
                            float v = Float.parseFloat(data.substring(1));
                            if (!newLG) {
                                v /= 100f;
                                if (v >= 0.13f)
                                    v -= 0.13f;
                            }
                            m.obj = String.format(Locale.CHINA, "%.2f", v);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (data.length() > 1 && data.startsWith("A")) {
                        //myApp.myToast(SemiProductActivity.this, data + "," + readCurrentCount);
                        m.what = REFRESH_CURRENT;
                        float current = 0;
                        try {
                            current = Float.parseFloat(data.substring(1));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        m.obj = String.format(Locale.CHINA, "%.2f", current);
                        switch (flowStep) {
                            case 0:
                                if (current > 50)
                                    readCurrentCount++;
                                else
                                    readCurrentCount = 0;
                                if (readCurrentCount > 3) {
                                    finishedDetect(FAIL_DETECT_LARGE_CURRENT);
                                }
                                break;
                            case 1:
                                if (current > 50) {
                                    if (readCurrentCount >= 10) {
                                        finishedDetect(FAIL_LARGE_CURRENT);
                                    } else
                                        readCurrentCount++;
                                } else if (current < 20) {
                                    if (readCurrentCount >= 10) {
                                        finishedDetect(current <= 0 ? FAIL_NO_RESPOND : FAIL_SMALL_CURRENT);
                                    } else
                                        readCurrentCount++;
                                } else {
                                    lastCurrent = current;
                                    flowStep++;
                                    if (autoStart) {
                                        changeMode(false, false);
                                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 2500);
                                    } else
                                        startFlow();
                                }
                                break;
                            case 5:
                                flowStep++;
                                startFlow();
//                                if (current > 200) {
//                                    //myApp.myToast(SemiProductActivity.this, "电流：" + current);
//                                    finishedDetect(FAIL_CAPACITY_ERROR);
//                                } else {
//                                    flowStep++;
//                                    startFlow();
//                                }
                                break;
                            case 6:
                                if (newLG)
                                    lastCurrent = current;
                                break;
                        }
                    }
                    myHandler.sendMessage(m);
                } else if (scanCode) {
                    if ((newLG && data.contains(SerialCommand.SCAN_RESPOND) && data.length() > 10) || (!newLG && data.contains(SerialCommand.RESPOND_SUCCESS) && !data.equals(SerialCommand.RESPOND_SUCCESS))) {
                        analyzeCode(data);
                    }
                } else if (newLG) {
                    serialListener.setRcvData("");
                    if (data.contains(SerialCommand.AT_CMD_RESPOND)) {
                        if ((flowStep > 0 && flowStep < 3) || (flowStep > 5 && flowStep < 11)) {
                            flowStep++;
                            startFlow();
                        }
                    } else if (data.startsWith(SerialCommand.DATA_PREFIX) && data.length() > 6) {
                        if (data.substring(4).startsWith(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.WRITE_SN)))) {
                            flowStep++;
                            finishWriting = true;
                            startFlow();
                        } else if (data.substring(4).startsWith(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_TEST_TIMES)))) {
                            try {
                                if (serialPortUtil.checkData(data)) {
                                    finishedDetect(Integer.parseInt(data.substring(6, 8), 16));
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        }
                    }
                } else {
                    String confirm;
                    switch (flowStep) {
                        case 4:
                            confirm = SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.WRITE_SN);
                            if (null != confirm && data.contains(confirm)) {
                                finishWriting = true;
                                flowStep++;
                                startFlow();
                            }
                            break;
                        case 11:
                        case 12:
                            confirm = SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_TEST_TIMES);
                            if (null != confirm && data.contains(confirm)) {
                                try {
                                    finishedDetect(Integer.parseInt(data.substring(data.indexOf(confirm) + confirm.length()), 16));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                    }
                }
            });
            serialListener.setSemiDetect(true);
            serialPortUtil.setOnDataReceiveListener(serialListener);
            flowStep = -1;

            textViewCurrent = findViewById(R.id.tv_current);
            textViewVoltage = findViewById(R.id.tv_voltage);
            textViewCode = findViewById(R.id.tv_code);
            List<String> listType = new ArrayList<>();
            listType.add("钢带桥丝");
            listType.add("贴片桥丝");
            LocalSettingBean bean = BaseApplication.readSettings();
            detonatorType = bean.getDefaultType();

            ArrayAdapter<String> adapterType = new ArrayAdapter<>(this, R.layout.layout_spinner_item, listType);
            adapterType.setDropDownViewResource(R.layout.layout_spinner_item);
            ((Spinner) findViewById(R.id.spinner)).setAdapter(adapterType);
            ((Spinner) findViewById(R.id.spinner)).setSelection(detonatorType, true);
            ((Spinner) findViewById(R.id.spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    detonatorType = i;
                    LocalSettingBean bean = new LocalSettingBean();
                    bean.setDefaultType(i);
                    myApp.saveSettings(bean);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            findViewById(R.id.btn_scan).setOnClickListener(view -> startScan());

            findViewById(R.id.btn_test).setOnClickListener(view -> doTest());
        } catch (
                Exception e) {
            BaseApplication.writeErrorLog(e);
        }

    }

    private void analyzeCode(String data) {
        serialListener.setRcvData("");
        if (!newLG)
            data = data.substring(0, data.indexOf(SerialCommand.RESPOND_SUCCESS));
        else {
            data = data.split("\\+")[1];
            data = data.substring(data.indexOf(SerialCommand.SCAN_RESPOND) + SerialCommand.SCAN_RESPOND.length());
        }
        data = data.replace("\1", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "");

        Message m = myHandler.obtainMessage(REFRESH_CODE);
        try {
            if (data.length() == 13 && Integer.parseInt(data.substring(0, 2)) > 0) {
                m.obj = data;
                myHandler.removeCallbacksAndMessages(null);
                if (!newLG)
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                changeMode(true, false);
                flowStep = 0;
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        myHandler.sendMessage(m);
        enabledButton(true);
    }

    private void doTest() {
        if (!textViewCode.getText().toString().isEmpty()) {
            textViewCode.setText(textViewCode.getText().toString().toUpperCase());
            myHandler.removeCallbacksAndMessages(null);
            enabledButton(false);
            flowStep = 1;
            startFlow();
        } else {
            SemiProductDialog dialog = new SemiProductDialog(SemiProductActivity.this);
            dialog.setTitleId(R.string.dialog_scan_first);
            dialog.show();
        }
    }

    private void finishedDetect(int times) {
        final int TYPE1_TIMES_RANGE_UP = 6,
                TYPE1_TIMES_RANGE_DOWN = 4,
                TYPE2_TIMES_RANGE_UP = 8,
                TYPE2_TIMES_RANGE_DOWN = 3;
        myHandler.removeMessages(CMD_TIMEOUT);
        flowStep = -1;
        autoStart = false;
        SemiProductDialog myDialog = new SemiProductDialog(SemiProductActivity.this);
        if (!newLG) {
            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
        }
        if (times < 100)
            myApp.myToast(SemiProductActivity.this, "自检" + times + "次(" + (System.currentTimeMillis() - timeCounter) + "ms," + lastCurrent + "μA)");
        if (((detonatorType == 0 || detonatorType == 2) && times >= TYPE1_TIMES_RANGE_DOWN && times <= TYPE1_TIMES_RANGE_UP)
                || ((detonatorType == 1 || detonatorType == 3) && times >= TYPE2_TIMES_RANGE_DOWN && times <= TYPE2_TIMES_RANGE_UP)) {
            myDialog.setStyle(3);
            myDialog.setTitleId(R.string.dialog_qualified);
            myDialog.setCode("");
            Message m = myHandler.obtainMessage(REFRESH_CODE);
            String id = textViewCode.getText().toString();
            m.obj = id.substring(0, 8) + String.format(Locale.CHINESE, "%05d", Integer.parseInt(id.substring(8)) + 1);
            myHandler.sendMessage(m);
        } else {
            myDialog.setAutoClose(false);
            myDialog.setStyle(2);
            myApp.playSoundVibrate(soundPool, soundFail);
            myDialog.setCanceledOnTouchOutside(false);
            myDialog.setTitleId(R.string.dialog_unqualified);
            myDialog.setSubtitleId(R.string.dialog_unqualified_hint);
            if (null != failCode.get(times))
                myDialog.setCode(failCode.get(times));
            else
                myDialog.setCode("自检" + times + "次");
//            else
//                myDialog.setCode("其他错误！");
        }
        changeMode(true, false);
        myDialog.show();
        enabledButton(true);
    }

    private void startScan() {
        myHandler.removeCallbacksAndMessages(null);
        changeMode(false, true);
        enabledButton(false);
        serialListener.setRcvData("");
        serialPortUtil.sendCmd(SerialCommand.CMD_SCAN);
        myHandler.sendEmptyMessageDelayed(START_SCAN, ConstantUtils.SCAN_TIMEOUT);
    }

    private void enabledButton(boolean enabled) {
        setProgressVisibility(!enabled);
        findViewById(R.id.btn_scan).setEnabled(enabled);
        findViewById(R.id.btn_test).setEnabled(enabled);
    }

    private void startFlow() {
        if (!textViewCode.getText().toString().isEmpty()) {
            myHandler.removeCallbacksAndMessages(null);
            changeMode(false, false);

            serialListener.setRcvData("");
            if (newLG) {
                if (flowStep < 10)
                    myApp.myToast(SemiProductActivity.this, "第" + flowStep + "步");
                switch (flowStep) {
                    case 1:
                        timeCounter = System.currentTimeMillis();
                    case 9:
                        serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_INITIAL_VOLTAGE, defaultVoltage, defaultVoltage));
                        break;
                    case 2:
                        writeSNCount = 0;
                    case 6:
                    case 10:
                        serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 1));
                        break;
                    case 3:
                        writeSNCount++;
                        serialPortUtil.sendCmd(textViewCode.getText().toString(), SerialCommand.ACTION_TYPE.WRITE_SN, 0);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case 4:
                        flowStep++;
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 1, defaultVoltage);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case 5:
                        flowStep++;
                        changeMode(true, false);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 3000);
                        break;
                    case 7:
                        flowStep++;
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.SELF_TEST, 0);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, chargeTime);
                        break;
                    case 8:
                        serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                        break;
                    case 11:
                    case 12:
                        flowStep++;
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_TEST_TIMES, 0);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                }
            } else {
                myApp.myToast(SemiProductActivity.this, "第" + flowStep + "步");
                switch (flowStep) {
                    case 1:
                        timeCounter = System.currentTimeMillis();
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + boostVoltage + "###");
                        myHandler.sendEmptyMessageDelayed(RESTORE_VOLTAGE, ConstantUtils.BOOST_TIME);
                        break;
                    case 2:
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.INSTANT_OPEN_CAPACITOR, 0);
                        finishWriting = false;
                        flowStep++;
                        writeSNCount = 0;
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 500);
                        break;
                    case 3:
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.INSTANT_OPEN_CAPACITOR, 0);
                        flowStep++;
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 3500);
                        break;
                    case 4:
                        writeSNCount++;
                        serialPortUtil.sendCmd(textViewCode.getText().toString(), SerialCommand.ACTION_TYPE.WRITE_SN, 0);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case 5:
                        changeMode(true, false);
                        break;
                    case 6:
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.SELF_TEST, 0);
                        flowStep++;
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, chargeTime);
                        break;
                    case 7:
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                        flowStep++;
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 2000);
                        break;
                    case 8:
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + boostVoltage + "###");
                        flowStep++;
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 500);
                        break;
                    case 9:
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                        flowStep++;
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 500);
                        break;
                    case 10:
                    case 11:
                        flowStep++;
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_TEST_TIMES, 0);
                        myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                }
            }
        } else {
            SemiProductDialog dialog = new SemiProductDialog(SemiProductActivity.this);
            dialog.setTitleId(R.string.dialog_scan_first);
            dialog.show();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                doTest();
                break;
            case KeyEvent.KEYCODE_2:
                if (findViewById(R.id.btn_scan).isEnabled()) {
                    startScan();
                }
                break;
            case KeyEvent.KEYCODE_4:
                final View view = LayoutInflater.from(SemiProductActivity.this).inflate(R.layout.layout_edit_dialog, null);
                final EditText etDelay = view.findViewById(R.id.et_dialog);
                view.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                etDelay.setHint(R.string.hint_input_voltage);
                etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                etDelay.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                myHandler.removeCallbacksAndMessages(null);
                new AlertDialog.Builder(SemiProductActivity.this, R.style.AlertDialog)
                        .setTitle("修改启动电压")
                        .setView(view)
                        .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                            try {
                                boostVoltage = Integer.parseInt(etDelay.getText().toString());
                                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + boostVoltage + "###");
                                changeMode(true, false);
                                myApp.myToast(SemiProductActivity.this, "修改成功！");
                                return;
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            myApp.myToast(SemiProductActivity.this, "输入错误！");
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

                break;
            case KeyEvent.KEYCODE_5:
                final View view1 = LayoutInflater.from(SemiProductActivity.this).inflate(R.layout.layout_edit_dialog, null);
                final EditText etDelay1 = view1.findViewById(R.id.et_dialog);
                view1.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                myHandler.removeCallbacksAndMessages(null);
                etDelay1.setHint(R.string.hint_input_voltage);
                etDelay1.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                etDelay1.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                new AlertDialog.Builder(SemiProductActivity.this, R.style.AlertDialog)
                        .setTitle("修改检测电压")
                        .setView(view1)
                        .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                            try {
                                defaultVoltage = Integer.parseInt(etDelay1.getText().toString());
                                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                                changeMode(true, false);
                                myApp.myToast(SemiProductActivity.this, "修改成功！");
                                return;
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            myApp.myToast(SemiProductActivity.this, "输入错误！");
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

                break;
            case KeyEvent.KEYCODE_6:
                final View view2 = LayoutInflater.from(SemiProductActivity.this).inflate(R.layout.layout_edit_dialog, null);
                final EditText etDelay2 = view2.findViewById(R.id.et_dialog);
                view2.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                etDelay2.setHint(R.string.hint_input_voltage);
                myHandler.removeCallbacksAndMessages(null);
                etDelay2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                etDelay2.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                new AlertDialog.Builder(SemiProductActivity.this, R.style.AlertDialog)
                        .setTitle("修改充电时间")
                        .setView(view2)
                        .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                            try {
                                chargeTime = Integer.parseInt(etDelay2.getText().toString());
                                myApp.myToast(SemiProductActivity.this, "修改成功！");
                                return;
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            myApp.myToast(SemiProductActivity.this, "输入错误！");
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
                break;
            case KeyEvent.KEYCODE_POUND:
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
                            if (code.length() == 13) {
                                Message m = myHandler.obtainMessage(REFRESH_CODE);
                                m.obj = code.getText().toString();
                                myHandler.sendMessage(m);
                                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                            } else {
                                myApp.myToast(SemiProductActivity.this, R.string.message_detonator_input_error);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create().show();
                break;
            case KeyEvent.KEYCODE_R:
                if (!textViewCode.getText().toString().isEmpty()) {
                    try {
                        int i = Integer.parseInt(textViewCode.getText().toString().substring(8)) + 1;
                        textViewCode.setText(String.format(Locale.CHINA, "%s%05d", textViewCode.getText().toString().substring(0, 8), i));
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                }
                break;
            case KeyEvent.KEYCODE_B:
                if (!textViewCode.getText().toString().isEmpty()) {
                    try {
                        int i = Integer.parseInt(textViewCode.getText().toString().substring(8)) - 1;
                        textViewCode.setText(String.format(Locale.CHINA, "%s%05d", textViewCode.getText().toString().substring(0, 8), i));
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                }
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
        if (null != serialListener) {
            serialListener.closeAllHandler();
            serialListener = null;
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
