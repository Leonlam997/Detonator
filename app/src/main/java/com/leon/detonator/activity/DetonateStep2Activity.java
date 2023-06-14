package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.dialog.CustomProgressDialog;
import com.leon.detonator.fragment.TabFragment;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialDataReceiveListener;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DetonateStep2Activity extends BaseActivity {
    private final int DETECT_SUCCESS = 1;
    private final int DETECT_FINISH = 2;
    private final int DETECT_RESCAN = 3;
    private final int DETECT_NEXT_STEP = 6;
    private final int DETECT_SEND_COMMAND = 7;
    private final int STEP_SET_PARAM_LEVEL = 1;
    private final int STEP_RELEASE_1 = 2;
    private final int STEP_RELEASE_2 = 3;
    private final int STEP_RELEASE_3 = 4;
    private final int STEP_RESET_1 = 5;
    private final int STEP_RESET_2 = 6;
    private final int STEP_RESET_3 = 7;
    private final int STEP_INITIAL = 8;
    private final int STEP_CHECK_CONFIG = 9;
    private final int STEP_CLEAR_STATUS = 10;
    private final int STEP_SCAN = 11;
    private final int STEP_READ_SHELL = 12;
    private final int STEP_WRITE_FIELD = 13;
    private final int STEP_READ_FIELD = 14;
    private final int STEP_LOCK_1 = 15;
    private final int STEP_LOCK_2 = 16;
    private final int STEP_LOCK_3 = 17;
    private boolean nextStep;
    private boolean checkedHint;
    private int listIndex = 0;
    private int hiddenKeyCount = 0;
    private int hiddenType = 0;
    private int flowStep;
    private int countScanZero;
    private int soundSuccess;
    private int soundAlert;
    private float chargeVoltage;
    private String[] fileList;
    private ConstantUtils.ListType rescanWhich;
    private SerialDataReceiveListener myReceiveListener;
    private SoundPool soundPool;
    private List<List<DetonatorInfoBean>> lists;
    private List<TabFragment> fragments;
    private Map<ConstantUtils.ListType, String> tabTitle;
    private List<DetonatorListAdapter> adapterList;
    private SerialPortUtil serialPortUtil;
    private MyButton btnCharge;
    private MyButton btnRescan;
    private TabLayout tabList;
    private ViewPager pagerList;
    private CustomProgressDialog pDialog;
    private BaseApplication myApp;

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_SUCCESS:
                    adapterList.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                    resetTabTitle(false);
                    break;
                case DETECT_FINISH:
                    adapterList.get(ConstantUtils.ListType.ALL.ordinal()).setEnabled(true);
                    BaseApplication.releaseWakeLock(DetonateStep2Activity.this);
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    btnCharge.setEnabled(lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() == 0 && lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size() == 0
                            && lists.get(ConstantUtils.ListType.END.ordinal()).size() == lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size());
                    if (pDialog != null) {
                        if (pDialog.isShowing())
                            pDialog.dismiss();
                        pDialog = null;
                    }
                    rescanWhich = ConstantUtils.ListType.END;
                    btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.ListType.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                    setProgressVisibility(false);
                    myReceiveListener.setStartAutoDetect(true);
                    int i = lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size() > 0 ? ConstantUtils.ListType.NOT_FOUND.ordinal() :
                            (lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() > 0 ? ConstantUtils.ListType.ERROR.ordinal() : 0);
                    if (i > 0) {
                        pagerList.setCurrentItem(i, true);
                    }
                    break;
                case DETECT_RESCAN:
                    BaseApplication.acquireWakeLock(DetonateStep2Activity.this);
                    checkedHint = true;
                    myReceiveListener.setDetonatorAmount(lists.get(ConstantUtils.ListType.END.ordinal()).size());
                    resetTabTitle(false);
                    adapterList.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                    pDialog.show();
                    pDialog.setMax(lists.get(rescanWhich == ConstantUtils.ListType.NOT_FOUND ? ConstantUtils.ListType.NOT_FOUND.ordinal() : ConstantUtils.ListType.END.ordinal()).size());
                    pDialog.setProgress(0);
                    pDialog.setSecondaryProgress(0);
                    pDialog.setOnCancelListener(dialogInterface -> finish());
                    pDialog.setMessage(R.string.progress_detect);
                    btnCharge.setEnabled(false);
                    btnRescan.setEnabled(false);
                    setProgressVisibility(true);
                    myReceiveListener.setStartAutoDetect(false);
                    listIndex = 0;
                    flowStep = rescanWhich == ConstantUtils.ListType.NOT_FOUND ? STEP_READ_FIELD : STEP_LOCK_1;
                    countScanZero = 0;
                    myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                    break;
                case DETECT_NEXT_STEP:
                    switch (flowStep) {
                        case STEP_LOCK_1:
                            flowStep = STEP_LOCK_2;
                            break;
                        case STEP_LOCK_2:
                            flowStep = STEP_LOCK_3;
                            break;
                        case STEP_LOCK_3:
                        case STEP_SET_PARAM_LEVEL:
                            flowStep = STEP_RELEASE_1;
                            break;
                        case STEP_RELEASE_1:
                            flowStep = STEP_RELEASE_2;
                            break;
                        case STEP_RELEASE_2:
                            flowStep = STEP_RELEASE_3;
                            break;
                        case STEP_RELEASE_3:
                            flowStep = STEP_RESET_1;
                            break;
                        case STEP_RESET_1:
                            flowStep = STEP_RESET_2;
                            break;
                        case STEP_RESET_2:
                            flowStep = STEP_RESET_3;
                            break;
                        case STEP_RESET_3:
                            flowStep = STEP_INITIAL;
                            break;
                        case STEP_INITIAL:
                            flowStep = STEP_CHECK_CONFIG;
                            break;
                        case STEP_CHECK_CONFIG:
                            flowStep = STEP_CLEAR_STATUS;
                            break;
                        case STEP_CLEAR_STATUS:
                            flowStep = STEP_SCAN;
                            break;
                    }
                case DETECT_SEND_COMMAND:
                    myHandler.removeMessages(DETECT_SEND_COMMAND);
                    if (null == serialPortUtil)
                        return false;
                    switch (flowStep) {
                        case STEP_SET_PARAM_LEVEL:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_SET_PARAM_LEVEL, 1);
                            break;
                        case STEP_LOCK_1:
                        case STEP_LOCK_2:
                        case STEP_LOCK_3:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_LOCK, 0);
                            break;
                        case STEP_RELEASE_1:
                        case STEP_RELEASE_2:
                        case STEP_RELEASE_3:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHARGE, 0);
                            break;
                        case STEP_RESET_1:
                        case STEP_RESET_2:
                        case STEP_RESET_3:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_RESET, 0);
                            break;
                        case STEP_INITIAL:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_INITIAL, 0);
                            break;
                        case STEP_CHECK_CONFIG:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHECK_CONFIG, ConstantUtils.UID_LEN, ConstantUtils.PSW_LEN, ConstantUtils.DETONATOR_VERSION);
                            break;
                        case STEP_CLEAR_STATUS:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CLEAR_READ_STATUS, 0);
                            break;
                        case STEP_SCAN:
                            if (countScanZero < ConstantUtils.SCAN_ZERO_COUNT) {
                                serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_UID, ConstantUtils.UID_LEN);
                                myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_SCAN_UID_TIMEOUT);
                                return false;
                            } else {
                                listIndex = 0;
                                int amount = 0;
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.ListType.DETECTED.ordinal()))
                                    if (!b.isDownloaded())
                                        amount++;
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.ListType.END.ordinal()))
                                    if (!lists.get(ConstantUtils.ListType.DETECTED.ordinal()).contains(b)) {
                                        lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).add(b);
                                        BaseApplication.writeFile("不在线：" + b.getAddress());
                                    }
                                myHandler.sendEmptyMessage(DETECT_SUCCESS);
                                if (0 == amount && lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() == 0) {
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                    myHandler.sendEmptyMessage(DETECT_FINISH);
                                    return false;
                                } else {
                                    pDialog.setProgress(0);
                                    if (lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() > 0) {
                                        pDialog.setMax(lists.get(ConstantUtils.ListType.ERROR.ordinal()).size());
                                        pDialog.setMessage(R.string.progress_reading);
                                        flowStep = STEP_READ_SHELL;
                                    } else {
                                        pDialog.setMax(amount);
                                        pDialog.setMessage(R.string.progress_set_delay);
                                        nextDownloadIndex();
                                        flowStep = STEP_WRITE_FIELD;
                                    }
                                    myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                                }
                            }
                            break;
                        case STEP_READ_SHELL:
                            if (listIndex >= lists.get(ConstantUtils.ListType.ERROR.ordinal()).size()) {
                                listIndex = 0;
                                int amount = 0;
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.ListType.DETECTED.ordinal()))
                                    if (!b.isDownloaded())
                                        amount++;
                                pDialog.setProgress(0);
                                pDialog.setMax(amount);
                                pDialog.setMessage(R.string.progress_set_delay);
                                nextDownloadIndex();
                                flowStep = STEP_WRITE_FIELD;
                                myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                                return false;
                            }
                            serialPortUtil.sendCmd(lists.get(ConstantUtils.ListType.ERROR.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            return false;
                        case STEP_WRITE_FIELD:
                            if (listIndex >= lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size()) {
                                if (rescanWhich != ConstantUtils.ListType.NOT_FOUND && lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size() > 0) {
                                    rescanWhich = ConstantUtils.ListType.NOT_FOUND;
                                    myHandler.sendEmptyMessage(DETECT_RESCAN);
                                } else {
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                    myHandler.sendEmptyMessage(DETECT_FINISH);
                                }
                                return false;
                            }
                            DetonatorInfoBean bean = lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex);
                            serialPortUtil.sendCmd(bean.getAddress(), SerialCommand.CODE_WRITE_FIELD,
                                    lists.get(ConstantUtils.ListType.END.ordinal()).indexOf(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex)) + 1
                                    , bean.getDelayTime(), bean.getHole());
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            return false;
                        case STEP_READ_FIELD:
                            if (listIndex >= lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size()) {
                                lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).clear();
                                countScanZero = ConstantUtils.SCAN_ZERO_COUNT;
                                flowStep = STEP_SCAN;
                                myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                            } else {
                                serialPortUtil.sendCmd(lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_FIELD, ConstantUtils.UID_LEN, 0, 0, 0);
                                myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            }
                            return false;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                    break;
                default:
                    myApp.myToast(DetonateStep2Activity.this, (String) msg.obj);
                    break;
            }
            return false;
        }
    });

    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] received = myReceiveListener.getRcvData();
            if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT || received[0] == SerialCommand.ALERT_LARGE_CURRENT) {
                closeAllHandler();
                if (serialPortUtil != null) {
                    serialPortUtil.closeSerialPort();
                    serialPortUtil = null;
                }
                runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_warning)
                        .setMessage(received[0] == SerialCommand.ALERT_SHORT_CIRCUIT ? R.string.dialog_short_circuit : R.string.dialog_large_current)
                        .setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                            saveList();
                            nextStep = true;
                            finish();
                        })
                        .show(), true));
                myApp.playSoundVibrate(soundPool, soundAlert);
            } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                checkBoxStatus();
                if (rescanWhich == ConstantUtils.ListType.END) {
                    myHandler.sendEmptyMessage(DETECT_FINISH);
                } else if (rescanWhich == ConstantUtils.ListType.ALL) {
                    myHandler.sendEmptyMessage(DETECT_RESCAN);
                }
            } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                myApp.myToast(DetonateStep2Activity.this, R.string.message_open_module_fail);
                nextStep = true;
                finish();
            } else if (received.length > 5) {
                myHandler.removeMessages(DETECT_SEND_COMMAND);
                myHandler.removeMessages(DETECT_NEXT_STEP);
                if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                    switch (flowStep) {
                        case STEP_LOCK_1:
                        case STEP_LOCK_2:
                        case STEP_LOCK_3:
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.LOCK_DELAY_TIME);
                            return;
                        case STEP_RELEASE_1:
                        case STEP_RELEASE_2:
                        case STEP_RELEASE_3:
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.RELEASE_DELAY_TIME);
                            return;
                        case STEP_RESET_1:
                        case STEP_RESET_2:
                        case STEP_RESET_3:
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.RESET_DELAY_TIME);
                            return;
                        case STEP_READ_FIELD:
                        case STEP_SCAN:
                            try {
                                DetonatorInfoBean bean;
                                if (flowStep == STEP_READ_FIELD) {
                                    bean = new DetonatorInfoBean(lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).get(listIndex).getAddress(),
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 4]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 5]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 6]),//Delay
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 2]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 3]),//Number
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 7]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 8]),//Hole
                                            Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]), false);//Status
                                    lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).remove(listIndex);
                                } else {
                                    boolean isNull = true;
                                    for (int i = 0; i < 7; i++)
                                        if (received[SerialCommand.CODE_CHAR_AT + 2 + i] != 0) {
                                            isNull = false;
                                            break;
                                        }
                                    if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                        received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                                    bean = new DetonatorInfoBean(isNull ? ConstantUtils.NULL_ID : new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9)),
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 11]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 12]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 13]),//Delay
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 10]),//Number
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 14]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 15]),//Hole
                                            Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 16]), false);//Status
                                    listIndex++;
                                }
                                BaseApplication.writeFile("雷壳码：" + bean.getAddress());
                                pDialog.incrementProgressBy(1);
                                pDialog.setSecondaryProgress(45 * pDialog.getProgress() / (pDialog.getMax() + ConstantUtils.SCAN_ZERO_COUNT));
                                countScanZero = 0;
                                if (ConstantUtils.NULL_ID.equals(bean.getAddress()))
                                    lists.get(ConstantUtils.ListType.ERROR.ordinal()).add(bean);
                                else if (flowStep == STEP_READ_FIELD || Pattern.matches(ConstantUtils.UID_PATTERN, bean.getAddress())) {
                                    int i = lists.get(ConstantUtils.ListType.END.ordinal()).indexOf(bean);
                                    if (i >= 0) {
                                        DetonatorInfoBean b1 = lists.get(ConstantUtils.ListType.END.ordinal()).get(i);
                                        if (bean.getDelayTime() == b1.getDelayTime() && i + 1 == bean.getRow() && b1.getHole() == bean.getHole()) {
                                            b1.setDownloaded(true);
                                            int j = lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(b1);
                                            if (j >= 0)
                                                lists.get(ConstantUtils.ListType.ALL.ordinal()).get(j).setDownloaded(true);
                                        }
                                        if (lists.get(ConstantUtils.ListType.DETECTED.ordinal()).contains(b1)) {
                                            myApp.myToast(DetonateStep2Activity.this, String.format(getString(R.string.message_detect_multiple), b1.getAddress()));
                                        } else {
                                            if (lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size() == 0)
                                                lists.get(ConstantUtils.ListType.DETECTED.ordinal()).add(b1);
                                            else {
                                                int k = 0;
                                                for (; k < lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size(); k++)
                                                    if (lists.get(ConstantUtils.ListType.END.ordinal()).indexOf(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(k)) > i) {
                                                        lists.get(ConstantUtils.ListType.DETECTED.ordinal()).add(k, b1);
                                                        break;
                                                    }
                                                if (k >= lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size())
                                                    lists.get(ConstantUtils.ListType.DETECTED.ordinal()).add(b1);
                                            }
                                        }
                                    } else
                                        lists.get(ConstantUtils.ListType.ERROR.ordinal()).add(bean);
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            myHandler.sendEmptyMessage(DETECT_SUCCESS);
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                            return;
                        case STEP_READ_SHELL:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(45 + 10 * pDialog.getProgress() / pDialog.getMax());
                            if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                            String address = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                            BaseApplication.writeFile("雷壳码：" + address);
                            if (Pattern.matches(ConstantUtils.SHELL_PATTERN, address)) {
                                lists.get(ConstantUtils.ListType.ERROR.ordinal()).get(listIndex).setAddress(address);
                            }
                            listIndex++;
                            break;
                        case STEP_WRITE_FIELD:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(55 + 45 * pDialog.getProgress() / pDialog.getMax());
                            lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex).setDownloaded(true);
                            lists.get(ConstantUtils.ListType.ALL.ordinal()).get(lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex))).setDownloaded(true);
                            nextDownloadIndex();
                            break;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                } else {
                    if (flowStep != STEP_SCAN && flowStep != STEP_WRITE_FIELD)
                        BaseApplication.writeFile("返回错误！");
                    switch (flowStep) {
                        case STEP_LOCK_1:
                        case STEP_LOCK_2:
                        case STEP_LOCK_3:
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.LOCK_DELAY_TIME);
                            return;
                        case STEP_SCAN:
                            boolean allZero = true;
                            for (int i = SerialCommand.CODE_CHAR_AT + 2; i < SerialCommand.CODE_CHAR_AT + 9; i++) {
                                if (0 != received[i]) {
                                    allZero = false;
                                    break;
                                }
                            }
                            if (allZero) {
                                BaseApplication.writeFile("点名结束！" + countScanZero);
                                countScanZero++;
                            }
                            listIndex++;
                            pDialog.setSecondaryProgress(45 * (pDialog.getProgress() + countScanZero) / (pDialog.getMax() + ConstantUtils.SCAN_ZERO_COUNT));
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.SCAN_DELAY_TIME);
                            return;
                        case STEP_CHECK_CONFIG:
                            myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_error);
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                            return;
                        case STEP_READ_SHELL:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(45 + 10 * pDialog.getProgress() / pDialog.getMax());
                            listIndex++;
                            break;
                        case STEP_WRITE_FIELD:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(55 + 45 * pDialog.getProgress() / pDialog.getMax());
                            lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex).setDownloaded(true);
                            lists.get(ConstantUtils.ListType.ALL.ordinal()).get(lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex))).setDownloaded(true);
                            nextDownloadIndex();
                            break;
                        case STEP_READ_FIELD:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(45 * pDialog.getProgress() / (pDialog.getMax() + ConstantUtils.SCAN_ZERO_COUNT));
                            listIndex++;
                            break;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.COMMAND_DELAY_TIME);
                }
            }
        }
    };

    private void nextDownloadIndex() {
        while (listIndex < lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size() && lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex).isDownloaded())
            listIndex++;
        myHandler.sendEmptyMessage(DETECT_SUCCESS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonate_step2);

        btnCharge = findViewById(R.id.btn_charge);
        if (getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false)) {
            setTitle(R.string.check_detonator, R.string.subtitle_unite);
            if (btnCharge.getKeyCode() != -1)
                btnCharge.setText(String.format(Locale.getDefault(), "%d.%s", btnCharge.getKeyCode(), getString(R.string.button_enter_unite)));
            else
                btnCharge.setText(R.string.button_enter_unite);
        } else
            setTitle(R.string.check_detonator);
        setProgressVisibility(true);
        myApp = (BaseApplication) getApplication();

        lists = new ArrayList<>();
        checkedHint = true;
        fileList = new String[FilePath.FILE_LIST[0].length + 1];
        fileList[0] = myApp.getListFile();
        System.arraycopy(FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1], 0, fileList, 1, FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1].length);
        rescanWhich = ConstantUtils.ListType.END;
        try {
            for (int i = 0; i < fileList.length; i++) {
                lists.add(new ArrayList<>());
                myApp.readFromFile(fileList[i], lists.get(i), DetonatorInfoBean.class);
            }
            if (lists.get(ConstantUtils.ListType.ALL.ordinal()).size() <= 0) {
                myApp.myToast(DetonateStep2Activity.this, R.string.message_list_not_found);
                nextStep = true;
                finish();
                return;
            } else {
                for (DetonatorInfoBean bean : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                    bean.setDownloaded(false);
            }
            if (lists.get(ConstantUtils.ListType.END.ordinal()).size() > 0) {
                Iterator<DetonatorInfoBean> iterator = lists.get(ConstantUtils.ListType.END.ordinal()).iterator();
                while (iterator.hasNext()) {
                    int i = lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(iterator.next());
                    if (i >= 0)
                        lists.get(ConstantUtils.ListType.ALL.ordinal()).get(i).setSelected(true);
                    else
                        iterator.remove();
                }
            } else {
                lists.get(ConstantUtils.ListType.END.ordinal()).addAll(lists.get(ConstantUtils.ListType.ALL.ordinal()));
                for (DetonatorInfoBean b : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                    b.setSelected(true);
            }
            initPager();
            btnCharge.setEnabled(false);
            btnRescan = findViewById(R.id.btn_rescan);
            btnRescan.requestFocus();
            btnRescan.setEnabled(false);
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(DetonateStep2Activity.this, bufferRunnable);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            myReceiveListener.setDetonatorAmount(lists.get(ConstantUtils.ListType.END.ordinal()).size());
            btnCharge.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));
            btnRescan.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initSound();
    }

    private void resetTabTitle(boolean init) {
        for (int i = 0; i < tabTitle.size(); i++) {
            TextView tv = (TextView) LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_tab_view, tabList, false);
            String text = tabTitle.get(ConstantUtils.ListType.values()[i]) + "(" + lists.get(i == 0 ? ConstantUtils.ListType.END.ordinal() : i).size() +
                    (i == 0 ? "/" + lists.get(ConstantUtils.ListType.ALL.ordinal()).size() : "") + ")";
            tv.setText(text);
            TabLayout.Tab tab = tabList.getTabAt(i);
            if (tab != null) {
                if (tab.isSelected())
                    tv.setTextColor(getColor(R.color.text_blue));
                else
                    tv.setTextColor(getColor(R.color.text_black));
                if (!init && tab.getCustomView() != null) {
                    final ViewParent customParent = tab.getCustomView().getParent();
                    if (customParent != null) {
                        ((ViewGroup) customParent).removeView(tab.getCustomView());
                    }
                }
                tab.setCustomView(tv);
            }
        }
    }

    private void initPager() {
        tabList = findViewById(R.id.tab_title);
        pagerList = findViewById(R.id.view_pager);
        fragments = new ArrayList<>();
        adapterList = new ArrayList<>();
        int[] title = {R.string.tab_title_all_list, R.string.tab_title_online, R.string.tab_title_offline, R.string.tab_title_delay_error};
        tabTitle = new HashMap<>();
        for (int i = 0; i < ConstantUtils.ListType.END.ordinal(); i++)
            tabTitle.put(ConstantUtils.ListType.values()[i], getString(title[i]));

        for (int i = 0; i < tabTitle.size(); i++) {
            tabList.addTab(tabList.newTab());

            DetonatorListAdapter adapter = new DetonatorListAdapter(this, lists.get(i));
            adapter.setCanSelect(0 == i);
            adapter.setTunnel(myApp.isTunnel());
            TabFragment tabFragment = new TabFragment(adapter, 0 == i ? pos -> {
                if (checkedHint) {
                    runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                            .setTitle(R.string.progress_title)
                            .setMessage(R.string.dialog_clear_detected)
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> checked(pos))
                            .setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
                                if (pos == -1)
                                    fragments.get(0).allChecked(!fragments.get(0).isAllChecked());
                            })
                            .setOnCancelListener(dialog -> {
                                if (pos == -1)
                                    fragments.get(0).allChecked(!fragments.get(0).isAllChecked());
                            })
                            .show(), true));
                } else {
                    checked(pos);
                }
            } : null);
            adapterList.add(adapter);
            fragments.add(tabFragment);
        }
        pagerList.setAdapter(new ListPagerAdapter(getSupportFragmentManager()));
        tabList.setupWithViewPager(pagerList);

        resetTabTitle(true);
        tabList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                resetTabTitle(false);
                pagerList.setCurrentItem(tab.getPosition());
                adapterList.get(tab.getPosition()).updateList(lists.get(tab.getPosition()));
                if (rescanWhich == ConstantUtils.ListType.END) {
                    btnRescan.setEnabled(tab.getPosition() == ConstantUtils.ListType.DETECTED.ordinal() ||
                            (tab.getPosition() != ConstantUtils.ListType.ERROR.ordinal() && lists.get(tab.getPosition()).size() > 0));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void checked(int pos) {
        if (pos >= 0) {
            if (pos < lists.get(ConstantUtils.ListType.ALL.ordinal()).size()) {
                DetonatorInfoBean item = lists.get(ConstantUtils.ListType.ALL.ordinal()).get(pos);
                item.setSelected(!item.isSelected());
            }
            checkBoxStatus();
        } else
            for (DetonatorInfoBean bean : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                bean.setSelected(fragments.get(0).isAllChecked());
        adapterList.get(ConstantUtils.ListType.ALL.ordinal()).updateList(lists.get(ConstantUtils.ListType.ALL.ordinal()));
        if (!btnRescan.isEnabled()) {
            myHandler.sendEmptyMessage(DETECT_FINISH);
        }
        myReceiveListener.setDetonatorAmount(0);
        for (int j = ConstantUtils.ListType.ALL.ordinal() + 1; j <= ConstantUtils.ListType.END.ordinal(); j++)
            lists.get(j).clear();
        for (DetonatorInfoBean bean : lists.get(ConstantUtils.ListType.ALL.ordinal()))
            if (bean.isSelected())
                lists.get(ConstantUtils.ListType.END.ordinal()).add(bean);

        BaseApplication.writeFile("Change List:" + lists.get(ConstantUtils.ListType.ALL.ordinal()).size() + " to " + lists.get(ConstantUtils.ListType.END.ordinal()).size());
        checkedHint = false;
        runOnUiThread(() -> {
            resetTabTitle(false);
            btnRescan.setEnabled(lists.get(ConstantUtils.ListType.END.ordinal()).size() > 0);
            btnCharge.setEnabled(false);
        });
    }

    private void checkBoxStatus() {
        fragments.get(0).allChecked(true);
        for (DetonatorInfoBean bean : lists.get(ConstantUtils.ListType.ALL.ordinal())) {
            if (!bean.isSelected()) {
                fragments.get(0).allChecked(false);
                break;
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        executeFunction(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void executeFunction(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                if (btnRescan.isEnabled()) {
                    int max = maxDelay();
                    if (max > ConstantUtils.MAX_DELAY_TIME) {
                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(String.format(Locale.getDefault(), getString(R.string.dialog_delay_out_of_range), ConstantUtils.MAX_DELAY_TIME))
                                .setPositiveButton(R.string.btn_confirm, null)
                                .show(), true));
                    } else if (max < 0) {
                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(R.string.dialog_delay_out_of_range_2)
                                .setPositiveButton(R.string.btn_confirm, null)
                                .show(), true));
                    } else if (btnCharge.isEnabled()) {
                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_detect_again)
                                .setMessage(R.string.dialog_detect_again)
                                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> startDetect())
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show(), true));
                    } else {
                        startDetect();
                    }
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (btnCharge.isEnabled()) {
                    BaseApplication.writeFile(getString(R.string.button_charge));
                    enterCharge();
                }
                break;
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
                if (hiddenKeyCount == 3) {
                    hiddenType = which - KeyEvent.KEYCODE_1 + 1;
                    hiddenKeyCount++;
                } else
                    hiddenKeyCount = 0;
                break;
            case KeyEvent.KEYCODE_STAR:
                if (hiddenKeyCount == 5) {
                    hiddenKeyCount = 0;
                    if (hiddenType == 5)
                        enterCharge();
                    else if (hiddenType == 3) {
                        final View view = LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_edit_dialog, null);
                        final EditText etDelay = view.findViewById(R.id.et_dialog);
                        view.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                        etDelay.setHint(R.string.hint_input_voltage);
                        etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                        etDelay.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_edit_charge_voltage)
                                .setView(view)
                                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                                    try {
                                        float v = Float.parseFloat(etDelay.getText().toString());
                                        if (v >= 18 && v <= 22) {
                                            chargeVoltage = v;
                                            LocalSettingBean bean = BaseApplication.readSettings();
                                            bean.setChargeVoltage(v);
                                            myApp.saveBean(bean);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_input_voltage_error);
                                })
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show(), false);
                    }
                } else if (hiddenKeyCount == 0 || hiddenKeyCount == 1 || hiddenKeyCount == 4)
                    hiddenKeyCount++;
                else
                    hiddenKeyCount = 0;
                break;
            case KeyEvent.KEYCODE_0:
                if (hiddenKeyCount == 2)
                    hiddenKeyCount++;
                else
                    hiddenKeyCount = 0;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                hiddenKeyCount = 0;
                if (tabList.getSelectedTabPosition() > 0) {
                    TabLayout.Tab tab = tabList.getTabAt(tabList.getSelectedTabPosition() - 1);
                    if (tab != null) {
                        tab.select();
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                hiddenKeyCount = 0;
                if (tabList.getSelectedTabPosition() < tabList.getTabCount() - 1) {
                    TabLayout.Tab tab = tabList.getTabAt(tabList.getSelectedTabPosition() + 1);
                    if (tab != null) {
                        tab.select();
                    }
                }
                break;
            default:
                hiddenKeyCount = 0;
                break;
        }
    }

    private void enterCharge() {
        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                .setView(R.layout.layout_enter_charge_dialog)
                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                    closeAllHandler();
                    Intent intent = new Intent();
                    intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_VOLTAGE, chargeVoltage);
                    intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
                    intent.setClass(DetonateStep2Activity.this, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false) ? UniteExplodeActivity.class : DetonateStep3Activity.class);
                    saveList();
                    startActivity(intent);
                    nextStep = true;
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show(), false);
    }

    private int maxDelay() {
        int result = 0;
        for (DetonatorInfoBean bean : lists.get(ConstantUtils.ListType.END.ordinal())) {
            if (bean.getDelayTime() > result)
                result = bean.getDelayTime();
            if (bean.getDelayTime() < 0)
                return -1;
        }
        return result;
    }

    private void startDetect() {
        pDialog = new CustomProgressDialog(this);
        pDialog.setCanceledOnTouchOutside(false);
        if (tabList.getSelectedTabPosition() != 2) {
            BaseApplication.writeFile(getString(R.string.button_start_detect) + ", " + getString(R.string.tab_title_online) + ":" + lists.get(ConstantUtils.ListType.END.ordinal()).size());
            for (int i = ConstantUtils.ListType.ALL.ordinal() + 1; i < ConstantUtils.ListType.END.ordinal(); i++)
                lists.get(i).clear();
            for (DetonatorInfoBean d : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                d.setDownloaded(false);
            for (DetonatorInfoBean d : lists.get(ConstantUtils.ListType.END.ordinal()))
                d.setDownloaded(false);
            rescanWhich = ConstantUtils.ListType.ALL;
        } else {
            BaseApplication.writeFile(getString(R.string.button_start_detect) + ", " + getString(R.string.tab_title_offline) + ":" + lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size());
            rescanWhich = ConstantUtils.ListType.NOT_FOUND;
        }
        myReceiveListener.setStartAutoDetect(false);
        myHandler.sendEmptyMessage(DETECT_RESCAN);
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

    private void closeAllHandler() {
        myHandler.removeCallbacksAndMessages(null);
        if (myReceiveListener != null) {
            myReceiveListener.setStartAutoDetect(false);
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
    }

    @Override
    public void finish() {
        if (!nextStep) {
            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_detect)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                        if (pDialog != null && pDialog.isShowing())
                            pDialog.dismiss();
                        DetonateStep2Activity.super.finish();
                    })
                    .setNegativeButton(R.string.btn_cancel, (dialogInterface, i) -> {
                        if (pDialog != null && pDialog.getMax() != pDialog.getProgress())
                            pDialog.show();
                    })
                    .show(), true);
        } else
            super.finish();
    }

    @Override
    public void onDestroy() {
        closeAllHandler();
        if (serialPortUtil != null && !nextStep) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        if (!nextStep) {
            saveList();
        }
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        BaseApplication.releaseWakeLock(DetonateStep2Activity.this);
        super.onDestroy();
    }

    private void saveList() {
        try {
            for (int i = ConstantUtils.ListType.ALL.ordinal() + 1; i < ConstantUtils.ListType.END.ordinal(); i++)
                lists.get(i).clear();
            for (int i = ConstantUtils.ListType.ALL.ordinal(); i <= ConstantUtils.ListType.END.ordinal(); i++) {
                if (lists.get(i).size() > 0) {
                    myApp.writeToFile(fileList[i], lists.get(i));
                } else {
                    File file = new File(fileList[i]);
                    if (file.exists() && !file.delete())
                        myApp.myToast(DetonateStep2Activity.this, String.format(Locale.getDefault(), getString(R.string.message_delete_file_fail), file.getName()));
                }
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private class ListPagerAdapter extends FragmentPagerAdapter {
        ListPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

    }
}
