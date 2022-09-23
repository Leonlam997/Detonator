package com.leon.detonator.bean;

import java.util.List;

public class EnterpriseProjectBean extends BaseResultBean {
    private ResultBean Result;

    public ResultBean getResult() {
        return Result;
    }

    public void setResult(ResultBean result) {
        Result = result;
    }

    public static class ResultBean {
        /**
         * Count : 1
         * PageTotal : 1
         * PageIndex : 1
         * PageSize : 20
         * PageList : [{"Detonator":[{"DetonatorID":616,"SOC":1000000000000000,"DSC":"4490610300000","Model":0,"Line":1.2,"Delay":6000,"BoxID":3,"BoxNumber":99,"MakeDate":"2019-07-12T00:00:00","UID":null,"Version":null,"ExpiryDate":"2021-07-12T00:00:00","IsDisabled":false},{"DetonatorID":617,"SOC":1000000000000001,"DSC":"4490610300001","Model":0,"Line":1.2,"Delay":6000,"BoxID":3,"BoxNumber":98,"MakeDate":"2019-07-12T00:00:00","UID":null,"Version":null,"ExpiryDate":"2021-07-12T00:00:00","IsDisabled":false},{"DetonatorID":618,"SOC":1000000000000002,"DSC":"4490610300002","Model":0,"Line":1.2,"Delay":6000,"BoxID":3,"BoxNumber":97,"MakeDate":"2019-07-12T00:00:00","UID":null,"Version":null,"ExpiryDate":"2021-07-12T00:00:00","IsDisabled":false}],"ProjectID":10000005,"EnterpriseID":1000000,"Name":"三河乡采石场","Lng":114.16844715692068,"Lat":22.698500964235272,"Radius":500,"StartTime":"2019-08-02T16:55:14","EndTime":"2019-08-16T11:55:14","EnvironmentType":0,"IsBlast":false,"BlastTime":null,"BlastLng":null,"BlastLat":null,"UserID":1000000,"sbbh":"F4410000001","htid":"370101318060011","xmbh":"370100X15040032","IsMBOnlineDownloadRule":false,"IsMBOfflineDownloadRule":false,"IsMBDetonatorUploadRule":false,"CreateTime":"2019-08-01T16:44:02.623"}]
         * QuerySql : null
         */

        private int Count;
        private int PageTotal;
        private int PageIndex;
        private int PageSize;
        private Object QuerySql;
        private List<PageListBean> PageList;

        public int getCount() {
            return Count;
        }

        public void setCount(int Count) {
            this.Count = Count;
        }

        public int getPageTotal() {
            return PageTotal;
        }

        public void setPageTotal(int PageTotal) {
            this.PageTotal = PageTotal;
        }

        public int getPageIndex() {
            return PageIndex;
        }

        public void setPageIndex(int PageIndex) {
            this.PageIndex = PageIndex;
        }

        public int getPageSize() {
            return PageSize;
        }

        public void setPageSize(int PageSize) {
            this.PageSize = PageSize;
        }

        public Object getQuerySql() {
            return QuerySql;
        }

        public void setQuerySql(Object QuerySql) {
            this.QuerySql = QuerySql;
        }

        public List<PageListBean> getPageList() {
            return PageList;
        }

        public void setPageList(List<PageListBean> PageList) {
            this.PageList = PageList;
        }

        public static class PageListBean {
            /**
             * Detonator : [{"DetonatorID":616,"SOC":1000000000000000,"DSC":"4490610300000","Model":0,"Line":1.2,"Delay":6000,"BoxID":3,"BoxNumber":99,"MakeDate":"2019-07-12T00:00:00","UID":null,"Version":null,"ExpiryDate":"2021-07-12T00:00:00","IsDisabled":false},{"DetonatorID":617,"SOC":1000000000000001,"DSC":"4490610300001","Model":0,"Line":1.2,"Delay":6000,"BoxID":3,"BoxNumber":98,"MakeDate":"2019-07-12T00:00:00","UID":null,"Version":null,"ExpiryDate":"2021-07-12T00:00:00","IsDisabled":false},{"DetonatorID":618,"SOC":1000000000000002,"DSC":"4490610300002","Model":0,"Line":1.2,"Delay":6000,"BoxID":3,"BoxNumber":97,"MakeDate":"2019-07-12T00:00:00","UID":null,"Version":null,"ExpiryDate":"2021-07-12T00:00:00","IsDisabled":false}]
             * ProjectID : 10000005
             * EnterpriseID : 1000000
             * Name : 三河乡采石场
             * Lng : 114.16844715692068
             * Lat : 22.698500964235272
             * Radius : 500
             * StartTime : 2019-08-02T16:55:14
             * EndTime : 2019-08-16T11:55:14
             * EnvironmentType : 0
             * IsBlast : false
             * BlastTime : null
             * BlastLng : null
             * BlastLat : null
             * UserID : 1000000
             * sbbh : F4410000001
             * htid : 370101318060011
             * xmbh : 370100X15040032
             * IsMBOnlineDownloadRule : false
             * IsMBOfflineDownloadRule : false
             * IsMBDetonatorUploadRule : false
             * CreateTime : 2019-08-01T16:44:02.623
             */

            private int ProjectID;
            private int EnterpriseID;
            private String Name;
            private double Lng;
            private double Lat;
            private int Radius;
            private String StartTime;
            private String EndTime;
            private int EnvironmentType;
            private boolean IsBlast;
            private Object BlastTime;
            private Object BlastLng;
            private Object BlastLat;
            private int UserID;
            private String sbbh;
            private String htid;
            private String xmbh;
            private boolean IsMBOnlineDownloadRule;
            private boolean IsMBOfflineDownloadRule;
            private boolean IsMBDetonatorUploadRule;
            private String CreateTime;
            private List<DetonatorBean> Detonator;

            public int getProjectID() {
                return ProjectID;
            }

            public void setProjectID(int ProjectID) {
                this.ProjectID = ProjectID;
            }

            public int getEnterpriseID() {
                return EnterpriseID;
            }

            public void setEnterpriseID(int EnterpriseID) {
                this.EnterpriseID = EnterpriseID;
            }

            public String getName() {
                return Name;
            }

            public void setName(String Name) {
                this.Name = Name;
            }

            public double getLng() {
                return Lng;
            }

            public void setLng(double Lng) {
                this.Lng = Lng;
            }

            public double getLat() {
                return Lat;
            }

            public void setLat(double Lat) {
                this.Lat = Lat;
            }

            public int getRadius() {
                return Radius;
            }

            public void setRadius(int Radius) {
                this.Radius = Radius;
            }

            public String getStartTime() {
                return StartTime;
            }

            public void setStartTime(String StartTime) {
                this.StartTime = StartTime;
            }

            public String getEndTime() {
                return EndTime;
            }

            public void setEndTime(String EndTime) {
                this.EndTime = EndTime;
            }

            public int getEnvironmentType() {
                return EnvironmentType;
            }

            public void setEnvironmentType(int EnvironmentType) {
                this.EnvironmentType = EnvironmentType;
            }

            public boolean isIsBlast() {
                return IsBlast;
            }

            public void setIsBlast(boolean IsBlast) {
                this.IsBlast = IsBlast;
            }

            public Object getBlastTime() {
                return BlastTime;
            }

            public void setBlastTime(Object BlastTime) {
                this.BlastTime = BlastTime;
            }

            public Object getBlastLng() {
                return BlastLng;
            }

            public void setBlastLng(Object BlastLng) {
                this.BlastLng = BlastLng;
            }

            public Object getBlastLat() {
                return BlastLat;
            }

            public void setBlastLat(Object BlastLat) {
                this.BlastLat = BlastLat;
            }

            public int getUserID() {
                return UserID;
            }

            public void setUserID(int UserID) {
                this.UserID = UserID;
            }

            public String getSbbh() {
                return sbbh;
            }

            public void setSbbh(String sbbh) {
                this.sbbh = sbbh;
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

            public boolean isIsMBOnlineDownloadRule() {
                return IsMBOnlineDownloadRule;
            }

            public void setIsMBOnlineDownloadRule(boolean IsMBOnlineDownloadRule) {
                this.IsMBOnlineDownloadRule = IsMBOnlineDownloadRule;
            }

            public boolean isIsMBOfflineDownloadRule() {
                return IsMBOfflineDownloadRule;
            }

            public void setIsMBOfflineDownloadRule(boolean IsMBOfflineDownloadRule) {
                this.IsMBOfflineDownloadRule = IsMBOfflineDownloadRule;
            }

            public boolean isIsMBDetonatorUploadRule() {
                return IsMBDetonatorUploadRule;
            }

            public void setIsMBDetonatorUploadRule(boolean IsMBDetonatorUploadRule) {
                this.IsMBDetonatorUploadRule = IsMBDetonatorUploadRule;
            }

            public String getCreateTime() {
                return CreateTime;
            }

            public void setCreateTime(String CreateTime) {
                this.CreateTime = CreateTime;
            }

            public List<DetonatorBean> getDetonator() {
                return Detonator;
            }

            public void setDetonator(List<DetonatorBean> Detonator) {
                this.Detonator = Detonator;
            }

            public static class DetonatorBean {
                /**
                 * DetonatorID : 616
                 * SOC : 1000000000000000
                 * DSC : 4490610300000
                 * Model : 0
                 * Line : 1.2
                 * Delay : 6000
                 * BoxID : 3
                 * BoxNumber : 99
                 * MakeDate : 2019-07-12T00:00:00
                 * UID : null
                 * Version : null
                 * ExpiryDate : 2021-07-12T00:00:00
                 * IsDisabled : false
                 */

                private int DetonatorID;
                private long SOC;
                private String DSC;
                private int Model;
                private double Line;
                private int Delay;
                private int BoxID;
                private int BoxNumber;
                private String MakeDate;
                private Object UID;
                private Object Version;
                private String ExpiryDate;
                private boolean IsDisabled;

                public int getDetonatorID() {
                    return DetonatorID;
                }

                public void setDetonatorID(int DetonatorID) {
                    this.DetonatorID = DetonatorID;
                }

                public long getSOC() {
                    return SOC;
                }

                public void setSOC(long SOC) {
                    this.SOC = SOC;
                }

                public String getDSC() {
                    return DSC;
                }

                public void setDSC(String DSC) {
                    this.DSC = DSC;
                }

                public int getModel() {
                    return Model;
                }

                public void setModel(int Model) {
                    this.Model = Model;
                }

                public double getLine() {
                    return Line;
                }

                public void setLine(double Line) {
                    this.Line = Line;
                }

                public int getDelay() {
                    return Delay;
                }

                public void setDelay(int Delay) {
                    this.Delay = Delay;
                }

                public int getBoxID() {
                    return BoxID;
                }

                public void setBoxID(int BoxID) {
                    this.BoxID = BoxID;
                }

                public int getBoxNumber() {
                    return BoxNumber;
                }

                public void setBoxNumber(int BoxNumber) {
                    this.BoxNumber = BoxNumber;
                }

                public String getMakeDate() {
                    return MakeDate;
                }

                public void setMakeDate(String MakeDate) {
                    this.MakeDate = MakeDate;
                }

                public Object getUID() {
                    return UID;
                }

                public void setUID(Object UID) {
                    this.UID = UID;
                }

                public Object getVersion() {
                    return Version;
                }

                public void setVersion(Object Version) {
                    this.Version = Version;
                }

                public String getExpiryDate() {
                    return ExpiryDate;
                }

                public void setExpiryDate(String ExpiryDate) {
                    this.ExpiryDate = ExpiryDate;
                }

                public boolean isIsDisabled() {
                    return IsDisabled;
                }

                public void setIsDisabled(boolean IsDisabled) {
                    this.IsDisabled = IsDisabled;
                }
            }
        }
    }
}
