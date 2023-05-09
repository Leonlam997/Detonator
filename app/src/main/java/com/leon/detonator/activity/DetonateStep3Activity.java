package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.SliderImageView;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.LocationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DetonateStep3Activity extends BaseActivity {
    private final int COUNT_TIME = 1000;
    private final int STEP_DELAY = 1;
    private final int STEP_CHECK_PSW = 2;
    private final int STEP_READ_STATUS = 3;
    private final int STEP_CHARGE = 4;
    private final int STEP_EXPLODE = 5;
    private final int STEP_CLOSE_BUS = 6;
    private final int STEP_WAIT = 7;
    private final int STEP_RELEASE_1 = 9;
    private final int STEP_RELEASE_2 = 10;
    private final int STEP_RELEASE_3 = 11;
    private final int STEP_RESET_1 = 12;
    private final int STEP_RESET_2 = 13;
    private final int STEP_RESET_3 = 14;
    private final int STEP_EXIT_CLOSE_BUS = 15;
    private final int STEP_LOWER_VOLTAGE = 16;
    private final int STEP_SCAN = 17;
    private final int STEP_INITIAL = 18;
    private final int STEP_UPPER_VOLTAGE = 19;
    private final int STEP_EXPLODE_2 = 20;
    private final int STEP_EXPLODE_3 = 21;
    private final int STEP_LOWER_VOLTAGE_2 = 22;
    private final int STEP_CHECK_STATUS = 23;
    private final int STEP_CHECK_ONLINE = 24;
    private final int HANDLER_PRESS_KEY = 1;
    private final int HANDLER_CHARGE = 2;
    private final int HANDLER_SEND_COMMAND = 3;
    private final int HANDLER_NEXT_STEP = 4;
    private final int HANDLER_PROGRESS = 5;
    private TextView tvHint;
    private AlphaAnimation hide;
    private AlphaAnimation show;
    private MyProgressDialog pDialog;
    private boolean keepingEnd;
    private boolean confirmExplode;
    private boolean nextStep;
    private boolean notAllow;
    private boolean uniteExplode;
    private boolean rKeyDown;
    private boolean bKeyDown;
    private boolean bothKeyUp;
    private boolean doubleSend;
    private boolean chargeFinished = false;
    private boolean notCheckStatus = true;
    private int changeAction = 0;   //0：正常，1：关屏，2：开屏
    private int countDown;
    private int chargeTime;
    private int delayTime;
    private int explodeStep;
    private int soundSuccess;
    private int soundAlert;
    private int countScanZero;
    private long keyUpPeriod;
    private long startExplodeTime;
    private TextView tvPercentage, tvChargePercentage;
    private ProgressBar progressBar, pbCharge;
    private SliderImageView sliderImageView;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private List<DetonatorInfoBean> detonatorList;
    private SoundPool soundPool;
    private DownloadDetonatorBean offlineBean;
    private BaseApplication myApp;
    private EditText tvLog;

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case HANDLER_PRESS_KEY:
                    if (countDown < COUNT_TIME / 100) {
                        countDown++;
                        int percent = countDown * 10000 / COUNT_TIME;
                        tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
                        progressBar.setProgress(percent);
                        myHandler.removeMessages(HANDLER_PRESS_KEY);
                        myHandler.sendEmptyMessageDelayed(HANDLER_PRESS_KEY, 100);
                    } else {
                        confirmExplode = true;
                        myHandler.removeMessages(HANDLER_PRESS_KEY);
                        tvHint.setText(rKeyDown ? R.string.det_cancel_key : R.string.det_cancel_hint);
                    }
                    break;
                case HANDLER_CHARGE:
                    myHandler.removeMessages(HANDLER_CHARGE);
                    if (countDown < chargeTime / 100) {
                        countDown++;
                        int percent = countDown * 10000 / chargeTime;
                        tvChargePercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
                        pbCharge.setProgress(percent);
                        myHandler.sendEmptyMessageDelayed(HANDLER_CHARGE, 100);
                        if (countDown * 100 > chargeTime - 5000 && notCheckStatus) {
                            myHandler.removeMessages(HANDLER_NEXT_STEP);
                            myHandler.removeMessages(HANDLER_SEND_COMMAND);
                            explodeStep = STEP_LOWER_VOLTAGE_2;
                            myReceiveListener.setStartAutoDetect(false);
                            myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
                            notCheckStatus = false;
                        }
                    } else {
                        tvChargePercentage.setText("0%");
                        pbCharge.setProgress(0);
                        chargeFinished = true;
                        myReceiveListener.setDetonatorAmount(detonatorList.size());
                        myReceiveListener.setStartDetectShort(true);
                        BaseApplication.writeFile(getString(R.string.message_charge_success));
                        if (changeAction == 0)
                            changeActionBar();
                        else if (changeAction == 1)
                            changeAction = 2;
                        countDown = 0;
                    }
                    break;
                case HANDLER_PROGRESS:
                    myHandler.removeMessages(HANDLER_PROGRESS);
                    if (pDialog.getProgress() < pDialog.getMax() - 1) {
                        pDialog.incrementProgressBy(1);
                        myHandler.sendEmptyMessageDelayed(HANDLER_PROGRESS, 50);
                    }
                    break;
                case HANDLER_NEXT_STEP:
                    switch (explodeStep) {
                        case STEP_CHECK_PSW:
                            explodeStep = STEP_DELAY;
                            break;
                        case STEP_DELAY:
                            if (doubleSend) {
                                explodeStep = STEP_CHECK_PSW;
                                doubleSend = false;
                            } else {
                                explodeStep = STEP_CHARGE;
                            }
                            break;
                        case STEP_UPPER_VOLTAGE:
                        case STEP_CHARGE:
                            explodeStep = STEP_WAIT;
                            break;
                        case STEP_READ_STATUS:
                            explodeStep = STEP_EXPLODE;
                            break;
                        case STEP_LOWER_VOLTAGE_2:
                            explodeStep = STEP_CHECK_STATUS;
                            break;
                        case STEP_LOWER_VOLTAGE:
                            explodeStep = STEP_READ_STATUS;
                            break;
                        case STEP_CHECK_STATUS:
                            explodeStep = STEP_UPPER_VOLTAGE;
                            break;
                        case STEP_EXPLODE:
                            explodeStep = STEP_EXPLODE_2;
                            break;
                        case STEP_EXPLODE_2:
                            explodeStep = STEP_EXPLODE_3;
                            break;
                        case STEP_EXPLODE_3:
                            explodeStep = STEP_CLOSE_BUS;
                            break;
                        case STEP_RELEASE_1:
                            explodeStep = STEP_RELEASE_2;
                            break;
                        case STEP_RELEASE_2:
                            explodeStep = STEP_RELEASE_3;
                            break;
                        case STEP_RELEASE_3:
                            explodeStep = STEP_RESET_1;
                            break;
                        case STEP_RESET_1:
                            explodeStep = STEP_RESET_2;
                            break;
                        case STEP_RESET_2:
                            explodeStep = STEP_RESET_3;
                            break;
                        case STEP_RESET_3:
                            explodeStep = STEP_EXIT_CLOSE_BUS;
                            break;
                        case STEP_INITIAL:
                            explodeStep = STEP_SCAN;
                            break;
                        case STEP_CHECK_ONLINE:
                            explodeStep = STEP_CHECK_PSW;
                            break;
                    }
                case HANDLER_SEND_COMMAND:
                    if (serialPortUtil == null)
                        return false;
                    switch (explodeStep) {
                        case STEP_DELAY:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_DELAY, delayTime >= 1024 ? 0xFFFF : delayTime);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, BaseApplication.isRemote() ? 3000 : (delayTime >= 1024 ? 1100 : delayTime + 75));
                            break;
                        case STEP_CHECK_PSW:
                            serialPortUtil.sendCmd(ConstantUtils.EXPLODE_PSW, SerialCommand.CODE_CHECK_PSW, 0);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_READ_STATUS:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_GET_ALL_STATUS, 0);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_CHARGE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHARGE, BaseApplication.readSettings().getChargeVoltage() == 18 ? 0x18 : 0x22);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_WAIT:
                            myReceiveListener.setStartAutoDetect(true);
                            return false;
                        case STEP_EXPLODE:
                            startExplodeTime = System.currentTimeMillis();
                        case STEP_EXPLODE_2:
                        case STEP_EXPLODE_3:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_EXPLODE, 0, 0);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_CLOSE_BUS:
                        case STEP_EXIT_CLOSE_BUS:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0, 0xFF, 0x16);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_RELEASE_1:
                        case STEP_RELEASE_2:
                        case STEP_RELEASE_3:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHARGE, 0);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_RESET_1:
                        case STEP_RESET_2:
                        case STEP_RESET_3:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_RESET, 0);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_CHECK_STATUS:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHECK_STATUS, detonatorList.size());
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, detonatorList.size() * 2L + ConstantUtils.SCAN_DELAY_TIME);
                            break;
                        case STEP_LOWER_VOLTAGE_2:
                        case STEP_LOWER_VOLTAGE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0xFF, 0XFF, 0X16);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_UPPER_VOLTAGE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0xFF, 0XFF, 0X32);
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                            break;
                        case STEP_INITIAL:
                            myReceiveListener.setStartAutoDetect(false);
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CLEAR_READ_STATUS, 0);
                            myHandler.removeMessages(HANDLER_PROGRESS);
                            pDialog = new MyProgressDialog(DetonateStep3Activity.this);
                            pDialog.setInverseBackgroundForced(false);
                            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            pDialog.setCanceledOnTouchOutside(false);
                            pDialog.setMax(detonatorList.size());
                            pDialog.setProgress(0);
                            pDialog.setMessage(getString(R.string.progress_detect));
                            pDialog.show();
                            break;
                        case STEP_SCAN:
                            myHandler.removeMessages(HANDLER_SEND_COMMAND);
                            if (serialPortUtil != null) {
                                if (countScanZero < ConstantUtils.SCAN_ZERO_COUNT) {
                                    serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_UID, ConstantUtils.UID_LEN);
                                    myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_SCAN_UID_TIMEOUT);
                                } else {
                                    explodeStep = STEP_RELEASE_1;
                                    myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                                    runOnUiThread(() -> tvLog.setText(String.format("%s\n%s", tvLog.getText(), getString(R.string.detect_finished))));
                                    BaseApplication.writeFile(tvLog.getText().toString());
                                    pDialog.dismiss();
                                }
                            }
                            break;
                        case STEP_CHECK_ONLINE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHECK_ONLINE, detonatorList.size());
                            myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, detonatorList.size() * 2L + ConstantUtils.SCAN_DELAY_TIME);
                            break;
                    }
                    break;
            }
            return false;
        }
    });

    private int maxDelay() {
        int result = 0;
        for (DetonatorInfoBean b : detonatorList)
            result = Math.max(result, b.getDelayTime());
        return result;
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
            if (0 == soundSuccess)
                myApp.myToast(this, R.string.message_media_load_error);
            soundAlert = soundPool.load(this, R.raw.alert, 1);
            if (0 == soundAlert)
                myApp.myToast(this, R.string.message_media_load_error);
        } else
            myApp.myToast(this, R.string.message_media_init_error);
    }

    private void changeActionBar() {
        myApp.myToast(DetonateStep3Activity.this, R.string.message_charge_success);
        if (uniteExplode) {
            explode();
        } else {
            setTitle(R.string.det_slide);
            setProgressVisibility(false);
            findViewById(R.id.sv_log).setVisibility(View.GONE);
            findViewById(R.id.fl_charge).setVisibility(View.GONE);
            findViewById(R.id.fl_slide).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonate_step3);

        uniteExplode = getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false);
        if (uniteExplode)
            setTitle(R.string.det_charge, R.string.subtitle_unite);
        else
            setTitle(R.string.det_charge);

        setProgressVisibility(true);

        myApp = (BaseApplication) getApplication();
        progressBar = findViewById(R.id.pbCountDown);
        tvPercentage = findViewById(R.id.tv_percentage);
        tvHint = findViewById(R.id.tv_hint);
        tvLog = findViewById(R.id.tv_log);
        progressBar.setVisibility(View.INVISIBLE);
        tvPercentage.setVisibility(View.INVISIBLE);
        findViewById(R.id.sv_log).setVisibility(View.GONE);
        findViewById(R.id.fl_slide).setVisibility(View.GONE);
        tvChargePercentage = findViewById(R.id.tv_charge_percentage);
        pbCharge = findViewById(R.id.pb_charge);
//        onlineExplode = getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false);
//        onlineBean = myApp.readDownloadList(true);
        offlineBean = myApp.readDownloadList(false);
        notAllow = false;
        confirmExplode = false;
        keepingEnd = false;
        rKeyDown = false;
        bKeyDown = false;
        bothKeyUp = false;
        nextStep = false;
        countDown = 0;
        hide = new AlphaAnimation(1.0f, 0.3f);
        hide.setDuration(800);
        hide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                show.start();
                tvHint.setAnimation(show);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        show = new AlphaAnimation(0.3f, 1.0f);
        show.setDuration(800);
        show.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                hide.start();
                tvHint.setAnimation(hide);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tvHint.setAnimation(hide);
        sliderImageView = findViewById(R.id.iv_slider);
        sliderImageView.setOnSliderTouchListener(new SliderImageView.OnSliderTouchListener() {
            @Override
            public void OnStartMove() {
            }

            @Override
            public void OnMoveToOthers() {
                if (keepingEnd) {
                    resetStatus();
                }
            }

            @Override
            public void OnMoveToEnd() {
                if (!keepingEnd) {
                    keepingEnd = true;
                    if (rKeyDown) {
                        tvHint.setText(String.format(Locale.getDefault(), getString(R.string.det_slide_hold_key), COUNT_TIME / 1000));
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        tvPercentage.setVisibility(View.VISIBLE);
                        tvHint.setText(String.format(Locale.getDefault(), getString(R.string.det_slide_hold_key), COUNT_TIME / 1000));
                        myHandler.sendEmptyMessageDelayed(HANDLER_PRESS_KEY, 100);
                    }
                }
            }

            @Override
            public void OnStopMove() {
                if (confirmExplode) {
                    explode();
                }
                resetStatus();
            }
        });

        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(this, () -> {
                byte[] buffer = myReceiveListener.getRcvData();
                if (buffer[0] == SerialCommand.ALERT_SHORT_CIRCUIT || buffer[0] == SerialCommand.ALERT_LARGE_CURRENT) {
                    shortCircuit();
                    return;
                }
                if (buffer.length > 5) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (byte i : buffer)
                        stringBuilder.append(String.format("0x%02X ", i));
                    myHandler.removeMessages(HANDLER_SEND_COMMAND);
                    myHandler.removeMessages(HANDLER_NEXT_STEP);
                    if (0 == buffer[SerialCommand.CODE_CHAR_AT + 1]) {
                        switch (explodeStep) {
                            case STEP_CHARGE:
                                myHandler.sendEmptyMessage(HANDLER_CHARGE);
                                break;
                            case STEP_LOWER_VOLTAGE_2:
                            case STEP_LOWER_VOLTAGE:
                                myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, 2000);
                                return;
                            case STEP_CLOSE_BUS:
                                myHandler.removeCallbacksAndMessages(null);
                                if (pDialog.isShowing()) {
                                    Intent intent = new Intent(DetonateStep3Activity.this, DetonateStep4Activity.class);
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_ELAPSED, startExplodeTime);
                                    if (null != offlineBean) {
                                        for (DetonatorInfoBean bean : detonatorList) {
                                            Iterator<LgBean> it = offlineBean.getResult().getLgs().getLg().iterator();
                                            while (it.hasNext()) {
                                                if (it.next().getFbh().equals(bean.getAddress())) {
                                                    it.remove();
                                                    break;
                                                }
                                            }
                                        }
                                        myApp.saveDownloadList(offlineBean, false);
                                    }
                                    startActivity(intent);
                                    pDialog.dismiss();
                                    finish();
                                }
                                return;
                            case STEP_EXPLODE:
                            case STEP_EXPLODE_2:
                            case STEP_EXPLODE_3:
                            case STEP_RELEASE_1:
                            case STEP_RELEASE_2:
                            case STEP_RELEASE_3:
                                myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.RELEASE_DELAY_TIME);
                                return;
                            case STEP_RESET_1:
                            case STEP_RESET_2:
                            case STEP_RESET_3:
                                myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.RESET_DELAY_TIME);
                                return;
                            case STEP_EXIT_CLOSE_BUS:
                                if (countScanZero < ConstantUtils.SCAN_ZERO_COUNT)
                                    if (nextStep)
                                        finish();
                                    else {
                                        nextStep = true;
                                        if (uniteExplode)
                                            setResult(RESULT_CANCELED);
                                    }
                                return;
                            case STEP_SCAN:
                                try {
                                    myHandler.removeMessages(HANDLER_SEND_COMMAND);
                                    countScanZero = 0;
                                    DetonatorInfoBean bean = new DetonatorInfoBean(new String(Arrays.copyOfRange(buffer, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9)),
                                            (Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 11]) << 16) + (Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 12]) << 8) + Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 13]),//Delay
                                            (Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 9]) << 8) + Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 10]),//Number
                                            (Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 14]) << 8) + Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 15]),//Hole
                                            Byte.toUnsignedInt(buffer[SerialCommand.CODE_CHAR_AT + 16]), true);//Status
                                    int i = detonatorList.indexOf(bean);
                                    StringBuilder text = new StringBuilder().append(tvLog.getText()).append("\n");
                                    if (i >= 0) {
                                        DetonatorInfoBean b = detonatorList.get(i);
                                        if (myApp.isTunnel())
                                            text.append(b.getHole()).append(getString(R.string.unit_section)).append(b.getInside());
                                        else
                                            text.append(b.getRow()).append(getString(R.string.unit_row)).append(b.getHole());
                                        text.append(getString(R.string.unit_hole)).append(":").append(b.getAddress());
                                    } else
                                        text.append(getString(R.string.text_uid)).append(":").append(bean.getAddress());
                                    if (pDialog.getProgress() < pDialog.getMax() - 1)
                                        pDialog.incrementProgressBy(1);

                                    if ((bean.getInside() & SerialCommand.MASK_STATUS_LOCK) == 0)
                                        runOnUiThread(() -> tvLog.setText(text.append(" ").append(getString(R.string.error_unlock)).toString()));
                                    if ((bean.getInside() & SerialCommand.MASK_STATUS_PSW) == 0)
                                        runOnUiThread(() -> tvLog.setText(text.append(" ").append(getString(R.string.error_password)).toString()));
                                    if ((bean.getInside() & SerialCommand.MASK_STATUS_CHARGE_FULL) == 0)
                                        runOnUiThread(() -> tvLog.setText(text.append(" ").append(getString(R.string.error_charge)).toString()));
                                    if ((bean.getInside() & SerialCommand.MASK_STATUS_DELAY_FLAG) == 0)
                                        runOnUiThread(() -> tvLog.setText(text.append(" ").append(getString(R.string.error_delay)).toString()));
                                } catch (Exception e) {
                                    BaseApplication.writeErrorLog(e);
                                }
                                myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                                return;
                            case STEP_CHECK_PSW:
                            case STEP_DELAY:
                                myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, 50);
                                return;
                            case STEP_CHECK_STATUS:
                            case STEP_CHECK_ONLINE:
                                int j = 0;
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < detonatorList.size(); i++)
                                    if ((buffer[SerialCommand.CODE_CHAR_AT + 2 + i / 8] & (1 << (7 - i % 8))) == 0) {
                                        j++;
                                        if (myApp.isTunnel())
                                            builder.append(detonatorList.get(i).getHole()).append(getString(R.string.unit_section)).append(detonatorList.get(i).getInside());
                                        else
                                            builder.append(detonatorList.get(i).getRow()).append(getString(R.string.unit_row)).append(detonatorList.get(i).getHole());
                                        builder.append(getString(R.string.unit_hole)).append(":").append(detonatorList.get(i).getAddress()).append("\n");
                                    }
                                builder.insert(0, "(" + j + getString(R.string.unit_detonator) + ")\n");
                                if (j > 0) {
                                    final int k = j;
                                    countDown = 0;
                                    countScanZero = ConstantUtils.SCAN_ZERO_COUNT;
                                    myHandler.removeCallbacksAndMessages(null);
                                    explodeStep = STEP_RELEASE_1;
                                    myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
                                    nextStep = true;
                                    if (uniteExplode)
                                        setResult(RESULT_CANCELED);
                                    runOnUiThread(() -> {
                                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                                                .setTitle(R.string.progress_title)
                                                .setMessage(String.format(Locale.getDefault(), getString(R.string.dialog_exist_offline), k))
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.btn_confirm, null)
                                                .show(), true);
                                        findViewById(R.id.sv_log).setVisibility(View.VISIBLE);
                                        findViewById(R.id.fl_charge).setVisibility(View.GONE);
                                        findViewById(R.id.fl_slide).setVisibility(View.GONE);
                                        setTitle(R.string.detect_result);
                                        tvLog.setText(builder.toString());
                                        BaseApplication.writeFile(tvLog.getText().toString());
                                        setProgressVisibility(false);
                                    });
                                    return;
                                }
                                break;
                        }
                        myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                    } else {
                        switch (explodeStep) {
                            case STEP_CHECK_PSW:
                            case STEP_READ_STATUS:
                                runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                                        .setTitle(R.string.progress_title)
                                        .setMessage(R.string.dialog_explode_status_error)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.button_start_detect, (dialogInterface, i) -> {
                                            if (pDialog != null && pDialog.isShowing())
                                                pDialog.dismiss();
                                            dialogInterface.dismiss();
                                            nextStep = true;
                                            if (uniteExplode)
                                                setResult(RESULT_CANCELED);
                                            explodeStep = STEP_INITIAL;
                                            myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
                                            countScanZero = 0;
                                            findViewById(R.id.sv_log).setVisibility(View.VISIBLE);
                                            findViewById(R.id.fl_charge).setVisibility(View.GONE);
                                            findViewById(R.id.fl_slide).setVisibility(View.GONE);
                                            setTitle(R.string.det_check);
                                            tvLog.setText(R.string.camera_detecting);
                                        }).show(), true));
                                return;
                            case STEP_SCAN:
                                myHandler.removeMessages(HANDLER_SEND_COMMAND);
                                boolean allZero = true;
                                for (int i = SerialCommand.CODE_CHAR_AT + 2; i < SerialCommand.CODE_CHAR_AT + 9; i++)
                                    if (0 != buffer[i]) {
                                        allZero = false;
                                        break;
                                    }
                                if (allZero)
                                    countScanZero++;
                                if (pDialog.getProgress() < pDialog.getMax() - 1)
                                    pDialog.incrementProgressBy(1);
                                myHandler.sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                                break;
                            default:
                                BaseApplication.writeFile("返回错误！");
                                myHandler.sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.COMMAND_DELAY_TIME);
                                break;
                        }
                    }
                }
            }, false);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            detonatorList = new ArrayList<>();
            try {
                myApp.readFromFile(FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1][ConstantUtils.ListType.END.ordinal() - 1], detonatorList, DetonatorInfoBean.class);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            delayTime = maxDelay();
            if (detonatorList.size() >= 500)
                chargeTime = 72000;
            else if (detonatorList.size() >= 400)
                chargeTime = 50000;
            else if (detonatorList.size() >= 300)
                chargeTime = 32000;
            else if (detonatorList.size() >= 200)
                chargeTime = 20000;
            else if (detonatorList.size() >= 100)
                chargeTime = 10000;
            else
                chargeTime = 6000;
            chargeTime += 8000 + detonatorList.size() * 4;
            explodeStep = STEP_CHECK_ONLINE;
            doubleSend = true;
            myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
            BaseApplication.acquireWakeLock(this);
            initSound();
        } catch (
                Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void shortCircuit() {
        BaseApplication.writeFile(getString(R.string.dialog_short_circuit));
        myHandler.removeCallbacksAndMessages(null);
        myReceiveListener.setStartAutoDetect(false);
        explodeStep = STEP_RELEASE_1;
        myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
        if (chargeFinished)
            resetStatus();
        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(R.string.dialog_short_circuit)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    if (nextStep)
                        finish();
                    else {
                        nextStep = true;
                        if (uniteExplode)
                            setResult(RESULT_CANCELED);
                    }
                }).show(), true));
    }

    private void resetStatus() {
        confirmExplode = false;
        keepingEnd = false;
        countDown = 0;
        myHandler.removeMessages(HANDLER_PRESS_KEY);
        runOnUiThread(() -> {
            tvPercentage.setText("0%");
            tvHint.setText(R.string.det_slide_hint);
            progressBar.setProgress(0);
            sliderImageView.startMove(false);
            progressBar.setVisibility(View.INVISIBLE);
            tvPercentage.setVisibility(View.INVISIBLE);
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_R:
                if (notAllow) {
                    myApp.myToast(this, R.string.message_not_allow_area);
                } else if (!rKeyDown && !bKeyDown && chargeFinished) {
                    sliderImageView.startMove(true);
                    rKeyDown = true;
                }
                break;
            case KeyEvent.KEYCODE_B:
                if (notAllow) {
                    myApp.myToast(this, R.string.message_not_allow_area);
                } else if (rKeyDown && keepingEnd && !bKeyDown && !confirmExplode) {
                    bKeyDown = true;
                    countDown = 0;
                    tvPercentage.setText("0%");
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);
                    tvPercentage.setVisibility(View.VISIBLE);
                    myHandler.sendEmptyMessageDelayed(HANDLER_PRESS_KEY, 100);
                }
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_R:
                if (!bKeyDown) {
                    tvHint.setText(R.string.det_slide_hint);
                    sliderImageView.startMove(false);
                } else {
                    if (keepingEnd && !confirmExplode) {
                        resetStatus();
                    }
                }
                rKeyDown = false;
                break;
            case KeyEvent.KEYCODE_B:
                if (keepingEnd && !confirmExplode && rKeyDown) {
                    tvHint.setText(String.format(Locale.getDefault(), getString(R.string.det_slide_hold_key), COUNT_TIME / 1000));
                    myHandler.removeMessages(HANDLER_PRESS_KEY);
                    countDown = 0;
                    tvPercentage.setText("0%");
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.INVISIBLE);
                    tvPercentage.setVisibility(View.INVISIBLE);
                }
                bKeyDown = false;
                break;
            default:
                break;
        }
        if (confirmExplode) {
            int KEY_UP_TIME = 200;
            if (!bothKeyUp) {
                bothKeyUp = true;
                keyUpPeriod = System.currentTimeMillis();
                new Handler().postDelayed(() -> {
                    if (bothKeyUp) {
                        bothKeyUp = false;
                        resetStatus();
                    }
                }, KEY_UP_TIME * 2);
            } else {
                bothKeyUp = false;
                if (System.currentTimeMillis() - keyUpPeriod < KEY_UP_TIME) {
                    explode();
                }
                resetStatus();
            }
        } else if (keepingEnd && !rKeyDown && !bKeyDown) {
            resetStatus();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void explode() {
        nextStep = true;
        if (uniteExplode) {
            setResult(RESULT_OK);
            finish();
        } else {
            myReceiveListener.setStartAutoDetect(false);
            myHandler.removeMessages(HANDLER_NEXT_STEP);
            myHandler.removeMessages(HANDLER_SEND_COMMAND);
            explodeStep = STEP_LOWER_VOLTAGE;
            myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
            pDialog = new MyProgressDialog(this);
            pDialog.setInverseBackgroundForced(false);
            pDialog.setCancelable(true);
            pDialog.setCanceledOnTouchOutside(false);
            pDialog.setTitle(R.string.progress_title);
            pDialog.setMessage(getString(R.string.progress_explode));
            pDialog.show();
            setViewFontSize(pDialog.getWindow().getDecorView(), getResources().getDimensionPixelSize(R.dimen.label_text_size));
            WindowManager.LayoutParams layoutParams = pDialog.getWindow().getAttributes();
            layoutParams.height = 160;
            layoutParams.width = 300;
            pDialog.getWindow().setAttributes(layoutParams);
            myHandler.sendEmptyMessage(HANDLER_PROGRESS);
        }
    }

    private void setViewFontSize(View view, int size) {
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                setViewFontSize(parent.getChildAt(i), size);
            }
        } else if (view instanceof TextView) {
            TextView textview = (TextView) view;
            textview.setTextSize(size);
        }
    }

    @Override
    public void finish() {
        if (!nextStep) {
            if(chargeFinished)
                resetStatus();
            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_explode)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                        if (uniteExplode)
                            setResult(RESULT_CANCELED);
                        nextStep = true;
                        countScanZero = 0;
                        myReceiveListener.setStartAutoDetect(false);
                        explodeStep = STEP_RELEASE_1;
                        myHandler.sendEmptyMessage(HANDLER_SEND_COMMAND);
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show(), true);
        } else
            super.finish();
    }

    @Override
    protected void onPause() {
        changeAction = 1;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (changeAction == 2)
            changeActionBar();
        changeAction = 0;
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        LocationUtils.unRegisterListener(this);
        BaseApplication.releaseWakeLock(DetonateStep3Activity.this);
        if (!nextStep)
            myApp.deleteDetectTempFiles();

        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        if (!uniteExplode && null != myReceiveListener) {
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
        if (!uniteExplode && serialPortUtil != null) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        super.onDestroy();
    }
}
