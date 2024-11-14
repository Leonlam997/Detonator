package com.leon.detonator.bean;

public class DanLinOfflineDownloadRequestBean {
    /**
     * 箱条码
     **/
    private String xtm;
    /**
     * 盒条码
     **/
    private String htm;
    /**
     * 雷管发编号
     * 注：现只针对离线下载
     **/
    private String fbh;
    /**
     * 合同编号
     **/
    private String htid;
    /**
     * 项目编号
     **/
    private String xmbh;
    /**
     * 起爆器编号
     **/
    private String sbbh;
    /**
     * 单位代码
     **/
    private String dwdm;

    public DanLinOfflineDownloadRequestBean() {
        xtm = "";
        htm = "";
        fbh = "";
        htid = "";
        xmbh = "";
        sbbh = "";
        dwdm = "";
    }

    public String getXtm() {
        return xtm;
    }

    public void setXtm(String xtm) {
        this.xtm = xtm;
    }

    public String getHtm() {
        return htm;
    }

    public void setHtm(String htm) {
        this.htm = htm;
    }

    public String getFbh() {
        return fbh;
    }

    public void setFbh(String fbh) {
        this.fbh = fbh;
    }

    public String getHtid() {
        return htid;
    }

    public void setHtid(String htid) {
        this.htid = htid;
    }

    public String getXmbh() {
        return xmbh;
    }

    public void setXmbh(String xmbh) {
        this.xmbh = xmbh;
    }

    public String getSbbh() {
        return sbbh;
    }

    public void setSbbh(String sbbh) {
        this.sbbh = sbbh;
    }

    public String getDwdm() {
        return dwdm;
    }

    public void setDwdm(String dwdm) {
        this.dwdm = dwdm;
    }
}
