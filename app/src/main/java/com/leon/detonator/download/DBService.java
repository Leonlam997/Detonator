package com.leon.detonator.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.leon.detonator.base.BaseApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBService {
    private final DbOperateHelper dbHelper;

    public DBService(Context context) {
        dbHelper = new DbOperateHelper(context);
    }

    /**
     * 获得指定URI的每条线程已经下载的文件长度
     */
    public List<DWManagerInfo> getData(String downloadPath) {
        //获得可读数据库句柄,通常内部实现返回的其实都是可写的数据库句柄
        //根据下载的路径查询所有现场的下载数据,返回的Cursor指向第一条记录之前
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select thread_id, download_length from " + DbOperateHelper.DB_TABLE + " where download_path=?",
                new String[]{downloadPath});
        List<DWManagerInfo> data = new ArrayList<>();
        try {
            //从第一条记录开始遍历Cursor对象
            //cursor.moveToFirst();
            while (cursor.moveToNext()) {
                DWManagerInfo dwInfo = new DWManagerInfo();
                dwInfo.setThreadId(cursor.getInt(cursor.getColumnIndexOrThrow("thread_id")));
                dwInfo.setDownloadLength(cursor.getInt(cursor.getColumnIndexOrThrow("download_length")));
                data.add(dwInfo);
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        } finally {
            try {
                cursor.close();//关闭cursor,释放资源;
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            try {
                db.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        return data;
    }

    /**
     * 保存每条线程已经下载的文件长度
     */
    public void save(String downloadPath, Map<Integer, Integer> map) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            //使用增强for循环遍历数据集合
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                db.execSQL("insert into " + DbOperateHelper.DB_TABLE + "(download_path, thread_id, download_length) values(?,?,?)",
                        new Object[]{downloadPath, entry.getKey(), entry.getValue()});
            }
            //设置一个事务成功的标志,如果成功就提交事务,如果没调用该方法的话那么事务回滚
            //就是上面的数据库操作撤销
            db.setTransactionSuccessful();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        } finally {
            //结束一个事务
            db.endTransaction();
            try {
                db.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

    public void updateItem(DWManagerInfo dwInfo) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues newValues = new ContentValues();
            newValues.put("download_length", dwInfo.getDownloadLength());
            newValues.put("thread_id", dwInfo.getThreadId());
            newValues.put("download_path", dwInfo.getDownloadPath());
            db.update(DbOperateHelper.DB_TABLE, newValues, "thread_id=? and download_path=?", new String[]{dwInfo.getThreadId() + "", dwInfo.getDownloadPath()});
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new Exception("update item error: " + e.getMessage(), e);
        } finally {
            try {
                db.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

    public void delete(String path) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.delete(DbOperateHelper.DB_TABLE, "download_path=?", new String[]{path});
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        } finally {
            try {
                db.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

    public long addItem(DWManagerInfo dwInfo) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues newValues = new ContentValues();
            newValues.put("download_path", dwInfo.getDownloadPath());
            newValues.put("thread_id", dwInfo.getThreadId());
            newValues.put("download_length", dwInfo.getDownloadLength());
            return db.insert(DbOperateHelper.DB_TABLE, null, newValues);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new Exception(">>>>>>addItem into db error: " + e.getMessage(), e);
        } finally {
            try {
                db.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }
}
