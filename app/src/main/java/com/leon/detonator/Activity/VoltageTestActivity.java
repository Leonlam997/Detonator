package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.SeekBar;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class VoltageTestActivity extends BaseActivity {
    private SerialPortUtil serialPortUtil;
    private EditText etVoltage;
    private SeekBar sbVoltage;
    private SerialDataReceiveListener myReceiveListener;
    private int tempDac;    private final Handler delayCmdHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    serialPortUtil.sendCmd(SerialCommand.CMD_ADJUST_CLOCK);
                    break;
                case 2:
                    serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                    delayCmdHandler.sendEmptyMessageDelayed(2, 500);
                    break;
                case 3:
                    myReceiveListener.setStartAutoDetect(true);
                    break;
            }
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
                String received = myReceiveListener.getRcvData();
                if (received.contains(SerialCommand.INITIAL_FAIL)) {
                    myApp.myToast(VoltageTestActivity.this, R.string.message_open_module_fail);
                    finish();
                    myReceiveListener.setRcvData("");
                } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                    enabledButton(true);
                    myReceiveListener.setRcvData("");
                    myReceiveListener.setStartAutoDetect(true);
                } else if (received.startsWith("V")) {
                    try {
                        float v = Integer.parseInt(received.substring(1)) / 100.0f,
                                workVoltage = Float.parseFloat(etVoltage.getText().toString().replace("V", ""));
                        delayCmdHandler.removeMessages(2);

                        if (Math.abs(v - workVoltage) < 0.1f) {
                            myReceiveListener.setFeedback(false);
                            myReceiveListener.setStartAutoDetect(true);
                            settingBean.getDacMap().put(workVoltage, tempDac);
                            runOnUiThread(() -> {
                                myApp.myToast(VoltageTestActivity.this, R.string.message_modify_finished);
                                enabledButton(true);
                            });
                            myApp.saveSettings(settingBean);
                        } else {
                            tempDac += (v > workVoltage ? 100 : -100) * (Math.abs(v - workVoltage) - 0.01f);
                            if (tempDac < 50 || tempDac > 4000) {
                                tempDac = 94 + (int) ((29 - workVoltage) * 124);
                            }
                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + tempDac + "###");
                            delayCmdHandler.sendEmptyMessageDelayed(2, 500);
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                    myReceiveListener.setRcvData("");
                }
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
            /*
            successMsg = true;
            enabledButton(false);
            if (!serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0)) {
                sendMsg(3, "发送命令失败！");
            } else {
                Message msg = Message.obtain();
                msg.what = 1;
                delayCmdHandler.sendMessageDelayed(msg, 1000);
            }
            */
            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.ADJUST_CLOCK, 0);
            delayCmdHandler.sendEmptyMessageDelayed(1, 800);

        });

        findViewById(R.id.btn_boost_capacitor).setOnClickListener(v -> {
            enabledButton(false);
            myReceiveListener.setStartAutoDetect(false);
            myReceiveListener.setFeedback(true);
            myReceiveListener.setRcvData("");
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
            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + tempDac + "###");
            delayCmdHandler.sendEmptyMessageDelayed(2, 200);
        });

        findViewById(R.id.btn_open_capacitor).setOnClickListener(v -> {
            //enabledButton(false);
            myApp.myToast(VoltageTestActivity.this, R.string.button_start_charge);
            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0);
        });

        findViewById(R.id.btn_explode).setOnClickListener(v -> new AlertDialog.Builder(VoltageTestActivity.this, R.style.AlertDialog)
                .setMessage(R.string.dialog_confirm_explode)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.NEW_EXPLODE, 0);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
                .show());

        sbVoltage = findViewById(R.id.sbVoltage);
        sbVoltage.setMax(140);
        sbVoltage.setProgress(100);
        sbVoltage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                etVoltage.setText(String.format(Locale.CHINA, "%.1fV", (progress + 100) / 10.0f));
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
                etVoltage.setText(String.format(Locale.CHINA, "%.1fV", (sbVoltage.getProgress() + 100) / 10.0f));
            }
        });
        findViewById(R.id.btn_decrease).setOnClickListener(v -> {
            if (sbVoltage.getProgress() > 0) {
                sbVoltage.setProgress(sbVoltage.getProgress() - 1);
                etVoltage.setText(String.format(Locale.CHINA, "%.1fV", (sbVoltage.getProgress() + 100) / 10.0f));
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
