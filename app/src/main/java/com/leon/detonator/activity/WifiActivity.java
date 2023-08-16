package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.adapter.WifiListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.WifiBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@SuppressLint("MissingPermission")
public class WifiActivity extends BaseActivity {
    private List<WifiBean> list;
    private WifiListAdapter adapter;
    private RssiBroadcast broadcast;
    private PopupWindow popupMenu;
    private WifiManager wm;
    private final int WIFI_SECURE_NO = 1;
    private final int WIFI_SECURE_WEP = 2;
    private final int WIFI_SECURE_WPA = 3;
    private boolean isConnecting;
    private boolean isStop;
    private int lastPosition;
    private int lastTouchX;
    private int scanCount;
    private final Handler myHandler = new Handler(msg -> {
        adapter.updateList(list);
        if (msg.what == WifiManager.WIFI_STATE_ENABLED) {
            isStop = false;
            scanCount = 0;
            new ScanWifiThread().start();
        }
        return false;
    });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        setTitle(R.string.settings_wifi);
        wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        initData();
        ListView lvWifiList = findViewById(R.id.lv_wifi);
        adapter = new WifiListAdapter(this, list, which -> {
            WifiBean bean;
            switch (which) {
                case 0:
                    bean = list.get(0);
                    bean.setEnabled(false);
                    bean.setChangingStatus(true);
                    list.set(0, bean);
                    Iterator<WifiBean> it = list.iterator();
                    if (it.hasNext())
                        it.next();
                    while (it.hasNext()) {
                        it.next();
                        it.remove();
                    }
                    isStop = true;
                    adapter.updateList(list);
                    wm.setWifiEnabled(false);
                    new WaitForStateThread(false).start();
                    break;
                case 1:
                    bean = list.get(0);
                    bean.setEnabled(true);
                    bean.setChangingStatus(true);
                    list.set(0, bean);
                    adapter.updateList(list);
                    isStop = true;
                    wm.setWifiEnabled(true);
                    new WaitForStateThread(true).start();
                    break;
                case 2:
                    isStop = false;
                    scanCount = 0;
                    new ScanWifiThread().start();
                    break;
                default:
                    break;
            }
        });
        lvWifiList.setAdapter(adapter);
        lvWifiList.setOnTouchListener((v, event) -> {
            lastTouchX = (int) event.getX();
            if (MotionEvent.ACTION_UP == event.getAction())
                v.performClick();
            return false;
        });
        lvWifiList.setOnItemLongClickListener((parent, view, position, id) -> {
            if (list.size() > 1) {
                if (position > (list.get(1).isConnected() ? 2 : 1) || (list.get(1).isConnected() && position == 1)) {
                    String[] menu;
                    if (position == 1)
                        menu = new String[]{getString(R.string.menu_wifi_forget), getString(R.string.menu_wifi_modify)};
                    else {
                        WifiBean bean = list.get(position);
                        if (isExist(bean.getSSID()) != null)
                            menu = new String[]{getString(R.string.menu_wifi_connect), getString(R.string.menu_wifi_forget), getString(R.string.menu_wifi_modify)};
                        else
                            menu = new String[]{getString(R.string.menu_wifi_connect)};
                    }
                    View popupView = WifiActivity.this.getLayoutInflater().inflate(R.layout.layout_popup_window, parent, false);
                    ListView lsvMore = popupView.findViewById(R.id.lvPopupMenu);
                    lastPosition = position;
                    ((TextView) popupView.findViewById(R.id.tvTitle)).setText(list.get(position).getSSID());
                    lsvMore.setAdapter(new ArrayAdapter<>(WifiActivity.this, R.layout.layout_item_popup_window, menu));
                    lsvMore.setOnItemClickListener((parent1, view1, position1, id1) -> {
                        String title = ((TextView) view1).getText().toString();
                        if (getString(R.string.menu_wifi_modify).equals(title))
                            showPasswordDialog(true);
                        else if (getString(R.string.menu_wifi_forget).equals(title)) {
                            WifiConfiguration tempConfig = isExist(list.get(lastPosition).getSSID());
                            if (tempConfig != null) {
                                wm.removeNetwork(tempConfig.networkId);
                                wm.saveConfiguration();
                                scanCount = 0;
                                if (isStop) {
                                    isStop = false;
                                    new ScanWifiThread().start();
                                }
                            }
                        } else if (getString(R.string.menu_wifi_connect).equals(title)) {
                            WifiBean bean = list.get(lastPosition);
                            if (bean.isSaved()) {
                                bean.setConnecting(true);
                                adapter.updateList(list);
                                WifiConfiguration tempConfig = isExist(bean.getSSID());
                                if (null != tempConfig)
                                    new ConnectWifiThread(tempConfig.networkId).start();
                            } else
                                showPasswordDialog(false);
                        }
                        popupMenu.dismiss();
                    });
                    popupMenu = new PopupWindow(popupView, 150 + BaseApplication.settings.getFontScale() * 10, 40 * (menu.length + 1));
                    popupMenu.setAnimationStyle(R.style.popup_window_anim);
                    popupMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    popupMenu.setFocusable(true);
                    popupMenu.setOutsideTouchable(true);
                    popupMenu.update();
                    popupMenu.showAsDropDown(view, lastTouchX > 75 ? lastTouchX - 75 : 0, 0);
                }
            }
            return true;
        });
        lvWifiList.setOnItemClickListener((parent, view, position, id) -> {
            if (list.size() > 1 && !isConnecting) {
                if (position > (list.get(1).isConnected() ? 2 : 1)) {
                    if (!isStop)
                        isStop = true;
                    WifiBean bean = list.get(position);
                    WifiConfiguration config = isExist(bean.getSSID());
                    lastPosition = position;
                    if (config == null) {
                        if (bean.isEncrypted())
                            showPasswordDialog(false);
                        else {
                            bean.setConnecting(true);
                            adapter.updateList(list);
                            new ConnectWifiThread(wm.addNetwork(createWifiInfo(bean.getSSID(), null, WIFI_SECURE_NO))).start();
                        }
                    } else {
                        bean.setConnecting(true);
                        adapter.updateList(list);
                        new ConnectWifiThread(config.networkId).start();
                    }
                }
            }
        });

        isConnecting = false;
        isStop = false;
        if (list.size() > 1) {
            scanCount = 0;
            new ScanWifiThread().start();
        }
        broadcast = new RssiBroadcast();
        IntentFilter ifrssi = new IntentFilter();
        ifrssi.addAction(WifiManager.RSSI_CHANGED_ACTION);
        ifrssi.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        ifrssi.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(broadcast, ifrssi);
    }

    public WifiConfiguration createWifiInfo(String ssid, String password, int type) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();
        configuration.SSID = "\"" + ssid + "\"";

        WifiConfiguration tempConfig = isExist(ssid);
        if (tempConfig != null)
            wm.removeNetwork(tempConfig.networkId);
        switch (type) {
            case WIFI_SECURE_NO://不加密
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case WIFI_SECURE_WEP://wep加密
                configuration.hiddenSSID = true;
                configuration.wepKeys[0] = "\"" + password + "\"";
                configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case WIFI_SECURE_WPA: //wpa加密
                configuration.preSharedKey = "\"" + password + "\"";
                configuration.hiddenSSID = true;
                configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                configuration.status = WifiConfiguration.Status.ENABLED;
                break;
        }
        return configuration;
    }

    private void showPasswordDialog(boolean modify) {
        runOnUiThread(() -> {
            final View v = LayoutInflater.from(WifiActivity.this).inflate(R.layout.layout_dialog_edit, null, false);
            final EditText etPassword = v.findViewById(R.id.et_dialog);
            v.findViewById(R.id.tv_dialog).setVisibility(View.INVISIBLE);
            v.findViewById(R.id.cb_dispose).setVisibility(View.VISIBLE);
            ((CheckBox) v.findViewById(R.id.cb_dispose)).setOnCheckedChangeListener((buttonView, isChecked) -> {
                etPassword.setInputType(isChecked ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) :
                        (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
                etPassword.setSelection(etPassword.getText().length());
            });
            etPassword.setHint(R.string.hint_input_password);
            etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
            BaseApplication.customDialog(new AlertDialog.Builder(WifiActivity.this, R.style.AlertDialog)
                    .setTitle(String.format(getString(R.string.dialog_title_input_password), list.get(lastPosition).getSSID()))
                    .setView(v)
                    .setCancelable(false)
                    .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                        if (etPassword.getText() != null && !etPassword.getText().toString().isEmpty()) {
                            WifiBean bean = list.get(lastPosition);
                            int netID;
                            if (modify) {
                                WifiConfiguration configuration = isExist(list.get(lastPosition).getSSID());
                                if (configuration == null)
                                    return;
                                if (bean.getCapabilities().toUpperCase().contains("WPA"))
                                    configuration.wepKeys[0] = "\"" + etPassword.getText().toString() + "\"";
                                else
                                    configuration.preSharedKey = "\"" + etPassword.getText().toString() + "\"";
                                netID = wm.updateNetwork(configuration);
                            } else
                                netID = wm.addNetwork(createWifiInfo(bean.getSSID(), etPassword.getText().toString(), (bean.getCapabilities().toUpperCase().contains("WPA")) ? WIFI_SECURE_WPA : WIFI_SECURE_WEP));
                            if (lastPosition != 1)
                                bean.setConnecting(true);
                            adapter.updateList(list);
                            new ConnectWifiThread(netID).start();
                        } else
                            myApp.myToast(WifiActivity.this, R.string.message_name_input_error);
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show(), false);
        });
    }

    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();
        if (configs != null)
            for (WifiConfiguration config : configs)
                if (config.SSID.equals("\"" + ssid + "\""))
                    return config;
        return null;
    }

    private void initData() {
        if (list == null)
            list = new ArrayList<>();
        else
            list.clear();
        WifiBean bean = new WifiBean();
        bean.setSSID("Wifi");
        bean.setSignalLevel(WifiBean.ITEM_WIFI);
        if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            bean.setEnabled(true);
            list.add(bean);
            connectedWifiList();
            bean = new WifiBean();
            bean.setSignalLevel(WifiBean.ITEM_AVAILABLE);
            bean.setSSID(getString(R.string.wifi_available_list));
            bean.setRescanLine(true);
            bean.setScanning(true);
        }
        list.add(bean);
    }

    private int connectedWifiList() {
        WifiInfo wifiInfo = wm.getConnectionInfo();
        WifiBean bean;
        String ssid = wifiInfo.getSSID();
        int hasChanged = 0;
        if (ssid.startsWith("\"") && ssid.endsWith("\""))
            ssid = ssid.substring(1, ssid.length() - 1);
        final boolean b = ssid.length() > 0 && !"0x".equals(ssid) && !"<unknown ssid>".equals(ssid);
        if (list.size() > 1 && list.get(1).isConnected()) {
            if (b) {
                bean = list.get(1);
                int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4);
                if (bean.getSignalLevel() != level) {
                    bean.setSignalLevel(level);
                    hasChanged = 1;
                }
                if (!ssid.equals(bean.getSSID())) {
                    bean.setSSID(ssid);
                    hasChanged = 2;
                }
            } else
                list.remove(1);
        } else if (b) {
            bean = new WifiBean(wifiInfo.getSSID(), wifiInfo.getBSSID(), "", WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4), false);
            bean.setConnected(true);
            bean.setSSID(ssid);
            List<WifiConfiguration> wifiConfiguration = wm.getConfiguredNetworks();
            for (WifiConfiguration configuration : wifiConfiguration) {
                if (configuration != null && configuration.status == WifiConfiguration.Status.CURRENT) {
                    if (TextUtils.isEmpty(wifiInfo.getSSID()) || wifiInfo.getSSID().equalsIgnoreCase(configuration.SSID)) {
                        //KeyMgmt.NONE表示无需密码
                        bean.setEncrypted(!configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
                        break;
                    }
                }
            }
            list.add(1, bean);
            hasChanged = 2;
        }
        return hasChanged;
    }

    protected void onDestroy() {
        isStop = true;
        unregisterReceiver(broadcast);
        super.onDestroy();
    }

    private class ScanWifiThread extends Thread {
        @Override
        public void run() {
            super.run();
            synchronized (this) {
                while (!isStop && !isInterrupted()) {
                    if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED && list.size() > 1) {
                        WifiInfo wi = wm.getConnectionInfo();
                        connectedWifiList();
                        int count = list.get(1).isConnected() ? 2 : 1;
                        List<WifiBean> tempList = new ArrayList<>(list);
                        if (tempList.size() > count + 1)
                            tempList.subList(count + 1, tempList.size()).clear();
                        List<ScanResult> scanResults = wm.getScanResults();
                        String ssid = wi.getSSID();
                        WifiBean bean;
                        bean = tempList.get(count);
                        bean.setScanning(true);
                        tempList.set(count, bean);
                        if (ssid.startsWith("\"") && ssid.endsWith("\""))
                            ssid = ssid.substring(1, ssid.length() - 1);
                        for (ScanResult sr : scanResults) {
                            if (!sr.SSID.equals(ssid) && !sr.SSID.isEmpty()) {
                                boolean encrypted = false;
                                if (!TextUtils.isEmpty(sr.capabilities)) {
                                    encrypted = (sr.capabilities.toUpperCase().contains("WPA")
                                            || sr.capabilities.toUpperCase().contains("WEP"));
                                }
                                bean = new WifiBean(sr.SSID, sr.BSSID, sr.capabilities, WifiManager.calculateSignalLevel(sr.level, 4), encrypted);
                                bean.setSaved(isExist(sr.SSID) != null);
                                tempList.add(bean);
                            }
                        }
                        Collections.sort(tempList);
                        final int scanTimes = 10;
                        if (scanCount >= scanTimes) {
                            bean = tempList.get(count);
                            bean.setScanning(false);
                            tempList.set(count, bean);
                        }
                        list.clear();
                        list.addAll(tempList);
                        myHandler.sendEmptyMessage(100);
                        if (scanCount++ > scanTimes) {
                            isStop = true;
                            interrupt();
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        }
                    }
                }
            }
        }
    }

    private class WaitForStateThread extends Thread {
        private final int status;

        public WaitForStateThread(boolean Enabled) {
            status = Enabled ? WifiManager.WIFI_STATE_ENABLED : WifiManager.WIFI_STATE_DISABLED;
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                if (wm.getWifiState() == status) {
                    initData();
                    if (WifiManager.WIFI_STATE_ENABLED == status)
                        myHandler.sendEmptyMessageDelayed(status, 3000);
                    else
                        myHandler.sendEmptyMessage(status);
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
        }
    }

    private class ConnectWifiThread extends Thread {
        private final int networkId;

        public ConnectWifiThread(int netID) {
            networkId = netID;
        }

        @Override
        public void run() {
            super.run();
            while (isConnecting) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
            isConnecting = true;
            try {
                if (!wm.enableNetwork(networkId, true))
                    runOnUiThread(() -> myApp.myToast(WifiActivity.this, R.string.wifi_check_password));
                if (!wm.reconnect())
                    runOnUiThread(() -> myApp.myToast(WifiActivity.this, R.string.wifi_reconnect_fail));
                scanCount = 0;
                if (isStop) {
                    isStop = false;
                    new ScanWifiThread().start();
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            isConnecting = false;
        }

    }

    private class RssiBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                int changed = connectedWifiList();
                if (changed != 0) {
                    adapter.updateList(list);
                    if (changed == 2) {
                        scanCount = 0;
                        if (isStop) {
                            isStop = false;
                            new ScanWifiThread().start();
                        }
                    }
                }
            }
            if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING)
                    myApp.myToast(WifiActivity.this, R.string.wifi_password_error);
            }
        }
    }
}
