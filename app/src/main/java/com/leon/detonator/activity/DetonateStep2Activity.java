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
    private List<List<DetonatorInfoBean>> lists;
    private List<DetonatorInfoBean> checkList;
    private List<Fragment> fragments;
    private Map<ConstantUtils.LIST_TYPE, String> tabTitle;
    private List<DetonatorListAdapter> adapterList;
    private SerialPortUtil serialPortUtil;
    private boolean nextStep = false;
    private boolean scanAll = false;
    private MyButton btnCharge, btnRescan;
    private TabLayout tabList;
    private ViewPager pagerList;
    private CustomProgressDialog pDialog;
    private BaseApplication myApp;
    private int listIndex = 0;
    private int hiddenKeyCount = 0;
    private int hiddenType = 0;
    private int flowStep;
    private int countScanZero;
    private int soundSuccess;
    private int soundAlert;
    private ConstantUtils.LIST_TYPE rescanWhich;
    private SerialDataReceiveListener myReceiveListener;
    private SoundPool soundPool;
    private String[] fileList;
    private TabFragment allListTab;

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case DETECT_SUCCESS:
                    adapterList.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                    resetTabTitle(false);
                    break;
                case DETECT_FINISH:
                    adapterList.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).setEnabled(true);
                    checkAllListHint();
                    BaseApplication.releaseWakeLock(DetonateStep2Activity.this);
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    btnCharge.setEnabled(lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() == 0 && lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size() == 0
                            && lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size() == lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size());
                    if (pDialog != null) {
                        if (pDialog.isShowing())
                            pDialog.dismiss();
                        pDialog = null;
                    }
                    rescanWhich = ConstantUtils.LIST_TYPE.END;
                    btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                    setProgressVisibility(false);
                    myReceiveListener.setStartAutoDetect(true);
                    int i = lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size() > 0 ? ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal() :
                            (lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() > 0 ? ConstantUtils.LIST_TYPE.ERROR.ordinal() : 0);
                    if (i > 0) {
                        pagerList.setCurrentItem(i, true);
                    }
                    break;
                case DETECT_RESCAN:
                    BaseApplication.acquireWakeLock(DetonateStep2Activity.this);
                    allListTab.setCheckedHint(true);
                    resetTabTitle(false);
                    adapterList.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                    pDialog.show();
                    pDialog.setMax(lists.get(rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND ? ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal() : ConstantUtils.LIST_TYPE.END.ordinal()).size());
                    pDialog.setProgress(0);
                    pDialog.setSecondaryProgress(0);
                    pDialog.setOnCancelListener(dialogInterface -> finish());
                    pDialog.setMessage(R.string.progress_detect);
                    btnCharge.setEnabled(false);
                    btnRescan.setEnabled(false);
                    setProgressVisibility(true);
                    myReceiveListener.setStartAutoDetect(false);
                    listIndex = 0;
                    flowStep = rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND ? STEP_READ_FIELD : STEP_RELEASE_1;
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
                                serialPortUtil.sendCmd(listIndex < checkList.size() ? checkList.get(listIndex).getAddress() : "", SerialCommand.CODE_SCAN_UID, ConstantUtils.UID_LEN);
                                myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_SCAN_UID_TIMEOUT);
                                return false;
                            } else {
                                listIndex = 0;
                                int amount = 0;
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()))
                                    if (!b.isDownloaded())
                                        amount++;
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
                                    boolean notFound = true;
                                    for (DetonatorInfoBean b1 : lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal())) {
                                        if (b.getAddress().endsWith(b1.getAddress())) {
                                            notFound = false;
                                            break;
                                        }
                                    }
                                    if (notFound)
                                        lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).add(b);
                                }
                                myHandler.sendEmptyMessage(DETECT_SUCCESS);
                                if (0 == amount && lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() == 0) {
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                    myHandler.sendEmptyMessage(DETECT_FINISH);
                                    return false;
                                } else {
                                    pDialog.setProgress(0);
                                    if (lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() > 0) {
                                        pDialog.setMax(lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size());
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
                            if (listIndex >= lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size()) {
                                listIndex = 0;
                                int amount = 0;
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()))
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
                            serialPortUtil.sendCmd(lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            return false;
                        case STEP_WRITE_FIELD:
                            if (listIndex >= lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size()) {
                                if (scanAll && lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size() > 0) {
                                    scanAll = false;
                                    rescanWhich = ConstantUtils.LIST_TYPE.NOT_FOUND;
                                    myHandler.sendEmptyMessage(DETECT_RESCAN);
                                } else {
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                    myHandler.sendEmptyMessage(DETECT_FINISH);
                                }
                                return false;
                            }
                            DetonatorInfoBean bean = lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex);
                            serialPortUtil.sendCmd(bean.getAddress(), SerialCommand.CODE_WRITE_FIELD, checkIndex(), bean.getDelayTime(), bean.getHole());
                            myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            return false;
                        case STEP_READ_FIELD:
                            if (listIndex >= lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size()) {
                                lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).clear();
                                countScanZero = ConstantUtils.SCAN_ZERO_COUNT;
                                flowStep = STEP_SCAN;
                                myHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                            } else {
                                serialPortUtil.sendCmd(lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_FIELD, ConstantUtils.UID_LEN, 0, 0, 0);
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
            if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                closeAllHandler();
                if (serialPortUtil != null) {
                    serialPortUtil.closeSerialPort();
                    serialPortUtil = null;
                }
                runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_warning)
                        .setMessage(getString(R.string.dialog_short_circuit))
                        .setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                            nextStep = true;
                            finish();
                        })
                        .show()));
                myApp.playSoundVibrate(soundPool, soundAlert);
            } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                if (rescanWhich == ConstantUtils.LIST_TYPE.END) {
                    myHandler.sendEmptyMessage(DETECT_FINISH);
                } else if (rescanWhich == ConstantUtils.LIST_TYPE.ALL) {
                    myHandler.sendEmptyMessage(DETECT_RESCAN);
                }
            } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                closeAllHandler();
                if (serialPortUtil != null) {
                    serialPortUtil.closeSerialPort();
                    serialPortUtil = null;
                }
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
                                    bean = new DetonatorInfoBean(lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).get(listIndex).getAddress(),
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 4]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 5]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 6]),//Delay
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 2]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 3]),//Number
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 7]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 8]),//Hole
                                            Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]), false);//Status
                                    lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).remove(listIndex);
                                } else {
                                    if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                        received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                                    bean = new DetonatorInfoBean(new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9)),
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 11]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 12]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 13]),//Delay
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 10]),//Number
                                            (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 14]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 15]),//Hole
                                            Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 16]), false);//Status
                                    listIndex++;
                                }
                                pDialog.incrementProgressBy(1);
                                pDialog.setSecondaryProgress(45 * pDialog.getProgress() / (pDialog.getMax() + ConstantUtils.SCAN_ZERO_COUNT));
                                countScanZero = 0;
                                if (flowStep == STEP_READ_FIELD || Pattern.matches(ConstantUtils.UID_PATTERN, bean.getAddress())) {
                                    boolean notFound = true;
                                    for (int i = 1; i <= lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size(); i++) {
                                        DetonatorInfoBean b1 = lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(i - 1);
                                        if (b1.getAddress().endsWith(bean.getAddress())) {
                                            if (bean.getDelayTime() == b1.getDelayTime() && i == bean.getRow() && b1.getHole() == bean.getHole()) {
                                                b1.setDownloaded(true);
                                                for (DetonatorInfoBean b2 : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                                                    if (b2.getAddress().endsWith(bean.getAddress())) {
                                                        b2.setDownloaded(true);
                                                        break;
                                                    }
                                            }
                                            for (DetonatorInfoBean b2 : lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal())) {
                                                if (b2.getAddress().equals(b1.getAddress())) {
                                                    myApp.myToast(DetonateStep2Activity.this, String.format(getString(R.string.message_detect_multiple), b1.getAddress()));
                                                    notFound = false;
                                                    break;
                                                }
                                            }
                                            if (notFound) {
                                                if (lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size() == 0)
                                                    lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).add(b1);
                                                else {
                                                    boolean inserted = false;
                                                    for (int j = 0; j < lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size(); j++)
                                                        if (lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(j).getAddress().equals(b1.getAddress())) {
                                                            for (int k = 0; k < lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size(); k++) {
                                                                for (int l = 0; l < lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size(); l++) {
                                                                    if (lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(k).getAddress().equals(
                                                                            lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(l).getAddress())) {
                                                                        if (l > j) {
                                                                            lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).add(k, b1);
                                                                            inserted = true;
                                                                        }
                                                                        break;
                                                                    }
                                                                }
                                                                if (inserted)
                                                                    break;
                                                            }
                                                            break;
                                                        }
                                                    if (!inserted)
                                                        lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).add(b1);
                                                }
                                                myHandler.sendEmptyMessage(DETECT_SUCCESS);
                                            }
                                            notFound = false;
                                            break;
                                        }
                                    }
                                    if (notFound)
                                        lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).add(bean);
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                            return;
                        case STEP_READ_SHELL:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(45 + 10 * pDialog.getProgress() / pDialog.getMax());
                            if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                            String address = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                            if (Pattern.matches(ConstantUtils.SHELL_PATTERN, address))
                                lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).get(listIndex).setAddress(address);
                            listIndex++;
                            break;
                        case STEP_WRITE_FIELD:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(55 + 45 * pDialog.getProgress() / pDialog.getMax());
                            lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex).setDownloaded(true);
                            for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                                if (bean.getAddress().equals(lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex).getAddress()))
                                    bean.setDownloaded(true);
                            nextDownloadIndex();
                            break;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                } else {
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
                            if (listIndex < lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size())
                                lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).remove(listIndex);
                            break;
                        case STEP_WRITE_FIELD:
                            pDialog.incrementProgressBy(1);
                            pDialog.setSecondaryProgress(55 + 45 * pDialog.getProgress() / pDialog.getMax());
                            String address;
                            address = lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex).getAddress();
                            lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex).setDownloaded(true);
                            for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                                if (bean.getAddress().equals(address))
                                    bean.setDownloaded(true);
//                            lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).add(lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex));
//                            lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).remove(listIndex);
//                            listIndex++;
                            nextDownloadIndex();
                            break;
                        case STEP_READ_FIELD:
                            listIndex++;
                            break;
                    }
                    myHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.COMMAND_DELAY_TIME);
                }
            }
        }
    };

    private void nextDownloadIndex() {
        while (listIndex < lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size() && lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex).isDownloaded())
            listIndex++;
        myHandler.sendEmptyMessage(DETECT_SUCCESS);
    }

    private int checkIndex() {
        int result = 1;
        for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
            if (b.getAddress().equals(lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(listIndex).getAddress()))
                return result;
            result++;
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonate_step2);

        btnCharge = findViewById(R.id.btn_charge);
        if (getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false)) {
            setTitle(R.string.check_detonator, R.string.subtitle_unite);
            if (btnCharge.getKeyCode() != -1)
                btnCharge.setText(String.format(Locale.CHINA, "%d.%s", btnCharge.getKeyCode(), getString(R.string.button_enter_unite)));
            else
                btnCharge.setText(R.string.button_enter_unite);
        } else
            setTitle(R.string.check_detonator);
        setProgressVisibility(true);
        myApp = (BaseApplication) getApplication();

        lists = new ArrayList<>();
        fileList = FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1];
        rescanWhich = ConstantUtils.LIST_TYPE.END;
        try {
            for (int i = 0; i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
                lists.add(new ArrayList<>());
                if (new File(fileList[i]).exists()) {
                    myApp.readFromFile(fileList[i], lists.get(i), DetonatorInfoBean.class);
                }
            }
            if (lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).size() <= 0) {
                myApp.myToast(DetonateStep2Activity.this, R.string.message_list_not_found);
                nextStep = true;
                finish();
                return;
            } else {
                for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                    bean.setDownloaded(false);
            }
            if (lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size() > 0) {
                for (DetonatorInfoBean b1 : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
                    for (DetonatorInfoBean b2 : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal())) {
                        if (b1.getAddress().equals(b2.getAddress())) {
                            b2.setSelected(true);
                        }
                    }
                }
            } else {
                lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).addAll(lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()));
                for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal())) {
                    b.setSelected(true);
                }
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }

        initPager();
        btnCharge.setEnabled(false);
        btnRescan = findViewById(R.id.btn_rescan);
        btnRescan.requestFocus();
        btnRescan.setEnabled(false);
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(DetonateStep2Activity.this, bufferRunnable);
            if (lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).size() > 0) {
                listIndex = countIndex();
            }
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            btnCharge.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));
            btnRescan.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initSound();
    }

    private int countIndex() {
        if (lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size() > 0 || lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size() > 0 || lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() > 0) {
            for (int i = lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).size() - 1; i >= 0; i--)
                for (int j = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; j < ConstantUtils.LIST_TYPE.END.ordinal(); j++) {
                    if (lists.get(j).size() > 0 &&
                            lists.get(j).get(lists.get(j).size() - 1).getAddress().equals(lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).get(i).getAddress())) {
                        return i + 1;
                    }
                }
        }
        return 0;
    }

    private void resetTabTitle(boolean init) {
        for (int i = 0; i < tabTitle.size(); i++) {
            TextView tv = (TextView) LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_tab_view, tabList, false);
            String text = tabTitle.get(ConstantUtils.LIST_TYPE.values()[i]) + "(" + lists.get(i == 0 ? ConstantUtils.LIST_TYPE.END.ordinal() : i).size() +
                    (i == 0 ? "/" + lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).size() : "") + ")";
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
        for (int i = 0; i < ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
            tabTitle.put(ConstantUtils.LIST_TYPE.values()[i], getString(title[i]));
        }
        for (int i = 0; i < tabTitle.size(); i++) {
            tabList.addTab(tabList.newTab());

            DetonatorListAdapter adapter = new DetonatorListAdapter(this, lists.get(i));
            adapter.setCanSelect(0 == i);
            adapter.setTunnel(myApp.isTunnel());
            TabFragment tabFragment = new TabFragment(adapter);
            if (0 == i) {
                allListTab = tabFragment;
                checkAllListHint();
            }
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
                if (rescanWhich == ConstantUtils.LIST_TYPE.END) {
                    btnRescan.setEnabled(tab.getPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() ||
                            (tab.getPosition() != ConstantUtils.LIST_TYPE.ERROR.ordinal() && lists.get(tab.getPosition()).size() > 0));
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
                        BaseApplication.writeFile(getString(R.string.dialog_delay_out_of_range));
                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(R.string.dialog_delay_out_of_range)
                                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> startDetect())
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show());
                    } else if (max < 0) {
                        BaseApplication.writeFile(getString(R.string.dialog_delay_out_of_range_2));
                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(R.string.dialog_delay_out_of_range_2)
                                .setPositiveButton(R.string.btn_confirm, null)
                                .show());
                    } else if (btnCharge.isEnabled()) {
                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_detect_again)
                                .setMessage(R.string.dialog_detect_again)
                                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> startDetect())
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show());
                    } else {
                        startDetect();
                    }
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (btnCharge.isEnabled()) {
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
                        new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_edit_charge_voltage)
                                .setView(view)
                                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                                    try {
                                        float v = Float.parseFloat(etDelay.getText().toString());
                                        if (v >= 18 && v <= 22) {
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
                                .show();

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
                    if (tab != null)
                        tab.select();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                hiddenKeyCount = 0;
                if (tabList.getSelectedTabPosition() < tabList.getTabCount() - 1) {
                    TabLayout.Tab tab = tabList.getTabAt(tabList.getSelectedTabPosition() + 1);
                    if (tab != null)
                        tab.select();                    
                }
                break;
            default:
                hiddenKeyCount = 0;
                break;
        }
    }

    private void enterCharge() {
        new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                .setView(R.layout.layout_enter_charge_dialog)
                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                    closeAllHandler();
                    Intent intent = new Intent();
                    intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
                    intent.setClass(DetonateStep2Activity.this, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false) ? UniteExplodeActivity.class : DetonateStep3Activity.class);
                    saveList();
                    startActivity(intent);
                    nextStep = true;
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private int maxDelay() {
        int result = 0;
        for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
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
        scanAll = tabList.getSelectedTabPosition() != 2;
        if (scanAll) {
            checkList = new ArrayList<>();
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i < ConstantUtils.LIST_TYPE.END.ordinal(); i++)
                lists.get(i).clear();
            for (DetonatorInfoBean d : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                d.setDownloaded(false);
            for (DetonatorInfoBean d : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
                d.setDownloaded(false);
                boolean notAdd = true;
                for (int i = 0; i < checkList.size(); i++) {
                    if (d.getAddress().compareTo(checkList.get(i).getAddress()) > 0) {
                        checkList.add(i, d);
                        notAdd = false;
                        break;
                    }
                }
                if (notAdd)
                    checkList.add(d);
            }
            rescanWhich = ConstantUtils.LIST_TYPE.ALL;
        } else
            rescanWhich = ConstantUtils.LIST_TYPE.NOT_FOUND;
        myReceiveListener.setStartAutoDetect(false);
        myHandler.sendEmptyMessage(DETECT_RESCAN);
    }

    private void checkAllListHint() {
        allListTab.setCheckedHint(false);
        for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i < ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
            if (lists.get(i).size() > 0) {
                allListTab.setCheckedHint(true);
                break;
            }
        }
    }

    public void onChangedSelectedList() {
        if (!btnRescan.isEnabled()) {
            myHandler.sendEmptyMessage(DETECT_FINISH);
        }
        boolean enabled = false;
        for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++)
            lists.get(i).clear();
        for (DetonatorInfoBean bean : adapterList.get(0).getList()) {
            if (bean.isSelected()) {
                enabled = true;
                lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).add(bean);
            }
        }
        allListTab.setCheckedHint(false);
        final boolean finalEnabled = enabled;
        runOnUiThread(() -> {
            resetTabTitle(false);
            btnRescan.setEnabled(finalEnabled);
            btnCharge.setEnabled(false);
        });
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
                    .show());
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
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i < ConstantUtils.LIST_TYPE.END.ordinal(); i++)
                lists.get(i).clear();
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal(); i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
                if (lists.get(i).size() > 0) {
                    myApp.writeToFile(fileList[i], lists.get(i));
                } else {
                    File file = new File(fileList[i]);
                    if (file.exists() && !file.delete())
                        myApp.myToast(DetonateStep2Activity.this, String.format(Locale.CHINA, getString(R.string.message_delete_file_fail), file.getName()));
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
