package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.kfree.expd.ExpdDevMgr;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.component.SliderImageView;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DetonateStep3Activity extends BaseActivity {
    private TextView tvHint;
    private TextView tvCount;
    private AlphaAnimation hide;
    private AlphaAnimation show;
    private MyProgressDialog pDialog;
    private TextView tvPercentage;
    private TextView tvChargePercentage;
    private ProgressBar progressBar;
    private ProgressBar pbCharge;
    private SliderImageView sliderImageView;
    private SerialPortUtil serialPortUtil;
    private DataReceiveListener myReceiveListener;
    private List<DetonatorBean> detonatorList;
    private SoundPool soundPool;
    private EditText tvLog;
    private final int HANDLER_PRESS_KEY = 1;
    private final int HANDLER_PROGRESS = 2;
    private final int HANDLER_START = 3;
    private final int HANDLER_COUNT_DOWN = 7;
    private boolean keepingEnd;
    private boolean confirmExplode;
    private boolean nextStep;
    private boolean notAllow;
    private boolean uniteExplode;
    private boolean leftKeyDown;
    private boolean rightKeyDown;
    private boolean bothKeyUp;
    private boolean doubleSend;
    private boolean chargeFinished;
    private boolean ignoreCurrent;
    private boolean dialogShowing;
    private boolean notCheckStatus;
    private boolean enabledKeys;
    private int countingStatus;
    private int changeAction;   //0：正常，1：关屏，2：开屏
    private int countDown;
    private int countBeforeExplode;
    private int chargeTime;
    private int delayTime;
    private int explodeStep;
    private int soundSuccess;
    private int countScanZero;
    private long keyUpPeriod;
    private long startExplodeTime;

    private final Handler myHandler = new Handler(msg -> {
        final int COUNT_TIME = 1000;
        final int HANDLER_SEND_COMMAND = 4;
        final int HANDLER_NEXT_STEP = 5;
        final int HANDLER_CHARGE = 6;
        final int STEP_DELAY = 1;
        final int STEP_CHECK_PSW = 2;
        final int STEP_READ_STATUS = 3;
        final int STEP_CHARGE = 4;
        final int STEP_EXPLODE = 5;
        final int STEP_CLOSE_BUS = 6;
        final int STEP_WAIT = 7;
        final int STEP_RELEASE_1 = 9;
        final int STEP_RELEASE_2 = 10;
        final int STEP_RELEASE_3 = 11;
        final int STEP_RESET_1 = 12;
        final int STEP_RESET_2 = 13;
        final int STEP_RESET_3 = 14;
        final int STEP_EXIT_CLOSE_BUS = 15;
        final int STEP_LOWER_VOLTAGE = 16;
        final int STEP_SCAN = 17;
        final int STEP_INITIAL = 18;
        final int STEP_UPPER_VOLTAGE = 19;
        final int STEP_EXPLODE_2 = 20;
        final int STEP_EXPLODE_3 = 21;
        final int STEP_LOWER_VOLTAGE_2 = 22;
        final int STEP_CHECK_STATUS = 23;
        final int STEP_CHECK_ONLINE = 24;
        final int STEP_UPPER_VOLTAGE_2 = 25;
        final int STEP_LOWER_VOLTAGE_3 = 26;
        switch (msg.what) {
            case HANDLER_START:
                switch (explodeStep) {
                    case 0:
                        explodeStep = STEP_CHECK_ONLINE;
                        break;
                    case 1:
                        explodeStep = STEP_INITIAL;
                        break;
                    case 2:
                        explodeStep = STEP_RELEASE_1;
                        break;
                    case 3:
                        explodeStep = STEP_LOWER_VOLTAGE;
                        break;
                }
                msg.getTarget().sendEmptyMessage(HANDLER_SEND_COMMAND);
                break;
            case HANDLER_PRESS_KEY:
                if (countDown < COUNT_TIME / 100) {
                    countDown++;
                    int percent = countDown * 10000 / COUNT_TIME;
                    tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
                    progressBar.setProgress(percent);
                    msg.getTarget().removeMessages(HANDLER_PRESS_KEY);
                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_PRESS_KEY, 100);
                } else {
                    confirmExplode = true;
                    msg.getTarget().removeMessages(HANDLER_PRESS_KEY);
                    tvHint.setText(leftKeyDown ? R.string.det_cancel_key : R.string.det_cancel_hint);
                }
                break;
            case HANDLER_CHARGE:
                msg.getTarget().removeMessages(HANDLER_CHARGE);
                if (countDown < chargeTime / 100) {
                    countDown++;
                    int percent = countDown * 10000 / chargeTime;
                    tvChargePercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
                    pbCharge.setProgress(percent);
                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_CHARGE, 100);
                    if (countDown * 100 > chargeTime - 5000 && notCheckStatus) {
                        msg.getTarget().removeMessages(HANDLER_NEXT_STEP);
                        msg.getTarget().removeMessages(HANDLER_SEND_COMMAND);
                        explodeStep = STEP_LOWER_VOLTAGE_2;
                        myReceiveListener.setStartAutoDetect(false);
                        msg.getTarget().sendEmptyMessage(HANDLER_SEND_COMMAND);
                        notCheckStatus = false;
                    }
                } else {
                    tvChargePercentage.setText("0%");
                    pbCharge.setProgress(0);
                    chargeFinished = true;
                    enabledKeys = true;
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
                msg.getTarget().removeMessages(HANDLER_PROGRESS);
                if (pDialog.getProgress() < pDialog.getMax() - 1) {
                    pDialog.incrementProgressBy(1);
                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_PROGRESS, 50);
                }
                break;
            case HANDLER_COUNT_DOWN:
                if (countingStatus != 3) {
                    tvCount.setText(String.format(Locale.getDefault(), "%d", countBeforeExplode--));
                    if (countBeforeExplode >= 0)
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_COUNT_DOWN, 1000);
                    else {
                        countingStatus = 2;
                        explodeStep = STEP_LOWER_VOLTAGE_3;
                        msg.getTarget().sendEmptyMessage(HANDLER_SEND_COMMAND);
                    }
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
                        } else
                            explodeStep = STEP_CHARGE;
                        break;
                    case STEP_UPPER_VOLTAGE:
                    case STEP_CHARGE:
                        explodeStep = STEP_WAIT;
                        break;
                    case STEP_READ_STATUS:
                        explodeStep = STEP_UPPER_VOLTAGE_2;
                        break;
                    case STEP_UPPER_VOLTAGE_2:
                        if (pDialog != null && pDialog.isShowing())
                            pDialog.dismiss();
                        findViewById(R.id.fl_count).setVisibility(View.VISIBLE);
                        countBeforeExplode = 5;
                        countingStatus = 1;
                        msg.getTarget().sendEmptyMessage(HANDLER_COUNT_DOWN);
                        return false;
                    case STEP_LOWER_VOLTAGE_3:
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
                switch (explodeStep) {
                    case STEP_DELAY:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_DELAY, delayTime >= 1024 ? 0xFFFF : delayTime);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, BaseApplication.isRemote() ? 3000 : (delayTime >= 1024 ? 1100 : delayTime + 75));
                        break;
                    case STEP_CHECK_PSW:
                        serialPortUtil.sendCmd(ConstantUtils.EXPLODE_PSW, SerialCommand.CODE_CHECK_PSW, 0);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_READ_STATUS:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_GET_ALL_STATUS, 0);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_CHARGE:
                        float voltage = BaseApplication.settings.getChargeVoltage();
                        serialPortUtil.sendCmd("", SerialCommand.CODE_CHARGE, voltage == 18 ? 0x18 : (voltage == 20 ? 0x20 : 0x22));
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_WAIT:
                        myReceiveListener.setStartAutoDetect(true);
                        break;
                    case STEP_EXPLODE:
                        startExplodeTime = System.currentTimeMillis();
                    case STEP_EXPLODE_2:
                    case STEP_EXPLODE_3:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_EXPLODE, 0, 0);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_CLOSE_BUS:
                    case STEP_EXIT_CLOSE_BUS:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0, 0xFF, 0x16);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_RELEASE_1:
                    case STEP_RELEASE_2:
                    case STEP_RELEASE_3:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_CHARGE, 0);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_RESET_1:
                    case STEP_RESET_2:
                    case STEP_RESET_3:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_RESET, 0);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_CHECK_STATUS:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_CHECK_STATUS, detonatorList.size());
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, detonatorList.size() * 2L + ConstantUtils.SCAN_DELAY_TIME);
                        break;
                    case STEP_LOWER_VOLTAGE_3:
                    case STEP_LOWER_VOLTAGE_2:
                    case STEP_LOWER_VOLTAGE:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0xFF, 0XFF, 0X16);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_UPPER_VOLTAGE:
                    case STEP_UPPER_VOLTAGE_2:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0xFF, 0XFF, 0X32);
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case STEP_INITIAL:
                        myReceiveListener.setStartAutoDetect(false);
                        serialPortUtil.sendCmd("", SerialCommand.CODE_CLEAR_READ_STATUS, 0);
                        msg.getTarget().removeMessages(HANDLER_PROGRESS);
                        enabledKeys = false;
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
                        msg.getTarget().removeMessages(HANDLER_SEND_COMMAND);
                        if (serialPortUtil != null) {
                            if (countScanZero < ConstantUtils.SCAN_ZERO_COUNT) {
                                serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_UID, ConstantUtils.UID_LEN);
                                msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.RESEND_SCAN_UID_TIMEOUT);
                            } else {
                                explodeStep = STEP_RELEASE_1;
                                msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                                setProgressVisibility(false);
                                tvLog.setText(String.format("%s\n%s", tvLog.getText(), getString(R.string.detect_finished)));
                                BaseApplication.writeFile(tvLog.getText().toString());
                                pDialog.dismiss();
                            }
                        }
                        break;
                    case STEP_CHECK_ONLINE:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_CHECK_ONLINE, detonatorList.size());
                        msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, detonatorList.size() * 2L + ConstantUtils.SCAN_DELAY_TIME);
                        break;
                }
                break;
            case DataReceiveListener.HANDLER_RECEIVED_DATA:
                byte[] received = (byte[]) msg.obj;
                if (received != null && received.length > 0)
                    if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                        breakExplode(R.string.dialog_short_circuit);
                    } else if (!ignoreCurrent && received[0] == SerialCommand.ALERT_LARGE_CURRENT) {
                        if (!dialogShowing) {
                            dialogShowing = true;
                            if (chargeFinished)
                                resetStatus();
                            enabledKeys = false;
                            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog).setTitle(R.string.dialog_title_warning).setMessage(R.string.dialog_large_current).setCancelable(false).setNegativeButton(R.string.button_exit, (dialog, which) -> {
                                msg.getTarget().removeCallbacksAndMessages(null);
                                myReceiveListener.setStartAutoDetect(false);
                                explodeStep = STEP_RELEASE_1;
                                msg.getTarget().sendEmptyMessage(HANDLER_SEND_COMMAND);
                                nextStep = true;
                                if (uniteExplode)
                                    setResult(RESULT_CANCELED);
                            }).setPositiveButton(R.string.button_ignore, ((dialog, which) -> {
                                enabledKeys = true;
                                ignoreCurrent = true;
                            })).show());
                        }
                    } else if (received[0] == SerialCommand.ALERT_BREAK_CIRCUIT) {
                        breakExplode(R.string.dialog_break_circuit);
                    } else if (received.length > 5) {
                        msg.getTarget().removeMessages(HANDLER_SEND_COMMAND);
                        msg.getTarget().removeMessages(HANDLER_NEXT_STEP);
                        if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                            switch (explodeStep) {
                                case STEP_CHARGE:
                                    myApp.speakText(R.string.speech_start_charge);
                                    msg.getTarget().sendEmptyMessage(HANDLER_CHARGE);
                                    break;
                                case STEP_LOWER_VOLTAGE_3:
                                case STEP_LOWER_VOLTAGE_2:
                                case STEP_LOWER_VOLTAGE:
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, 2000);
                                    return false;
                                case STEP_CLOSE_BUS:
                                    msg.getTarget().removeCallbacksAndMessages(null);
                                    Intent intent = new Intent(DetonateStep3Activity.this, DetonateStep4Activity.class);
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_ELAPSED, startExplodeTime);
                                    int t = 0;
                                    for (DetonatorBean bean : detonatorList)
                                        t = Math.max(t, bean.getDelayTime());
                                    intent.putExtra(KeyUtils.KEY_EXPLODE_TIME, t);
                                    intent.putExtra(KeyUtils.KEY_TABLE_ID, detonatorList.get(0).getSchemeId());
                                    startActivity(intent);
                                    countingStatus = 0;
                                    nextStep = true;
                                    finish();
                                    return false;
                                case STEP_EXPLODE:
                                case STEP_EXPLODE_2:
                                case STEP_EXPLODE_3:
                                case STEP_RELEASE_1:
                                case STEP_RELEASE_2:
                                case STEP_RELEASE_3:
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.RELEASE_DELAY_TIME);
                                    return false;
                                case STEP_RESET_1:
                                case STEP_RESET_2:
                                case STEP_RESET_3:
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.RESET_DELAY_TIME);
                                    return false;
                                case STEP_EXIT_CLOSE_BUS:
                                    msg.getTarget().removeCallbacksAndMessages(null);
                                    if (countScanZero < ConstantUtils.SCAN_ZERO_COUNT)
                                        if (nextStep)
                                            finish();
                                        else {
                                            nextStep = true;
                                            if (uniteExplode)
                                                setResult(RESULT_CANCELED);
                                        }
                                    return false;
                                case STEP_SCAN:
                                    try {
                                        msg.getTarget().removeMessages(HANDLER_SEND_COMMAND);
                                        countScanZero = 0;
                                        boolean isNull = true;
                                        for (int j = 0; j < 7; j++)
                                            if (received[SerialCommand.CODE_CHAR_AT + 2 + j] != 0) {
                                                isNull = false;
                                                break;
                                            }
                                        DetonatorBean bean = new DetonatorBean(-1, new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9)),
                                                (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 11]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 12]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 13]),//Delay
                                                (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 10]),//Number
                                                (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 14]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 15]),//Hole
                                                Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 16]), true);//Status
                                        StringBuilder text = new StringBuilder().append(tvLog.getText()).append("\n");
                                        if (isNull) {
                                            text.append(ConstantUtils.NULL_ID);
                                        } else {
                                            int j = detonatorList.indexOf(bean);
                                            if (j >= 0) {
                                                DetonatorBean b = detonatorList.get(j);
                                                if (BaseApplication.settings.isTunnel())
                                                    text.append(b.getHole()).append(getString(R.string.unit_section)).append(b.getInside());
                                                else
                                                    text.append(b.getRow()).append(getString(R.string.unit_row)).append(b.getHole());
                                                text.append(getString(R.string.unit_hole)).append(":").append(b.getAddress());
                                            } else
                                                text.append(getString(R.string.text_uid)).append(":").append(bean.getAddress());
                                        }
                                        if (pDialog.getProgress() < pDialog.getMax() - 1)
                                            pDialog.incrementProgressBy(1);
                                        if ((bean.getInside() & SerialCommand.MASK_STATUS_LOCK) == 0)
                                            tvLog.setText(text.append(" ").append(getString(R.string.error_unlock)).toString());
                                        if ((bean.getInside() & SerialCommand.MASK_STATUS_PSW) == 0)
                                            tvLog.setText(text.append(" ").append(getString(R.string.error_password)).toString());
                                        if ((bean.getInside() & SerialCommand.MASK_STATUS_CHARGE_FULL) == 0)
                                            tvLog.setText(text.append(" ").append(getString(R.string.error_charge)).toString());
                                        if ((bean.getInside() & SerialCommand.MASK_STATUS_DELAY_FLAG) == 0)
                                            tvLog.setText(text.append(" ").append(getString(R.string.error_delay)).toString());
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                                    return false;
                                case STEP_CHECK_PSW:
                                case STEP_DELAY:
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, 50);
                                    return false;
                                case STEP_CHECK_STATUS:
                                case STEP_CHECK_ONLINE:
                                    int j = 0;
                                    StringBuilder builder = new StringBuilder();
                                    for (int i = 0; i < detonatorList.size(); i++)
                                        if ((received[SerialCommand.CODE_CHAR_AT + 2 + i / 8] & (1 << (7 - i % 8))) == 0) {
                                            j++;
                                            if (BaseApplication.settings.isTunnel())
                                                builder.append(detonatorList.get(i).getHole()).append(getString(R.string.unit_section)).append(detonatorList.get(i).getInside());
                                            else
                                                builder.append(detonatorList.get(i).getRow()).append(getString(R.string.unit_row)).append(detonatorList.get(i).getHole());
                                            builder.append(getString(R.string.unit_hole)).append(":").append(detonatorList.get(i).getAddress()).append("\n");
                                        }
                                    builder.insert(0, "(" + j + getString(R.string.unit_detonator) + ")\n");
                                    if (j > 0) {
                                        countDown = 0;
                                        countScanZero = ConstantUtils.SCAN_ZERO_COUNT;
                                        msg.getTarget().removeCallbacksAndMessages(null);
                                        explodeStep = STEP_RELEASE_1;
                                        msg.getTarget().sendEmptyMessage(HANDLER_SEND_COMMAND);
                                        nextStep = true;
                                        enabledKeys = false;
                                        if (uniteExplode)
                                            setResult(RESULT_CANCELED);
                                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                                                .setTitle(R.string.progress_title)
                                                .setMessage(String.format(Locale.getDefault(), getString(R.string.dialog_exist_offline), j))
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.button_confirm, null).show());
                                        findViewById(R.id.sv_log).setVisibility(View.VISIBLE);
                                        findViewById(R.id.fl_charge).setVisibility(View.GONE);
                                        findViewById(R.id.fl_slide).setVisibility(View.GONE);
                                        setTitle(R.string.detect_result);
                                        tvLog.setText(builder.toString());
                                        BaseApplication.writeFile(tvLog.getText().toString());
                                        setProgressVisibility(false);
                                        return false;
                                    }
                                    break;
                            }
                            msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                        } else {
                            switch (explodeStep) {
                                case STEP_CHECK_PSW:
                                case STEP_READ_STATUS:
                                    detect();
                                    break;
                                case STEP_SCAN:
                                    msg.getTarget().removeMessages(HANDLER_SEND_COMMAND);
                                    boolean allZero = true;
                                    for (int i = SerialCommand.CODE_CHAR_AT + 2; i < SerialCommand.CODE_CHAR_AT + 9; i++)
                                        if (0 != received[i]) {
                                            allZero = false;
                                            break;
                                        }
                                    if (allZero)
                                        countScanZero++;
                                    if (pDialog.getProgress() < pDialog.getMax() - 1)
                                        pDialog.incrementProgressBy(1);
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                                    break;
                                default:
                                    BaseApplication.writeFile("返回错误！");
                                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_SEND_COMMAND, ConstantUtils.COMMAND_DELAY_TIME);
                                    break;
                            }
                        }
                    }
                break;
        }
        return false;
    });

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
        tvCount = findViewById(R.id.tv_count);
        progressBar.setVisibility(View.INVISIBLE);
        tvPercentage.setVisibility(View.INVISIBLE);
        findViewById(R.id.sv_log).setVisibility(View.GONE);
        findViewById(R.id.fl_slide).setVisibility(View.GONE);
        tvChargePercentage = findViewById(R.id.tv_charge_percentage);
        pbCharge = findViewById(R.id.pb_charge);
        enabledKeys = false;
        notCheckStatus = true;
        countingStatus = 0;
        ignoreCurrent = true;
        notAllow = false;
        hide = new AlphaAnimation(1.0f, 0.5f);
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
        show = new AlphaAnimation(0.5f, 1.0f);
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
                    runOnUiThread(() -> {
                        tvHint.setText(R.string.det_slide_hold_key);
                        if (!leftKeyDown) {
                            progressBar.setVisibility(View.VISIBLE);
                            tvPercentage.setVisibility(View.VISIBLE);
                            myHandler.sendEmptyMessageDelayed(HANDLER_PRESS_KEY, 100);
                        }
                    });
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
            serialPortUtil = SerialPortUtil.getInstance(this);
            myReceiveListener = DataReceiveListener.getInstance(this, myHandler);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            serialPortUtil.setDevEventCb(new ExpdDevMgr.IOnExpdDevEventCb() {
                @Override
                public void onSafeSwitchStChg(boolean b) {
                    if (!b)
                        breakExplode(R.string.dialog_safe_switch_off);
                }

                @Override
                public void onBackcoverStChg(boolean b) {
                    myApp.myToast(DetonateStep3Activity.this, b ? R.string.message_back_cover_open : R.string.message_back_cover_close);
                }
            });
            detonatorList = getIntent().getParcelableArrayListExtra(KeyUtils.KEY_LIST);
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
                chargeTime = 5000;
            chargeTime += 15000 + detonatorList.size() * 4;
            explodeStep = 0;
            doubleSend = true;
            myHandler.sendEmptyMessage(HANDLER_START);
            BaseApplication.acquireWakeLock(this);
            initSound();
        } catch (
                Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void detect() {
        enabledKeys = false;
        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
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
                    explodeStep = 1;
                    myHandler.sendEmptyMessage(HANDLER_START);
                    countScanZero = 0;
                    findViewById(R.id.sv_log).setVisibility(View.VISIBLE);
                    findViewById(R.id.fl_charge).setVisibility(View.GONE);
                    findViewById(R.id.fl_slide).setVisibility(View.GONE);
                    setTitle(R.string.det_check);
                    tvLog.setText(R.string.camera_detecting);
                }).show());
    }

    private int maxDelay() {
        int result = 0;
        for (DetonatorBean b : detonatorList)
            result = Math.max(result, b.getDelayTime());
        return result;
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
        }
    }

    private void changeActionBar() {
        myApp.myToast(DetonateStep3Activity.this, R.string.message_charge_success);
        if (uniteExplode)
            explode();
        else {
            setTitle(R.string.det_slide);
            setProgressVisibility(false);
            findViewById(R.id.sv_log).setVisibility(View.GONE);
            findViewById(R.id.fl_charge).setVisibility(View.GONE);
            findViewById(R.id.fl_slide).setVisibility(View.VISIBLE);
        }
    }

    private void breakExplode(@StringRes int message) {
        myHandler.removeCallbacksAndMessages(null);
        myReceiveListener.setStartAutoDetect(false);
        explodeStep = 2;
        myHandler.sendEmptyMessage(HANDLER_START);
        if (chargeFinished)
            resetStatus();
        enabledKeys = false;
        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                    if (nextStep)
                        finish();
                    else {
                        nextStep = true;
                        if (uniteExplode)
                            setResult(RESULT_CANCELED);
                    }
                }).show()));
    }

    private void resetStatus() {
        confirmExplode = false;
        keepingEnd = false;
        bothKeyUp = false;
        leftKeyDown = false;
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        BaseApplication.writeFile("key:" + event.getAction() + "," + event.getKeyCode());
        if (enabledKeys) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case ConstantUtils.KEYCODE_LEFT_CHARGE:
                        if (notAllow)
                            myApp.myToast(this, R.string.message_not_allow_area);
                        else if (!leftKeyDown && !rightKeyDown && chargeFinished) {
                            runOnUiThread(() -> sliderImageView.startMove(true));
                            leftKeyDown = true;
                        }
                        break;
                    case ConstantUtils.KEYCODE_RIGHT_CHARGE:
                        if (notAllow)
                            myApp.myToast(this, R.string.message_not_allow_area);
                        else if (leftKeyDown && keepingEnd && !rightKeyDown && !confirmExplode) {
                            rightKeyDown = true;
                            countDown = 0;
                            runOnUiThread(() -> {
                                tvPercentage.setText("0%");
                                progressBar.setProgress(0);
                                progressBar.setVisibility(View.VISIBLE);
                                tvPercentage.setVisibility(View.VISIBLE);
                                myHandler.sendEmptyMessageDelayed(HANDLER_PRESS_KEY, 100);
                            });
                        }
                        break;
                    default:
                        if (chargeFinished)
                            resetStatus();
                        break;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case ConstantUtils.KEYCODE_LEFT_CHARGE:
                        if (!rightKeyDown) {
                            tvHint.setText(R.string.det_slide_hint);
                            sliderImageView.startMove(false);
                        } else {
                            if (keepingEnd && !confirmExplode)
                                resetStatus();
                        }
                        leftKeyDown = false;
                        break;
                    case ConstantUtils.KEYCODE_RIGHT_CHARGE:
                        if (keepingEnd && !confirmExplode && leftKeyDown) {
                            myHandler.removeMessages(HANDLER_PRESS_KEY);
                            countDown = 0;
                            runOnUiThread(() -> {
                                tvPercentage.setText("0%");
                                progressBar.setProgress(0);
                                progressBar.setVisibility(View.INVISIBLE);
                                tvPercentage.setVisibility(View.INVISIBLE);
                            });
                        }
                        rightKeyDown = false;
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
                                resetStatus();
                            }
                        }, KEY_UP_TIME * 2);
                    } else {
                        if (System.currentTimeMillis() - keyUpPeriod < KEY_UP_TIME) {
                            explode();
                        }
                        resetStatus();
                    }
                } else if (keepingEnd && !leftKeyDown && !rightKeyDown) {
                    resetStatus();
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }


    private void explode() {
        if (uniteExplode) {
            nextStep = true;
            setResult(RESULT_OK);
            finish();
        } else {
            myReceiveListener.setStartAutoDetect(false);
            myHandler.removeCallbacksAndMessages(null);
            explodeStep = 3;
            myHandler.sendEmptyMessage(HANDLER_START);
            enabledKeys = false;
            pDialog = new MyProgressDialog(this);
            pDialog.setInverseBackgroundForced(false);
            pDialog.setCancelable(false);
            pDialog.setCanceledOnTouchOutside(false);
            pDialog.setTitle(R.string.progress_title);
            pDialog.setMessage(getString(R.string.progress_explode));
            pDialog.show();
            myHandler.sendEmptyMessage(HANDLER_PROGRESS);
        }
    }

    @Override
    public void finish() {
        if (countingStatus == 2)
            return;
        if (!nextStep) {
            if (countingStatus > 0)
                myHandler.removeMessages(HANDLER_COUNT_DOWN);
            else if (chargeFinished)
                resetStatus();
            enabledKeys = false;
            sliderImageView.setEnabled(false);
            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep3Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_explode)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_exit_explode, (dialogInterface, i) -> {
                        if (uniteExplode)
                            setResult(RESULT_CANCELED);
                        nextStep = true;
                        countScanZero = 0;
                        myReceiveListener.setStartAutoDetect(false);
                        explodeStep = 2;
                        if (countingStatus > 0) {
                            resetStatus();
                            runOnUiThread(() -> findViewById(R.id.fl_count).setVisibility(View.GONE));
                        }
                        myHandler.sendEmptyMessage(HANDLER_START);
                    }).setNegativeButton(R.string.button_continue_explode, (dialog, which) -> {
                        enabledKeys = true;
                        sliderImageView.setEnabled(true);
                        if (countingStatus > 0)
                            myHandler.sendEmptyMessageDelayed(HANDLER_COUNT_DOWN, 500);
                    }).show());
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
        BaseApplication.releaseWakeLock(DetonateStep3Activity.this);
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.release();
            soundPool = null;
        }
        if (!uniteExplode && null != myReceiveListener)
            myReceiveListener.closeAllHandler();
        super.onDestroy();
    }
}
