package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.SliderImageView;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Bean.DownloadDetonatorBean;
import com.leon.detonator.Bean.LgBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Dialog.ListDialog;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.KeyUtils;
import com.leon.detonator.Util.LocationUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DetonateStep3Activity extends BaseActivity {
    private final int COUNT_TIME = 1000;
    private final int KEY_UP_TIME = 200;
    private final int STEP_OFF = 1,
            STEP_BOOST = 2,
            STEP_SYNC_1 = 3,
            STEP_SYNC_2 = 4,
            STEP_SYNC_3 = 5,
            STEP_SYNC_4 = 6,
            STEP_OPEN_CAPACITY = 7,
            STEP_DETECT_FINISHED = 8,
            STEP_READ_VOLTAGE = 9,
            STEP_CHECK_CAPACITOR = 10,
            STEP_FAST_DETECT = 11,
            STEP_BOOST_HIGH = 12,
            STEP_EXPLODE = 13,
            STEP_ENTER_EXPLODE = 14,
            STEP_OPEN_CAPACITY_2 = 16,
            STEP_OPEN_CAPACITY_3 = 17,
            STEP_OPEN_CAPACITY_4 = 18,
            STEP_BOOST_CHARGING_VOLTAGE = 19;
    private final boolean fastDetect = true;
    private int changeAction = 0;   //0：正常，1：关屏，2：开屏
    private TextView tvHint;
    private AlphaAnimation hide, show;
    private boolean keepingEnd, confirmExplode, nextStep, notAllow, changingVoltage = false, bypass, uniteExplode;
    private boolean rKeyDown, bKeyDown, bothKeyUp;
    private int countDown, dac, chargeTime, chargeDac = 1600, soundSuccess, soundAlert;
    private float voltage;
    private long keyUpPeriod;
    private TextView tvPercentage, tvChargePercentage;
    private ProgressBar progressBar, pbCharge;
    private SliderImageView sliderImageView;
    private SerialPortUtil serialPortUtil;
    private List<DetonatorInfoBean> detonatorList, wrongList;
    private ListDialog listDialog;
    private int checkIndex, resendCount;
    private boolean checking, finished = false, startShortDetect = false;
    private StringBuilder tempRcv;
    private SoundPool soundPool;
    private DownloadDetonatorBean offlineBean;
    private LocalSettingBean settingBean;
    private BaseApplication myApp;
    private final Handler refreshProgressBar = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (1 == msg.what) {
                if (countDown < COUNT_TIME / 100) {
                    countDown++;
                    int percent = countDown * 10000 / COUNT_TIME;
                    startShortDetect = percent > 80;
                    tvPercentage.setText(String.format(Locale.CHINA, "%d%%", percent));
                    progressBar.setProgress(percent);
                    refreshProgressBar.removeMessages(1);
                    refreshProgressBar.sendEmptyMessageDelayed(1, 100);
                } else {
                    confirmExplode = true;
                    refreshProgressBar.removeMessages(1);
                    tvHint.setText(rKeyDown ? R.string.det_cancel_key : R.string.det_cancel_hint);
                }
            } else {
                refreshProgressBar.removeMessages(2);
                if (checking) {
                    int percent = checkIndex * 100 / detonatorList.size();
                    tvChargePercentage.setText(String.format(Locale.CHINA, "%d%%", percent));
                    pbCharge.setProgress(percent);
                } else if (countDown < chargeTime / 100) {
                    countDown++;
                    int percent = countDown * 10000 / chargeTime;
                    tvChargePercentage.setText(String.format(Locale.CHINA, "%d%%", percent));
                    pbCharge.setProgress(percent);
                    refreshProgressBar.sendEmptyMessageDelayed(2, 100);
                } else {
                    tvChargePercentage.setText("0%");
                    pbCharge.setProgress(0);
                    if (myApp.isNewClock())
                        startChecking();
                    else {
                        finished = true;
                        delaySendCmdHandler.sendEmptyMessage(STEP_READ_VOLTAGE);
                    }
                    if (changeAction == 0)
                        changeActionBar();
                    else if (changeAction == 1)
                        changeAction = 2;
                    countDown = 0;
                }
            }
            return false;
        }
    });
    private final Handler delaySendCmdHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case STEP_OFF:
                    if (settingBean.isNewLG()) {
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.SET_VOLTAGE, (int) voltage * 10);
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_SYNC_1, 100);
                    } else {
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST_HIGH, 2000);
                    }
                    refreshProgressBar.sendEmptyMessageDelayed(2, 100);
                    break;
                case STEP_BOOST_CHARGING_VOLTAGE:
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + chargeDac + "###");
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_OPEN_CAPACITY, ConstantUtils.BOOST_TIME);
                    break;
                case STEP_BOOST_HIGH:
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "0###");
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST, 2000);
                    break;
                case STEP_BOOST:
                    if (voltage != 22) {
//                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + (int) voltage + "###");
//                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_SYNC_1, 2000);
                        delaySendCmdHandler.removeMessages(STEP_BOOST);
                        if (!changingVoltage)
                            startChangeVoltage(voltage);
                        else
                            serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST, 200);
                    } else {
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_READ_VOLTAGE, 2000);
                    }
                    //refreshProgressBar.sendEmptyMessageDelayed(2, 100);
                    break;
                case STEP_SYNC_1:
                    if (settingBean.isNewLG()) {
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.ADJUST_CLOCK, 0);
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_OPEN_CAPACITY, 1000);
                    } else {
                        serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.SYNC_CLOCK, 0);
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_SYNC_2, 800);
                    }
                    break;
                case STEP_SYNC_2:
                    serialPortUtil.sendCmd(SerialCommand.CMD_ADJUST_CLOCK);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_SYNC_3, 6000);
                    break;
                case STEP_SYNC_3:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.SYNC_CLOCK, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_SYNC_4, 800);
                    break;
                case STEP_SYNC_4:
                    serialPortUtil.sendCmd(SerialCommand.CMD_ADJUST_CLOCK);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST_CHARGING_VOLTAGE, 6000);
                    break;
                case STEP_OPEN_CAPACITY:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, settingBean.isNewLG() ?
                            (detonatorList.size() > 50 ? (detonatorList.size() + 50) / 50 - 1 : 0) : 0);
                    if (!settingBean.isNewLG())
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_OPEN_CAPACITY_2, 5000);
                    break;
                case STEP_OPEN_CAPACITY_2:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_OPEN_CAPACITY_3, 1000);
                    break;
                case STEP_OPEN_CAPACITY_3:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_OPEN_CAPACITY_4, 1000);
                    break;
                case STEP_OPEN_CAPACITY_4:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST, 1000);
                    break;
                case STEP_READ_VOLTAGE:
                    serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_READ_VOLTAGE, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                    break;
                case STEP_CHECK_CAPACITOR:
                    int what = STEP_READ_VOLTAGE;
                    tempRcv = new StringBuilder();
                    if (resendCount >= ConstantUtils.RESEND_TIMES) {
                        myApp.myToast(DetonateStep3Activity.this,
                                String.format(Locale.CHINA, getResources().getString(R.string.message_detonator_capacity_status), checkIndex + 1, detonatorList.get(checkIndex).getAddress()));
                        wrongList.add(detonatorList.get(checkIndex));
                        checkIndex++;
                        refreshProgressBar.sendEmptyMessage(2);
                        resendCount = 0;
                    }
                    if (checkIndex >= detonatorList.size()) {
                        if (wrongList.size() > 0) {
                            initListDialog();
                        } else {
                            checking = false;
                            serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                            if (changeAction == 0)
                                changeActionBar();
                            else if (changeAction == 1)
                                changeAction = 2;
                            myApp.playSoundVibrate(soundPool, soundSuccess);
                            BaseApplication.releaseWakeLock(DetonateStep3Activity.this);
                        }
                    } else {
                        what = STEP_CHECK_CAPACITOR;
                        resendCount++;
                        serialPortUtil.sendCmd(detonatorList.get(checkIndex).getAddress(), SerialCommand.ACTION_TYPE.CHECK_CAPACITOR, 0);
                    }
                    delaySendCmdHandler.sendEmptyMessageDelayed(what, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
                case STEP_FAST_DETECT:
                    delaySendCmdHandler.removeMessages(STEP_FAST_DETECT);
                    tempRcv = new StringBuilder();
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.FAST_DETECT, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_DETECT_FINISHED, (long) ConstantUtils.FAST_DETECT_TIMEOUT * (detonatorList.size() + 2));
                    break;
                case STEP_DETECT_FINISHED:
                    finished = true;
                    if (wrongList.size() != detonatorList.size()) {
                        for (DetonatorInfoBean bean : detonatorList) {
                            boolean notFound = true;
                            for (DetonatorInfoBean b : wrongList) {
                                if (b.getAddress().equals(bean.getAddress())) {
                                    notFound = false;
                                    break;
                                }
                            }
                            if (notFound) {
                                wrongList.add(bean);
                                wrongList.get(wrongList.size() - 1).setDownloaded(false);
                                wrongList.get(wrongList.size() - 1).setSelected(true);
                            }
                        }
                    }
                    Iterator<DetonatorInfoBean> iterator = wrongList.iterator();
                    while (iterator.hasNext()) {
                        if (!iterator.next().isSelected()) {
                            iterator.remove();
                        }
                    }
                    if (wrongList.size() > 0) {
                        initListDialog();
                    } else {
                        checking = false;
                        delaySendCmdHandler.sendEmptyMessage(STEP_READ_VOLTAGE);
                        if (changeAction == 0)
                            changeActionBar();
                        else if (changeAction == 1)
                            changeAction = 2;
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                        BaseApplication.releaseWakeLock(DetonateStep3Activity.this);
                    }
                    break;
                case STEP_EXPLODE:
                    BaseApplication.writeFile("发送起爆命令！1");
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.NEW_EXPLODE, 0);
                    if (settingBean.isNewLG())
                        delaySendCmdHandler.sendEmptyMessageDelayed(resendCount++ < ConstantUtils.EXPLODE_TIMES - 1 ? STEP_EXPLODE : STEP_ENTER_EXPLODE, 10);
                    else
                        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_ENTER_EXPLODE, 500);
                    break;
                case STEP_ENTER_EXPLODE:
                    BaseApplication.writeFile("发送起爆命令！2");
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.NEW_EXPLODE, 0);
                    Intent intent = new Intent(DetonateStep3Activity.this, DetonateStep4Activity.class);
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_ELAPSED, System.currentTimeMillis() - 500);

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
                    finish();
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private void startChangeVoltage(float voltage) {
        if (null == settingBean.getDacMap())
            settingBean.setDacMap(new HashMap<>());
        Integer v = settingBean.getDacMap().get(voltage);
        if (null != v) {
            dac = v;
        } else {
            dac = 94 + (int) ((29 - voltage) * 124);
        }
        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
        changingVoltage = true;
        delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST, 200);
    }

    private void startChecking() {
        checkIndex = 0;
        checking = true;
        delaySendCmdHandler.removeMessages(STEP_READ_VOLTAGE);
        delaySendCmdHandler.sendEmptyMessage(fastDetect ? STEP_FAST_DETECT : STEP_CHECK_CAPACITOR);
        resendCount = 0;
    }

    private void initListDialog() {
        listDialog = new ListDialog(this, wrongList);
        listDialog.setTunnel(myApp.isTunnel());
        listDialog.setCanceledOnTouchOutside(false);
        listDialog.setCancelable(false);
        listDialog.setListener1(view -> {
            checkIndex = 0;
            checking = true;
            resendCount = 0;
            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 0);
            delaySendCmdHandler.removeMessages(STEP_READ_VOLTAGE);
            delaySendCmdHandler.removeMessages(fastDetect ? STEP_FAST_DETECT : STEP_CHECK_CAPACITOR);
            delaySendCmdHandler.sendEmptyMessageDelayed(fastDetect ? STEP_FAST_DETECT : STEP_CHECK_CAPACITOR, ConstantUtils.RESEND_CMD_TIMEOUT);
            wrongList.clear();
            finished = false;
            refreshProgressBar.sendEmptyMessage(2);
            listDialog.dismiss();
        });
        listDialog.setListener2(view -> {
            checking = false;
            delaySendCmdHandler.removeMessages(fastDetect ? STEP_FAST_DETECT : STEP_CHECK_CAPACITOR);
            delaySendCmdHandler.sendEmptyMessage(STEP_READ_VOLTAGE);
            listDialog.dismiss();
            BaseApplication.releaseWakeLock(DetonateStep3Activity.this);
            changeActionBar();
        });
        listDialog.show();
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
        if (checking) {
            if (uniteExplode)
                setTitle(R.string.det_check_cap, R.string.subtitle_unite);
            else
                setTitle(R.string.det_check_cap);

            setProgressVisibility(true);
            ((TextView) findViewById(R.id.progress_title)).setText(R.string.check_progress);
            if (listDialog != null && !listDialog.isShowing())
                listDialog.show();
        } else {
            //myApp.myToast(DetonateStep3Activity.this, "检测完成！");
            if (uniteExplode) {
                explode();
            } else {
                setTitle(R.string.det_slide);
                setProgressVisibility(false);
                findViewById(R.id.fl_charge).setVisibility(View.GONE);
                findViewById(R.id.fl_slide).setVisibility(View.VISIBLE);
            }
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
        progressBar.setVisibility(View.INVISIBLE);
        tvPercentage.setVisibility(View.INVISIBLE);
        findViewById(R.id.fl_slide).setVisibility(View.GONE);
        tvChargePercentage = findViewById(R.id.tv_charge_percentage);
        pbCharge = findViewById(R.id.pb_charge);
//        onlineExplode = getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false);
//        onlineBean = myApp.readDownloadList(true);
        offlineBean = myApp.readDownloadList(false);
        settingBean = BaseApplication.readSettings();
        notAllow = false;
        confirmExplode = false;
        keepingEnd = false;
        rKeyDown = false;
        bKeyDown = false;
        bothKeyUp = false;
        nextStep = false;
        checking = false;
        dac = 1000;
        voltage = getIntent().getFloatExtra(KeyUtils.KEY_EXPLODE_VOLTAGE, settingBean.isNewLG() ? 20 : 22);
        bypass = getIntent().getBooleanExtra(KeyUtils.KEY_BYPASS_EXPLODE, false);
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
                        tvHint.setText(String.format(Locale.CHINA, getResources().getString(R.string.det_slide_hold_key), COUNT_TIME / 1000));
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        tvPercentage.setVisibility(View.VISIBLE);
                        tvHint.setText(String.format(Locale.CHINA, getResources().getString(R.string.det_slide_hold_key), COUNT_TIME / 1000));
                        refreshProgressBar.sendEmptyMessageDelayed(1, 100);
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
            tempRcv = new StringBuilder();
            serialPortUtil.setOnDataReceiveListener(buffer -> {
                if (checking) {
                    for (byte i : buffer)
                        tempRcv.append(String.format("%02X", i));
                    String data = tempRcv.toString();
                    String confirm = SerialCommand.RESPOND_CONFIRM.get(fastDetect ? SerialCommand.ACTION_TYPE.FAST_DETECT : SerialCommand.ACTION_TYPE.CHECK_CAPACITOR);
                    assert confirm != null;
                    int index = data.indexOf(confirm) + confirm.length();
                    if (fastDetect) {
                        if (!finished) {
                            if (data.contains(confirm) && data.length() >= index + 24) {
                                data = data.substring(index, index + 24);
                                try {
                                    int checkSum = 0;
                                    for (int i = 0; i < data.length() - 2; i += 2) {
                                        checkSum += Integer.parseInt(data.substring(i, i + 2), 16);
                                    }
                                    if (String.format("%02X", checkSum).endsWith(data.substring(data.length() - 2))) {
                                        char character = (char) (int) Integer.valueOf(data.substring(12, 14), 16);
                                        String address = data.substring(0, 7) + character + data.substring(7, 12);
                                        int i = searchIndex(address);
                                        if (i < detonatorList.size()) {
                                            data = data.substring(14);
                                            int delay = Integer.valueOf(data.substring(0, 4), 16);
                                            if (delay != detonatorList.get(i).getDelayTime()) {
                                                myApp.myToast(DetonateStep3Activity.this,
                                                        String.format(Locale.CHINA, getResources().getString(R.string.message_delay_error), i + 1));
                                            }
                                            wrongList.add(detonatorList.get(i));
                                            if (0 == Integer.valueOf(data.substring(4), 16)) {
                                                myApp.myToast(DetonateStep3Activity.this,
                                                        String.format(Locale.CHINA, getResources().getString(R.string.message_charge_fail), i + 1));
                                                wrongList.get(wrongList.size() - 1).setSelected(true);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    BaseApplication.writeErrorLog(e);
                                }
                                checkIndex++;
                                refreshProgressBar.sendEmptyMessage(2);
                                tempRcv.delete(0, tempRcv.indexOf(confirm) + confirm.length());
                                if (checkIndex >= detonatorList.size()) {
                                    delaySendCmdHandler.removeMessages(STEP_DETECT_FINISHED);
                                    delaySendCmdHandler.sendEmptyMessage(STEP_DETECT_FINISHED);
                                }
                            }
                        }
                    } else {
                        if (data.contains(confirm)) {
                            data = data.substring(index);
                            if (data.equals("0008") || data.equals("0109")) {
                                if (data.startsWith("00")) {
                                    wrongList.add(detonatorList.get(checkIndex));
                                }
                                checkIndex++;
                                refreshProgressBar.sendEmptyMessage(2);
                                resendCount = 0;
                                delaySendCmdHandler.removeMessages(STEP_CHECK_CAPACITOR);
                                delaySendCmdHandler.sendEmptyMessage(STEP_CHECK_CAPACITOR);
                            }
                        }
                    }
                } else {
                    String data = new String(buffer);
                    if (data.contains(SerialCommand.RESPOND_SUCCESS)) {
                        data = data.replace(SerialCommand.RESPOND_SUCCESS, "");
                        try {
                            if (data.contains(SerialCommand.RESPOND_VOLTAGE) && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_VOLTAGE)) {
                                final int vol = Integer.parseInt(data.substring(data.indexOf(SerialCommand.RESPOND_VOLTAGE) + SerialCommand.RESPOND_VOLTAGE.length(), data.indexOf("\r")));
                                setVoltage(vol / 100.0f);
                                if (changingVoltage) {
                                    try {
                                        float v = vol / 100.0f + 0.1f;
                                        delaySendCmdHandler.removeMessages(STEP_BOOST);
                                        if (Math.abs(v - voltage) < 0.1f) {
                                            changingVoltage = false;
                                            settingBean.getDacMap().put(voltage, dac);
                                            myApp.saveSettings(settingBean);
                                            delaySendCmdHandler.sendEmptyMessage(STEP_READ_VOLTAGE);
                                        } else {
                                            dac += (v > voltage ? 100 : -100) * (Math.abs(v - voltage) - 0.01f);
                                            if (dac < 50 || dac > 4000) {
                                                dac = 94 + (int) ((29 - voltage) * 124);
                                            }
                                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
                                            delaySendCmdHandler.sendEmptyMessageDelayed(STEP_BOOST, 200);
                                        }
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                } else if (startShortDetect && vol < ConstantUtils.MINIMUM_VOLTAGE && vol > 0) {
                                    exit(R.string.dialog_short_circuit);
                                } else
                                    serialPortUtil.sendCmd(SerialCommand.CMD_READ_CURRENT);
                            } else if (data.contains(SerialCommand.RESPOND_CURRENT) && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_CURRENT)) {
                                data = data.substring(data.indexOf(SerialCommand.RESPOND_CURRENT) + SerialCommand.RESPOND_CURRENT.length(), data.indexOf("\r"));
                                if (data.length() > 0) {
                                    int c = Integer.parseInt(data);
                                    if (detonatorList.size() >= 5 && 0 == c && !bypass) {
                                        exit(R.string.dialog_open_circuit);
                                    } else {
                                        int count;
                                        count = (int) ((c + 2) / 3.4f) + 1;
                                        float current = 0.0f;
                                        if (c == 1)
                                            current = 25;
                                        else if (c > 0 && count > 0)
                                            current = count * 25 + ((c + 2) - (count - 1) * 3.4f) / 3.4f;
                                        setCurrent(current);
                                        if (startShortDetect && current > ConstantUtils.MAXIMUM_CURRENT) {
                                            delaySendCmdHandler.removeMessages(STEP_EXPLODE);
                                            exit(R.string.dialog_short_circuit);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                    }
                }
            });
            detonatorList = new ArrayList<>();
            wrongList = new ArrayList<>();
            try {
                myApp.readFromFile(
                        FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1][ConstantUtils.LIST_TYPE.END.ordinal()],
                        detonatorList, DetonatorInfoBean.class);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            if (settingBean.isNewLG()) {
                chargeTime = (detonatorList.size() > 50 ? (detonatorList.size() + 50) / 50 : 1) * 25 * 240 + 1000;
            } else {
                chargeTime = 120000;
//                chargeTime = 10000 + detonatorList.size() * 200;
//                if (chargeTime < 46000)
//                    chargeTime = 46000;
            }
            refreshProgressBar.sendEmptyMessageDelayed(2, 100);
            delaySendCmdHandler.sendEmptyMessage(STEP_SYNC_1);
            BaseApplication.acquireWakeLock(this);
            initSound();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void exit(@StringRes int res) {
        refreshProgressBar.removeCallbacksAndMessages(null);
        delaySendCmdHandler.removeCallbacksAndMessages(null);
        if (serialPortUtil != null) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        nextStep = true;
        myApp.playSoundVibrate(soundPool, soundAlert);
        runOnUiThread(() -> new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(res)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> finish())
                .create().show());
    }

    private int searchIndex(String address) {
        int index = 0;
        for (DetonatorInfoBean b : detonatorList) {
            if (b.getAddress().equals(address)) {
                break;
            }
            index++;
        }
        return index;
    }

    private void resetStatus() {
        confirmExplode = false;
        keepingEnd = false;
        countDown = 0;
        tvHint.setText(R.string.det_slide_hint);
        refreshProgressBar.removeMessages(1);
        tvPercentage.setText("0%");
        progressBar.setProgress(0);
        progressBar.setVisibility(View.INVISIBLE);
        tvPercentage.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_R:
                BaseApplication.writeFile("按下R键" + "b=" + (bKeyDown ? 1 : 0) + ",r=" + (bKeyDown ? 1 : 0) + ",finished=" + (finished ? 1 : 0));
                //myApp.myToast(this, "b=" + (bKeyDown ? 1 : 0) + ",r=" + (bKeyDown ? 1 : 0)+ ",o=" + (otherKeyDown ? 1 : 0));
                if (notAllow) {
                    myApp.myToast(this, R.string.message_not_allow_area);
                } else if (!rKeyDown && !bKeyDown && finished) {
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
                    refreshProgressBar.sendEmptyMessageDelayed(1, 100);
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
                        sliderImageView.startMove(false);
                    }
                }
                rKeyDown = false;
                break;
            case KeyEvent.KEYCODE_B:
                if (keepingEnd && !confirmExplode && rKeyDown) {
                    tvHint.setText(String.format(Locale.CHINA, getResources().getString(R.string.det_slide_hold_key), COUNT_TIME / 1000));
                    refreshProgressBar.removeMessages(1);
                    countDown = 0;
                    tvPercentage.setText("0%");
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.INVISIBLE);
                    tvPercentage.setVisibility(View.INVISIBLE);
                }
                bKeyDown = false;
                break;
//            case KeyEvent.KEYCODE_STAR:
//                newExplode = !newExplode;
//                myApp.myToast(this, newExplode ? "切换到新起爆方式！" : "切换到旧起爆方式！");
//                break;
            default:
                break;
        }
        if (confirmExplode) {
            if (!bothKeyUp) {
                bothKeyUp = true;
                keyUpPeriod = System.currentTimeMillis();
                new Handler().postDelayed(() -> {
                    if (bothKeyUp) {
                        bothKeyUp = false;
                        sliderImageView.startMove(false);
                        resetStatus();
                    }
                }, KEY_UP_TIME * 2);
            } else {
                bothKeyUp = false;
                if (System.currentTimeMillis() - keyUpPeriod < KEY_UP_TIME) {
                    BaseApplication.writeFile("松开按键！");
                    explode();
                }
                sliderImageView.startMove(false);
                resetStatus();
            }
        } else if (keepingEnd && !rKeyDown && !bKeyDown) {
            sliderImageView.startMove(false);
            resetStatus();
        }
        return super.onKeyUp(keyCode, event);
    }

//    private boolean checkLocation() {
//        if (onlineExplode && null != onlineBean && null != onlineBean.getResult() && null != ((ResultBean)onlineBean.getResult()).getZbqys() && null != ((ResultBean)onlineBean.getResult()).getZbqys().getZbqy() ||
//                (!onlineExplode && null != offlineBean && null != offlineBean.getResult() && null != ((ResultBean)offlineBean.getResult()).getZbqys() && null != ((ResultBean)offlineBean.getResult()).getZbqys().getZbqy())) {
//            List<ZbqyBean> list = onlineExplode ? ((ResultBean)onlineBean.getResult()).getZbqys().getZbqy() : ((ResultBean)offlineBean.getResult()).getZbqys().getZbqy();
//            for (ZbqyBean bean : list) {
//                try {
//                    if (BaseApplication.distance(Double.parseDouble(bean.getZbqywd()), Double.parseDouble(bean.getZbqyjd()), lastLocation.getLatitude(), lastLocation.getLongitude())
//                            < Double.parseDouble(bean.getZbqybj())) {
//                        return true;
//                    }
//                } catch (Exception e) {
//                    BaseApplication.writeErrorLog(e);
//                }
//            }
//        }
//        return false;
//    }

    private void explode() {
        nextStep = true;
        if (uniteExplode) {
            setResult(RESULT_OK);
            finish();
        } else {
            delaySendCmdHandler.removeMessages(STEP_READ_VOLTAGE);
            serialPortUtil.sendCmd(SerialCommand.CMD_READ_CURRENT);
            resendCount = 0;
            delaySendCmdHandler.sendEmptyMessageDelayed(STEP_EXPLODE, settingBean.isNewLG() ? 0 : 100);
        }
    }

    @Override
    public void finish() {
        if (!nextStep) {
            new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_explode)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                        if (settingBean.isNewLG())
                            serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.RELEASE_CAPACITOR, 0);
                        if (uniteExplode)
                            setResult(RESULT_CANCELED);
                        DetonateStep3Activity.super.finish();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
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
        refreshProgressBar.removeCallbacksAndMessages(null);
        delaySendCmdHandler.removeCallbacksAndMessages(null);
        if (!uniteExplode && serialPortUtil != null) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        LocationUtils.unRegisterListener(this);
        BaseApplication.releaseWakeLock(DetonateStep3Activity.this);
        if (!nextStep) {
            String[] fileList = FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1];
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
                File file = new File(fileList[i]);
                if (file.exists() && !file.delete())
                    myApp.myToast(DetonateStep3Activity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
            }
        }
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }

        super.onDestroy();
    }
}
