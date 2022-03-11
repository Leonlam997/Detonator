package com.leon.detonator.Dialog;

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

import com.leon.detonator.Bean.EnterpriseBean;
import com.leon.detonator.R;

public class EnterpriseDialog extends Dialog {
    private final EnterpriseBean enterpriseBean;
    private View.OnClickListener clickConfirm, clickModify;

    public EnterpriseDialog(@NonNull Context context, EnterpriseBean bean) {
        super(context);
        enterpriseBean = bean;
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
        if (null != enterpriseBean) {
            ((TextView) findViewById(R.id.tv_code)).setText(enterpriseBean.getCode());
            ((TextView) findViewById(R.id.tv_id)).setText(enterpriseBean.getId());
            findViewById(R.id.rl_commercial).setVisibility(enterpriseBean.isCommercial() ? View.VISIBLE : View.INVISIBLE);
            if (enterpriseBean.isCommercial()) {
                ((TextView) findViewById(R.id.tv_contract)).setText(enterpriseBean.getContract());
                ((TextView) findViewById(R.id.tv_project)).setText(enterpriseBean.getProject());
            }
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
            findViewById(R.id.btn_dialog_confirm).setOnClickListener(clickConfirm);
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
                if (clickConfirm != null)
                    clickConfirm.onClick(findViewById(R.id.btn_dialog_confirm));
                break;
            case KeyEvent.KEYCODE_2:
                if (clickModify != null)
                    clickModify.onClick(findViewById(R.id.btn_dialog_modify));
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

}
