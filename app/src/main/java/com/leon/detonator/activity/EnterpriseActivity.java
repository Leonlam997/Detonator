package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;

import java.util.regex.Pattern;

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
                findViewById(R.id.btn_clear).setEnabled(!etCode.getText().toString().isEmpty() || !etId.getText().toString().isEmpty() || !etContract.getText().toString().isEmpty() || !etProject.getText().toString().isEmpty());
                findViewById(R.id.btn_save).setEnabled(!etCode.getText().toString().isEmpty() && !etId.getText().toString().isEmpty() && (!cbCommercial.isChecked() || !etContract.getText().toString().isEmpty()));
            }
        };
        etCode.addTextChangedListener(textWatcher);
        etId.setKeyListener(new NumberKeyListener() {
            @NonNull
            @Override
            protected char[] getAcceptedChars() {
                return ConstantUtils.INPUT_ID_ACCEPT.toCharArray();
            }

            @Override
            public int getInputType() {
                return InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
        });
        etId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                findViewById(R.id.btn_clear).setEnabled(!etCode.getText().toString().isEmpty() || !etId.getText().toString().isEmpty() || !etContract.getText().toString().isEmpty() || !etProject.getText().toString().isEmpty());
                findViewById(R.id.btn_save).setEnabled(!etCode.getText().toString().isEmpty() && !etId.getText().toString().isEmpty() && (!cbCommercial.isChecked() || !etContract.getText().toString().isEmpty()));
                if (editable.toString().contains("."))
                    editable.replace(editable.toString().indexOf("."), editable.toString().indexOf(".") + 1, "X");
            }
        });
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
            } else if (!Pattern.matches(ConstantUtils.ID_PATTERN, etId.getText())) {
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
                enterprise.setContract(cbCommercial.isChecked() ? etContract.getText().toString() : "");
                enterprise.setProject(cbCommercial.isChecked() ? etProject.getText().toString() : "");
                myApp.saveBean(enterprise);
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
                || (cbCommercial.isChecked() && !etContract.getText().toString().equals(null == bean ? "" : bean.getContract()))
                || (cbCommercial.isChecked() && !etProject.getText().toString().equals(null == bean ? "" : bean.getProject()))) {
            BaseApplication.customDialog(new AlertDialog.Builder(EnterpriseActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> EnterpriseActivity.super.finish())
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setOnKeyListener((dialog, keyCode, event) -> {
                        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.dismiss();
                        }
                        return false;
                    }).show());
        } else super.finish();
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
        findViewById(R.id.btn_clear).setEnabled(!etCode.getText().toString().isEmpty() || !etId.getText().toString().isEmpty() || !etContract.getText().toString().isEmpty() || !etProject.getText().toString().isEmpty());
        findViewById(R.id.btn_save).setEnabled(!etCode.getText().toString().isEmpty() && !etId.getText().toString().isEmpty() && (!cbCommercial.isChecked() || !etContract.getText().toString().isEmpty()));
    }
}
