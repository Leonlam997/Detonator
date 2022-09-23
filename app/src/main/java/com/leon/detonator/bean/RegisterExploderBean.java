package com.leon.detonator.bean;

public class RegisterExploderBean extends BaseResultBean {
    private ResultBean Result;

    public ResultBean getResult() {
        return Result;
    }

    public void setResult(ResultBean resultBean) {
        Result = resultBean;
    }

    public static class ResultBean {
        /**
         * Exploder : {"ExploderID":7,"PlanID":1,"EnterpriseID":1,"EnterpriseCode":"44","FeatureCode":"F","Number":7,"CodeID":"F44AB000007","IMEI":"704465660000070","MAC":"11-11-11-11-92-E6","ICCID":"33860084191839009000","IMSI":"334011234567091","MobilePhone":"33798249502","Model":0,"MakeDate":"2019-09-27T00:00:00","IsDisabled":false,"PrintLabelNumber":0,"PrintLabelTime":null,"Remarks":null,"CreateTime":"2019-09-27T11:04:35.657"}
         * QuerySql : null
         */

        private ExploderBean Exploder;
        private Object QuerySql;

        public ExploderBean getExploder() {
            return Exploder;
        }

        public void setExploder(ExploderBean Exploder) {
            this.Exploder = Exploder;
        }

        public Object getQuerySql() {
            return QuerySql;
        }

        public void setQuerySql(Object QuerySql) {
            this.QuerySql = QuerySql;
        }

        public static class ExploderBean {
            /**
             * ExploderID : 7
             * PlanID : 1
             * EnterpriseID : 1
             * EnterpriseCode : 44
             * FeatureCode : F
             * Number : 7
             * CodeID : F44AB000007
             * IMEI : 704465660000070
             * MAC : 11-11-11-11-92-E6
             * ICCID : 33860084191839009000
             * IMSI : 334011234567091
             * MobilePhone : 33798249502
             * Model : 0
             * MakeDate : 2019-09-27T00:00:00
             * IsDisabled : false
             * PrintLabelNumber : 0
             * PrintLabelTime : null
             * Remarks : null
             * CreateTime : 2019-09-27T11:04:35.657
             */

            private int ExploderID;
            private int PlanID;
            private int EnterpriseID;
            private String EnterpriseCode;
            private String FeatureCode;
            private int Number;
            private String CodeID;
            private String IMEI;
            private String MAC;
            private String ICCID;
            private String IMSI;
            private String MobilePhone;
            private int Model;
            private String MakeDate;
            private boolean IsDisabled;
            private int PrintLabelNumber;
            private Object PrintLabelTime;
            private Object Remarks;
            private String CreateTime;

            public int getExploderID() {
                return ExploderID;
            }

            public void setExploderID(int ExploderID) {
                this.ExploderID = ExploderID;
            }

            public int getPlanID() {
                return PlanID;
            }

            public void setPlanID(int PlanID) {
                this.PlanID = PlanID;
            }

            public int getEnterpriseID() {
                return EnterpriseID;
            }

            public void setEnterpriseID(int EnterpriseID) {
                this.EnterpriseID = EnterpriseID;
            }

            public String getEnterpriseCode() {
                return EnterpriseCode;
            }

            public void setEnterpriseCode(String EnterpriseCode) {
                this.EnterpriseCode = EnterpriseCode;
            }

            public String getFeatureCode() {
                return FeatureCode;
            }

            public void setFeatureCode(String FeatureCode) {
                this.FeatureCode = FeatureCode;
            }

            public int getNumber() {
                return Number;
            }

            public void setNumber(int Number) {
                this.Number = Number;
            }

            public String getCodeID() {
                return CodeID;
            }

            public void setCodeID(String CodeID) {
                this.CodeID = CodeID;
            }

            public String getIMEI() {
                return IMEI;
            }

            public void setIMEI(String IMEI) {
                this.IMEI = IMEI;
            }

            public String getMAC() {
                return MAC;
            }

            public void setMAC(String MAC) {
                this.MAC = MAC;
            }

            public String getICCID() {
                return ICCID;
            }

            public void setICCID(String ICCID) {
                this.ICCID = ICCID;
            }

            public String getIMSI() {
                return IMSI;
            }

            public void setIMSI(String IMSI) {
                this.IMSI = IMSI;
            }

            public String getMobilePhone() {
                return MobilePhone;
            }

            public void setMobilePhone(String MobilePhone) {
                this.MobilePhone = MobilePhone;
            }

            public int getModel() {
                return Model;
            }

            public void setModel(int Model) {
                this.Model = Model;
            }

            public String getMakeDate() {
                return MakeDate;
            }

            public void setMakeDate(String MakeDate) {
                this.MakeDate = MakeDate;
            }

            public boolean isIsDisabled() {
                return IsDisabled;
            }

            public void setIsDisabled(boolean IsDisabled) {
                this.IsDisabled = IsDisabled;
            }

            public int getPrintLabelNumber() {
                return PrintLabelNumber;
            }

            public void setPrintLabelNumber(int PrintLabelNumber) {
                this.PrintLabelNumber = PrintLabelNumber;
            }

            public Object getPrintLabelTime() {
                return PrintLabelTime;
            }

            public void setPrintLabelTime(Object PrintLabelTime) {
                this.PrintLabelTime = PrintLabelTime;
            }

            public Object getRemarks() {
                return Remarks;
            }

            public void setRemarks(Object Remarks) {
                this.Remarks = Remarks;
            }

            public String getCreateTime() {
                return CreateTime;
            }

            public void setCreateTime(String CreateTime) {
                this.CreateTime = CreateTime;
            }
        }
    }
}
