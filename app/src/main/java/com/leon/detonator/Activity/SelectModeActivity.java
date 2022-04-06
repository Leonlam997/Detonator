package com.leon.detonator.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.R;

public class SelectModeActivity extends AppCompatActivity {
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mode);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        findViewById(R.id.ib_open_air).setOnClickListener(v -> enterApp(1));
        findViewById(R.id.rl_open_air).setOnClickListener(v -> enterApp(1));
        findViewById(R.id.ib_tunnel).setOnClickListener(v -> enterApp(2));
        findViewById(R.id.rl_tunnel).setOnClickListener(v -> enterApp(2));
        findViewById(R.id.ib_open_air).requestFocus();
        myApp = (BaseApplication) getApplication();
//        findViewById(R.id.ib_pit).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                enterApp(3);
//            }
//        });

    }

    private void enterApp(int mode) {
        switch (mode) {
            case 1:
                myApp.setTunnel(false);
                break;
            case 2:
                myApp.setTunnel(true);
                break;
//            case 3:
//                myApp.setPit(true);
//                myApp.setTunnel(true);
//                break;
            default:
                return;
        }
        Intent intent = new Intent(SelectModeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        enterApp(keyCode - KeyEvent.KEYCODE_0);
        return super.onKeyUp(keyCode, event);
    }
}
