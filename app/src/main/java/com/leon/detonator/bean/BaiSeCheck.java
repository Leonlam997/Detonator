package com.leon.detonator.bean;

public class BaiSeCheck {
    private boolean checked;
    private Data data;

    public BaiSeCheck() {
        data = new Data();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String deviceNO;
        private String gpsCoordinateSystems;
        private String lngLat;
        private String userIdCard;
        private String projectCode;
        private String appVersion;

        public String getDeviceNO() {
            return deviceNO;
        }

        public void setDeviceNO(String deviceNO) {
            this.deviceNO = deviceNO;
        }

        public String getGpsCoordinateSystems() {
            return gpsCoordinateSystems;
        }

        public void setGpsCoordinateSystems(String gpsCoordinateSystems) {
            this.gpsCoordinateSystems = gpsCoordinateSystems;
        }

        public String getLngLat() {
            return lngLat;
        }

        public void setLngLat(String lngLat) {
            this.lngLat = lngLat;
        }

        public String getUserIdCard() {
            return userIdCard;
        }

        public void setUserIdCard(String userIdCard) {
            this.userIdCard = userIdCard;
        }

        public String getProjectCode() {
            return projectCode;
        }

        public void setProjectCode(String projectCode) {
            this.projectCode = projectCode;
        }

        public String getAppVersion() {
            return appVersion;
        }

        public void setAppVersion(String appVersion) {
            this.appVersion = appVersion;
        }
    }
}
