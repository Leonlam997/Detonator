package com.leon.detonator.Bean;

public class UploadExplodeRecordsBean extends BaseResultBean {
    private ResultBean Result;

    public ResultBean getResult() {
        return Result;
    }

    public void setResult(ResultBean result) {
        Result = result;
    }

    public static class ResultBean {
        /**
         * success : true
         * cwxx : null
         */

        private boolean success;
        private Object cwxx;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Object getCwxx() {
            return cwxx;
        }

        public void setCwxx(Object cwxx) {
            this.cwxx = cwxx;
        }
    }
}
