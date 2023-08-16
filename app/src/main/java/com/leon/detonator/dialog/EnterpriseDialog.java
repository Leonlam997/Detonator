package com.leon.detonator.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;

public class EnterpriseDialog extends Dialog {
    private boolean checkBlaster;
    private MyButton btnConfirm;
    private MyButton btnModify;

    public EnterpriseDialog(@NonNull Context context) {
        super(context);
    }

    public EnterpriseDialog(@NonNull Context context, boolean checkBlaster) {
        super(context);
        this.checkBlaster = checkBlaster;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_dialog_enterprise_info);
        if (getWindow() != null) {
            final WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = 380;
            params.height = 260;
            getWindow().setAttributes(params);
        }
        btnConfirm = findViewById(R.id.btn_dialog_confirm);
        btnModify = findViewById(R.id.btn_dialog_modify);
        if (BaseApplication.settings.getServerHost() == 2) {
            findViewById(R.id.ll_enterprise).setVisibility(View.GONE);
            if (checkBlaster) {
                ((TextView) findViewById(R.id.txt_title)).setText(R.string.dialog_title_detector);
                findViewById(R.id.ll_detector).setVisibility(View.VISIBLE);
                findViewById(R.id.ll_project).setVisibility(View.GONE);
                BaiSeBlasterBean baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                if (baiSeBlasterBean != null && baiSeBlasterBean.getData() != null) {
                    ((TextView) findViewById(R.id.txt_project_code)).setText(baiSeBlasterBean.isProject() ? R.string.enterprise_project_code : R.string.enterprise_contract_code);
                    if (baiSeBlasterBean.getData().getUserIdCard().isEmpty())
                        btnConfirm.setEnabled(false);
                    else
                        ((TextView) findViewById(R.id.tv_detector_id)).setText(baiSeBlasterBean.getData().getUserIdCard());
                    if (baiSeBlasterBean.getData().getProjectCode().isEmpty())
                        btnConfirm.setEnabled(false);
                    else
                        ((TextView) findViewById(R.id.tv_detector_code)).setText(baiSeBlasterBean.getData().getProjectCode());
                    ((TextView) findViewById(R.id.tv_detector_name)).setText(baiSeBlasterBean.getName());
                    ((TextView) findViewById(R.id.tv_org_code)).setText(baiSeBlasterBean.getData().getBurstOrgCode());
                } else {
                    btnConfirm.setEnabled(false);
                    btnModify.setTextId(R.string.button_select);
                }
            } else {
                findViewById(R.id.ll_detector).setVisibility(View.GONE);
                findViewById(R.id.ll_project).setVisibility(View.VISIBLE);
                BaiSeInfoBean baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                if (null != baiSeInfoBean) {
                    if (baiSeInfoBean.getProjectType().equals(ConstantUtils.ENTERPRISE_CONTRACT)) {
                        ((TextView) findViewById(R.id.txt_name)).setText(R.string.enterprise_contract_name);
                        ((TextView) findViewById(R.id.txt_code)).setText(R.string.enterprise_contract_code);
                    }
                    ((TextView) findViewById(R.id.tv_name)).setText(baiSeInfoBean.getBursterName());
                    ((TextView) findViewById(R.id.tv_blaster_id)).setText(baiSeInfoBean.getIdCard());
                    ((TextView) findViewById(R.id.tv_company)).setText(baiSeInfoBean.getBurstOrgName());
                    ((TextView) findViewById(R.id.tv_company_code)).setText(baiSeInfoBean.getBurstOrgName());
                    ((TextView) findViewById(R.id.tv_project_code)).setText(baiSeInfoBean.getProjectCode());
                    ((TextView) findViewById(R.id.tv_project_name)).setText(baiSeInfoBean.getProjectName());
                } else {
                    btnConfirm.setEnabled(false);
                    btnModify.setTextId(R.string.button_select);
                }
            }
        } else {
            findViewById(R.id.ll_detector).setVisibility(View.GONE);
            findViewById(R.id.ll_project).setVisibility(View.GONE);
            findViewById(R.id.ll_enterprise).setVisibility(View.VISIBLE);
            EnterpriseBean enterpriseBean = DbUtil.getCurrentEnterprise();
            if (null != enterpriseBean) {
                ((TextView) findViewById(R.id.tv_code)).setText(enterpriseBean.getCode());
                ((TextView) findViewById(R.id.tv_id)).setText(enterpriseBean.getBlasterId());
                findViewById(R.id.ll_commercial).setVisibility(enterpriseBean.isCommercial() ? View.VISIBLE : View.GONE);
                if (enterpriseBean.isCommercial()) {
                    ((TextView) findViewById(R.id.tv_contract)).setText(enterpriseBean.getContract());
                    ((TextView) findViewById(R.id.tv_project)).setText(enterpriseBean.getProject());
                }
            } else {
                btnConfirm.setEnabled(false);
                btnModify.setTextId(R.string.button_select);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_1 && btnConfirm.isEnabled())
            btnConfirm.callOnClick();
        else if (keyCode == KeyEvent.KEYCODE_2)
            btnModify.callOnClick();
        return super.onKeyUp(keyCode, event);
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
}
