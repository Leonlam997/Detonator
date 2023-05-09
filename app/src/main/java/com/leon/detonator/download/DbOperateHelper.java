package com.leon.detonator.download;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbOperateHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "dw_manager.db";
    public static final String DB_TABLE = "dw_info";
    private static final int DB_VERSION = 1;

    public DbOperateHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    private static final String DB_CREATE = "create table " + DB_TABLE + " ("
            + "dw_id Integer primary key autoincrement,"
            + "download_path varchar,"
            + "thread_id Integer,"
            + "download_length Integer);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int _oldVersion, int _newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
        onCreate(db);
    }
}
