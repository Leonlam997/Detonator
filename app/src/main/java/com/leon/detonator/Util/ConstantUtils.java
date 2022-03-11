package com.leon.detonator.Util;

import com.leon.detonator.Base.BaseApplication;

/**
 * Created by Leon on 2018/1/24.
 */

public class ConstantUtils {
    public final static String HOST_URL = "http://www.zhongbao360.com/open_service_v1";
    //        public final static String HOST_URL = "http://192.168.0.2/open_service_v1";
    public final static String VERSION_URL = "http://zhongbao360.com/File/Client/update.json";
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
    public final static int BUTTON_INVISIBLE = 0;
    public final static int BUTTON_VISIBLE = 1;
    public final static int RESUME_LIST = 1;
    public final static int MODIFY_LIST = 2;
    public final static int HISTORY_LIST = 3;
    public final static int AUTHORIZED_LIST = 4;
    public final static int PRESET_ROW_DELAY_TIME = 50;
    public final static int PRESET_HOLE_DELAY_TIME = 10;
    public final static int PRESET_HOLE_INSIDE_DELAY_TIME = 0;
    public final static int PRESET_SECTION_DELAY_TIME = 50;
    public final static int PRESET_SECTION_INSIDE_DELAY_TIME = 0;
    public final static int REFRESH_STATUS_BAR_PERIOD = 1000;
    public final static int MAX_DELAY_TIME = 6000;
    public final static int SELF_TEST_TIME = 6600;
    public final static int READ_VOLTAGE_TIMEOUT = 500;
    public final static int BOOST_TIME = 600;
    public final static int MAXIMUM_CURRENT = 30000;
    public final static int MINIMUM_VOLTAGE = 600;
    public final static int FAST_DETECT_TIMEOUT = 1000;
    public final static int UPLOAD_TIMEOUT = 10000;
    public final static int RESEND_TIMES = 3;
    public final static int SCAN_TIMEOUT = 3000;
    public final static int MAX_VOLUME = 5;
    public final static int INSERT_HOLE = 1;
    public final static int PRESET_DELAY = 100;
    public final static int EXPLODE_TIMES = 3;
    public final static int INSERT_INSIDE = 2; //1:孔间插入 2.孔内插入
    public final static int ERROR_RESULT_OPEN_FAIL = 1;
    public final static int ERROR_RESULT_SHORT_CIRCUIT = 2;
    private final static boolean newLG = BaseApplication.readSettings().isNewLG();
    public final static int RESEND_CMD_TIMEOUT = newLG ? 100 : 800;
    public final static int INITIAL_TIME = newLG ? 2000 : 8000;

    public enum LIST_TYPE {
        ALL,
        DETECTED,
        NOT_FOUND,
        ERROR,
        END
    }
}
