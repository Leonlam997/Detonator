package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class DetonatorListActivity extends BaseActivity {
    private final int MODE_SCHEME = 1;
    private final int MODE_HISTORY = 5;
    private List<DetonatorBean> list;
    private MyButton btnModify;
    private MyButton btnDelete;
    private ListView listView;
    private DetonatorListAdapter adapter;
    private SerialPortUtil serialPortUtil;
    private DataReceiveListener myReceiveListener;
    private SchemeBean schemeBean;
    private SoundPool soundPool;
    private BaseApplication myApp;
    private EditText etStart;
    private long lastKeyDownTime;
    private long schemeId;
    private int soundSuccess;
    private int soundFail;
    private final Handler myHandler = new Handler(msg -> {
        int HANDLER_SCAN_CODE = 1;
        if (msg.what == HANDLER_SCAN_CODE) {
            msg.getTarget().removeMessages(HANDLER_SCAN_CODE);
            myApp.playSoundVibrate(soundPool, soundFail);
            myApp.myToast(DetonatorListActivity.this, R.string.message_scan_timeout);
        } else {
            byte[] received = (byte[]) msg.obj;
            if (received != null && received.length > 0)
                if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                    myApp.shortCircuit(DetonatorListActivity.this, msg.getTarget());
                } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                    myApp.myToast(DetonatorListActivity.this, R.string.message_open_module_fail);
                    finish();
                } else if (received.length >= SerialCommand.CODE_CHAR_AT + 15) {
                    msg.getTarget().removeMessages(HANDLER_SCAN_CODE);
                    if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                        String tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                        if (Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                            myApp.playSoundVibrate(soundPool, soundSuccess);
                            if (etStart == null) {
                                int index = list.indexOf(new DetonatorBean(tempAddress));
                                if (index >= 0 && index < list.size())
                                    listView.setSelection(index);
                                else
                                    myApp.myToast(DetonatorListActivity.this, String.format(getString(R.string.message_detonator_not_found), tempAddress));
                            } else
                                etStart.setText(tempAddress);
                        }
                    }
                }
        }
        return false;
    });
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode()) {
            list = DbUtil.getDetonatorList(DetonatorListActivity.this, schemeId);
            checkButton();
            adapter.updateList(list);
        } else if (RESULT_CANCELED == result.getResultCode() && null != result.getData()
                && (ConstantUtils.ERROR_RESULT_SHORT_CIRCUIT == result.getData().getIntExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_OPEN_FAIL)))
            myApp.shortCircuit(DetonatorListActivity.this, DetonatorListActivity.this.myHandler);
    });
    private int keyMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonator_list);

        myApp = (BaseApplication) getApplication();
        int title = getIntent().getIntExtra(KeyUtils.KEY_DETONATOR_LIST, ConstantUtils.RESUME_LIST);
        schemeId = getIntent().getLongExtra(KeyUtils.KEY_TABLE_ID, -1);
        list = DbUtil.getDetonatorList(DetonatorListActivity.this, schemeId);
        if (schemeId != -1)
            schemeBean = DbUtil.getScheme(DetonatorListActivity.this, schemeId);
        else
            schemeBean = new SchemeBean();
        initSound();
        listView = findViewById(R.id.lv_delay_list);
        keyMode = 0;
        adapter = new DetonatorListAdapter(this, list);
        int titleID = 0;
        switch (title) {
            case ConstantUtils.RESUME_LIST:
                titleID = R.string.detonator_list;
                findViewById(R.id.rl_scan_view).setVisibility(View.VISIBLE);
                findViewById(R.id.ll_info).setVisibility(View.GONE);
                setTitle(titleID, R.string.menu_send_list);
                setSubtitleClickListener(view -> startActivity(new Intent(DetonatorListActivity.this, SendSchemeActivity.class)));
                break;
            case ConstantUtils.MODIFY_LIST:
                adapter.setCanSelect(true);
                titleID = R.string.check_schedule;
                findViewById(R.id.rl_scan_view).setVisibility(View.VISIBLE);
                findViewById(R.id.ll_info).setVisibility(View.GONE);
                break;
            case ConstantUtils.HISTORY_LIST:
                titleID = R.string.detail_schedule;
                findViewById(R.id.rl_scan_view).setVisibility(View.GONE);
                findViewById(R.id.ll_info).setVisibility(View.VISIBLE);
                break;
            case ConstantUtils.AUTHORIZED_LIST:
                titleID = R.string.detail_auth;
                findViewById(R.id.rl_scan_view).setVisibility(View.GONE);
                findViewById(R.id.ll_info).setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
        if (ConstantUtils.RESUME_LIST != title)
            setTitle(titleID);
        listView.setAdapter(adapter);
        switch (title) {
            case ConstantUtils.RESUME_LIST:
                keyMode = MODE_SCHEME;
                findViewById(R.id.btn_add).setOnClickListener(v -> scanDetonator());
                findViewById(R.id.btn_add).setOnLongClickListener(v -> {
                    manualAppend();
                    return true;
                });
                btnModify = findViewById(R.id.btn_modify);
                btnModify.setOnClickListener(view -> modifyBatch());
                btnDelete = findViewById(R.id.btn_delete);
                btnDelete.setOnClickListener(view -> deleteDetonator());
                listView.setOnItemClickListener((adapterView, view, i, l) -> modifyDetonator(i));
                checkButton();
                break;
            case ConstantUtils.HISTORY_LIST:
                keyMode = MODE_HISTORY;
                String text = String.format(Locale.getDefault(), "%.4f", getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                ((TextView) findViewById(R.id.tv_info1)).setText(R.string.map_longitude);
                ((TextView) findViewById(R.id.tv_info1_detail)).setText(text);
                text = String.format(Locale.getDefault(), "%.4f", getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                ((TextView) findViewById(R.id.tv_info2)).setText(R.string.map_latitude);
                ((TextView) findViewById(R.id.tv_info2_detail)).setText(text);
                findViewById(R.id.btn_restore).setOnClickListener(v -> restoreList());
                break;
            case ConstantUtils.AUTHORIZED_LIST:
                ((TextView) findViewById(R.id.tv_info1)).setText(R.string.detonator_total);
                ((TextView) findViewById(R.id.tv_info1_detail)).setText(String.format(Locale.getDefault(), getString(R.string.detonator_amount), list.size()));
                ((TextView) findViewById(R.id.tv_info2)).setText(R.string.detonator_used);
                ((TextView) findViewById(R.id.tv_info2_detail)).setText(String.format(Locale.getDefault(), getString(R.string.detonator_amount), 0));
                break;
            default:
                break;
        }
        listView.requestFocus();
    }

    private void checkButton() {
        btnModify.setEnabled(list.size() > 0);
        btnDelete.setEnabled(list.size() > 0);
    }

    private void modifyDetonator(int i) {
        runOnUiThread(() -> {
            final View modifyView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_edit_detonator, listView, false);
            final EditText etDelay = modifyView.findViewById(R.id.et_delay);
            final EditText etRow = modifyView.findViewById(R.id.et_row);
            final EditText etHole = modifyView.findViewById(R.id.et_hole);
            final EditText etInside = modifyView.findViewById(R.id.et_inside);
            etDelay.setText(String.format(Locale.getDefault(), "%d", list.get(i).getDelayTime()));
            etRow.setText(String.format(Locale.getDefault(), "%d", list.get(i).getRow()));
            etHole.setText(String.format(Locale.getDefault(), "%d", list.get(i).getHole()));
            etInside.setText(String.format(Locale.getDefault(), "%d", list.get(i).getInside()));
            etDelay.requestFocus();
            ((TextView) modifyView.findViewById(R.id.tv_sn)).setText(list.get(i).getAddress());
            if (BaseApplication.settings.isTunnel()) {
                modifyView.findViewById(R.id.ll_row).setVisibility(View.GONE);
                ((TextView) modifyView.findViewById(R.id.tv_hole)).setText(R.string.text_section_num);
                ((TextView) modifyView.findViewById(R.id.tv_inside)).setText(R.string.text_section_inside_num);
            }
            etDelay.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (!charSequence.toString().isEmpty())
                        try {
                            int num = Integer.parseInt(charSequence.toString());
                            if (num < 0 || num > ConstantUtils.MAX_DELAY_TIME)
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                        } catch (Exception e) {
                            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                        }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (!charSequence.toString().isEmpty())
                        try {
                            int num = Integer.parseInt(charSequence.toString());
                            if (num <= 0 || num > ConstantUtils.MAX_ROW_NUMBER)
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                        } catch (Exception e) {
                            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                        }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            };
            etRow.addTextChangedListener(watcher);
            etHole.addTextChangedListener(watcher);
            etInside.addTextChangedListener(watcher);
            BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_modify_detonator)
                    .setView(modifyView)
                    .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                        int delay;
                        try {
                            delay = Integer.parseInt(etDelay.getText().toString());
                            if (delay < 0 || delay > ConstantUtils.MAX_DELAY_TIME) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                return;
                            }
                        } catch (Exception e) {
                            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                            return;
                        }
                        try {
                            int hole = Integer.parseInt(etHole.getText().toString());
                            int inside = Integer.parseInt(etInside.getText().toString());
                            int row = 1;
                            if (!BaseApplication.settings.isTunnel()) {
                                row = Integer.parseInt(etRow.getText().toString());
                                if (row <= 0 || row > ConstantUtils.MAX_ROW_NUMBER
                                        || hole <= 0 || hole > ConstantUtils.MAX_ROW_NUMBER || inside <= 0 || inside > ConstantUtils.MAX_ROW_NUMBER) {
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                                    return;
                                }
                            }
                            boolean changed = list.get(i).getRow() != row || list.get(i).getHole() != hole || list.get(i).getInside() != inside;
                            if (list.get(i).getDelayTime() != delay || changed) {
                                list.get(i).setDelayTime(delay);
                                if (changed) {
                                    list.get(i).setRow(row);
                                    list.get(i).setHole(hole);
                                    list.get(i).setInside(inside);
                                    Collections.sort(list);
                                    DbUtil.updateDetonatorList(DetonatorListActivity.this, list);
                                } else
                                    DbUtil.updateDetonator(DetonatorListActivity.this, list.get(i));
                                adapter.updateList(list);
                            }
                        } catch (Exception e) {
                            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show());
        });
    }

    private void modifyBatch() {
        if (btnModify.isEnabled())
            runOnUiThread(() -> {
                final View modifyView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_batch_modify, listView, false);
                final EditText etFrom = modifyView.findViewById(R.id.et_from);
                final EditText etTo = modifyView.findViewById(R.id.et_to);
                final EditText etDelay = modifyView.findViewById(R.id.et_delay);
                final EditText etRow = modifyView.findViewById(R.id.et_row);
                final EditText etHole = modifyView.findViewById(R.id.et_hole);
                final EditText etInside = modifyView.findViewById(R.id.et_inside);
                final CheckBox cbSelectAll = modifyView.findViewById(R.id.cb_selected_all);
                final CheckBox cbDelay = modifyView.findViewById(R.id.cb_delay);
                final CheckBox cbDelayIncrease = modifyView.findViewById(R.id.cb_delay_increase);
                final CheckBox cbRow = modifyView.findViewById(R.id.cb_row);
                final CheckBox cbRowIncrease = modifyView.findViewById(R.id.cb_row_increase);
                final CheckBox cbHole = modifyView.findViewById(R.id.cb_hole);
                final CheckBox cbHoleIncrease = modifyView.findViewById(R.id.cb_hole_increase);
                final CheckBox cbInside = modifyView.findViewById(R.id.cb_inside);
                final CheckBox cbInsideIncrease = modifyView.findViewById(R.id.cb_inside_increase);
                final TextView tvDelay = modifyView.findViewById(R.id.tv_delay);
                final TextView tvRow = modifyView.findViewById(R.id.tv_row);
                final TextView tvHole = modifyView.findViewById(R.id.tv_hole);
                final TextView tvInside = modifyView.findViewById(R.id.tv_inside);
                if (BaseApplication.settings.isTunnel()) {
                    modifyView.findViewById(R.id.ll_row).setVisibility(View.GONE);
                    tvHole.setText(R.string.text_section_num);
                    tvInside.setText(R.string.text_section_inside_num);
                }
                cbRowIncrease.setOnCheckedChangeListener((compoundButton, b) -> {
                    tvRow.setText(b ? R.string.text_start_row_num : R.string.text_row_num);
                });
                cbHoleIncrease.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (BaseApplication.settings.isTunnel())
                        tvHole.setText(b ? R.string.text_start_section_num : R.string.text_section_num);
                    else
                        tvHole.setText(b ? R.string.text_start_hole_num : R.string.text_hole_num);
                });
                cbInsideIncrease.setOnCheckedChangeListener((compoundButton, b) -> {
                    if (BaseApplication.settings.isTunnel())
                        tvInside.setText(b ? R.string.text_start_section_inside_num : R.string.text_section_inside_num);
                    else
                        tvInside.setText(b ? R.string.text_start_inside_num : R.string.text_inside_num);
                });
                cbDelayIncrease.setOnCheckedChangeListener((compoundButton, b) -> {
                    tvDelay.setText(b ? R.string.edit_interval : R.string.text_delay);
                });
                cbDelay.setOnCheckedChangeListener((compoundButton, b) -> {
                    etDelay.setEnabled(b);
                    cbDelayIncrease.setEnabled(b);
                    if (b)
                        etDelay.requestFocus();
                });
                cbRow.setOnCheckedChangeListener((compoundButton, b) -> {
                    etRow.setEnabled(b);
                    cbRowIncrease.setEnabled(b);
                    if (b)
                        etRow.requestFocus();
                });
                cbHole.setOnCheckedChangeListener((compoundButton, b) -> {
                    etHole.setEnabled(b);
                    cbHoleIncrease.setEnabled(b);
                    if (b)
                        etHole.requestFocus();
                });
                cbInside.setOnCheckedChangeListener((compoundButton, b) -> {
                    etInside.setEnabled(b);
                    cbInsideIncrease.setEnabled(b);
                    if (b)
                        etInside.requestFocus();
                });
                etRow.setEnabled(false);
                cbRowIncrease.setEnabled(false);
                etHole.setEnabled(false);
                cbHoleIncrease.setEnabled(false);
                etInside.setEnabled(false);
                cbInsideIncrease.setEnabled(false);
                TextWatcher watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (!charSequence.toString().isEmpty())
                            try {
                                int num = Integer.parseInt(charSequence.toString());
                                if (num <= 0 || num > ConstantUtils.MAX_ROW_NUMBER)
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                            }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                };
                etRow.addTextChangedListener(watcher);
                etHole.addTextChangedListener(watcher);
                etInside.addTextChangedListener(watcher);
                etFrom.requestFocus();
                cbSelectAll.setOnCheckedChangeListener((compoundButton, b) -> {
                    etFrom.setEnabled(!b);
                    etTo.setEnabled(!b);
                    if (b) {
                        etFrom.setText("1");
                        etTo.setText(String.format(Locale.getDefault(), "%d", list.size()));
                        etFrom.setTextColor(getColor(R.color.colorDisabledText));
                        etTo.setTextColor(getColor(R.color.colorDisabledText));
                        etDelay.requestFocus();
                        etDelay.setSelection(etDelay.getText().length());
                    } else {
                        etFrom.setTextColor(getColor(R.color.colorLabelText));
                        etTo.setTextColor(getColor(R.color.colorLabelText));
                        etTo.requestFocus();
                        etTo.setSelection(etTo.getText().length());
                    }
                });
                etDelay.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (!charSequence.toString().isEmpty())
                            try {
                                int num = Integer.parseInt(charSequence.toString());
                                if (num < 0 || num > ConstantUtils.MAX_DELAY_TIME)
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                            }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                TextWatcher watcher1 = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (!charSequence.toString().isEmpty())
                            try {
                                int num = Integer.parseInt(charSequence.toString());
                                if (num <= 0 || num > list.size())
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                };
                cbDelay.setChecked(true);
                etFrom.addTextChangedListener(watcher1);
                etTo.addTextChangedListener(watcher1);
                BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_batch_modify)
                        .setView(modifyView)
                        .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                            int from;
                            int to;
                            try {
                                from = Integer.parseInt(etFrom.getText().toString());
                                to = Integer.parseInt(etTo.getText().toString());
                                if (from <= 0 || from > list.size() || to <= 0 || to > list.size()) {
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    return;
                                } else if (cbDelayIncrease.isChecked() && to == from) {
                                    myApp.myToast(DetonatorListActivity.this, getString(R.string.message_interval_number_out_of_range));
                                    return;
                                }
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                return;
                            }
                            int delay = 0;
                            try {
                                if (cbDelay.isChecked()) {
                                    delay = Integer.parseInt(etDelay.getText().toString());
                                    if (delay < 0 || delay > ConstantUtils.MAX_DELAY_TIME
                                            || (cbDelayIncrease.isChecked() && list.get(from - 1).getDelayTime() + delay * Math.abs(to - from) > ConstantUtils.MAX_DELAY_TIME)) {
                                        myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                        return;
                                    }
                                }
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                return;
                            }
                            try {
                                int row = 0;
                                int hole = 0;
                                if (cbRow.isChecked()) {
                                    row = Integer.parseInt(etRow.getText().toString());
                                    if (row <= 0 || row > ConstantUtils.MAX_ROW_NUMBER
                                            || (cbRowIncrease.isChecked() && row + Math.abs(to - from) > ConstantUtils.MAX_ROW_NUMBER)) {
                                        myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                                        return;
                                    }
                                }
                                if (cbHole.isChecked()) {
                                    hole = Integer.parseInt(etHole.getText().toString());
                                    if (hole <= 0 || hole > ConstantUtils.MAX_ROW_NUMBER
                                            || (cbHoleIncrease.isChecked() && hole + Math.abs(to - from) > ConstantUtils.MAX_ROW_NUMBER)) {
                                        myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                                        return;
                                    }
                                }
                                if (cbInside.isChecked()) {
                                    int inside = Integer.parseInt(etInside.getText().toString());
                                    if (inside <= 0 || inside > ConstantUtils.MAX_ROW_NUMBER
                                            || (cbInsideIncrease.isChecked() && inside + Math.abs(to - from) > ConstantUtils.MAX_ROW_NUMBER)) {
                                        myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                                        return;
                                    }
                                    if (cbInsideIncrease.isChecked() && to != from)
                                        if (from < to)
                                            for (int i = from - 1; i < to; i++)
                                                list.get(i).setInside(inside + i - from + 1);
                                        else
                                            for (int i = from - 1; i >= to - 1; i--)
                                                list.get(i).setInside(inside + from - 1 - i);
                                    else
                                        for (int i = from - 1; i < to; i++)
                                            list.get(i).setInside(inside);
                                }
                                if (cbDelay.isChecked())
                                    if (cbDelayIncrease.isChecked())
                                        if (from < to)
                                            for (int i = from; i < to; i++)
                                                list.get(i).setDelayTime(list.get(from - 1).getDelayTime() + delay * (i - from + 1));
                                        else
                                            for (int i = from - 2; i >= to - 1; i--)
                                                list.get(i).setDelayTime(list.get(from - 1).getDelayTime() + delay * (from - 1 - i));
                                    else
                                        for (int i = from - 1; i < to; i++)
                                            list.get(i).setDelayTime(delay);
                                if (cbRow.isChecked())
                                    if (cbRowIncrease.isChecked() && to != from)
                                        if (from < to)
                                            for (int i = from - 1; i < to; i++)
                                                list.get(i).setRow(row + i - from + 1);
                                        else
                                            for (int i = from - 1; i >= to - 1; i--)
                                                list.get(i).setRow(row + from - 1 - i);
                                    else
                                        for (int i = from - 1; i < to; i++)
                                            list.get(i).setRow(row);
                                if (cbHole.isChecked())
                                    if (cbHoleIncrease.isChecked() && to != from)
                                        if (from < to)
                                            for (int i = from - 1; i < to; i++)
                                                list.get(i).setHole(hole + i - from + 1);
                                        else
                                            for (int i = from - 1; i >= to - 1; i--)
                                                list.get(i).setHole(hole + from - 1 - i);
                                    else
                                        for (int i = from - 1; i < to; i++)
                                            list.get(i).setHole(hole);
                                if (cbRow.isChecked() || cbHole.isChecked() || cbInside.isChecked())
                                    Collections.sort(list);
                                if (cbDelay.isChecked() || cbRow.isChecked() || cbHole.isChecked() || cbInside.isChecked()) {
                                    adapter.updateList(list);
                                    DbUtil.updateDetonatorList(DetonatorListActivity.this, list);
                                }
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_detonator_input_out_of_range), ConstantUtils.MAX_ROW_NUMBER));
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .show());
            });
    }

    private void deleteDetonator() {
        if (btnDelete.isEnabled())
            runOnUiThread(() -> {
                final View deleteView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_batch_modify, listView, false);
                final EditText etFrom = deleteView.findViewById(R.id.et_from);
                final EditText etTo = deleteView.findViewById(R.id.et_to);
                final CheckBox cbSelectAll = deleteView.findViewById(R.id.cb_selected_all);
                deleteView.findViewById(R.id.ll_delay).setVisibility(View.GONE);
                deleteView.findViewById(R.id.ll_row).setVisibility(View.GONE);
                deleteView.findViewById(R.id.ll_hole).setVisibility(View.GONE);
                deleteView.findViewById(R.id.ll_inside).setVisibility(View.GONE);
                etFrom.requestFocus();
                cbSelectAll.setOnCheckedChangeListener((compoundButton, b) -> {
                    etFrom.setEnabled(!b);
                    etTo.setEnabled(!b);
                    if (b) {
                        etFrom.setText("1");
                        etTo.setText(String.format(Locale.getDefault(), "%d", list.size()));
                        etFrom.setTextColor(getColor(R.color.colorDisabledText));
                        etTo.setTextColor(getColor(R.color.colorDisabledText));
                    } else {
                        etTo.requestFocus();
                        etTo.setSelection(etTo.getText().length());
                        etFrom.setTextColor(getColor(R.color.colorLabelText));
                        etTo.setTextColor(getColor(R.color.colorLabelText));
                    }
                });
                TextWatcher watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (!charSequence.toString().isEmpty())
                            try {
                                int num = Integer.parseInt(charSequence.toString());
                                if (num <= 0 || num > list.size())
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                };
                etFrom.addTextChangedListener(watcher);
                etTo.addTextChangedListener(watcher);
                BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_batch_delete)
                        .setView(deleteView)
                        .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                            try {
                                if (etTo.getText().toString().isEmpty())
                                    etTo.setText(etFrom.getText());
                                int from = Integer.parseInt(etFrom.getText().toString());
                                int to = Integer.parseInt(etTo.getText().toString());
                                if (from <= 0 || from > list.size() || to <= 0 || to > list.size() || to < from) {
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    return;
                                }
                                for (int i = 0; i < list.size(); i++)
                                    list.get(i).setSelected(i >= from - 1 && i < to);
                                runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                                        .setTitle(R.string.dialog_title_delete_detonator)
                                        .setMessage(R.string.dialog_confirm_delete_detonator)
                                        .setPositiveButton(R.string.button_confirm, (dialog1, which) -> {
                                            list.removeIf(DetonatorBean::isSelected);
                                            DbUtil.updateScheme(DetonatorListActivity.this, schemeBean);
                                            if (list.size() == 0)
                                                DbUtil.deleteDetonatorTable(DetonatorListActivity.this, schemeBean.getId());
                                            else
                                                DbUtil.updateDetonatorList(DetonatorListActivity.this, list);
                                            adapter.updateList(list);
                                            checkButton();
                                        })
                                        .setNegativeButton(R.string.button_cancel, null)
                                        .show()));

                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .show());
            });
    }

    private void initSerial() {
        try {
            serialPortUtil = SerialPortUtil.getInstance(this);
            myReceiveListener = DataReceiveListener.getInstance(this, myHandler);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            myReceiveListener.setStartAutoDetect(false);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void restoreList() {
        SchemeBean bean = DbUtil.getScheme(DetonatorListActivity.this, schemeId);
        bean.setCreateTime(new Date());
        bean.setId(-1);
        DbUtil.updateScheme(DetonatorListActivity.this, bean);
        for (DetonatorBean b : list)
            b.setSchemeId(bean.getId());
        DbUtil.updateDetonatorList(DetonatorListActivity.this, list);
        myApp.myToast(DetonatorListActivity.this, R.string.message_restore_success);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyMode) {
                case MODE_SCHEME:
                    switch (event.getKeyCode()) {
                        case ConstantUtils.KEYCODE_ADD:
                            if (System.currentTimeMillis() - lastKeyDownTime > ConstantUtils.LONG_PRESS_TIME)
                                manualAppend();
                            else
                                scanDetonator();
                            lastKeyDownTime = 0;
                            return true;
                        case KeyEvent.KEYCODE_TAB:
                            modifyBatch();
                            return true;
                        case ConstantUtils.KEYCODE_SUB:
                            deleteDetonator();
                            return true;
                        case KeyEvent.KEYCODE_2:
                            listView.requestFocus();
                            if (listView.getSelectedItemPosition() > 0) {
                                listView.setSelection(listView.getSelectedItemPosition() - 1);
                            } else
                                listView.setSelection(list.size() - 1);
                            return true;
                        case KeyEvent.KEYCODE_8:
                            listView.requestFocus();
                            if (listView.getSelectedItemPosition() < list.size() - 1) {
                                listView.setSelection(listView.getSelectedItemPosition() + 1);
                            } else
                                listView.setSelection(0);
                            return true;
                        case KeyEvent.KEYCODE_MENU:
                            startActivity(new Intent(DetonatorListActivity.this, SendSchemeActivity.class));
                            return true;
                        case ConstantUtils.KEYCODE_CENTER_SCAN:
                        case ConstantUtils.KEYCODE_LEFT_SCAN:
                        case ConstantUtils.KEYCODE_RIGHT_SCAN:
                            BaseApplication.writeFile(getString(R.string.button_scan));
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                            return true;
                    }
                    break;
                case MODE_HISTORY:
                    if (KeyEvent.KEYCODE_1 == event.getKeyCode())
                        restoreList();
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (lastKeyDownTime == 0 && event.getKeyCode() == ConstantUtils.KEYCODE_ADD) {
                lastKeyDownTime = System.currentTimeMillis();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void manualAppend() {
        final View inputCodeView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_add_detonator, listView, false);
        etStart = inputCodeView.findViewById(R.id.et_start);
        final EditText etAmount = inputCodeView.findViewById(R.id.et_amount);
        inputCodeView.findViewById(R.id.cb_import).setVisibility(View.GONE);
        etStart.setKeyListener(new NumberKeyListener() {
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
        etAmount.setHint("1");
        BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_manual_input)
                .setView(inputCodeView)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, ii) -> {
                    if (Pattern.matches(ConstantUtils.SHELL_PATTERN, etStart.getText().toString().toUpperCase())) {
                        etStart.setText(etStart.getText().toString().toUpperCase());
                        for (int j = 0, k = etAmount.getText().toString().trim().length() < 1 ? 1 : Integer.parseInt(etAmount.getText().toString()); j < k; j++) {
                            String det = etStart.getText().toString().substring(0, 8) + String.format(Locale.getDefault(), "%05d", (Long.parseLong(etStart.getText().toString().substring(8)) + j));
                            int i = list.indexOf(new DetonatorBean(det));
                            if (i >= 0) {
                                myApp.myToast(DetonatorListActivity.this, det +
                                        String.format(Locale.getDefault(), getString(R.string.message_detonator_exist), i + 1));
                                return;
                            }
                        }
                        BaseApplication.writeFile(getString(R.string.dialog_title_manual_input) + ", " + etStart.getText() + ", " + etAmount.getText());
                        int delayTime = 0, hole, inside, row;
                        if (list.size() > 0) {
                            row = list.get(list.size() - 1).getRow();
                            if (BaseApplication.settings.isTunnel()) {
                                inside = list.get(list.size() - 1).getInside() + 1;
                                hole = list.get(list.size() - 1).getHole();
                                delayTime = list.get(list.size() - 1).getDelayTime() + BaseApplication.settings.getSectionInside();
                            } else {
                                inside = 1;
                                hole = list.get(list.size() - 1).getHole() + 1;
                                for (int i = list.size() - 1; i > 0; i--)
                                    if (list.get(i).getInside() == 1) {
                                        delayTime = list.get(i).getDelayTime() + BaseApplication.settings.getHole();
                                        break;
                                    }
                            }
                        } else {
                            row = 1;
                            hole = 1;
                            inside = 1;
                        }
                        for (int j = 0, k = etAmount.getText().toString().trim().length() < 1 ? 1 : Integer.parseInt(etAmount.getText().toString()); j < k; j++)
                            list.add(new DetonatorBean(schemeId, (etStart.getText().toString().substring(0, 8).toUpperCase() + String.format(Locale.getDefault(), "%05d", (Long.parseLong(etStart.getText().toString().substring(8)) + j))),
                                    Math.min(delayTime + j * (BaseApplication.settings.isTunnel() ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHole()), ConstantUtils.MAX_DELAY_TIME),
                                    row, (BaseApplication.settings.isTunnel() ? hole : j + hole), (BaseApplication.settings.isTunnel() ? j + inside : inside), false));
                        DbUtil.updateScheme(DetonatorListActivity.this, schemeBean);
                        DbUtil.updateDetonatorList(DetonatorListActivity.this, list);
                        checkButton();
                        adapter.updateList(list);
                    } else
                        myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_input_error);
                })
                .setOnDismissListener(dialogInterface -> etStart = null)
                .setNegativeButton(R.string.button_cancel, null)
                .show());
    }

    private void scanDetonator() {
        Intent intent = new Intent();
        int lastRow = 0, lastHole = 0, lastInside = 0, lastDelay = -1;
        if (list.size() > 0) {
            lastRow = list.get(list.size() - 1).getRow();
            lastHole = list.get(list.size() - 1).getHole();
            lastInside = list.get(list.size() - 1).getInside();
            lastDelay = list.get(list.size() - 1).getDelayTime();
        }
        intent.putExtra(KeyUtils.KEY_LAST_ROW, lastRow);
        intent.putExtra(KeyUtils.KEY_LAST_HOLE, lastHole);
        intent.putExtra(KeyUtils.KEY_LAST_INSIDE, lastInside);
        intent.putExtra(KeyUtils.KEY_LAST_DELAY, lastDelay);
        intent.putExtra(KeyUtils.KEY_TABLE_ID, schemeId);
        intent.setClass(DetonatorListActivity.this, DetectActivity.class);
        BaseApplication.writeFile(getString(R.string.button_add));
        launcher.launch(intent);
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
            soundFail = soundPool.load(this, R.raw.fail, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        listView.post(() -> listView.requestFocus());
        initSerial();
    }

    @Override
    protected void onDestroy() {
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.release();
            soundPool = null;
        }
        myHandler.removeCallbacksAndMessages(null);
        if (myReceiveListener != null)
            myReceiveListener.closeAllHandler();
        super.onDestroy();
    }
}
