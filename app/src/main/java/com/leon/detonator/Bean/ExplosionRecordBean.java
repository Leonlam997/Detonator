package com.leon.detonator.Bean;

import com.leon.detonator.Util.ConstantUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Leon on 2018/1/25.
 */

public class ExplosionRecordBean {
    private Date explodeDate;
    private int amount;
    private boolean uploaded;
    private String recordPath;
    private double lat, lng;
    private boolean selected;

    public ExplosionRecordBean() {
    }

    public ExplosionRecordBean(Date date, int count, boolean isUpload, double lat, double lng, String path) {
        this.explodeDate = date;
        this.amount = count;
        this.uploaded = isUpload;
        this.recordPath = path;
        this.lat = lat;
        this.lng = lng;
        this.selected = false;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int num) {
        this.amount = num;
    }

    public Date getExplodeDate() {
        return this.explodeDate;
    }

    public void setExplodeDate(Date date) {
        this.explodeDate = date;
    }

    public boolean isUploaded() {
        return this.uploaded;
    }

    public void setUploaded(boolean isUpload) {
        this.uploaded = isUpload;
    }

    public String getRecordPath() {
        return this.recordPath;
    }

    public void setRecordPath(String path) {
        this.recordPath = path;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean isSelect) {
        this.selected = isSelect;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String toStirng() {
        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
        return formatter.format(this.explodeDate) + "," +
                this.amount + "," +
                this.recordPath + "," +
                this.lat + "," +
                this.lng + "," +
                uploaded;
    }
}
