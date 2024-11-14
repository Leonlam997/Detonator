package com.leon.detonator.util;

import com.leon.detonator.base.BaseApplication;

/**
 * Created by Leon on 2018/1/24.
 */

public class ConstantUtils {
    public final static String HOST_URL = "http://www.zhongbao360.com/open_service_v1";
    //         public final static String HOST_URL = "http://192.168.0.2/open_service_v1";
    public final static String VERSION_URL = "http://www.zhongbao360.com/File/Client/General/update.json";
    public final static String UPLOAD_LOG_URL = "http://www.zhongbao360.com/Exploder/UploadLog";
    //    public final static String BAI_SE_UPLOAD_URL = "http://test.99mb.net:810/api/MbSystem/pda/uploadBlastRecord";
//    public final static String BAI_SE_CHECK_URL = "http://test.99mb.net:810/api/MbSystem/pda/checkAllowDetonate";
    public final static String BAI_SE_UPLOAD_URL = "https://app.99mb.net/api/MbSystem/pda/uploadBlastRecord";
    public final static String BAI_SE_CHECK_URL = "https://app.99mb.net/api/MbSystem/pda/checkAllowDetonate";
    //    public final static String DAN_LIN_DOWNLOAD_URL = "http://qq.mbdzlg.com/mbdzlgtxzx/servlet/DzlgMmlxxzJsonServlert";
//    public final static String DAN_LIN_UPLOAD_URL = "http://qq.mbdzlg.com/mbdzlgtxzx/servlet/DzlgSysbJsonServlert";
    public final static String DAN_LIN_DOWNLOAD_URL = "http://test.mbdzlg.com/mbdzlgtxzx/servlet/DzlgMmlxxzJsonServlert";
    public final static String DAN_LIN_UPLOAD_URL = "http://test.mbdzlg.com/mbdzlgtxzx/servlet/DzlgSysbJsonServlert";
    public final static String ACCESS_TOKEN = "B23294542Z3A0990AB837C2C";
    public final static String[][] UPLOAD_HOST = {{"丹灵网", ""},
            {"广西民爆", "119.29.111.172:6088"},
            {"智慧民爆", "119.29.111.172:6088"},
            {"丹灵+民爆", "119.29.111.172:6088"},
            {"黔南民爆", "113.140.1.135:9903"},
            {"黔东南民爆", "113.140.1.137:8608"},
            {"贵阳民爆", "119.29.111.172:6089"},
            {"贵安民爆", "113.140.1.137:8610"}};
    public final static String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
    public final static String DATE_FORMAT_PART = "MM-dd HH:mm:ss";
    public final static String INPUT_DETONATOR_ACCEPT = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";
    public final static String INPUT_ID_ACCEPT = "0123456789.X";
    public final static String EXPLODE_PSW = "772298";
    public final static String SHELL_PATTERN = "\\d{7}[0-9A-Z]\\d{5}$";
    public final static String UID_PATTERN = "\\d[0-9A-Z]\\d{5}$";
    public final static String ID_PATTERN = "(^\\d{18}$)|(^\\d{17}(\\d|X)$)";
    public final static String BOX_CODE_PATTERN = "^[a-zA-Z]\\d{2}[0-9A-Z][a-zA-Z]\\d{9}[0-9a-zA-Z]\\d{3}$";
    public final static String TRUNK_CODE_PATTERN = "^[a-zA-Z]\\d{2}[0-9A-Z][a-zA-Z]\\d{2}[0-9A-Z]\\d{9}$";
    public final static String ENTERPRISE_PROJECT = "Project";
    public final static String ENTERPRISE_CONTRACT = "Contract";
    public final static String GPS_SYSTEM = "BD09";
    public final static String NULL_ID = "1234567890123";
    public final static String BT_RESEND_ACK = "Resend";
    public final static String BT_SUCCESS_ACK = "Success";
    public final static int ITEM_TEXT_SIZE = 20;
    public final static int DIALOG_BUTTON_TEXT_SIZE = 20;
    public final static int RESUME_LIST = 1;
    public final static int MODIFY_LIST = 2;
    public final static int HISTORY_LIST = 3;
    public final static int AUTHORIZED_LIST = 4;
    public final static int INFO_ENTERPRISE = 0;
    public final static int INFO_PROJECT = 1;
    public final static int INFO_BLASTER = 2;
    public final static int REFRESH_STATUS_BAR_PERIOD = 1000;
    public final static int MAX_DELAY_TIME = 16000;
    public final static int MAX_ROW_NUMBER = 999;
    public final static int MAX_AMOUNT_PER_BOX = 100;
    public final static int UPLOAD_TIMEOUT = 10000;
    public final static int INSERT_HOLE = 1;
    public final static int INSERT_INSIDE = 2; //1:孔间插入 2.孔内插入
    public final static int ERROR_RESULT_OPEN_FAIL = 1;
    public final static int ERROR_RESULT_SHORT_CIRCUIT = 2;
    public final static int RESEND_CMD_TIMEOUT = BaseApplication.isRemote() ? 3000 : 500;
    public final static int RESEND_STATUS_TIMEOUT = BaseApplication.isRemote() ? 3000 : 500;
    public final static int RESEND_READ_FIELD_CMD_TIMEOUT = 1000;
    public final static int RESEND_SCAN_UID_TIMEOUT = 1500;
    public final static int COMMAND_DELAY_TIME = 10;
    public final static int RELEASE_DELAY_TIME = 20;
    public final static int RESET_DELAY_TIME = 40;
    public final static int SCAN_DELAY_TIME = 100;
    public final static int LOCK_DELAY_TIME = 100;
    public final static int INITIAL_TIME = 500;
    public final static int CHECK_CAPACITY_TIME = 5500;
    public final static int UID_LEN = 7;
    public final static int PSW_LEN = 6;
    public final static int DETONATOR_VERSION = 1;
    public final static int CHECK_CAPACITY_LEVEL = 0x1;
    public final static int SCAN_CODE_TIME = 3000;
    public final static int RESEND_SCAN_TIMEOUT = 3500;
    public final static int SCAN_ZERO_COUNT = 5;
    public final static int SHORT_CIRCUIT_CURRENT = 40000;
    public final static int CURRENT_DETECT_COUNT = 2;
    public final static int CURRENT_PER_DETONATOR = 30;
    public final static int CURRENT_BREAK_CIRCUIT = 10;
    public final static float CURRENT_OVER_PERCENTAGE = 2.5f;
    public final static int KEYCODE_CENTER_SCAN = 287;
    public final static int KEYCODE_LEFT_SCAN = 288;
    public final static int KEYCODE_RIGHT_SCAN = 289;
    public final static int KEYCODE_LEFT_CHARGE = 293;
    public final static int KEYCODE_RIGHT_CHARGE = 294;
    //    public final static int KEYCODE_ADD = 291;
//    public final static int KEYCODE_SUB = 290;
    public final static int KEYCODE_ADD = 66;
    public final static int KEYCODE_SUB = 67;
    public final static int HIDE_TEST_COUNT = 5;
    public final static int BT_CONNECTED = 1;
    public final static int BT_DATA = 2;
    public final static int BT_RESEND = 3;
    public final static int BT_ERROR = 4;
    public final static int APP_TOAST = 5;
    public final static int FAST_CLICK_DELAY_TIME = 1000;
    public final static int BT_RESEND_TIMEOUT = 100;
    public final static int LONG_PRESS_TIME = 1000;

    public enum ListType {
        ALL,
        DETECTED,
        NOT_FOUND,
        ERROR,
        NONE
    }
}
