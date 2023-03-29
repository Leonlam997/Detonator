package com.leon.detonator.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.activity.DetonateStep1Activity;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.BaiSeCheck;
import com.leon.detonator.bean.BaiSeUpload;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;

public class EnterpriseDialog extends Dialog {
    private final BaseApplication myApp;
    private View.OnClickListener clickConfirm, clickModify;
    private final boolean checkDetector;
    private MyButton btnConfirm;

    public EnterpriseDialog(@NonNull Context context) {
        super(context);
        checkDetector = context instanceof DetonateStep1Activity;
        myApp = (BaseApplication) ((BaseActivity) context).getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_enterprise_info_dialog);
        if (getWindow() != null) {
            final WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = 380;
            params.height = 260;
            getWindow().setAttributes(params);
        }
        btnConfirm = findViewById(R.id.btn_dialog_confirm);
        if (BaseApplication.readSettings().getServerHost() == 2) {
            findViewById(R.id.ll_enterprise).setVisibility(View.INVISIBLE);
            if (checkDetector) {
                ((TextView) findViewById(R.id.txt_title)).setText(R.string.dialog_title_detector);
                findViewById(R.id.ll_detector).setVisibility(View.VISIBLE);
                findViewById(R.id.sv_content).setVisibility(View.INVISIBLE);
                BaiSeCheck baiSeCheck = myApp.readBaiSeCheck();
                if (baiSeCheck != null && baiSeCheck.getData() != null) {
                    if (baiSeCheck.getData().getUserIdCard().isEmpty())
                        btnConfirm.setEnabled(false);
                    else
                        ((TextView) findViewById(R.id.tv_detector_id)).setText(baiSeCheck.getData().getUserIdCard());
                    if (baiSeCheck.getData().getProjectCode().isEmpty())
                        btnConfirm.setEnabled(false);
                    else
                        ((TextView) findViewById(R.id.tv_detector_code)).setText(baiSeCheck.getData().getProjectCode());
                } else
                    btnConfirm.setEnabled(false);
            } else {
                findViewById(R.id.ll_detector).setVisibility(View.INVISIBLE);
                findViewById(R.id.sv_content).setVisibility(View.VISIBLE);
                BaiSeUpload baiSeUpload = myApp.readBaiSeUpload();
                if (null != baiSeUpload) {
                    if (baiSeUpload.getProjectType().equals(ConstantUtils.ENTERPRISE_CONTRACT)) {
                        ((TextView) findViewById(R.id.txt_name)).setText(R.string.enterprise_contract_name);
                        ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_contract_code);
                    }
                    ((TextView) findViewById(R.id.tv_name)).setText(baiSeUpload.getBursterName());
                    ((TextView) findViewById(R.id.tv_blaster_id)).setText(baiSeUpload.getIdCard());
                    ((TextView) findViewById(R.id.tv_company)).setText(baiSeUpload.getBurstOrgName());
                    ((TextView) findViewById(R.id.tv_company_code)).setText(baiSeUpload.getBurstOrgName());
                    ((TextView) findViewById(R.id.tv_project_code)).setText(baiSeUpload.getProjectCode());
                    ((TextView) findViewById(R.id.tv_project_name)).setText(baiSeUpload.getProjectName());
                } else
                    btnConfirm.setEnabled(false);
            }
        } else {
            findViewById(R.id.ll_detector).setVisibility(View.INVISIBLE);
            findViewById(R.id.sv_content).setVisibility(View.INVISIBLE);
            findViewById(R.id.ll_enterprise).setVisibility(View.VISIBLE);
            EnterpriseBean enterpriseBean = myApp.readEnterprise();
            if (null != enterpriseBean) {
                ((TextView) findViewById(R.id.tv_code)).setText(enterpriseBean.getCode());
                ((TextView) findViewById(R.id.tv_id)).setText(enterpriseBean.getId());
                findViewById(R.id.ll_commercial).setVisibility(enterpriseBean.isCommercial() ? View.VISIBLE : View.INVISIBLE);
                if (enterpriseBean.isCommercial()) {
                    ((TextView) findViewById(R.id.tv_contract)).setText(enterpriseBean.getContract());
                    ((TextView) findViewById(R.id.tv_project)).setText(enterpriseBean.getProject());
                }
            } else
                btnConfirm.setEnabled(false);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (null != getWindow()) {
            View decorView = getWindow().getDecorView();
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void setClickConfirm(View.OnClickListener listener) {
        this.clickConfirm = listener;
        new Handler(message -> {
            btnConfirm.setOnClickListener(clickConfirm);
            return false;
        }).sendEmptyMessage(1);
    }

    public void setClickModify(View.OnClickListener listener) {
        this.clickModify = listener;
        new Handler(message -> {
            findViewById(R.id.btn_dialog_modify).setOnClickListener(clickModify);
            return false;
        }).sendEmptyMessage(1);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                if (clickConfirm != null && btnConfirm.isEnabled())
                    clickConfirm.onClick(btnConfirm);
                break;
            case KeyEvent.KEYCODE_2:
                if (clickModify != null)
                    clickModify.onClick(findViewById(R.id.btn_dialog_modify));
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

}
