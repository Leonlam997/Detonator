package com.leon.detonator.bean;

public class BaiSeCheckResult {

    private Data data;
    private String message;
    private boolean success;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static class Data {
        private String checkRecordId;
        private String deviceNO;
        private String projectCode;
        private boolean isPass;
        private String msg;
        private String userId;
        private String userName;
        private String checkTime;
        private int expires;
        private boolean canForceDetonate;
        private String description;

        public String getCheckRecordId() {
            return checkRecordId;
        }

        public void setCheckRecordId(String checkRecordId) {
            this.checkRecordId = checkRecordId;
        }

        public String getDeviceNO() {
            return deviceNO;
        }

        public void setDeviceNO(String deviceNO) {
            this.deviceNO = deviceNO;
        }

        public String getProjectCode() {
            return projectCode;
        }

        public void setProjectCode(String projectCode) {
            this.projectCode = projectCode;
        }

        public boolean isIsPass() {
            return isPass;
        }

        public void setIsPass(boolean isPass) {
            this.isPass = isPass;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getCheckTime() {
            return checkTime;
        }

        public void setCheckTime(String checkTime) {
            this.checkTime = checkTime;
        }

        public int getExpires() {
            return expires;
        }

        public void setExpires(int expires) {
            this.expires = expires;
        }

        public boolean isCanForceDetonate() {
            return canForceDetonate;
        }

        public void setCanForceDetonate(boolean canForceDetonate) {
            this.canForceDetonate = canForceDetonate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
