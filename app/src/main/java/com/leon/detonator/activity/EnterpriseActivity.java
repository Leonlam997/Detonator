package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.regex.Pattern;

public class EnterpriseActivity extends BaseActivity {
    private EditText etCode;
    private EditText etId;
    private EditText etContract;
    private EditText etProject;
    private CheckBox cbCommercial;
    private EnterpriseBean enterprise;
    private MyButton btnClear;
    private MyButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise);

        setTitle(R.string.settings_enterprise);
        cbCommercial = findViewById(R.id.cb_commercial);
        cbCommercial.setOnClickListener(v -> findViewById(R.id.rl_commercial).setVisibility(cbCommercial.isChecked() ? View.VISIBLE : View.GONE));
        etCode = findViewById(R.id.et_code);
        etId = findViewById(R.id.et_id);
        etContract = findViewById(R.id.et_contract);
        etProject = findViewById(R.id.et_project);
        btnClear = findViewById(R.id.btn_clear);
        btnSave = findViewById(R.id.btn_save);
        initData();
        findViewById(R.id.rl_commercial).setVisibility(cbCommercial.isChecked() ? View.VISIBLE : View.GONE);
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkButton();
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
                checkButton();
                if (editable.toString().contains("."))
                    editable.replace(editable.toString().indexOf("."), editable.toString().indexOf(".") + 1, "X");
            }
        });
        etContract.addTextChangedListener(textWatcher);
        etProject.addTextChangedListener(textWatcher);
        btnClear.setOnClickListener(view -> {
            etCode.setText("");
            etId.setText("");
            etContract.setText("");
            etProject.setText("");
            btnSave.setEnabled(false);
            btnClear.setEnabled(false);
        });
        btnSave.setOnClickListener(view -> {
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
                enterprise.setCode(etCode.getText().toString());
                enterprise.setBlasterId(etId.getText().toString());
                enterprise.setCommercial(cbCommercial.isChecked());
                enterprise.setContract(cbCommercial.isChecked() ? etContract.getText().toString() : "");
                enterprise.setProject(cbCommercial.isChecked() ? etProject.getText().toString() : "");
                DbUtil.updateEnterprise(enterprise);
                setResult(RESULT_OK);
                finish();
            }
        });
        cbCommercial.setOnCheckedChangeListener((compoundButton, b) -> checkButton());
        etCode.requestFocus();
    }

    private void checkButton() {
        btnClear.setEnabled(!etCode.getText().toString().isEmpty() || !etId.getText().toString().isEmpty() || !etContract.getText().toString().isEmpty() || !etProject.getText().toString().isEmpty());
        btnSave.setEnabled(!etCode.getText().toString().isEmpty() && !etId.getText().toString().isEmpty() && (!cbCommercial.isChecked() || !etContract.getText().toString().isEmpty()));
    }

    @Override
    public void finish() {
        if (!etCode.getText().toString().equals(null == enterprise ? "" : enterprise.getCode())
                || !etId.getText().toString().equals(null == enterprise ? "" : enterprise.getBlasterId())
                || cbCommercial.isChecked() != (null != enterprise && enterprise.isCommercial())
                || (cbCommercial.isChecked() && !etContract.getText().toString().equals(null == enterprise ? "" : enterprise.getContract()))
                || (cbCommercial.isChecked() && !etProject.getText().toString().equals(null == enterprise ? "" : enterprise.getProject()))) {
            BaseApplication.customDialog(new AlertDialog.Builder(EnterpriseActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.button_save, (dialog, which) -> btnSave.callOnClick())
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> EnterpriseActivity.super.finish())
                    .show(), true);
        } else super.finish();
    }

    private void initData() {
        if (getIntent().getBooleanExtra(KeyUtils.KEY_NEW_INFO, false)) {
            enterprise = new EnterpriseBean();
            enterprise.setSelected(true);
            enterprise.setId(-1);
        } else {
            enterprise = DbUtil.getCurrentEnterprise();
            if (null != enterprise) {
                etCode.setText(enterprise.getCode());
                etId.setText(enterprise.getBlasterId());
                if (enterprise.isCommercial()) {
                    cbCommercial.setChecked(enterprise.isCommercial());
                    etContract.setText(enterprise.getContract());
                    etProject.setText(enterprise.getProject());
                }
            }
        }
        checkButton();
    }
}
