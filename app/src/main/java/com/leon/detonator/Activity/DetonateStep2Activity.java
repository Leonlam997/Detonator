package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
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
import com.leon.detonator.Adapter.DetonatorListAdapter;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Dialog.MyProgressDialog;
import com.leon.detonator.Fragment.TabFragment;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.KeyUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetonateStep2Activity extends BaseActivity {
    private final int DETECT_SUCCESS = 1,
            DETECT_FINISH = 2,
            DETECT_RESCAN = 3,
            DETECT_RESCAN_STEP_1 = 4,
            DETECT_RESCAN_STEP_2 = 5,
            DETECT_RESCAN_STEP_3 = 6,
            DETECT_SEND_COMMAND = 7,
            DETECT_GET_RECORD = 8,
            DETECT_RESTART = 10;
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private List<List<DetonatorInfoBean>> lists;
    private List<DetonatorInfoBean> checkList;
    private List<Fragment> fragments;
    private Map<ConstantUtils.LIST_TYPE, String> tabTitle;
    private List<DetonatorListAdapter> adapterList;
    private SerialPortUtil serialPortUtil;
    private boolean setDelay, beforeDetect = false, bypass = false, drawWaveform = false,
            nextStep = false, changingVoltage = false, restart, firstTime, charged = false, startVoltage = false;
    private MyButton btnCharge, btnRescan;
    private TabLayout tabList;
    private ViewPager pagerList;
    private MyProgressDialog pDialog;
    private final Handler progressHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (pDialog.getProgress() < pDialog.getMax()) {
                pDialog.incrementProgressBy(1);
            }
            return false;
        }
    });
    private BaseApplication myApp;
    private int bypassCount = 0, listIndex = 0, resendCount = 0, loadCount, dac = 2330, cmdMode = 0, tempDac, startDac = 0,
            hiddenKeyCount = 0, hiddenType = 0, soundSuccess, soundAlert, changeMode, totalActivationTime;
    private float voltage = 22, workVoltage = 12;
    private ConstantUtils.LIST_TYPE rescanWhich;
    private SerialDataReceiveListener myReceiveListener;
    private LocalSettingBean settingBean;
    private SoundPool soundPool;
    private String[] fileList;
    private TabFragment allListTab;
    private String waveformData;

    private SerialCommand.ACTION_TYPE getCmdType(boolean get) {
        switch (cmdMode) {
            case 2:
                return get ? SerialCommand.ACTION_TYPE.SHORT_CMD2_GET_DELAY : SerialCommand.ACTION_TYPE.SHORT_CMD2_SET_DELAY;
            case 0:
                if (!newLG)
                    return get ? SerialCommand.ACTION_TYPE.GET_DELAY : SerialCommand.ACTION_TYPE.SET_DELAY;
            default:
                return get ? SerialCommand.ACTION_TYPE.SHORT_CMD1_GET_DELAY : SerialCommand.ACTION_TYPE.SHORT_CMD1_SET_DELAY;
        }
    }

    private final Handler statusHandler = new Handler(new Handler.Callback() {
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
                    btnCharge.setEnabled(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size() == lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size());
                    if (pDialog.isShowing())
                        pDialog.dismiss();
                    rescanWhich = ConstantUtils.LIST_TYPE.END;
                    btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
//                    if (tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal())
//                        btnRescan.setEnabled(countIndex() < lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).size());
//                    else
//                        btnRescan.setEnabled(lists.get(tabList.getSelectedTabPosition()).size() > 0);
                    setProgressVisibility(false);
                    myReceiveListener.setStartAutoDetect(true);
                    int i = lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size() > 0 ? ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal() :
                            (lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() > 0 ? ConstantUtils.LIST_TYPE.ERROR.ordinal() : 0);
                    if (i > 0) {
                        pagerList.setCurrentItem(i, true);
                    }
                    break;
                case DETECT_RESCAN:
                    if (!drawWaveform) {
                        BaseApplication.acquireWakeLock(DetonateStep2Activity.this);
                        allListTab.setCheckedHint(true);
                        resetTabTitle(false);
                        adapterList.get(pagerList.getCurrentItem()).updateList(lists.get(pagerList.getCurrentItem()));
                        pDialog.show();
                    }
                    btnCharge.setEnabled(false);
                    btnRescan.setEnabled(false);
                    setProgressVisibility(true);
                    restart = false;
                    if (newLG) {
                        changeMode = 4;
                        statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                        break;
                    }
                    if (firstTime && !charged) {
                        firstTime = false;
                        myReceiveListener.setMaxCurrent(ConstantUtils.MAXIMUM_CURRENT * 2);
                    } else {
                        myReceiveListener.setMaxCurrent(ConstantUtils.MAXIMUM_CURRENT);
                        statusHandler.sendEmptyMessage(DETECT_RESCAN_STEP_3);
                        break;
                    }
                case DETECT_RESTART:
                    beforeDetect = true;
                    tempDac = startDac;
//                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
//                    statusHandler.sendEmptyMessageDelayed(DETECT_RESCAN_STEP_1, 2000);
//                    break;
//                case DETECT_RESCAN_STEP_1:
                    serialPortUtil.sendCmd("bypass,000000,pwstart," + startDac + "###");
                    statusHandler.sendEmptyMessageDelayed(DETECT_RESCAN_STEP_1, 2000);
//                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "0###");
//                    statusHandler.sendEmptyMessageDelayed(DETECT_RESCAN_STEP_4, 1500);
                    break;
                case DETECT_RESCAN_STEP_1:
                    tempDac += 200;
                    if (tempDac < dac) {
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + tempDac + "###");
                        statusHandler.sendEmptyMessageDelayed(DETECT_RESCAN_STEP_1, ConstantUtils.BOOST_TIME);
                        break;
                    }
                case DETECT_RESCAN_STEP_2:
                    if (newLG) {
                        myReceiveListener.setStartAutoDetect(false);
                        changeMode = drawWaveform ? 10 : 6;
                        statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                    } else {
                        serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
//                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + (int) workVoltage + "###");
                        if (restart) {
                            statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, 2000);
                        } else {
                            myReceiveListener.setStartAutoDetect(true);
//                        waitForKey = true;
//                        myApp.myToast(DetonateStep2Activity.this, "按任意键开始检测！");
                            statusHandler.sendEmptyMessageDelayed(DETECT_RESCAN_STEP_3, 2000);
                        }
                    }
                    break;
                case DETECT_RESCAN_STEP_3:
                    changeMode = 0;
                    myApp.myToast(DetonateStep2Activity.this, "开始检测！");
                    myReceiveListener.setStartAutoDetect(false);
                    beforeDetect = false;
                    statusHandler.sendEmptyMessage(DETECT_GET_RECORD);
                    break;
                case DETECT_SEND_COMMAND:
                    if (null == serialPortUtil)
                        return false;
                    statusHandler.removeMessages(DETECT_SEND_COMMAND);
                    myReceiveListener.setRcvData("");
                    beforeDetect = false;
                    if (newLG && changeMode > 0) {
                        switch (changeMode) {
                            case 1:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 2:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_WORK_VOLTAGE, workVoltage));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 4:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_OPEN_BUS, 1));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 6:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 1));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 8:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_OPEN_BUS, 0));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 10:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_SIGNAL_WAVEFORM, 1));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                            case 12:
                                serialPortUtil.sendCmd(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getAddress(), getCmdType(false), lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getDelayTime());
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, 1500);
                                return false;
                            case 14:
                                serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_SIGNAL_WAVEFORM, 0));
                                statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, ConstantUtils.RESEND_CMD_TIMEOUT);
                                return false;
                        }
                    }
                    if (changingVoltage) {
                        serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                        statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, 200);
                    } else {
                        myReceiveListener.setFeedback(false);
                        if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND) {
                            if (listIndex >= checkList.size()) {
                                myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                statusHandler.sendEmptyMessage(DETECT_FINISH);
                                break;
                            }
                            setDelay = !checkList.get(listIndex).isDownloaded();
                            if (setDelay) {
                                serialPortUtil.sendCmd(checkList.get(listIndex).getAddress(), getCmdType(false), checkList.get(listIndex).getDelayTime());
                            } else
                                serialPortUtil.sendCmd(checkList.get(listIndex).getAddress(), getCmdType(true), 0);
                        } else if (rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
                            if (listIndex >= checkList.size()) {
                                myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                statusHandler.sendEmptyMessage(DETECT_FINISH);
                                break;
                            }
                            setDelay = !checkList.get(listIndex).isDownloaded();
                            if (setDelay)
                                for (int j = 0; j < lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size(); j++) {
                                    if (lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(j).getAddress().equals(checkList.get(listIndex).getAddress())) {
                                        serialPortUtil.sendCmd(checkList.get(listIndex).getAddress(), getCmdType(false), lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(j).getDelayTime());
                                        break;
                                    }
                                }
                            else
                                serialPortUtil.sendCmd(checkList.get(listIndex).getAddress(), getCmdType(true), 0);
                        } else {
                            if (listIndex >= lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size()) {
                                myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                                statusHandler.sendEmptyMessage(DETECT_FINISH);
                                break;
                            }
                            setDelay = !lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).isDownloaded();
                            if (setDelay) {
                                serialPortUtil.sendCmd(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getAddress(), getCmdType(false), lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getDelayTime());
                            } else
                                serialPortUtil.sendCmd(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getAddress(), getCmdType(true), 0);
                        }
                        statusHandler.sendEmptyMessageDelayed(DETECT_GET_RECORD, ConstantUtils.RESEND_CMD_TIMEOUT);
                    }
                    break;
                case DETECT_GET_RECORD:
                    statusHandler.removeMessages(DETECT_GET_RECORD);
                    if (rescanWhich == ConstantUtils.LIST_TYPE.END || beforeDetect)
                        break;
                    if (resendCount >= ConstantUtils.RESEND_TIMES) {
                        if (bypass) {
                            addDetonator(-1);
                        } else if (rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
                            for (int j = 0; j < lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).size(); j++) {
                                if (lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).get(j).getAddress().equals(checkList.get(listIndex).getAddress())) {
                                    lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).remove(j);
                                    break;
                                }
                            }
                            lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).add(insertLocate(lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal())), checkList.get(listIndex));
                        } else
                            lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).add(rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND ? checkList.get(listIndex) : lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex));
                        statusHandler.sendEmptyMessage(DETECT_SUCCESS);
                        progressHandler.sendEmptyMessage(1);
                        restart = false;
                        listIndex++;
                        cmdMode = nextCmdMode();
                        resendCount = 0;
                        setDelay = false;
                    }
                    if (((rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND || rescanWhich == ConstantUtils.LIST_TYPE.ERROR) && listIndex >= checkList.size())
                            || listIndex >= lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size()) {
                        myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                        statusHandler.sendEmptyMessage(DETECT_FINISH);
                    } else {
                        resendCount++;
                        statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                    }
                    break;
                default:
                    myApp.myToast(DetonateStep2Activity.this, (String) msg.obj);
                    break;
            }
            return false;
        }
    });

    private String getConfirmString(boolean get) {
        switch (cmdMode) {
            case 1:
                return SerialCommand.RESPOND_CONFIRM.get(get ? SerialCommand.ACTION_TYPE.SHORT_CMD1_GET_DELAY : SerialCommand.ACTION_TYPE.SHORT_CMD1_SET_DELAY);
            case 2:
                return SerialCommand.RESPOND_CONFIRM.get(get ? SerialCommand.ACTION_TYPE.SHORT_CMD2_GET_DELAY : SerialCommand.ACTION_TYPE.SHORT_CMD2_SET_DELAY);
            default:
                return SerialCommand.RESPOND_CONFIRM.get(get ? SerialCommand.ACTION_TYPE.GET_DELAY : SerialCommand.ACTION_TYPE.SET_DELAY);
        }
    }

    private final Runnable bufferRunnable = new Runnable() {
        @Override
        public void run() {
            String received = myReceiveListener.getRcvData();
            if (beforeDetect || received.equals(""))
                return;

            if (received.contains(SerialCommand.ALERT_SHORT_CIRCUIT)) {
                closeAllHandler();
                if (serialPortUtil != null) {
                    serialPortUtil.closeSerialPort();
                    serialPortUtil = null;
                }
                runOnUiThread(() -> new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_warning)
                        .setMessage(loadCount > 400 || loadCount == 0 ?
                                getResources().getString(R.string.dialog_short_circuit) :
                                String.format(Locale.CHINA, getResources().getString(R.string.dialog_overload), loadCount))
                        .setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                            nextStep = true;
                            finish();
                        })
                        .create().show());
                myApp.playSoundVibrate(soundPool, soundAlert);
            } else if (received.startsWith("A")) {
                try {
                    loadCount = (int) Float.parseFloat(received.substring(1)) / 25;
                    BaseApplication.writeFile("检测电流：" + received.substring(1) + "，列表总共" + lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size() + "发，负载约" + loadCount + "发");
//                    sendMsg("负载约：" + loadCount + "发");
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
                if (!changingVoltage)
                    myReceiveListener.setFeedback(false);
                if (restart) {
                    statusHandler.removeMessages(DETECT_RESTART);
                    statusHandler.sendEmptyMessage(DETECT_RESTART);
                } else {
                    statusHandler.removeMessages(DETECT_SEND_COMMAND);
                    statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                }
            } else if (newLG && received.contains(SerialCommand.AT_CMD_RESPOND)) {
                statusHandler.removeMessages(DETECT_SEND_COMMAND);
                Log.d("ZBEST", changeMode + "");
                switch (changeMode) {
                    case 1:
                        if (drawWaveform) {
                            changeMode = 14;
                        } else {
                            changeMode++;
                        }
                        statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                        break;
                    case 2:
                        changeMode++;
                        myApp.myToast(DetonateStep2Activity.this, R.string.message_modify_finished);
                        myReceiveListener.setStartAutoDetect(true);
                        runOnUiThread(() -> {
                            setProgressVisibility(false);
                            btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                        });
                        break;
                    case 4:
                        changeMode++;
                        myReceiveListener.setStartAutoDetect(true);
                        statusHandler.sendEmptyMessageDelayed(DETECT_RESCAN_STEP_2, totalActivationTime);
                        break;
                    case 6:
                        if (drawWaveform) {
                            listIndex = 0;
                            changeMode = 12;
                            rescanWhich = ConstantUtils.LIST_TYPE.ALL;
                            cmdMode = nextCmdMode();
                            statusHandler.sendEmptyMessage(DETECT_GET_RECORD);
                        } else {
                            changeMode++;
                            statusHandler.sendEmptyMessage(DETECT_RESCAN_STEP_3);
                        }
                        break;
                    case 10:
                        changeMode = 6;
                        statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                        break;
                    case 14:
                        changeMode++;
                        drawWaveform = false;
                        statusHandler.sendEmptyMessage(DETECT_FINISH);
                        if (waveformData.length() == 2048) {
                            Intent intent = new Intent(DetonateStep2Activity.this, WaveformActivity.class);
                            intent.putExtra(KeyUtils.KEY_WAVEFORM_DATA, waveformData);
                            startActivity(intent);
                        } else {
                            ((BaseApplication) getApplication()).myToast(DetonateStep2Activity.this, R.string.message_waveform_data_error);
                        }
                        break;
                }
                myReceiveListener.setRcvData("");
            } else if (changingVoltage) {
                if (received.startsWith("V")) {
                    try {
                        float v = Integer.parseInt(received.substring(1)) / 100.0f;
                        statusHandler.removeMessages(DETECT_SEND_COMMAND);
                        if (Math.abs(v - workVoltage) < 0.1f) {
                            changingVoltage = false;
                            myApp.myToast(DetonateStep2Activity.this, R.string.message_modify_finished);
                            myReceiveListener.setFeedback(false);
                            myReceiveListener.setStartAutoDetect(true);
                            settingBean.getDacMap().put(workVoltage, tempDac);
                            if (startVoltage)
                                startDac = tempDac;
                            else
                                dac = tempDac;
                            runOnUiThread(() -> {
                                setProgressVisibility(false);
                                btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                            });
                            myApp.saveSettings(settingBean);
                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                        } else {
                            tempDac += (v > workVoltage ? 100 : -100) * (Math.abs(v - workVoltage) - 0.01f);
                            if (tempDac < 50 || tempDac > 4000) {
                                tempDac = 94 + (int) ((29 - workVoltage) * 124);
                            }
                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + tempDac + "###");
                            statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, 200);
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                }
            } else if (received.contains(SerialCommand.INITIAL_FAIL)) {
                nextStep = true;
                myApp.myToast(DetonateStep2Activity.this, R.string.message_open_module_fail);
                finish();
            } else if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                myReceiveListener.setRcvData("");
                if (!newLG)
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                if (rescanWhich == ConstantUtils.LIST_TYPE.END) {
                    statusHandler.sendEmptyMessage(DETECT_FINISH);
                } else if (rescanWhich == ConstantUtils.LIST_TYPE.ALL) {
                    statusHandler.sendEmptyMessage(DETECT_RESCAN);
                }
            } else if (drawWaveform) {
                waveformData = received;
                statusHandler.removeMessages(DETECT_SEND_COMMAND);
                changeMode = 1;
                statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
            } else if (rescanWhich != ConstantUtils.LIST_TYPE.END) {
                boolean success = false;
                int feedback = -1;
                statusHandler.removeMessages(DETECT_GET_RECORD);
                if (setDelay) {
                    myApp.myToast(DetonateStep2Activity.this, received + "," + getConfirmString(false));
                    if (!newLG || received.startsWith(SerialCommand.DATA_PREFIX)) {//(!newLG && received.contains(getConfirmString(false)))
                        String address;
                        if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND || rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
                            checkList.get(listIndex).setDownloaded(true);
                            address = checkList.get(listIndex).getAddress();
                            if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND)
                                for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
                                    if (b.getAddress().equals(address)) {
                                        b.setDownloaded(true);
                                        break;
                                    }
                                }
                        } else {
                            address = lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getAddress();
                            lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).setDownloaded(true);
                        }
                        try {
                            for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                                if (bean.getAddress().equals(address))
                                    bean.setDownloaded(true);
                            myApp.writeToFile(myApp.getListFile(), lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()));
                        } catch (JSONException e) {
                            BaseApplication.writeErrorLog(e);
                        }
                        setDelay = false;
                        success = true;
                    }
                } else {
                    String confirm = getConfirmString(true);
//                        myApp.myToast(DetonateStep2Activity.this, received + "," + confirm);

                    if (received.contains(confirm)) {
                        String tempDelay = received.substring(received.indexOf(confirm) + confirm.length());
                        if (tempDelay.length() == 6) {
                            try {
                                int checkSum = 0;
                                for (int i = 0; i < tempDelay.length() - 2; i += 2) {
                                    checkSum += Integer.parseInt(tempDelay.substring(i, i + 2), 16);
                                }
                                if (String.format("%02X", checkSum).endsWith(tempDelay.substring(4))) {
                                    feedback = Integer.parseInt(tempDelay.substring(0, 4), 16);
                                    success = true;
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        }
                    }
                }
                if (success) {
                    addDetonator(feedback);
                    resendCount = 1;
                    restart = false;
                    listIndex++;
                    cmdMode = nextCmdMode();
                    progressHandler.sendEmptyMessage(1);
                    statusHandler.sendEmptyMessage(DETECT_SUCCESS);

                    if (((rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND || rescanWhich == ConstantUtils.LIST_TYPE.ERROR) && listIndex >= checkList.size()) || listIndex >= lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size()) {
                        myApp.myToast(DetonateStep2Activity.this, R.string.message_detect_finished);
                        statusHandler.sendEmptyMessage(DETECT_FINISH);
                    } else
                        statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                } else
                    statusHandler.sendEmptyMessageDelayed(DETECT_GET_RECORD, ConstantUtils.RESEND_CMD_TIMEOUT);
            }
        }
    };

    private int nextCmdMode() {
        if (((rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND || rescanWhich == ConstantUtils.LIST_TYPE.ERROR) && listIndex >= checkList.size()) || listIndex >= lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size())
            return 0;
        int result = 1;
        String address;
        if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND || rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
            address = checkList.get(listIndex).getAddress();
        } else {
            address = lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex).getAddress();
        }
        for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal())) {
            if (!bean.getAddress().equals(address)) {
                if (bean.getAddress().endsWith(address.substring(address.length() - 4))) {
                    return 0;
                } else if (1 == result && bean.getAddress().endsWith(address.substring(address.length() - 2))) {
                    result = 2;
                }
            }
        }
        return result;
    }

    private void addDetonator(int feedback) {
        DetonatorInfoBean bean;
        if (rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
            bean = new DetonatorInfoBean();
            for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal())) {
                if (b.getAddress().equals(checkList.get(listIndex).getAddress())) {
                    bean = new DetonatorInfoBean(b);
                    break;
                }
            }
        } else
            bean = new DetonatorInfoBean(rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND ? checkList.get(listIndex) : lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(listIndex));
        if (-1 != feedback && feedback != bean.getDelayTime()) {
            myApp.myToast(DetonateStep2Activity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_detonator_delay_error), bean.getAddress(), feedback, bean.getDelayTime()));
            bean.setDownloaded(false);
            bean.setDelayTime(feedback);
            if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND)
                lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).add(insertLocate(lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal())), bean);
            else
                lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).add(bean);
        }
        if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND) {
            lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).add(insertLocate(lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal())), bean);
        } else if (rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
            for (DetonatorInfoBean b : lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal())) {
                if (b.getAddress().equals(checkList.get(listIndex).getAddress())) {
                    b.setDownloaded(-1 == feedback || bean.isDownloaded());
                    b.setDelayTime(-1 == feedback ? bean.getDelayTime() : feedback);
                    break;
                }
            }
        } else
            lists.get(ConstantUtils.LIST_TYPE.DETECTED.ordinal()).add(bean);
    }

    private int searchIndex(String address) {
        int i = 0;
        for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal())) {
            if (bean.getAddress().equals(address))
                break;
            i++;
        }
        return i + 3;
    }

    private int insertLocate(List<DetonatorInfoBean> list) {
        for (int j = 0; j < lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size(); j++) {
            if (lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(j).getAddress().equals(checkList.get(listIndex).getAddress())) {
                for (int k = j - 1; k > 0; k--) {
                    for (int l = 0; l < list.size(); l++) {
                        if (lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).get(k).getAddress().equals(list.get(l).getAddress())) {
                            return l + 1;
                        }
                    }
                }
            }
        }
        return 0;
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
        firstTime = true;
        myApp = (BaseApplication) getApplication();

        settingBean = BaseApplication.readSettings();
        totalActivationTime = settingBean.getFirstPulseTime() + settingBean.getSecondPulseTime() + settingBean.getThirdPulseTime() + 10;
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
//                if (listIndex == 0) {
//                    rescanWhich = ConstantUtils.LIST_TYPE.ALL;
//                    checkFastDetect();
//                }
            }
            initProgressDialog();
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
            btnCharge.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_2));
            btnRescan.setOnClickListener(v -> executeFunction(KeyEvent.KEYCODE_1));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initSound();
    }

    private void initProgressDialog() {
        pDialog = new MyProgressDialog(this);
        pDialog.setInverseBackgroundForced(false);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCanceledOnTouchOutside(false);
        pDialog.setTitle(R.string.progress_title);
        pDialog.setMessage(getResources().getString(R.string.progress_detect));
        pDialog.setMax(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size());
        pDialog.setProgress(0);
//        pDialog.setOnKeyListener((dialog, keyCode, event) -> {
//            if (waitForKey) {
//                statusHandler.sendEmptyMessage(DETECT_RESCAN_STEP_3);
//                waitForKey = false;
//            }
//            return false;
//        });
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
            tabTitle.put(ConstantUtils.LIST_TYPE.values()[i], getResources().getString(title[i]));
        }
        for (int i = 0; i < tabTitle.size() - 1; i++) {
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
                    btnRescan.setEnabled(tab.getPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tab.getPosition()).size() > 0);
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
                    if (maxDelay() > ConstantUtils.MAX_DELAY_TIME) {
                        new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_out_of_range)
                                .setMessage(R.string.dialog_delay_out_of_range)
                                .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> startDetect())
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show();
                    } else {
                        startDetect();
                    }
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (btnCharge.isEnabled()) {
                    enterCharge(false);
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
                    hiddenFunction();
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
            case KeyEvent.KEYCODE_POUND:
                hiddenKeyCount = 0;
                if (bypassCount < 2)
                    bypassCount++;
                else if (!bypass) {
                    bypass = true;
                    myApp.myToast(this, "Bypass mode");
                }
                break;
            default:
                hiddenKeyCount = 0;
                bypass = false;
                bypassCount = 0;
                break;
        }
    }

    private void enterCharge(boolean bypass) {
        closeAllHandler();
        Intent intent = new Intent();
        intent.putExtra(KeyUtils.KEY_EXPLODE_VOLTAGE, Math.max(voltage, workVoltage));
        intent.putExtra(KeyUtils.KEY_BYPASS_EXPLODE, bypass);
        intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
        intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
        intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
        intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
        intent.setClass(DetonateStep2Activity.this, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false) ? UniteExplodeActivity.class : DetonateStep3Activity.class);
        saveList();
        startActivity(intent);
        nextStep = true;
        finish();
    }

    private void hiddenFunction() {
        startVoltage = false;
        switch (hiddenType) {
            case 7:
                if (newLG) {
                    if (!btnRescan.isEnabled())
                        statusHandler.sendEmptyMessage(DETECT_FINISH);
                    drawWaveform = true;
                    myReceiveListener.setStartAutoDetect(false);
                    statusHandler.sendEmptyMessage(DETECT_RESCAN);
                    break;
                } else
                    startVoltage = true;
            case 3:
                myReceiveListener.setStartAutoDetect(false);
                statusHandler.removeCallbacksAndMessages(null);
//                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                btnRescan.setEnabled(false);
                setProgressVisibility(true);
                final View view1 = LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_edit_dialog, null);
                final EditText etDelay1 = view1.findViewById(R.id.et_dialog);
                view1.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                etDelay1.setHint(R.string.hint_input_voltage);
                etDelay1.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                etDelay1.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                        .setTitle(startVoltage ? R.string.dialog_title_edit_start_voltage : R.string.dialog_title_edit_work_voltage)
                        .setView(view1)
                        .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
//                            if (!newLG)
//                                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "0###");
                            new Handler(msg -> {
                                if (null != serialPortUtil) {
                                    try {
                                        float v = Float.parseFloat(etDelay1.getText().toString());
                                        if (v > 6 && v <= 29) {
                                            firstTime = true;
                                            workVoltage = v;
                                            startChangeVoltage();
//                                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + (int) workVoltage + "###");
//                                            sendMsg(DETECT_MESSAGE, R.string.message_modify_finished);
//                                            myReceiveListener.setStartAutoDetect(true);
//                                            btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
//                                            setProgressVisibility(false);
                                            return false;
                                        }
                                    } catch (Exception e) {
                                        BaseApplication.writeErrorLog(e);
                                    }
                                    myApp.myToast(DetonateStep2Activity.this, R.string.message_input_voltage_error);
                                    btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                                    setProgressVisibility(false);
                                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
                                }
                                return false;
                            }).sendEmptyMessageDelayed(1, 0);//newLG ? 0 : 2000);
                        })
                        .setNegativeButton(R.string.btn_cancel, (dialog, which1) -> {
                            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "0###");
                            new Handler(msg -> {
                                if (null != serialPortUtil) {
                                    btnRescan.setEnabled(tabList.getSelectedTabPosition() == ConstantUtils.LIST_TYPE.DETECTED.ordinal() || lists.get(tabList.getSelectedTabPosition()).size() > 0);
                                    setProgressVisibility(false);
                                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + dac + "###");
                                }
                                return false;
                            }).sendEmptyMessageDelayed(1, 2000);
                        })
                        .show();
                break;
            case 4:
                final View view = LayoutInflater.from(DetonateStep2Activity.this).inflate(R.layout.layout_edit_dialog, null);
                final EditText etDelay = view.findViewById(R.id.et_dialog);
                view.findViewById(R.id.tv_dialog).setVisibility(View.GONE);
                etDelay.setHint(R.string.hint_input_voltage);
                etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                etDelay.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_edit_explode_voltage)
                        .setView(view)
                        .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> {
                            try {
                                float v = Float.parseFloat(etDelay.getText().toString());
                                if (v >= 6 && v <= 29) {
                                    voltage = v;
                                    return;
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            myApp.myToast(DetonateStep2Activity.this, R.string.message_input_voltage_error);
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
                break;
            case 5:
                if (!btnRescan.isEnabled())
                    statusHandler.sendEmptyMessage(DETECT_FINISH);
                if (newLG) {
                    changeMode = 8;
                    statusHandler.sendEmptyMessage(DETECT_SEND_COMMAND);
                } else {
                    firstTime = true;
                    serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
                }
                break;
            case 6:
                statusHandler.removeCallbacksAndMessages(null);
                if (newLG) {
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size(), (int) (workVoltage * 10));
                } else {
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.OPEN_CAPACITOR, 1);
                }
                myApp.myToast(this, "Charging");
                btnRescan.setEnabled(true);
                break;
//            case 7:
//                statusHandler.removeCallbacksAndMessages(null);
//                myReceiveListener.setStartAutoDetect(false);
//                btnRescan.setEnabled(false);
//                setProgressVisibility(true);
//                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "0###");
//                statusHandler.sendEmptyMessageDelayed(DETECT_CHARGE, ConstantUtils.BOOST_TIME);
//                break;
            case 8:
                enterCharge(true);
                break;
            case 9:
                statusHandler.removeCallbacksAndMessages(null);
                for (List<DetonatorInfoBean> l : lists)
                    for (DetonatorInfoBean b : l) {
                        b.setDownloaded(false);
                        b.setDelayTime(b.getDelayTime() + 1);
                    }
                adapterList.get(ConstantUtils.LIST_TYPE.ALL.ordinal()).updateList(lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()));
                break;

        }
    }

    private int maxDelay() {
        int result = 0;
        switch (ConstantUtils.LIST_TYPE.values()[pagerList.getCurrentItem()]) {
            case DETECTED:
            case ALL:
                for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.END.ordinal())) {
                    if (bean.getDelayTime() > result)
                        result = bean.getDelayTime();
                }
                break;
            case NOT_FOUND:
                for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal())) {
                    if (bean.getDelayTime() > result)
                        result = bean.getDelayTime();
                }
                break;
            case ERROR:
                for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal())) {
                    if (bean.getDelayTime() > result)
                        result = bean.getDelayTime();
                }
                break;
        }
        return result;
    }

    private void startDetect() {
        initProgressDialog();
        switch (ConstantUtils.LIST_TYPE.values()[pagerList.getCurrentItem()]) {
            case DETECTED:
                listIndex = countIndex();
                cmdMode = nextCmdMode();
                if (listIndex < lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size()) {
                    rescanWhich = ConstantUtils.LIST_TYPE.DETECTED;
                    pDialog.setMax(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size());
                    break;
                }
            case ALL:
                listIndex = 0;
                for (DetonatorInfoBean bean : lists.get(ConstantUtils.LIST_TYPE.ALL.ordinal()))
                    bean.setDownloaded(false);
                pDialog.setMax(lists.get(ConstantUtils.LIST_TYPE.END.ordinal()).size());
                for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i < ConstantUtils.LIST_TYPE.END.ordinal(); i++)
                    lists.get(i).clear();
                rescanWhich = ConstantUtils.LIST_TYPE.ALL;
                cmdMode = nextCmdMode();
                break;
            case NOT_FOUND:
                if (lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size() > 0) {
                    listIndex = 0;
                    rescanWhich = ConstantUtils.LIST_TYPE.NOT_FOUND;
                    pDialog.setMax(lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).size());
                    checkList = new ArrayList<>(lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()));
                    lists.get(ConstantUtils.LIST_TYPE.NOT_FOUND.ordinal()).clear();
                    cmdMode = nextCmdMode();
                } else {
                    myApp.myToast(DetonateStep2Activity.this, R.string.message_empty_list);
                    return;
                }
                break;
            case ERROR:
                if (lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size() > 0) {
                    listIndex = 0;
                    rescanWhich = ConstantUtils.LIST_TYPE.ERROR;
                    pDialog.setMax(lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).size());
                    checkList = new ArrayList<>(lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()));
                    lists.get(ConstantUtils.LIST_TYPE.ERROR.ordinal()).clear();
                    cmdMode = nextCmdMode();
                } else {
                    myApp.myToast(DetonateStep2Activity.this, R.string.message_empty_list);
                    return;
                }
                break;
            default:
                break;
        }
        pDialog.setProgress(listIndex);
        resendCount = 0;
        myReceiveListener.setStartAutoDetect(false);
        myReceiveListener.setRcvData("");
        statusHandler.sendEmptyMessage(DETECT_RESCAN);
    }

    private void startChangeVoltage() {
        if (newLG) {
            changeMode = 1;
            serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
        } else {
            changingVoltage = true;
            myReceiveListener.setFeedback(true);
            if (null == settingBean.getDacMap())
                settingBean.setDacMap(new HashMap<>());
            Integer v = settingBean.getDacMap().get(workVoltage);
            if (null != v) {
                tempDac = v;
            } else {
                tempDac = 94 + (int) ((29 - workVoltage) * 124);
            }
            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + tempDac + "###");
        }
        statusHandler.sendEmptyMessageDelayed(DETECT_SEND_COMMAND, 200);
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
            statusHandler.sendEmptyMessage(DETECT_FINISH);
            firstTime = true;
            serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + "9999###");
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
        progressHandler.removeCallbacksAndMessages(null);
        statusHandler.removeCallbacksAndMessages(null);
        if (myReceiveListener != null) {
            myReceiveListener.setStartAutoDetect(false);
            myReceiveListener.closeAllHandler();
            myReceiveListener = null;
        }
    }

    @Override
    public void finish() {
        if (!nextStep) {
            new AlertDialog.Builder(DetonateStep2Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.progress_title)
                    .setMessage(R.string.dialog_exit_detect)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_confirm, (dialogInterface, i) -> DetonateStep2Activity.super.finish())
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
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
        if (rescanWhich == ConstantUtils.LIST_TYPE.NOT_FOUND || rescanWhich == ConstantUtils.LIST_TYPE.ERROR) {
            lists.get(rescanWhich.ordinal()).addAll(checkList.subList(listIndex, checkList.size()));
        }
        if (!nextStep) {
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++)
                lists.get(i).clear();
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
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
                if (lists.get(i).size() > 0) {
                    myApp.writeToFile(fileList[i], lists.get(i));
                } else {
                    File file = new File(fileList[i]);
                    if (file.exists() && !file.delete())
                        myApp.myToast(DetonateStep2Activity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
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
