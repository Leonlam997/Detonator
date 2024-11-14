package com.leon.detonator.bean;

import java.util.List;

public class DanLinDetonatorBean extends BaseResultBean {
    private ResultBean Result;
    private long id;
    private long enterpriseId;
    private boolean offline;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(long enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

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

        /**
         * 错误代码
         * 0 成功
         * 1 非法的申请信息
         * 2 未找到该起爆器设备信息或起爆器未设置作业任务
         * 3 该起爆器未设置作业任务
         * 4 起爆器在黑名单中
         * 5 起爆位置不在起爆区域内
         * 6 起爆位置在禁爆区域内
         * 7 该起爆器已注销/报废
         * 8 禁爆任务
         * 9 作业合同存在项目
         * 10 作业任务未设置准爆区域
         * 11 离线下载不支持生产厂家试爆
         * 12 营业性单位必须设置合同或者项目
         * 99	网络连接失败
         **/
        private String cwxx;
        /**
         * 申请时间
         **/
        private String sqrq;
        /**
         * 准爆区域
         **/
        private ZbqysBean zbqys;
        /**
         * 禁爆区域
         **/
        private JbqysBean jbqys;
        /**
         * 雷管信息
         **/
        private LgsBean lgs;
        /**
         * 起爆器设备编号
         **/
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
