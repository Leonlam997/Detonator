package com.leon.detonator.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.leon.detonator.util.FilePath;

public class MyHelper extends SQLiteOpenHelper {

    public MyHelper(@Nullable Context context) {
        super(context, FilePath.FILE_DATABASE, null, DatabaseStatic.DATABASE_VERSION);
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE " + DatabaseStatic.TABLE_DAN_LING + " ADD COLUMN " + DatabaseStatic.DanLing.USED + " tinyint(1) not null default 0");
                db.execSQL("ALTER TABLE " + DatabaseStatic.TABLE_DAN_LING + " ADD COLUMN " + DatabaseStatic.DanLing.ENTERPRISE_ID + " integer");
                db.execSQL("ALTER TABLE " + DatabaseStatic.TABLE_DOWNLOADED_DETONATOR + " ADD COLUMN " + DatabaseStatic.DownloadedDetonator.SCHEME_ID + " integer default -1");
                db.execSQL("ALTER TABLE " + DatabaseStatic.TABLE_SCHEME + " ADD COLUMN " + DatabaseStatic.Scheme.DELETED + " tinyint(1) not null default 0");
                db.execSQL("ALTER TABLE " + DatabaseStatic.TABLE_ENTERPRISE + " ADD COLUMN " + DatabaseStatic.Enterprise.DELETED + " tinyint(1) not null default 0");
        }
    }
}
