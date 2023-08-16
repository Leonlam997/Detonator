package com.leon.detonator.bean;

import com.leon.detonator.util.ConstantUtils;

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
    private int defaultType = 0;
    private int userID;
    private int volume;
    private int serverHost;
    private int fontScale;
    private int firstPulseTime;
    private int secondPulseTime;
    private int thirdPulseTime;
    private boolean vibrate;
    private boolean registered;
    private boolean uploadedLog;
    private boolean updateHint;
    private float chargeVoltage;
    private double latitude;
    private double longitude;
    private Map<Float, Integer> dacMap;

    public LocalSettingBean() {
        row = 50;
        hole = 10;
        holeInside = 0;
        section = 50;
        sectionInside = 0;
        volume = ConstantUtils.MAX_VOLUME;
        latitude = 0;
        longitude = 0;
        serverHost = 1;
        firstPulseTime = 1200;
        secondPulseTime = 600;
        thirdPulseTime = 600;
        vibrate = true;
        registered = false;
    }

    public int getFirstPulseTime() {
        return firstPulseTime;
    }

    public void setFirstPulseTime(int firstPulseTime) {
        this.firstPulseTime = firstPulseTime;
    }

    public int getSecondPulseTime() {
        return secondPulseTime;
    }

    public void setSecondPulseTime(int secondPulseTime) {
        this.secondPulseTime = secondPulseTime;
    }

    public int getThirdPulseTime() {
        return thirdPulseTime;
    }

    public void setThirdPulseTime(int thirdPulseTime) {
        this.thirdPulseTime = thirdPulseTime;
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

    public int getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(int defaultType) {
        this.defaultType = defaultType;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
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

    public int getFontScale() {
        return fontScale;
    }

    public void setFontScale(int fontScale) {
        this.fontScale = fontScale;
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

    public Map<Float, Integer> getDacMap() {
        return dacMap;
    }

    public void setDacMap(Map<Float, Integer> dacMap) {
        this.dacMap = dacMap;
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

    public boolean isUpdateHint() {
        return updateHint;
    }

    public void setUpdateHint(boolean updateHint) {
        this.updateHint = updateHint;
    }
}
