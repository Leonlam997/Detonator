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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DetectActivity extends BaseActivity {
    private final int DETECT_SUCCESS = 1;
    private final int DETECT_CONTINUE = 2;
    private final int DETECT_FAIL = 3;
    private final int DETECT_INITIAL = 4;
    private final int DETECT_SEND_COMMAND = 5;
    private final int DETECT_NEXT_STEP = 6;
    private final int STEP_CHECK_CONFIG = 1;
    private final int STEP_CLEAR_STATUS = 2;
    private final int STEP_SCAN = 3;
    private final int STEP_READ_SHELL = 4;
    private final int STEP_WRITE_FIELD = 5;
    private final int STEP_END = 6;
    private final int STEP_SCAN_CODE = 7;
    private final int STEP_DATA_ERROR = 8;
    private int lastRow;
    private int lastHole;
    private int lastInside;
    private int lastDelay;
    private int delayTime;
    private int insertMode;
    private int insertIndex;
    private int soundSuccess;
    private int soundFail;
    private int soundAlert;
    private int flowStep;
    private boolean scanMode;
    private SerialPortUtil serialPortUtil;
    private TextView tvRow;
    private TextView tvHole;
    private TextView tvInside;
    private TextView tvLastDelay;
    private TextView tvTube;
    private TextView tvDelayTime;
    private TextView tvSectionDelay;
    private TextView tvInsideDelay;
    private ArrayList<DetonatorInfoBean> oldList;
    private MyButton btnNextRow;
    private MyButton btnNextHole;
    private MyButton btnInside;
    private MyButton btnNextSection;
    private MyButton btnSection;
    private String tempAddress;
    private SerialDataReceiveListener myReceiveListener;
    private LocalSettingBean settings;
    private BaseApplication myApp;
    private SoundPool soundPool;
    private ADD_MODE add_mode;
    private final Map<Integer, Integer> failCode = new HashMap<Integer, Integer>() {
        {
            put(STEP_SCAN, R.string.message_detonator_not_detected);
            put(STEP_READ_SHELL, R.string.message_detonator_read_shell_error);
            put(STEP_WRITE_FIELD, R.string.message_detonator_write_error);
            put(STEP_CHECK_CONFIG, R.string.message_detect_error);
            put(STEP_CLEAR_STATUS, R.string.message_detonator_write_error);
            put(STEP_SCAN_CODE, R.string.message_scan_timeout);
            put(STEP_DATA_ERROR, R.string.message_return_data_error);
        }
    };


    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_INITIAL:
                    enabledButton(true);
                    break;
                case DETECT_SUCCESS: //检测成功
                    myHandler.removeMessages(DETECT_SEND_COMMAND);
                    flowStep = STEP_END;
                    myApp.playSoundVibrate(soundPool, soundSuccess);
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
                    flowStep = scanMode ? STEP_SCAN_CODE : STEP_CHECK_CONFIG;
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
                    myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                    break;
                case DETECT_FAIL: //检测失败
                    myHandler.removeMessages(DETECT_SEND_COMMAND);
                    if (STEP_END != flowStep) {
                        Integer i = failCode.get(flowStep);
                        if (null != i)
                            myApp.myToast(DetectActivity.this, i);
                    }
                    flowStep = STEP_END;
                    myApp.playSoundVibrate(soundPool, soundFail);
                    tvRow.setText(myApp.isTunnel() ? (lastHole == 0 ? "--" : (lastHole + "")) : (lastRow == 0 ? "--" : (lastRow + "")));
                    tvHole.setText(lastHole == 0 ? "--" : (lastHole + ""));
                    tvInside.setText(lastInside == 0 ? "--" : (lastInside + ""));
                    //tvTube.setText("--");
                    tvDelayTime.setText(R.string.no_delay_time);
                    enabledButton(true);
                    tempAddress = "";
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
                            flowStep = STEP_WRITE_FIELD;
                            break;
                    }
                case DETECT_SEND_COMMAND:
                    myHandler.removeMessages(DETECT_SEND_COMMAND);
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
                        case STEP_WRITE_FIELD:
                            serialPortUtil.sendCmd(tempAddress, SerialCommand.CODE_WRITE_FIELD, oldList.size() + insertIndex + 1, delayTime, Integer.parseInt(tvHole.getText().toString()));
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            break;
                        case STEP_SCAN_CODE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_SCAN_TIMEOUT);
                            break;
                    }
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
    }

    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] received = myReceiveListener.getRcvData();
//            myApp.myToast(DetectActivity.this, received);
            if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                setResult(RESULT_CANCELED, new Intent().putExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_SHORT_CIRCUIT));
                finish();
            } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                myHandler.sendEmptyMessage(DETECT_INITIAL);
            } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                myApp.myToast(DetectActivity.this, R.string.message_open_module_fail);
                finish();
            } else if (received.length > 5) {
                myHandler.removeMessages(DETECT_SEND_COMMAND);
                if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                    switch (flowStep) {
                        case STEP_SCAN:
                            tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9));
                            if (!Pattern.matches(ConstantUtils.UID_PATTERN, tempAddress)) {
                                flowStep = STEP_DATA_ERROR;
                                myHandler.sendEmptyMessage(DETECT_FAIL);
                                return;
                            }
                            break;
                        case STEP_SCAN_CODE:
                            if (received.length < 22) {
                                myHandler.sendEmptyMessage(DETECT_FAIL);
                                return;
                            }
                        case STEP_READ_SHELL:
                            tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                            if (!Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                                flowStep = STEP_DATA_ERROR;
                                myHandler.sendEmptyMessage(DETECT_FAIL);
                                return;
                            } else {
                                int index = isExist(tempAddress);
                                if (index > 0) {
                                    flowStep = STEP_END;
                                    myApp.myToast(DetectActivity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_current_detonator_exist), index));
                                    myHandler.sendEmptyMessage(DETECT_FAIL);
                                    return;
                                } else if (flowStep == STEP_SCAN_CODE) {
                                    myHandler.sendEmptyMessage(DETECT_SUCCESS);
                                    return;
                                }
                            }
                            break;
                        case STEP_WRITE_FIELD:
                            myHandler.sendEmptyMessage(DETECT_SUCCESS);
                            return;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                } else {
                    if (STEP_WRITE_FIELD == flowStep)
                        myHandler.sendEmptyMessage(DETECT_SUCCESS);
                    else
                        myHandler.sendEmptyMessage(DETECT_FAIL);
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

        add_mode = ADD_MODE.NONE;
        btnNextHole.requestFocus();

        enabledButton(false);
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(DetectActivity.this, bufferRunnable);
            myReceiveListener.setSingleConnect(true);
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
        for (int i = 0; i < oldList.size(); i++)
            if (oldList.get(i).getAddress().equals(id)) {
                return i + 1;
            }
        return 0;
    }

    private void enabledButton(boolean enable) {
        if (null != myReceiveListener) {
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
        if (null != serialPortUtil) {
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
