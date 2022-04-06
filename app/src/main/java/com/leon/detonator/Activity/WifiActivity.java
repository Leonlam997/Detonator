package com.leon.detonator.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.leon.detonator.Adapter.WifiListAdapter;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.WifiListBean;
import com.leon.detonator.R;
import com.leon.detonator.Util.KeyUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class WifiActivity extends BaseActivity {
    private final int WIFI_SECURE_NO = 1;
    private final int WIFI_SECURE_WEP = 2;
    private final int WIFI_SECURE_WPA = 3;
    private int scanCount, index, lastTouchX, lastPosition;
    private List<WifiListBean> list;
    private WifiListAdapter adapter;
    private boolean isStop;
    private boolean isConnecting;
    private RssiBroadcast broadcast;
    private WifiManager wm;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode() && null != result.getData()) {
            WifiListBean bean = list.get(index);
            String psw = result.getData().getStringExtra(KeyUtils.KEY_WIFI_CONNECT_PASSWORD);
            int netID = wm.addNetwork(createWifiInfo(bean.getSSID(), psw, (bean.getCapabilities().contains("WPA") || bean.getCapabilities().contains("wpa")) ? WIFI_SECURE_WPA
                    : (bean.getCapabilities().contains("WEP") || bean.getCapabilities().contains("wep") ? WIFI_SECURE_WEP : WIFI_SECURE_NO)));
            bean.setConnecting(true);
            adapter.updateList(list);
            new ConnectWifiThread(netID).start();
        }
    });
    private final Handler refreshHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (msg.what != 1000)
                initData();
            adapter.updateList(list);
            if (msg.what == WifiManager.WIFI_STATE_ENABLED) {
                isStop = false;
                scanCount = 0;
                new ScanWifiThread().start();
            }
            return false;
        }
    });
    private BaseApplication myApp;
    private PopupWindow popupMenu;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        setTitle(R.string.settings_wifi);
        myApp = (BaseApplication) getApplication();
        wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        initData();
        ListView lvWifiList = findViewById(R.id.lv_wifi);
        adapter = new WifiListAdapter(this, list);
        adapter.setOnButtonClickListener(which -> {
            WifiListBean bean;
            switch (which) {
                case 0:
                    bean = list.get(0);
                    bean.setEnabled(false);
                    bean.setChangingStatus(true);
                    list.set(0, bean);
                    Iterator<WifiListBean> it = list.iterator();
                    if (it.hasNext())
                        it.next();
                    while (it.hasNext()) {
                        it.next();
                        it.remove();
                    }
                    isStop = true;
                    adapter.updateList(list);
//                    openGPS(false);
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
//                    openGPS(true);
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
                    if (position == 1) {
                        menu = new String[]{getResources().getString(R.string.menu_wifi_forget),
                                getResources().getString(R.string.menu_wifi_modify)};
                    } else {
                        WifiListBean bean = list.get(position);
                        if (isExist(bean.getSSID()) != null) {
                            menu = new String[]{getResources().getString(R.string.menu_wifi_connect),
                                    getResources().getString(R.string.menu_wifi_forget),
                                    getResources().getString(R.string.menu_wifi_modify)};
                        } else {
                            menu = new String[]{getResources().getString(R.string.menu_wifi_connect)};
                        }
                    }
                    View popupView = WifiActivity.this.getLayoutInflater().inflate(R.layout.layout_popupwindow, parent, false);
                    ListView lsvMore = popupView.findViewById(R.id.lvPopupMenu);
                    lastPosition = position;
                    ((TextView) popupView.findViewById(R.id.tvTitle)).setText(list.get(position).getSSID());
                    lsvMore.setAdapter(new ArrayAdapter<>(WifiActivity.this, R.layout.layout_popupwindow_menu, menu));
                    lsvMore.setOnItemClickListener((parent1, view1, position1, id1) -> {
                        String title = ((TextView) view1).getText().toString();
                        String ssid = list.get(lastPosition).getSSID();
                        if (getResources().getString(R.string.menu_wifi_modify).equals(title)) {
                            Intent intent = new Intent();
                            intent.setClass(WifiActivity.this, WifiConnectActivity.class);
                            intent.putExtra(KeyUtils.KEY_WIFI_CONNECT_SSID, ssid);
                            index = lastPosition;
                            launcher.launch(intent);
                        } else if (getResources().getString(R.string.menu_wifi_forget).equals(title)) {
                            WifiConfiguration tempConfig = isExist(ssid);
                            if (tempConfig != null) {
                                wm.removeNetwork(tempConfig.networkId);
                                wm.saveConfiguration();
                                scanCount = 0;
                                if (isStop) {
                                    isStop = false;
                                    new ScanWifiThread().start();
                                }
                            }
                        } else if (getResources().getString(R.string.menu_wifi_connect).equals(title)) {
                            WifiListBean bean = list.get(lastPosition);
                            if (bean.isSaved()) {
                                bean.setConnecting(true);
                                adapter.updateList(list);
                                WifiConfiguration tempConfig = isExist(ssid);
                                if (null != tempConfig)
                                    new ConnectWifiThread(tempConfig.networkId).start();
                            } else {
                                Intent intent = new Intent();
                                intent.setClass(WifiActivity.this, WifiConnectActivity.class);
                                intent.putExtra(KeyUtils.KEY_WIFI_CONNECT_SSID, ssid);
                                index = lastPosition;
                                launcher.launch(intent);
                            }
                        }
                        popupMenu.dismiss();
                    });

                    popupMenu = new PopupWindow(popupView, 150, 40 * (menu.length + 1));
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
                    WifiListBean bean = list.get(position);
                    WifiConfiguration config = isExist(bean.getSSID());
                    if (config == null) {
                        if (bean.isEncrypted()) {
                            Intent intent = new Intent();
                            intent.setClass(WifiActivity.this, WifiConnectActivity.class);
                            intent.putExtra(KeyUtils.KEY_WIFI_CONNECT_SSID, bean.getSSID());
                            index = position;
                            launcher.launch(intent);
                        } else {
                            bean.setConnecting(true);
                            adapter.updateList(list);
                            new ConnectWifiThread(wm.addNetwork(createWifiInfo(bean.getSSID(), "", WIFI_SECURE_NO))).start();
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

    public WifiConfiguration createWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();
        configuration.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = isExist(SSID);
        if (tempConfig != null) {
            wm.removeNetwork(tempConfig.networkId);
        }

        switch (Type) {
            case WIFI_SECURE_NO://不加密
                configuration.wepKeys[0] = "";
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                configuration.wepTxKeyIndex = 0;
                configuration.priority = 20000;
                break;
            case WIFI_SECURE_WEP://wep加密
                configuration.hiddenSSID = true;
                configuration.wepKeys[0] = "\"" + Password + "\"";
                configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case WIFI_SECURE_WPA: //wpa加密
                configuration.preSharedKey = "\"" + Password + "\"";
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

    private WifiConfiguration isExist(String ssid) {
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(WifiActivity.this, Manifest.permission.ACCESS_WIFI_STATE)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(WifiActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            List<WifiConfiguration> configs = wm.getConfiguredNetworks();
            if (configs != null)
                for (WifiConfiguration config : configs) {
                    if (config.SSID.equals("\"" + ssid + "\"")) {
                        return config;
                    }
                }
        }
        return null;
    }

    private void initData() {
        if (list == null)
            list = new ArrayList<>();
        else
            list.clear();
        WifiListBean bean = new WifiListBean();
        bean.setSSID("Wifi");
        bean.setSignalLevel(100);
        if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            bean.setEnabled(true);
            list.add(bean);

            connectedWifiList();

            bean = new WifiListBean();
            bean.setSignalLevel(80);
            bean.setSSID(getResources().getString(R.string.wifi_available_list));
            bean.setRescanLine(true);
            bean.setScanning(true);
        }
        list.add(bean);
    }

    private int connectedWifiList() {
        WifiInfo wi = wm.getConnectionInfo();
        boolean isConnected = false;
        WifiListBean bean;
        String ssid = wi.getSSID();
        int hasChanged = 0;

        if (list.size() > 1) {
            if (list.get(1).isConnected())
                isConnected = true;
        }
        if (ssid.startsWith("\"") && ssid.endsWith("\""))
            ssid = ssid.substring(1, ssid.length() - 1);
        final boolean b = ssid.length() > 0 && !"0x".equals(ssid) && !"<unknown ssid>".equals(ssid);
        if (isConnected) {
            if (b) {
                bean = list.get(1);
                int level = WifiManager.calculateSignalLevel(wi.getRssi(), 4);
                if (bean.getSignalLevel() != level) {
                    bean.setSignalLevel(level);
                    hasChanged = 1;
                }

                if (!ssid.equals(bean.getSSID())) {
                    bean.setSSID(ssid);
                    hasChanged = 2;
                }
                list.set(1, bean);
            } else {
                list.remove(1);
            }
        } else {
            if (b) {
                bean = new WifiListBean(wi.getSSID(), wi.getBSSID(), "", WifiManager.calculateSignalLevel(wi.getRssi(), 4), false);
                bean.setConnected(true);
                bean.setSSID(ssid);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return hasChanged;
                }
                List<WifiConfiguration> wifiConfiguration = wm.getConfiguredNetworks();
                for (WifiConfiguration configuration : wifiConfiguration) {
                    if (configuration != null && configuration.status == WifiConfiguration.Status.CURRENT) {
                        if (TextUtils.isEmpty(wi.getSSID()) || wi.getSSID().equalsIgnoreCase(configuration.SSID)) {
                            //KeyMgmt.NONE表示无需密码
                            bean.setEncrypted(!configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
                            break;
                        }
                    }
                }
                list.add(1, bean);
                hasChanged = 2;
            }
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
                        List<WifiListBean> tempList = new ArrayList<>(list);

                        if (tempList.size() > count + 1) {
                            tempList.subList(count + 1, tempList.size()).clear();
                        }
                        List<ScanResult> scanResults = wm.getScanResults();
                        String ssid = wi.getSSID();
                        WifiListBean bean;
                        bean = tempList.get(count);
                        bean.setScanning(true);
                        tempList.set(count, bean);
                        if (ssid.startsWith("\"") && ssid.endsWith("\""))
                            ssid = ssid.substring(1, ssid.length() - 1);

                        for (ScanResult sr : scanResults) {
                            if (!sr.SSID.equals(ssid) && !sr.SSID.isEmpty()) {
                                boolean encrypted = false;
                                if (!TextUtils.isEmpty(sr.capabilities)) {
                                    encrypted = (sr.capabilities.contains("WPA")
                                            || sr.capabilities.contains("wpa")
                                            || sr.capabilities.contains("WEP")
                                            || sr.capabilities.contains("wep"));
                                }
                                bean = new WifiListBean(sr.SSID, sr.BSSID, sr.capabilities, WifiManager.calculateSignalLevel(sr.level, 4), encrypted);
                                bean.setSaved(isExist(sr.SSID) != null);
                                tempList.add(bean);
                            }
                        }
                        Collections.sort(tempList);
                        int SCANTIMES = 5;
                        if (scanCount >= SCANTIMES) {
                            bean = tempList.get(count);
                            bean.setScanning(false);
                            tempList.set(count, bean);
                        }

                        list.clear();
                        list.addAll(tempList);
                        refreshHandler.sendEmptyMessage(1000);

                        if (scanCount++ > SCANTIMES) {
                            isStop = true;
                            interrupt();
                        } else {
                            try {
                                Thread.sleep(1000);
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
                    refreshHandler.sendEmptyMessage(status);
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
                if (!wm.enableNetwork(networkId, true)) {
                    runOnUiThread(() -> myApp.myToast(WifiActivity.this, R.string.wifi_check_password));
                }
                if (!wm.reconnect()) {
                    runOnUiThread(() -> myApp.myToast(WifiActivity.this, R.string.wifi_reconnect_fail));
                }
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
                if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                    myApp.myToast(WifiActivity.this, R.string.wifi_password_error);
                }
            }
        }
    }
}
