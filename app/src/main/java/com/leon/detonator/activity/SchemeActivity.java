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
import com.leon.detonator.adapter.SchemeAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchemeActivity extends BaseActivity {
    private List<SchemeBean> list = new ArrayList<>();
    private SchemeAdapter adapter;
    private MyButton btnAdd;
    private MyButton btnDelete;
    private MyButton btnModify;
    private CheckBox cbSelected;
    private boolean[] beforeSelect;
    private boolean selectScheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheme);
        selectScheme = getIntent().getBooleanExtra(KeyUtils.KEY_SELECT_SCHEME, false);
        setTitle(selectScheme ? R.string.select_schedule : R.string.delay_scheme);
        btnAdd = findViewById(R.id.btn_new_scheme);
        btnAdd.setOnClickListener(v -> launchTask(0));
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> launchTask(2));
        btnModify = findViewById(R.id.btn_modify);
        btnModify.setOnClickListener(v -> launchTask(1));
        list = DbUtil.getSchemeList();
        if (selectScheme) {
            beforeSelect = new boolean[list.size()];
            for (int i = 0; i < list.size(); i++)
                beforeSelect[i] = list.get(i).isSelected();
            btnDelete.setVisibility(View.GONE);
            btnModify.setVisibility(View.GONE);
            btnAdd.setTextId(R.string.button_confirm);
            btnAdd.setBigButton(true);
        } else {
            boolean b = false;
            for (SchemeBean bean : list)
                if (bean.isSelected())
                    if (b) {
                        bean.setSelected(false);
                        DbUtil.updateScheme(bean);
                    } else
                        b = true;
        }
        adapter = new SchemeAdapter(this, list);
        ListView listView = findViewById(R.id.lv_scheme);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            if (!selectScheme && list.get(i).isSelected())
                launchTask(3);
            else {
                if (!selectScheme)
                    for (SchemeBean bean : list)
                        if (bean.isSelected()) {
                            bean.setSelected(false);
                            DbUtil.updateScheme(bean);
                        }
                list.get(i).setSelected(!list.get(i).isSelected());
                adapter.updateList(list);
                if (!selectScheme)
                    DbUtil.updateScheme(list.get(i));
                checkButton();
            }
        });
        cbSelected = findViewById(R.id.cb_selected);
        cbSelected.setClickable(true);
        cbSelected.setVisibility(selectScheme ? View.VISIBLE : View.INVISIBLE);
        cbSelected.setOnClickListener(v -> {
            if (list.size() > 0) {
                for (SchemeBean bean : list)
                    bean.setSelected(cbSelected.isChecked());
                checkButton();
                adapter.updateList(list);
            } else
                cbSelected.setChecked(false);
            btnAdd.setEnabled(!selectScheme || cbSelected.isChecked());
        });
        checkButton();
        findViewById(R.id.btn_new_scheme).requestFocus();
    }

    private void checkButton() {
        btnAdd.setEnabled(!selectScheme);
        btnModify.setEnabled(false);
        btnDelete.setEnabled(list.size() > 0);
        if (selectScheme) {
            cbSelected.setChecked(list.size() > 0);
            for (SchemeBean bean : list)
                if (!bean.isSelected()) {
                    cbSelected.setChecked(false);
                    break;
                }
        }
        for (SchemeBean bean : list)
            if (bean.isSelected()) {
                btnModify.setEnabled(true);
                btnAdd.setEnabled(true);
                break;
            }
    }

    private void launchTask(int what) {
        switch (what) {
            case KeyEvent.KEYCODE_1:
            case 0:
                if (selectScheme) {
                    if (btnAdd.isEnabled()) {
                        for (int i = 0; i < list.size(); i++)
                            if (list.get(i).isSelected() != beforeSelect[i])
                                DbUtil.updateScheme(list.get(i));
                        setResult(RESULT_OK);
                        finish();
                    }
                } else
                    modifyName(true);
                break;
            case KeyEvent.KEYCODE_2:
            case 1:
                if (!selectScheme && btnModify.isEnabled())
                    modifyName(false);
                break;
            case KeyEvent.KEYCODE_3:
            case 2:
                if (!selectScheme && btnDelete.isEnabled())
                    batchDelete();
                break;
            case 3:
                if (!selectScheme)
                    for (SchemeBean bean : list)
                        if (bean.isSelected()) {
                            Intent intent = new Intent(SchemeActivity.this, DetonatorListActivity.class);
                            intent.putExtra(KeyUtils.KEY_TABLE_ID, bean.getId());
                            startActivity(intent);
                            break;
                        }
        }
    }

    private void modifyName(boolean newScheme) {
        runOnUiThread(() -> {
            final View v = LayoutInflater.from(SchemeActivity.this).inflate(R.layout.layout_dialog_edit, null, false);
            final EditText etName = v.findViewById(R.id.et_dialog);
            final TextView tvDelay = v.findViewById(R.id.tv_dialog);
            int i;
            for (i = 0; i < list.size(); i++)
                if (list.get(i).isSelected())
                    break;
            tvDelay.setVisibility(View.GONE);
            etName.setHint(R.string.hint_input_name);
            etName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            etName.setInputType(InputType.TYPE_CLASS_TEXT);
            etName.setText(newScheme ? String.format(Locale.getDefault(), getString(R.string.default_scheme_name), list.size() + 1) : list.get(i).getName());
            etName.setSelection(etName.getText().length());
            int finalI = i;
            BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                    .setTitle(newScheme ? R.string.dialog_title_new_scheme : R.string.dialog_title_modify_scheme)
                    .setView(v)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                        if (etName.getText() != null && !etName.getText().toString().isEmpty()) {
                            SchemeBean bean = newScheme ? new SchemeBean() : list.get(finalI);
                            bean.setName(etName.getText().toString());
                            for (SchemeBean bean1 : list)
                                if (bean1.isSelected()) {
                                    bean1.setSelected(false);
                                    DbUtil.updateScheme(bean1);
                                }
                            if (newScheme) {
                                bean.setSelected(true);
                                list.add(bean);
                                checkButton();
                            }
                            DbUtil.updateScheme(bean);
                            adapter.updateList(list);
                        } else
                            myApp.myToast(SchemeActivity.this, R.string.message_name_input_error);
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show(), false);
        });
    }

    private void batchDelete() {
        final View modifyView = LayoutInflater.from(SchemeActivity.this).inflate(R.layout.layout_dialog_batch_modify, findViewById(R.id.lv_scheme), false);
        final EditText etFrom = modifyView.findViewById(R.id.et_from);
        final EditText etTo = modifyView.findViewById(R.id.et_to);
        final EditText etDelay = modifyView.findViewById(R.id.et_delay);
        final CheckBox cbSelectAll = modifyView.findViewById(R.id.cb_selected_all);
        modifyView.findViewById(R.id.ll_delay).setVisibility(View.GONE);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).isSelected()) {
                etFrom.setText(String.format(Locale.getDefault(), "%d", i + 1));
                etFrom.setSelection(etFrom.getText().length());
                break;
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
                            myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                    } catch (Exception e) {
                        myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                    }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        etFrom.addTextChangedListener(watcher1);
        etTo.addTextChangedListener(watcher1);
        BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                .setTitle(R.string.button_batch_delete)
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
                            myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            return;
                        }
                    } catch (Exception e) {
                        myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                        return;
                    }
                    int finalFrom = Math.min(from - 1, to - 1);
                    int finalTo = Math.max(from - 1, to - 1);
                    BaseApplication.customDialog(new AlertDialog.Builder(SchemeActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_delete_scheme)
                            .setMessage(R.string.dialog_confirm_delete_scheme)
                            .setPositiveButton(R.string.button_confirm, (dialog1, which) -> {
                                List<Long> idList = new ArrayList<>();
                                for (int i = finalTo; i >= finalFrom; i--) {
                                    idList.add(list.get(i).getId());
                                    list.remove(i);
                                }
                                if (idList.size() > 0) {
                                    long[] id = new long[idList.size()];
                                    for (int i = 0; i < idList.size(); i++)
                                        id[i] = idList.get(i);
                                    DbUtil.deleteScheme(id);
                                }
                                adapter.updateList(list);
                                checkButton();
                            })
                            .setNegativeButton(R.string.button_cancel, null)
                            .show(), true);
                }).
                setNegativeButton(R.string.button_cancel, null)
                .show(), false);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launchTask(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        list = DbUtil.getSchemeList();
        adapter.updateList(list);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (list.size() == 1 && !list.get(0).isSelected()) {
            list.get(0).setSelected(true);
            DbUtil.updateScheme(list.get(0));
        }
        super.onDestroy();
    }
}