package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DetectActivity extends BaseActivity {
    private DataReceiveListener myReceiveListener;
    private SerialPortUtil serialPortUtil;
    private List<DetonatorBean> list;
    private TextView tvRow;
    private TextView tvHole;
    private TextView tvInside;
    private TextView tvLastDelay;
    private TextView tvTube;
    private TextView tvDelayTime;
    private TextView tvRowDelay;
    private TextView tvHoleDelay;
    private TextView tvInsideDelay;
    private MyButton btnNextRow;
    private MyButton btnNextHole;
    private MyButton btnInside;
    private RadioButton rbScan;
    private RadioButton rbRegister;
    private String tempAddress;
    private SoundPool soundPool;
    private AddMode addMode;
    private final int DETECT_CONTINUE = 1;
    private int lastRow;
    private int lastHole;
    private int lastInside;
    private int lastDelay;
    private int delayTime;
    private int insertMode;
    private int insertIndex;
    private int soundSuccess;
    private int soundFail;
    private int flowStep;
    private long schemeId;

    private final Handler myHandler = new Handler(msg -> {
        final int DETECT_SUCCESS = 2;
        final int DETECT_FAIL = 3;
        final int DETECT_SEND_COMMAND = 5;
        final int DETECT_NEXT_STEP = 6;
        final int STEP_CHECK_CONFIG = 1;
        final int STEP_CLEAR_STATUS = 2;
        final int STEP_SCAN = 3;
        final int STEP_READ_SHELL = 4;
        final int STEP_WRITE_FIELD = 5;
        final int STEP_SCAN_CODE = 7;
        final int STEP_DATA_ERROR = 8;
        final int STEP_END = 6;
        switch (msg.what) {
            case DETECT_SUCCESS: //检测成功
                msg.getTarget().removeMessages(DETECT_SEND_COMMAND);
                flowStep = STEP_END;
                myApp.playSoundVibrate(soundPool, soundSuccess);
                if (lastDelay != -1) {
                    switch (addMode) {
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
                saveData(new DetonatorBean(schemeId, tempAddress, delayTime, lastRow, lastHole, lastInside, !rbScan.isChecked()));
                setResult(RESULT_OK);
                //tvDelayTime.setText("-- ms");
                tvTube.setText(tempAddress);
                lastDelay = delayTime;
                enabledButton(true);
                break;
            case DETECT_CONTINUE: //开始检测
                msg.getTarget().removeCallbacksAndMessages(null);
                enabledButton(false);
                tempAddress = "";
                flowStep = rbScan.isChecked() ? STEP_SCAN_CODE : STEP_CLEAR_STATUS;
                int row = lastRow, hole = lastHole, inside = lastInside;
                if (lastDelay != -1) {
                    switch (addMode) {
                        case NEXT_ROW:
                            row++;
                            hole = 1;
                            inside = 1;
                            delayTime = 0;
                            if (row > 1) {
                                int r = 0;
                                for (DetonatorBean bean : list)
                                    if (bean.getRow() != r) {
                                        r = bean.getRow();
                                        if (bean.getRow() >= row)
                                            break;
                                        delayTime = bean.getDelayTime() + BaseApplication.settings.getRow();
                                    }
                            }
                            break;
                        case NEXT_SECTION:
                            row = ++hole;
                            delayTime = lastDelay + BaseApplication.settings.getSection();
                            inside = 1;
                            break;
                        case NEXT_HOLE:
                            hole++;
                            delayTime = 0;
                            if (hole > 1) {
                                int h = 0;
                                for (DetonatorBean bean : list)
                                    if (bean.getRow() == row && bean.getHole() != h) {
                                        h = bean.getHole();
                                        if (bean.getHole() >= hole)
                                            break;
                                        delayTime = bean.getDelayTime() + BaseApplication.settings.getHole();
                                    }
                            }
                            break;
                        case INSIDE_SECTION:
                            row = hole;
                            inside++;
                            delayTime = lastDelay + BaseApplication.settings.getSectionInside();
                            break;
                        case INSIDE_HOLE:
                            inside++;
                            delayTime = lastDelay + BaseApplication.settings.getHoleInside();
                            break;
                    }
                } else {
                    row = 1;
                    hole = 1;
                    inside = 1;
                    delayTime = 0;
                }
                tvRow.setText(String.format(Locale.getDefault(), "%d", row));
                tvHole.setText(String.format(Locale.getDefault(), "%d", hole));
                tvInside.setText(String.format(Locale.getDefault(), "%d", inside));
                tvTube.setText("--");
                tvDelayTime.setText(String.format(Locale.getDefault(), "%dms", delayTime));
                tvLastDelay.setText(lastDelay == -1 ? getString(R.string.no_delay_time) : (lastDelay + "ms"));
                msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                break;
            case DETECT_FAIL: //检测失败
                final Map<Integer, Integer> failCode = new HashMap<Integer, Integer>() {
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
                msg.getTarget().removeMessages(DETECT_SEND_COMMAND);
                if (STEP_END != flowStep) {
                    Integer i = failCode.get(flowStep);
                    if (null != i)
                        myApp.myToast(DetectActivity.this, i);
                }
                flowStep = STEP_END;
                myApp.playSoundVibrate(soundPool, soundFail);
                tvRow.setText(BaseApplication.isTunnel ? (lastHole == 0 ? "--" : (lastHole + "")) : (lastRow == 0 ? "--" : (lastRow + "")));
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
                msg.getTarget().removeMessages(DETECT_SEND_COMMAND);
                switch (flowStep) {
                    case STEP_CHECK_CONFIG:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_SINGLE_READ_CONFIG, 0);
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case STEP_CLEAR_STATUS:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_CLEAR_READ_STATUS, 0);
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_SCAN:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_UID, ConstantUtils.UID_LEN);
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case STEP_READ_SHELL:
                        serialPortUtil.sendCmd(tempAddress, SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case STEP_WRITE_FIELD:
                        serialPortUtil.sendCmd(tempAddress, SerialCommand.CODE_WRITE_FIELD, list.size() + insertIndex + 1, delayTime, Integer.parseInt(tvHole.getText().toString()));
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        break;
                    case STEP_SCAN_CODE:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_SCAN_TIMEOUT);
                        break;
                }
                break;
            case DataReceiveListener.HANDLER_RECEIVED_DATA:
                byte[] received = (byte[]) msg.obj;
                if (received != null && received.length > 0)
                    if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                        setResult(RESULT_CANCELED, new Intent().putExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_SHORT_CIRCUIT));
                        finish();
                    } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                        enabledButton(true);
                        myReceiveListener.setStartDetectShort(true);
                    } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                        myApp.myToast(DetectActivity.this, R.string.message_open_module_fail);
                        finish();
                    } else if (received.length > 5) {
                        msg.getTarget().removeMessages(DETECT_SEND_COMMAND);
                        if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                            switch (flowStep) {
                                case STEP_SCAN:
                                    if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                        received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                                    tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9));
                                    if (!Pattern.matches(ConstantUtils.UID_PATTERN, tempAddress)) {
                                        flowStep = STEP_DATA_ERROR;
                                        msg.getTarget().sendEmptyMessage(DETECT_FAIL);
                                        return false;
                                    }
                                    break;
                                case STEP_SCAN_CODE:
                                    if (received.length < 22) {
                                        msg.getTarget().sendEmptyMessage(DETECT_FAIL);
                                        return false;
                                    }
                                case STEP_READ_SHELL:
                                    tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                                    if (!Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                                        flowStep = STEP_DATA_ERROR;
                                        msg.getTarget().sendEmptyMessage(DETECT_FAIL);
                                        return false;
                                    } else {
                                        int index = list.indexOf(new DetonatorBean(tempAddress));
                                        if (index >= 0) {
                                            flowStep = STEP_END;
                                            myApp.myToast(DetectActivity.this, String.format(Locale.getDefault(), getString(R.string.message_current_detonator_exist), index + 1));
                                            msg.getTarget().sendEmptyMessage(DETECT_FAIL);
                                            return false;
                                        } else if (flowStep == STEP_SCAN_CODE) {
                                            msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                            return false;
                                        }
                                    }
                                    break;
                                case STEP_WRITE_FIELD:
                                    msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                    return false;
                            }
                            msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                        } else
                            msg.getTarget().sendEmptyMessage(STEP_WRITE_FIELD == flowStep ? DETECT_SUCCESS : DETECT_FAIL);
                    }
            default:
                break;
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        lastRow = getIntent().getIntExtra(KeyUtils.KEY_LAST_ROW, 0);
        lastHole = getIntent().getIntExtra(KeyUtils.KEY_LAST_HOLE, 0);
        lastInside = getIntent().getIntExtra(KeyUtils.KEY_LAST_INSIDE, 0);
        lastDelay = getIntent().getIntExtra(KeyUtils.KEY_LAST_DELAY, 0);
        insertMode = getIntent().getIntExtra(KeyUtils.KEY_INSERT_MODE, 0);
        insertIndex = getIntent().getIntExtra(KeyUtils.KEY_INSERT_INDEX, 0);
        setTitle(R.string.add_detonator);
        schemeId = getIntent().getLongExtra(KeyUtils.KEY_TABLE_ID, -1);
        BaseApplication.writeFile("schemeId:" + schemeId + "row:" + lastRow + ", hole:" + lastHole + ", inside:" + lastInside + ", delay:" + lastDelay + ", mode:" + insertMode + ", index:" + insertIndex);
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
        tvLastDelay.setText(lastDelay == -1 ? getString(R.string.no_delay_time) : (lastDelay + "ms"));
        btnNextRow = findViewById(R.id.btn_next_row);
        btnNextHole = findViewById(R.id.btn_next_hole);
        btnInside = findViewById(R.id.btn_inside);
        rbScan = findViewById(R.id.rb_scan);
        rbRegister = findViewById(R.id.rb_register);
        tvRowDelay = findViewById(R.id.tv_row_delay);
        tvHoleDelay = findViewById(R.id.tv_hole_delay);
        tvInsideDelay = findViewById(R.id.tv_inside_delay);
        tvRowDelay.setOnClickListener((v) -> modifyDelay(1));
        findViewById(R.id.txt_delay1).setOnClickListener((v) -> modifyDelay(1));
        tvInsideDelay.setOnClickListener((v) -> modifyDelay(3));
        findViewById(R.id.txt_delay3).setOnClickListener((v) -> modifyDelay(3));
        list = DbUtil.getDetonatorList(schemeId);

        if (BaseApplication.isTunnel) {
            findViewById(R.id.ll_hole).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.txt_row)).setText(R.string.text_section_num);
            ((TextView) findViewById(R.id.txt_inside)).setText(R.string.text_section_inside_num);
            ((TextView) findViewById(R.id.txt_delay1)).setText(R.string.txt_section_delay);
            findViewById(R.id.txt_delay2).setVisibility(View.INVISIBLE);
            btnNextHole.setTextId(R.string.button_section_inside);
            btnNextRow.setTextId(R.string.button_next_section);
            btnInside.setVisibility(View.GONE);
            tvHoleDelay.setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.txt_delay3)).setText(R.string.txt_section_inside_delay);
            tvRowDelay.setText(String.format(Locale.getDefault(), "%dms", BaseApplication.settings.getSection()));
            tvInsideDelay.setText(String.format(Locale.getDefault(), "%dms", BaseApplication.settings.getSectionInside()));
            tvRow.setText(lastHole == 0 ? "--" : (lastHole + ""));
        } else {
            tvHoleDelay.setOnClickListener((v) -> modifyDelay(2));
            findViewById(R.id.txt_delay2).setOnClickListener((v) -> modifyDelay(2));
            ((TextView) findViewById(R.id.txt_delay1)).setText(R.string.txt_row_delay);
            findViewById(R.id.txt_delay2).setVisibility(View.VISIBLE);
            tvHoleDelay.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.txt_delay3)).setText(R.string.txt_inside_delay);
            tvRowDelay.setText(String.format(Locale.getDefault(), "%dms", BaseApplication.settings.getRow()));
            tvHoleDelay.setText(String.format(Locale.getDefault(), "%dms", BaseApplication.settings.getHole()));
            tvInsideDelay.setText(String.format(Locale.getDefault(), "%dms", BaseApplication.settings.getHoleInside()));
            tvRow.setText(lastRow == 0 ? "--" : (lastRow + ""));
        }
        btnNextHole.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        btnNextRow.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));
        btnInside.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_3));
        addMode = AddMode.NONE;
        btnNextHole.requestFocus();

        enabledButton(false);
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = DataReceiveListener.getInstance(DetectActivity.this, myHandler);
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

    private void modifyDelay(final int whichDelay) {
        runOnUiThread(() -> {
            final View view = LayoutInflater.from(DetectActivity.this).inflate(R.layout.layout_dialog_edit, null);
            final EditText etDelay = view.findViewById(R.id.et_dialog);
            int title = R.string.dialog_title_modify_open_air_hole;

            etDelay.setHint(R.string.hint_input_delay_time);
            etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
            etDelay.setInputType(InputType.TYPE_CLASS_NUMBER);
            if (whichDelay == 1) {
                title = BaseApplication.isTunnel ? R.string.dialog_title_modify_tunnel_section : R.string.dialog_title_modify_open_air_row;
            } else if (whichDelay == 3) {
                title = BaseApplication.isTunnel ? R.string.dialog_title_modify_tunnel_hole : R.string.dialog_title_modify_open_air_inside;
            }
            BaseApplication.customDialog(new AlertDialog.Builder(DetectActivity.this, R.style.AlertDialog)
                    .setTitle(title)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                        if (!etDelay.getText().toString().isEmpty()) {
                            String text = etDelay.getText().toString() + "ms";
                            if (whichDelay == 1) {
                                tvRowDelay.setText(text);
                                if (BaseApplication.isTunnel) {
                                    BaseApplication.settings.setSection(Integer.parseInt(etDelay.getText().toString()));
                                } else {
                                    BaseApplication.settings.setRow(Integer.parseInt(etDelay.getText().toString()));
                                }
                            } else if (whichDelay == 2) {
                                tvHoleDelay.setText(text);
                                BaseApplication.settings.setHole(Integer.parseInt(etDelay.getText().toString()));
                            } else {
                                tvInsideDelay.setText(text);
                                if (BaseApplication.isTunnel) {
                                    BaseApplication.settings.setSectionInside(Integer.parseInt(etDelay.getText().toString()));
                                } else {
                                    BaseApplication.settings.setHoleInside(Integer.parseInt(etDelay.getText().toString()));
                                }
                            }
                            myApp.saveSettings();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show(), false);
        });
    }

    private void enabledButton(boolean enable) {
        if (null != myReceiveListener)
            myReceiveListener.setStartAutoDetect(enable);
        setProgressVisibility(!enable);
        switch (insertMode) {
            case ConstantUtils.INSERT_INSIDE:
                if (BaseApplication.isTunnel)
                    btnNextHole.setEnabled(enable);
                else
                    btnNextHole.setEnabled(false);
                btnNextRow.setEnabled(false);
                break;
            case ConstantUtils.INSERT_HOLE:
                if (!BaseApplication.isTunnel) {
                    btnNextHole.setEnabled(enable);
                    btnNextRow.setEnabled(false);
                    break;
                }
            default:
                btnNextHole.setEnabled(enable);
                btnNextRow.setEnabled(enable);
        }
        if (!BaseApplication.isTunnel)
            btnInside.setEnabled(enable);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        executeFunction(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void executeFunction(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                if (btnNextHole.isEnabled()) {
                    BaseApplication.writeFile(btnNextHole.getText().toString());
                    addMode = BaseApplication.isTunnel ? AddMode.INSIDE_SECTION : AddMode.NEXT_HOLE;
                    myHandler.sendEmptyMessage(DETECT_CONTINUE);
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (btnNextRow.isEnabled()) {
                    BaseApplication.writeFile(btnNextRow.getText().toString());
                    addMode = BaseApplication.isTunnel ? AddMode.NEXT_SECTION : AddMode.NEXT_ROW;
                    myHandler.sendEmptyMessage(DETECT_CONTINUE);
                }
                break;
            case KeyEvent.KEYCODE_3:
                if (btnInside.isEnabled() && !BaseApplication.isTunnel) {
                    BaseApplication.writeFile(btnInside.getText().toString());
                    addMode = AddMode.INSIDE_HOLE;
                    myHandler.sendEmptyMessage(DETECT_CONTINUE);
                }
                break;
            case KeyEvent.KEYCODE_4:
                rbScan.setChecked(true);
                break;
            case KeyEvent.KEYCODE_5:
                rbRegister.setChecked(true);
                break;
            case KeyEvent.KEYCODE_POUND:
                tvInsideDelay.callOnClick();
                break;
            case KeyEvent.KEYCODE_0:
                if (!BaseApplication.isTunnel)
                    tvHoleDelay.callOnClick();
                break;
            case KeyEvent.KEYCODE_STAR:
                tvRowDelay.callOnClick();
                break;
        }
    }

    private void saveData(DetonatorBean bean) {
        int i = list.indexOf(bean);
        if (i >= 0) list.remove(i);
//        myApp.myToast(this, "index:" + insertIndex + ",size" + oldList.size());
        if (insertIndex <= list.size() - 1 && insertMode != 0) {
            int period;
            if (insertMode == ConstantUtils.INSERT_INSIDE) {
                period = BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHoleInside();
            } else {
                period = BaseApplication.isTunnel ? BaseApplication.settings.getSection() : BaseApplication.settings.getHole();
            }

            for (i = insertIndex; i < list.size(); i++) {
                DetonatorBean b = list.get(i);
                if (bean.getRow() != b.getRow() || (insertMode == ConstantUtils.INSERT_INSIDE && bean.getHole() != b.getHole()))
                    break;
                b.setDownloaded(false);
                b.setDelayTime(b.getDelayTime() + period);
                if (insertMode == ConstantUtils.INSERT_HOLE && bean.getRow() == b.getRow()) {
                    b.setHole(b.getHole() + 1);
                } else if (insertMode == ConstantUtils.INSERT_INSIDE && bean.getRow() == b.getRow() && bean.getHole() == b.getHole()) {
                    b.setInside(b.getInside() + 1);
                }
            }
            list.add(insertIndex, bean);
        } else {
            list.add(bean);
        }
        BaseApplication.writeFile(bean.toString());
        insertIndex++;
        DbUtil.updateDetonatorList(list);
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
            soundFail = soundPool.load(this, R.raw.fail, 1);
        }
    }

    @Override
    public void onDestroy() {
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.release();
            soundPool = null;
        }
        myHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private enum AddMode {
        NONE,                         //无效
        NEXT_HOLE,                   //下一孔
        NEXT_ROW,                    //下一排
        INSIDE_HOLE,                //当前孔
        INSIDE_SECTION,            //当前段
        NEXT_SECTION               //下一段
    }
}
