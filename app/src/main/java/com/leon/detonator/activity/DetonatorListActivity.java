package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class DetonatorListActivity extends BaseActivity {
    private DataReceiveListener myReceiveListener;
    private SerialPortUtil serialPortUtil;
    private DetonatorListAdapter adapter;
    private List<DetonatorBean> list;
    private SoundPool soundPool;
    private PopupWindow popupMenu;
    private ListView listView;
    private MyButton btnModify;
    private MyButton btnDelete;
    private MyButton btnAdd;
    private EditText etStart;
    private boolean insertUp;
    private boolean inputCode;
    private long schemeId;
    private long lastKeyTime;
    private long lastClickTime;
    private int soundSuccess;
    private int soundFail;
    private int clickIndex;
    private int lastTouchX;
    private int insertMode;
    private int keyMode;
    private final Handler myHandler = new Handler(msg -> {
        if (msg.what == 1) {
            msg.getTarget().removeMessages(1);
            myApp.playSoundVibrate(soundPool, soundFail);
            myApp.myToast(DetonatorListActivity.this, R.string.message_scan_timeout);
        } else {
            byte[] received = (byte[]) msg.obj;
            if (received != null && received.length > 0)
                if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT)
                    myApp.shortCircuit(DetonatorListActivity.this, msg.getTarget());
                else if (received[0] == SerialCommand.INITIAL_FAIL) {
                    myApp.myToast(DetonatorListActivity.this, R.string.message_open_module_fail);
                    finish();
                } else if (received.length >= SerialCommand.CODE_CHAR_AT + 15) {
                    msg.getTarget().removeMessages(1);
                    if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                        String tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                        if (Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                            myApp.playSoundVibrate(soundPool, soundSuccess);
                            if (inputCode) {
                                etStart.setText(tempAddress);
                            } else {
                                int index = list.indexOf(new DetonatorBean(tempAddress));
                                if (index >= 0 && index < list.size())
                                    listView.setSelection(index);
                                else
                                    myApp.myToast(DetonatorListActivity.this, String.format(getString(R.string.message_detonator_not_found), tempAddress));
                            }
                            myReceiveListener.setStartAutoDetect(true);
                        }
                    }
                }
        }
        return false;
    });
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode()) {
            list = DbUtil.getDetonatorList(schemeId);
            adapter.updateList(list);
            btnModify.setEnabled(list.size() > 0);
            btnDelete.setEnabled(list.size() > 0);
        } else if (RESULT_CANCELED == result.getResultCode() && null != result.getData()
                && (ConstantUtils.ERROR_RESULT_SHORT_CIRCUIT == result.getData().getIntExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_OPEN_FAIL)))
            myApp.shortCircuit(DetonatorListActivity.this, myHandler);
    });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonator_list);
        int title = getIntent().getIntExtra(KeyUtils.KEY_CREATE_DELAY_LIST, ConstantUtils.RESUME_LIST);
        schemeId = getIntent().getLongExtra(KeyUtils.KEY_TABLE_ID, -1);
        list = DbUtil.getDetonatorList(schemeId);
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        initSound();
        listView = findViewById(R.id.lv_list);
        keyMode = 0;
        insertMode = 0;
        clickIndex = -1;
        adapter = new DetonatorListAdapter(this, list);
        if (BaseApplication.isTunnel) {
            findViewById(R.id.line_inside).setVisibility(View.GONE);
            findViewById(R.id.text_inside).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.text_row)).setText(R.string.table_section);
            ((TextView) findViewById(R.id.text_hole)).setText(R.string.table_section_inside);
        }
        int titleID = 0;
        switch (title) {
            case ConstantUtils.RESUME_LIST:
                titleID = R.string.scheme_detonator;
                findViewById(R.id.rl_modify_view).setVisibility(View.VISIBLE);
                findViewById(R.id.ll_info).setVisibility(View.INVISIBLE);
                break;
            case ConstantUtils.HISTORY_LIST:
                titleID = R.string.detail_record;
                findViewById(R.id.rl_modify_view).setVisibility(View.INVISIBLE);
                findViewById(R.id.ll_info).setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
        setTitle(titleID);
        listView.setAdapter(adapter);
        switch (title) {
            case ConstantUtils.RESUME_LIST:
                keyMode = 1;
                listView.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastTouchX = (int) event.getX();
                            break;
                        case MotionEvent.ACTION_UP:
                            listView.performClick();
                            break;
                    }
                    return false;
                });
                btnAdd = findViewById(R.id.btn_add_detonator);
                btnAdd.setOnClickListener(v -> scanDetonator());
                btnAdd.setOnLongClickListener(v -> {
                    manualAppend(false, listView);
                    return true;
                });
                btnModify = findViewById(R.id.btn_batch_modify);
                btnModify.setOnClickListener(v -> batchModify(false));
                btnDelete = findViewById(R.id.btn_batch_delete);
                btnDelete.setOnClickListener(v -> batchModify(true));
                btnModify.setEnabled(list.size() > 0);
                btnDelete.setEnabled(list.size() > 0);
                listView.setOnItemClickListener((adapterView, view, i, l) -> showPopupWindow(adapterView, view, i));
                btnAdd.requestFocus();
                break;
            case ConstantUtils.HISTORY_LIST:
                String text = String.format(Locale.getDefault(), "%.4f", getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                ((TextView) findViewById(R.id.tv_content1)).setText(text);
                text = String.format(Locale.getDefault(), "%.4f", getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                ((TextView) findViewById(R.id.tv_content2)).setText(text);
                break;
            default:
                break;
        }
    }

    private void initSerial() {
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = DataReceiveListener.getInstance(this, myHandler);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            myReceiveListener.setStartAutoDetect(true);
            myReceiveListener.setStartDetectShort(true);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void showPopupWindow(AdapterView<?> adapterView, View view, int position) {
        String[] menu;
        menu = new String[]{getString(R.string.menu_delete_detonator),
                getString(R.string.menu_modify),
                getString(BaseApplication.isTunnel ? R.string.menu_tunnel_section_insert : R.string.menu_open_air_hole_insert),
                getString(BaseApplication.isTunnel ? R.string.menu_tunnel_hole_insert : R.string.menu_open_air_inside_insert)};
        for (int i = 0; i < menu.length; i++)
            menu[i] = (i + 1) + "." + menu[i];
        keyMode = 2;
        View popupView = DetonatorListActivity.this.getLayoutInflater().inflate(R.layout.layout_popup_window, adapterView, false);
        popupView.findViewById(R.id.tvTitle).setVisibility(View.GONE);
        clickIndex = position;

        ListView lsvMenu = popupView.findViewById(R.id.lvPopupMenu);
        lsvMenu.setAdapter(new ArrayAdapter<>(DetonatorListActivity.this, R.layout.layout_item_popup_window, menu));
        lsvMenu.setOnItemClickListener((parent, view1, position1, id) -> launchMenu(position1, parent));
        lsvMenu.setOnKeyListener((v, keyCode, event) -> {
            launchMenu(keyCode - KeyEvent.KEYCODE_1, (ViewGroup) v.getRootView());
            return false;
        });
        popupMenu = new PopupWindow(popupView, 150 + BaseApplication.settings.getFontScale() * 10, 38 * menu.length);
        popupMenu.setAnimationStyle(R.style.popup_window_anim);
        popupMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupMenu.setFocusable(true);
        popupMenu.setOutsideTouchable(true);
        popupMenu.update();
        popupMenu.showAsDropDown(view, lastTouchX > 75 ? lastTouchX - 75 : 0, list.size() == 0 ? -145 : 0);
        popupMenu.setOnDismissListener(() -> keyMode = 1);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyMode == 1 && keyCode == KeyEvent.KEYCODE_1 && lastKeyTime == 0)
            lastKeyTime = System.currentTimeMillis();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyMode) {
            case 1:
                switch (keyCode) {
                    case KeyEvent.KEYCODE_1:
                        if (System.currentTimeMillis() - lastKeyTime > 1000)
                            manualAppend(false, listView);
                        else
                            scanDetonator();
                        lastKeyTime = 0;
                        break;
                    case KeyEvent.KEYCODE_2:
                        if (btnModify.isEnabled())
                            batchModify(false);
                        break;
                    case KeyEvent.KEYCODE_3:
                        if (btnDelete.isEnabled())
                            batchModify(true);
                        break;
                    case KeyEvent.KEYCODE_B:
                        inputCode = false;
                        myReceiveListener.setStartAutoDetect(false);
                        BaseApplication.writeFile(getString(R.string.button_scan));
                        serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                        myHandler.sendEmptyMessageDelayed(1, ConstantUtils.SCAN_CODE_TIME);
                        break;
                }
                break;
            case 2:
                launchMenu(keyCode - KeyEvent.KEYCODE_1, null);
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void batchModify(boolean delete) {
        final View modifyView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_batch_modify, listView, false);
        final EditText etFrom = modifyView.findViewById(R.id.et_from);
        final EditText etTo = modifyView.findViewById(R.id.et_to);
        final EditText etDelay = modifyView.findViewById(R.id.et_delay);
        final CheckBox cbSelectAll = modifyView.findViewById(R.id.cb_selected_all);
        final CheckBox cbDelayIncrease = modifyView.findViewById(R.id.cb_modify_interval);
        if (delete)
            modifyView.findViewById(R.id.ll_delay).setVisibility(View.GONE);
        else {
            final TextView tvDelay = modifyView.findViewById(R.id.tv_delay);
            cbDelayIncrease.setOnCheckedChangeListener((compoundButton, b) -> tvDelay.setText(b ? R.string.text_interval : R.string.text_delay));
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
        }
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etFrom.setEnabled(!isChecked);
            etTo.setEnabled(!isChecked);
            if (isChecked) {
                etFrom.setText("1");
                etTo.setText(String.format(Locale.getDefault(), "%d", list.size()));
                etFrom.setTextColor(getColor(R.color.colorHintText));
                etTo.setTextColor(getColor(R.color.colorHintText));
                etDelay.requestFocus();
                etDelay.setSelection(etDelay.getText().length());
            } else {
                etFrom.setTextColor(getColor(R.color.colorLabelText));
                etTo.setTextColor(getColor(R.color.colorLabelText));
                etTo.requestFocus();
                etTo.setSelection(etTo.getText().length());
            }
        });
        final TextWatcher watcher1 = new TextWatcher() {
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
        etFrom.addTextChangedListener(watcher1);
        etTo.addTextChangedListener(watcher1);
        BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(delete ? R.string.button_batch_delete : R.string.button_batch_modify)
                .setView(modifyView)
                .setCancelable(false)
                .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                    int from;
                    int to;
                    try {
                        if (etTo.getText().toString().isEmpty())
                            etTo.setText(etFrom.getText());
                        from = Integer.parseInt(etFrom.getText().toString());
                        to = Integer.parseInt(etTo.getText().toString());
                        if (from <= 0 || from > list.size() || to <= 0 || to > list.size()) {
                            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            return;
                        }
                    } catch (Exception e) {
                        myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                        return;
                    }
                    if (delete) {
                        if (to == from) {
                            clickIndex = to - 1;
                            deleteDetonators(true);
                        } else {
                            if (to < from) {
                                int i = to;
                                to = from;
                                from = i;
                            }
                            for (int i = 0; i < list.size(); i++)
                                list.get(i).setSelected(i >= from - 1 && i < to);
                            deleteDetonators(false);
                        }
                    } else {
                        try {
                            int delay = Integer.parseInt(etDelay.getText().toString());
                            if (delay < 0 || delay > ConstantUtils.MAX_DELAY_TIME
                                    || (cbDelayIncrease.isChecked() && list.get(from - 1).getDelayTime() + delay * Math.abs(to - from) > ConstantUtils.MAX_DELAY_TIME)) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                return;
                            }
                            if (cbDelayIncrease.isChecked())
                                if (from < to)
                                    for (int i = from; i < to; i++) {
                                        list.get(i).setDelayTime(list.get(from - 1).getDelayTime() + delay * (i - from + 1));
                                        list.get(i).setDownloaded(false);
                                    }
                                else
                                    for (int i = from - 2; i >= to - 1; i--) {
                                        list.get(i).setDelayTime(list.get(from - 1).getDelayTime() + delay * (from - 1 - i));
                                        list.get(i).setDownloaded(false);
                                    }
                            else
                                for (int i = Math.min(from, to) - 1; i < Math.max(from, to); i++) {
                                    list.get(i).setDelayTime(delay);
                                    list.get(i).setDownloaded(false);
                                }
                            DbUtil.updateDetonatorList(list);
                            adapter.updateList(list);
                        } catch (Exception e) {
                            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                        }
                    }
                }).
                setNegativeButton(R.string.button_cancel, null)
                .show(), false);
    }

    private void launchMenu(int which, ViewGroup viewGroup) {
        switch (which) {
            case 0:
                deleteDetonators(true);
                break;
            case 1:
                final View modifyView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_edit, viewGroup, false);
                final EditText etModifyDelay = modifyView.findViewById(R.id.et_dialog);
                etModifyDelay.setHint(R.string.hint_input_delay_time);
                etModifyDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                etModifyDelay.setInputType(InputType.TYPE_CLASS_NUMBER);
                BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_modify_delay)
                        .setView(modifyView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                            if (etModifyDelay.getText().toString().isEmpty()) {
                                myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_delay_input_error);
                            } else {
                                try {
                                    confirmDelay(Integer.parseInt(etModifyDelay.getText().toString()));
                                } catch (Exception e) {
                                    myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_delay_input_error);
                                }
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .show(), false);
                break;
            case 2:
            case 3:
                insertMode = which - 1;
                doInsert(viewGroup);
                break;
            default:
                return;
        }
        popupMenu.dismiss();
    }

    private void deleteDetonators(final boolean single) {
        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_delete_detonator)
                .setMessage(R.string.dialog_confirm_delete_detonator)
                .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                    if (single)
                        deleteDetonator(clickIndex);
                    else
                        for (int i = list.size() - 1; i >= 0; i--)
                            if (list.get(i).isSelected())
                                deleteDetonator(i);
                    adapter.updateList(list);
                    if (list.size() == 0)
                        DbUtil.deleteDetonatorTable(schemeId);
                    else
                        DbUtil.updateDetonatorList(list);
                    if (btnModify != null) {
                        btnModify.setEnabled(list.size() > 0);
                        btnDelete.setEnabled(list.size() > 0);
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show(), true));
    }

    private void deleteDetonator(int index) {
        boolean onlyRow = index >= list.size() - 1;
        boolean onlyHole;
        if (!onlyRow)
            onlyRow = list.get(index).getRow() != list.get(index + 1).getRow() && (index == 0 || list.get(index).getRow() != list.get(index - 1).getRow());
        onlyHole = !onlyRow && list.get(index).getHole() != list.get(index + 1).getHole()
                && (index == 0 || list.get(index - 1).getRow() != list.get(index).getRow() || list.get(index).getHole() != list.get(index - 1).getHole());
        for (int i = index + 1; i < list.size(); i++) {
            if (onlyRow) {
                if (list.get(i).getRow() > 1)
                    list.get(i).setRow(list.get(i).getRow() - 1);
            } else if (onlyHole && list.get(i).getRow() == list.get(index).getRow()) {
                if (list.get(i).getHole() > 1)
                    list.get(i).setHole(list.get(i).getHole() - 1);
            } else if (list.get(i).getRow() == list.get(index).getRow() && list.get(i).getHole() == list.get(index).getHole()) {
                if (list.get(i).getInside() > 1)
                    list.get(i).setInside(list.get(i).getInside() - 1);
            }
        }
        BaseApplication.writeFile(getString(R.string.dialog_title_delete_detonator) + index + ":" + list.get(index).toString());
        list.remove(index);
    }

    private void doInsert(final ViewGroup viewGroup) {
        final View insertView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_insert_mode, viewGroup, false);
        final RadioButton rbManuel = insertView.findViewById(R.id.rb_manual_line);
        final RadioButton rbUp = insertView.findViewById(R.id.rb_insert_up);
        BaseApplication.customDialog(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_select_mode)
                .setView(insertView)
                .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                    insertUp = rbUp.isChecked();
                    if (list.size() > 0 && clickIndex < list.size()) {
                        if (insertMode == ConstantUtils.INSERT_HOLE) {
                            boolean hasChanged = false;
                            if (insertUp) {
                                if (list.get(clickIndex).getInside() > 1 && clickIndex > 0) {
                                    for (int i = clickIndex - 1; i >= 0; i--)
                                        if (list.get(clickIndex).getHole() != list.get(i).getHole()) {
                                            clickIndex = i + 1;
                                            hasChanged = true;
                                            break;
                                        }
                                    if (!hasChanged)
                                        clickIndex = 0;
                                }
                            } else if (clickIndex != list.size() - 1) {
                                for (int i = clickIndex + 1; i < list.size(); i++)
                                    if (list.get(clickIndex).getHole() != list.get(i).getHole()) {
                                        clickIndex = i - 1;
                                        hasChanged = true;
                                        break;
                                    }
                                if (!hasChanged)
                                    clickIndex = list.size() - 1;
                            }
                        }
                        if (rbManuel.isChecked()) {
                            manualAppend(true, viewGroup);
                        } else {
                            int lastRow = 0, lastHole = 0, lastInside = 0, lastDelay = -1;
                            if (insertUp) {
                                if (clickIndex > 0) {
                                    lastRow = list.get(clickIndex).getRow();
                                    lastDelay = list.get(clickIndex - 1).getDelayTime();
                                    if (ConstantUtils.INSERT_INSIDE == insertMode) {
                                        lastHole = list.get(clickIndex).getHole();
                                        if (1 != list.get(clickIndex).getInside())
                                            lastInside = list.get(clickIndex - 1).getInside();
                                        else
                                            lastDelay = list.get(clickIndex).getDelayTime()
                                                    - (BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHoleInside());
                                    } else
                                        lastHole = list.get(clickIndex - 1).getHole();
                                }
                            } else {
                                lastRow = list.get(clickIndex).getRow();
                                lastHole = list.get(clickIndex).getHole();
                                lastInside = list.get(clickIndex).getInside();
                                lastDelay = list.get(clickIndex).getDelayTime();
                            }
                            Intent intent = new Intent();
                            intent.putExtra(KeyUtils.KEY_LAST_ROW, lastRow);
                            intent.putExtra(KeyUtils.KEY_LAST_HOLE, lastHole);
                            intent.putExtra(KeyUtils.KEY_LAST_INSIDE, lastInside);
                            intent.putExtra(KeyUtils.KEY_LAST_DELAY, lastDelay);
                            intent.putExtra(KeyUtils.KEY_INSERT_MODE, insertMode);
                            intent.putExtra(KeyUtils.KEY_INSERT_INDEX, clickIndex + (insertUp ? 0 : 1));
                            intent.putExtra(KeyUtils.KEY_TABLE_ID, schemeId);
                            intent.setClass(DetonatorListActivity.this, DetectActivity.class);
                            myReceiveListener.closeAllHandler();
                            launcher.launch(intent);
                        }
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show(), false);
    }

    private void manualAppend(final boolean insert, ViewGroup viewGroup) {
        final View inputCodeView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_dialog_add_detonator, viewGroup, false);
        etStart = inputCodeView.findViewById(R.id.et_start);
        final EditText etBox = inputCodeView.findViewById(R.id.et_box);
        final EditText etAmount = inputCodeView.findViewById(R.id.et_amount);
        final CheckBox cbMultiple = inputCodeView.findViewById(R.id.cb_multiple);
        final TextView tvStart = inputCodeView.findViewById(R.id.tv_start);
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
        cbMultiple.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            tvStart.setText(isChecked ? R.string.input_start_shell : R.string.text_shell_code);
            etBox.setEnabled(isChecked);
            etAmount.setEnabled(isChecked);
        }));
        etBox.setEnabled(false);
        etAmount.setEnabled(false);
        etAmount.setHint("1");
        etBox.setHint("10");
        BaseApplication.customDialogWithoutKey(new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_manual_input)
                .setView(inputCodeView)
                .setCancelable(false)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, ii) -> {
                    etStart.setText(etStart.getText().toString().toUpperCase());
                    if (Pattern.matches(ConstantUtils.SHELL_PATTERN, etStart.getText().toString())) {
                        int amount = !cbMultiple.isChecked() || etAmount.getText().toString().trim().length() < 1 ? 1 : Integer.parseInt(etAmount.getText().toString());
                        int box = cbMultiple.isChecked() ? (etBox.getText().toString().isEmpty() ? 10 : Integer.parseInt(etBox.getText().toString())) : 1;
                        if (box < 0 || box > 100) {
                            myApp.myToast(DetonatorListActivity.this, R.string.message_amount_in_box_out_of_range);
                            return;
                        }
                        List<String> addressList = new ArrayList<>();
                        for (int j = 0; j < amount; j++)
                            for (int k = 0; k < box; k++) {
                                String det = etStart.getText().toString().substring(0, 8) + String.format(Locale.getDefault(), "%05d", (Long.parseLong(etStart.getText().toString().substring(8)) + j * 100L + k) % 100000);
                                int i = list.indexOf(new DetonatorBean(det));
                                if (i >= 0) {
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_current_detonator_exist), det, i + 1));
                                    listView.smoothScrollToPosition(i);
                                    return;
                                }
                                addressList.add(det);
                            }
                        String scheme = DbUtil.checkDetonatorListExist(addressList, schemeId);
                        if (scheme != null) {
                            try {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_current_detonator_exist_other_scheme),
                                        addressList.get(Integer.parseInt(scheme.substring(1, 5))),
                                        getString(Integer.parseInt(scheme.substring(0, 1)) == 0 ? R.string.mode_open_air : R.string.mode_tunnel),
                                        scheme.substring(5)));
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            return;
                        }
                        BaseApplication.writeFile((insert ? clickIndex + ", " : "") + getString(insert ? (insertUp ? R.string.insert_up : R.string.insert_down) : R.string.button_manual)
                                + " " + etStart.getText().toString() + ", " + etAmount.getText().toString());
                        int delayTime = 0;
                        int hole = 0;
                        int inside = 0;
                        int row = 0;
                        if (insert) {
                            if (insertUp) {
                                if (clickIndex > 0) {
                                    row = list.get(clickIndex).getRow();
                                    delayTime = list.get(clickIndex - 1).getDelayTime();
                                    if (ConstantUtils.INSERT_INSIDE == insertMode) {
                                        hole = list.get(clickIndex).getHole();
                                        if (1 != list.get(clickIndex).getInside())
                                            inside = list.get(clickIndex - 1).getInside();
                                        else
                                            delayTime = list.get(clickIndex).getDelayTime()
                                                    - (BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHoleInside());

                                    } else
                                        hole = list.get(clickIndex - 1).getHole();
                                }
                            } else {
                                row = list.get(clickIndex).getRow();
                                hole = list.get(clickIndex).getHole();
                                inside = list.get(clickIndex).getInside();
                                delayTime = list.get(clickIndex).getDelayTime();
                            }
                            if ((insertMode == ConstantUtils.INSERT_HOLE && delayTime + (BaseApplication.isTunnel ? BaseApplication.settings.getSection() : BaseApplication.settings.getHole()) * amount > ConstantUtils.MAX_DELAY_TIME)
                                    || (insertMode != ConstantUtils.INSERT_HOLE && delayTime + (BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHoleInside()) * amount > ConstantUtils.MAX_DELAY_TIME)) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                return;
                            }
                            int period = amount;
                            period *= insertMode == ConstantUtils.INSERT_INSIDE ? (BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHoleInside())
                                    : (BaseApplication.isTunnel ? BaseApplication.settings.getSection() : BaseApplication.settings.getHole());
                            for (int i = clickIndex + (insertUp ? 0 : 1); i < list.size(); i++)
                                if (list.get(i).getDelayTime() + period > ConstantUtils.MAX_DELAY_TIME) {
                                    myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                    return;
                                }
                            List<DetonatorBean> newList = new ArrayList<>();
                            for (int j = 0; j < amount; j++)
                                for (int k = 0; k < box; k++) {
                                    if (row == 0) {
                                        row = 1;
                                        hole = 1;
                                        inside = 1;
                                    } else if (insertMode == ConstantUtils.INSERT_HOLE) {
                                        hole++;
                                        inside = 1;
                                        delayTime += BaseApplication.isTunnel ? BaseApplication.settings.getSection() : BaseApplication.settings.getHole();
                                    } else {
                                        inside++;
                                        delayTime += BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHoleInside();
                                    }
                                    newList.add(new DetonatorBean(schemeId, etStart.getText().toString().substring(0, 8) + String.format(Locale.getDefault(), "%05d", (Long.parseLong(etStart.getText().toString().substring(8)) + j * 100L + k) % 100000),
                                            Math.min(delayTime, ConstantUtils.MAX_DELAY_TIME), row, hole, inside, false));
                                }
                            for (int i = clickIndex + (insertUp ? 0 : 1); i < list.size(); i++) {
                                DetonatorBean bean = list.get(i);
                                if (newList.get(0).getRow() != bean.getRow()
                                        || (insertMode == ConstantUtils.INSERT_INSIDE && newList.get(0).getHole() != bean.getHole()))
                                    break;
                                bean.setDownloaded(false);
                                bean.setDelayTime(bean.getDelayTime() + period);
                                if (insertMode == ConstantUtils.INSERT_HOLE) {
                                    bean.setHole(bean.getHole() + newList.size());
                                } else {
                                    bean.setInside(bean.getInside() + newList.size());
                                }
                            }
                            if (insertUp) {
                                if (clickIndex >= 0)
                                    list.addAll(clickIndex, newList);
                                else
                                    list.addAll(newList);
                            } else {
                                if (clickIndex == list.size() - 1)
                                    list.addAll(newList);
                                else
                                    list.addAll(clickIndex + 1, newList);
                            }
                        } else {
                            if (list.size() > 0) {
                                row = list.get(list.size() - 1).getRow();
                                if (BaseApplication.isTunnel) {
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
                            if (delayTime + (BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHole()) * (amount - 1) > ConstantUtils.MAX_DELAY_TIME) {
                                myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
                                return;
                            }
                            for (int j = 0; j < amount; j++)
                                for (int k = 0; k < box; k++)
                                    list.add(new DetonatorBean(schemeId, (etStart.getText().toString().substring(0, 8) + String.format(Locale.getDefault(), "%05d", (Long.parseLong(etStart.getText().toString().substring(8)) + j * 100L + k) % 100000)),
                                            delayTime + (j * box + k) * (BaseApplication.isTunnel ? BaseApplication.settings.getSectionInside() : BaseApplication.settings.getHole()),
                                            row, (BaseApplication.isTunnel ? hole : (j * box + k) + hole), (BaseApplication.isTunnel ? (j * box + k) + inside : inside), false));
                        }
                        btnModify.setEnabled(list.size() > 0);
                        btnDelete.setEnabled(list.size() > 0);
                        adapter.updateList(list);
                        DbUtil.updateDetonatorList(list);
                    } else
                        myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_input_error);
                })
                .setNegativeButton(R.string.button_cancel, null)
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_B || keyCode == KeyEvent.KEYCODE_R) {
                        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_B) {
                            inputCode = true;
                            myReceiveListener.setStartAutoDetect(false);
                            BaseApplication.writeFile(getString(R.string.dialog_title_manual_input) + ":" + getString(R.string.button_scan));
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                            myHandler.sendEmptyMessageDelayed(1, ConstantUtils.SCAN_CODE_TIME);
                        }
                        return true;
                    }
                    return false;
                })
                .show(), false);
    }

    private void confirmDelay(int time) {
        if (time < 0 || time > ConstantUtils.MAX_DELAY_TIME)
            myApp.myToast(DetonatorListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_range), ConstantUtils.MAX_DELAY_TIME));
        else {
            DetonatorBean bean = list.get(clickIndex);
            if (time != bean.getDelayTime()) {
                BaseApplication.writeFile(getString(R.string.dialog_title_modify_delay) + ", " + list.get(clickIndex).toString() + ": " + time);
                bean.setDownloaded(false);
                bean.setDelayTime(time);
                list.set(clickIndex, bean);
                adapter.updateList(list);
                DbUtil.updateDetonator(list.get(clickIndex));
            }
        }
    }

    private void scanDetonator() {
        if (System.currentTimeMillis() - lastClickTime > ConstantUtils.FAST_CLICK_DELAY_TIME) {
            lastClickTime = System.currentTimeMillis();
            if (btnAdd.isEnabled()) {
                Intent intent = new Intent();
                insertMode = 0;
                clickIndex = -1;
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
                BaseApplication.writeFile(getString(R.string.button_add_detonator));
                launcher.launch(intent);
            }
        }
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
