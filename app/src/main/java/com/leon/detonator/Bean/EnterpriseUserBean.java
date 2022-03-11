package com.leon.detonator.Bean;

import java.util.List;

public class EnterpriseUserBean extends BaseResultBean {
    private ResultBean Result;

    public ResultBean getResult() {
        return Result;
    }

    public void setResult(ResultBean resultBean) {
        Result = resultBean;
    }

    public static class ResultBean {
        /**
         * Count : 1
         * PageTotal : 1
         * PageIndex : 1
         * PageSize : 20
         * PageList : [{"UserID":1000000,"Account":"zgx2","Password":"83263512CDDD0BD9","IsLock":false,"LastLoginTime":"2018-04-25T16:07:00","LastLoginIP":"::1","EnterpriseID":1000000,"Type":0,"Name":"赵光许","Gender":"男","Ethnicity":"汉","DateOfBirth":"2018-04-25T00:00:00","CitizenIDNumber":"510723198410151354","WorkUnit":"中爆","ExpiryDate":"2018-04-20T00:00:00","Authority":"广西公安厅2","PermitID":"236346346342","PermitPhoto":"/file/common/20180425/85D505C51F98CC9A.jpg","IsCheck":false,"Phone":"111111111112","HeadPhoto":"/file/common/20181229/E9B7E64150A673C7.jpg","CardFrontPhoto":"/file/common/20180425/A15758E32F407157.jpg","CardBackPhoto":"/file/common/20180425/E8446042293A0DF0.jpg","CardSignaturePhoto":"/file/common/20180425/B579DEE57E57C262.jpg","LeftFinger2Photo":"/file/common/20180425/376BEEACA26CFB0A.jpg","RightFinger2Photo":"/file/common/20180425/2B1AE5C8CFC7F567.jpg","CreateTime":"2018-04-25T16:07:00"}]
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
             * UserID : 1000000
             * Account : zgx2
             * Password : 83263512CDDD0BD9
             * IsLock : false
             * LastLoginTime : 2018-04-25T16:07:00
             * LastLoginIP : ::1
             * EnterpriseID : 1000000
             * Type : 0
             * Name : 赵光许
             * Gender : 男
             * Ethnicity : 汉
             * DateOfBirth : 2018-04-25T00:00:00
             * CitizenIDNumber : 510723198410151354
             * WorkUnit : 中爆
             * ExpiryDate : 2018-04-20T00:00:00
             * Authority : 广西公安厅2
             * PermitID : 236346346342
             * PermitPhoto : /file/common/20180425/85D505C51F98CC9A.jpg
             * IsCheck : false
             * Phone : 111111111112
             * HeadPhoto : /file/common/20181229/E9B7E64150A673C7.jpg
             * CardFrontPhoto : /file/common/20180425/A15758E32F407157.jpg
             * CardBackPhoto : /file/common/20180425/E8446042293A0DF0.jpg
             * CardSignaturePhoto : /file/common/20180425/B579DEE57E57C262.jpg
             * LeftFinger2Photo : /file/common/20180425/376BEEACA26CFB0A.jpg
             * RightFinger2Photo : /file/common/20180425/2B1AE5C8CFC7F567.jpg
             * CreateTime : 2018-04-25T16:07:00
             */

            private int UserID;
            private String Account;
            private String Password;
            private boolean IsLock;
            private String LastLoginTime;
            private String LastLoginIP;
            private int EnterpriseID;
            private int Type;
            private String Name;
            private String Gender;
            private String Ethnicity;
            private String DateOfBirth;
            private String CitizenIDNumber;
            private String WorkUnit;
            private String ExpiryDate;
            private String Authority;
            private String PermitID;
            private String PermitPhoto;
            private boolean IsCheck;
            private String Phone;
            private String HeadPhoto;
            private String CardFrontPhoto;
            private String CardBackPhoto;
            private String CardSignaturePhoto;
            private String LeftFinger2Photo;
            private String RightFinger2Photo;
            private String CreateTime;

            public int getUserID() {
                return UserID;
            }

            public void setUserID(int UserID) {
                this.UserID = UserID;
            }

            public String getAccount() {
                return Account;
            }

            public void setAccount(String Account) {
                this.Account = Account;
            }

            public String getPassword() {
                return Password;
            }

            public void setPassword(String Password) {
                this.Password = Password;
            }

            public boolean isIsLock() {
                return IsLock;
            }

            public void setIsLock(boolean IsLock) {
                this.IsLock = IsLock;
            }

            public String getLastLoginTime() {
                return LastLoginTime;
            }

            public void setLastLoginTime(String LastLoginTime) {
                this.LastLoginTime = LastLoginTime;
            }

            public String getLastLoginIP() {
                return LastLoginIP;
            }

            public void setLastLoginIP(String LastLoginIP) {
                this.LastLoginIP = LastLoginIP;
            }

            public int getEnterpriseID() {
                return EnterpriseID;
            }

            public void setEnterpriseID(int EnterpriseID) {
                this.EnterpriseID = EnterpriseID;
            }

            public int getType() {
                return Type;
            }

            public void setType(int Type) {
                this.Type = Type;
            }

            public String getName() {
                return Name;
            }

            public void setName(String Name) {
                this.Name = Name;
            }

            public String getGender() {
                return Gender;
            }

            public void setGender(String Gender) {
                this.Gender = Gender;
            }

            public String getEthnicity() {
                return Ethnicity;
            }

            public void setEthnicity(String Ethnicity) {
                this.Ethnicity = Ethnicity;
            }

            public String getDateOfBirth() {
                return DateOfBirth;
            }

            public void setDateOfBirth(String DateOfBirth) {
                this.DateOfBirth = DateOfBirth;
            }

            public String getCitizenIDNumber() {
                return CitizenIDNumber;
            }

            public void setCitizenIDNumber(String CitizenIDNumber) {
                this.CitizenIDNumber = CitizenIDNumber;
            }

            public String getWorkUnit() {
                return WorkUnit;
            }

            public void setWorkUnit(String WorkUnit) {
                this.WorkUnit = WorkUnit;
            }

            public String getExpiryDate() {
                return ExpiryDate;
            }

            public void setExpiryDate(String ExpiryDate) {
                this.ExpiryDate = ExpiryDate;
            }

            public String getAuthority() {
                return Authority;
            }

            public void setAuthority(String Authority) {
                this.Authority = Authority;
            }

            public String getPermitID() {
                return PermitID;
            }

            public void setPermitID(String PermitID) {
                this.PermitID = PermitID;
            }

            public String getPermitPhoto() {
                return PermitPhoto;
            }

            public void setPermitPhoto(String PermitPhoto) {
                this.PermitPhoto = PermitPhoto;
            }

            public boolean isIsCheck() {
                return IsCheck;
            }

            public void setIsCheck(boolean IsCheck) {
                this.IsCheck = IsCheck;
            }

            public String getPhone() {
                return Phone;
            }

            public void setPhone(String Phone) {
                this.Phone = Phone;
            }

            public String getHeadPhoto() {
                return HeadPhoto;
            }

            public void setHeadPhoto(String HeadPhoto) {
                this.HeadPhoto = HeadPhoto;
            }

            public String getCardFrontPhoto() {
                return CardFrontPhoto;
            }

            public void setCardFrontPhoto(String CardFrontPhoto) {
                this.CardFrontPhoto = CardFrontPhoto;
            }

            public String getCardBackPhoto() {
                return CardBackPhoto;
            }

            public void setCardBackPhoto(String CardBackPhoto) {
                this.CardBackPhoto = CardBackPhoto;
            }

            public String getCardSignaturePhoto() {
                return CardSignaturePhoto;
            }

            public void setCardSignaturePhoto(String CardSignaturePhoto) {
                this.CardSignaturePhoto = CardSignaturePhoto;
            }

            public String getLeftFinger2Photo() {
                return LeftFinger2Photo;
            }

            public void setLeftFinger2Photo(String LeftFinger2Photo) {
                this.LeftFinger2Photo = LeftFinger2Photo;
            }

            public String getRightFinger2Photo() {
                return RightFinger2Photo;
            }

            public void setRightFinger2Photo(String RightFinger2Photo) {
                this.RightFinger2Photo = RightFinger2Photo;
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
