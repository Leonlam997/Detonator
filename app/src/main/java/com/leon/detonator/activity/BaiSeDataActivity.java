package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.component.MyButton;
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
    private CheckBox cbProject;
    private MyButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bai_se_data);
        setTitle(R.string.settings_modify_enterprise);
        myApp = (BaseApplication) getApplication();
        etName = findViewById(R.id.et_name);
        etId = findViewById(R.id.et_id);
        etCompany = findViewById(R.id.et_company);
        etCode = findViewById(R.id.et_code);
        etProjectName = findViewById(R.id.et_project_name);
        etProjectCode = findViewById(R.id.et_project_code);
        cbProject = findViewById(R.id.cb_project);
        cbProject.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                ((TextView) findViewById(R.id.txt_name)).setText(R.string.enterprise_project_name);
                ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_project_code);
            } else {
                ((TextView) findViewById(R.id.txt_name)).setText(R.string.enterprise_contract_name);
                ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_contract_code);
            }
        });
        findViewById(R.id.tv_project).setOnClickListener(view -> cbProject.setChecked(true));
        findViewById(R.id.tv_contract).setOnClickListener(view -> cbProject.setChecked(false));
        if (!getIntent().getBooleanExtra(KeyUtils.KEY_NEW_INFO, false))
            baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
        if (baiSeInfoBean != null) {
            etName.setText(baiSeInfoBean.getBursterName());
            etId.setText(baiSeInfoBean.getIdCard());
            etCompany.setText(baiSeInfoBean.getBurstOrgName());
            etCode.setText(baiSeInfoBean.getBurstOrgCode());
            etProjectCode.setText(baiSeInfoBean.getProjectCode());
            etProjectName.setText(baiSeInfoBean.getProjectName());
            cbProject.setChecked(ConstantUtils.ENTERPRISE_PROJECT.equals(baiSeInfoBean.getProjectType()));
        }
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
        btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(view -> {
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
                    if (baiSeInfoBean == null)
                        baiSeInfoBean = new BaiSeInfoBean();
                    baiSeInfoBean.setProjectType(cbProject.isChecked() ? ConstantUtils.ENTERPRISE_PROJECT : ConstantUtils.ENTERPRISE_CONTRACT);
                    baiSeInfoBean.setBursterName(etName.getText().toString());
                    baiSeInfoBean.setIdCard(etId.getText().toString());
                    baiSeInfoBean.setBurstOrgName(etCompany.getText().toString());
                    baiSeInfoBean.setBurstOrgCode(etCode.getText().toString());
                    baiSeInfoBean.setProjectCode(etProjectCode.getText().toString());
                    baiSeInfoBean.setProjectName(etProjectName.getText().toString());
                    baiSeInfoBean.setSelected(true);
                    DbUtil.updateBaiSeInfo(baiSeInfoBean);
                    BaiSeBlasterBean baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                    if (baiSeBlasterBean != null) {
                        baiSeBlasterBean.setChecked(false);
                        baiSeBlasterBean.setName(etName.getText().toString());
                        baiSeBlasterBean.getData().setUserIdCard(etId.getText().toString());
                        baiSeBlasterBean.getData().setProjectCode(etProjectCode.getText().toString());
                        DbUtil.updateBaiSeBlaster(baiSeBlasterBean);
                    }
                    setResult(RESULT_OK);
                    finish();
                }
                return;
            }
            myApp.myToast(BaiSeDataActivity.this, R.string.message_data_input_error);
        });
        etCompany.requestFocus();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F1 || keyCode == KeyEvent.KEYCODE_F2)
            cbProject.setChecked(keyCode == KeyEvent.KEYCODE_F1);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void finish() {
        if ((null != baiSeInfoBean && !baiSeInfoBean.getProjectType().equals(cbProject.isChecked() ? ConstantUtils.ENTERPRISE_PROJECT : ConstantUtils.ENTERPRISE_CONTRACT))
                || !etCompany.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getBurstOrgName())
                || !etCode.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getBurstOrgCode())
                || !etId.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getIdCard())
                || !etName.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getBursterName())
                || !etProjectCode.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getProjectCode())
                || !etProjectName.getText().toString().equals(null == baiSeInfoBean ? "" : baiSeInfoBean.getProjectName())) {
            BaseApplication.customDialog(new AlertDialog.Builder(BaiSeDataActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.button_confirm, (dialog, which) -> btnSave.callOnClick())
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> BaiSeDataActivity.super.finish())
                    .show());
        } else
            super.finish();
    }
}