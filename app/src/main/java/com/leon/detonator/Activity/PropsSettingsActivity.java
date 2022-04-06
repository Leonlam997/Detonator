package com.leon.detonator.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Fragment.PropsFragment;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialDataReceiveListener;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PropsSettingsActivity extends BaseActivity {
    private final String[] AT_CMD = {" AT+BWKV=?\r\n",   //工作电压0
            " AT+CMDAC=?\r\n",  //设置总线接收的比较门限值1
            " AT+CURR0=?\r\n", //电流的基础偏移值2
            " AT+BSPV1=?\r\n",  //第1启动脉冲电压3
            " AT+BSPW1=?\r\n",  //第1启动宽度脉冲4
            " AT+BBPW1=?\r\n",  //第1停顿脉冲宽度5
            " AT+BSPV2=?\r\n",  //第2启动脉冲电压6
            " AT+BSPW2=?\r\n",  //第2启动宽度脉冲7
            " AT+BBPW2=?\r\n",  //第2停顿脉冲宽度8
            " AT+BSPV3=?\r\n",  //第3启动脉冲电压9
            " AT+BSPW3=?\r\n",  //第3启动宽度脉冲10
            " AT+LPW=?\r\n",      //低电平脉冲宽度11
            " AT+BIT0R=?\r\n",  //Bit0的高电平停顿长度12
            " AT+BIT1R=?\r\n"};  //Bit1的高电平停顿长度13
    private final EditText[] etAll = new EditText[AT_CMD.length];
    private final Button[][] btnAll = new Button[2][AT_CMD.length];
    private final String[] allData = new String[AT_CMD.length];
    private final int HANDLER_SEND_CMD = 1,
            HANDLER_FINISHED = 2;
    private BaseApplication myApp;
    private SerialPortUtil serialPortUtil;
    private SerialDataReceiveListener myReceiveListener;
    private int changeMode, stepGet;
    private boolean getAllFlag, setFlag, busOpened = false;
    private TabLayout tabList;
    private ViewPager pagerList;
    private List<Fragment> fragments;
    private LocalSettingBean settingBean;
    private MyButton btnBus, btnGetAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_props_settings);
        setTitle(R.string.title_props_settings);
        setProgressVisibility(true);
        myApp = (BaseApplication) getApplication();
        settingBean = BaseApplication.readSettings();
        initPager();
        try {
            serialPortUtil = SerialPortUtil.getInstance();
            myReceiveListener = new SerialDataReceiveListener(this, () -> {
                String received = myReceiveListener.getRcvData();
                if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                    changeMode = 1;
                    myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
                    myReceiveListener.setRcvData("");
                } else if (received.contains(SerialCommand.AT_CMD_RESPOND)) {
                    myHandler.removeMessages(HANDLER_SEND_CMD);
                    switch (changeMode) {
                        case 1:
                            changeMode++;
                            getAllData();
                            break;
                        case 3:
                            changeMode = 7;
                            myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
                            break;
                        case 5:
                            changeMode++;
                            myHandler.sendEmptyMessageDelayed(HANDLER_FINISHED, busOpened ? 0 : settingBean.getFirstPulseTime() + settingBean.getSecondPulseTime() + settingBean.getThirdPulseTime());
                            break;
                        case 7:
                            changeMode++;
                            if (4 == stepGet)
                                settingBean.setFirstPulseTime(Integer.parseInt(etAll[stepGet].getText().toString()));
                            else if (7 == stepGet)
                                settingBean.setSecondPulseTime(Integer.parseInt(etAll[stepGet].getText().toString()));
                            else if (10 == stepGet)
                                settingBean.setThirdPulseTime(Integer.parseInt(etAll[stepGet].getText().toString()));
                            myHandler.sendEmptyMessage(HANDLER_FINISHED);
                            break;
                    }
                    myReceiveListener.setRcvData("");
                } else if (received.contains(" +")) {
                    myHandler.removeMessages(HANDLER_SEND_CMD);
                    received = received.replace("\r", "").replace("\n", "").substring(received.indexOf(":") + 1);
                    boolean result = false;
                    try {
                        float value = Float.parseFloat(received);
                        if (value >= 0) {
                            result = true;
                            allData[stepGet] = received;
                            if (null != etAll[stepGet])
                                etAll[stepGet].setText(received);
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                    myReceiveListener.setRcvData("");
                    if (result) {
                        if (4 == stepGet)
                            settingBean.setFirstPulseTime(Integer.parseInt(received));
                        else if (7 == stepGet)
                            settingBean.setSecondPulseTime(Integer.parseInt(received));
                        else if (10 == stepGet)
                            settingBean.setThirdPulseTime(Integer.parseInt(received));
                        if (getAllFlag)
                            stepGet++;
                        else {
                            myHandler.sendEmptyMessage(HANDLER_FINISHED);
                            return;
                        }
                    }
                    if (stepGet < AT_CMD.length)
                        myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
                    else
                        myHandler.sendEmptyMessage(HANDLER_FINISHED);
                }
            });
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        btnBus = findViewById(R.id.btn_open_bus);
        btnBus.setOnClickListener(v -> {
            enabledButton(false);
            changeMode = 5;
            myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
        });
        btnGetAll = findViewById(R.id.btn_get_all);
        btnGetAll.setOnClickListener(v -> getAllData());
    }

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case HANDLER_SEND_CMD:
                    switch (changeMode) {
                        case 1:
                            serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                            break;
                        case 5:
                            serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_OPEN_BUS, busOpened ? 0 : 1));
                            break;
                        case 7:
                            serialPortUtil.sendCmd(SerialCommand.CMD_SAVE_SETTINGS);
                            break;
                        default:
                            if (stepGet < AT_CMD.length) {
                                if (setFlag)
                                    serialPortUtil.sendCmd(AT_CMD[stepGet].replace("?", etAll[stepGet].getText()));
                                else
                                    serialPortUtil.sendCmd(AT_CMD[stepGet]);
                            }
                    }
                    myHandler.sendEmptyMessageDelayed(HANDLER_SEND_CMD, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
                case HANDLER_FINISHED:
                    myHandler.removeMessages(HANDLER_SEND_CMD);
                    if (6 == changeMode) {
                        busOpened = !busOpened;
                        btnBus.setText(busOpened ? R.string.button_close_bus : R.string.button_open_bus);
                    } else
                        myApp.myToast(PropsSettingsActivity.this, setFlag ? R.string.message_props_set_success : R.string.message_props_get_success);
                    enabledButton(true);
                    break;
            }
            return false;
        }
    });

    private void getAllData() {
        enabledButton(false);
        stepGet = 0;
        getAllFlag = true;
        setFlag = false;
        changeMode = 2;
        myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
    }

    private void initPager() {
        tabList = findViewById(R.id.tab_title);
        pagerList = findViewById(R.id.view_pager);
        fragments = new ArrayList<>();
        int[] layoutId = new int[]{R.layout.layout_props_page1, R.layout.layout_props_page2, R.layout.layout_props_page3, R.layout.layout_props_page4, R.layout.layout_props_page5};

        for (int i = 0; i < layoutId.length; i++) {
            PropsFragment fragment = new PropsFragment(layoutId[i], this, i);
            tabList.addTab(tabList.newTab());
            fragments.add(fragment);
        }
        pagerList.setAdapter(new ListPagerAdapter(getSupportFragmentManager()));
        tabList.setupWithViewPager(pagerList);
        resetTabTitle(true);
        tabList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                resetTabTitle(false);
                pagerList.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public void fillElement(View view, int page) {
        int[][] etList = new int[][]{
                {R.id.et_wv, R.id.et_dac, R.id.et_current},
                {R.id.et_bspv1, R.id.et_bspw1, R.id.et_bbpw1},
                {R.id.et_bspv2, R.id.et_bspw2, R.id.et_bbpw2},
                {R.id.et_bspv3, R.id.et_bspw3},
                {R.id.et_lpw, R.id.et_bit0r, R.id.et_bit1r},
        };
        int[][][] btnList = new int[][][]{
                {{R.id.btn_set_wv, R.id.btn_set_dac, R.id.btn_set_current}, {R.id.btn_get_wv, R.id.btn_get_dac, R.id.btn_get_current}},
                {{R.id.btn_set_bspv1, R.id.btn_set_bspw1, R.id.btn_set_bbpw1}, {R.id.btn_get_bspv1, R.id.btn_get_bspw1, R.id.btn_get_bbpw1}},
                {{R.id.btn_set_bspv2, R.id.btn_set_bspw2, R.id.btn_set_bbpw2}, {R.id.btn_get_bspv2, R.id.btn_get_bspw2, R.id.btn_get_bbpw2}},
                {{R.id.btn_set_bspv3, R.id.btn_set_bspw3}, {R.id.btn_get_bspv3, R.id.btn_get_bspw3}},
                {{R.id.btn_set_lpw, R.id.btn_set_bit0r, R.id.btn_set_bit1r}, {R.id.btn_get_lpw, R.id.btn_get_bit0r, R.id.btn_get_bit1r}}
        };
        int n = 0;
        for (int i = 0; i < page; i++)
            n += etList[i].length;
        for (int j = 0; j < etList[page].length; j++) {
            etAll[n + j] = view.findViewById(etList[page][j]);
            if (null != allData[n + j] && etAll[n + j].getText().toString().isEmpty())
                etAll[n + j].setText(allData[n + j]);
            btnAll[0][n + j] = view.findViewById(btnList[page][0][j]);
            btnAll[0][n + j].setOnClickListener(v -> {
                for (int k = 0; k < btnAll[0].length; k++) {
                    if (null != btnAll[0][k]) {
                        if (v.getId() == btnAll[0][k].getId()) {
                            try {
                                if (Float.parseFloat(etAll[k].getText().toString()) >= 0) {
                                    enabledButton(false);
                                    getAllFlag = false;
                                    stepGet = k;
                                    setFlag = true;
                                    changeMode = 3;
                                    myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
                                }
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            break;
                        }
                    }
                }
            });
            btnAll[1][n + j] = view.findViewById(btnList[page][1][j]);
            btnAll[1][n + j].setOnClickListener(v -> {
                for (int k = 0; k < btnAll[1].length; k++) {
                    if (null != btnAll[1][k]) {
                        if (v.getId() == btnAll[1][k].getId()) {
                            enabledButton(false);
                            getAllFlag = false;
                            stepGet = k;
                            setFlag = false;
                            changeMode = 2;
                            myHandler.sendEmptyMessage(HANDLER_SEND_CMD);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void enabledButton(boolean enabled) {
        setProgressVisibility(!enabled);
        if (busOpened)
            myReceiveListener.setStartAutoDetect(enabled);
        for (int k = 0; k < btnAll[0].length; k++) {
            if (null != btnAll[0][k])
                btnAll[0][k].setEnabled(enabled);
            if (null != btnAll[1][k])
                btnAll[1][k].setEnabled(enabled);
            if (null != etAll[k])
                etAll[k].setEnabled(enabled);
        }
        btnBus.setEnabled(enabled);
        btnGetAll.setEnabled(enabled);
    }

    private void resetTabTitle(boolean init) {
        final int[] title = {R.string.tab_title_props_page1, R.string.tab_title_props_page2, R.string.tab_title_props_page3, R.string.tab_title_props_page4, R.string.tab_title_props_page5};
        for (int i = 0; i < title.length; i++) {
            TextView tv = (TextView) LayoutInflater.from(PropsSettingsActivity.this).inflate(R.layout.layout_tab_view, tabList, false);
            tv.setText(title[i]);
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

    @Override
    protected void onDestroy() {
        myApp.saveSettings(settingBean);
        serialPortUtil.closeSerialPort();
        myReceiveListener.closeAllHandler();
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