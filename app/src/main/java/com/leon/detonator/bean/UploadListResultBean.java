package com.leon.detonator.bean;

public class UploadListResultBean extends BaseResultBean {
    private ResultBean Result;

    public ResultBean getResult() {
        return Result;
    }

    public void setResult(ResultBean resultBean) {
        this.Result = resultBean;
    }

    public static class ResultBean {
        private Object query;
    }
}
