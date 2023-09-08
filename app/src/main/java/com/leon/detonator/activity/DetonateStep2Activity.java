package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.CustomProgressDialog;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class DetonateStep2Activity extends BaseActivity {
    private List<List<DetonatorBean>> lists;
    private SerialPortUtil serialPortUtil;
    private MyButton btnCharge;
    private MyButton btnRescan;
    private ViewPager viewPager;
    private CustomProgressDialog pDialog;
    private BaseApplication myApp;
    private ConstantUtils.ListType rescanWhich;
    private DataReceiveListener myReceiveListener;
    private SoundPool soundPool;
    private String[] tabTitle;
    private final int DETECT_RESCAN = 1;
    private boolean nextStep;
    private int listIndex;
    private int hiddenKeyCount;
    private int hiddenType;
    private int flowStep;
    private int countScanZero;
    private int soundSuccess;
    private int soundAlert;

    private final Handler myHandler = new Handler(new Handler.Callback() {
        private final int DETECT_SUCCESS = 2;

        private void nextDownloadIndex() {
            while (listIndex < lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size() && lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex).isDownloaded())
                listIndex++;
            myHandler.sendEmptyMessage(DETECT_SUCCESS);
        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            final int DETECT_FINISH = 3;
            final int DETECT_NEXT_STEP = 6;
            final int DETECT_SEND_COMMAND = 7;
            final int STEP_SET_PARAM_LEVEL = 1;
            final int STEP_RELEASE_1 = 2;
            final int STEP_RELEASE_2 = 3;
            final int STEP_RELEASE_3 = 4;
            final int STEP_RESET_1 = 5;
            final int STEP_RESET_2 = 6;
            final int STEP_RESET_3 = 7;
            final int STEP_INITIAL = 8;
            final int STEP_CHECK_CONFIG = 9;
            final int STEP_CLEAR_STATUS = 10;
            final int STEP_SCAN = 11;
            final int STEP_READ_SHELL = 12;
            final int STEP_WRITE_FIELD = 13;
            final int STEP_READ_FIELD = 14;
            final int STEP_LOCK_1 = 15;
            final int STEP_LOCK_2 = 16;
            final int STEP_LOCK_3 = 17;
            final int STEP_CHECK_ONLINE = 18;
            switch (msg.what) {
                case DETECT_SUCCESS:
                    if (null != viewPager.getAdapter())
                        viewPager.getAdapter().notifyDataSetChanged();
                    break;
                case DETECT_FINISH:
                    BaseApplication.releaseWakeLock(DetonateStep2Activity.this);
                    myApp.playSoundVibrate(soundPool, soundSuccess);
                    btnCharge.setEnabled(lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() == 0 && lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size() == 0
                            && lists.get(ConstantUtils.ListType.ALL.ordinal()).size() == lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size());
                    if (pDialog != null) {
                        if (pDialog.isShowing())
                            pDialog.dismiss();
                        pDialog = null;
                    }
                    rescanWhich = ConstantUtils.ListType.NONE;
                    btnRescan.setEnabled(viewPager.getCurrentItem() == ConstantUtils.ListType.DETECTED.ordinal() || lists.get(viewPager.getCurrentItem()).size() > 0);
                    setProgressVisibility(false);
                    myReceiveListener.setStartAutoDetect(true);
                    int item = lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size() > 0 ? ConstantUtils.ListType.NOT_FOUND.ordinal() :
                            (lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() > 0 ? ConstantUtils.ListType.ERROR.ordinal() : 0);
                    if (item > 0)
                        viewPager.setCurrentItem(item, true);
                    break;
                case DETECT_RESCAN:
                    BaseApplication.acquireWakeLock(DetonateStep2Activity.this);
                    if (null != viewPager.getAdapter())
                        viewPager.getAdapter().notifyDataSetChanged();
                    pDialog.show();
                    pDialog.setMax(lists.get(rescanWhich == ConstantUtils.ListType.NOT_FOUND ? ConstantUtils.ListType.NOT_FOUND.ordinal() : ConstantUtils.ListType.ALL.ordinal()).size());
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
                    msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
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
                    msg.getTarget().removeMessages(DETECT_SEND_COMMAND);
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
                                msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_SCAN_UID_TIMEOUT);
                                return false;
                            } else {
                                listIndex = 0;
                                int amount = 0;
                                for (DetonatorBean b : lists.get(ConstantUtils.ListType.DETECTED.ordinal()))
                                    if (!b.isDownloaded())
                                        amount++;
                                for (DetonatorBean b : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                                    if (!lists.get(ConstantUtils.ListType.DETECTED.ordinal()).contains(b)) {
                                        lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).add(b);
                                        BaseApplication.writeFile("不在线：" + b.getAddress());
                                    }
                                msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                if (0 == amount && lists.get(ConstantUtils.ListType.ERROR.ordinal()).size() == 0) {
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                    msg.getTarget().sendEmptyMessage(DETECT_FINISH);
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
                                    msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                                }
                            }
                            break;
                        case STEP_READ_SHELL:
                            if (listIndex >= lists.get(ConstantUtils.ListType.ERROR.ordinal()).size()) {
                                listIndex = 0;
                                int amount = 0;
                                for (DetonatorBean b : lists.get(ConstantUtils.ListType.DETECTED.ordinal()))
                                    if (!b.isDownloaded())
                                        amount++;
                                pDialog.setProgress(0);
                                pDialog.setMax(amount);
                                pDialog.setMessage(R.string.progress_set_delay);
                                nextDownloadIndex();
                                flowStep = STEP_WRITE_FIELD;
                                msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                                return false;
                            }
                            serialPortUtil.sendCmd(lists.get(ConstantUtils.ListType.ERROR.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                            msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            return false;
                        case STEP_WRITE_FIELD:
                            if (listIndex >= lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size()) {
                                if (rescanWhich != ConstantUtils.ListType.NOT_FOUND && lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size() > 0) {
                                    rescanWhich = ConstantUtils.ListType.NOT_FOUND;
                                    msg.getTarget().sendEmptyMessage(DETECT_RESCAN);
                                } else {
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                    msg.getTarget().sendEmptyMessage(DETECT_FINISH);
                                }
                                return false;
                            }
                            DetonatorBean bean = lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex);
                            serialPortUtil.sendCmd(bean.getAddress(), SerialCommand.CODE_WRITE_FIELD,
                                    lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex)) + 1
                                    , bean.getDelayTime(), bean.getHole());
                            msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            return false;
                        case STEP_READ_FIELD:
                            if (listIndex >= lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).size()) {
                                lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).clear();
                                countScanZero = ConstantUtils.SCAN_ZERO_COUNT;
                                flowStep = STEP_SCAN;
                                msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                            } else {
                                serialPortUtil.sendCmd(lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_FIELD, ConstantUtils.UID_LEN, 0, 0, 0);
                                msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                            }
                            return false;
                        case STEP_CHECK_ONLINE:
                            serialPortUtil.sendCmd("", SerialCommand.CODE_CHECK_ONLINE, lists.get(ConstantUtils.ListType.ALL.ordinal()).size());
                            msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, lists.get(ConstantUtils.ListType.ALL.ordinal()).size() * 2L + ConstantUtils.SCAN_DELAY_TIME);
                            return false;
                    }
                    msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                    break;
                case DataReceiveListener.HANDLER_RECEIVED_DATA:
                    byte[] received = (byte[]) msg.obj;
                    if (received != null && received.length > 0) {
                        if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                            nextStep = true;
                            myApp.shortCircuit(DetonateStep2Activity.this, msg.getTarget());
                        } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                            if (rescanWhich == ConstantUtils.ListType.NONE)
                                msg.getTarget().sendEmptyMessage(DETECT_FINISH);
                            else if (rescanWhich == ConstantUtils.ListType.ALL)
                                msg.getTarget().sendEmptyMessage(DETECT_RESCAN);
                        } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                            myApp.myToast(DetonateStep2Activity.this, R.string.message_open_module_fail);
                            nextStep = true;
                            if (myReceiveListener != null)
                                myReceiveListener.closeAllHandler();
                            finish();
                        } else if (received.length > 5) {
                            msg.getTarget().removeMessages(DETECT_SEND_COMMAND);
                            msg.getTarget().removeMessages(DETECT_NEXT_STEP);
                            if (0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                                switch (flowStep) {
                                    case STEP_LOCK_1:
                                    case STEP_LOCK_2:
                                    case STEP_LOCK_3:
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.LOCK_DELAY_TIME);
                                        return false;
                                    case STEP_RELEASE_1:
                                    case STEP_RELEASE_2:
                                    case STEP_RELEASE_3:
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.RELEASE_DELAY_TIME);
                                        return false;
                                    case STEP_RESET_1:
                                    case STEP_RESET_2:
                                    case STEP_RESET_3:
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.RESET_DELAY_TIME);
                                        return false;
                                    case STEP_READ_FIELD:
                                    case STEP_SCAN:
                                        try {
                                            DetonatorBean bean;
                                            if (flowStep == STEP_READ_FIELD) {
                                                bean = new DetonatorBean(-1, lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).get(listIndex).getAddress(),
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
                                                bean = new DetonatorBean(-1, isNull ? ConstantUtils.NULL_ID : new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 9)),
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
                                                int i = lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(bean);
                                                if (i >= 0) {
                                                    DetonatorBean b1 = lists.get(ConstantUtils.ListType.ALL.ordinal()).get(i);
                                                    if (bean.getDelayTime() == b1.getDelayTime() && i + 1 == bean.getRow() && b1.getHole() == bean.getHole()) {
                                                        b1.setDownloaded(true);
                                                        DbUtil.updateDetonator(b1);
                                                    }
                                                    if (lists.get(ConstantUtils.ListType.DETECTED.ordinal()).contains(b1))
                                                        myApp.myToast(DetonateStep2Activity.this, String.format(getString(R.string.message_detect_multiple), b1.getAddress()));
                                                    else {
                                                        if (lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size() == 0)
                                                            lists.get(ConstantUtils.ListType.DETECTED.ordinal()).add(b1);
                                                        else {
                                                            int k = 0;
                                                            for (; k < lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size(); k++)
                                                                if (lists.get(ConstantUtils.ListType.ALL.ordinal()).indexOf(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(k)) > i) {
                                                                    lists.get(ConstantUtils.ListType.DETECTED.ordinal()).add(k, b1);
                                                                    break;
                                                                }
                                                            if (k >= lists.get(ConstantUtils.ListType.DETECTED.ordinal()).size())
                                                                lists.get(ConstantUtils.ListType.DETECTED.ordinal()).add(b1);
                                                        }
                                                        msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                                    }
                                                } else
                                                    lists.get(ConstantUtils.ListType.ERROR.ordinal()).add(bean);
                                            }
                                        } catch (Exception e) {
                                            BaseApplication.writeErrorLog(e);
                                        }
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                                        return false;
                                    case STEP_READ_SHELL:
                                        pDialog.incrementProgressBy(1);
                                        pDialog.setSecondaryProgress(45 + 10 * pDialog.getProgress() / pDialog.getMax());
                                        if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                            received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                                        String address = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                                        BaseApplication.writeFile("雷壳码：" + address);
                                        if (Pattern.matches(ConstantUtils.SHELL_PATTERN, address))
                                            lists.get(ConstantUtils.ListType.ERROR.ordinal()).get(listIndex).setAddress(address);
                                        listIndex++;
                                        break;
                                    case STEP_WRITE_FIELD:
                                        pDialog.incrementProgressBy(1);
                                        pDialog.setSecondaryProgress(55 + 45 * pDialog.getProgress() / pDialog.getMax());
                                        lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex).setDownloaded(true);
                                        DbUtil.updateDetonator(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex));
                                        nextDownloadIndex();
                                        break;
                                    case STEP_CHECK_ONLINE:
                                        for (int i = 0; i < lists.get(ConstantUtils.ListType.ALL.ordinal()).size(); i++)
                                            if ((received[SerialCommand.CODE_CHAR_AT + 2 + i / 8] & (1 << (7 - i % 8))) == 0) {
                                                if (!lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).contains(lists.get(ConstantUtils.ListType.ALL.ordinal()).get(i)))
                                                    lists.get(ConstantUtils.ListType.NOT_FOUND.ordinal()).add(lists.get(ConstantUtils.ListType.ALL.ordinal()).get(i));
                                                int j = lists.get(ConstantUtils.ListType.DETECTED.ordinal()).indexOf(lists.get(ConstantUtils.ListType.ALL.ordinal()).get(i));
                                                if (j >= 0)
                                                    lists.get(ConstantUtils.ListType.DETECTED.ordinal()).remove(j);
                                            }
                                        msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                        myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                        msg.getTarget().sendEmptyMessage(DETECT_FINISH);
                                        return false;
                                }
                                msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                            } else {
                                if (flowStep != STEP_SCAN && flowStep != STEP_WRITE_FIELD)
                                    BaseApplication.writeFile("返回错误！");
                                switch (flowStep) {
                                    case STEP_LOCK_1:
                                    case STEP_LOCK_2:
                                    case STEP_LOCK_3:
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.LOCK_DELAY_TIME);
                                        return false;
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
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.SCAN_DELAY_TIME);
                                        return false;
                                    case STEP_CHECK_CONFIG:
                                        myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_error);
                                        msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.COMMAND_DELAY_TIME);
                                        return false;
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
                                        DbUtil.updateDetonator(lists.get(ConstantUtils.ListType.DETECTED.ordinal()).get(listIndex));
                                        nextDownloadIndex();
                                        break;
                                    case STEP_READ_FIELD:
                                        listIndex++;
                                        pDialog.incrementProgressBy(1);
                                        pDialog.setSecondaryProgress(45 * pDialog.getProgress() / (pDialog.getMax() + ConstantUtils.SCAN_ZERO_COUNT));
                                        break;
                                }
                                msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.COMMAND_DELAY_TIME);
                            }
                        }
                    }
                    break;
                default:
                    myApp.myToast(DetonateStep2Activity.this, (String) msg.obj);
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonate_step2);

        btnCharge = findViewById(R.id.btn_charge);
        if (getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false)) {
            setTitle(R.string.title_check_detonator, R.string.subtitle_unite);
            btnCharge.setTextId(R.string.button_enter_unite);
        } else
            setTitle(R.string.title_check_detonator);
        setProgressVisibility(true);
        myApp = (BaseApplication) getApplication();

        lists = new ArrayList<>();
        rescanWhich = ConstantUtils.ListType.NONE;
        try {
            lists.add(getIntent().getParcelableArrayListExtra(KeyUtils.KEY_LIST));
            for (int i = 1; i < ConstantUtils.ListType.NONE.ordinal(); i++)
                lists.add(new ArrayList<>());
            if (lists.get(ConstantUtils.ListType.ALL.ordinal()).size() <= 0) {
                myApp.myToast(DetonateStep2Activity.this, R.string.message_list_not_found);
                nextStep = true;
                finish();
                return;
            } else
                for (DetonatorBean bean : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                    bean.setDownloaded(false);
            initPager();
            btnCharge.setEnabled(false);
            btnRescan = findViewById(R.id.btn_rescan);
            btnRescan.requestFocus();
            btnRescan.setEnabled(false);
            serialPortUtil = SerialPortUtil.getInstance(this);
            myReceiveListener = DataReceiveListener.getInstance(DetonateStep2Activity.this, myHandler);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            myReceiveListener.setDetonatorAmount(lists.get(ConstantUtils.ListType.ALL.ordinal()).size());
            btnCharge.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_3));
            btnRescan.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initSound();
    }

    private void initPager() {
        viewPager = findViewById(R.id.view_pager);
        tabTitle = new String[]{getString(R.string.tab_title_all_list), getString(R.string.tab_title_online), getString(R.string.tab_title_offline), getString(R.string.tab_title_delay_error)};
        ((PagerTabStrip) findViewById(R.id.pager_tab_strip)).setTabIndicatorColor(getColor(BaseApplication.settings.isTunnel() ? R.color.colorActionbarBackground : R.color.colorOpenAirBackground));
        viewPager.setAdapter(new ListPagerAdapter());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                btnRescan.setEnabled(position == ConstantUtils.ListType.DETECTED.ordinal() || lists.get(position).size() > 0);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
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
                    if (max > ConstantUtils.MAX_DELAY_TIME)
                        myApp.myToast(DetonateStep2Activity.this, String.format(Locale.getDefault(), getString(R.string.message_delay_time_out_of_upper_range), ConstantUtils.MAX_DELAY_TIME));
                    else if (max < 0)
                        myApp.myToast(DetonateStep2Activity.this, R.string.message_delay_time_out_of_lower_range);
                    else if (btnCharge.isEnabled()) {
                        BaseApplication.writeFile(getString(R.string.dialog_title_detect_again));
                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_detect_again)
                                .setMessage(R.string.dialog_detect_again)
                                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> startDetect())
                                .setNegativeButton(R.string.button_cancel, null)
                                .show());
                    } else
                        startDetect();
                }
                break;
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_8:
                viewPager.requestFocus();
                break;
            case KeyEvent.KEYCODE_3:
                if (btnCharge.isEnabled()) {
                    BaseApplication.writeFile(getString(R.string.button_charge));
                    enterCharge();
                }
                break;
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_9:
                if (hiddenKeyCount == 3) {
                    hiddenType = which - KeyEvent.KEYCODE_1 + 1;
                    hiddenKeyCount++;
                } else
                    hiddenKeyCount = 0;
                break;
            case KeyEvent.KEYCODE_F1:
                if (hiddenKeyCount == 5) {
                    hiddenKeyCount = 0;
                    if (hiddenType == 5)
                        enterCharge();
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
            case KeyEvent.KEYCODE_4:
                hiddenKeyCount = 0;
                if (viewPager.getCurrentItem() > 0)
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
                else
                    viewPager.setCurrentItem(lists.size() - 1, true);
                break;
            case KeyEvent.KEYCODE_6:
                hiddenKeyCount = 0;
                if (viewPager.getCurrentItem() < lists.size() - 1)
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                else
                    viewPager.setCurrentItem(0, true);
                break;
            default:
                hiddenKeyCount = 0;
                break;
        }
    }

    private void enterCharge() {
        if (!serialPortUtil.isSafeSwitchOpened())
            myApp.myToast(DetonateStep2Activity.this, R.string.message_turn_on_safe_switch);
        else
            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                    .setView(R.layout.layout_dialog_enter_charge)
                    .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                        if (!serialPortUtil.isSafeSwitchOpened())
                            myApp.myToast(DetonateStep2Activity.this, R.string.message_turn_on_safe_switch);
                        else {
                            Intent intent = new Intent();
                            intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
                            intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                            intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                            intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
                            intent.putParcelableArrayListExtra(KeyUtils.KEY_LIST, (ArrayList<DetonatorBean>) lists.get(ConstantUtils.ListType.ALL.ordinal()));
                            intent.setClass(DetonateStep2Activity.this, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false) ? UniteExplodeActivity.class : DetonateStep3Activity.class);
                            startActivity(intent);
                            nextStep = true;
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show());
    }

    private int maxDelay() {
        int result = 0;
        for (DetonatorBean bean : lists.get(ConstantUtils.ListType.ALL.ordinal())) {
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
        if (viewPager.getCurrentItem() != 2) {
            BaseApplication.writeFile(getString(R.string.button_start_detect) + ", " + getString(R.string.tab_title_online) + ":" + lists.get(ConstantUtils.ListType.ALL.ordinal()).size());
            for (int i = ConstantUtils.ListType.ALL.ordinal() + 1; i < ConstantUtils.ListType.NONE.ordinal(); i++)
                lists.get(i).clear();
            for (DetonatorBean d : lists.get(ConstantUtils.ListType.ALL.ordinal()))
                d.setDownloaded(false);
            DbUtil.clearDetonatorDownloadFlag(lists.get(ConstantUtils.ListType.ALL.ordinal()).get(0).getSchemeId());
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
            soundAlert = soundPool.load(this, R.raw.alert, 1);
        }
    }

    @Override
    public void finish() {
        if (!nextStep) {
            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_detect)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                        if (myReceiveListener != null)
                            myReceiveListener.closeAllHandler();
                        if (pDialog != null && pDialog.isShowing())
                            pDialog.dismiss();
                        DetonateStep2Activity.super.finish();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {
                        if (pDialog != null && pDialog.getMax() != pDialog.getProgress())
                            pDialog.show();
                    })
                    .show());
        } else
            super.finish();
    }

    @Override
    public void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
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

    private class ListPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return lists.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            // 解决 notifyDataSetChanged() 页面不刷新问题的方法
            return POSITION_NONE;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.layout_detonator_listview, container, false);
            ListView listView = view.findViewById(R.id.list);
            listView.setAdapter(new DetonatorListAdapter(container.getContext(), lists.get(position)));
            container.addView(listView);
            listView.setOnKeyListener((view1, i, keyEvent) -> {
                if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_2) {
                        listView.requestFocus();
                        if (listView.getSelectedItemPosition() > 0)
                            listView.setSelection(listView.getSelectedItemPosition() - 1);
                        else
                            listView.setSelection(lists.get(position).size() - 1);
                    } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_8) {
                        listView.requestFocus();
                        if (listView.getSelectedItemPosition() < lists.get(position).size() - 1)
                            listView.setSelection(listView.getSelectedItemPosition() + 1);
                        else
                            listView.setSelection(0);
                    }
                return false;
            });
            return listView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return String.format(Locale.getDefault(), "%s(%d)", tabTitle[position], lists.get(position).size());
        }
    }
}
