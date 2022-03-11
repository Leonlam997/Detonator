package com.leon.detonator.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.leon.detonator.R;

public class SemiProductDialog extends Dialog {
    private final Context mContext;
    private final Handler autoCloseHandler = new Handler(message -> {
        dismiss();
        return false;
    });
    private int style = 0;
    private int titleId = 0;
    private int subtitleId = 0;
    private String code;
    private boolean autoClose = true;

    public SemiProductDialog(@NonNull Context context) {
        super(context, R.style.SemiProductDialog);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_dialog);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        ConstraintLayout layout = findViewById(R.id.cl_dialog);
        layout.measure(0, 0);
        lp.width = layout.getMeasuredWidth() + 15;
        lp.height = layout.getMeasuredHeight() + 20;
        getWindow().setAttributes(lp);
        if (style == 0) {
            findViewById(R.id.iv_dialog_text_back).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.tv_dialog_message)).setTextColor(mContext.getColor(R.color.colorSemiProductCommonText));
        } else {
            ((TextView) findViewById(R.id.tv_dialog_message)).setTextColor(mContext.getColor(R.color.colorDialogText));
            ((ImageView) findViewById(R.id.iv_dialog_text_back)).setImageResource(style == 1 || style == 3 ? R.mipmap.back_qualified : R.mipmap.back_unqualified);
        }
        if (titleId != 0)
            if (style >= 2) {
                findViewById(R.id.tv_dialog_message).setVisibility(View.INVISIBLE);
                ((TextView) findViewById(R.id.tv_dialog_title)).setText(titleId);
                if (subtitleId != 0)
                    ((TextView) findViewById(R.id.tv_dialog_subtitle)).setText(subtitleId);
                else
                    findViewById(R.id.tv_dialog_subtitle).setVisibility(style == 4 ? View.VISIBLE : View.INVISIBLE);
                if (null != code && !code.isEmpty())
                    ((TextView) findViewById(style == 2 ? R.id.tv_dialog_code : R.id.tv_dialog_subtitle)).setText(code);
            } else {
                ((TextView) findViewById(R.id.tv_dialog_message)).setText(titleId);
                findViewById(R.id.tv_dialog_title).setVisibility(View.INVISIBLE);
                findViewById(R.id.tv_dialog_code).setVisibility(View.INVISIBLE);
                findViewById(R.id.tv_dialog_subtitle).setVisibility(View.INVISIBLE);
            }

        //setCanceledOnTouchOutside(true);
        findViewById(R.id.btn_dialog_confirm).setOnClickListener(view -> dismiss());

        int AUTO_CLOSE_TIMEOUT = 5000;
        if (autoClose)
            autoCloseHandler.sendMessageDelayed(Message.obtain(), AUTO_CLOSE_TIMEOUT);
    }

    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setSubtitleId(int subtitleId) {
        this.subtitleId = subtitleId;
    }

    public void setTitleId(int titleId) {
        this.titleId = titleId;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
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
