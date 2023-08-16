package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
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
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.component.TabFragment;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.CustomProgressDialog;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class DetonateStep2Activity extends BaseActivity {
    private DataReceiveListener myReceiveListener;
    private SerialPortUtil serialPortUtil;
    private List<List<DetonatorBean>> lists;
    private List<TabFragment> fragments;
    private Map<ListType, String> tabTitle;
    private CustomProgressDialog pDialog;
    private ListType rescanWhich;
    private SoundPool soundPool;
    private ViewPager pagerList;
    private TabLayout tabList;
    private MyButton btnCharge;
    private MyButton btnRescan;
    private final int DETECT_RESCAN = 1;
    private float chargeVoltage;
    private long[] schemeId;
    private int hiddenKeyCount;
    private int countScanZero;
    private int soundSuccess;
    private int hiddenType;
    private int listIndex;
    private int flowStep;
    private boolean nextStep;

    private enum ListType {
        ALL,
        DETECTED,
        NOT_FOUND,
        ERROR,
        NONE
    }

    private final Handler myHandler = new Handler(msg -> {
        final int DETECT_SUCCESS = 2;
        final int DETECT_FINISH = 3;
        final int DETECT_NEXT_STEP = 4;
        final int DETECT_SEND_COMMAND = 5;
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
        switch (msg.what) {
            case DETECT_SUCCESS:
                fragments.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                resetTabTitle(false);
                break;
            case DETECT_FINISH:
                BaseApplication.releaseWakeLock(DetonateStep2Activity.this);
                myApp.playSoundVibrate(soundPool, soundSuccess);
                btnCharge.setEnabled(lists.get(ListType.ERROR.ordinal()).size() == 0 && lists.get(ListType.NOT_FOUND.ordinal()).size() == 0
                        && lists.get(0).size() == lists.get(ListType.DETECTED.ordinal()).size());
                if (pDialog != null) {
                    if (pDialog.isShowing())
                        pDialog.dismiss();
                    pDialog = null;
                }
                rescanWhich = ListType.NONE;
                btnRescan.setEnabled(tabList.getSelectedTabPosition() == ListType.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                setProgressVisibility(false);
                myReceiveListener.setStartAutoDetect(true);
                int i = lists.get(ListType.NOT_FOUND.ordinal()).size() > 0 ? ListType.NOT_FOUND.ordinal() :
                        (lists.get(ListType.ERROR.ordinal()).size() > 0 ? ListType.ERROR.ordinal() : 0);
                if (i > 0) {
                    pagerList.setCurrentItem(i, true);
                }
                break;
            case DETECT_RESCAN:
                BaseApplication.acquireWakeLock(DetonateStep2Activity.this);
                resetTabTitle(false);
                fragments.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                pDialog.show();
                pDialog.setMax(lists.get(rescanWhich == ListType.NOT_FOUND ? ListType.NOT_FOUND.ordinal() : 0).size());
                pDialog.setProgress(0);
                pDialog.setSecondaryProgress(0);
                pDialog.setOnCancelListener(dialogInterface -> finish());
                pDialog.setMessage(R.string.progress_detect);
                btnCharge.setEnabled(false);
                btnRescan.setEnabled(false);
                setProgressVisibility(true);
                myReceiveListener.setStartAutoDetect(false);
                listIndex = 0;
                flowStep = rescanWhich == ListType.NOT_FOUND ? STEP_READ_FIELD : STEP_LOCK_1;
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
                            for (DetonatorBean b : lists.get(ListType.DETECTED.ordinal()))
                                if (!b.isDownloaded())
                                    amount++;
                            for (DetonatorBean b : lists.get(0))
                                if (!lists.get(ListType.DETECTED.ordinal()).contains(b)) {
                                    lists.get(ListType.NOT_FOUND.ordinal()).add(b);
                                    BaseApplication.writeFile("不在线：" + b.getAddress());
                                }
                            msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                            if (0 == amount && lists.get(ListType.ERROR.ordinal()).size() == 0) {
                                myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                msg.getTarget().sendEmptyMessage(DETECT_FINISH);
                                return false;
                            } else {
                                pDialog.setProgress(0);
                                if (lists.get(ListType.ERROR.ordinal()).size() > 0) {
                                    pDialog.setMax(lists.get(ListType.ERROR.ordinal()).size());
                                    pDialog.setMessage(R.string.progress_reading);
                                    flowStep = STEP_READ_SHELL;
                                } else {
                                    pDialog.setMax(amount);
                                    pDialog.setMessage(R.string.progress_set_delay);
                                    while (listIndex < lists.get(ListType.DETECTED.ordinal()).size() && lists.get(ListType.DETECTED.ordinal()).get(listIndex).isDownloaded())
                                        listIndex++;
                                    msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                    flowStep = STEP_WRITE_FIELD;
                                }
                                msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                            }
                        }
                        break;
                    case STEP_READ_SHELL:
                        if (listIndex >= lists.get(ListType.ERROR.ordinal()).size()) {
                            listIndex = 0;
                            int amount = 0;
                            for (DetonatorBean b : lists.get(ListType.DETECTED.ordinal()))
                                if (!b.isDownloaded())
                                    amount++;
                            pDialog.setProgress(0);
                            pDialog.setMax(amount);
                            pDialog.setMessage(R.string.progress_set_delay);
                            while (listIndex < lists.get(ListType.DETECTED.ordinal()).size() && lists.get(ListType.DETECTED.ordinal()).get(listIndex).isDownloaded())
                                listIndex++;
                            msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                            flowStep = STEP_WRITE_FIELD;
                            msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                            return false;
                        }
                        serialPortUtil.sendCmd(lists.get(ListType.ERROR.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_SHELL, ConstantUtils.UID_LEN);
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        return false;
                    case STEP_WRITE_FIELD:
                        if (listIndex >= lists.get(ListType.DETECTED.ordinal()).size()) {
                            if (rescanWhich != ListType.NOT_FOUND && lists.get(ListType.NOT_FOUND.ordinal()).size() > 0) {
                                rescanWhich = ListType.NOT_FOUND;
                                msg.getTarget().sendEmptyMessage(DETECT_RESCAN);
                            } else {
                                myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                msg.getTarget().sendEmptyMessage(DETECT_FINISH);
                            }
                            return false;
                        }
                        DetonatorBean bean = lists.get(ListType.DETECTED.ordinal()).get(listIndex);
                        serialPortUtil.sendCmd(bean.getAddress(), SerialCommand.CODE_WRITE_FIELD,
                                lists.get(0).indexOf(lists.get(ListType.DETECTED.ordinal()).get(listIndex)) + 1
                                , bean.getDelayTime(), bean.getHole());
                        msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        return false;
                    case STEP_READ_FIELD:
                        if (listIndex >= lists.get(ListType.NOT_FOUND.ordinal()).size()) {
                            lists.get(ListType.NOT_FOUND.ordinal()).clear();
                            countScanZero = ConstantUtils.SCAN_ZERO_COUNT;
                            flowStep = STEP_SCAN;
                            msg.getTarget().sendEmptyMessage(DETECT_SEND_COMMAND);
                        } else {
                            serialPortUtil.sendCmd(lists.get(ListType.NOT_FOUND.ordinal()).get(listIndex).getAddress(), SerialCommand.CODE_READ_FIELD, ConstantUtils.UID_LEN, 0, 0, 0);
                            msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                        }
                        return false;
                }
                msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_STATUS_TIMEOUT);
                break;
            case DataReceiveListener.HANDLER_RECEIVED_DATA:
                byte[] received = (byte[]) msg.obj;
                if (received != null && received.length > 0)
                    if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT || received[0] == SerialCommand.ALERT_LARGE_CURRENT) {
                        nextStep = true;
                        myApp.shortCircuit(DetonateStep2Activity.this, msg.getTarget());
                    } else if (received[0] == SerialCommand.INITIAL_FINISHED) {
                        if (rescanWhich == ListType.NONE) {
                            msg.getTarget().sendEmptyMessage(DETECT_FINISH);
                        } else if (rescanWhich == ListType.ALL) {
                            msg.getTarget().sendEmptyMessage(DETECT_RESCAN);
                        }
                    } else if (received[0] == SerialCommand.INITIAL_FAIL) {
                        myApp.myToast(DetonateStep2Activity.this, R.string.message_open_module_fail);
                        nextStep = true;
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
                                            bean = new DetonatorBean(-1, lists.get(ListType.NOT_FOUND.ordinal()).get(listIndex).getAddress(),
                                                    (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 4]) << 16) + (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 5]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 6]),//Delay
                                                    (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 2]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 3]),//Number
                                                    (Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 7]) << 8) + Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 8]),//Hole
                                                    Byte.toUnsignedInt(received[SerialCommand.CODE_CHAR_AT + 9]), false);//Status
                                            lists.get(ListType.NOT_FOUND.ordinal()).remove(listIndex);
                                        } else {
                                            boolean isNull = true;
                                            for (int j = 0; j < 7; j++)
                                                if (received[SerialCommand.CODE_CHAR_AT + 2 + j] != 0) {
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
                                            lists.get(ListType.ERROR.ordinal()).add(bean);
                                        else if (flowStep == STEP_READ_FIELD || Pattern.matches(ConstantUtils.UID_PATTERN, bean.getAddress())) {
                                            int index = lists.get(0).indexOf(bean);
                                            if (index >= 0) {
                                                DetonatorBean b1 = lists.get(0).get(index);
                                                if (bean.getDelayTime() == b1.getDelayTime() && index + 1 == bean.getRow() && b1.getHole() == bean.getHole()) {
                                                    b1.setDownloaded(true);
                                                    DbUtil.updateDetonator(b1);
                                                }
                                                if (lists.get(ListType.DETECTED.ordinal()).contains(b1)) {
                                                    myApp.myToast(DetonateStep2Activity.this, String.format(getString(R.string.message_detect_multiple), b1.getAddress()));
                                                } else {
                                                    if (lists.get(ListType.DETECTED.ordinal()).size() == 0)
                                                        lists.get(ListType.DETECTED.ordinal()).add(b1);
                                                    else {
                                                        int k = 0;
                                                        for (; k < lists.get(ListType.DETECTED.ordinal()).size(); k++)
                                                            if (lists.get(0).indexOf(lists.get(ListType.DETECTED.ordinal()).get(k)) > index) {
                                                                lists.get(ListType.DETECTED.ordinal()).add(k, b1);
                                                                break;
                                                            }
                                                        if (k >= lists.get(ListType.DETECTED.ordinal()).size())
                                                            lists.get(ListType.DETECTED.ordinal()).add(b1);
                                                    }
                                                }
                                            } else
                                                lists.get(ListType.ERROR.ordinal()).add(bean);
                                        }
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                    msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                    msg.getTarget().sendEmptyMessageDelayed(DETECT_NEXT_STEP, ConstantUtils.SCAN_DELAY_TIME);
                                    return false;
                                case STEP_READ_SHELL:
                                    pDialog.incrementProgressBy(1);
                                    pDialog.setSecondaryProgress(45 + 10 * pDialog.getProgress() / pDialog.getMax());
                                    if (received[SerialCommand.CODE_CHAR_AT + 3] < 0x30)
                                        received[SerialCommand.CODE_CHAR_AT + 3] += 0x40;
                                    String address = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                                    BaseApplication.writeFile("雷壳码：" + address);
                                    if (Pattern.matches(ConstantUtils.SHELL_PATTERN, address)) {
                                        lists.get(ListType.ERROR.ordinal()).get(listIndex).setAddress(address);
                                    }
                                    listIndex++;
                                    break;
                                case STEP_WRITE_FIELD:
                                    pDialog.incrementProgressBy(1);
                                    pDialog.setSecondaryProgress(55 + 45 * pDialog.getProgress() / pDialog.getMax());
                                    DetonatorBean bean = lists.get(ListType.DETECTED.ordinal()).get(listIndex);
                                    bean.setDownloaded(true);
                                    DbUtil.updateDetonator(bean);
                                    while (listIndex < lists.get(ListType.DETECTED.ordinal()).size() && lists.get(ListType.DETECTED.ordinal()).get(listIndex).isDownloaded())
                                        listIndex++;
                                    msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                    break;
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
                                    for (int j = SerialCommand.CODE_CHAR_AT + 2; j < SerialCommand.CODE_CHAR_AT + 9; j++) {
                                        if (0 != received[j]) {
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
                                    DetonatorBean bean = lists.get(ListType.DETECTED.ordinal()).get(listIndex);
                                    bean.setDownloaded(true);
                                    DbUtil.updateDetonator(bean);
                                    while (listIndex < lists.get(ListType.DETECTED.ordinal()).size() && lists.get(ListType.DETECTED.ordinal()).get(listIndex).isDownloaded())
                                        listIndex++;
                                    msg.getTarget().sendEmptyMessage(DETECT_SUCCESS);
                                    break;
                                case STEP_READ_FIELD:
                                    pDialog.incrementProgressBy(1);
                                    pDialog.setSecondaryProgress(45 * pDialog.getProgress() / (pDialog.getMax() + ConstantUtils.SCAN_ZERO_COUNT));
                                    listIndex++;
                                    break;
                            }
                            msg.getTarget().sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.COMMAND_DELAY_TIME);
                        }
                    }
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonate_step2);
        btnCharge = findViewById(R.id.btn_charge);
        if (getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false)) {
            setTitle(R.string.check_detonator, R.string.subtitle_unite);
            btnCharge.setTextId(R.string.button_enter_unite);
        } else
            setTitle(R.string.check_detonator);
        setProgressVisibility(true);
        lists = new ArrayList<>();
        rescanWhich = ListType.NONE;
        try {
            schemeId = DbUtil.getSelectedSchemeId();
            if (schemeId.length > 1) {
                lists.add(new ArrayList<>());
                for (long i : schemeId) {
                    List<DetonatorBean> list = DbUtil.getDetonatorList(i);
                    if (list.size() == 0) {
                        myApp.myToast(DetonateStep2Activity.this, String.format(getString(R.string.message_scheme_empty_list), DbUtil.getScheme(i).getName()));
                        nextStep = true;
                        finish();
                        return;
                    } else
                        for (int k = 0; k < lists.get(0).size(); k++) {
                            int j = list.indexOf(lists.get(0).get(k));
                            if (j >= 0) {
                                int m = 0;
                                for (int l = k - 1; l >= 0; l--)
                                    if (lists.get(0).get(l).getSchemeId() != lists.get(0).get(k).getSchemeId()) {
                                        m = l + 1;
                                        break;
                                    }
                                myApp.myToast(DetonateStep2Activity.this, String.format(Locale.getDefault(),
                                        getString(R.string.message_detonator_duplicate), lists.get(0).get(k).getAddress(),
                                        DbUtil.getScheme(lists.get(0).get(k).getSchemeId()).getName(), lists.get(0).get(k).getId() - lists.get(0).get(m).getId() + 1,
                                        DbUtil.getScheme(list.get(j).getSchemeId()).getName(), j + 1));
                                nextStep = true;
                                finish();
                                return;
                            }
                        }
                    lists.get(0).addAll(list);
                }
            } else
                lists.add(DbUtil.getCurrentDetonatorList());
            if (lists.get(0).size() <= 0) {
                myApp.myToast(DetonateStep2Activity.this, R.string.message_list_not_found);
                nextStep = true;
                finish();
                return;
            }
            for (int i = 1; i <= ListType.NONE.ordinal(); i++)
                lists.add(new ArrayList<>());
            for (DetonatorBean bean : lists.get(0))
                bean.setDownloaded(false);
            initPager();
            btnCharge.setEnabled(false);
            btnRescan = findViewById(R.id.btn_rescan);
            btnRescan.requestFocus();
            btnRescan.setEnabled(false);
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = DataReceiveListener.getInstance(DetonateStep2Activity.this, myHandler);
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            myReceiveListener.setDetonatorAmount(lists.get(ListType.ALL.ordinal()).size());
            btnCharge.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));
            btnRescan.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initSound();
    }

    private void resetTabTitle(boolean init) {
        for (int i = 0; i < tabTitle.size(); i++) {
            TextView tv = (TextView) LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_tab_textview, tabList, false);
            tv.setText(String.format(Locale.getDefault(), "%s(%d)", tabTitle.get(ListType.values()[i]), lists.get(i).size()));
            TabLayout.Tab tab = tabList.getTabAt(i);
            if (tab != null) {
                tv.setTextColor(getColor(tab.isSelected() ? R.color.text_blue : R.color.text_black));
                if (!init && tab.getCustomView() != null) {
                    final ViewParent customParent = tab.getCustomView().getParent();
                    if (customParent != null)
                        ((ViewGroup) customParent).removeView(tab.getCustomView());
                }
                tab.setCustomView(tv);
            }
        }
    }

    private void initPager() {
        tabList = findViewById(R.id.tab_title);
        pagerList = findViewById(R.id.view_pager);
        fragments = new ArrayList<>();
        int[] title = {R.string.tab_title_all_list, R.string.tab_title_online, R.string.tab_title_offline, R.string.tab_title_delay_error};
        tabTitle = new HashMap<>();
        for (int i = 0; i < ListType.NONE.ordinal(); i++)
            tabTitle.put(ListType.values()[i], getString(title[i]));

        for (int i = 0; i < tabTitle.size(); i++) {
            tabList.addTab(tabList.newTab());
            TabFragment tabFragment = new TabFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(KeyUtils.KEY_LIST, (ArrayList<DetonatorBean>) lists.get(i));
            tabFragment.setArguments(bundle);
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
                fragments.get(tab.getPosition()).updateList(lists.get(tab.getPosition()));
                if (rescanWhich == ListType.NONE) {
                    btnRescan.setEnabled(tab.getPosition() == ListType.DETECTED.ordinal() ||
                            (tab.getPosition() != ListType.ERROR.ordinal() && lists.get(tab.getPosition()).size() > 0));
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
                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(String.format(Locale.getDefault(), getString(R.string.dialog_delay_out_of_range), ConstantUtils.MAX_DELAY_TIME))
                                .setPositiveButton(R.string.button_confirm, null)
                                .show(), true));
                    } else if (max < 0) {
                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(R.string.dialog_delay_out_of_range_2)
                                .setPositiveButton(R.string.button_confirm, null)
                                .show(), true));
                    } else if (btnCharge.isEnabled()) {
                        runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_detect_again)
                                .setMessage(R.string.dialog_detect_again)
                                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> startDetect())
                                .setNegativeButton(R.string.button_cancel, null)
                                .show(), true));
                    } else
                        startDetect();
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (btnCharge.isEnabled())
                    enterCharge();
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
                        final View view = LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_dialog_edit, null);
                        final EditText etDelay = view.findViewById(R.id.et_dialog);
                        view.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                        etDelay.setHint(R.string.hint_input_voltage);
                        etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                        etDelay.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_edit_charge_voltage)
                                .setView(view)
                                .setCancelable(false)
                                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                                    try {
                                        float v = Float.parseFloat(etDelay.getText().toString());
                                        if (v >= 18 && v <= 22) {
                                            chargeVoltage = v;
                                            BaseApplication.settings.setChargeVoltage(v);
                                            myApp.saveSettings();
                                            return;
                                        }
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_input_voltage_error);
                                })
                                .setNegativeButton(R.string.button_cancel, null)
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
        BaseApplication.writeFile(getString(R.string.button_start_charge));
        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                .setView(R.layout.layout_dialog_enter_charge)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                    myHandler.removeCallbacksAndMessages(null);
                    Intent intent = new Intent();
                    intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_VOLTAGE, chargeVoltage);
                    intent.putParcelableArrayListExtra(KeyUtils.KEY_LIST, (ArrayList<DetonatorBean>) lists.get(0));
                    intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
                    intent.setClass(DetonateStep2Activity.this, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false) ? UniteExplodeActivity.class : DetonateStep3Activity.class);
                    startActivity(intent);
                    nextStep = true;
                    finish();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show(), false);
    }

    private int maxDelay() {
        int result = 0;
        for (DetonatorBean bean : lists.get(0)) {
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
            BaseApplication.writeFile(getString(R.string.button_start_detect) + ", " + getString(R.string.tab_title_online) + ":" + lists.get(0).size());
            for (int i = 1; i < ListType.NONE.ordinal(); i++)
                lists.get(i).clear();
            for (DetonatorBean d : lists.get(0))
                d.setDownloaded(false);
            for (long id : schemeId)
                DbUtil.clearDetonatorDownloadFlag(id);
            rescanWhich = ListType.ALL;
        } else {
            BaseApplication.writeFile(getString(R.string.button_start_detect) + ", " + getString(R.string.tab_title_offline) + ":" + lists.get(ListType.NOT_FOUND.ordinal()).size());
            rescanWhich = ListType.NOT_FOUND;
        }
        myReceiveListener.setStartAutoDetect(false);
        myHandler.sendEmptyMessage(DETECT_RESCAN);
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool)
            soundSuccess = soundPool.load(this, R.raw.found, 1);
    }

    @Override
    public void finish() {
        if (!nextStep) {
            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_detect)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                        if (pDialog != null && pDialog.isShowing())
                            pDialog.dismiss();
                        DetonateStep2Activity.super.finish();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialogInterface, i) -> {
                        if (pDialog != null && pDialog.getMax() != pDialog.getProgress())
                            pDialog.show();
                    })
                    .show(), true);
        } else
            super.finish();
    }

    @Override
    public void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        if (!nextStep && myReceiveListener != null)
            myReceiveListener.closeAllHandler();
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.release();
            soundPool = null;
        }
        BaseApplication.releaseWakeLock(DetonateStep2Activity.this);
        super.onDestroy();
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
