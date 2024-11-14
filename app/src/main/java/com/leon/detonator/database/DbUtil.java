package com.leon.detonator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.DanLinDetonatorBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.JbqyBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bean.ZbqyBean;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DbUtil {
    private static MyHelper myHelper;

    public static void initHelper(Context context) {
        myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        db.close();
    }

    private static boolean isTableExist(String name) {
        if (name == null)
            return false;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        String sql = "select count(*) from sqlite_master where type ='table' and name ='" + name + "';";
        boolean result = false;
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToNext())
            result = cursor.getInt(0) > 0;
        cursor.close();
        return result;
    }

    public static List<SchemeBean> getSchemeList() {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        List<SchemeBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.TUNNEL + "=? and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
        while (cursor.moveToNext()) {
            SchemeBean bean = new SchemeBean();
            bean.setId(cursor.getInt(DatabaseStatic.Scheme.COL_ID));
            bean.setName(cursor.getString(DatabaseStatic.Scheme.COL_NAME));
            try {
                if (cursor.getString(DatabaseStatic.Scheme.COL_CREATE_TIME) != null)
                    bean.setCreateTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_CREATE_TIME)));
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            bean.setAmount(cursor.getInt(DatabaseStatic.Scheme.COL_AMOUNT));
            bean.setSelected(cursor.getInt(DatabaseStatic.Scheme.COL_SELECTED) == 1);
            list.add(bean);
        }
        cursor.close();
        db.close();
        return list;
    }

    public static SchemeBean getScheme(long id) {
        SchemeBean bean = new SchemeBean();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.ID + "=?", new String[]{id + ""}, null, null, null);
        if (cursor.moveToNext()) {
            bean.setId(cursor.getInt(DatabaseStatic.Scheme.COL_ID));
            bean.setName(cursor.getString(DatabaseStatic.Scheme.COL_NAME));
            try {
                SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
                if (cursor.getString(DatabaseStatic.Scheme.COL_CREATE_TIME) != null)
                    bean.setCreateTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_CREATE_TIME)));
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            bean.setAmount(cursor.getInt(DatabaseStatic.Scheme.COL_AMOUNT));
            bean.setSelected(cursor.getInt(DatabaseStatic.Scheme.COL_SELECTED) == 1);
        }
        cursor.close();
        db.close();
        return bean;
    }

    public static long[] getSelectedSchemeId() {
        List<Long> ids = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.SELECTED + "=1 and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.TUNNEL + "=?", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
        while (cursor.moveToNext())
            ids.add(cursor.getLong(DatabaseStatic.Scheme.COL_ID));
        cursor.close();
        db.close();
        long[] result = new long[ids.size()];
        int i = 0;
        for (Long l : ids)
            result[i++] = l;
        return result;
    }

    public static List<SchemeBean> getCurrentSchemeList() {
        List<SchemeBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.SELECTED + "=1 and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.TUNNEL + "=?", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
        while (cursor.moveToNext()) {
            SchemeBean bean = new SchemeBean();
            bean.setId(cursor.getLong(DatabaseStatic.Scheme.COL_ID));
            bean.setName(cursor.getString(DatabaseStatic.Scheme.COL_NAME));
            try {
                SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
                if (cursor.getString(DatabaseStatic.Scheme.COL_CREATE_TIME) != null)
                    bean.setCreateTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_CREATE_TIME)));
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            bean.setAmount(cursor.getInt(DatabaseStatic.Scheme.COL_AMOUNT));
            bean.setSelected(cursor.getInt(DatabaseStatic.Scheme.COL_SELECTED) == 1);
            list.add(bean);
        }
        cursor.close();
        db.close();
        return list;
    }

    public static void selectScheme(long[] ids) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Scheme.SELECTED, 0);
        db.update(DatabaseStatic.TABLE_SCHEME, values, null, null);
        if (ids.length > 0) {
            values = new ContentValues();
            values.put(DatabaseStatic.Scheme.SELECTED, 1);
            StringBuilder c = new StringBuilder();
            c.append(" in (");
            for (long id : ids)
                c.append(id).append(",");
            c.deleteCharAt(c.lastIndexOf(","));
            c.append(")");
            db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + c, null);
        }
        db.close();
    }

    public static void updateScheme(SchemeBean bean) {
        updateScheme(bean, BaseApplication.settings.isTunnel());
    }

    public static void updateScheme(SchemeBean bean, boolean tunnel) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Scheme.AMOUNT, bean.getAmount());
        values.put(DatabaseStatic.Scheme.NAME, bean.getName());
        values.put(DatabaseStatic.Scheme.SELECTED, bean.isSelected() ? 1 : 0);
        values.put(DatabaseStatic.Scheme.CREATE_TIME, df.format(bean.getCreateTime()));
        values.put(DatabaseStatic.Scheme.TUNNEL, tunnel ? 1 : 0);
        if (bean.getId() == -1) {
            values.put(DatabaseStatic.Scheme.SYNCHRONIZE, 0);
            values.put(DatabaseStatic.Scheme.UPLOAD_SERVER, -1);
            bean.setId(db.insert(DatabaseStatic.TABLE_SCHEME, null, values));
            db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_DETONATOR + bean.getId()));
        } else
            db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
    }

    public static void deleteScheme(List<Long> id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Long i : id) {
                if (isTableExist(DatabaseStatic.TABLE_DETONATOR + i))
                    db.execSQL("drop table " + DatabaseStatic.TABLE_DETONATOR + i);
                db.delete(DatabaseStatic.TABLE_SCHEME, DatabaseStatic.Scheme.ID + "=?", new String[]{i + ""});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        db.endTransaction();
        db.close();
    }

    public static void deleteRecord(List<Long> id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Long i : id) {
                Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, new String[]{DatabaseStatic.Scheme.SYNCHRONIZE},
                        DatabaseStatic.Scheme.ID + "=?", new String[]{i + ""}, null, null, null);
                if (cursor.moveToNext() && cursor.getInt(0) == 1) {
                    if (isTableExist(DatabaseStatic.TABLE_DETONATOR + i))
                        db.execSQL("drop table " + DatabaseStatic.TABLE_DETONATOR + i);
                    db.delete(DatabaseStatic.TABLE_SCHEME, DatabaseStatic.Scheme.ID + "=?", new String[]{i + ""});
                } else {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseStatic.Scheme.DELETED, 1);
                    db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{i + ""});
                }
                cursor.close();
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        db.endTransaction();
        db.close();
    }

    public static List<DetonatorBean> getCurrentDetonatorList() {
        List<DetonatorBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, new String[]{DatabaseStatic.Scheme.ID},
                DatabaseStatic.Scheme.SELECTED + "=1 and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.TUNNEL + "=?", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(DatabaseStatic.Scheme.COL_ID);
            Cursor c = db.query(DatabaseStatic.TABLE_DETONATOR + id, null, null, null, null, null, null);
            while (c.moveToNext()) {
                DetonatorBean bean = new DetonatorBean();
                bean.setSchemeId(id);
                bean.setAddress(c.getString(DatabaseStatic.Detonator.COL_SHELL));
                bean.setDownloaded(c.getInt(DatabaseStatic.Detonator.COL_DOWNLOADED) == 1);
                bean.setDelayTime(c.getInt(DatabaseStatic.Detonator.COL_DELAY_TIME));
                bean.setRow(c.getInt(DatabaseStatic.Detonator.COL_ROW));
                bean.setHole(c.getInt(DatabaseStatic.Detonator.COL_HOLE));
                bean.setInside(c.getInt(DatabaseStatic.Detonator.COL_INSIDE));
                bean.setId(c.getInt(DatabaseStatic.Detonator.COL_ID));
                list.add(bean);
            }
            c.close();
        }
        cursor.close();
        db.close();
        return list;
    }

    public static String checkDetonatorListExist(List<String> shell, long schemeId) {
        String result = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, new String[]{DatabaseStatic.Scheme.ID, DatabaseStatic.Scheme.NAME, DatabaseStatic.Scheme.TUNNEL},
                DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.ID + " <>?", new String[]{schemeId + ""}, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            Cursor c = db.query(DatabaseStatic.TABLE_DETONATOR + id, null, null, null, null, null, null);
            while (c.moveToNext()) {
                int i = shell.indexOf(c.getString(DatabaseStatic.Detonator.COL_SHELL));
                if (i >= 0) {
                    result = String.format(Locale.getDefault(), "%d%04d%s", cursor.getInt(2), i, cursor.getString(1));
                    break;
                }
            }
            c.close();
            if (result != null)
                break;
        }
        cursor.close();
        db.close();
        return result;
    }

    public static List<DetonatorBean> getDetonatorList(long schemeId) {
        return getTableDetonatorList(schemeId, DatabaseStatic.TABLE_DETONATOR + schemeId);
    }

    public static void updateDetonatorList(List<DetonatorBean> list) {
        if (list.size() > 0)
            updateTableDetonatorList(list, null);
    }

    public static void clearDetonatorDownloadFlag(long schemeId) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Detonator.DOWNLOADED, 0);
        db.update(DatabaseStatic.TABLE_DETONATOR + schemeId, values, null, null);
        db.close();
    }

    public static void deleteDetonatorTable(long schemeId) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.execSQL("drop table " + DatabaseStatic.TABLE_DETONATOR + schemeId);
        db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_DETONATOR + schemeId));
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Scheme.AMOUNT, 0);
        db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{schemeId + ""});
        db.close();
    }

    public static List<ExplosionRecordBean> getExplosionRecordList() {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        List<ExplosionRecordBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.EXPLODE_TIME + " is not null", null, null, null, DatabaseStatic.Scheme.EXPLODE_TIME);
        while (cursor.moveToNext()) {
            ExplosionRecordBean bean = new ExplosionRecordBean();
            bean.setId(cursor.getLong(DatabaseStatic.Scheme.COL_ID));
            bean.setLat(cursor.getFloat(DatabaseStatic.Scheme.COL_LATITUDE));
            bean.setLng(cursor.getFloat(DatabaseStatic.Scheme.COL_LONGITUDE));
            bean.setName(cursor.getString(DatabaseStatic.Scheme.COL_NAME));
            try {
                if (cursor.getString(DatabaseStatic.Scheme.COL_EXPLODE_TIME) != null)
                    bean.setExplodeTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_EXPLODE_TIME)));
                if (cursor.getString(DatabaseStatic.Scheme.COL_UPLOAD_TIME) != null)
                    bean.setUploadTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_UPLOAD_TIME)));
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            bean.setAmount(cursor.getInt(DatabaseStatic.Scheme.COL_AMOUNT));
            bean.setUploadServer(cursor.getInt(DatabaseStatic.Scheme.COL_UPLOAD_SERVER));
            bean.setSynchronize(cursor.getInt(DatabaseStatic.Scheme.COL_SYNCHRONIZE) == 1);
            bean.setTunnel(cursor.getInt(DatabaseStatic.Scheme.COL_TUNNEL) == 1);
            bean.setDeleted(cursor.getInt(DatabaseStatic.Scheme.COL_DELETED) == 1);
            list.add(bean);
        }
        cursor.close();
        db.close();
        return list;
    }

    public static ExplosionRecordBean getExplosionRecord(long id) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        ExplosionRecordBean bean = new ExplosionRecordBean();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.ID + "=?", new String[]{id + ""}, null, null, null);
        if (cursor.moveToNext()) {
            bean.setId(cursor.getLong(DatabaseStatic.Scheme.COL_ID));
            bean.setLat(cursor.getFloat(DatabaseStatic.Scheme.COL_LATITUDE));
            bean.setLng(cursor.getFloat(DatabaseStatic.Scheme.COL_LONGITUDE));
            try {
                if (cursor.getString(DatabaseStatic.Scheme.COL_EXPLODE_TIME) != null)
                    bean.setExplodeTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_EXPLODE_TIME)));
                if (cursor.getString(DatabaseStatic.Scheme.COL_UPLOAD_TIME) != null)
                    bean.setUploadTime(df.parse(cursor.getString(DatabaseStatic.Scheme.COL_UPLOAD_TIME)));
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            bean.setAmount(cursor.getInt(DatabaseStatic.Scheme.COL_AMOUNT));
            bean.setUploadServer(cursor.getInt(DatabaseStatic.Scheme.COL_UPLOAD_SERVER));
            bean.setTunnel(cursor.getInt(DatabaseStatic.Scheme.COL_TUNNEL) == 1);
            bean.setSynchronize(cursor.getInt(DatabaseStatic.Scheme.COL_SYNCHRONIZE) == 1);
            bean.setDeleted(cursor.getInt(DatabaseStatic.Scheme.COL_DELETED) == 1);
        }
        cursor.close();
        db.close();
        return bean;
    }

    public static void updateExplosionRecord(ExplosionRecordBean bean) {
        List<ExplosionRecordBean> list = new ArrayList<>();
        list.add(bean);
        updateExplosionRecordList(list);
    }

    public static void updateExplosionRecordList(List<ExplosionRecordBean> list) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ExplosionRecordBean bean : list) {
                ContentValues values = new ContentValues();
                if (bean.getExplodeTime() != null)
                    values.put(DatabaseStatic.Scheme.EXPLODE_TIME, df.format(bean.getExplodeTime()));
                if (bean.getUploadTime() != null)
                    values.put(DatabaseStatic.Scheme.UPLOAD_TIME, df.format(bean.getUploadTime()));
                values.put(DatabaseStatic.Scheme.SYNCHRONIZE, bean.isSynchronize() ? 1 : 0);
                values.put(DatabaseStatic.Scheme.LATITUDE, bean.getLat());
                values.put(DatabaseStatic.Scheme.LONGITUDE, bean.getLng());
                values.put(DatabaseStatic.Scheme.UPLOAD_SERVER, bean.getUploadServer());
                db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{bean.getId() + ""});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        db.endTransaction();
        db.close();
    }

    private static EnterpriseBean fillEnterpriseBean(Cursor cursor) {
        EnterpriseBean bean = new EnterpriseBean();
        bean.setId(cursor.getLong(DatabaseStatic.Enterprise.COL_ID));
        bean.setBlasterId(cursor.getString(DatabaseStatic.Enterprise.COL_BLASTER_ID_CARD));
        bean.setCode(cursor.getString(DatabaseStatic.Enterprise.COL_ENTERPRISE_CODE));
        bean.setCommercial(cursor.getInt(DatabaseStatic.Enterprise.COL_COMMERCIAL) == 1);
        bean.setContract(cursor.getString(DatabaseStatic.Enterprise.COL_CONTRACT_CODE));
        bean.setProject(cursor.getString(DatabaseStatic.Enterprise.COL_PROJECT_CODE));
        bean.setSelected(cursor.getInt(DatabaseStatic.Enterprise.COL_SELECTED) == 1);
        return bean;
    }

    public static List<EnterpriseBean> getEnterpriseList() {
        List<EnterpriseBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_ENTERPRISE, null, DatabaseStatic.Enterprise.DELETED + "=0", null, null, null, null);
        while (cursor.moveToNext())
            list.add(fillEnterpriseBean(cursor));
        cursor.close();
        db.close();
        return list;
    }

    public static EnterpriseBean getEnterpriseById(long id) {
        EnterpriseBean bean = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_ENTERPRISE, null, DatabaseStatic.Enterprise.ID + "=?", new String[]{id + ""}, null, null, null);
        if (cursor.moveToNext())
            bean = fillEnterpriseBean(cursor);
        cursor.close();
        db.close();
        return bean;
    }

    public static EnterpriseBean getCurrentEnterprise() {
        EnterpriseBean bean = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_ENTERPRISE, null, DatabaseStatic.Enterprise.SELECTED + "=1", null, null, null, null);
        if (cursor.moveToNext())
            bean = fillEnterpriseBean(cursor);
        cursor.close();
        db.close();
        return bean;
    }

    public static void updateEnterprise(EnterpriseBean bean) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (bean.isSelected()) {
            values.put(DatabaseStatic.Enterprise.SELECTED, 0);
            db.update(DatabaseStatic.TABLE_ENTERPRISE, values, DatabaseStatic.Enterprise.SELECTED + "=1", null);
            values = new ContentValues();
        }
        values.put(DatabaseStatic.Enterprise.SELECTED, bean.isSelected() ? 1 : 0);
        values.put(DatabaseStatic.Enterprise.ENTERPRISE_CODE, bean.getCode());
        values.put(DatabaseStatic.Enterprise.BLASTER_ID_CARD, bean.getBlasterId());
        values.put(DatabaseStatic.Enterprise.COMMERCIAL, bean.isCommercial() ? 1 : 0);
        values.put(DatabaseStatic.Enterprise.CONTRACT_CODE, bean.getContract());
        values.put(DatabaseStatic.Enterprise.PROJECT_CODE, bean.getProject());
        if (bean.getId() == -1)
            bean.setId(db.insert(DatabaseStatic.TABLE_ENTERPRISE, null, values));
        else
            db.update(DatabaseStatic.TABLE_ENTERPRISE, values, DatabaseStatic.Enterprise.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
    }

    public static void deleteEnterprise(long id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_DAN_LING, new String[]{DatabaseStatic.DanLing.ID}, DatabaseStatic.DanLing.ENTERPRISE_ID + "=?", new String[]{id + ""}, null, null, null);
        if (cursor.moveToNext()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseStatic.Enterprise.DELETED, 1);
            db.update(DatabaseStatic.TABLE_ENTERPRISE, values, DatabaseStatic.Enterprise.ID + "=?", new String[]{id + ""});
        } else
            db.delete(DatabaseStatic.TABLE_ENTERPRISE, DatabaseStatic.Enterprise.ID + "=?", new String[]{id + ""});
        cursor.close();
        db.close();
    }

    private static DanLinDetonatorBean fillDanLinDetonatorBean(Cursor cursor) {
        SQLiteDatabase db = myHelper.getReadableDatabase();
        DanLinDetonatorBean bean = new DanLinDetonatorBean();
        bean.setId(cursor.getLong(DatabaseStatic.DanLing.COL_ID));
        bean.setOffline(cursor.getInt(DatabaseStatic.DanLing.COL_OFFLINE) == 1);
        bean.setEnterpriseId(cursor.getLong(DatabaseStatic.DanLing.COL_ENTERPRISE_ID));
        DanLinDetonatorBean.ResultBean resultBean = new DanLinDetonatorBean.ResultBean();
        resultBean.setCwxx(cursor.getString(DatabaseStatic.DanLing.COL_ERROR_CODE));
        resultBean.setSqrq(cursor.getString(DatabaseStatic.DanLing.COL_APPLICATION_TIME));
        List<DanLinDetonatorBean.ResultBean.SbbhsBean> sbbhsBeans = new ArrayList<>();
        Cursor c = db.query(DatabaseStatic.TABLE_DOWNLOADED_EXPLODER, null, DatabaseStatic.DownloadedExploder.DAN_LING_ID + "=?"
                , new String[]{cursor.getLong(DatabaseStatic.DanLing.COL_ID) + ""}, null, null, null);
        while (c.moveToNext()) {
            DanLinDetonatorBean.ResultBean.SbbhsBean b = new DanLinDetonatorBean.ResultBean.SbbhsBean();
            b.setSbbh(c.getString(DatabaseStatic.DownloadedExploder.COL_EXPLODER_SN));
            sbbhsBeans.add(b);
        }
        c.close();
        resultBean.setSbbhs(sbbhsBeans);
        c = db.query(DatabaseStatic.TABLE_ALLOWED_AREA, null, DatabaseStatic.AllowedArea.DAN_LING_ID + "=?"
                , new String[]{cursor.getLong(DatabaseStatic.DanLing.COL_ID) + ""}, null, null, null);
        List<ZbqyBean> zbqyBeans = new ArrayList<>();
        while (c.moveToNext()) {
            ZbqyBean b = new ZbqyBean();
            b.setZbqymc(c.getString(DatabaseStatic.AllowedArea.COL_NAME));
            b.setZbqyjd(c.getFloat(DatabaseStatic.AllowedArea.COL_LONGITUDE) + "");
            b.setZbqywd(c.getFloat(DatabaseStatic.AllowedArea.COL_LATITUDE) + "");
            b.setZbqybj(c.getFloat(DatabaseStatic.AllowedArea.COL_RADIUS) + "");
            b.setZbqssj(c.getString(DatabaseStatic.AllowedArea.COL_START_TIME));
            b.setZbjzsj(c.getString(DatabaseStatic.AllowedArea.COL_END_TIME));
            zbqyBeans.add(b);
        }
        c.close();
        DanLinDetonatorBean.ResultBean.ZbqysBean zbqysBean = new DanLinDetonatorBean.ResultBean.ZbqysBean();
        zbqysBean.setZbqy(zbqyBeans);
        resultBean.setZbqys(zbqysBean);
        c = db.query(DatabaseStatic.TABLE_FORBIDDEN_AREA, null, DatabaseStatic.ForbiddenArea.DAN_LING_ID + "=?"
                , new String[]{cursor.getLong(DatabaseStatic.DanLing.COL_ID) + ""}, null, null, null);
        List<JbqyBean> jbqyBeans = new ArrayList<>();
        while (c.moveToNext()) {
            JbqyBean b = new JbqyBean();
            b.setJbqyjd(c.getFloat(DatabaseStatic.ForbiddenArea.COL_LONGITUDE) + "");
            b.setJbqywd(c.getFloat(DatabaseStatic.ForbiddenArea.COL_LATITUDE) + "");
            b.setJbqybj(c.getFloat(DatabaseStatic.ForbiddenArea.COL_RADIUS) + "");
            b.setJbqssj(c.getString(DatabaseStatic.ForbiddenArea.COL_START_TIME));
            b.setJbjzsj(c.getString(DatabaseStatic.ForbiddenArea.COL_END_TIME));
            jbqyBeans.add(b);
        }
        c.close();
        DanLinDetonatorBean.ResultBean.JbqysBean jbqysBean = new DanLinDetonatorBean.ResultBean.JbqysBean();
        jbqysBean.setJbqy(jbqyBeans);
        resultBean.setJbqys(jbqysBean);
        c = db.query(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, null, DatabaseStatic.DownloadedDetonator.DAN_LING_ID + "=?"
                , new String[]{cursor.getLong(DatabaseStatic.DanLing.COL_ID) + ""}, null, null, null);
        List<LgBean> lgBeans = new ArrayList<>();
        while (c.moveToNext()) {
            LgBean b = new LgBean();
            b.setSchemeId(c.getLong(DatabaseStatic.DownloadedDetonator.COL_SCHEME_ID));
            b.setFbh(c.getString(DatabaseStatic.DownloadedDetonator.COL_OFFLINE_SN));
            b.setGzm(c.getString(DatabaseStatic.DownloadedDetonator.COL_WORK_ID));
            b.setGzmcwxx(c.getString(DatabaseStatic.DownloadedDetonator.COL_ERROR_CODE));
            b.setUid(c.getString(DatabaseStatic.DownloadedDetonator.COL_UID));
            b.setYxq(c.getString(DatabaseStatic.DownloadedDetonator.COL_VALID_TIME));
            lgBeans.add(b);
        }
        c.close();
        DanLinDetonatorBean.ResultBean.LgsBean lgsBean = new DanLinDetonatorBean.ResultBean.LgsBean();
        lgsBean.setLg(lgBeans);
        resultBean.setLgs(lgsBean);
        bean.setResult(resultBean);
        return bean;
    }

    public static DanLinDetonatorBean getDanLinDownloadDetonatorById(long id) {
        DanLinDetonatorBean bean = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_DAN_LING, null, DatabaseStatic.DanLing.ID + "=?", new String[]{id + ""}, null, null, null);
        if (cursor.moveToNext())
            bean = fillDanLinDetonatorBean(cursor);
        cursor.close();
        db.close();
        return bean;

    }

    public static DanLinDetonatorBean getDanLinDownloadDetonator(boolean offline) {
        DanLinDetonatorBean bean = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_DAN_LING, null, DatabaseStatic.DanLing.OFFLINE + "=? and " + DatabaseStatic.DanLing.USED + "=0", new String[]{offline ? "1" : "0"}, null, null, null);
        if (cursor.moveToNext())
            bean = fillDanLinDetonatorBean(cursor);
        cursor.close();
        db.close();
        return bean;
    }

    public static long updateDownloadDetonator(DanLinDetonatorBean bean) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.beginTransaction();
        long id = -1;
        try {
            Cursor cursor = db.query(DatabaseStatic.TABLE_DAN_LING, null,
                    DatabaseStatic.DanLing.OFFLINE + "=? and " + DatabaseStatic.DanLing.USED + "=0", new String[]{bean.isOffline() ? "1" : "0"}, null, null, null);
            while (cursor.moveToNext()) {
                long id1 = cursor.getLong(DatabaseStatic.DanLing.COL_ID);
                db.delete(DatabaseStatic.TABLE_ALLOWED_AREA, DatabaseStatic.AllowedArea.DAN_LING_ID + "=?", new String[]{id1 + ""});
                db.delete(DatabaseStatic.TABLE_FORBIDDEN_AREA, DatabaseStatic.ForbiddenArea.DAN_LING_ID + "=?", new String[]{id1 + ""});
                db.delete(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, DatabaseStatic.DownloadedDetonator.DAN_LING_ID + "=?", new String[]{id1 + ""});
                db.delete(DatabaseStatic.TABLE_DOWNLOADED_EXPLODER, DatabaseStatic.DownloadedExploder.DAN_LING_ID + "=?", new String[]{id1 + ""});
            }
            cursor.close();
            db.delete(DatabaseStatic.TABLE_DAN_LING, DatabaseStatic.DanLing.OFFLINE + "=? and " + DatabaseStatic.DanLing.USED + "=0", new String[]{bean.isOffline() ? "1" : "0"});
            ContentValues values = new ContentValues();
            values.put(DatabaseStatic.DanLing.ERROR_CODE, bean.getResult().getCwxx());
            values.put(DatabaseStatic.DanLing.OFFLINE, bean.isOffline() ? 1 : 0);
            values.put(DatabaseStatic.DanLing.APPLICATION_TIME, bean.getResult().getSqrq());
            values.put(DatabaseStatic.DanLing.USED, 0);
            values.put(DatabaseStatic.DanLing.ENTERPRISE_ID, bean.getEnterpriseId());
            id = db.insert(DatabaseStatic.TABLE_DAN_LING, null, values);
            for (DanLinDetonatorBean.ResultBean.SbbhsBean b : bean.getResult().getSbbhs()) {
                values = new ContentValues();
                values.put(DatabaseStatic.DownloadedExploder.DAN_LING_ID, id);
                values.put(DatabaseStatic.DownloadedExploder.EXPLODER_SN, b.getSbbh());
                db.insert(DatabaseStatic.TABLE_DOWNLOADED_EXPLODER, null, values);
            }
            for (ZbqyBean b : bean.getResult().getZbqys().getZbqy()) {
                values = new ContentValues();
                values.put(DatabaseStatic.AllowedArea.DAN_LING_ID, id);
                values.put(DatabaseStatic.AllowedArea.NAME, b.getZbqymc());
                values.put(DatabaseStatic.AllowedArea.LATITUDE, Float.parseFloat(b.getZbqywd()));
                values.put(DatabaseStatic.AllowedArea.LONGITUDE, Float.parseFloat(b.getZbqyjd()));
                values.put(DatabaseStatic.AllowedArea.RADIUS, Float.parseFloat(b.getZbqybj()));
                values.put(DatabaseStatic.AllowedArea.START_TIME, b.getZbqssj());
                values.put(DatabaseStatic.AllowedArea.END_TIME, b.getZbjzsj());
                db.insert(DatabaseStatic.TABLE_ALLOWED_AREA, null, values);
            }
            for (JbqyBean b : bean.getResult().getJbqys().getJbqy()) {
                values = new ContentValues();
                values.put(DatabaseStatic.ForbiddenArea.DAN_LING_ID, id);
                values.put(DatabaseStatic.ForbiddenArea.LATITUDE, Float.parseFloat(b.getJbqywd()));
                values.put(DatabaseStatic.ForbiddenArea.LONGITUDE, Float.parseFloat(b.getJbqyjd()));
                values.put(DatabaseStatic.ForbiddenArea.RADIUS, Float.parseFloat(b.getJbqybj()));
                values.put(DatabaseStatic.ForbiddenArea.START_TIME, b.getJbqssj());
                values.put(DatabaseStatic.ForbiddenArea.END_TIME, b.getJbjzsj());
                db.insert(DatabaseStatic.TABLE_FORBIDDEN_AREA, null, values);
            }
            for (LgBean b : bean.getResult().getLgs().getLg()) {
                values = new ContentValues();
                values.put(DatabaseStatic.DownloadedDetonator.DAN_LING_ID, id);
                values.put(DatabaseStatic.DownloadedDetonator.OFFLINE_SN, b.getFbh());
                values.put(DatabaseStatic.DownloadedDetonator.UID, b.getUid());
                values.put(DatabaseStatic.DownloadedDetonator.WORK_ID, b.getGzm());
                values.put(DatabaseStatic.DownloadedDetonator.VALID_TIME, b.getYxq());
                values.put(DatabaseStatic.DownloadedDetonator.ERROR_CODE, b.getGzmcwxx());
                db.insert(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        db.endTransaction();
        db.close();
        return id;
    }

    public static long getDanLinIdByExplodeRecordId(long id) {
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, new String[]{DatabaseStatic.DownloadedDetonator.DAN_LING_ID}, DatabaseStatic.DownloadedDetonator.SCHEME_ID + "=?", new String[]{id + ""}, null, null, null);
        long result = -1;
        if (cursor.moveToNext())
            result = cursor.getLong(0);
        cursor.close();
        db.close();
        return result;
    }

    public static List<BaiSeInfoBean> getBaiSeInfoList() {
        List<BaiSeInfoBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_BAI_SE_PROJECT, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            BaiSeInfoBean bean = new BaiSeInfoBean();
            bean.setId(cursor.getLong(DatabaseStatic.BaiSeProject.COL_ID));
            bean.setSelected(cursor.getInt(DatabaseStatic.BaiSeProject.COL_SELECTED) == 1);
            bean.setBursterName(cursor.getString(DatabaseStatic.BaiSeProject.COL_BLASTER_NAME));
            bean.setProjectName(cursor.getString(DatabaseStatic.BaiSeProject.COL_PROJECT_NAME));
            bean.setProjectType(cursor.getString(DatabaseStatic.BaiSeProject.COL_PROJECT_TYPE));
            bean.setProjectCode(cursor.getString(DatabaseStatic.BaiSeProject.COL_PROJECT_CODE));
            bean.setDeviceNO(cursor.getString(DatabaseStatic.BaiSeProject.COL_DEVICE_NO));
            bean.setBurstOrgCode(cursor.getString(DatabaseStatic.BaiSeProject.COL_BURST_ORG_CODE));
            bean.setBurstOrgName(cursor.getString(DatabaseStatic.BaiSeProject.COL_BURST_ORG_NAME));
            bean.setBurstTime(cursor.getString(DatabaseStatic.BaiSeProject.COL_BURST_TIME));
            bean.setDetonatorCount(cursor.getInt(DatabaseStatic.BaiSeProject.COL_DETONATOR_COUNT));
            bean.setGpsCoordinateSystems(cursor.getString(DatabaseStatic.BaiSeProject.COL_GPS_COORDINATE_SYSTEMS));
            bean.setLngLat(cursor.getString(DatabaseStatic.BaiSeProject.COL_LNG_LAT));
            bean.setIdCard(cursor.getString(DatabaseStatic.BaiSeProject.COL_ID_CARD));
            bean.setOffline(cursor.getInt(DatabaseStatic.BaiSeProject.COL_OFFLINE) == 1);
            bean.setOfflineTime(cursor.getFloat(DatabaseStatic.BaiSeProject.COL_OFFLINE_TIME));
            list.add(bean);
        }
        cursor.close();
        db.close();
        return list;
    }

    public static BaiSeInfoBean getCurrentBaiSeInfo() {
        BaiSeInfoBean bean = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_BAI_SE_PROJECT, null, DatabaseStatic.BaiSeProject.SELECTED + "=1", null, null, null, null);
        if (cursor.moveToNext()) {
            bean = new BaiSeInfoBean();
            bean.setId(cursor.getLong(DatabaseStatic.BaiSeProject.COL_ID));
            bean.setSelected(cursor.getInt(DatabaseStatic.BaiSeProject.COL_SELECTED) == 1);
            bean.setBursterName(cursor.getString(DatabaseStatic.BaiSeProject.COL_BLASTER_NAME));
            bean.setProjectName(cursor.getString(DatabaseStatic.BaiSeProject.COL_PROJECT_NAME));
            bean.setProjectType(cursor.getString(DatabaseStatic.BaiSeProject.COL_PROJECT_TYPE));
            bean.setProjectCode(cursor.getString(DatabaseStatic.BaiSeProject.COL_PROJECT_CODE));
            bean.setDeviceNO(cursor.getString(DatabaseStatic.BaiSeProject.COL_DEVICE_NO));
            bean.setBurstOrgCode(cursor.getString(DatabaseStatic.BaiSeProject.COL_BURST_ORG_CODE));
            bean.setBurstOrgName(cursor.getString(DatabaseStatic.BaiSeProject.COL_BURST_ORG_NAME));
            bean.setBurstTime(cursor.getString(DatabaseStatic.BaiSeProject.COL_BURST_TIME));
            bean.setDetonatorCount(cursor.getInt(DatabaseStatic.BaiSeProject.COL_DETONATOR_COUNT));
            bean.setGpsCoordinateSystems(cursor.getString(DatabaseStatic.BaiSeProject.COL_GPS_COORDINATE_SYSTEMS));
            bean.setLngLat(cursor.getString(DatabaseStatic.BaiSeProject.COL_LNG_LAT));
            bean.setIdCard(cursor.getString(DatabaseStatic.BaiSeProject.COL_ID_CARD));
            bean.setOffline(cursor.getInt(DatabaseStatic.BaiSeProject.COL_OFFLINE) == 1);
            bean.setOfflineTime(cursor.getFloat(DatabaseStatic.BaiSeProject.COL_OFFLINE_TIME));
        }
        cursor.close();
        db.close();
        return bean;
    }

    public static void updateBaiSeInfo(BaiSeInfoBean bean) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (bean.isSelected()) {
            values.put(DatabaseStatic.BaiSeProject.SELECTED, 0);
            db.update(DatabaseStatic.TABLE_BAI_SE_PROJECT, values, DatabaseStatic.BaiSeProject.SELECTED + "=1", null);
            values = new ContentValues();
        }
        values.put(DatabaseStatic.BaiSeProject.SELECTED, bean.isSelected() ? 1 : 0);
        values.put(DatabaseStatic.BaiSeProject.BLASTER_NAME, bean.getBursterName());
        values.put(DatabaseStatic.BaiSeProject.PROJECT_NAME, bean.getProjectName());
        values.put(DatabaseStatic.BaiSeProject.PROJECT_TYPE, bean.getProjectType());
        values.put(DatabaseStatic.BaiSeProject.PROJECT_CODE, bean.getProjectCode());
        values.put(DatabaseStatic.BaiSeProject.DEVICE_NO, bean.getDeviceNO());
        values.put(DatabaseStatic.BaiSeProject.BURST_ORG_CODE, bean.getBurstOrgCode());
        values.put(DatabaseStatic.BaiSeProject.BURST_ORG_NAME, bean.getBurstOrgName());
        values.put(DatabaseStatic.BaiSeProject.BURST_TIME, bean.getBurstTime());
        values.put(DatabaseStatic.BaiSeProject.DETONATOR_COUNT, bean.getDetonatorCount());
        values.put(DatabaseStatic.BaiSeProject.GPS_COORDINATE_SYSTEMS, bean.getGpsCoordinateSystems());
        values.put(DatabaseStatic.BaiSeProject.LNG_LAT, bean.getLngLat());
        values.put(DatabaseStatic.BaiSeProject.ID_CARD, bean.getIdCard());
        values.put(DatabaseStatic.BaiSeProject.OFFLINE, bean.isOffline() ? 1 : 0);
        values.put(DatabaseStatic.BaiSeProject.OFFLINE_TIME, bean.getOfflineTime());
        if (bean.getId() == -1)
            bean.setId((int) db.insert(DatabaseStatic.TABLE_BAI_SE_PROJECT, null, values));
        else
            db.update(DatabaseStatic.TABLE_BAI_SE_PROJECT, values, DatabaseStatic.BaiSeProject.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
    }

    public static void deleteBaiSeProject(long id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.delete(DatabaseStatic.TABLE_BAI_SE_PROJECT, DatabaseStatic.BaiSeProject.ID + "=?", new String[]{id + ""});
        db.close();
    }

    public static List<BaiSeBlasterBean> getBaiSeBlasterList() {
        List<BaiSeBlasterBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_BAI_SE_BLASTER, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            BaiSeBlasterBean bean = new BaiSeBlasterBean();
            bean.setId(cursor.getLong(DatabaseStatic.BaiSeBlaster.COL_ID));
            bean.setName(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_NAME));
            bean.setSelected(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_SELECTED) == 1);
            bean.setChecked(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_CHECKED) == 1);
            bean.setProject(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_PROJECT) == 1);
            BaiSeBlasterBean.Data data = new BaiSeBlasterBean.Data();
            data.setAppVersion(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_APP_VERSION));
            data.setProjectCode(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_PROJECT_CODE));
            data.setBurstOrgCode(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_BURST_ORG_CODE));
            data.setDeviceNO(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_DEVICE_NO));
            data.setGpsCoordinateSystems(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_GPS_COORDINATE_SYSTEMS));
            data.setLngLat(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_LNG_LAT));
            data.setUserIdCard(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_ID_CARD));
            bean.setData(data);
            list.add(bean);
        }
        cursor.close();
        db.close();
        return list;
    }

    public static BaiSeBlasterBean getCurrentBaiSeBlaster() {
        BaiSeBlasterBean bean = null;
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_BAI_SE_BLASTER, null, DatabaseStatic.BaiSeBlaster.SELECTED + "=1", null, null, null, null);
        if (cursor.moveToNext()) {
            bean = new BaiSeBlasterBean();
            bean.setId(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_ID));
            bean.setName(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_NAME));
            bean.setSelected(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_SELECTED) == 1);
            bean.setChecked(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_CHECKED) == 1);
            bean.setProject(cursor.getInt(DatabaseStatic.BaiSeBlaster.COL_PROJECT) == 1);
            BaiSeBlasterBean.Data data = new BaiSeBlasterBean.Data();
            data.setAppVersion(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_APP_VERSION));
            data.setProjectCode(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_PROJECT_CODE));
            data.setBurstOrgCode(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_BURST_ORG_CODE));
            data.setDeviceNO(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_DEVICE_NO));
            data.setGpsCoordinateSystems(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_GPS_COORDINATE_SYSTEMS));
            data.setLngLat(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_LNG_LAT));
            data.setUserIdCard(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_ID_CARD));
            bean.setData(data);
        }
        cursor.close();
        db.close();
        return bean;
    }

    public static void updateBaiSeBlaster(BaiSeBlasterBean bean) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (bean.isSelected()) {
            values.put(DatabaseStatic.BaiSeBlaster.SELECTED, 0);
            db.update(DatabaseStatic.TABLE_BAI_SE_BLASTER, values, DatabaseStatic.BaiSeBlaster.SELECTED + "=1", null);
            values = new ContentValues();
        }
        values.put(DatabaseStatic.BaiSeBlaster.CHECKED, bean.isChecked() ? 1 : 0);
        values.put(DatabaseStatic.BaiSeBlaster.SELECTED, bean.isSelected() ? 1 : 0);
        values.put(DatabaseStatic.BaiSeBlaster.NAME, bean.getName());
        values.put(DatabaseStatic.BaiSeBlaster.APP_VERSION, bean.getData().getAppVersion());
        values.put(DatabaseStatic.BaiSeBlaster.DEVICE_NO, bean.getData().getDeviceNO());
        values.put(DatabaseStatic.BaiSeBlaster.PROJECT_CODE, bean.getData().getProjectCode());
        values.put(DatabaseStatic.BaiSeBlaster.BURST_ORG_CODE, bean.getData().getBurstOrgCode());
        values.put(DatabaseStatic.BaiSeBlaster.PROJECT, bean.isProject() ? 1 : 0);
        values.put(DatabaseStatic.BaiSeBlaster.GPS_COORDINATE_SYSTEMS, bean.getData().getGpsCoordinateSystems());
        values.put(DatabaseStatic.BaiSeBlaster.LNG_LAT, bean.getData().getLngLat());
        values.put(DatabaseStatic.BaiSeBlaster.ID_CARD, bean.getData().getUserIdCard());
        if (bean.getId() == -1)
            bean.setId((int) db.insert(DatabaseStatic.TABLE_BAI_SE_BLASTER, null, values));
        else
            db.update(DatabaseStatic.TABLE_BAI_SE_BLASTER, values, DatabaseStatic.BaiSeBlaster.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
    }

    public static void deleteBaiSeBlaster(long id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.delete(DatabaseStatic.TABLE_BAI_SE_BLASTER, DatabaseStatic.BaiSeBlaster.ID + "=?", new String[]{id + ""});
        db.close();
    }

    private static List<DetonatorBean> getTableDetonatorList(long schemeId, String table) {
        List<DetonatorBean> list = new ArrayList<>();
        SQLiteDatabase db = myHelper.getReadableDatabase();
        if (isTableExist(table)) {
            Cursor cursor = db.query(table, null, null, null, null, null, DatabaseStatic.TABLE_AUTH_DETONATOR.equals(table) ? DatabaseStatic.Detonator.SHELL : null);
            while (cursor.moveToNext()) {
                DetonatorBean bean = new DetonatorBean();
                bean.setSchemeId(schemeId);
                bean.setAddress(cursor.getString(DatabaseStatic.Detonator.COL_SHELL));
                bean.setUID(cursor.getString(DatabaseStatic.Detonator.COL_UID));
                bean.setDownloaded(cursor.getInt(DatabaseStatic.Detonator.COL_DOWNLOADED) == 1);
                bean.setDelayTime(cursor.getInt(DatabaseStatic.Detonator.COL_DELAY_TIME));
                bean.setRow(cursor.getInt(DatabaseStatic.Detonator.COL_ROW));
                bean.setHole(cursor.getInt(DatabaseStatic.Detonator.COL_HOLE));
                bean.setInside(cursor.getInt(DatabaseStatic.Detonator.COL_INSIDE));
                bean.setId(cursor.getInt(DatabaseStatic.Detonator.COL_ID));
                list.add(bean);
            }
            cursor.close();
        }
        db.close();
        return list;
    }

    public static List<DetonatorBean> getAuthDetonatorList() {
        return getTableDetonatorList(-1, DatabaseStatic.TABLE_AUTH_DETONATOR);
    }

    private static void updateTableDetonatorList(List<DetonatorBean> list, String table) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        boolean isExist = isTableExist(table == null && list.size() > 0 ? DatabaseStatic.TABLE_DETONATOR + list.get(0).getSchemeId() : table);
        db.beginTransaction();
        try {
            long schemeId = -1;
            int i = 0;
            if (table != null)
                if (isExist)
                    db.delete(table, null, null);
                else if (list.size() > 0)
                    db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_DETONATOR + list.get(0).getSchemeId()));
            for (DetonatorBean bean : list) {
                ContentValues values = new ContentValues();
                if (table == null && schemeId != bean.getSchemeId()) {
                    if (-1 != schemeId) {
                        ContentValues v = new ContentValues();
                        v.put(DatabaseStatic.Scheme.AMOUNT, i);
                        db.update(DatabaseStatic.TABLE_SCHEME, v, DatabaseStatic.Scheme.ID + "=?", new String[]{bean.getSchemeId() + ""});
                        i = 0;
                    }
                    schemeId = bean.getSchemeId();
                    if (isTableExist(DatabaseStatic.TABLE_DETONATOR + bean.getSchemeId()))
                        db.delete(DatabaseStatic.TABLE_DETONATOR + schemeId, null, null);
                    else
                        db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_DETONATOR + bean.getSchemeId()));
                }
                values.put(DatabaseStatic.Detonator.SHELL, bean.getAddress());
                values.put(DatabaseStatic.Detonator.UID, bean.getUID());
                values.put(DatabaseStatic.Detonator.DELAY_TIME, bean.getDelayTime());
                values.put(DatabaseStatic.Detonator.ROW, bean.getRow());
                values.put(DatabaseStatic.Detonator.HOLE, bean.getHole());
                values.put(DatabaseStatic.Detonator.INSIDE, bean.getInside());
                values.put(DatabaseStatic.Detonator.DOWNLOADED, bean.isDownloaded() ? 1 : 0);
                bean.setId((int) db.insert(table == null ? DatabaseStatic.TABLE_DETONATOR + schemeId : table, null, values));
                i++;
            }
            if (table == null) {
                if (schemeId != -1) {
                    ContentValues v = new ContentValues();
                    v.put(DatabaseStatic.Scheme.AMOUNT, i);
                    db.update(DatabaseStatic.TABLE_SCHEME, v, DatabaseStatic.Scheme.ID + "=?", new String[]{schemeId + ""});
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        db.endTransaction();
        db.close();
    }

    public static void updateDetonator(DetonatorBean bean) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Detonator.SHELL, bean.getAddress());
        values.put(DatabaseStatic.Detonator.DELAY_TIME, bean.getDelayTime());
        values.put(DatabaseStatic.Detonator.ROW, bean.getRow());
        values.put(DatabaseStatic.Detonator.HOLE, bean.getHole());
        values.put(DatabaseStatic.Detonator.INSIDE, bean.getInside());
        values.put(DatabaseStatic.Detonator.DOWNLOADED, bean.isDownloaded() ? 1 : 0);
        db.update(DatabaseStatic.TABLE_DETONATOR + bean.getSchemeId(), values, DatabaseStatic.Detonator.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
    }

    public static void updateAuthDetonatorList(List<DetonatorBean> list) {
        updateTableDetonatorList(list, DatabaseStatic.TABLE_AUTH_DETONATOR);
    }

    public static void setDanLinRecord(long id, List<DetonatorBean> list) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.beginTransaction();
        for (DetonatorBean bean : list) {
            ContentValues values = new ContentValues();
            values.put(DatabaseStatic.DownloadedDetonator.SCHEME_ID, bean.getSchemeId());
            db.update(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, values, DatabaseStatic.DownloadedDetonator.OFFLINE_SN + "=? and "
                    + DatabaseStatic.DownloadedDetonator.DAN_LING_ID + "=?", new String[]{bean.getAddress(), id + ""});
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public static void setDanLinUsed(long id) {
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.DanLing.USED, 1);
        db.update(DatabaseStatic.TABLE_DAN_LING, values, DatabaseStatic.DanLing.ID + "=?", new String[]{id + ""});
        db.close();
    }

    public static void checkDanLinUsed(long id) {
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, new String[]{DatabaseStatic.DownloadedDetonator.ID},
                DatabaseStatic.DownloadedDetonator.DAN_LING_ID + "=? and " + DatabaseStatic.DownloadedDetonator.SCHEME_ID + ">=0", new String[]{id + ""}, null, null, null);
        boolean b = cursor.moveToNext();
        cursor.close();
        db.close();
        if (b)
            setDanLinUsed(id);
    }

    public static boolean clearDatabase(Context context) {
        myHelper.close();
        boolean result = new File(FilePath.FILE_DATABASE).delete();
        initHelper(context);
        return result;
    }
}
