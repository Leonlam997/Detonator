package com.leon.detonator.database;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.FilePath;

public class MyHelper extends SQLiteOpenHelper {
    private BaseApplication myApp;

    public MyHelper(@Nullable Context context) {
        super(context, FilePath.FILE_DATABASE, null, DatabaseStatic.DATABASE_VERSION);
        if (context != null)
            myApp = (BaseApplication) ((Activity) context).getApplication();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseStatic.CREATE_TABLE_SCHEME);
        db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_AUTH_DETONATOR));
        db.execSQL(DatabaseStatic.CREATE_TABLE_ENTERPRISE);
        db.execSQL(DatabaseStatic.CREATE_TABLE_DAN_LING);
        db.execSQL(DatabaseStatic.CREATE_TABLE_DOWNLOADED_EXPLODER);
        db.execSQL(DatabaseStatic.CREATE_TABLE_ALLOWED_AREA);
        db.execSQL(DatabaseStatic.CREATE_TABLE_FORBIDDEN_AREA);
        db.execSQL(DatabaseStatic.CREATE_TABLE_DOWNLOADED_DETONATOR);
        db.execSQL(DatabaseStatic.CREATE_TABLE_BAI_SE_BLASTER);
        db.execSQL(DatabaseStatic.CREATE_TABLE_BAI_SE_PROJECT);
        myApp.importOldData();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
