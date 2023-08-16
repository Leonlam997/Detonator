package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.regex.Pattern;

public class BaiSeDataActivity extends BaseActivity {
    private BaiSeInfoBean baiSeInfoBean;
    private EditText etName;
    private EditText etId;
    private EditText etCompany;
    private EditText etCode;
    private EditText etProjectName;
    private EditText etProjectCode;
    private RadioButton rbProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bai_se_data);
        setTitle(R.string.settings_enterprise);
        rbProject = findViewById(R.id.rb_project);
        rbProject.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                ((TextView) findViewById(R.id.txt_name)).setText(R.string.enterprise_project_name);
                ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_project_code);
            } else {
                ((TextView) findViewById(R.id.txt_name)).setText(R.string.enterprise_contract_name);
                ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_contract_code);
            }
        });
        etName = findViewById(R.id.et_name);
        etId = findViewById(R.id.et_id);
        etCompany = findViewById(R.id.et_company);
        etCode = findViewById(R.id.et_code);
        etProjectName = findViewById(R.id.et_project_name);
        etProjectCode = findViewById(R.id.et_project_code);
        if (getIntent().getBooleanExtra(KeyUtils.KEY_NEW_INFO, false)) {
            baiSeInfoBean = new BaiSeInfoBean();
            baiSeInfoBean.setId(-1);
            baiSeInfoBean.setSelected(true);
        } else
            baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
        if (baiSeInfoBean != null) {
            etName.setText(baiSeInfoBean.getBursterName());
            etId.setText(baiSeInfoBean.getIdCard());
            etCompany.setText(baiSeInfoBean.getBurstOrgName());
            etCode.setText(baiSeInfoBean.getBurstOrgCode());
            etProjectCode.setText(baiSeInfoBean.getProjectCode());
            etProjectName.setText(baiSeInfoBean.getProjectName());
            ((RadioButton) findViewById(R.id.rb_contract)).setChecked(baiSeInfoBean.getProjectType().equals(ConstantUtils.ENTERPRISE_CONTRACT));
        }
        etCompany.requestFocus();
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
                if (editable.toString().contains("."))
                    editable.replace(editable.toString().indexOf("."), editable.toString().indexOf(".") + 1, "X");
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(view -> {
            etName.setText("");
            etId.setText("");
            etCompany.setText("");
            etCode.setText("");
            etProjectCode.setText("");
            etProjectName.setText("");
        });
        findViewById(R.id.btn_save).setOnClickListener(view -> {
            if (etCompany.getText() == null || etCompany.getText().toString().isEmpty())
                etCompany.requestFocus();
            else if (etCode.getText() == null || etCode.getText().toString().isEmpty())
                etCode.requestFocus();
            else if (etProjectName.getText() == null || etProjectName.getText().toString().isEmpty())
                etProjectName.requestFocus();
            else if (etProjectCode.getText() == null || etProjectCode.getText().toString().isEmpty())
                etProjectCode.requestFocus();
            else if (etId.getText() == null || etId.getText().toString().isEmpty())
                etId.requestFocus();
            else if (etName.getText() == null || etName.getText().toString().isEmpty())
                etName.requestFocus();
            else {
                if (!Pattern.matches(ConstantUtils.ID_PATTERN, etId.getText().toString())) {
                    myApp.myToast(BaiSeDataActivity.this, R.string.message_input_id_error);
                    etId.requestFocus();
                } else {
                    baiSeInfoBean.setProjectType(rbProject.isChecked() ? ConstantUtils.ENTERPRISE_PROJECT : ConstantUtils.ENTERPRISE_CONTRACT);
                    baiSeInfoBean.setBursterName(etName.getText().toString());
                    baiSeInfoBean.setIdCard(etId.getText().toString());
                    baiSeInfoBean.setBurstOrgName(etCompany.getText().toString());
                    baiSeInfoBean.setBurstOrgCode(etCode.getText().toString());
                    baiSeInfoBean.setProjectCode(etProjectCode.getText().toString());
                    baiSeInfoBean.setProjectName(etProjectName.getText().toString());
                    if (baiSeInfoBean.getId() == -1) {
                        BaiSeBlasterBean baiSeBlasterBean = new BaiSeBlasterBean();
                        baiSeBlasterBean.setChecked(false);
                        baiSeBlasterBean.setSelected(true);
                        baiSeBlasterBean.setName(etName.getText().toString());
                        baiSeBlasterBean.getData().setUserIdCard(etId.getText().toString());
                        baiSeBlasterBean.getData().setProjectCode(etProjectCode.getText().toString());
                        baiSeBlasterBean.getData().setBurstOrgCode(etCode.getText().toString());
                        DbUtil.updateBaiSeBlaster(baiSeBlasterBean);
                    }
                    DbUtil.updateBaiSeInfo(baiSeInfoBean);
                    setResult(RESULT_OK);
                    finish();
                }
                return;
            }
            myApp.myToast(BaiSeDataActivity.this, R.string.message_data_input_error);
        });
    }

    @Override
    public void finish() {
        if ((null != baiSeInfoBean && !baiSeInfoBean.getProjectType().equals(rbProject.isChecked() ? ConstantUtils.ENTERPRISE_PROJECT : ConstantUtils.ENTERPRISE_CONTRACT))
                || !etCompany.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getBurstOrgName())
                || !etCode.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getBurstOrgCode())
                || !etId.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getIdCard())
                || !etName.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getBursterName())
                || !etProjectCode.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getProjectCode())
                || !etProjectName.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getProjectName())) {
            BaseApplication.customDialog(new AlertDialog.Builder(BaiSeDataActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.button_save, (dialog, which) -> findViewById(R.id.btn_save).callOnClick())
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> BaiSeDataActivity.super.finish())
                    .show(), true);
        } else
            super.finish();
    }
}