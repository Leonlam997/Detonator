package com.leon.detonator.util;

/**
 * Created by Leon on 2018/1/24.
 */

public class ConstantUtils {
    public final static String HOST_URL = "http://www.zhongbao360.com/open_service_v1";
    //        public final static String HOST_URL = "http://192.168.0.2/open_service_v1";
    public final static String VERSION_URL = "http://zhongbao360.com/File/Client/Temp/update.json";
    public final static String UPLOAD_LOG_URL = "http://www.zhongbao360.com/Exploder/UploadLog";
    public final static String[][] UPLOAD_HOST = {{"丹灵网", ""},
            {"广西民爆", "119.29.111.172:6088"},
            {"黔南民爆", "113.140.1.135:9903"},
            {"黔东南民爆", "113.140.1.137:8608"},
            {"贵阳民爆", "119.29.111.172:6089"},
            {"贵安民爆", "113.140.1.137:8610"}};
    public final static String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
    public final static String DATE_FORMAT_CHINESE = "yyyy年MM月dd日HH:mm";
    public final static String INPUT_DETONATOR_ACCEPT = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";
    public final static String EXPLODE_PSW = "772298";
    public final static String SHELL_PATTERN = "\\d{7}[0-9A-Z]\\d{5}$";
    public final static String UID_PATTERN = "\\d[0-9A-Z]\\d{5}$";
    public final static int RESUME_LIST = 1;
    public final static int MODIFY_LIST = 2;
    public final static int HISTORY_LIST = 3;
    public final static int AUTHORIZED_LIST = 4;
    public final static int REFRESH_STATUS_BAR_PERIOD = 1000;
    public final static int MAX_DELAY_TIME = 12000;
    public final static int BOOST_TIME = 600;
    public final static int UPLOAD_TIMEOUT = 10000;
    public final static int MAX_VOLUME = 5;
    public final static int INSERT_HOLE = 1;
    public final static int INSERT_INSIDE = 2; //1:孔间插入 2.孔内插入
    public final static int ERROR_RESULT_OPEN_FAIL = 1;
    public final static int ERROR_RESULT_SHORT_CIRCUIT = 2;
    public final static int RESEND_CMD_TIMEOUT = 500;
    public final static int RESEND_STATUS_TIMEOUT = 500;
    public final static int RESEND_READ_FIELD_CMD_TIMEOUT = 1000;
    public final static int RESEND_SCAN_UID_TIMEOUT = 1500;
    public final static int COMMAND_DELAY_TIME = 10;
    public final static int RELEASE_DELAY_TIME = 20;
    public final static int RESET_DELAY_TIME = 40;
    public final static int SCAN_DELAY_TIME = 100;
    public final static int INITIAL_TIME = 35;
    public final static int CHECK_CAPACITY_TIME = 5500;
    public final static int UID_LEN = 7;
    public final static int PSW_LEN = 6;
    public final static int DETONATOR_VERSION = 1;
    public final static int CHECK_CAPACITY_LEVEL = 0x1;
    public final static int SCAN_CODE_TIME = 3000;
    public final static int RESEND_SCAN_TIMEOUT = 3500;
    public final static int SCAN_ZERO_COUNT = 5;
    public enum LIST_TYPE {
        ALL,
        DETECTED,
        NOT_FOUND,
        ERROR,
        END
    }
}
