package com.leon.detonator.BluetoothTool;

import android.os.ParcelUuid;

public class BluetoothBean {
    public final static String MY_UUID = "42BCBC2C-40E7-414B-B08F-E9B0FF25011B";

    private String name;
    private String address;
    private int type;
    private int deviceType;
    private boolean connected;
    private ParcelUuid[] uuid;

    public static String getMyUuid() {
        return MY_UUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public ParcelUuid[] getUuid() {
        return uuid;
    }

    public void setUuid(ParcelUuid[] uuid) {
        this.uuid = uuid;
    }

}
