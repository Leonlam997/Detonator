package com.leon.detonator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.JbqyBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bean.ZbqyBean;
import com.leon.detonator.util.ConstantUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DbUtil {
    public static List<SchemeBean> getSchemeList(Context context) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        List<SchemeBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.TUNNEL + "=? and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.DELETED + "=0", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
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
        myHelper.close();
        return list;
    }

    public static SchemeBean getScheme(Context context, long id) {
        SchemeBean bean = new SchemeBean();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.ID + "=? ", new String[]{id + ""}, null, null, null);
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
        myHelper.close();
        return bean;
    }

    public static SchemeBean getCurrentScheme(Context context) {
        SchemeBean bean = null;
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.SELECTED + "=1 and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.TUNNEL + "=?", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
        if (cursor.moveToNext()) {
            bean = new SchemeBean();
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
        }
        cursor.close();
        db.close();
        myHelper.close();
        return bean;
    }

    public static void updateScheme(Context context, SchemeBean bean) {
        updateScheme(context, bean, BaseApplication.settings.isTunnel());
    }

    public static void updateScheme(Context context, SchemeBean bean, boolean tunnel) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (bean.isSelected()) {
            values.put(DatabaseStatic.Scheme.SELECTED, 0);
            db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.SELECTED + "=1 and " + DatabaseStatic.Scheme.TUNNEL + "=?", new String[]{tunnel ? "1" : "0"});
            values = new ContentValues();
        }
        values.put(DatabaseStatic.Scheme.AMOUNT, bean.getAmount());
        values.put(DatabaseStatic.Scheme.NAME, bean.getName());
        values.put(DatabaseStatic.Scheme.SELECTED, bean.isSelected() ? 1 : 0);
        values.put(DatabaseStatic.Scheme.CREATE_TIME, df.format(bean.getCreateTime()));
        values.put(DatabaseStatic.Scheme.TUNNEL, tunnel ? 1 : 0);
        if (bean.getId() == -1) {
            values.put(DatabaseStatic.Scheme.SYNCHRONIZE, 0);
            values.put(DatabaseStatic.Scheme.DELETED, 0);
            values.put(DatabaseStatic.Scheme.UPLOAD_SERVER, -1);
            bean.setId(db.insert(DatabaseStatic.TABLE_SCHEME, null, values));
            db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_DETONATOR + bean.getId()));
        } else
            db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
        myHelper.close();
    }

    public static void deleteScheme(Context context, long id) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Scheme.DELETED, 1);
        db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{id + ""});
        db.close();
        myHelper.close();
    }

    public static List<DetonatorBean> getCurrentDetonatorList(Context context) {
        long id = -1;
        List<DetonatorBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, new String[]{DatabaseStatic.Scheme.ID},
                DatabaseStatic.Scheme.SELECTED + "=1 and "
                        + DatabaseStatic.Scheme.EXPLODE_TIME + " is null and "
                        + DatabaseStatic.Scheme.TUNNEL + "=?", new String[]{BaseApplication.settings.isTunnel() ? "1" : "0"}, null, null, null);
        if (cursor.moveToNext())
            id = cursor.getLong(DatabaseStatic.Scheme.COL_ID);
        cursor.close();
        if (-1 != id) {
            cursor = db.query(DatabaseStatic.TABLE_DETONATOR + id, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                DetonatorBean bean = new DetonatorBean();
                bean.setSchemeId(id);
                bean.setAddress(cursor.getString(DatabaseStatic.Detonator.COL_SHELL));
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
        myHelper.close();
        return list;
    }

    public static List<DetonatorBean> getDetonatorList(Context context, long schemeId) {
        return getTableDetonatorList(context, schemeId, DatabaseStatic.TABLE_DETONATOR + schemeId);
    }

    public static void updateDetonatorList(Context context, List<DetonatorBean> list) {
        if (list.size() > 0)
            updateTableDetonatorList(context, list, DatabaseStatic.TABLE_DETONATOR + list.get(0).getSchemeId());
    }

    public static void clearDetonatorDownloadFlag(Context context, long schemeId) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Detonator.DOWNLOADED, 0);
        db.update(DatabaseStatic.TABLE_DETONATOR + schemeId, values, null, null);
        db.close();
        myHelper.close();
    }

    public static void deleteDetonatorTable(Context context, long schemeId) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.execSQL("drop table " + DatabaseStatic.TABLE_DETONATOR + schemeId);
        db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, DatabaseStatic.TABLE_DETONATOR + schemeId));
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.Scheme.AMOUNT, 0);
        db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{schemeId + ""});
        db.close();
        myHelper.close();
    }

    public static List<ExplosionRecordBean> getExplosionRecordList(Context context) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        List<ExplosionRecordBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.EXPLODE_TIME + " is not null and " + DatabaseStatic.Scheme.DELETED + "=0", null, null, null, null);
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
            bean.setProjectId(cursor.getLong(DatabaseStatic.Scheme.COL_PROJECT_ID));
            bean.setBlasterId(cursor.getLong(DatabaseStatic.Scheme.COL_BLASTER_ID));
            bean.setSynchronize(cursor.getInt(DatabaseStatic.Scheme.COL_SYNCHRONIZE) == 1);
            bean.setTunnel(cursor.getInt(DatabaseStatic.Scheme.COL_TUNNEL) == 1);
            list.add(bean);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return list;
    }

    public static ExplosionRecordBean getExplosionRecord(Context context, long id) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        ExplosionRecordBean bean = new ExplosionRecordBean();
        MyHelper myHelper = new MyHelper(context);
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
            bean.setProjectId(cursor.getLong(DatabaseStatic.Scheme.COL_PROJECT_ID));
            bean.setBlasterId(cursor.getLong(DatabaseStatic.Scheme.COL_BLASTER_ID));
            bean.setTunnel(cursor.getInt(DatabaseStatic.Scheme.COL_TUNNEL) == 1);
            bean.setSynchronize(cursor.getInt(DatabaseStatic.Scheme.COL_SYNCHRONIZE) == 1);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return bean;

    }

    public static void updateExplosionRecord(Context context, ExplosionRecordBean bean) {
        List<ExplosionRecordBean> list = new ArrayList<>();
        list.add(bean);
        updateExplosionRecordList(context, list);
    }

    public static void updateExplosionRecordList(Context context, List<ExplosionRecordBean> list) {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
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
            values.put(DatabaseStatic.Scheme.PROJECT_ID, bean.getProjectId());
            values.put(DatabaseStatic.Scheme.BLASTER_ID, bean.getBlasterId());
            db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{bean.getId() + ""});
        }
        db.close();
        myHelper.close();
    }

    public static List<EnterpriseBean> getEnterpriseList(Context context) {
        List<EnterpriseBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_ENTERPRISE, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            EnterpriseBean bean = new EnterpriseBean();
            bean.setId(cursor.getLong(DatabaseStatic.Enterprise.COL_ID));
            bean.setBlasterId(cursor.getString(DatabaseStatic.Enterprise.COL_BLASTER_ID_CARD));
            bean.setCode(cursor.getString(DatabaseStatic.Enterprise.COL_ENTERPRISE_CODE));
            bean.setCommercial(cursor.getInt(DatabaseStatic.Enterprise.COL_COMMERCIAL) == 1);
            bean.setContract(cursor.getString(DatabaseStatic.Enterprise.COL_CONTRACT_CODE));
            bean.setProject(cursor.getString(DatabaseStatic.Enterprise.COL_PROJECT_CODE));
            bean.setSelected(cursor.getInt(DatabaseStatic.Enterprise.COL_SELECTED) == 1);
            list.add(bean);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return list;
    }

    public static EnterpriseBean getCurrentEnterprise(Context context) {
        EnterpriseBean bean = null;
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_ENTERPRISE, null, DatabaseStatic.Enterprise.SELECTED + "=1", null, null, null, null);
        if (cursor.moveToNext()) {
            bean = new EnterpriseBean();
            bean.setId(cursor.getLong(DatabaseStatic.Enterprise.COL_ID));
            bean.setBlasterId(cursor.getString(DatabaseStatic.Enterprise.COL_BLASTER_ID_CARD));
            bean.setCode(cursor.getString(DatabaseStatic.Enterprise.COL_ENTERPRISE_CODE));
            bean.setCommercial(cursor.getInt(DatabaseStatic.Enterprise.COL_COMMERCIAL) == 1);
            bean.setContract(cursor.getString(DatabaseStatic.Enterprise.COL_CONTRACT_CODE));
            bean.setProject(cursor.getString(DatabaseStatic.Enterprise.COL_PROJECT_CODE));
            bean.setSelected(cursor.getInt(DatabaseStatic.Enterprise.COL_SELECTED) == 1);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return bean;
    }

    public static void updateEnterprise(Context context, EnterpriseBean bean) {
        MyHelper myHelper = new MyHelper(context);
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
        myHelper.close();
    }

    public static void deleteEnterprise(Context context, long id) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.delete(DatabaseStatic.TABLE_ENTERPRISE, DatabaseStatic.Enterprise.ID + "=?", new String[]{id + ""});
        db.close();
        myHelper.close();
    }

    public static DownloadDetonatorBean getDownloadDetonator(Context context, boolean offline) {
        DownloadDetonatorBean bean = new DownloadDetonatorBean();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_DAN_LING, null, DatabaseStatic.DanLing.USED + "=0 and "
                + DatabaseStatic.DanLing.OFFLINE + "=?", new String[]{offline ? "1" : "0"}, null, null, null);
        if (cursor.moveToNext()) {
            DownloadDetonatorBean.ResultBean resultBean = new DownloadDetonatorBean.ResultBean();
            resultBean.setCwxx(cursor.getString(DatabaseStatic.DanLing.COL_ERROR_CODE));
            resultBean.setSqrq(cursor.getString(DatabaseStatic.DanLing.COL_APPLICATION_TIME));
            List<DownloadDetonatorBean.ResultBean.SbbhsBean> sbbhsBeans = new ArrayList<>();
            Cursor c = db.query(DatabaseStatic.TABLE_DOWNLOADED_EXPLODER, null, DatabaseStatic.DownloadedExploder.DAN_LING_ID + "=?"
                    , new String[]{cursor.getLong(DatabaseStatic.DanLing.COL_ID) + ""}, null, null, null);
            while (c.moveToNext()) {
                DownloadDetonatorBean.ResultBean.SbbhsBean b = new DownloadDetonatorBean.ResultBean.SbbhsBean();
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
            DownloadDetonatorBean.ResultBean.ZbqysBean zbqysBean = new DownloadDetonatorBean.ResultBean.ZbqysBean();
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
            DownloadDetonatorBean.ResultBean.JbqysBean jbqysBean = new DownloadDetonatorBean.ResultBean.JbqysBean();
            jbqysBean.setJbqy(jbqyBeans);
            resultBean.setJbqys(jbqysBean);
            c = db.query(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, null, DatabaseStatic.DownloadedDetonator.DAN_LING_ID + "=?"
                    , new String[]{cursor.getLong(DatabaseStatic.DanLing.COL_ID) + ""}, null, null, null);
            List<LgBean> lgBeans = new ArrayList<>();
            while (c.moveToNext()) {
                LgBean b = new LgBean();
                b.setFbh(c.getString(DatabaseStatic.DownloadedDetonator.COL_OFFLINE_SN));
                b.setGzm(c.getString(DatabaseStatic.DownloadedDetonator.COL_WORK_ID));
                b.setGzmcwxx(c.getString(DatabaseStatic.DownloadedDetonator.COL_ERROR_CODE));
                b.setUid(c.getString(DatabaseStatic.DownloadedDetonator.COL_UID));
                b.setYxq(c.getString(DatabaseStatic.DownloadedDetonator.COL_VALID_TIME));
                lgBeans.add(b);
            }
            c.close();
            DownloadDetonatorBean.ResultBean.LgsBean lgsBean = new DownloadDetonatorBean.ResultBean.LgsBean();
            lgsBean.setLg(lgBeans);
            resultBean.setLgs(lgsBean);
            bean.setResult(resultBean);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return bean;
    }

    public static void addDownloadDetonator(Context context, boolean offline, DownloadDetonatorBean bean) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseStatic.DanLing.USED, 1);
        db.update(DatabaseStatic.TABLE_DAN_LING, values, DatabaseStatic.DanLing.OFFLINE + "=?", new String[]{offline ? "1" : "0"});
        values = new ContentValues();
        values.put(DatabaseStatic.DanLing.ERROR_CODE, bean.getResult().getCwxx());
        values.put(DatabaseStatic.DanLing.USED, 0);
        values.put(DatabaseStatic.DanLing.OFFLINE, offline ? 1 : 0);
        values.put(DatabaseStatic.DanLing.APPLICATION_TIME, bean.getResult().getSqrq());
        int id = (int) db.insert(DatabaseStatic.TABLE_DAN_LING, null, values);
        for (DownloadDetonatorBean.ResultBean.SbbhsBean b : bean.getResult().getSbbhs()) {
            values = new ContentValues();
            values.put(DatabaseStatic.DownloadedExploder.DAN_LING_ID, id);
            values.put(DatabaseStatic.DownloadedExploder.EXPLODER_SN, b.getSbbh());
            db.insert(DatabaseStatic.TABLE_DOWNLOADED_EXPLODER, null, values);
        }
        for (ZbqyBean b : bean.getResult().getZbqys().getZbqy()) {
            values = new ContentValues();
            values.put(DatabaseStatic.AllowedArea.DAN_LING_ID, id);
            values.put(DatabaseStatic.AllowedArea.NAME, b.getZbqymc());
            values.put(DatabaseStatic.AllowedArea.LATITUDE, Float.parseFloat(b.getZbqyjd()));
            values.put(DatabaseStatic.AllowedArea.LONGITUDE, Float.parseFloat(b.getZbqywd()));
            values.put(DatabaseStatic.AllowedArea.RADIUS, Float.parseFloat(b.getZbqybj()));
            values.put(DatabaseStatic.AllowedArea.START_TIME, b.getZbqssj());
            values.put(DatabaseStatic.AllowedArea.END_TIME, b.getZbjzsj());
            db.insert(DatabaseStatic.TABLE_ALLOWED_AREA, null, values);
        }
        for (JbqyBean b : bean.getResult().getJbqys().getJbqy()) {
            values = new ContentValues();
            values.put(DatabaseStatic.ForbiddenArea.DAN_LING_ID, id);
            values.put(DatabaseStatic.ForbiddenArea.LATITUDE, Float.parseFloat(b.getJbqyjd()));
            values.put(DatabaseStatic.ForbiddenArea.LONGITUDE, Float.parseFloat(b.getJbqywd()));
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
        db.close();
        myHelper.close();
    }

    public static List<BaiSeInfoBean> getBaiSeInfoList(Context context) {
        List<BaiSeInfoBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
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
        myHelper.close();
        return list;
    }

    public static BaiSeInfoBean getCurrentBaiSeInfo(Context context) {
        BaiSeInfoBean bean = null;
        MyHelper myHelper = new MyHelper(context);
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
        myHelper.close();
        return bean;
    }

    public static void updateBaiSeInfo(Context context, BaiSeInfoBean bean) {
        MyHelper myHelper = new MyHelper(context);
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
        myHelper.close();
    }

    public static void deleteBaiSeProject(Context context, long id) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.delete(DatabaseStatic.TABLE_BAI_SE_PROJECT, DatabaseStatic.BaiSeProject.ID + "=?", new String[]{id + ""});
        db.close();
        myHelper.close();
    }

    public static List<BaiSeBlasterBean> getBaiSeBlasterList(Context context) {
        List<BaiSeBlasterBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
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
            data.setDeviceNO(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_DEVICE_NO));
            data.setGpsCoordinateSystems(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_GPS_COORDINATE_SYSTEMS));
            data.setLngLat(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_LNG_LAT));
            data.setUserIdCard(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_ID_CARD));
            bean.setData(data);
            list.add(bean);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return list;
    }

    public static BaiSeBlasterBean getCurrentBaiSeBlaster(Context context) {
        BaiSeBlasterBean bean = null;
        MyHelper myHelper = new MyHelper(context);
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
            data.setDeviceNO(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_DEVICE_NO));
            data.setGpsCoordinateSystems(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_GPS_COORDINATE_SYSTEMS));
            data.setLngLat(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_LNG_LAT));
            data.setUserIdCard(cursor.getString(DatabaseStatic.BaiSeBlaster.COL_ID_CARD));
            bean.setData(data);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return bean;
    }

    public static void updateBaiSeBlaster(Context context, BaiSeBlasterBean bean) {
        MyHelper myHelper = new MyHelper(context);
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
        values.put(DatabaseStatic.BaiSeBlaster.PROJECT, bean.isProject() ? 1 : 0);
        values.put(DatabaseStatic.BaiSeBlaster.GPS_COORDINATE_SYSTEMS, bean.getData().getGpsCoordinateSystems());
        values.put(DatabaseStatic.BaiSeBlaster.LNG_LAT, bean.getData().getLngLat());
        values.put(DatabaseStatic.BaiSeBlaster.ID_CARD, bean.getData().getUserIdCard());
        if (bean.getId() == -1)
            bean.setId((int) db.insert(DatabaseStatic.TABLE_BAI_SE_BLASTER, null, values));
        else
            db.update(DatabaseStatic.TABLE_BAI_SE_BLASTER, values, DatabaseStatic.BaiSeBlaster.ID + "=?", new String[]{bean.getId() + ""});
        db.close();
        myHelper.close();
    }

    public static void deleteBaiSeBlaster(Context context, long id) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.delete(DatabaseStatic.TABLE_BAI_SE_BLASTER, DatabaseStatic.BaiSeBlaster.ID + "=?", new String[]{id + ""});
        db.close();
        myHelper.close();
    }

    private static List<DetonatorBean> getTableDetonatorList(Context context, long schemeId, String table) {
        List<DetonatorBean> list = new ArrayList<>();
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getReadableDatabase();
        Cursor cursor = db.query(table, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            DetonatorBean bean = new DetonatorBean();
            bean.setSchemeId(schemeId);
            bean.setAddress(cursor.getString(DatabaseStatic.Detonator.COL_SHELL));
            bean.setDownloaded(cursor.getInt(DatabaseStatic.Detonator.COL_DOWNLOADED) == 1);
            bean.setDelayTime(cursor.getInt(DatabaseStatic.Detonator.COL_DELAY_TIME));
            bean.setRow(cursor.getInt(DatabaseStatic.Detonator.COL_ROW));
            bean.setHole(cursor.getInt(DatabaseStatic.Detonator.COL_HOLE));
            bean.setInside(cursor.getInt(DatabaseStatic.Detonator.COL_INSIDE));
            bean.setId(cursor.getInt(DatabaseStatic.Detonator.COL_ID));
            list.add(bean);
        }
        cursor.close();
        db.close();
        myHelper.close();
        return list;
    }

    public static List<DetonatorBean> getAuthDetonatorList(Context context) {
        return getTableDetonatorList(context, -1, DatabaseStatic.TABLE_AUTH_DETONATOR);
    }

    private static void updateTableDetonatorList(Context context, List<DetonatorBean> list, String table) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        db.execSQL("drop table " + table);
        db.execSQL(String.format(DatabaseStatic.CREATE_TABLE_DETONATOR, table));
        for (DetonatorBean bean : list) {
            ContentValues values = new ContentValues();
            values.put(DatabaseStatic.Detonator.SHELL, bean.getAddress());
            values.put(DatabaseStatic.Detonator.DELAY_TIME, bean.getDelayTime());
            values.put(DatabaseStatic.Detonator.ROW, bean.getRow());
            values.put(DatabaseStatic.Detonator.HOLE, bean.getHole());
            values.put(DatabaseStatic.Detonator.INSIDE, bean.getInside());
            values.put(DatabaseStatic.Detonator.DOWNLOADED, bean.isDownloaded() ? 1 : 0);
            bean.setId((int) db.insert(table, null, values));
        }
        if (table.startsWith(DatabaseStatic.TABLE_DETONATOR)) {
            long l = Long.parseLong(table.substring(DatabaseStatic.TABLE_DETONATOR.length()));
            ContentValues values = new ContentValues();
            values.put(DatabaseStatic.Scheme.AMOUNT, list.size());
            db.update(DatabaseStatic.TABLE_SCHEME, values, DatabaseStatic.Scheme.ID + "=?", new String[]{l + ""});
        }
        db.close();
        myHelper.close();
    }

    public static void updateDetonator(Context context, DetonatorBean bean) {
        MyHelper myHelper = new MyHelper(context);
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
        myHelper.close();
    }

    public static void updateAuthDetonatorList(Context context, List<DetonatorBean> list) {
        updateTableDetonatorList(context, list, DatabaseStatic.TABLE_AUTH_DETONATOR);
    }

    public static void cleanDatabase(Context context) {
        MyHelper myHelper = new MyHelper(context);
        SQLiteDatabase db = myHelper.getWritableDatabase();
        Cursor cursor = db.query(DatabaseStatic.TABLE_SCHEME, null,
                DatabaseStatic.Scheme.DELETED + "=1", null, null, null, null);
        while (cursor.moveToNext()) {
            db.execSQL("drop table " + DatabaseStatic.TABLE_DETONATOR + cursor.getLong(DatabaseStatic.Scheme.COL_ID));
        }
        cursor.close();
        db.delete(DatabaseStatic.TABLE_SCHEME, DatabaseStatic.Scheme.DELETED + "=1", null);
        cursor = db.query(DatabaseStatic.TABLE_DAN_LING, null, DatabaseStatic.DanLing.USED + "=1", null, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(DatabaseStatic.DanLing.COL_ID);
            db.delete(DatabaseStatic.TABLE_ALLOWED_AREA, DatabaseStatic.AllowedArea.DAN_LING_ID + "=?", new String[]{id + ""});
            db.delete(DatabaseStatic.TABLE_FORBIDDEN_AREA, DatabaseStatic.ForbiddenArea.DAN_LING_ID + "=?", new String[]{id + ""});
            db.delete(DatabaseStatic.TABLE_DOWNLOADED_DETONATOR, DatabaseStatic.DownloadedDetonator.DAN_LING_ID + "=?", new String[]{id + ""});
            db.delete(DatabaseStatic.TABLE_DOWNLOADED_EXPLODER, DatabaseStatic.DownloadedExploder.DAN_LING_ID + "=?", new String[]{id + ""});
        }
        cursor.close();
        db.delete(DatabaseStatic.TABLE_DAN_LING, DatabaseStatic.DanLing.USED + "=1", null);
        db.close();
        myHelper.close();
    }
}
