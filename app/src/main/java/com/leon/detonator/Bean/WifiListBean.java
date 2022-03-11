package com.leon.detonator.Bean;


import androidx.annotation.NonNull;

/**
 * Created by Leon on 2018/3/20.
 */

public class WifiListBean implements Comparable<WifiListBean> {
    private String ssid;    //WIFI名称
    private String mac; //WIFI MAC地址
    private String capabilities;    //WIFI描述
    private int signalLevel;    //信号强度
    private boolean enabled;    //是否打开WIFI
    private boolean encrypted;  //是否加密
    private boolean connected;  //是否已连接
    private boolean rescanLine; //是否显示可用列表标识行
    private boolean scanning;   //是否正在扫描
    private boolean changingStatus; //是否正在打开关闭WIFI
    private boolean connecting; //是否正在连接
    private boolean saved;  //是否为已保存WIFI

    public WifiListBean() {
        enabled = false;
        encrypted = false;
        connected = false;
        rescanLine = false;
        scanning = false;
        changingStatus = false;
        connecting = false;
        saved = false;
        ssid = "";
        mac = "";
        capabilities = "";
        signalLevel = 0;
    }

    public WifiListBean(String ssid, String mac, String capabilities, int signalLevel, boolean encrypted) {
        enabled = false;
        connected = false;
        rescanLine = false;
        scanning = false;
        changingStatus = false;
        connecting = false;
        saved = false;
        this.encrypted = encrypted;
        this.ssid = ssid;
        this.mac = mac;
        this.signalLevel = signalLevel;
        this.capabilities = capabilities;
    }

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
    }

    public String getMAC() {
        return mac;
    }

    public void setMAC(String mac) {
        this.mac = mac;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public int getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(int signalLevel) {
        this.signalLevel = signalLevel;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRescanLine() {
        return rescanLine;
    }

    public void setRescanLine(boolean line) {
        this.rescanLine = line;
    }

    public boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
    }

    public boolean isChangingStatus() {
        return changingStatus;
    }

    public void setChangingStatus(boolean changingStatus) {
        this.changingStatus = changingStatus;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    @Override
    public int compareTo(@NonNull WifiListBean o) {
        if (this.isConnected())
            return o.getSignalLevel() - 90;
        if (o.isConnected())
            return 90 - this.signalLevel;

        if (this.signalLevel + (this.saved ? 50 : 0) != o.getSignalLevel() + (o.isSaved() ? 50 : 0))
            return o.getSignalLevel() + (o.isSaved() ? 50 : 0) - (this.signalLevel + (this.saved ? 50 : 0));
        return this.ssid.compareTo(o.getSSID());
    }
}
