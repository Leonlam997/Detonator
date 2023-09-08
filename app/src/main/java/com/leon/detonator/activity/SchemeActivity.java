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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchemeActivity extends BaseActivity {
    private List<SchemeBean> list;
    private InfoAdapter<SchemeBean> adapter;
    private MyButton btnAdd;
    private MyButton btnDelete;
    private MyButton btnModify;
    private ListView listView;
    private boolean selectScheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheme);
        selectScheme = getIntent().getBooleanExtra(KeyUtils.KEY_SELECT_SCHEME, false);
        setTitle(selectScheme ? R.string.select_schedule : R.string.delay_scheme);
        setTitle(R.string.delay_scheme);
        myApp = (BaseApplication) getApplication();
        btnDelete = findViewById(R.id.btn_delete_scheme);
        btnDelete.setOnClickListener(v -> launchTask(2));
        btnModify = findViewById(R.id.btn_modify_scheme);
        btnModify.setOnClickListener(v -> launchTask(1));
        btnAdd = findViewById(R.id.btn_new_scheme);
        btnAdd.setOnClickListener(v -> launchTask(0));

        list = DbUtil.getSchemeList();
        if (selectScheme) {
            btnDelete.setVisibility(View.GONE);
            btnModify.setVisibility(View.GONE);
            btnAdd.setTextId(R.string.button_confirm);
        } else {
            int count = 0;
            long id = -1;
            for (SchemeBean bean : list)
                if (bean.isSelected()) {
                    if (count++ == 0) {
                        id = bean.getId();
                    } else {
                        bean.setSelected(false);
                    }
                }
            if (count > 1)
                DbUtil.selectScheme(new long[]{id});
        }
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
            if (selectScheme) {
                list.get(i).setSelected(!list.get(i).isSelected());
                adapter.updateList(list);
            } else {
                if (list.get(i).isSelected()) {
                    Intent intent = new Intent(SchemeActivity.this, DetonatorListActivity.class);
                    DbUtil.selectScheme(new long[]{list.get(i).getId()});
                    intent.putExtra(KeyUtils.KEY_TABLE_ID, list.get(i).getId());
                    startActivity(intent);
                } else {
                    for (SchemeBean bean : list)
                        if (bean.isSelected())
                            bean.setSelected(false);
                    list.get(i).setSelected(true);
                    adapter.updateList(list);
                }
            }
        });
        enabledButton(true);
    }

    private boolean hasSelected() {
        for (SchemeBean bean : list)
            if (bean.isSelected())
                return true;
        return false;
    }

    private void enabledButton(boolean b) {
        runOnUiThread(() -> {
            setProgressVisibility(!b);
            listView.setEnabled(b);
            btnAdd.setEnabled(b);
            btnDelete.setEnabled(b && list.size() > 0);
            btnModify.setEnabled(b && hasSelected());
        });
    }

    private void launchTask(int what) {
        switch (what) {
            case ConstantUtils.KEYCODE_ADD:
            case 0:
                if (selectScheme) {
                    if (btnAdd.isEnabled()) {
                        List<Long> ids = new ArrayList<>();
                        for (SchemeBean bean : list)
                            if (bean.isSelected())
                                ids.add(bean.getId());
                        if (ids.size() > 0) {
                            long[] id = new long[ids.size()];
                            for (int i = 0; i < ids.size(); i++)
                                id[i] = ids.get(i);
                            DbUtil.selectScheme(id);
                            setResult(RESULT_OK);
                        } else
                            setResult(RESULT_CANCELED);
                        finish();
                    }
                } else
                    modifyName(true);
                break;
            case KeyEvent.KEYCODE_TAB:
            case 1:
                if (!selectScheme && btnModify.isEnabled())
                    modifyName(false);
                break;
            case ConstantUtils.KEYCODE_SUB:
            case 2:
                if (!selectScheme && btnDelete.isEnabled())
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
                                .setCancelable(false)
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
                                                    List<Long> ids = new ArrayList<>();
                                                    for (int j = Math.max(to, from) - 1; j >= Math.min(from, to) - 1; j--) {
                                                        BaseApplication.writeFile(getString(R.string.button_delete) + ":" + list.get(j).getName());
                                                        ids.add(list.get(j).getId());
                                                        list.remove(j);
                                                    }
                                                    if (ids.size() > 0)
                                                        DbUtil.deleteScheme(ids);
                                                    adapter.updateList(list);
                                                })
                                                .setOnDismissListener(dialogInterface -> enabledButton(true))
                                                .setNegativeButton(R.string.button_cancel, null)
                                                .show()));
                                    } catch (Exception e) {
                                        myApp.myToast(SchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    }
                                })
                                .setOnDismissListener(dialogInterface -> enabledButton(true))
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
                    .setCancelable(false)
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
                    if (!selectScheme) {
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
                    }
                    return true;
                case KeyEvent.KEYCODE_8:
                    listView.requestFocus();
                    if (!selectScheme) {
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
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MENU:
                    if (!selectScheme) {
                        for (SchemeBean bean : list)
                            if (bean.isSelected()) {
                                DbUtil.selectScheme(new long[]{bean.getId()});
                                Intent intent = new Intent(SchemeActivity.this, DetonatorListActivity.class);
                                intent.putExtra(KeyUtils.KEY_TABLE_ID, bean.getId());
                                startActivity(intent);
                                break;
                            }
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

    @Override
    protected void onDestroy() {
        if (!selectScheme)
            if (list.size() == 1 && !list.get(0).isSelected()) {
                list.get(0).setSelected(true);
                DbUtil.selectScheme(new long[]{list.get(0).getId()});
            } else {
                boolean notFound = true;
                for (SchemeBean bean : list)
                    if (bean.isSelected()) {
                        notFound = false;
                        DbUtil.selectScheme(new long[]{bean.getId()});
                        break;
                    }
                if (notFound)
                    DbUtil.selectScheme(new long[0]);
            }
        super.onDestroy();
    }
}