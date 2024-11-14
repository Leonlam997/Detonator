package com.leon.detonator.bean;

public class DanLinUploadResultBean {
    public static final String SUCCESS = "true";
    /**
     * 上传结果
     * true:成功
     * fail:失败
     */
    private String success;
    /**
     * 错误信息
     * 1 非法的申请信息
     * 2 起爆器未备案或未设置作业任务
     */
    private String cwxx;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getCwxx() {
        return cwxx;
    }

    public void setCwxx(String cwxx) {
        this.cwxx = cwxx;
    }
}
