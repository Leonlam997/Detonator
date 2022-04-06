package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.KeyUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class DetectActivity extends BaseActivity {
    private final int DETECT_SUCCESS = 1,
            DETECT_CONTINUE = 2,
            DETECT_FAIL = 3,
            DETECT_INITIAL = 4,
            DETECT_RESEND = 5,
            DETECT_VOLTAGE = 6;
    private int lastRow, lastHole, lastInside, lastDelay, delayTime, insertMode, insertIndex, soundSuccess, soundFail, soundAlert, changeMode;
    private boolean setDelayTime, scanMode, startReceive;
    private SerialPortUtil serialPortUtil;
    private TextView tvRow, tvHole, tvInside, tvLastDelay, tvTube, tvDelayTime, tvSectionDelay, tvInsideDelay;
    private ArrayList<DetonatorInfoBean> oldList;
    private MyButton btnNextRow, btnNextHole, btnInside, btnNextSection, btnSection;
    private String tempAddress;
    private int resendCount;
    private SerialDataReceiveListener myReceiveListener;
    private LocalSettingBean settings;
    private BaseApplication myApp;
    private SoundPool soundPool;
    private ADD_MODE add_mode;

    private boolean analyzeCode(String received) {
        myReceiveListener.setRcvData("");
        if (!settings.isNewLG())
            received = received.substring(0, received.indexOf(SerialCommand.RESPOND_SUCCESS));
        else {
            received = received.split("\\+")[1];
            received = received.substring(received.indexOf(SerialCommand.SCAN_RESPOND) + SerialCommand.SCAN_RESPOND.length());
        }
        received = received.replace("\1", "")
                .replace(" ", "")
                .replace("\n", "")
                .replace("\r", "");
        if (received.length() == 13) {
            tempAddress = received;
            int index = isExist(tempAddress);
            if (index > 0) {
                myApp.myToast(DetectActivity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_current_detonator_exist), index));
                myHandler.sendEmptyMessage(DETECT_FAIL);
            } else {
                myHandler.sendEmptyMessage(DETECT_SUCCESS);
            }
            return true;
        }
        return false;
    }    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_INITIAL:
                    enabledButton(true);
                    break;
                case DETECT_SUCCESS: //检测成功
                    myHandler.removeMessages(DETECT_RESEND);
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    resendCount = 0;
                    myReceiveListener.setRcvData("");
                    setDelayTime = false;
                    if (lastDelay != -1) {
                        switch (add_mode) {
                            case NEXT_ROW:
                                lastRow++;
                                lastHole = 1;
                                lastInside = 1;
                                break;
                            case NEXT_HOLE:
                            case NEXT_SECTION:
                                lastHole++;
                                lastInside = 1;
                                break;
                            case INSIDE_SECTION:
                            case INSIDE_HOLE:
                                lastInside++;
                                break;
                        }
                    } else {
                        lastRow = 1;
                        lastHole = 1;
                        lastInside = 1;
                    }
                    saveData(new DetonatorInfoBean(tempAddress, delayTime, lastRow, lastHole, lastInside, !scanMode));
                    setResult(RESULT_OK);
                    //tvDelayTime.setText("-- ms");
                    tvTube.setText(tempAddress);
                    lastDelay = delayTime;
                    enabledButton(true);
                    break;
                case DETECT_CONTINUE: //开始检测
                    myHandler.removeCallbacksAndMessages(null);
                    enabledButton(false);
                    tempAddress = "";
                    int row = lastRow, hole = lastHole, inside = lastInside;
                    if (lastDelay != -1) {
                        switch (add_mode) {
                            case NEXT_ROW:
                                row++;
                                hole = 1;
                                inside = 1;
                                break;
                            case NEXT_SECTION:
                                row = hole + 1;
                                delayTime = lastDelay + settings.getSection();
                            case NEXT_HOLE:
                                hole++;
                                inside = 1;
                                break;
                            case INSIDE_SECTION:
                                row = hole;
                                delayTime = lastDelay + settings.getSectionInside();
                            case INSIDE_HOLE:
                                inside++;
                                break;
                        }
                        if (!myApp.isTunnel()) {
                            delayTime = (row - 1) * settings.getRow() + (row - 1) * settings.getHole() + (hole - 1) * settings.getHole() + (inside - 1) * settings.getHoleInside();
                        }
                        if (delayTime < 0)
                            delayTime = 0;
                    } else {
                        row = 1;
                        hole = 1;
                        inside = 1;
                        delayTime = 0;
                    }
                    tvRow.setText(String.format(Locale.CHINA, "%d", row));
                    tvHole.setText(String.format(Locale.CHINA, "%d", hole));
                    tvInside.setText(String.format(Locale.CHINA, "%d", inside));
                    tvTube.setText("--");
                    tvDelayTime.setText(String.format(Locale.CHINA, "%dms", delayTime));
                    tvLastDelay.setText(lastDelay == -1 ? getResources().getString(R.string.no_delay_time) : (lastDelay + "ms"));
                    if (scanMode)
                        myHandler.sendEmptyMessage(DETECT_RESEND);
                    else if (settings.isNewLG()) {
                        changeMode = 4;
                        myHandler.sendEmptyMessage(DETECT_RESEND);
                    } else {
                        startReceive = false;
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "1950###");
                        myHandler.sendEmptyMessageDelayed(DETECT_VOLTAGE, ConstantUtils.BOOST_TIME);
                    }
                    break;
                case DETECT_FAIL: //检测失败
                    myHandler.removeMessages(DETECT_RESEND);
                    myApp.playSoundVibrate(soundPool, soundFail);
                    tvRow.setText(myApp.isTunnel() ? (lastHole == 0 ? "--" : (lastHole + "")) : (lastRow == 0 ? "--" : (lastRow + "")));
                    tvHole.setText(lastHole == 0 ? "--" : (lastHole + ""));
                    tvInside.setText(lastInside == 0 ? "--" : (lastInside + ""));
                    //tvTube.setText("--");
                    tvDelayTime.setText(R.string.no_delay_time);
                    enabledButton(true);
                    tempAddress = "";
                    break;
                case DETECT_VOLTAGE:
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "2900###");
                    myHandler.sendEmptyMessageDelayed(DETECT_RESEND, ConstantUtils.BOOST_TIME);
                    break;
                case DETECT_RESEND:
                    startReceive = true;
                    myHandler.removeMessages(DETECT_RESEND);
                    if (scanMode) {
                        //myApp.myToast(DetectActivity.this, "超时重发扫描指令！" + resendCount);
                        if (resendCount >= ConstantUtils.RESEND_TIMES) {
                            myApp.myToast(DetectActivity.this, R.string.message_scan_timeout);
                            myHandler.sendEmptyMessage(DETECT_FAIL);
                            break;
                        } else {
                            serialPortUtil.sendCmd(SerialCommand.CMD_SCAN);
                        }
                    } else if (4 == changeMode) {
                        serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 1));
                    } else if (!setDelayTime) {
                        //myApp.myToast(DetectActivity.this, "超时重发查询指令！" + resendCount);
                        if (resendCount >= ConstantUtils.RESEND_TIMES) {
                            myApp.myToast(DetectActivity.this, R.string.message_detonator_not_detected);
                            myHandler.sendEmptyMessage(DETECT_FAIL);
                            break;
                        }
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.READ_SN, 0);
                    } else {
                        //myApp.myToast(DetectActivity.this, "超时重发设置延期指令！" + tempAddress + "," + totalTime + "ms" + resendCount);
                        if (resendCount >= ConstantUtils.RESEND_TIMES) {
                            resendCount = 0;
                            setDelayTime = false;
                            myApp.myToast(DetectActivity.this, R.string.message_detonator_not_detected);
                            myHandler.sendEmptyMessage(DETECT_FAIL);
                            break;
                        } else {
                            serialPortUtil.sendCmd(tempAddress, settings.isNewLG() ? SerialCommand.ACTION_TYPE.SHORT_CMD2_SET_DELAY : SerialCommand.ACTION_TYPE.SET_DELAY, delayTime);
                        }
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_RESEND, scanMode ? ConstantUtils.SCAN_TIMEOUT : ConstantUtils.RESEND_CMD_TIMEOUT);
                    resendCount++;
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private void saveData(DetonatorInfoBean bean) {
        for (DetonatorInfoBean b : oldList) {
            if (b.getAddress().equals(bean.getAddress())) {
                oldList.remove(b);
                break;
            }
        }
//        myApp.myToast(this, "index:" + insertIndex + ",size" + oldList.size());
        if (insertIndex <= oldList.size() - 1 && insertMode != 0) {
            int period;
            if (insertMode == ConstantUtils.INSERT_INSIDE) {
                period = myApp.isTunnel() ? settings.getSectionInside() : settings.getHoleInside();
            } else {
                period = myApp.isTunnel() ? settings.getSection() : settings.getHole();
            }

            for (int i = insertIndex; i < oldList.size(); i++) {
                DetonatorInfoBean b = oldList.get(i);
                if (bean.getRow() != b.getRow()
                        || (insertMode == ConstantUtils.INSERT_INSIDE && bean.getHole() != b.getHole()))
                    break;
                b.setDownloaded(false);
                b.setDelayTime(b.getDelayTime() + period);
                if (insertMode == ConstantUtils.INSERT_HOLE && bean.getRow() == b.getRow()) {
                    b.setHole(b.getHole() + 1);
                } else if (insertMode == ConstantUtils.INSERT_INSIDE && bean.getRow() == b.getRow() && bean.getHole() == b.getHole()) {
                    b.setInside(b.getInside() + 1);
                }
            }
            oldList.add(insertIndex, bean);
        } else {
            oldList.add(bean);
        }
        insertIndex++;
        try {
            myApp.writeToFile(myApp.getListFile(), oldList);
            String[] fileList = Arrays.copyOfRange(FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1], 1,
                    FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1].length);
            for (String s : fileList) {
                File file = new File(s);
                if (file.exists() && !file.delete()) {
                    myApp.myToast(DetectActivity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
                }
            }
        } catch (JSONException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(DetectActivity.this, R.string.message_transfer_error);
        }
    }    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            String received = myReceiveListener.getRcvData();
//            myApp.myToast(DetectActivity.this, received);
            if (received.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                setResult(RESULT_CANCELED, new Intent().putExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_SHORT_CIRCUIT));
                finish();
            } else if (received.contains(SerialCommand.INITIAL_FAIL)) {
                myApp.myToast(DetectActivity.this, R.string.message_open_module_fail);
                setResult(RESULT_CANCELED, new Intent().putExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_OPEN_FAIL));
                finish();
            } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                myReceiveListener.setRcvData("");
                if (settings.isNewLG()) {
                    myReceiveListener.setRcvData("");
                    changeMode = 1;
                    serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                } else {
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "2900###");
                    myHandler.sendEmptyMessage(DETECT_INITIAL);
                }
            } else if (startReceive) {
                if (settings.isNewLG()) {
                    if (received.contains(SerialCommand.SCAN_RESPOND)) {
                        if (scanMode && received.length() > 10)
                            analyzeCode(received);
                    } else if (received.contains(SerialCommand.AT_CMD_RESPOND)) {
                        myReceiveListener.setRcvData("");
                        switch (changeMode) {
                            case 1:
                                changeMode++;
                                if (scanMode) {
                                    changeMode++;
                                    myHandler.sendEmptyMessage(DETECT_INITIAL);
                                } else
                                    serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_INITIAL_VOLTAGE, ConstantUtils.DEFAULT_SINGLE_WORK_VOLTAGE, ConstantUtils.DEFAULT_SINGLE_WORK_VOLTAGE));
                                break;
                            case 2:
                                changeMode++;
                                myHandler.sendEmptyMessageDelayed(DETECT_INITIAL, ConstantUtils.INITIAL_VOLTAGE_DELAY);
                                break;
                            case 4:
                                changeMode++;
                                myHandler.sendEmptyMessage(DETECT_RESEND);
                                break;
                        }
                    } else if (received.startsWith(SerialCommand.DATA_PREFIX)) {
                        if (!setDelayTime) {
                            if (received.length() > 10 && serialPortUtil.checkData(received)) {
                                tempAddress = received.substring(6, received.length() - 2);
                                char character = (char) (int) Integer.valueOf(tempAddress.substring(4, 6), 16);
                                if (character == 0xff)
                                    character = 'F';
                                tempAddress = tempAddress.substring(0, 4) + tempAddress.substring(6, 9) + character + tempAddress.substring(9);
                                myReceiveListener.setRcvData("");
                                resendCount = 0;
                                int index = isExist(tempAddress);
                                if (index > 0) {
                                    myApp.myToast(DetectActivity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_current_detonator_exist), index));
                                    myHandler.sendEmptyMessage(DETECT_FAIL);
                                    return;
                                }
                                setDelayTime = true;
                                myHandler.removeMessages(DETECT_RESEND);
                                myHandler.sendEmptyMessage(DETECT_RESEND);
                            }
                        } else {
                            myHandler.removeMessages(DETECT_RESEND);
                            myHandler.sendEmptyMessage(DETECT_SUCCESS);
                        }
                    }
                } else if (scanMode) {
                    if (received.contains(SerialCommand.RESPOND_SUCCESS)) {
                        if (analyzeCode(received))
                            myHandler.removeMessages(DETECT_RESEND);
                    }
                } else {
                    if (!setDelayTime) {
                        String confirm = SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.READ_SN);
                        assert confirm != null;
                        if (received.contains(confirm)) {
                            received = received.substring(received.indexOf(confirm) + confirm.length());
                            if (received.length() >= 16) {
                                tempAddress = received.substring(0, 14);
                                try {
                                    int checkSum = 0;
                                    StringBuilder temp = new StringBuilder();
                                    for (int j = 0; j < tempAddress.length(); j += 2) {
                                        byte a = (byte) Integer.parseInt(tempAddress.substring(j, j + 2), 16);
                                        checkSum += a;
                                        if (j >= 4) {
                                            a ^= SerialCommand.XOR_DATA;
                                            temp.append(String.format("%02X", a));
                                        }
                                    }

                                    if (String.format("%02X", checkSum).endsWith(received.substring(14, 16))) {
                                        char character = (char) (int) Integer.valueOf(tempAddress.substring(12), 16);
                                        tempAddress = tempAddress.substring(0, 7) + character + tempAddress.substring(7, 12);
                                        myReceiveListener.setRcvData("");
                                        resendCount = 0;
                                        int index = isExist(tempAddress);
                                        if (index > 0) {
                                            myApp.myToast(DetectActivity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_current_detonator_exist), index));
                                            myHandler.sendEmptyMessage(DETECT_FAIL);
                                            return;
                                        }
                                        setDelayTime = true;
                                        myHandler.removeMessages(DETECT_RESEND);
                                        myHandler.sendEmptyMessage(DETECT_RESEND);
                                    }
                                } catch (Exception e) {
                                    BaseApplication.writeErrorLog(e);
                                }
                            }
                        } else if (received.contains(SerialCommand.RESPOND_FAIL)) {
                            myReceiveListener.setRcvData("");
                            myHandler.removeMessages(DETECT_RESEND);
                            myApp.myToast(DetectActivity.this, R.string.message_detonator_not_detected);
                            myHandler.sendEmptyMessage(DETECT_FAIL);
                        }
                    } else {
                        if (received.contains(Objects.requireNonNull(SerialCommand.RESPOND_CONFIRM.get(SerialCommand.ACTION_TYPE.SET_DELAY)))) {
                            myHandler.removeMessages(DETECT_RESEND);
                            myHandler.sendEmptyMessage(DETECT_SUCCESS);
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);

        myApp = (BaseApplication) getApplication();
        lastRow = getIntent().getIntExtra(KeyUtils.KEY_LAST_ROW, 0);
        lastHole = getIntent().getIntExtra(KeyUtils.KEY_LAST_HOLE, 0);
        lastInside = getIntent().getIntExtra(KeyUtils.KEY_LAST_INSIDE, 0);
        lastDelay = getIntent().getIntExtra(KeyUtils.KEY_LAST_DELAY, 0);
        scanMode = getIntent().getBooleanExtra(KeyUtils.KEY_SCAN_MODE, false);
        insertMode = getIntent().getIntExtra(KeyUtils.KEY_INSERT_MODE, 0);
        insertIndex = getIntent().getIntExtra(KeyUtils.KEY_INSERT_INDEX, 0);
        setTitle(scanMode ? R.string.scan_line : R.string.register_line);

        tvTube = findViewById(R.id.tv_tube);
        tvTube.setText("--");
        tvDelayTime = findViewById(R.id.tv_delay);
        tvDelayTime.setText(R.string.no_delay_time);
        tvRow = findViewById(R.id.tv_row);
        tvHole = findViewById(R.id.tv_hole);
        tvHole.setText(lastHole == 0 ? "--" : (lastHole + ""));
        tvInside = findViewById(R.id.tv_inside);
        tvInside.setText(lastInside == 0 ? "--" : (lastInside + ""));
        tvLastDelay = findViewById(R.id.tv_last_delay);
        tvLastDelay.setText(lastDelay == -1 ? getResources().getString(R.string.no_delay_time) : (lastDelay + "ms"));
        btnNextRow = findViewById(R.id.btn_next_row);
        btnNextHole = findViewById(R.id.btn_next_hole);
        btnInside = findViewById(R.id.btn_inside);
        btnNextSection = findViewById(R.id.btn_next_section);
        btnSection = findViewById(R.id.btn_section);
        oldList = new ArrayList<>();
        myApp.readFromFile(myApp.getListFile(), oldList, DetonatorInfoBean.class);
        settings = BaseApplication.readSettings();

        if (myApp.isTunnel()) {
            findViewById(R.id.rl_set_delay).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_tunnel).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_open_air).setVisibility(View.INVISIBLE);
            findViewById(R.id.ll_hole).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.txt_row)).setText(R.string.text_section_num);
            ((TextView) findViewById(R.id.txt_inside)).setText(R.string.text_section_inside_num);
            tvSectionDelay = findViewById(R.id.tv_row_delay);
            tvInsideDelay = findViewById(R.id.tv_inside_delay);
            tvSectionDelay.setText(String.format(Locale.CHINA, "%dms", settings.getSection()));
            tvInsideDelay.setText(String.format(Locale.CHINA, "%dms", settings.getSectionInside()));
            tvSectionDelay.setOnClickListener((v) -> modifyDelay(false));
            findViewById(R.id.txt_delay1).setOnClickListener((v) -> modifyDelay(false));
            tvInsideDelay.setOnClickListener((v) -> modifyDelay(true));
            findViewById(R.id.txt_delay3).setOnClickListener((v) -> modifyDelay(true));
            tvRow.setText(lastHole == 0 ? "--" : (lastHole + ""));
        } else {
            findViewById(R.id.rl_set_delay).setVisibility(View.GONE);
            findViewById(R.id.rl_open_air).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_tunnel).setVisibility(View.INVISIBLE);
            tvRow.setText(lastRow == 0 ? "--" : (lastRow + ""));
        }

        btnNextHole.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        btnNextRow.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));
        btnInside.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_3));
        btnSection.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        btnNextSection.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));

        setDelayTime = false;
        resendCount = 0;
        add_mode = ADD_MODE.NONE;
        btnNextHole.requestFocus();

        enabledButton(false);
        startReceive = settings.isNewLG();
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(DetectActivity.this, bufferRunnable, settings.isNewLG());
            myReceiveListener.setScanMode(scanMode);
            myReceiveListener.setMaxCurrent(1000);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(DetectActivity.this, R.string.message_open_module_fail);
            setResult(RESULT_CANCELED, new Intent().putExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_OPEN_FAIL));
            finish();
        }
        initSound();
    }

    private void modifyDelay(final boolean inside) {
        runOnUiThread(() -> {
            final View view = LayoutInflater.from(DetectActivity.this).inflate(R.layout.layout_edit_dialog, null);
            final EditText etDelay = view.findViewById(R.id.et_dialog);
            int title = inside ? R.string.dialog_title_modify_tunnel_hole : R.string.dialog_title_modify_tunnel_section;

            etDelay.setHint(R.string.hint_input_delay_time);
            etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
            etDelay.setInputType(InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(DetectActivity.this, R.style.AlertDialog)
                    .setTitle(title)
                    .setView(view)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                        if (!etDelay.getText().toString().isEmpty()) {
                            String text = etDelay.getText().toString() + "ms";
                            if (inside) {
                                tvInsideDelay.setText(text);
                                settings.setSectionInside(Integer.parseInt(etDelay.getText().toString()));
                            } else {
                                tvSectionDelay.setText(text);
                                settings.setSection(Integer.parseInt(etDelay.getText().toString()));
                            }
                            myApp.saveSettings(settings);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create().show();
        });
    }

    private int isExist(String id) {
//        if (insertMode == 0) {
        for (int i = 0; i < oldList.size(); i++)
            if (oldList.get(i).getAddress().equals(id)) {
                return i + 1;
            }
//        } else {
//            for (int i = 0; i < insertIndex; i++)
//                if (oldList.get(i).getAddress().equals(id)) {
//                    return i + 1;
//                }
//            for (int i = 0; i < newList.size(); i++)
//                if (newList.get(i).getAddress().equals(id)) {
//                    return insertIndex + i + 1;
//                }
//            for (int i = insertIndex; i < oldList.size(); i++)
//                if (oldList.get(i).getAddress().equals(id)) {
//                    return insertIndex + newList.size() + i + 1;
//                }
//
//        }
        return 0;
    }

    private void enabledButton(boolean enable) {
        startReceive = !enable;
        resendCount = 0;
        if (null != myReceiveListener) {
            myReceiveListener.setRcvData("");
            myReceiveListener.setStartAutoDetect(enable);
        }
        setProgressVisibility(!enable);
        if (insertMode > 0)
            btnNextRow.setEnabled(false);
        else
            btnNextRow.setEnabled(enable);
        if (insertMode == ConstantUtils.INSERT_INSIDE) {
            btnNextHole.setEnabled(false);
            btnNextSection.setEnabled(false);
        } else {
            btnNextHole.setEnabled(enable);
            btnNextSection.setEnabled(enable);
        }
        if (insertMode == ConstantUtils.INSERT_HOLE) {
            btnInside.setEnabled(false);
            btnSection.setEnabled(false);
        } else {
            btnInside.setEnabled(enable);
            btnSection.setEnabled(enable);
        }
        if (myApp.isTunnel()) {
            btnNextRow.setEnabled(false);
            btnNextHole.setEnabled(false);
            btnInside.setEnabled(false);
        } else {
            btnNextSection.setEnabled(false);
            btnSection.setEnabled(false);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        executeFunction(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void executeFunction(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                if (btnNextHole.isEnabled() || btnSection.isEnabled()) {
                    add_mode = myApp.isTunnel() ? ADD_MODE.INSIDE_SECTION : ADD_MODE.NEXT_HOLE;
                    myHandler.sendEmptyMessage(DETECT_CONTINUE);
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (btnNextRow.isEnabled() || btnNextSection.isEnabled()) {
                    add_mode = myApp.isTunnel() ? ADD_MODE.NEXT_SECTION : ADD_MODE.NEXT_ROW;
                    myHandler.sendEmptyMessage(DETECT_CONTINUE);
//                    if (myApp.isTunnel()) {
//                        add_mode = ADD_MODE.NEXT_SECTION;
//                        myHandler.sendEmptyMessage(DETECT_CONTINUE);
//
//                        if (nextSection) {
//                            myHandler.sendEmptyMessage(DETECT_CONTINUE);
//                        } else {
//                            final View holeView = LayoutInflater.from(DetectActivity.this).inflate(R.layout.layout_section_dialog, null);
//                            final EditText etSection = holeView.findViewById(R.id.et_section);
//                            final EditText etInside = holeView.findViewById(R.id.et_inside);
//                            etSection.setText(String.format(Locale.CHINA, "%d", settings.getSection()));
//                            etInside.setText(String.format(Locale.CHINA, "%d", settings.getSectionInside()));
//
//                            new AlertDialog.Builder(DetectActivity.this, R.style.AlertDialog)
//                                    .setTitle(R.string.dialog_title_input_delay)
//                                    .setView(holeView)
//                                    .setPositiveButton(R.string.btn_confirm, (dialog, which1) -> {
//                                        try {
//                                            add_mode = ADD_MODE.NEXT_SECTION;
//                                            settings.setSectionInside(Integer.parseInt(etInside.getText().toString()));
//                                            settings.setSection(Integer.parseInt(etSection.getText().toString()));
//                                            myApp.saveSettings(settings);
//                                            nextSection = true;
//                                        } catch (Exception e) {
//                                            myApp.myToast(DetectActivity.this, R.string.message_input_error);
//                                            BaseApplication.writeErrorLog(e);
//                                        }
//                                    })
//                                    .setNegativeButton(R.string.btn_cancel, null)
//                                    .create().show();
//                        }
//                    } else {
//                        add_mode = ADD_MODE.NEXT_ROW;
//                        myHandler.sendEmptyMessage(DETECT_CONTINUE);
//                    }
                }
                break;
            case KeyEvent.KEYCODE_3:
                if (btnInside.isEnabled() && !myApp.isTunnel()) {
                    add_mode = ADD_MODE.INSIDE_HOLE;
                    myHandler.sendEmptyMessage(DETECT_CONTINUE);
                }
                break;
            case KeyEvent.KEYCODE_POUND:
                if (myApp.isTunnel()) {
                    modifyDelay(true);
                }
                break;
            case KeyEvent.KEYCODE_STAR:
                if (myApp.isTunnel()) {
                    modifyDelay(false);
                }
                break;
        }
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
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
        if (settings.isNewLG() && null != serialPortUtil) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        super.onDestroy();
    }

    private enum ADD_MODE {
        NONE,                         //无效
        NEXT_HOLE,                   //下一孔
        NEXT_ROW,                    //下一排
        INSIDE_HOLE,                //当前孔
        INSIDE_SECTION,            //当前段
        NEXT_SECTION               //下一段
    }




}
