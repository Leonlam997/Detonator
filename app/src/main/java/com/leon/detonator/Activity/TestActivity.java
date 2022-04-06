package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;

import java.io.IOException;
import java.util.Locale;

public class TestActivity extends BaseActivity {
    private final int MAX_VOLTAGE = 22;
    private SerialPortUtil serialPortUtil;
    private EditText etVoltage;
    private long timeCounter;
    private SeekBar sbVoltage;
    private boolean successMsg;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        setTitle(R.string.det_up_down_vol);
        setProgressVisibility(true);
        timeCounter = System.currentTimeMillis();
        successMsg = false;
        enabledButton(false);
        myApp = (BaseApplication) getApplication();

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            serialPortUtil.setOnDataReceiveListener(buffer -> {
                String data = new String(buffer);
                //sendMsg(3, data);
                if (data.contains(SerialCommand.RESPOND_SUCCESS)) {
                    if (data.contains(SerialCommand.RESPOND_VOLTAGE)) {
                        if (data.contains("\r") && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_VOLTAGE))
                            setVoltage(Integer.parseInt(data.substring(data.indexOf(SerialCommand.RESPOND_VOLTAGE) + SerialCommand.RESPOND_VOLTAGE.length(), data.indexOf("\r"))) / 100.0f);
                        serialPortUtil.sendCmd(SerialCommand.CMD_READ_CURRENT);
                    } else if (data.contains(SerialCommand.RESPOND_CURRENT) && data.lastIndexOf("\r") > data.indexOf(SerialCommand.RESPOND_CURRENT)) {
                        data = data.substring(data.indexOf(SerialCommand.RESPOND_CURRENT) + SerialCommand.RESPOND_CURRENT.length(), data.lastIndexOf("\r"));
                        if (data.length() > 0) {
                            try {
                                int c = Integer.parseInt(data);
                                int count = (int) ((c + 2) / 3.4f) + 1;
                                float current = 0.0f;
                                if (c == 1)
                                    current = 25;
                                else if (c > 0 && count > 0)
                                    current = count * 25 + ((c + 2) - (count - 1) * 3.4f) / 3.4f;
                                setCurrent(current);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (successMsg) {
                            myApp.myToast(this, "命令返回成功！");
                            refreshStatusBar.sendEmptyMessage(2);
                            successMsg = false;
                        }
                    }
                } else if (data.contains(SerialCommand.INITIAL_FINISHED)) {
                    myApp.myToast(this, "初始化完成！(" + ((System.currentTimeMillis() - timeCounter) / 1000.0f) + "s)");
                    refreshStatusBar.sendEmptyMessage(2);
                    serialPortUtil.sendCmd(SerialCommand.CMD_DEBUG_ON);
                    refreshStatusBar.removeMessages(1);
                    refreshStatusBar.sendEmptyMessageDelayed(1, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                }
            });
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            myApp.myToast(this, R.string.message_open_module_fail);
            setResult(0, null);
            finish();
        }
        etVoltage = findViewById(R.id.etVoltage);
        etVoltage.setText("1870");
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
            final Handler sendDelay = new Handler(msg -> {
                successMsg = true;
                serialPortUtil.sendCmd(SerialCommand.CMD_ADJUST_CLOCK);
                return false;
            });
            sendDelay.sendEmptyMessageDelayed(1, 800);

        });

        findViewById(R.id.btn_boost_capacitor).setOnClickListener(v -> {
            if (etVoltage.getText().toString().isEmpty()) {
                myApp.myToast(this, "请输入数值！");
                return;
            }
            successMsg = true;
            enabledButton(false);
            if (!serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + etVoltage.getText().toString() + "###")) {
                myApp.myToast(this, "发送命令失败！");
            } else {
                refreshStatusBar.removeMessages(1);
                refreshStatusBar.sendEmptyMessageDelayed(1, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
            }
        });

        findViewById(R.id.btn_open_capacitor).setOnClickListener(v -> {
            //enabledButton(false);
            refreshStatusBar.removeMessages(1);
            successMsg = true;
            myApp.myToast(this, "发送打开电容指令！");
            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0);
            refreshStatusBar.removeMessages(1);
            refreshStatusBar.sendEmptyMessageDelayed(1, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
        });

        findViewById(R.id.btn_explode).setOnClickListener(v -> new AlertDialog.Builder(TestActivity.this, R.style.AlertDialog)
                .setMessage("是否确定起爆？")
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    successMsg = true;
                    enabledButton(false);
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.EXPLODE, 0);
                    refreshStatusBar.removeMessages(1);
                    refreshStatusBar.sendEmptyMessageDelayed(1, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                })
                .setNegativeButton("取消", null)
                .create()
                .show());

        sbVoltage = findViewById(R.id.sbVoltage);
        sbVoltage.setProgress(1870 / MAX_VOLTAGE);
        sbVoltage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                etVoltage.setText(String.format(Locale.CHINA, "%d", MAX_VOLTAGE * progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        findViewById(R.id.btn_increase).setOnClickListener(v -> {
            if (sbVoltage.getProgress() < 100)
                sbVoltage.setProgress(sbVoltage.getProgress() + 1);
        });
        findViewById(R.id.btn_decrease).setOnClickListener(v -> {
            if (sbVoltage.getProgress() > 0)
                sbVoltage.setProgress(sbVoltage.getProgress() - 1);
        });
    }    private Handler refreshStatusBar = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 1:
                    serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                    refreshStatusBar.removeMessages(1);
                    refreshStatusBar.sendEmptyMessageDelayed(1, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                    break;
                case 2:
                    enabledButton(true);
                    break;
            }
            return false;
        }
    });

    private void enabledButton(boolean enabled) {
        refreshStatusBar.removeMessages(1);
        if (enabled) {
            findViewById(R.id.btn_self_test).setEnabled(true);
            findViewById(R.id.btn_boost_capacitor).setEnabled(true);
            findViewById(R.id.btn_explode).setEnabled(true);
            setProgressVisibility(false);
            refreshStatusBar.sendEmptyMessageDelayed(1, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
        }
    }

    @Override
    public void onDestroy() {
        refreshStatusBar.removeCallbacksAndMessages(null);
        refreshStatusBar = null;
        serialPortUtil.closeSerialPort();
        super.onDestroy();
    }



}
