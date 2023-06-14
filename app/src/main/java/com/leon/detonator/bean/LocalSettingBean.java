package com.leon.detonator.bean;

import java.util.Map;

public class LocalSettingBean {
    private String serialNum;
    private String delayTime;
    private String delayPeriod;
    private String mtMac;
    private String exploderID;
    private String IMEI;
    private int row;
    private int hole;
    private int holeInside;
    private int section;
    private int sectionInside;
    private int userID;
    private int serverHost;
    private boolean registered;
    private boolean uploadedLog;
    private boolean tunnel;
    private float chargeVoltage;
    private float workVoltage;
    private double latitude;
    private double longitude;
    private boolean scanMode;
    private Map<Float, Integer> dacMap;

    public LocalSettingBean() {
        row = 50;
        hole = 10;
        section = 50;
        serverHost = 1;
    }

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }

    public String getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(String delayTime) {
        this.delayTime = delayTime;
    }

    public String getDelayPeriod() {
        return delayPeriod;
    }

    public void setDelayPeriod(String delayPeriod) {
        this.delayPeriod = delayPeriod;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getHole() {
        return hole;
    }

    public void setHole(int hole) {
        this.hole = hole;
    }

    public int getHoleInside() {
        return holeInside;
    }

    public void setHoleInside(int holeInside) {
        this.holeInside = holeInside;
    }

    public int getSection() {
        return section;
    }

    public void setSection(int section) {
        this.section = section;
    }

    public int getSectionInside() {
        return sectionInside;
    }

    public void setSectionInside(int sectionInside) {
        this.sectionInside = sectionInside;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public String getMtMac() {
        return mtMac;
    }

    public void setMtMac(String mtMac) {
        this.mtMac = mtMac;
    }

    public String getExploderID() {
        return exploderID;
    }

    public void setExploderID(String exploderID) {
        this.exploderID = exploderID;
    }

    public int getServerHost() {
        return serverHost;
    }

    public void setServerHost(int serverHost) {
        this.serverHost = serverHost;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getIMEI() {
        return IMEI;
    }

    public void setIMEI(String IMEI) {
        this.IMEI = IMEI;
    }

    public boolean isUploadedLog() {
        return uploadedLog;
    }

    public void setUploadedLog(boolean uploadedLog) {
        this.uploadedLog = uploadedLog;
    }

    public float getChargeVoltage() {
        return chargeVoltage;
    }

    public void setChargeVoltage(float chargeVoltage) {
        this.chargeVoltage = chargeVoltage;
    }

    public boolean isTunnel() {
        return tunnel;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }

    public float getWorkVoltage() {
        return workVoltage;
    }

    public void setWorkVoltage(float workVoltage) {
        this.workVoltage = workVoltage;
    }

    public boolean isScanMode() {
        return scanMode;
    }

    public void setScanMode(boolean scanMode) {
        this.scanMode = scanMode;
    }

    public Map<Float, Integer> getDacMap() {
        return dacMap;
    }

    public void setDacMap(Map<Float, Integer> dacMap) {
        this.dacMap = dacMap;
    }
}
