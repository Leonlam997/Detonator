package com.leon.detonator.bean;

import java.util.List;

public class FaceIdentifyBean {
    /**
     * error_code : 0
     * error_msg : SUCCESS
     * log_id : 747956931766626731
     * timestamp : 1563176662
     * cached : 0
     * result : {"face_token":"45bc93f8231303e9b4a37bcd50bca125","user_list":[{"group_id":"zbgroup","user_id":"ken","user_info":"","score":22.319892883301}]}
     */

    private int error_code;
    private String error_msg;
    private long log_id;
    private int timestamp;
    private int cached;
    private ResultBean result;

    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public String getError_msg() {
        return error_msg;
    }

    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }

    public long getLog_id() {
        return log_id;
    }

    public void setLog_id(long log_id) {
        this.log_id = log_id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getCached() {
        return cached;
    }

    public void setCached(int cached) {
        this.cached = cached;
    }

    public ResultBean getResult() {
        return result;
    }

    public void setResult(ResultBean result) {
        this.result = result;
    }

    public static class ResultBean {
        /**
         * face_token : 45bc93f8231303e9b4a37bcd50bca125
         * user_list : [{"group_id":"zbgroup","user_id":"ken","user_info":"","score":22.319892883301}]
         */

        private String face_token;
        private List<UserListBean> user_list;

        public String getFace_token() {
            return face_token;
        }

        public void setFace_token(String face_token) {
            this.face_token = face_token;
        }

        public List<UserListBean> getUser_list() {
            return user_list;
        }

        public void setUser_list(List<UserListBean> user_list) {
            this.user_list = user_list;
        }

        public static class UserListBean {
            /**
             * group_id : zbgroup
             * user_id : ken
             * user_info :
             * score : 22.319892883301
             */

            private String group_id;
            private String user_id;
            private String user_info;
            private double score;

            public String getGroup_id() {
                return group_id;
            }

            public void setGroup_id(String group_id) {
                this.group_id = group_id;
            }

            public String getUser_id() {
                return user_id;
            }

            public void setUser_id(String user_id) {
                this.user_id = user_id;
            }

            public String getUser_info() {
                return user_info;
            }

            public void setUser_info(String user_info) {
                this.user_info = user_info;
            }

            public double getScore() {
                return score;
            }

            public void setScore(double score) {
                this.score = score;
            }
        }
    }
}
