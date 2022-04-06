package com.leon.detonator.Activity;

import android.app.ProgressDialog;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.TextView;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Dialog.MyProgressDialog;
import com.leon.detonator.Dialog.SemiProductDialog;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

public class CapacityTestActivity extends BaseActivity {
    private SerialPortUtil serialPortUtil;
    private SoundPool soundPool;
    private int soundSuccess, soundFail;
    private MyProgressDialog pDialog;
    private float averageCurrent;
    private BaseApplication myApp;
    private int timeCount;    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case 1:
                    if (!serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.INSTANT_OPEN_CAPACITOR, 0)) {
                        setButtonEnabled(true);
                        myApp.myToast(CapacityTestActivity.this, R.string.message_send_command_fail);
                    } else {
                        averageCurrent = -1;
                        myHandler.sendEmptyMessageDelayed(2, 8000);
                        myHandler.removeMessages(4);
                        myHandler.sendEmptyMessageDelayed(4, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                    }
                    break;
                case 2:
                    averageCurrent = 0;
                    myHandler.sendEmptyMessageDelayed(3, 2500);
                    break;
                case 3:
                    myApp.myToast(CapacityTestActivity.this, "平均电流：" + averageCurrent);
                    setButtonEnabled(true);
                    SemiProductDialog myDialog = new SemiProductDialog(CapacityTestActivity.this);
                    if (averageCurrent > 80 || averageCurrent <= 0) {
                        myApp.playSoundVibrate(soundPool, soundFail);
                        myDialog.setStyle(2);
                        myDialog.setAutoClose(false);
                        myDialog.setTitleId(R.string.dialog_unqualified);
                        myDialog.setCanceledOnTouchOutside(false);
                    } else {
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                        myDialog.setStyle(3);
                        myDialog.setAutoClose(true);
                        myDialog.setTitleId(R.string.dialog_qualified);
                        myHandler.sendEmptyMessageDelayed(7, 2000);
                    }
                    myDialog.show();
                    break;
                case 4:
                    myHandler.removeMessages(4);
                    myHandler.sendEmptyMessageDelayed(4, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                    serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                    break;
                case 5:
                    ((TextView) findViewById(R.id.tv_voltage)).setText((String) msg.obj);
                    break;
                case 6:
                    ((TextView) findViewById(R.id.tv_current)).setText((String) msg.obj);
                    break;
                case 7:
                    if (pDialog.isShowing())
                        pDialog.dismiss();
                    setButtonEnabled(true);
                    break;
                case 8:
                    setButtonEnabled(false);
                    break;
            }
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
        pDialog = new MyProgressDialog(this);
        pDialog.setInverseBackgroundForced(false);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCancelable(false);
        pDialog.setTitle(R.string.progress_title);
        pDialog.setMessage(getResources().getString(R.string.progress_text));
        pDialog.setMax(100);
        pDialog.setProgress(0);
        pDialog.show();
        progressHandler.sendEmptyMessageDelayed(1, 100);

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            serialPortUtil.setOnDataReceiveListener(buffer -> {
                String data = new String(buffer);
                //myApp.myToast(CapacityTest.this, data);
                if (data.contains(SerialCommand.RESPOND_SUCCESS)) {
                    if (data.contains(SerialCommand.RESPOND_VOLTAGE)) {
                        if (data.contains("\r") && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_VOLTAGE)) {
                            Message m = myHandler.obtainMessage(5);
                            m.obj = (Integer.parseInt(data.substring(data.indexOf(SerialCommand.RESPOND_VOLTAGE) + SerialCommand.RESPOND_VOLTAGE.length(), data.indexOf("\r"))) / 100.0f)
                                    + "V";
                            myHandler.sendMessage(m);
                        }
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
                                Message m = myHandler.obtainMessage(6);
                                if (current >= 1000)
                                    m.obj = String.format(Locale.CHINA, "%.2fmA", current / 1000);
                                else
                                    m.obj = String.format(Locale.CHINA, "%.2fμA", current);
                                myHandler.sendMessage(m);
                                if (current != 0) {
                                    if (0 == averageCurrent) {
                                        averageCurrent = current;
                                    } else if (averageCurrent > 0) {
                                        averageCurrent = (averageCurrent + current) / 2;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if (data.contains(SerialCommand.INITIAL_FINISHED)) {
                    if (!serialPortUtil.sendCmd(SerialCommand.CMD_DEBUG_ON)) {
                        myApp.myToast(CapacityTestActivity.this, R.string.message_send_command_fail);
                    }
                    myHandler.sendEmptyMessage(7);
                    myHandler.removeMessages(4);
                    myHandler.sendEmptyMessage(4);
                }
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
    }    private final Handler progressHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            timeCount += 100;
            if (timeCount < ConstantUtils.INITIAL_TIME - 100) {
                pDialog.setProgress(100 * timeCount / ConstantUtils.INITIAL_TIME);
            }
            if (pDialog.isShowing()) {
                if (timeCount > ConstantUtils.INITIAL_TIME) {
                    setButtonEnabled(true);
                    pDialog.dismiss();
                    myHandler.removeMessages(4);
                    myHandler.sendEmptyMessage(4);
                } else {
                    progressHandler.sendEmptyMessageDelayed(0, 100);
                }
            }
            return false;
        }
    });

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