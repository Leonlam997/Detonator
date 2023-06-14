package com.leon.detonator.util;

import android.os.Environment;

public class FilePath {
    public final static String APP_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Detonator";
    public static final String FILE_DATABASE = APP_PATH + "/data.db";
    public final static String FILE_OPEN_AIR_DELAY_LIST = APP_PATH + "/OpenAir.lst";
    public final static String FILE_TUNNEL_DELAY_LIST = APP_PATH + "/Tunnel.lst";
    public final static String FILE_DEBUG_LOG = APP_PATH + "/Debug.log";
    public final static String FILE_SERIAL_LOG = APP_PATH + "/Serial.log";
    public final static String FILE_TEMP_LOG = APP_PATH + "/temp.log";
    public final static String FILE_LOCAL_SETTINGS = APP_PATH + "/LocalSettings.dat";
    public final static String FILE_UPDATE_PATH = APP_PATH + "/Version";
    public final static String FILE_UPDATE_APK = FILE_UPDATE_PATH + "/%s.apk";
}
