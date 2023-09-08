package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.adapter.InfoAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.List;
import java.util.Locale;

public class SchemeActivity extends BaseActivity {
    private List<SchemeBean> list;
    private BaseApplication myApp;
    private InfoAdapter<SchemeBean> adapter;
    private MyButton btnNew;
    private MyButton btnDelete;
    private MyButton btnModify;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheme);
        setTitle(R.string.delay_scheme);
        myApp = (BaseApplication) getApplication();
        btnDelete = findViewById(R.id.btn_delete_scheme);
        btnDelete.setOnClickListener(v -> launchTask(2));
        btnModify = findViewById(R.id.btn_modify_scheme);
        btnModify.setOnClickListener(v -> launchTask(1));
        btnNew = findViewById(R.id.btn_new_scheme);
        btnNew.setOnClickListener(v -> launchTask(0));

        list = DbUtil.getSchemeList();
        adapter = new InfoAdapter<>(this, list);
        listView = findViewById(R.id.lv_scheme);
        listView.setAdapter(adapter);
        listView.requestFocus();
        btnDelete.setEnabled(list.size() > 0);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).isSelected()) {
                listView.smoothScrollToPosition(i);
                listView.setSelection(i);
                break;
            }
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            enabledButton(false);
            list.get(i).setSelected(!list.get(i).isSelected());
            DbUtil.updateScheme(list.get(i));
            adapter.updateList(list);
            listView.setSelection(i);
            btnModify.setEnabled(singleSelect());
            enabledButton(true);
        });
        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            modifyName(false);
            return true;
        });
        enabledButton(true);
    }

    private void enabledButton(boolean b) {
        setProgressVisibility(!b);
        listView.setEnabled(b);
        btnNew.setEnabled(b);
        btnDelete.setEnabled(b && list.size() > 0);
        btnModify.setEnabled(b && singleSelect());
    }

    private boolean singleSelect() {
        boolean b = false;
        for (SchemeBean bean : list)
            if (bean.isSelected()) {
                if (!b)
                    b = true;
                else
                    return false;
            }
        return b;
    }

    private void launchTask(int i) {
        switch (i) {
            case ConstantUtils.KEYCODE_ADD:
            case 0:
                modifyName(true);
                break;
            case KeyEvent.KEYCODE_TAB:
            case 1:
                if (btnModify.isEnabled())
                    for (SchemeBean bean : list)
                        if (bean.isSelected()) {
                            Intent intent = new Intent(SchemeActivity.this, DetonatorListActivity.class);
                            intent.putExtra(KeyUtils.KEY_TABLE_ID, bean.getId());
                            startActivity(intent);
                            break;
                        }
                break;
            case ConstantUtils.KEYCODE_SUB:
            case 2:
                if (btnDelete.isEnabled())
                    runOnUiThread(() -> {
                        enabledButton(false);
                        final View deleteView = LayoutInflater.from(SchemeActivity.this).inflate(R.layout.layout_dialog_batch_modify, listView, false);
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
                        final TextWatcher watcher = new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                if (!charSequence.toString().isEmpty())
                                    try {
                                        int num = Integer.parseInt(charSequence.toString());
                                        if (num <= 0 || num > list.size())
                                            myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    } catch (Exception e) {
                                        myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    }
                            }

                            @Override
                            public void afterTextChanged(Editable editable) {

                            }
                        };
                        etFrom.addTextChangedListener(watcher);
                        etTo.addTextChangedListener(watcher);
                        BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_batch_delete)
                                .setView(deleteView)
                                .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                                    try {
                                        if (etTo.getText().toString().isEmpty())
                                            etTo.setText(etFrom.getText());
                                        int from = Integer.parseInt(etFrom.getText().toString());
                                        int to = Integer.parseInt(etTo.getText().toString());
                                        if (from <= 0 || from > list.size() || to <= 0 || to > list.size()) {
                                            myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                            return;
                                        }
                                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                                                .setTitle(R.string.dialog_title_delete_scheme)
                                                .setMessage(R.string.dialog_confirm_delete_scheme)
                                                .setPositiveButton(R.string.button_confirm, (dialog1, which) -> {
                                                    for (int j = Math.max(to, from) - 1; j >= Math.min(from, to) - 1; j--) {
                                                        BaseApplication.writeFile(getString(R.string.button_delete) + ":" + list.get(j).getName());
                                                        DbUtil.deleteScheme(list.get(j).getId());
                                                        list.remove(j);
                                                    }
                                                    adapter.updateList(list);
                                                })
                                                .setOnDismissListener(dialogInterface -> enabledButton(true))
                                                .setNegativeButton(R.string.button_cancel, null)
                                                .show()));
                                    } catch (Exception e) {
                                        myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    }
                                })
                                .setNegativeButton(R.string.button_cancel, null)
                                .show());
                    });
                break;
        }
    }

    private void modifyName(boolean newScheme) {
        runOnUiThread(() -> {
            enabledButton(false);
            final View v = LayoutInflater.from(SchemeActivity.this).inflate(R.layout.layout_dialog_edit, null, false);
            final EditText etName = v.findViewById(R.id.et_dialog);
            final TextView tvDelay = v.findViewById(R.id.tv_dialog);
            etName.setHint(R.string.hint_input_name);
            etName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            etName.setInputType(InputType.TYPE_CLASS_TEXT);
            etName.requestFocus();
            tvDelay.setVisibility(View.GONE);
            if (newScheme)
                etName.setText(String.format(Locale.getDefault(), getString(R.string.default_scheme_name), list.size() + 1));
            else
                for (int i = 0; i < list.size(); i++)
                    if (list.get(i).isSelected()) {
                        etName.setText(list.get(i).getName());
                        break;
                    }
            etName.setSelection(etName.getText().length());
            BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                    .setTitle(newScheme ? R.string.dialog_title_new_scheme : R.string.dialog_title_modify_scheme)
                    .setView(v)
                    .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                        if (etName.getText() != null && !etName.getText().toString().isEmpty()) {
                            if (newScheme) {
                                SchemeBean bean = new SchemeBean();
                                bean.setName(etName.getText().toString());
                                bean.setSelected(true);
                                bean.setId(-1);
                                btnDelete.setEnabled(true);
                                btnModify.setEnabled(true);
                                DbUtil.updateScheme(bean);
                                for (SchemeBean b : list)
                                    b.setSelected(false);
                                list.add(bean);
                                adapter.updateList(list);
                                listView.smoothScrollToPosition(list.size() - 1);
                                listView.setSelection(list.size() - 1);
                                BaseApplication.writeFile(getString(R.string.button_new) + ":" + bean);
                            } else
                                for (SchemeBean bean : list)
                                    if (bean.isSelected()) {
                                        bean.setName(etName.getText().toString());
                                        DbUtil.updateScheme(bean);
                                        adapter.updateList(list);
                                        break;
                                    }
                        } else
                            myApp.myToast(SchemeActivity.this, R.string.message_name_input_error);
                    })
                    .setOnDismissListener(dialogInterface -> enabledButton(true))
                    .setNegativeButton(R.string.button_cancel, null)
                    .show());
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN)
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                case ConstantUtils.KEYCODE_SUB:
                case KeyEvent.KEYCODE_TAB:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MENU:
                    return true;
            }
        if (event.getAction() == KeyEvent.ACTION_UP)
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                case ConstantUtils.KEYCODE_SUB:
                case KeyEvent.KEYCODE_TAB:
                    launchTask(event.getKeyCode());
                    return true;
                case KeyEvent.KEYCODE_2:
                    listView.requestFocus();
                    if (listView.getSelectedItemPosition() > 0)
                        listView.setSelection(listView.getSelectedItemPosition() - 1);
                    else if (listView.getSelectedItemPosition() == 0)
                        listView.setSelection(list.size() - 1);
                    else
                        for (int i = 0; i < list.size(); i++)
                            if (list.get(i).isSelected()) {
                                listView.setSelection(i);
                                break;
                            }
                    return true;
                case KeyEvent.KEYCODE_8:
                    listView.requestFocus();
                    if (listView.getSelectedItemPosition() < list.size() - 1)
                        listView.setSelection(listView.getSelectedItemPosition() + 1);
                    else if (listView.getSelectedItemPosition() == list.size() - 1)
                        listView.setSelection(0);
                    else
                        for (int i = 0; i < list.size(); i++)
                            if (list.get(i).isSelected()) {
                                listView.setSelection(i);
                                break;
                            }
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MENU:
                    for (SchemeBean bean : list)
                        if (bean.isSelected()) {
                            Intent intent = new Intent(SchemeActivity.this, DetonatorListActivity.class);
                            intent.putExtra(KeyUtils.KEY_TABLE_ID, bean.getId());
                            startActivity(intent);
                            break;
                        }
                    return true;
            }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        list = DbUtil.getSchemeList();
        adapter.updateList(list);
        super.onResume();
    }
}