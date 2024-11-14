package com.leon.detonator.bean;

public class LgBean {
    /**
     * fbh : 4490610300050
     * uid : 1000000000000050
     * gzm : 11372760
     * yxq : 2019-10-11T16:33:54
     * gzmcwxx : 0
     */

    /**
     * 雷管对应爆破记录
     **/
    private long schemeId;
    /**
     * 雷管发编号
     * 注：现只针对离线下载
     **/
    private String fbh;
    /**
     * 雷管UID码
     **/
    private String uid;
    /**
     * 工作码
     **/
    private String gzm;
    /**
     * 工作码有效期
     **/
    private String yxq;
    /**
     * 雷管工作码错误信息
     * 0 雷管正常
     * 1 雷管在黑名单中
     * 2 雷管已使用
     * 3 申请的雷管UID不存在
     **/
    private String gzmcwxx;

    public long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(long schemeId) {
        this.schemeId = schemeId;
    }

    public String getFbh() {
        return fbh;
    }

    public void setFbh(String fbh) {
        this.fbh = fbh;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getGzm() {
        return gzm;
    }

    public void setGzm(String gzm) {
        this.gzm = gzm;
    }

    public String getYxq() {
        return yxq;
    }

    public void setYxq(String yxq) {
        this.yxq = yxq;
    }

    public String getGzmcwxx() {
        return gzmcwxx;
    }

    public void setGzmcwxx(String gzmcwxx) {
        this.gzmcwxx = gzmcwxx;
    }
}
