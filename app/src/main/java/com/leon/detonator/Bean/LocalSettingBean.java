package com.leon.detonator.Bean;

import com.leon.detonator.Util.ConstantUtils;

import java.util.Map;

public class LocalSettingBean {
    private String serialNum, delayTime, delayPeriod, mtMac, exploderID, IMEI;
    private int row, hole, holeInside, section, sectionInside, defaultType = 0, userID, volume, serverHost, fontScale;
    private boolean vibrate, registered, newLG;
    private double latitude, longitude;
    private Map<Float, Integer> dacMap;

    public LocalSettingBean() {
        row = ConstantUtils.PRESET_ROW_DELAY_TIME;
        hole = ConstantUtils.PRESET_HOLE_DELAY_TIME;
        holeInside = ConstantUtils.PRESET_HOLE_INSIDE_DELAY_TIME;
        section = ConstantUtils.PRESET_SECTION_DELAY_TIME;
        sectionInside = ConstantUtils.PRESET_SECTION_INSIDE_DELAY_TIME;
        volume = ConstantUtils.MAX_VOLUME;
        latitude = 0;
        longitude = 0;
        serverHost = 1;
        vibrate = true;
        newLG = false;
        registered = false;
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

    public boolean isNewLG() {
        return newLG;
    }

    public void setNewLG(boolean newLG) {
        this.newLG = newLG;
    }
}
