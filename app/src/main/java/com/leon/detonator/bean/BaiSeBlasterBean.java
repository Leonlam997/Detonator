package com.leon.detonator.bean;

public class BaiSeBlasterBean {
    private long id;
    private boolean checked;
    private Data data;
    private boolean project;
    private boolean selected;
    private String name;

    public boolean isProject() {
        return project;
    }

    public void setProject(boolean project) {
        this.project = project;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public BaiSeBlasterBean() {
        id = -1;
        data = new Data();
        name = "";
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        private String burstOrgCode;

        public Data() {
            deviceNO = "";
            gpsCoordinateSystems = "";
            lngLat = "";
            userIdCard = "";
            projectCode = "";
            appVersion = "";
        }

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

        public String getBurstOrgCode() {
            return burstOrgCode;
        }

        public void setBurstOrgCode(String burstOrgCode) {
            this.burstOrgCode = burstOrgCode;
        }
    }
}
