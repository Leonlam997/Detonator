package com.leon.detonator.bean;

public class DanLinUploadRequestBean {
    /**
     * 起爆器设备编号
     */
    private String sbbh;
    /**
     * 经度
     */
    private String jd;
    /**
     * 纬度
     */
    private String wd;
    /**
     * 爆破时间
     */
    private String bpsj;
    /**
     * 爆破人员身份证
     */
    private String bprysfz;
    /**
     * 雷管UID
     * 多个雷管之间用逗号分隔
     */
    private String uid;
    /**
     * 合同ID
     */
    private String htid;
    /**
     * 项目编号
     */
    private String xmbh;
    /**
     * 单位代码
     */
    private String dwdm;

    public String getSbbh() {
        return sbbh;
    }

    public void setSbbh(String sbbh) {
        this.sbbh = sbbh;
    }

    public String getJd() {
        return jd;
    }

    public void setJd(String jd) {
        this.jd = jd;
    }

    public String getWd() {
        return wd;
    }

    public void setWd(String wd) {
        this.wd = wd;
    }

    public String getBpsj() {
        return bpsj;
    }

    public void setBpsj(String bpsj) {
        this.bpsj = bpsj;
    }

    public String getBprysfz() {
        return bprysfz;
    }

    public void setBprysfz(String bprysfz) {
        this.bprysfz = bprysfz;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public String getDwdm() {
        return dwdm;
    }

    public void setDwdm(String dwdm) {
        this.dwdm = dwdm;
    }
}
