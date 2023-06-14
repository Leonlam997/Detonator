package com.leon.detonator.database;

public class DatabaseStatic {
    public final static int DATABASE_VERSION = 1;

    //延期方案
    public static class Scheme {
        public final static String ID = "_id";
        public final static String NAME = "name";
        public final static String CREATE_TIME = "create_time";
        public final static String TUNNEL = "tunnel";
        public final static String AMOUNT = "amount";
        public final static String SELECTED = "selected";
        public final static String PROJECT_ID = "project_id";
        public final static String BLASTER_ID = "blaster_id";
        public final static String SYNCHRONIZE = "synchronize";
        public final static String UPLOAD_SERVER = "upload_server";
        public final static String EXPLODE_TIME = "explode_time";
        public final static String UPLOAD_TIME = "upload_time";
        public final static String LATITUDE = "latitude";
        public final static String LONGITUDE = "longitude";
        public final static String DELETED = "deleted";
        public final static int COL_ID = 0;
        public final static int COL_NAME = 1;
        public final static int COL_CREATE_TIME = 2;
        public final static int COL_TUNNEL = 3;
        public final static int COL_AMOUNT = 4;
        public final static int COL_SELECTED = 5;
        public final static int COL_PROJECT_ID = 6;
        public final static int COL_BLASTER_ID = 7;
        public final static int COL_SYNCHRONIZE = 8;
        public final static int COL_UPLOAD_SERVER = 9;
        public final static int COL_EXPLODE_TIME = 10;
        public final static int COL_UPLOAD_TIME = 11;
        public final static int COL_LATITUDE = 12;
        public final static int COL_LONGITUDE = 13;
        public final static int COL_DELETED = 14;
    }

    //雷管
    public static class Detonator {
        public final static String ID = "_id";
        public final static String SHELL = "shell";
        public final static String UID = "uid";
        public final static String PASSWORD = "password";
        public final static String DELAY_TIME = "delay_time";
        public final static String ROW = "row";
        public final static String HOLE = "hole";
        public final static String INSIDE = "inside";
        public final static String DOWNLOADED = "downloaded";
        public final static int COL_ID = 0;
        public final static int COL_SHELL = 1;
        public final static int COL_UID = 2;
        public final static int COL_PASSWORD = 3;
        public final static int COL_DELAY_TIME = 4;
        public final static int COL_ROW = 5;
        public final static int COL_HOLE = 6;
        public final static int COL_INSIDE = 7;
        public final static int COL_DOWNLOADED = 8;
    }

    //企业信息
    public static class Enterprise {
        public final static String ID = "_id";
        public final static String ENTERPRISE_CODE = "enterprise_code";
        public final static String BLASTER_ID_CARD = "blaster_id";
        public final static String COMMERCIAL = "commercial";
        public final static String CONTRACT_CODE = "contract_code";
        public final static String PROJECT_CODE = "project_code";
        public final static String SELECTED = "selected";
        public final static int COL_ID = 0;
        public final static int COL_ENTERPRISE_CODE = 1;
        public final static int COL_BLASTER_ID_CARD = 2;
        public final static int COL_COMMERCIAL = 3;
        public final static int COL_CONTRACT_CODE = 4;
        public final static int COL_PROJECT_CODE = 5;
        public final static int COL_SELECTED = 6;
    }

    //丹灵网下载
    public static class DanLing {
        public final static String ID = "_id";
        public final static String APPLICATION_TIME = "application_time";
        public final static String ERROR_CODE = "error_code";
        public final static String OFFLINE = "offline";
        public final static String USED = "used";
        public final static int COL_ID = 0;
        public final static int COL_APPLICATION_TIME = 1;
        public final static int COL_ERROR_CODE = 2;
        public final static int COL_OFFLINE = 3;
        public final static int COL_USED = 4;
    }

    //丹灵网下载起爆器列表
    public static class DownloadedExploder {
        public final static String ID = "_id";
        public final static String DAN_LING_ID = "dan_ling_id";
        public final static String EXPLODER_SN = "exploder_sn";
        public final static int COL_ID = 0;
        public final static int COL_DAN_LING_ID = 1;
        public final static int COL_EXPLODER_SN = 2;
    }

    //丹灵网下载准爆区域列表
    public static class AllowedArea {
        public final static String ID = "_id";
        public final static String DAN_LING_ID = "dan_ling_id";
        public final static String NAME = "name";
        public final static String LATITUDE = "latitude";
        public final static String LONGITUDE = "longitude";
        public final static String RADIUS = "radius";
        public final static String START_TIME = "start_time";
        public final static String END_TIME = "end_time";
        public final static int COL_ID = 0;
        public final static int COL_DAN_LING_ID = 1;
        public final static int COL_NAME = 2;
        public final static int COL_LATITUDE = 3;
        public final static int COL_LONGITUDE = 4;
        public final static int COL_RADIUS = 5;
        public final static int COL_START_TIME = 6;
        public final static int COL_END_TIME = 7;
    }

    //丹灵网下载准爆区域列表
    public static class ForbiddenArea {
        public final static String ID = "_id";
        public final static String DAN_LING_ID = "dan_ling_id";
        public final static String LATITUDE = "latitude";
        public final static String LONGITUDE = "longitude";
        public final static String RADIUS = "radius";
        public final static String START_TIME = "start_time";
        public final static String END_TIME = "end_time";
        public final static int COL_ID = 0;
        public final static int COL_DAN_LING_ID = 1;
        public final static int COL_LATITUDE = 2;
        public final static int COL_LONGITUDE = 3;
        public final static int COL_RADIUS = 4;
        public final static int COL_START_TIME = 5;
        public final static int COL_END_TIME = 6;
    }

    //丹灵网下载雷管列表
    public static class DownloadedDetonator {
        public final static String ID = "_id";
        public final static String DAN_LING_ID = "dan_ling_id";
        public final static String OFFLINE_SN = "offline_sn";
        public final static String UID = "uid";
        public final static String WORK_ID = "work_id";
        public final static String VALID_TIME = "valid_time";
        public final static String ERROR_CODE = "error_code";
        public final static int COL_ID = 0;
        public final static int COL_DAN_LING_ID = 1;
        public final static int COL_OFFLINE_SN = 2;
        public final static int COL_UID = 3;
        public final static int COL_WORK_ID = 4;
        public final static int COL_VALID_TIME = 5;
        public final static int COL_ERROR_CODE = 6;
    }

    //百色智慧民爆人员检测
    public static class BaiSeBlaster {
        public final static String ID = "_id";
        public final static String NAME = "name";
        public final static String CHECKED = "checked";
        public final static String DEVICE_NO = "device_no";
        public final static String GPS_COORDINATE_SYSTEMS = "gps_coordinate_systems";
        public final static String LNG_LAT = "lng_lat";
        public final static String ID_CARD = "id_card";
        public final static String PROJECT_CODE = "project_code";
        public final static String APP_VERSION = "app_version";
        public final static String PROJECT = "project";
        public final static String SELECTED = "selected";
        public final static int COL_ID = 0;
        public final static int COL_NAME = 1;
        public final static int COL_CHECKED = 2;
        public final static int COL_DEVICE_NO = 3;
        public final static int COL_GPS_COORDINATE_SYSTEMS = 4;
        public final static int COL_LNG_LAT = 5;
        public final static int COL_ID_CARD = 6;
        public final static int COL_PROJECT_CODE = 7;
        public final static int COL_APP_VERSION = 8;
        public final static int COL_PROJECT = 9;
        public final static int COL_SELECTED = 10;
    }

    //百色智慧民爆企业信息
    public static class BaiSeProject {
        public final static String ID = "_id";
        public final static String PROJECT_CODE = "project_code";
        public final static String PROJECT_NAME = "project_name";
        public final static String PROJECT_TYPE = "project_type";
        public final static String BURST_ORG_NAME = "burst_org_name";
        public final static String BURST_ORG_CODE = "burst_org_code";
        public final static String DEVICE_NO = "device_no";
        public final static String BURST_TIME = "burst_time";
        public final static String ID_CARD = "id_card";
        public final static String BLASTER_NAME = "blaster_name";
        public final static String DETONATOR_COUNT = "detonator_count";
        public final static String OFFLINE = "offline";
        public final static String OFFLINE_TIME = "offline_time";
        public final static String LNG_LAT = "lng_lat";
        public final static String GPS_COORDINATE_SYSTEMS = "gps_coordinate_systems";
        public final static String SELECTED = "selected";
        public final static int COL_ID = 0;
        public final static int COL_PROJECT_CODE = 1;
        public final static int COL_PROJECT_NAME = 2;
        public final static int COL_PROJECT_TYPE = 3;
        public final static int COL_BURST_ORG_NAME = 4;
        public final static int COL_BURST_ORG_CODE = 5;
        public final static int COL_DEVICE_NO = 6;
        public final static int COL_BURST_TIME = 7;
        public final static int COL_ID_CARD = 8;
        public final static int COL_BLASTER_NAME = 9;
        public final static int COL_DETONATOR_COUNT = 10;
        public final static int COL_OFFLINE = 11;
        public final static int COL_OFFLINE_TIME = 12;
        public final static int COL_LNG_LAT = 13;
        public final static int COL_GPS_COORDINATE_SYSTEMS = 14;
        public final static int COL_SELECTED = 15;
    }

    public final static String TABLE_SCHEME = "scheme";
    public final static String TABLE_DETONATOR = "detonator";
    public final static String TABLE_ENTERPRISE = "enterprise";
    public final static String TABLE_DAN_LING = "dan_ling";
    public final static String TABLE_DOWNLOADED_EXPLODER = "downloaded_exploder";
    public final static String TABLE_ALLOWED_AREA = "allowed_area";
    public final static String TABLE_FORBIDDEN_AREA = "forbidden_area";
    public final static String TABLE_DOWNLOADED_DETONATOR = "downloaded_detonator";
    public final static String TABLE_BAI_SE_BLASTER = "bai_se_blaster";
    public final static String TABLE_BAI_SE_PROJECT = "bai_se_project";
    public final static String TABLE_AUTH_DETONATOR = "auth_detonator";

    public final static String CREATE_TABLE_SCHEME = "create table " + TABLE_SCHEME + "(" +
            Scheme.ID + " integer primary key autoincrement, " +
            Scheme.NAME + " varchar(20) not null, " +
            Scheme.CREATE_TIME + " datetime not null, " +
            Scheme.TUNNEL + " tinyint(1) not null, " +
            Scheme.AMOUNT + " integer, " +
            Scheme.SELECTED + " tinyint(1) not null, " +
            Scheme.PROJECT_ID + " integer, " +
            Scheme.BLASTER_ID + " integer, " +
            Scheme.SYNCHRONIZE + " tinyint(1) not null, " +
            Scheme.UPLOAD_SERVER + " tinyint not null, " +
            Scheme.EXPLODE_TIME + " datetime, " +
            Scheme.UPLOAD_TIME + " datetime, " +
            Scheme.LATITUDE + " float, " +
            Scheme.LONGITUDE + " float, " +
            Scheme.DELETED + " tinyint(1))";

    public final static String CREATE_TABLE_DETONATOR = "create table %s (" +
            Detonator.ID + " Integer primary key autoincrement, " +
            Detonator.SHELL + " varchar(20) not null, " +
            Detonator.UID + " varchar(20), " +
            Detonator.PASSWORD + " varchar(20), " +
            Detonator.DELAY_TIME + " integer, " +
            Detonator.ROW + " integer, " +
            Detonator.HOLE + " integer, " +
            Detonator.INSIDE + " integer, " +
            Detonator.DOWNLOADED + " tinyint(1))";

    public final static String CREATE_TABLE_ENTERPRISE = "create table " + TABLE_ENTERPRISE + "(" +
            Enterprise.ID + " integer primary key autoincrement, " +
            Enterprise.ENTERPRISE_CODE + " varchar(50), " +
            Enterprise.BLASTER_ID_CARD + " varchar(20), " +
            Enterprise.COMMERCIAL + " tinyint(1), " +
            Enterprise.CONTRACT_CODE + " varchar(50), " +
            Enterprise.PROJECT_CODE + " varchar(50), " +
            Enterprise.SELECTED + " tinyint(1))";

    public final static String CREATE_TABLE_DAN_LING = "create table " + TABLE_DAN_LING + "(" +
            DanLing.ID + " integer primary key autoincrement, " +
            DanLing.APPLICATION_TIME + " datetime, " +
            DanLing.ERROR_CODE + " varchar(10), " +
            DanLing.OFFLINE + " tinyint(1) not null, " +
            DanLing.USED + " tinyint(1) not null)";

    public final static String CREATE_TABLE_DOWNLOADED_EXPLODER = "create table " + TABLE_DOWNLOADED_EXPLODER + "(" +
            DownloadedExploder.ID + " integer primary key autoincrement, " +
            AllowedArea.DAN_LING_ID + " integer, " +
            DownloadedExploder.EXPLODER_SN + " varchar(50))";

    public final static String CREATE_TABLE_ALLOWED_AREA = "create table " + TABLE_ALLOWED_AREA + "(" +
            AllowedArea.ID + " integer primary key autoincrement, " +
            AllowedArea.DAN_LING_ID + " integer, " +
            AllowedArea.NAME + " varchar(50), " +
            AllowedArea.LATITUDE + " float, " +
            AllowedArea.LONGITUDE + " float, " +
            AllowedArea.RADIUS + " float, " +
            AllowedArea.START_TIME + " datetime, " +
            AllowedArea.END_TIME + " datetime)";

    public final static String CREATE_TABLE_FORBIDDEN_AREA = "create table " + TABLE_FORBIDDEN_AREA + "(" +
            ForbiddenArea.ID + " integer primary key autoincrement, " +
            ForbiddenArea.DAN_LING_ID + " integer, " +
            ForbiddenArea.LATITUDE + " float, " +
            ForbiddenArea.LONGITUDE + " float, " +
            ForbiddenArea.RADIUS + " float, " +
            ForbiddenArea.START_TIME + " datetime, " +
            ForbiddenArea.END_TIME + " datetime)";

    public final static String CREATE_TABLE_DOWNLOADED_DETONATOR = "create table " + TABLE_DOWNLOADED_DETONATOR + "(" +
            DownloadedDetonator.ID + " integer primary key autoincrement, " +
            DownloadedDetonator.DAN_LING_ID + " integer, " +
            DownloadedDetonator.OFFLINE_SN + " varchar(20), " +
            DownloadedDetonator.UID + " varchar(20), " +
            DownloadedDetonator.WORK_ID + " varchar(20), " +
            DownloadedDetonator.VALID_TIME + " datetime, " +
            DownloadedDetonator.ERROR_CODE + " varchar(10))";

    public final static String CREATE_TABLE_BAI_SE_BLASTER = "create table " + TABLE_BAI_SE_BLASTER + "(" +
            BaiSeBlaster.ID + " integer primary key autoincrement, " +
            BaiSeBlaster.NAME + " varchar(20), " +
            BaiSeBlaster.CHECKED + " tinyint(1), " +
            BaiSeBlaster.DEVICE_NO + " varchar(20), " +
            BaiSeBlaster.GPS_COORDINATE_SYSTEMS + " varchar(10), " +
            BaiSeBlaster.LNG_LAT + " varchar(20), " +
            BaiSeBlaster.ID_CARD + " varchar(20), " +
            BaiSeBlaster.PROJECT_CODE + " varchar(50), " +
            BaiSeBlaster.APP_VERSION + " varchar(10), " +
            BaiSeBlaster.PROJECT + " tinyint(1), " +
            BaiSeBlaster.SELECTED + " tinyint(1))";

    public final static String CREATE_TABLE_BAI_SE_PROJECT = "create table " + TABLE_BAI_SE_PROJECT + "(" +
            BaiSeProject.ID + " integer primary key autoincrement, " +
            BaiSeProject.PROJECT_CODE + " varchar(50), " +
            BaiSeProject.PROJECT_NAME + " varchar(50), " +
            BaiSeProject.PROJECT_TYPE + " varchar(50), " +
            BaiSeProject.BURST_ORG_NAME + " varchar(50), " +
            BaiSeProject.BURST_ORG_CODE + " varchar(50), " +
            BaiSeProject.DEVICE_NO + " varchar(20), " +
            BaiSeProject.BURST_TIME + " datetime, " +
            BaiSeProject.ID_CARD + " varchar(20), " +
            BaiSeProject.BLASTER_NAME + " varchar(20), " +
            BaiSeProject.DETONATOR_COUNT + " integer, " +
            BaiSeProject.OFFLINE + " tinyint(1), " +
            BaiSeProject.OFFLINE_TIME + " float, " +
            BaiSeProject.LNG_LAT + " varchar(20), " +
            BaiSeProject.GPS_COORDINATE_SYSTEMS + " varchar(10), " +
            BaiSeProject.SELECTED + " tinyint(1))";
}