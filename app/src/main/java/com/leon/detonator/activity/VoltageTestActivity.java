package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.SeekBar;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class VoltageTestActivity extends BaseActivity {
    private SerialPortUtil serialPortUtil;
    private EditText etVoltage;
    private SeekBar sbVoltage;
    private SerialDataReceiveListener myReceiveListener;
    private int tempDac;
    private final Handler delayCmdHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });
    private LocalSettingBean settingBean;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        setTitle(R.string.det_up_down_vol);
        enabledButton(false);
        settingBean = BaseApplication.readSettings();
        myApp = (BaseApplication) getApplication();

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(this, () -> {
            });
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(VoltageTestActivity.this, R.string.message_open_module_fail);
            finish();
        }
        etVoltage = findViewById(R.id.etVoltage);
        etVoltage.setEnabled(false);
        etVoltage.setText("20.0V");
        findViewById(R.id.btn_self_test).setOnClickListener(v -> {
            delayCmdHandler.sendEmptyMessageDelayed(1, 800);
        });

        findViewById(R.id.btn_boost_capacitor).setOnClickListener(v -> {
            enabledButton(false);
            myReceiveListener.setStartAutoDetect(false);
            if (null == settingBean.getDacMap())
                settingBean.setDacMap(new HashMap<>());
            try {
                float vol = Float.parseFloat(etVoltage.getText().toString().replace("V", ""));
                Integer d = settingBean.getDacMap().get(vol);
                if (null != d) {
                    tempDac = d;
                } else {
                    tempDac = 94 + (int) ((29 - vol) * 124);
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            delayCmdHandler.sendEmptyMessageDelayed(2, 200);
        });

        findViewById(R.id.btn_open_capacitor).setOnClickListener(v -> {
            //enabledButton(false);
            myApp.myToast(VoltageTestActivity.this, R.string.button_start_charge);
        });

        findViewById(R.id.btn_explode).setOnClickListener(v -> BaseApplication.customDialog(new AlertDialog.Builder(VoltageTestActivity.this, R.style.AlertDialog)
                .setMessage(R.string.dialog_confirm_explode)
                .setPositiveButton(R.string.btn_confirm, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .show(), true));

        sbVoltage = findViewById(R.id.sbVoltage);
        sbVoltage.setMax(140);
        sbVoltage.setProgress(100);
        sbVoltage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                etVoltage.setText(String.format(Locale.getDefault(), "%.1fV", (progress + 100) / 10.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        findViewById(R.id.btn_increase).setOnClickListener(v -> {
            if (sbVoltage.getProgress() < 140) {
                sbVoltage.setProgress(sbVoltage.getProgress() + 1);
                etVoltage.setText(String.format(Locale.getDefault(), "%.1fV", (sbVoltage.getProgress() + 100) / 10.0f));
            }
        });
        findViewById(R.id.btn_decrease).setOnClickListener(v -> {
            if (sbVoltage.getProgress() > 0) {
                sbVoltage.setProgress(sbVoltage.getProgress() - 1);
                etVoltage.setText(String.format(Locale.getDefault(), "%.1fV", (sbVoltage.getProgress() + 100) / 10.0f));
            }
        });
    }

    private void enabledButton(boolean enabled) {
        setProgressVisibility(!enabled);
        findViewById(R.id.btn_self_test).setEnabled(enabled);
        findViewById(R.id.btn_boost_capacitor).setEnabled(enabled);
        findViewById(R.id.btn_explode).setEnabled(enabled);
        findViewById(R.id.btn_open_capacitor).setEnabled(enabled);
    }

    @Override
    public void onDestroy() {
        delayCmdHandler.removeCallbacksAndMessages(null);
        myReceiveListener.closeAllHandler();
        serialPortUtil.closeSerialPort();
        super.onDestroy();
    }
}
