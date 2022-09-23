package com.leon.detonator.activity;

import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.R;
import com.leon.detonator.serial.SerialPortUtil;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CapacityTestActivity extends BaseActivity {
    private SerialPortUtil serialPortUtil;
    private SoundPool soundPool;
    private int soundSuccess, soundFail;
    private MyProgressDialog pDialog;
    private float averageCurrent;
    private BaseApplication myApp;
    private int timeCount;
    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capacity_test);

        setTitle(R.string.det_capacity);
        setProgressVisibility(true);

        myApp = (BaseApplication) getApplication();
        initSound();
        findViewById(R.id.btn_check).setOnClickListener(v -> {
            myHandler.sendEmptyMessage(8);
            myHandler.sendEmptyMessageDelayed(1, 2000);
        });

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            serialPortUtil.setOnDataReceiveListener(buffer -> {
                String data = new String(buffer);
                //myApp.myToast(CapacityTest.this, data);
            });
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(CapacityTestActivity.this, R.string.message_open_module_fail);
            finish();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_1) {
            myHandler.sendEmptyMessage(8);
            myHandler.sendEmptyMessageDelayed(1, 2000);
        }
        return super.onKeyUp(keyCode, event);
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
            if (0 == soundSuccess)
                myApp.myToast(this, R.string.message_media_load_error);
            soundFail = soundPool.load(this, R.raw.fail, 1);
            if (0 == soundFail)
                myApp.myToast(this, R.string.message_media_load_error);
        } else
            myApp.myToast(this, R.string.message_media_init_error);
    }

    private void setButtonEnabled(boolean enabled) {
        myHandler.removeMessages(4);
        if (enabled)
            myHandler.sendEmptyMessage(4);
        findViewById(R.id.btn_check).setEnabled(enabled);
        setProgressVisibility(!enabled);
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        if (null != serialPortUtil) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }


}