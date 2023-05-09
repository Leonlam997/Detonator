package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.adapter.SchemeAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SchemeActivity extends BaseActivity {
    private final List<SchemeBean> list = new ArrayList<>();
    private final List<SchemeBean> allList = new ArrayList<>();
    private BaseApplication myApp;
    private SchemeAdapter adapter;
    private Date lastFile;
    private int lastTouchX;
    private int clickIndex;
    private PopupWindow popupMenu;
    private MyButton btnRegister;
    private MyButton btnModify;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheme);
        setTitle(R.string.delay_scheme);
        myApp = (BaseApplication) getApplication();
        File file = new File(FilePath.FILE_SCHEME_PATH);
        if (file.exists()) {
            if (!file.isDirectory() && file.delete() && !file.mkdirs()) {
                myApp.myToast(this, R.string.message_copy_file_fail);
                return;
            }
        } else if (!file.mkdirs()) {
            myApp.myToast(this, R.string.message_copy_file_fail);
            return;
        }
        btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(v -> launchTask(0));
        btnModify = findViewById(R.id.btn_modify);
        btnModify.setOnClickListener(v -> launchTask(1));
        findViewById(R.id.btn_new_scheme).setOnClickListener(v -> launchTask(2));
        myApp.readFromFile(FilePath.FILE_SCHEME_LIST, allList, SchemeBean.class);
        refreshList(false);
        initData();
        adapter = new SchemeAdapter(this, list, pos -> {
            btnRegister.setEnabled(true);
            btnModify.setEnabled(list.get(pos).getAmount() > 0);
            for (SchemeBean bean : allList)
                if (bean.isTunnel() == myApp.isTunnel()) {
                    bean.setSelected(bean.equals(list.get(pos)));
                }
            refreshList(true);
        });
        lastFile = null;
        ListView listView = findViewById(R.id.lv_scheme);
        listView.setAdapter(adapter);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).isSelected()) {
                lastFile = list.get(i).getCreateTime();
                listView.smoothScrollToPosition(i);
                listView.setSelection(i);
                break;
            }
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
        listView.setOnItemClickListener((adapterView, view, i, l) -> showPopupWindow(adapterView, view, i));
        listView.requestFocus();
    }

    private void initData() {
        for (SchemeBean bean : list) {
            List<DetonatorInfoBean> beans = new ArrayList<>();
            myApp.readFromFile(bean.fileName(), beans, DetonatorInfoBean.class);
            if (bean.getAmount() != beans.size())
                bean.setAmount(beans.size());
        }
    }

    private void refreshList(boolean save) {
        if (save)
            try {
                myApp.writeToFile(FilePath.FILE_SCHEME_LIST, allList);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        list.clear();
        btnRegister.setEnabled(false);
        btnModify.setEnabled(false);
        for (SchemeBean bean : allList)
            if (bean.isTunnel() == myApp.isTunnel()) {
                list.add(bean);
                if (bean.isSelected()) {
                    btnRegister.setEnabled(true);
                    btnModify.setEnabled(bean.getAmount() > 0);
                }
            }
        if (adapter != null)
            adapter.updateList(list);
    }

    private void showPopupWindow(AdapterView<?> adapterView, View view, int position) {
        String[] menu;
        menu = new String[]{getString(R.string.menu_select),
                getString(R.string.menu_modify_name),
                getString(R.string.menu_delete_scheme)};
        for (int i = 0; i < menu.length; i++)
            menu[i] = (i + 1) + "." + menu[i];
        View popupView = SchemeActivity.this.getLayoutInflater().inflate(R.layout.layout_popupwindow, adapterView, false);
        popupView.findViewById(R.id.tvTitle).setVisibility(View.GONE);
        clickIndex = position;
        ListView lsvMenu = popupView.findViewById(R.id.lvPopupMenu);
        lsvMenu.setAdapter(new ArrayAdapter<>(SchemeActivity.this, R.layout.layout_popupwindow_menu, menu));
        lsvMenu.setOnItemClickListener((parent, view1, position1, id) -> launchMenu(position1));
        lsvMenu.setOnKeyListener((v, keyCode, event) -> {
            launchMenu(keyCode - KeyEvent.KEYCODE_1);
            return false;
        });
        popupMenu = new PopupWindow(popupView, 150, 38 * menu.length);
        popupMenu.setAnimationStyle(R.style.popup_window_anim);
        popupMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupMenu.setFocusable(true);
        popupMenu.setOutsideTouchable(true);
        popupMenu.update();
        popupMenu.showAsDropDown(view, lastTouchX > 75 ? lastTouchX - 75 : 0, list.size() == 0 ? -145 : 0);
    }

    private void launchMenu(int position) {
        if (popupMenu != null && popupMenu.isShowing())
            popupMenu.dismiss();
        switch (position) {
            case 0:
                BaseApplication.writeFile(getString(R.string.menu_select) + ":" + list.get(clickIndex));
                for (SchemeBean schemeBean : allList)
                    if (schemeBean.isTunnel() == myApp.isTunnel()) {
                        schemeBean.setSelected(schemeBean.equals(list.get(clickIndex)));
                    }
                refreshList(true);
                break;
            case 1:
                modifyName(false);
                break;
            case 2:
                BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_delete_scheme)
                        .setMessage(R.string.dialog_confirm_delete_scheme)
                        .setPositiveButton(R.string.btn_confirm, (dialog1, which1) -> {
                            BaseApplication.writeFile(getString(R.string.menu_delete_scheme) + ":" + list.get(clickIndex));
                            allList.remove(list.get(clickIndex));
                            refreshList(true);
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show(), true);
                break;
        }
    }

    private void launchTask(int i) {
        if (i == 2)
            modifyName(true);
        else if ((i == 0 && btnRegister.isEnabled()) || (i == 1 && btnModify.isEnabled())) {
            for (SchemeBean schemeBean : list)
                if (schemeBean.isSelected()) {
                    BaseApplication.writeFile(getString(i == 0 ? R.string.button_add_detonator : R.string.menu_modify) + ":" + schemeBean);
                    if (lastFile == null || !lastFile.equals(schemeBean.getCreateTime())) {
                        change(schemeBean.fileName());
                        lastFile = schemeBean.getCreateTime();
                    }
                    Intent intent = new Intent();
                    intent.setClass(SchemeActivity.this, DetonatorListActivity.class);
                    intent.putExtra(KeyUtils.KEY_CREATE_DELAY_LIST, i == 0 ? ConstantUtils.RESUME_LIST : ConstantUtils.MODIFY_LIST);
                    startActivity(intent);
                    break;
                }
        }
    }

    private void modifyName(boolean newScheme) {
        runOnUiThread(() -> {
            final View v = LayoutInflater.from(SchemeActivity.this).inflate(R.layout.layout_edit_dialog, null, false);
            final EditText etName = v.findViewById(R.id.et_dialog);
            final TextView tvDelay = v.findViewById(R.id.tv_dialog);
            etName.setHint(R.string.hint_input_name);
            etName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            etName.setInputType(InputType.TYPE_CLASS_TEXT);
            tvDelay.setVisibility(View.GONE);
            if (!newScheme)
                etName.setText(list.get(clickIndex).getName());
            BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                    .setTitle(newScheme ? R.string.dialog_title_new_scheme : R.string.dialog_title_modify_scheme)
                    .setView(v)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                        if (etName.getText() != null && !etName.getText().toString().isEmpty()) {
                            if (newScheme) {
                                SchemeBean bean = new SchemeBean();
                                bean.setName(etName.getText().toString());
                                bean.setTunnel(myApp.isTunnel());
                                bean.setSelected(true);
                                for (SchemeBean schemeBean : allList)
                                    if (schemeBean.isTunnel() == myApp.isTunnel())
                                        schemeBean.setSelected(false);
                                allList.add(bean);
                                BaseApplication.writeFile(getString(R.string.button_new_scheme) + ":" + bean);
                                refreshList(true);
                                launchTask(0);
                            } else {
                                BaseApplication.writeFile(getString(R.string.menu_modify_name) + ":" + list.get(clickIndex).toString() + "->" + etName.getText().toString());
                                int i = allList.indexOf(list.get(clickIndex));
                                if (i >= 0) {
                                    allList.get(i).setName(etName.getText().toString());
                                    refreshList(true);
                                }
                            }
                        } else
                            myApp.myToast(SchemeActivity.this, R.string.message_name_input_error);
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show(), false);
        });
    }

    private void change(String name) {
        BaseApplication.copyFile(FilePath.FILE_SCHEME_PATH + "/" + name, myApp.getListFile());
        myApp.deleteDetectTempFiles();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launchTask(keyCode - KeyEvent.KEYCODE_1);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        myApp.readFromFile(FilePath.FILE_SCHEME_LIST, allList, SchemeBean.class);
        refreshList(false);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        File oldFile = new File(myApp.getListFile());
        if (lastFile == null) {
            if (list.size() > 0) {
                for (SchemeBean bean : list)
                    if (bean.isSelected()) {
                        change(bean.fileName());
                        break;
                    }
            } else if (oldFile.exists() && !oldFile.delete())
                myApp.myToast(SchemeActivity.this, R.string.message_delete_fail);
        } else {
            if (list.size() > 0) {
                for (SchemeBean bean : list)
                    if (bean.isSelected()) {
                        if (!lastFile.equals(bean.getCreateTime()))
                            change(bean.fileName());
                        break;
                    }
            } else if (oldFile.exists() && !oldFile.delete())
                myApp.myToast(SchemeActivity.this, R.string.message_delete_fail);
        }
        super.onDestroy();
    }
}