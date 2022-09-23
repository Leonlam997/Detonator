package com.leon.detonator.bean;

import com.leon.detonator.bluetooth.BluetoothBean;

public class BluetoothListBean {
    private BluetoothBean bluetooth;
    private boolean enabled;    //是否打开蓝牙
    private boolean rescanLine;
    private boolean scanning;   //是否正在扫描或连接
    private boolean changingStatus; //是否正在打开关闭蓝牙

    public BluetoothListBean() {
        enabled = false;
        rescanLine = false;
        scanning = false;
        changingStatus = false;
    }

    public BluetoothBean getBluetooth() {
        return bluetooth;
    }

    public void setBluetooth(BluetoothBean bluetooth) {
        this.bluetooth = bluetooth;
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

    public void setRescanLine(boolean rescanLine) {
        this.rescanLine = rescanLine;
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
}
