package com.leon.detonator.bean;

import java.util.Date;

/**
 * Created by Leon on 2018/1/25.
 */

public class ExplosionRecordBean {
    private long id;
    private String name;
    private Date uploadTime;
    private Date explodeTime;
    private int amount;
    private int uploadServer;
    private double lat;
    private double lng;
    private boolean synchronize;
    private boolean selected;
    private boolean tunnel;
    private boolean deleted;

    public ExplosionRecordBean() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int num) {
        this.amount = num;
    }

    public Date getExplodeTime() {
        return this.explodeTime;
    }

    public void setExplodeTime(Date date) {
        this.explodeTime = date;
    }

    public int getUploadServer() {
        return this.uploadServer;
    }

    public void setUploadServer(int isUpload) {
        this.uploadServer = isUpload;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public boolean isSynchronize() {
        return synchronize;
    }

    public void setSynchronize(boolean synchronize) {
        this.synchronize = synchronize;
    }

    public boolean isTunnel() {
        return tunnel;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
