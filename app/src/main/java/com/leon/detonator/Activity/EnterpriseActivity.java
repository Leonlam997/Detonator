package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.EnterpriseBean;
import com.leon.detonator.R;

public class EnterpriseActivity extends BaseActivity {
    private EditText etCode, etId, etContract, etProject;
    private CheckBox cbCommercial;
    private EnterpriseBean enterprise;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise);

        setTitle(R.string.settings_enterprise);
        myApp = (BaseApplication) getApplication();
        cbCommercial = findViewById(R.id.cb_commercial);
        cbCommercial.setOnClickListener(view -> findViewById(R.id.rl_commercial).setVisibility(((CheckBox) view).isChecked() ? View.VISIBLE : View.INVISIBLE));
        etCode = findViewById(R.id.et_code);
        etId = findViewById(R.id.et_id);
        etContract = findViewById(R.id.et_contract);
        etProject = findViewById(R.id.et_project);
        initData();
        findViewById(R.id.rl_commercial).setVisibility(cbCommercial.isChecked() ? View.VISIBLE : View.INVISIBLE);
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                findViewById(R.id.btn_clear).setEnabled(!etCode.getText().toString().isEmpty() || !etId.getText().toString().isEmpty()
                        || !etContract.getText().toString().isEmpty() || !etProject.getText().toString().isEmpty());
                findViewById(R.id.btn_save).setEnabled(!etCode.getText().toString().isEmpty() && !etId.getText().toString().isEmpty()
                        && (!cbCommercial.isChecked() || !etContract.getText().toString().isEmpty()));
            }
        };
        etCode.addTextChangedListener(textWatcher);
        etId.addTextChangedListener(textWatcher);
        etContract.addTextChangedListener(textWatcher);
        etProject.addTextChangedListener(textWatcher);
        findViewById(R.id.btn_clear).setOnClickListener(view -> {
            etCode.setText("");
            etId.setText("");
            etContract.setText("");
            etProject.setText("");
            findViewById(R.id.btn_save).setEnabled(false);
            findViewById(R.id.btn_clear).setEnabled(false);
        });
        findViewById(R.id.btn_save).setOnClickListener(view -> {
            if (etCode.getText().toString().isEmpty()) {
                myApp.myToast(EnterpriseActivity.this, R.string.message_input_enterprise_code);
                etCode.requestFocus();
            } else if (etId.getText().toString().isEmpty()) {
                myApp.myToast(EnterpriseActivity.this, R.string.message_input_id);
                etId.requestFocus();
            } else if (etId.getText().toString().length() != 18 || (etId.getText().toString().contains(".") && !etId.getText().toString().endsWith("."))) {
                myApp.myToast(EnterpriseActivity.this, R.string.message_input_id_error);
                etId.requestFocus();
            } else if (cbCommercial.isChecked() && etContract.getText().toString().isEmpty()) {
                myApp.myToast(EnterpriseActivity.this, R.string.message_input_contract_code);
                etContract.requestFocus();
            } else {
                enterprise = new EnterpriseBean();
                enterprise.setCode(etCode.getText().toString());
                enterprise.setId(etId.getText().toString());
                enterprise.setCommercial(cbCommercial.isChecked());
                if (cbCommercial.isChecked()) {
                    enterprise.setContract(etContract.getText().toString());
                    enterprise.setProject(etProject.getText().toString());
                }
                myApp.saveEnterprise(enterprise);
                myApp.myToast(EnterpriseActivity.this, R.string.message_save_success);
                finish();
            }
        });
    }

    @Override
    public void finish() {
        EnterpriseBean bean = myApp.readEnterprise();
        if (!etCode.getText().toString().equals(null == bean ? "" : bean.getCode())
                || !etId.getText().toString().equals(null == bean ? "" : bean.getId())
                || cbCommercial.isChecked() != (null != bean && bean.isCommercial())
                || !etContract.getText().toString().equals(null == bean ? "" : bean.getContract())
                || !etProject.getText().toString().equals(null == bean ? "" : bean.getProject())) {
            new AlertDialog.Builder(EnterpriseActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> EnterpriseActivity.super.finish())
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setOnKeyListener((dialog, keyCode, event) -> {
                        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.dismiss();
                        }
                        return false;
                    })
                    .create().show();
        } else
            super.finish();
    }

    private void initData() {
        enterprise = myApp.readEnterprise();
        if (null != enterprise) {
            etCode.setText(enterprise.getCode());
            etId.setText(enterprise.getId());
            if (enterprise.isCommercial()) {
                cbCommercial.setChecked(enterprise.isCommercial());
                etContract.setText(enterprise.getContract());
                etProject.setText(enterprise.getProject());
            }
        }
        findViewById(R.id.btn_clear).setEnabled(!etCode.getText().toString().isEmpty() || !etId.getText().toString().isEmpty()
                || !etContract.getText().toString().isEmpty() || !etProject.getText().toString().isEmpty());
        findViewById(R.id.btn_save).setEnabled(!etCode.getText().toString().isEmpty() && !etId.getText().toString().isEmpty()
                && (!cbCommercial.isChecked() || !etContract.getText().toString().isEmpty()));
    }
}
