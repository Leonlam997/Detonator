package com.leon.detonator.bean;

import java.util.List;

public class DownloadDetonatorBean extends BaseResultBean {
    private ResultBean Result;

    public ResultBean getResult() {
        return Result;
    }

    public void setResult(ResultBean result) {
        Result = result;
    }

    public static class ResultBean {
        /**
         * cwxx : 0
         * sqrq : 2019-10-08 16:33:54
         * sbbhs : [{"sbbh":"F44AB000001"}]
         * zbqys : {"zbqy":[{"zbqymc":"广西中爆科技有限公司","zbqyjd":"113.924142","zbqywd":"22.52001","zbqybj":"600","zbqssj":null,"zbjzsj":null}]}
         * jbqys : {"jbqy":[]}
         * lgs : {"lg":[{"fbh":"4490610300050","uid":"1000000000000050","gzm":"11372760","yxq":"2019-10-11T16:33:54","gzmcwxx":"0"},{"fbh":"4490610300049","uid":"1000000000000049","gzm":"65056276","yxq":"2019-10-11T16:33:54","gzmcwxx":"0"},{"fbh":"4490610300051","uid":"1000000000000051","gzm":"43529905","yxq":"2019-10-11T16:33:54","gzmcwxx":"0"}]}
         */

        private String cwxx;
        private String sqrq;
        private ZbqysBean zbqys;
        private JbqysBean jbqys;
        private LgsBean lgs;
        private List<SbbhsBean> sbbhs;

        public String getCwxx() {
            return cwxx;
        }

        public void setCwxx(String cwxx) {
            this.cwxx = cwxx;
        }

        public String getSqrq() {
            return sqrq;
        }

        public void setSqrq(String sqrq) {
            this.sqrq = sqrq;
        }

        public ZbqysBean getZbqys() {
            return zbqys;
        }

        public void setZbqys(ZbqysBean zbqys) {
            this.zbqys = zbqys;
        }

        public JbqysBean getJbqys() {
            return jbqys;
        }

        public void setJbqys(JbqysBean jbqys) {
            this.jbqys = jbqys;
        }

        public LgsBean getLgs() {
            return lgs;
        }

        public void setLgs(LgsBean lgs) {
            this.lgs = lgs;
        }

        public List<SbbhsBean> getSbbhs() {
            return sbbhs;
        }

        public void setSbbhs(List<SbbhsBean> sbbhs) {
            this.sbbhs = sbbhs;
        }

        public static class ZbqysBean {
            private List<ZbqyBean> zbqy;

            public List<ZbqyBean> getZbqy() {
                return zbqy;
            }

            public void setZbqy(List<ZbqyBean> zbqy) {
                this.zbqy = zbqy;
            }
        }

        public static class JbqysBean {
            private List<JbqyBean> jbqy;

            public List<JbqyBean> getJbqy() {
                return jbqy;
            }

            public void setJbqy(List<JbqyBean> jbqy) {
                this.jbqy = jbqy;
            }
        }

        public static class LgsBean {
            private List<LgBean> lg;

            public List<LgBean> getLg() {
                return lg;
            }

            public void setLg(List<LgBean> lg) {
                this.lg = lg;
            }

        }

        public static class SbbhsBean {
            /**
             * sbbh : F44AB000001
             */

            private String sbbh;

            public String getSbbh() {
                return sbbh;
            }

            public void setSbbh(String sbbh) {
                this.sbbh = sbbh;
            }
        }
    }
}
