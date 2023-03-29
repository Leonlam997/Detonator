package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeCheck;
import com.leon.detonator.bean.BaiSeUpload;
import com.leon.detonator.util.ConstantUtils;

import java.util.regex.Pattern;

public class BaiSeDetectorActivity extends BaseActivity {
    private BaseApplication myApp;
    private BaiSeCheck baiSeCheck;
    private EditText etId;
    private EditText etCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bai_se_detector);
        setTitle(R.string.bai_se_detector);
        myApp = (BaseApplication) getApplication();
        BaiSeUpload baiSeUpload = myApp.readBaiSeUpload();
        baiSeCheck = myApp.readBaiSeCheck();
        if (baiSeUpload.getProjectType().equals(ConstantUtils.ENTERPRISE_CONTRACT)) {
            ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_contract_code);
        } else {
            ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_project_code);

        }
        etId = findViewById(R.id.et_id);
        etCode = findViewById(R.id.et_code);
        if (baiSeCheck != null && baiSeCheck.getData() != null) {
            if (!baiSeCheck.getData().getProjectCode().isEmpty())
                etCode.setText(baiSeCheck.getData().getProjectCode());
            else if (!baiSeUpload.getProjectCode().isEmpty())
                etCode.setText(baiSeUpload.getProjectCode());
            if (!baiSeCheck.getData().getUserIdCard().isEmpty())
                etId.setText(baiSeCheck.getData().getUserIdCard());
        }
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
            } else if (etCode.getText() == null || etCode.getText().toString().isEmpty()) {
                etCode.requestFocus();
            } else {
                if (!Pattern.matches(ConstantUtils.ID_PATTERN, etId.getText().toString())) {
                    myApp.myToast(BaiSeDetectorActivity.this, R.string.message_input_id_error);
                    etId.requestFocus();
                } else {
                    if (baiSeCheck == null)
                        baiSeCheck = new BaiSeCheck();
                    baiSeCheck.getData().setProjectCode(etCode.getText().toString());
                    baiSeCheck.getData().setUserIdCard(etId.getText().toString());
                    myApp.saveBean(baiSeCheck);
                    finish();
                }
                return;
            }
            myApp.myToast(BaiSeDetectorActivity.this, R.string.message_data_input_error);
        });
        findViewById(R.id.btn_clear).setOnClickListener(view -> {
            etCode.setText("");
            etId.setText("");
        });
    }

    @Override
    public void finish() {
        if (!etCode.getText().toString().equals(null == baiSeCheck ? "" : baiSeCheck.getData().getProjectCode())
                || !etId.getText().toString().equals(null == baiSeCheck ? "" : baiSeCheck.getData().getUserIdCard())) {
            new AlertDialog.Builder(BaiSeDetectorActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> BaiSeDetectorActivity.super.finish())
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
}