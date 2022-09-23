package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.EnterpriseProjectBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.R;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class DetonatorListActivity extends BaseActivity {
    private final int HANDLER_SCAN_CODE = 1;
    private final int MODE_SCAN = 1;
    private final int MODE_MODIFY = 2;
    private final int MODE_MENU = 3;
    private final int MODE_MENU2 = 4;
    private final int MODE_HISTORY = 5;
    private List<DetonatorInfoBean> list;
    private CheckBox cbSelected;
    private TextView tvRowDelay;
    private TextView tvHoleDelay;
    private TextView tvInsideDelay;
    private MyButton btnModifyDelay;
    private ListView tableListView;
    private DetonatorListAdapter adapter;
    private SerialPortUtil serialPortUtil;
    private int lastTouchX;
    private int keyMode;
    private int clickIndex;
    private int newTime;
    private int insertMode;
    private int soundFail;
    private int soundSuccess;
    private int soundAlert;
    private boolean insertUp;
    private SerialDataReceiveListener myReceiveListener;
    private PopupWindow popupMenu;
    private LocalSettingBean settings;
    private SoundPool soundPool;
    private BaseApplication myApp;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode()) {
            initData(ConstantUtils.RESUME_LIST);
            adapter.updateList(list);
        } else if (RESULT_CANCELED == result.getResultCode() && null != result.getData()
                && (ConstantUtils.ERROR_RESULT_SHORT_CIRCUIT == result.getData().getIntExtra(KeyUtils.KEY_ERROR_RESULT, ConstantUtils.ERROR_RESULT_OPEN_FAIL))) {
            new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_warning)
                    .setMessage(R.string.dialog_short_circuit)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                    .create().show();
            myApp.playSoundVibrate(soundPool, soundAlert);
        }
        if (myApp.isTunnel()) {
            settings = BaseApplication.readSettings();
            tvRowDelay.setText(String.format(Locale.CHINA, "%dms", settings.getSection()));
            tvInsideDelay.setText(String.format(Locale.CHINA, "%dms", settings.getSectionInside()));
        }
    });
    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] received = myReceiveListener.getRcvData();
            if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_short_circuit)
                            .setCancelable(false)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                            .create().show();
                    myApp.playSoundVibrate(soundPool, soundAlert);
                });
            } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                myApp.myToast(DetonatorListActivity.this, R.string.message_open_module_fail);
                finish();
            } else if (received.length >= SerialCommand.CODE_CHAR_AT + 15) {
                myHandler.removeMessages(HANDLER_SCAN_CODE);
                if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                    String tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                    if (Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress)) {
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                        int index = searchIndex(tempAddress);
                        if (index >= 0 && index < list.size())
                            runOnUiThread(() -> tableListView.setSelection(index));
                        else
                            myApp.myToast(DetonatorListActivity.this, String.format(getString(R.string.message_detonator_not_found), tempAddress));
                    }
                }
            }
        }
    };

    private int searchIndex(String address) {
        int i = 0;
        for (DetonatorInfoBean bean : list) {
            if (bean.getAddress().equals(address))
                return i;
            i++;
        }
        return -1;
    }

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (msg.what == HANDLER_SCAN_CODE) {
                myHandler.removeMessages(HANDLER_SCAN_CODE);
                myApp.playSoundVibrate(soundPool, soundAlert);
                myApp.myToast(DetonatorListActivity.this, R.string.message_scan_timeout);
                return true;
            }
            return false;
        }
    });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonator_list);

        myApp = (BaseApplication) getApplication();
        int title = getIntent().getIntExtra(KeyUtils.KEY_CREATE_DELAY_LIST, ConstantUtils.RESUME_LIST);
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        cbSelected = findViewById(R.id.cb_selected);
        cbSelected.setVisibility(View.GONE);
        findViewById(R.id.v_selected).setVisibility(View.GONE);
        findViewById(R.id.rl_cb).setVisibility(View.GONE);
        initData(title);
        initSound();
        tableListView = findViewById(R.id.lv_delaylist);
        keyMode = 0;
        insertMode = 0;
        clickIndex = -1;
        settings = BaseApplication.readSettings();

        adapter = new DetonatorListAdapter(this, list);
        adapter.setTunnel(myApp.isTunnel());

        ViewGroup table = findViewById(R.id.ll_table);
        final ViewGroup.LayoutParams params = table.getLayoutParams();
        if (myApp.isTunnel()) {
            findViewById(R.id.line_row).setVisibility(View.GONE);
            findViewById(R.id.text_row).setVisibility(View.GONE);
            findViewById(R.id.ll_hole_delay).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.text_hole)).setText(R.string.table_section);
            ((TextView) findViewById(R.id.text_inside)).setText(R.string.table_section_inside);
        }

        int titleID = 0;
        switch (title) {
            case ConstantUtils.RESUME_LIST:
                titleID = R.string.add_detonator;
                params.height = Dp2Px(this, 240);
                findViewById(R.id.rl_scan_view).setVisibility(View.VISIBLE);
                findViewById(R.id.rl_modify_view).setVisibility(View.GONE);
                findViewById(R.id.rl_disp_view).setVisibility(View.GONE);
                break;
            case ConstantUtils.MODIFY_LIST:
                adapter.setCanSelect(true);
                titleID = R.string.check_schedule;
                params.height = Dp2Px(this, 280);
                findViewById(R.id.rl_scan_view).setVisibility(View.GONE);
                findViewById(R.id.rl_modify_view).setVisibility(View.VISIBLE);
                findViewById(R.id.rl_disp_view).setVisibility(View.GONE);
                cbSelected.setVisibility(View.VISIBLE);
                findViewById(R.id.v_selected).setVisibility(View.VISIBLE);
                findViewById(R.id.rl_cb).setVisibility(View.VISIBLE);
                break;
            case ConstantUtils.HISTORY_LIST:
                titleID = R.string.detail_schedule;
                params.height = Dp2Px(this, 280);
                findViewById(R.id.rl_scan_view).setVisibility(View.GONE);
                findViewById(R.id.rl_modify_view).setVisibility(View.GONE);
                findViewById(R.id.rl_disp_view).setVisibility(View.VISIBLE);
                break;
            case ConstantUtils.AUTHORIZED_LIST:
                titleID = R.string.detail_auth;
                params.height = Dp2Px(this, 280);
                findViewById(R.id.rl_scan_view).setVisibility(View.GONE);
                findViewById(R.id.rl_modify_view).setVisibility(View.GONE);
                findViewById(R.id.rl_disp_view).setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
        table.setLayoutParams(params);

        setTitle(titleID);
        tableListView.setAdapter(adapter);

        switch (title) {
            case ConstantUtils.RESUME_LIST:
                keyMode = MODE_SCAN;
                tvRowDelay = findViewById(R.id.tv_row_delay);
                tvInsideDelay = findViewById(R.id.tv_inside_delay);
                if (myApp.isTunnel()) {
                    tvRowDelay.setText(String.format(Locale.CHINA, "%dms", settings.getSection()));
                    tvInsideDelay.setText(String.format(Locale.CHINA, "%dms", settings.getSectionInside()));
                    ((TextView) findViewById(R.id.txt_delay1)).setText(R.string.txt_section_delay);
                    ((TextView) findViewById(R.id.txt_delay3)).setText(R.string.txt_section_inside_delay);
                } else {
                    tvHoleDelay = findViewById(R.id.tv_hole_delay);
                    tvRowDelay.setText(String.format(Locale.CHINA, "%dms", settings.getRow()));
                    tvHoleDelay.setText(String.format(Locale.CHINA, "%dms", settings.getHole()));
                    tvInsideDelay.setText(String.format(Locale.CHINA, "%dms", settings.getHoleInside()));
                    tvHoleDelay.setOnClickListener(v -> modifyDelay(2));
                    findViewById(R.id.txt_delay2).setOnClickListener(v -> modifyDelay(2));
                }
                tvRowDelay.setOnClickListener(v -> modifyDelay(1));
                findViewById(R.id.txt_delay1).setOnClickListener(v -> modifyDelay(1));
                tvInsideDelay.setOnClickListener(v -> modifyDelay(3));
                findViewById(R.id.txt_delay3).setOnClickListener(v -> modifyDelay(3));
                findViewById(R.id.btn_scan).setOnClickListener(v -> scanDetonator(true));

                findViewById(R.id.btn_register).setOnClickListener(v -> scanDetonator(false));

                findViewById(R.id.btn_manual).setOnClickListener(v -> manualAppend(false, (ViewGroup) v.getRootView()));

                tableListView.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastTouchX = (int) event.getX();
                            break;
                        case MotionEvent.ACTION_UP:
                            tableListView.performClick();
                            break;
                    }
                    return false;
                });
                tableListView.setOnItemClickListener((adapterView, view, i, l) -> showPopupWindow(adapterView, view, i));
                break;
            case ConstantUtils.MODIFY_LIST:
                keyMode = MODE_MODIFY;
                boolean allSelected = true;
                for (DetonatorInfoBean item : list) {
                    if (!item.isDownloaded()) {
                        item.setSelected(true);
                    } else
                        allSelected = false;
                }
                cbSelected.setOnClickListener(v -> {
                    for (DetonatorInfoBean item : list) {
                        item.setSelected(cbSelected.isChecked());
                    }
                    adapter.updateList(list);
                });
                cbSelected.setChecked(allSelected);

                tableListView.setOnItemClickListener((parent, view, position, id) -> {
                    DetonatorInfoBean item = list.get(position);
                    item.setSelected(!item.isSelected());
                    list.set(position, item);
                    checkboxStatus();
                    adapter.updateList(list);
                });

                btnModifyDelay = findViewById(R.id.btn_modify_delay);
                btnModifyDelay.setOnClickListener(v -> modifyFunction(KeyEvent.KEYCODE_1));

                MyButton btnModifyInterval = findViewById(R.id.btn_modify_interval);
                btnModifyInterval.setOnClickListener(v -> modifyFunction(KeyEvent.KEYCODE_2));

                MyButton btnDelete = findViewById(R.id.btn_delete);
                btnDelete.setOnClickListener(v -> modifyFunction(KeyEvent.KEYCODE_3));
                if (myApp.isTunnel())
                    findViewById(R.id.text_hole).setOnClickListener(v -> selectWhich(false, (ViewGroup) v.getRootView()));
                else
                    findViewById(R.id.text_row).setOnClickListener(v -> selectWhich(true, (ViewGroup) v.getRootView()));
                break;
            case ConstantUtils.HISTORY_LIST:
                keyMode = MODE_HISTORY;
                String text = String.format(Locale.CHINA, "%.4f", getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                ((TextView) findViewById(R.id.txt_disp1)).setText(R.string.map_longitude);
                ((TextView) findViewById(R.id.tv_disp1)).setText(text);
                text = String.format(Locale.CHINA, "%.4f", getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                ((TextView) findViewById(R.id.txt_disp2)).setText(R.string.map_latitude);
                ((TextView) findViewById(R.id.tv_disp2)).setText(text);
                findViewById(R.id.btn_restore).setOnClickListener(v -> restoreList());
                break;
            case ConstantUtils.AUTHORIZED_LIST:
                ((TextView) findViewById(R.id.txt_disp1)).setText(R.string.detonator_total);
                ((TextView) findViewById(R.id.tv_disp1)).setText(String.format(Locale.CHINA, getResources().getString(R.string.detonator_amount), list.size()));
                ((TextView) findViewById(R.id.txt_disp2)).setText(R.string.detonator_used);
                ((TextView) findViewById(R.id.tv_disp2)).setText(String.format(Locale.CHINA, getResources().getString(R.string.detonator_amount), 0));
                break;
            default:
                break;
        }
        tableListView.requestFocus();
    }

    private void initSerial() {
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(this, bufferRunnable);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void selectWhich(boolean row, ViewGroup v) {
        if (list.size() > 0) {
            final View modifyView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_edit_dialog, v, false);
            final EditText etModifyDelay = modifyView.findViewById(R.id.et_dialog);
            modifyView.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
            etModifyDelay.setHint(row ? R.string.hint_input_select_row : R.string.hint_input_select_hole);
            etModifyDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
            etModifyDelay.setInputType(InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                    .setTitle(row ? R.string.hint_input_select_row : R.string.hint_input_select_hole)
                    .setView(modifyView)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which1) -> {
                        if (etModifyDelay.getText().toString().isEmpty()) {
                            myApp.myToast(DetonatorListActivity.this, R.string.message_amount_input_error);
                        } else {
                            try {
                                int i = Integer.parseInt(etModifyDelay.getText().toString());
                                boolean selectNone = true;
                                if (row) {
                                    for (DetonatorInfoBean bean : list)
                                        if (bean.getRow() == i) {
                                            bean.setSelected(true);
                                            selectNone = false;
                                        } else {
                                            bean.setSelected(false);
                                        }
                                } else
                                    for (DetonatorInfoBean bean : list)
                                        if (bean.getHole() == i) {
                                            bean.setSelected(true);
                                            selectNone = false;
                                        } else {
                                            bean.setSelected(false);
                                        }
                                if (selectNone)
                                    myApp.myToast(DetonatorListActivity.this, R.string.message_amount_input_error);
                                adapter.updateList(list);
                            } catch (Exception e) {
                                myApp.myToast(DetonatorListActivity.this, R.string.message_amount_input_error);
                            }
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create().show();
        }
    }

    private void restoreList() {
        List<DetonatorInfoBean> beanList = new ArrayList<>();
        myApp.readFromFile(myApp.getListFile(), beanList, DetonatorInfoBean.class);
        if (beanList.size() > 0) {
            new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_restore)
                    .setMessage(R.string.dialog_import_cover_list)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which1) -> {
                        myApp.myToast(DetonatorListActivity.this, R.string.message_restore_success);
                        saveList();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create().show();

        } else {
            myApp.myToast(DetonatorListActivity.this, R.string.message_restore_success);
            saveList();
        }
    }

    private void checkboxStatus() {
        cbSelected.setChecked(true);
        for (DetonatorInfoBean i : list) {
            if (!i.isSelected()) {
                cbSelected.setChecked(false);
                break;
            }
        }
    }

    private void showPopupWindow(AdapterView<?> adapterView, View view, int position) {
        String[] menu;
        menu = new String[]{getResources().getString(R.string.menu_delete),
                getResources().getString(R.string.menu_modify),
                getResources().getString(myApp.isTunnel() ? R.string.menu_tunnel_section_insert : R.string.menu_open_air_hole_insert),
                getResources().getString(myApp.isTunnel() ? R.string.menu_tunnel_hole_insert : R.string.menu_open_air_inside_insert)};
        keyMode = MODE_MENU;
        View popupView = DetonatorListActivity.this.getLayoutInflater().inflate(R.layout.layout_popupwindow, adapterView, false);
        popupView.findViewById(R.id.tvTitle).setVisibility(View.GONE);
        clickIndex = position;

        ListView lsvMenu = popupView.findViewById(R.id.lvPopupMenu);
        lsvMenu.setAdapter(new ArrayAdapter<>(DetonatorListActivity.this, R.layout.layout_popupwindow_menu, menu));
        lsvMenu.setOnItemClickListener((parent, view1, position1, id) -> launchMenu(position1, parent));
        lsvMenu.setOnKeyListener((v, keyCode, event) -> {
            launchMenu(keyCode - KeyEvent.KEYCODE_1, (ViewGroup) v.getRootView());
            return false;
        });
        popupMenu = new PopupWindow(popupView, 150, 38 * menu.length);
        popupMenu.setAnimationStyle(R.style.popup_window_anim);
        popupMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupMenu.setFocusable(true);
        popupMenu.setOutsideTouchable(true);
        popupMenu.update();
        popupMenu.showAsDropDown(view, lastTouchX > 75 ? lastTouchX - 75 : 0, list.size() == 0 ? -145 : 0);
        popupMenu.setOnDismissListener(() -> keyMode = MODE_SCAN);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyMode) {
            case MODE_SCAN:
                switch (keyCode) {
                    case KeyEvent.KEYCODE_1:
                        scanDetonator(true);
                        break;
                    case KeyEvent.KEYCODE_2:
                        scanDetonator(false);
                        break;
                    case KeyEvent.KEYCODE_3:
                        manualAppend(false, null);
                        break;
                    case KeyEvent.KEYCODE_B:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                        myHandler.sendEmptyMessageDelayed(HANDLER_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                        break;
//                    case KeyEvent.KEYCODE_4:
//                        myApp.myToast(DetonatorListActivity.this,"调整电压");
//                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "2000###");
//                        break;
//                    case KeyEvent.KEYCODE_5:
//                        if (clickIndex >= 0 && clickIndex < list.size()) {
//                            myApp.myToast(DetonatorListActivity.this,"开灯");
//                            serialPortUtil.sendCmd(list.get(clickIndex).getAddress(), SerialCommand.ACTION_TYPE.LAMP_ON, 0);
//                        }
//                        break;
//                    case KeyEvent.KEYCODE_6:
//                        if (clickIndex >= 0 && clickIndex < list.size()) {
//                            myApp.myToast(DetonatorListActivity.this,"关灯");
//                            serialPortUtil.sendCmd(list.get(clickIndex).getAddress(), SerialCommand.ACTION_TYPE.LAMP_OFF, 0);
//                        }
//                        break;
                    case KeyEvent.KEYCODE_STAR:
                        modifyDelay(1);
                        break;
                    case KeyEvent.KEYCODE_0:
                        if (!myApp.isTunnel())
                            modifyDelay(2);
                        break;
                    case KeyEvent.KEYCODE_POUND:
                        modifyDelay(3);
                        break;
                }
                break;
            case MODE_MENU:
            case MODE_MENU2:
                launchMenu(keyCode - KeyEvent.KEYCODE_1, null);
                break;
            case MODE_MODIFY:
                if (modifyFunction(keyCode))
                    return true;
                break;
            case MODE_HISTORY:
                if (KeyEvent.KEYCODE_1 == keyCode)
                    restoreList();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void launchMenu(int which, ViewGroup viewGroup) {
        if (MODE_MENU2 == keyMode) {
            if (which == 0) {
                manualAppend(false, viewGroup);
            } else
                return;
        } else {
            switch (which) {
                case 0:
                    deleteDetonators(true);
                    break;
                case 1:
                    final View modifyView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_edit_dialog, viewGroup, false);
                    final EditText etModifyDelay = modifyView.findViewById(R.id.et_dialog);
                    etModifyDelay.setHint(R.string.hint_input_delay_time);
                    etModifyDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                    etModifyDelay.setInputType(InputType.TYPE_CLASS_NUMBER);
                    new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_modify_delay)
                            .setView(modifyView)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which1) -> {
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
                            .setNegativeButton(R.string.btn_cancel, null)
                            .setOnKeyListener((dialog, keyCode, event) -> {
                                if (event.getAction() == KeyEvent.ACTION_UP) {
                                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                                        dialog.dismiss();
                                    } else if (keyCode == 0) {
                                        confirmDelay(Integer.parseInt(etModifyDelay.getText().toString()));
                                        dialog.dismiss();
                                    }
                                }
                                return false;
                            })
                            .create().show();
                    break;
                case 2:
                case 3:
                    insertMode = which - 1;
                    doInsert(viewGroup);
                    break;
                default:
                    return;
            }
        }
        popupMenu.dismiss();
    }

    private void deleteDetonators(final boolean single) {
        runOnUiThread(() -> new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_delete_detonator)
                .setMessage(R.string.dialog_confirm_delete_detonator)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    if (single) {
                        int period = 0;
                        boolean onlyRow = clickIndex >= list.size() - 1, onlyHole;
                        if (!onlyRow) {
                            period = list.get(clickIndex + 1).getDelayTime() - list.get(clickIndex).getDelayTime();
                            onlyRow = list.get(clickIndex).getRow() != list.get(clickIndex + 1).getRow() && (clickIndex == 0 || list.get(clickIndex).getRow() != list.get(clickIndex - 1).getRow());
                        }
                        onlyHole = !onlyRow && list.get(clickIndex).getHole() != list.get(clickIndex + 1).getHole()
                                && (clickIndex == 0 || list.get(clickIndex - 1).getRow() != list.get(clickIndex).getRow() || list.get(clickIndex).getHole() != list.get(clickIndex - 1).getHole());
                        for (int i = clickIndex + 1; i < list.size(); i++) {
                            if (list.get(i).getRow() == list.get(clickIndex).getRow()) {
                                list.get(i).setDownloaded(false);
                                list.get(i).setDelayTime(list.get(i).getDelayTime() - period);
                            }
                            if (onlyRow)
                                list.get(i).setRow(list.get(i).getRow() - 1);
                            else if (onlyHole && list.get(i).getRow() == list.get(clickIndex).getRow())
                                list.get(i).setHole(list.get(i).getHole() - 1);
                            else if (list.get(i).getRow() == list.get(clickIndex).getRow() && list.get(i).getHole() == list.get(clickIndex).getHole())
                                list.get(i).setInside(list.get(i).getInside() - 1);
                        }
                        list.remove(clickIndex);
                    } else {
                        Iterator<DetonatorInfoBean> it = list.iterator();
                        while (it.hasNext()) {
                            if (it.next().isSelected()) {
                                it.remove();
                            }
                        }
                    }
                    adapter.updateList(list);
                    saveList();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                    }
                    return false;
                })
                .create().show());
    }

    private void doInsert(final ViewGroup viewGroup) {
        final View insertView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_scan_mode_dialog, viewGroup, false);
        final RadioButton rbScan = insertView.findViewById(R.id.rb_scan_line);
        final RadioButton rbManuel = insertView.findViewById(R.id.rb_manual_line);
        final RadioButton rbUp = insertView.findViewById(R.id.rb_insert_up);
        new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_select_mode)
                .setView(insertView)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
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
//                            myApp.myToast(DetonatorListActivity.this, "c=" + clickIndex + ",h=" + lastHole + ",i=" + lastInside);
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
                                                    - (myApp.isTunnel() ? settings.getSectionInside() : settings.getHoleInside());
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
                            intent.putExtra(KeyUtils.KEY_SCAN_MODE, rbScan.isChecked());
                            intent.putExtra(KeyUtils.KEY_INSERT_MODE, insertMode);
                            intent.putExtra(KeyUtils.KEY_INSERT_INDEX, clickIndex + (insertUp ? 0 : 1));

                            intent.setClass(DetonatorListActivity.this, DetectActivity.class);
                            myReceiveListener.closeAllHandler();
                            launcher.launch(intent);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    private void manualAppend(final boolean insert, ViewGroup viewGroup) {
        final View inputCodeView = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_add_detonator_dialog, viewGroup, false);
        final EditText code = inputCodeView.findViewById(R.id.et_start);
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
        final EditText amount = inputCodeView.findViewById(R.id.et_amount);
        new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_manual_input)
                .setView(inputCodeView)
                .setPositiveButton(R.string.btn_confirm, (dialogInterface, ii) -> {
                    if (Pattern.matches(ConstantUtils.SHELL_PATTERN, code.getText())) {
                        for (int j = 0, k = amount.getText().toString().trim().length() < 1 ? 1 : Integer.parseInt(amount.getText().toString()); j < k; j++) {
                            String det = code.getText().toString().substring(0, 8) + String.format(Locale.CHINA, "%05d", (Long.parseLong(code.getText().toString().substring(8)) + j));
                            int i = 0;
                            for (DetonatorInfoBean bean : list) {
                                i++;
                                if (bean.getAddress().equals(det)) {
                                    myApp.myToast(DetonatorListActivity.this, det +
                                            String.format(Locale.CHINA, getResources().getString(R.string.message_detonator_exist), i));
                                    return;
                                }
                            }
                        }
                        int delayTime = 0, hole = 0, inside = 0, row = 0;
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
                                                    - (myApp.isTunnel() ? settings.getSectionInside() : settings.getHoleInside());

                                    } else
                                        hole = list.get(clickIndex - 1).getHole();
                                }
                            } else {
                                row = list.get(clickIndex).getRow();
                                hole = list.get(clickIndex).getHole();
                                inside = list.get(clickIndex).getInside();
                                delayTime = list.get(clickIndex).getDelayTime();
                            }
//                                    myApp.myToast(DetonatorListActivity.this, "c=" + clickIndex + ",h=" + hole + ",i=" + inside);
                            List<DetonatorInfoBean> newList = new ArrayList<>();
                            for (int j = 0, k = amount.getText().toString().trim().length() < 1 ? 1 : Integer.parseInt(amount.getText().toString()); j < k; j++) {
                                if (row == 0) {
                                    row = 1;
                                    hole = 1;
                                    inside = 1;
                                } else if (insertMode == ConstantUtils.INSERT_HOLE) {
                                    hole++;
                                    inside = 1;
                                    delayTime += myApp.isTunnel() ? settings.getSection() : settings.getHole();
                                } else {
                                    inside++;
                                    delayTime += myApp.isTunnel() ? settings.getSectionInside() : settings.getHoleInside();
                                }
                                newList.add(new DetonatorInfoBean(code.getText().toString().substring(0, 8).toUpperCase() + String.format(Locale.CHINA, "%05d", (Long.parseLong(code.getText().toString().substring(8)) + j)), Math.min(delayTime, ConstantUtils.MAX_DELAY_TIME), row, hole, inside, false));
                            }
                            int period = newList.size();
                            if (insertMode == ConstantUtils.INSERT_INSIDE) {
                                period *= myApp.isTunnel() ? settings.getSectionInside() : settings.getHoleInside();
                            } else {
                                period *= myApp.isTunnel() ? settings.getSection() : settings.getHole();
                            }
                            for (int i = clickIndex + (insertUp ? 0 : 1); i < list.size(); i++) {
                                DetonatorInfoBean bean = list.get(i);
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
                                if (myApp.isTunnel()) {
                                    inside = list.get(list.size() - 1).getInside() + 1;
                                    hole = list.get(list.size() - 1).getHole();
                                    delayTime = list.get(list.size() - 1).getDelayTime() + settings.getSectionInside();
                                } else {
                                    inside = 1;
                                    hole = list.get(list.size() - 1).getHole() + 1;
                                    for (int i = list.size() - 1; i > 0; i--)
                                        if (list.get(i).getInside() == 1) {
                                            delayTime = list.get(i).getDelayTime() + settings.getHole();
                                            break;
                                        }
                                }
                            } else {
                                row = 1;
                                hole = 1;
                                inside = 1;
                            }
                            for (int j = 0, k = amount.getText().toString().trim().length() < 1 ? 1 : Integer.parseInt(amount.getText().toString()); j < k; j++)
                                list.add(new DetonatorInfoBean((code.getText().toString().substring(0, 8).toUpperCase() + String.format(Locale.CHINA, "%05d", (Long.parseLong(code.getText().toString().substring(8)) + j))),
                                        Math.min(delayTime + j * (myApp.isTunnel() ? settings.getSectionInside() : settings.getHole()), ConstantUtils.MAX_DELAY_TIME),
                                        row, (myApp.isTunnel() ? hole : j + hole), (myApp.isTunnel() ? j + inside : inside), false));
                        }
                        saveList();
                        adapter.updateList(list);
                    } else
                        myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_input_error);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create().show();
    }

    private void confirmDelay(int time) {
        newTime = time;
        if (newTime < 0 || newTime > ConstantUtils.MAX_DELAY_TIME)
            myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_time_input_error);
        else {
            DetonatorInfoBean bean = list.get(clickIndex);
            if (newTime != bean.getDelayTime()) {
                bean.setDownloaded(false);
                bean.setDelayTime(newTime);
                list.set(clickIndex, bean);
                adapter.updateList(list);
                saveList();
            }
        }
    }

    private void scanDetonator(boolean scanMode) {
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
        intent.putExtra(KeyUtils.KEY_SCAN_MODE, scanMode);
        intent.setClass(DetonatorListActivity.this, DetectActivity.class);
        launcher.launch(intent);
    }

    private void modifyDelay(final int whichDelay) {
        runOnUiThread(() -> {
            final View view = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_edit_dialog, null);
            final EditText etDelay = view.findViewById(R.id.et_dialog);
            int title = R.string.dialog_title_modify_open_air_hole;

            etDelay.setHint(R.string.hint_input_delay_time);
            etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
            etDelay.setInputType(InputType.TYPE_CLASS_NUMBER);
            if (whichDelay == 1) {
                title = myApp.isTunnel() ? R.string.dialog_title_modify_tunnel_section : R.string.dialog_title_modify_open_air_row;
            } else if (whichDelay == 3) {
                title = myApp.isTunnel() ? R.string.dialog_title_modify_tunnel_hole : R.string.dialog_title_modify_open_air_inside;
            }
            new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                    .setTitle(title)
                    .setView(view)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                        if (!etDelay.getText().toString().isEmpty()) {
                            String text = etDelay.getText().toString() + "ms";
                            if (whichDelay == 1) {
                                tvRowDelay.setText(text);
                                if (myApp.isTunnel()) {
                                    settings.setSection(Integer.parseInt(etDelay.getText().toString()));
                                } else {
                                    settings.setRow(Integer.parseInt(etDelay.getText().toString()));
                                }
                            } else if (whichDelay == 2) {
                                tvHoleDelay.setText(text);
                                settings.setHole(Integer.parseInt(etDelay.getText().toString()));
                            } else {
                                tvInsideDelay.setText(text);
                                if (myApp.isTunnel()) {
                                    settings.setSectionInside(Integer.parseInt(etDelay.getText().toString()));
                                } else {
                                    settings.setHoleInside(Integer.parseInt(etDelay.getText().toString()));
                                }
                            }
                            myApp.saveSettings(settings);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create().show();
        });
    }

    private boolean modifyFunction(int key) {
        switch (key) {
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
                int hasSelection = 0;
                for (DetonatorInfoBean item : list) {
                    if (item.isSelected()) {
                        hasSelection++;
                        if (key == KeyEvent.KEYCODE_1 || hasSelection > 1)
                            break;
                    }
                }
                if (hasSelection > key - KeyEvent.KEYCODE_1) {
                    final View view = LayoutInflater.from(DetonatorListActivity.this).inflate(R.layout.layout_edit_dialog, null);
                    final EditText etDelay = view.findViewById(R.id.et_dialog);
                    final boolean delayTime = key == KeyEvent.KEYCODE_1;
                    etDelay.setHint(delayTime ? R.string.hint_input_delay_time : R.string.hint_input_interval);
                    etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                    etDelay.setInputType(InputType.TYPE_CLASS_NUMBER);

                    new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                            .setTitle(delayTime ? R.string.dialog_title_modify_delay : R.string.dialog_title_modify_interval)
                            .setView(view)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which1) -> {
                                try {
                                    boolean invalidTime;
                                    newTime = Integer.parseInt(etDelay.getText().toString());
                                    if (!delayTime) {
                                        if (etDelay.getText().toString().length() > 2 && etDelay.getText().toString().startsWith("00"))
                                            newTime = -newTime;
                                        int i = -1, countTime = 0;
                                        for (DetonatorInfoBean item : list) {
                                            if (item.isSelected()) {
                                                if (i == -1)
                                                    countTime = item.getDelayTime();
                                                i++;
                                            }
                                        }
                                        countTime += i * newTime;
                                        invalidTime = countTime < 0 || countTime > ConstantUtils.MAX_DELAY_TIME;
                                    } else {
                                        invalidTime = newTime < 0 || newTime > ConstantUtils.MAX_DELAY_TIME;
                                    }
                                    if (invalidTime)
                                        myApp.myToast(DetonatorListActivity.this, R.string.message_detonator_time_input_error);
                                    else {
                                        int lastTime = -1;
                                        for (DetonatorInfoBean item : list) {
                                            if (item.isSelected()) {
                                                if (delayTime) {
                                                    if (item.getDelayTime() != newTime) {
                                                        item.setDelayTime(newTime);
                                                        item.setDownloaded(false);
                                                    }
                                                } else {
                                                    if (lastTime == -1)
                                                        lastTime = item.getDelayTime();
                                                    else {
                                                        lastTime += newTime;
                                                        if (lastTime < 0)
                                                            lastTime = 0;
                                                        if (item.getDelayTime() != lastTime) {
                                                            item.setDelayTime(lastTime);
                                                            item.setDownloaded(false);
                                                        }
                                                    }
                                                }
                                                item.setSelected(false);
                                            }
                                        }
                                        adapter.updateList(list);
                                        saveList();
                                    }
                                } catch (Exception e) {
                                    myApp.myToast(DetonatorListActivity.this, R.string.message_input_error);
                                    BaseApplication.writeErrorLog(e);
                                }
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .create().show();
                } else
                    myApp.myToast(DetonatorListActivity.this, R.string.message_select_list);
                break;
            case KeyEvent.KEYCODE_3:
                for (DetonatorInfoBean item : list) {
                    if (item.isSelected()) {
                        deleteDetonators(false);
                        return false;
                    }
                }
                myApp.myToast(DetonatorListActivity.this, R.string.message_select_list);
                break;
            case KeyEvent.KEYCODE_0:
                for (DetonatorInfoBean item : list)
                    item.setSelected(!item.isSelected());
                adapter.updateList(list);
                checkboxStatus();
                break;
            case KeyEvent.KEYCODE_B:
                serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                myHandler.sendEmptyMessageDelayed(HANDLER_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (tableListView.hasFocus() && tableListView.getSelectedItemPosition() >= 0 && tableListView.getSelectedItemPosition() < list.size()) {
                    list.get(tableListView.getSelectedItemPosition()).setSelected(true);
                    checkboxStatus();
                    adapter.updateList(list);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (tableListView.hasFocus() && tableListView.getSelectedItemPosition() >= 0 && tableListView.getSelectedItemPosition() < list.size()) {
                    list.get(tableListView.getSelectedItemPosition()).setSelected(false);
                    checkboxStatus();
                    adapter.updateList(list);
                    return true;
                }
                break;
        }
        return false;
    }

    private int Dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void initData(int type) {
        list = new ArrayList<>();
        switch (type) {
            case ConstantUtils.RESUME_LIST:
            case ConstantUtils.MODIFY_LIST:
                myApp.readFromFile(myApp.getListFile(), list, DetonatorInfoBean.class);
                break;
            case ConstantUtils.HISTORY_LIST:
                list = getIntent().getParcelableArrayListExtra(KeyUtils.KEY_RECORD_LIST);
                break;
            case ConstantUtils.AUTHORIZED_LIST:
                int projectID = getIntent().getIntExtra("ProjectID", 0);
                for (EnterpriseProjectBean.ResultBean.PageListBean bean : myApp.readProjectList(settings.getUserID())) {
                    if (bean.getProjectID() == projectID) {
                        for (EnterpriseProjectBean.ResultBean.PageListBean.DetonatorBean det : bean.getDetonator()) {
                            list.add(new DetonatorInfoBean(det.getDSC()));
                        }
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }

    public void saveList() {
        try {
            myApp.writeToFile(myApp.getListFile(), list);
            String[] fileList = Arrays.copyOfRange(FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1], 1,
                    FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1].length);
            for (String s : fileList) {
                File file = new File(s);
                if (file.exists() && !file.delete())
                    myApp.myToast(DetonatorListActivity.this, R.string.message_transfer_error);
            }
        } catch (JSONException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(DetonatorListActivity.this, R.string.message_transfer_error);
        }
    }

    @Override
    public void finish() {
        if (btnModifyDelay != null) {
            if (!btnModifyDelay.isEnabled()) {
                new AlertDialog.Builder(DetonatorListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.progress_title)
                        .setMessage(R.string.dialog_exit_download)
                        .setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> DetonatorListActivity.super.finish())
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            } else {
                super.finish();
            }
        } else {
            super.finish();
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
    protected void onResume() {
        super.onResume();
        tableListView.post(() -> tableListView.requestFocus());
        initSerial();
    }

    @Override
    protected void onDestroy() {
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        myHandler.removeCallbacksAndMessages(null);
        if (myReceiveListener != null) {
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
        if (serialPortUtil != null)
            serialPortUtil.closeSerialPort();
        super.onDestroy();
    }
}
