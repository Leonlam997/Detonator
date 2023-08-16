package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.widget.EditText;
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

public class BaiSeDetectorActivity extends BaseActivity {
    private BaiSeBlasterBean baiSeBlasterBean;
    private EditText etId;
    private EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bai_se_detector);
        setTitle(R.string.bai_se_detector);
        BaiSeInfoBean baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
        etId = findViewById(R.id.et_id);
        etName = findViewById(R.id.et_name);
        if (baiSeInfoBean == null) {
            myApp.myToast(this, R.string.message_select_enterprise);
            finish();
            return;
        }
        boolean project = baiSeInfoBean.getProjectType().equals(ConstantUtils.ENTERPRISE_PROJECT);
        ((TextView) findViewById(R.id.tv_project_code)).setText(String.format("%s%s",
                getString(project ? R.string.enterprise_project_code : R.string.enterprise_contract_code), baiSeInfoBean.getProjectCode()));
        ((TextView) findViewById(R.id.tv_company_name)).setText(String.format("%s%s", getString(R.string.enterprise_name), baiSeInfoBean.getBurstOrgName()));
        if (getIntent().getBooleanExtra(KeyUtils.KEY_NEW_INFO, false)) {
            baiSeBlasterBean = new BaiSeBlasterBean();
            baiSeBlasterBean.setId(-1);
            baiSeBlasterBean.setSelected(true);
        } else
            baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
        etName.requestFocus();
        if (baiSeBlasterBean != null && baiSeBlasterBean.getData() != null) {
            if (!baiSeBlasterBean.getData().getProjectCode().isEmpty())
                etName.setText(baiSeBlasterBean.getName());
            if (!baiSeBlasterBean.getData().getUserIdCard().isEmpty())
                etId.setText(baiSeBlasterBean.getData().getUserIdCard());
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
                if (editable.toString().contains(".")) {
                    editable.replace(editable.toString().indexOf("."), editable.toString().indexOf(".") + 1, "X");
                }
            }
        });
        findViewById(R.id.btn_save).setOnClickListener(view -> {
            if (etId.getText() == null || etId.getText().toString().isEmpty()) {
                etId.requestFocus();
            } else if (etName.getText() == null || etName.getText().toString().isEmpty()) {
                etName.requestFocus();
            } else {
                if (!Pattern.matches(ConstantUtils.ID_PATTERN, etId.getText().toString())) {
                    myApp.myToast(BaiSeDetectorActivity.this, R.string.message_input_id_error);
                    etId.requestFocus();
                } else {
                    baiSeBlasterBean.setSelected(true);
                    baiSeBlasterBean.setName(etName.getText().toString());
                    baiSeBlasterBean.getData().setUserIdCard(etId.getText().toString());
                    baiSeBlasterBean.getData().setProjectCode(baiSeInfoBean.getProjectCode());
                    baiSeBlasterBean.getData().setBurstOrgCode(baiSeInfoBean.getBurstOrgCode());
                    baiSeBlasterBean.setProject(project);
                    DbUtil.updateBaiSeBlaster(baiSeBlasterBean);
                    setResult(RESULT_OK);
                    finish();
                }
                return;
            }
            myApp.myToast(BaiSeDetectorActivity.this, R.string.message_data_input_error);
        });
        findViewById(R.id.btn_clear).setOnClickListener(view -> {
            etName.setText("");
            etId.setText("");
        });
    }

    @Override
    public void finish() {
        if (!etName.getText().toString().equals(null == baiSeBlasterBean ? "" : baiSeBlasterBean.getName())
                || !etId.getText().toString().equals(null == baiSeBlasterBean ? "" : baiSeBlasterBean.getData().getUserIdCard())) {
            BaseApplication.customDialog(new AlertDialog.Builder(BaiSeDetectorActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.button_save, (dialog, which) -> findViewById(R.id.btn_save).callOnClick())
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> BaiSeDetectorActivity.super.finish())
                    .show(), true);
        } else
            super.finish();
    }
}