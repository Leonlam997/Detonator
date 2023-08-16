package com.leon.detonator.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.leon.detonator.base.BaseApplication;

public class MyProgressDialog extends ProgressDialog {
    private int title = 0;

    public MyProgressDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            final WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = 330;
            params.height = 175 + BaseApplication.settings.getFontScale() * 10;
            getWindow().setAttributes(params);
        }
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        title = titleId;
    }

    @Override
    public void show() {
        super.show();
        if (title != 0)
            setText(getWindow().getDecorView(), 28);
    }

    private void setText(View view, int size) {
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++)
                setText(parent.getChildAt(i), size);
        } else if (view instanceof TextView) {
            TextView textview = (TextView) view;
            if (!textview.getText().equals(getContext().getString(title)))
                textview.setTextSize(size);
        }
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
