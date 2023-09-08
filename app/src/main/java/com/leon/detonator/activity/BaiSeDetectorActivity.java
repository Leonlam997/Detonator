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
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.regex.Pattern;

public class BaiSeDetectorActivity extends BaseActivity {
    private BaseApplication myApp;
    private BaiSeBlasterBean baiSeBlasterBean;
    private EditText etId;
    private EditText etName;
    private MyButton btnSave;
    private MyButton btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bai_se_detector);
        setTitle(R.string.bai_se_modify_detector);
        myApp = (BaseApplication) getApplication();

        if (getIntent().getBooleanExtra(KeyUtils.KEY_NEW_INFO, false))
            baiSeBlasterBean = new BaiSeBlasterBean();
        else
            baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
        BaiSeInfoBean baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
        if (baiSeInfoBean == null || baiSeBlasterBean == null) {
            myApp.myToast(BaiSeDetectorActivity.this, R.string.message_select_enterprise);
            finish();
            return;
        }
        etId = findViewById(R.id.et_id);
        etName = findViewById(R.id.et_name);
        etName.requestFocus();
        if (baiSeBlasterBean.getData() != null) {
            if (!baiSeBlasterBean.getName().isEmpty())
                etName.setText(baiSeBlasterBean.getName());
            if (!baiSeBlasterBean.getData().getUserIdCard().isEmpty())
                etId.setText(baiSeBlasterBean.getData().getUserIdCard());
        }
        ((TextView) findViewById(R.id.tv_company)).setText(baiSeInfoBean.getBurstOrgName());
        ((TextView) findViewById(R.id.txt_code)).setText(ConstantUtils.ENTERPRISE_PROJECT.equals(baiSeInfoBean.getProjectType()) ? R.string.enterprise_project_name : R.string.enterprise_contract_name);
        ((TextView) findViewById(R.id.tv_code)).setText(baiSeInfoBean.getProjectName());
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
        btnSave =findViewById(R.id.btn_save);
        btnSave.setOnClickListener(view -> {
            if (etId.getText() == null || etId.getText().toString().isEmpty()) {
                etId.requestFocus();
            } else if (etName.getText() == null || etName.getText().toString().isEmpty()) {
                etName.requestFocus();
            } else {
                if (!Pattern.matches(ConstantUtils.ID_PATTERN, etId.getText().toString())) {
                    myApp.myToast(BaiSeDetectorActivity.this, R.string.message_input_id_error);
                    etId.requestFocus();
                } else {
                    if (baiSeBlasterBean == null)
                        baiSeBlasterBean = new BaiSeBlasterBean();
                    baiSeBlasterBean.setName(etName.getText().toString());
                    baiSeBlasterBean.getData().setUserIdCard(etId.getText().toString());
                    baiSeBlasterBean.getData().setProjectCode(baiSeInfoBean.getProjectCode());
                    baiSeBlasterBean.getData().setBurstOrgCode(baiSeInfoBean.getBurstOrgCode());
                    baiSeBlasterBean.setSelected(true);
                    DbUtil.updateBaiSeBlaster(baiSeBlasterBean);
                    setResult(RESULT_OK);
                    finish();
                }
                return;
            }
            myApp.myToast(BaiSeDetectorActivity.this, R.string.message_data_input_error);
        });
        btnClear =findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(view -> {
            etName.setText("");
            etId.setText("");
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F1)
            btnSave.callOnClick();
        else if (keyCode == KeyEvent.KEYCODE_F2)
            btnClear.callOnClick();
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void finish() {
        if (!etName.getText().toString().equals(null == baiSeBlasterBean ? "" : baiSeBlasterBean.getName())
                || !etId.getText().toString().equals(null == baiSeBlasterBean ? "" : baiSeBlasterBean.getData().getUserIdCard())) {
            BaseApplication.customDialog(new AlertDialog.Builder(BaiSeDetectorActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_abort_modify)
                    .setMessage(R.string.dialog_exit_modify)
                    .setPositiveButton(R.string.button_confirm, (dialog, which) -> btnSave.callOnClick())
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> BaiSeDetectorActivity.super.finish())
                    .show());
        } else
            super.finish();
    }
}