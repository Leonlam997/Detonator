package com.leon.detonator.Activity;

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
    private int defaultVoltage = 2790;
    private SerialPortUtil serial;
    private int detonatorType, flowStep, readCurrentCount, writeSNCount, ldoVoltage, boostVoltage = 1950, chargeTime = 200, soundSuccess, soundFail, soundAlert;
    private SerialDataReceiveListener serialListener;
    private float resistance;
    private boolean autoDetect = false, scanCode = false, finishWriting, autoStart = false, startWaitingScan;
    private TextView textViewCurrent, textViewVoltage, textViewCode;
    private SoundPool soundPool;
    private boolean fastTest = false;
    private long timeCounter;
    private float lastCurrent;
    private BaseApplication myApp;
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
                    if (newLG) {
                        finishedDetect(FAIL_TIME_OUT);
                    } else if (flowStep > 11)
                        finishedDetect(FAIL_BRIDGE_BROKE);
                    else if (flowStep == 4 && !finishWriting && writeSNCount > 1)
                        finishedDetect(FAIL_WRITE_ERROR);
                    else
                        startFlow();
                    break;
//                case FINISHED_CHARGING://Finish charging
//                    if (finishWriting) {
//                        flowStep++;
//                        startFlow();
//                    } else if (flowStep == 2)
//                        myHandler.sendEmptyMessageDelayed(FINISHED_CHARGING, 500);
//                    break;
                case GET_RESULT:
                    serial.sendCmd("", SerialCommand.ACTION_TYPE.TEST_RESULT, 0);
                    myHandler.sendEmptyMessageDelayed(GET_RESULT, 100);
                    break;
                case SCAN_TIMEOUT:
                    changeMode(true, false);
                    myApp.myToast(SemiProductActivity.this, "扫描超时！");
                    break;
                case RESTORE_VOLTAGE:
                    serial.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                    //changeMode(true, false);
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.BOOST_TIME);
                    break;
            }
            return false;
        }
    });

    private void changeMode(boolean auto, boolean scan) {
        readCurrentCount = 0;
        autoDetect = auto;
        scanCode = scan;
        serialListener.setStartAutoDetect(auto);
        serialListener.setFeedback(auto);
        serialListener.setScanMode(scan);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semi_product);
        setTitle(R.string.semi_product_title);

        myApp = (BaseApplication) getApplication();
        try {
            initSound();
            serial = SerialPortUtil.getInstance();
            serialListener = new SerialDataReceiveListener(this, () -> {
                String data = serialListener.getRcvData();
                if (data.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                    if (serial != null) {
                        serial.closeSerialPort();
                        serial = null;
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
                    if (newLG)
                        serial.sendCmd("", SerialCommand.ACTION_TYPE.SET_VOLTAGE, 60);
                } else if (data.equals(SerialCommand.RESPOND_CONNECTED)) {
                    if (flowStep == 0) {
                        myHandler.removeMessages(START_SCAN);
                        myHandler.removeMessages(CMD_TIMEOUT);
                        setProgressVisibility(true);
                        findViewById(R.id.btn_scan).setEnabled(false);
                        findViewById(R.id.btn_test).setEnabled(false);
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
                            float v = Integer.parseInt(data.substring(1)) / 100.0f;
                            if (v >= 0.13f)
                                v -= 0.13f;
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
                        }
                    }
                    myHandler.sendMessage(m);
                } else if (scanCode) {
                    if (newLG) {
                        if (!startWaitingScan && data.contains(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.SCAN_CODE)))) {
                            myHandler.removeMessages(START_SCAN);
                            startWaitingScan = true;
                            myHandler.sendEmptyMessageDelayed(SCAN_TIMEOUT, ConstantUtils.SCAN_TIMEOUT);
                            serialListener.setRcvData("");
                        } else if (startWaitingScan) {
                            byte[] t = new byte[data.length() / 2];
                            for (int i = 0; i < data.length(); i += 2) {
                                t[i / 2] = (byte) Integer.parseInt(data.substring(i, i + 2), 16);
                            }
                            analyzeCode(new String(t));

                        }
                    } else if (data.contains(SerialCommand.RESPOND_SUCCESS) && !data.equals(SerialCommand.RESPOND_SUCCESS)) {
                        analyzeCode(data);
                    }
                } else if (newLG) {
                    serialListener.setRcvData("");
                    if (data.contains(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.SET_VOLTAGE)))) {
                        changeMode(true, false);
                    } else if (data.contains(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.WRITE_SN)))) {
                        myHandler.removeMessages(GET_RESULT);
                        serial.sendCmd("", fastTest ? SerialCommand.ACTION_TYPE.FAST_TEST : SerialCommand.ACTION_TYPE.SELF_TEST, 0);
                        myHandler.sendEmptyMessageDelayed(fastTest ? GET_RESULT : CMD_TIMEOUT, fastTest ? 2500 : ConstantUtils.SELF_TEST_TIME);
                    } else if (data.contains(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.RELEASE_CAPACITOR)))) {
                        changeMode(true, false);
                        myApp.myToast(SemiProductActivity.this, "放电成功！");
                    } else if (data.contains(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.FAST_TEST)))) {
                        myHandler.removeMessages(GET_RESULT);
                        try {
                            data = data.substring(data.indexOf(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.FAST_TEST))) + 2, data.indexOf(SerialCommand.STRING_DATA_END));
                            int cs = 0, j = 0;
                            int[] adc = new int[3];
                            for (int i = 0; i < data.length() - 2; i += 2) {
                                byte a = (byte) Integer.parseInt(data.substring(i, i + 2), 16);
                                cs += a;
                                if (i > 4 && j < adc.length) {
                                    a ^= SerialCommand.XOR_DATA;
                                    if (0 == (i - 4) % 4) {
                                        adc[j] = a;
                                    } else {
                                        adc[j++] += a * 0x100;
                                        Log.d("ZBEST", j + ":" + adc[j - 1]);
                                    }
                                }
                            }
                            if (String.format("%02X", cs).endsWith(data.substring(data.length() - 2))) {
                                if (data.charAt(3) == '6') {
                                    float c = (float) (adc[1] + adc[0]) / ((0 != adc[1] - adc[0]) ? (adc[1] - adc[0]) : 1) * 25 / 13f;
                                    Log.d("ZBEST", "C=" + c);
                                    resistance = (float) (adc[1] + adc[2]) / ((0 != adc[1] - adc[2]) ? (adc[1] - adc[2]) : 1) * 17 / 200f;
                                    if (resistance >= 0 && resistance < 0.8)
                                        finishedDetect(FAIL_BRIDGE_SHORT);
                                    else if (resistance > 6 || resistance < 0)
                                        finishedDetect(FAIL_BRIDGE_BROKE);
                                    else
                                        finishedDetect(3);
                                } else
                                    finishedDetect(FAIL_CAPACITY_FULL);
                            } else
                                finishedDetect(FAIL_DATA_ERROR);
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                    } else if (data.contains(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.TEST_RESULT)))) {
                        myHandler.removeMessages(GET_RESULT);
                        data = data.substring(data.indexOf(Objects.requireNonNull(SerialCommand.NEW_RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.TEST_RESULT))) + 2);
                        try {
                            int cs = 0, j = 0;
                            int[] adc = new int[12];
                            for (int i = 0; i < data.length() - 2; i += 2) {
                                final int i1 = Integer.parseInt(data.substring(i, i + 2), 16);
                                cs += i1;
                                if (i > 4 && j < adc.length) {
                                    if (0 == (i - 4) % 4) {
                                        adc[j] = ((byte) i1 & 0xFF);
                                    } else {
                                        adc[j++] += ((byte) i1 & 0xFF) * 0x100;
                                        Log.d("ZBEST", j + ":" + adc[j - 1]);
                                    }
                                }
                            }
                            if (String.format("%02X", cs).endsWith(data.substring(data.length() - 2))) {
//                                float fx = adc[0];
//                                if (fx == 0)
//                                    fx = 0.000001f;
//                                fx = 2.45f / fx;
                                resistance = (float) (adc[9] + adc[10]) / ((0 != adc[9] - adc[10]) ? (adc[9] - adc[10]) : 1) * 17 / 200f;
                                ldoVoltage = adc[0];
                                if (adc[0] > 3237)
                                    finishedDetect(FAIL_LOW_VOLTAGE);
                                else if (adc[5] - adc[4] < 30)
                                    finishedDetect(FAIL_CAPACITY_ERROR);
                                else if (resistance >= 0 && resistance < 0.8)
                                    finishedDetect(FAIL_BRIDGE_SHORT);
                                else if (resistance > 6 || resistance < 0)
                                    finishedDetect(FAIL_BRIDGE_BROKE);
                                else
                                    finishedDetect(3);
                            } else
                                finishedDetect(FAIL_DATA_ERROR);
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                            finishedDetect(FAIL_DATA_ERROR);
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
            serial.setOnDataReceiveListener(serialListener);
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
        data = data.replace("\1", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "");

        Message m = myHandler.obtainMessage(REFRESH_CODE);
        try {
            if (data.length() == 13 && Integer.parseInt(data.substring(0, 2)) > 0) {
                m.obj = data;
                myHandler.removeCallbacksAndMessages(null);
                serial.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                changeMode(true, false);
                flowStep = 0;
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        myHandler.sendMessage(m);
    }

    private void doTest() {
        if (!textViewCode.getText().toString().isEmpty()) {
            textViewCode.setText(textViewCode.getText().toString().toUpperCase());
            myHandler.removeCallbacksAndMessages(null);
            setProgressVisibility(true);
            if (newLG) {
                timeCounter = System.currentTimeMillis();
                findViewById(R.id.btn_scan).setEnabled(false);
                findViewById(R.id.btn_test).setEnabled(false);
                changeMode(false, false);
                serialListener.setRcvData("");
//                        serial.sendCmd("", SerialCommand.ACTION_TYPE.FAST_TEST, 0);
                serial.sendCmd(textViewCode.getText().toString(), SerialCommand.ACTION_TYPE.WRITE_SN, 0);
                myHandler.sendEmptyMessageDelayed(GET_RESULT, 500);
            } else {
                flowStep = 1;
                startFlow();
            }
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
        if (newLG && FAIL_TIME_OUT != times) {
            myApp.myToast(SemiProductActivity.this,
                    String.format(Locale.CHINA, "桥丝电阻：%.2f欧\nLDO:%.2fV(%dms)", resistance, 4096f / ldoVoltage * 2.45f, System.currentTimeMillis() - timeCounter));
        }
        if (!newLG) {
            serial.sendCmd(SerialCommand.CMD_BOOST + "9999###");
        }
        if (((detonatorType == 0 || detonatorType == 2) && times >= TYPE1_TIMES_RANGE_DOWN && times <= TYPE1_TIMES_RANGE_UP)
                || ((detonatorType == 1 || detonatorType == 3) && times >= TYPE2_TIMES_RANGE_DOWN && times <= TYPE2_TIMES_RANGE_UP)) {
            if (!newLG) {
                myApp.myToast(SemiProductActivity.this, "自检" + times + "次(" + (System.currentTimeMillis() - timeCounter) + "ms," + lastCurrent + "μA)");
            }
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
        setProgressVisibility(false);
        findViewById(R.id.btn_scan).setEnabled(true);
        findViewById(R.id.btn_test).setEnabled(true);
    }

    private void startScan() {
        myHandler.removeCallbacksAndMessages(null);
        changeMode(false, true);
        serialListener.setRcvData("");
        if (newLG) {
            serial.sendCmd("", SerialCommand.ACTION_TYPE.SCAN_CODE, 0);
            startWaitingScan = false;
            myHandler.sendEmptyMessageDelayed(START_SCAN, 300);
        } else {
            serial.sendCmd(SerialCommand.CMD_SCAN);
            myHandler.sendEmptyMessageDelayed(START_SCAN, 2000);
        }
    }

    private void startFlow() {
        if (!textViewCode.getText().toString().isEmpty()) {
            findViewById(R.id.btn_scan).setEnabled(false);
            findViewById(R.id.btn_test).setEnabled(false);
            myApp.myToast(SemiProductActivity.this, "第" + flowStep + "步");
            myHandler.removeCallbacksAndMessages(null);

            changeMode(false, false);
            serialListener.setRcvData("");
            switch (flowStep) {
                case 1:
                    timeCounter = System.currentTimeMillis();
                    serial.sendCmd(SerialCommand.CMD_BOOST + boostVoltage + "###");
                    myHandler.sendEmptyMessageDelayed(RESTORE_VOLTAGE, ConstantUtils.BOOST_TIME);
                    break;
                case 2:
                    serial.sendCmd("", SerialCommand.ACTION_TYPE.INSTANT_OPEN_CAPACITOR, 0);
                    finishWriting = false;
                    flowStep++;
                    writeSNCount = 0;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 500);
                    break;
                case 3:
                    serial.sendCmd("", SerialCommand.ACTION_TYPE.INSTANT_OPEN_CAPACITOR, 0);
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 3500);
                    break;
                case 4:
                    writeSNCount++;
                    serial.sendCmd(textViewCode.getText().toString(), SerialCommand.ACTION_TYPE.WRITE_SN, 0);
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
                case 5:
                    changeMode(true, false);
                    break;
                case 6:
                    serial.sendCmd("", SerialCommand.ACTION_TYPE.SELF_TEST, 0);
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, chargeTime);
                    break;
                case 7:
                    serial.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 2000);
                    break;
                case 8:
                    serial.sendCmd(SerialCommand.CMD_BOOST + boostVoltage + "###");
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 500);
                    break;
                case 9:
                    serial.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                    flowStep++;
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, 500);
                    break;
                case 10:
                case 11:
                    flowStep++;
                    serial.sendCmd("", SerialCommand.ACTION_TYPE.READ_TEST_TIMES, 0);
                    myHandler.sendEmptyMessageDelayed(CMD_TIMEOUT, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
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
            case KeyEvent.KEYCODE_3:
                if (findViewById(R.id.btn_test).isEnabled()) {
                    if (!textViewCode.getText().toString().isEmpty()) {
                        textViewCode.setText(textViewCode.getText().toString().toUpperCase());
                        changeMode(false, false);
                        serial.sendCmd(textViewCode.getText().toString(), SerialCommand.ACTION_TYPE.RELEASE_CAPACITOR, 0);
                    }
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
                                serial.sendCmd(SerialCommand.CMD_BOOST + boostVoltage + "###");
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
                                serial.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
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
                                serial.sendCmd(SerialCommand.CMD_BOOST + defaultVoltage + "###");
                            } else {
                                myApp.myToast(SemiProductActivity.this, R.string.message_detonator_input_error);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create().show();
                break;
            case KeyEvent.KEYCODE_STAR:
                fastTest = !fastTest;
                myApp.myToast(SemiProductActivity.this, fastTest ? "切换到快速检测模式！" : "切换到全面检测模式！");
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
        if (null != serial) {
            serial.closeSerialPort();
            serial = null;
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
