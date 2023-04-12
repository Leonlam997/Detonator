package com.leon.detonator.util;

import android.os.Environment;

public class FilePath {
    public final static String APP_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Detonator";
    public final static String FILE_OPEN_AIR_DELAY_LIST = APP_PATH + "/OpenAir.lst";
    public final static String FILE_TUNNEL_DELAY_LIST = APP_PATH + "/Tunnel.lst";
    public final static String FILE_OFFLINE_LIST = APP_PATH + "/Offline.lst";
    public final static String FILE_OFFLINE_DOWNLOAD_LIST = APP_PATH + "/Offline.dnl";
    public final static String FILE_ONLINE_DOWNLOAD_LIST = APP_PATH + "/Online.dnl";
    public final static String FILE_OPEN_AIR_DETECT_LIST = APP_PATH + "/O_Detect.lst";
    public final static String FILE_OPEN_AIR_NOT_FOUND_LIST = APP_PATH + "/O_NotFound.lst";
    public final static String FILE_OPEN_AIR_ERROR_LIST = APP_PATH + "/O_Error.lst";
    public final static String FILE_OPEN_AIR_SELECTED_LIST = APP_PATH + "/O_Selected.lst";
    public final static String FILE_TUNNEL_DETECT_LIST = APP_PATH + "/T_Detect.lst";
    public final static String FILE_TUNNEL_NOT_FOUND_LIST = APP_PATH + "/T_NotFound.lst";
    public final static String FILE_TUNNEL_ERROR_LIST = APP_PATH + "/T_Error.lst";
    public final static String FILE_TUNNEL_SELECTED_LIST = APP_PATH + "/T_Selected.lst";
    public final static String FILE_SCHEME_LIST = APP_PATH + "/Scheme.lst";
    public final static String FILE_SCHEME_PATH = APP_PATH + "/Schemes";
    public final static String[][] FILE_LIST = {{FILE_TUNNEL_DELAY_LIST, FILE_TUNNEL_DETECT_LIST, FILE_TUNNEL_NOT_FOUND_LIST, FILE_TUNNEL_ERROR_LIST, FILE_TUNNEL_SELECTED_LIST},
            {FILE_OPEN_AIR_DELAY_LIST, FILE_OPEN_AIR_DETECT_LIST, FILE_OPEN_AIR_NOT_FOUND_LIST, FILE_OPEN_AIR_ERROR_LIST, FILE_OPEN_AIR_SELECTED_LIST}};
    public final static String FILE_USER_INFO = APP_PATH + "/Users.dat";
    public final static String FILE_ENTERPRISE_INFO = APP_PATH + "/Enterprise.dat";
    public final static String FILE_BAI_SE_DATA = APP_PATH + "/BaiSeDataActivity.dat";
    public final static String FILE_BAI_SE_CHECK = APP_PATH + "/BaiSeCheck.dat";
    public final static String FILE_PROJECT_INFO = APP_PATH + "/Project";
    public final static String FILE_DEBUG_LOG = APP_PATH + "/Debug.log";
    public final static String FILE_SERIAL_LOG = APP_PATH + "/Serial.log";
    public final static String FILE_TEMP_LOG = FilePath.APP_PATH + "/temp.log";
    public final static String FILE_CAMERA_CAPTURE = APP_PATH + "/CAMERA/";
    public final static String FILE_LOCAL_SETTINGS = APP_PATH + "/LocalSettings.dat";
    public final static String FILE_DETONATE_RECORDS = APP_PATH + "/Records";
    public final static String FILE_UPLOAD_LIST = APP_PATH + "/Upload.lst";
    public final static String FILE_UPDATE_PATH = APP_PATH + "/Version";
    public final static String FILE_UPDATE_APK = FILE_UPDATE_PATH + "/%s.apk";
}
