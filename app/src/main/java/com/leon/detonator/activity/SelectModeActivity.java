package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;

public class SelectModeActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mode);

        findViewById(R.id.ib_open_air).setOnClickListener(this);
        findViewById(R.id.rl_open_air).setOnClickListener(this);
        findViewById(R.id.ib_tunnel).setOnClickListener(this);
        findViewById(R.id.rl_tunnel).setOnClickListener(this);
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

    private void enterApp(int mode) {
        switch (mode) {
            case 1:
                BaseApplication.isTunnel = false;
                break;
            case 2:
                BaseApplication.isTunnel = true;
                break;
            default:
                return;
        }
        startActivity(new Intent(SelectModeActivity.this, MainActivity.class));
        finish();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ib_open_air:
            case R.id.rl_open_air:
                enterApp(1);
                break;
            case R.id.ib_tunnel:
            case R.id.rl_tunnel:
                enterApp(2);
                break;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        enterApp(keyCode - KeyEvent.KEYCODE_0);
        return super.onKeyUp(keyCode, event);
    }
}
