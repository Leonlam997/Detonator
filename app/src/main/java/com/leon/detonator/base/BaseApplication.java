package com.leon.detonator.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leon.detonator.R;
import com.leon.detonator.activity.UpdateAppActivity;
import com.leon.detonator.bean.BaiSeCheck;
import com.leon.detonator.bean.BaiSeUpload;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.EnterpriseProjectBean;
import com.leon.detonator.bean.EnterpriseUserBean;
import com.leon.detonator.bean.ExploderBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.RegisterExploderBean;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.bean.UploadListResultBean;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.MD5;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Leon on 2018/1/29.
 */

public class BaseApplication extends Application {
    private static PowerManager.WakeLock wakeLock = null;
    private final Uri APN_LIST_URI = Uri.parse("content://telephony/carriers");
    private boolean tunnel;
    private boolean uploading;
    private boolean getVersion;
    private final static boolean remote = false;
    private boolean registerFinished = true;
    private Context mContext;
    private Vibrator vibrator;
    private String token;
    private Toast mToast;

    private final Handler toastHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG);
            LinearLayout layout = (LinearLayout) mToast.getView();
            TextView tv = (TextView) layout.getChildAt(0);
            tv.setTextSize(26);
            tv.setTextColor(mContext.getColor(R.color.colorToastText));
            mToast.setGravity(Gravity.CENTER, 0, 80);
            mToast.show();
            BaseApplication.writeFile((String) msg.obj);
            return false;
        }
    });
    private LocalSettingBean settingBean;

    public static void writeErrorLog(Exception e) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_SAVE, Locale.getDefault());
            PrintWriter p = new PrintWriter(new FileOutputStream(FilePath.FILE_DEBUG_LOG, true));
            e.printStackTrace(p);
            p.println(df.format(new Date()));
            p.flush();
            p.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public static void writeFile(String s) {
        if (s != null && !s.isEmpty())
            try {
                SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_SAVE, Locale.getDefault());
                PrintWriter p = new PrintWriter(new FileOutputStream(FilePath.FILE_DEBUG_LOG, true));
                p.println(s);
                p.println(df.format(new Date()));
                p.flush();
                p.close();
            } catch (Exception e) {
                writeErrorLog(e);
            }
    }

    public static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X-", b));
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception e) {
            writeErrorLog(e);
        }
        return "";
    }

    public static void acquireWakeLock(Activity activity) {
        if (null == wakeLock)
            try {
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (null != pm) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "Detonator:wakeLock");
                    if (null != wakeLock) {
                        wakeLock.acquire(60 * 60 * 1000L /*10 minutes*/);
                    }
                }
                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
    }

    public static void releaseWakeLock(Activity activity) {
        if (null != wakeLock && wakeLock.isHeld()) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            wakeLock.release();
            wakeLock = null;
        }
    }

    private static double haversine(double theta) {
        double v = Math.sin(theta / 2);
        return v * v;
    }

    private static double convertDegreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

//    private static double convertRadiansToDegrees(double radian) {
//        return radian * 180.0 / Math.PI;
//    }

    public static double distance(double lat1, double lng1, double lat2, double lng2) {
        final double EARTH_RADIUS = 6371.0;//km 地球半径 平均值，千米
        //用haversine公式计算球面两点间的距离。
        //经纬度转换成弧度
        lat1 = convertDegreesToRadians(lat1);
        lng1 = convertDegreesToRadians(lng1);
        lat2 = convertDegreesToRadians(lat2);
        lng2 = convertDegreesToRadians(lng2);

        //差值
        double vLon = Math.abs(lng1 - lng2);
        double vLat = Math.abs(lat1 - lat2);

        //h is the great circle distance in radians, great circle就是一个球体上的切面，它的圆心即是球心的一个周长最大的圆。
        double h = haversine(vLat) + Math.cos(lat1) * Math.cos(lat2) * haversine(vLon);

        return 2 * EARTH_RADIUS * Math.asin(Math.sqrt(h)) * 1000;
    }

    public static <T> T jsonFromString(String source, Class<T> clazz) {
        if (source != null && !source.isEmpty()) {
            try {
                return new Gson().fromJson(source, clazz);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        return null;
    }

    public static LocalSettingBean readSettings() {
        File dataFile = new File(FilePath.FILE_LOCAL_SETTINGS);
        LocalSettingBean settings = new LocalSettingBean();
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null) {
                    temp.append(content);
                }
                br.close();
                fr.close();
                settings = new Gson().fromJson(temp.toString(), LocalSettingBean.class);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return settings;
    }

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            File file = new File(FilePath.APP_PATH);
            if ((!file.exists() && !file.mkdir()) || (file.exists() && !file.isDirectory() && file.delete() && !file.mkdir()))
                myToast(this, R.string.message_create_folder_fail);
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            settingBean = readSettings();
            String apnName = "CMIOT";
            if (!checkApnIsExist(apnName))
                addApn(apnName);
            initFontScale();
            if (!getMobileDataState(this))
                setMobileDataState(this, true);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    public void deleteDetectTempFiles() {
        for (String s : FilePath.FILE_LIST[isTunnel() ? 0 : 1]) {
            File file = new File(s);
            if (file.exists() && !file.delete())
                myToast(this, String.format(Locale.getDefault(), getString(R.string.message_delete_file_fail), file.getName()));
        }
    }

    public void writeToFile(String path, List<? extends BaseJSONBean> list) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (BaseJSONBean bean : list) {
            jsonArray.put(bean.toJSON());
        }
        try {
            File file = new File(path);
            if (file.exists()) {
                if (!file.delete()) {
                    myToast(this, String.format(Locale.getDefault(), getResources().getString(R.string.message_delete_file_fail), file.getName()));
                    return;
                }
            }
            PrintWriter out = new PrintWriter(file);
            out.write(jsonArray.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            writeErrorLog(e);
        }
    }

    public <T extends BaseJSONBean> void readFromFile(String path, List<T> list, Class<T> clazz) {
        try {
            File file = new File(path);
            if (file.exists()) {
                FileReader fr = new FileReader(file);
                BufferedReader reader = new BufferedReader(fr);
                String tempString;
                StringBuilder sb = new StringBuilder();
                while ((tempString = reader.readLine()) != null) {
                    sb.append(tempString);
                }
                reader.close();
                fr.close();
                try {
                    JSONArray jsonArray = new JSONArray(sb.toString());
                    list.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        T bean1 = clazz.newInstance();
                        bean1.fromJSON(jsonArray.getJSONObject(i));
                        list.add(bean1);
                    }
                } catch (Exception e1) {
                    writeErrorLog(e1);
                }
            }
        } catch (IOException e) {
            writeErrorLog(e);
        }
    }

    public void myToast(@NonNull Context context, String msg) {
        if (msg != null && !msg.isEmpty()) {
            mContext = context;
            toastHandler.obtainMessage(1, msg).sendToTarget();
        }
    }

    public void myToast(@NonNull Context context, @StringRes int msg) {
        mContext = context;
        toastHandler.obtainMessage(1, getResources().getString(msg)).sendToTarget();
    }

    public void saveBean(Object bean) {
        try {
            FileWriter fw;
            if (bean instanceof EnterpriseBean)
                fw = new FileWriter(FilePath.FILE_ENTERPRISE_INFO);
            else if (bean instanceof BaiSeUpload)
                fw = new FileWriter(FilePath.FILE_BAI_SE_DATA);
            else if (bean instanceof BaiSeCheck)
                fw = new FileWriter(FilePath.FILE_BAI_SE_CHECK);
            else if (bean instanceof LocalSettingBean) {
                settingBean = (LocalSettingBean) bean;
                fw = new FileWriter(FilePath.FILE_LOCAL_SETTINGS);
            } else
                return;
            fw.append(new Gson().toJson(bean));
            fw.close();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    public static void copyFile(String src, String des) {
        File srcFile = new File(src);
        File desFile = new File(des);
        if ((!desFile.exists() || desFile.delete()) && srcFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(srcFile));
                String line;
                BufferedWriter bw = new BufferedWriter(new FileWriter(desFile));
                while ((line = br.readLine()) != null)
                    bw.write(line);
                bw.flush();
                br.close();
                bw.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

    public EnterpriseBean readEnterprise() {
        File dataFile = new File(FilePath.FILE_ENTERPRISE_INFO);
        EnterpriseBean bean = new EnterpriseBean();
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null)
                    temp.append(content);
                br.close();
                fr.close();
                bean = new Gson().fromJson(temp.toString(), EnterpriseBean.class);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else
            return null;
        return bean;
    }

    public BaiSeUpload readBaiSeUpload() {
        File dataFile = new File(FilePath.FILE_BAI_SE_DATA);
        BaiSeUpload bean = new BaiSeUpload();
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null)
                    temp.append(content);
                br.close();
                fr.close();
                bean = new Gson().fromJson(temp.toString(), BaiSeUpload.class);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else
            return null;
        return bean;
    }

    public BaiSeCheck readBaiSeCheck() {
        File dataFile = new File(FilePath.FILE_BAI_SE_CHECK);
        BaiSeCheck bean = new BaiSeCheck();
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null)
                    temp.append(content);
                br.close();
                fr.close();
                bean = new Gson().fromJson(temp.toString(), BaiSeCheck.class);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else
            return null;
        return bean;
    }

    public List<EnterpriseUserBean.ResultBean.PageListBean> readUserList() {
        List<EnterpriseUserBean.ResultBean.PageListBean> tempList;
        File dataFile = new File(FilePath.FILE_USER_INFO);
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null) {
                    temp.append(content);
                }
                br.close();
                fr.close();
                tempList = new Gson().fromJson(temp.toString(), new TypeToken<List<EnterpriseUserBean.ResultBean.PageListBean>>() {
                }.getType());
                return tempList;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public List<EnterpriseProjectBean.ResultBean.PageListBean> readProjectList(int userID) {
        List<EnterpriseProjectBean.ResultBean.PageListBean> tempList;
        File dataFile = new File(FilePath.FILE_PROJECT_INFO + userID + ".dat");
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null) {
                    temp.append(content);
                }
                br.close();
                fr.close();
                tempList = new Gson().fromJson(temp.toString(), new TypeToken<List<EnterpriseProjectBean.ResultBean.PageListBean>>() {
                }.getType());
                return tempList;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public DownloadDetonatorBean readDownloadList(boolean online) {
        DownloadDetonatorBean bean;
        File dataFile = new File(online ? FilePath.FILE_ONLINE_DOWNLOAD_LIST : FilePath.FILE_OFFLINE_DOWNLOAD_LIST);
        if (dataFile.exists()) {
            try {
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr);
                String content;
                StringBuilder temp = new StringBuilder();
                while ((content = br.readLine()) != null) {
                    temp.append(content);
                }
                br.close();
                fr.close();
                bean = new Gson().fromJson(temp.toString(), DownloadDetonatorBean.class);
                return bean;
            } catch (Exception e) {
                writeErrorLog(e);
            }
        }
        return null;
    }

    public void saveDownloadList(DownloadDetonatorBean bean, boolean online) {
        try {
            FileWriter fw = new FileWriter(online ? FilePath.FILE_ONLINE_DOWNLOAD_LIST : FilePath.FILE_OFFLINE_DOWNLOAD_LIST);
            fw.append(new Gson().toJson(bean));
            fw.close();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    public SoundPool getSoundPool() {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null)
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0);
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(3);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        builder.setAudioAttributes(attrBuilder.build());
        return builder.build();
    }

    public void playSoundVibrate(SoundPool soundPool, int soundID) {
        if (soundPool != null && soundID > 0) {
            soundPool.play(soundID, settingBean.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), settingBean.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), 0, 0, 1);
            if (settingBean.isVibrate() && vibrator != null && vibrator.hasVibrator()) {
                vibrator.cancel();
                vibrator.vibrate(500);
            }
        }
    }

    public void playSound(SoundPool soundPool, int soundID, int loop) {
        if (soundPool != null && soundID > 0) {
            soundPool.play(soundID, settingBean.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), settingBean.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), 0, loop, 1);
        }
    }

    public boolean checkApnIsExist(String ApnName) {
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(APN_LIST_URI, new String[]{"_id", "name", "apn"}, "apn like '%" + ApnName + "%'", null, null);
        if (c != null && c.moveToNext()) {
            //int id = c.getShort(c.getColumnIndex("_id")); //获取该apn的id信息
            c.close();
        } else {
            return false;
        }
        return true;
    }

    public void addApn(String apnName) {
        TelephonyManager iPhoneManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (null != iPhoneManager) {
            String NUMERIC = iPhoneManager.getSimOperator();
            if (NUMERIC == null || NUMERIC.length() < 4) {
                //myToast(this, "不存在SIM卡");
                return;
            }
            ContentResolver resolver = this.getContentResolver();
            ContentValues values = new ContentValues();
            values.put("name", "中爆"); //apn中文描述
            values.put("apn", apnName); //apn名称
            values.put("type", "default,supl");
            values.put("numeric", NUMERIC);
            values.put("mcc", NUMERIC.substring(0, 3));
            values.put("mnc", NUMERIC.substring(3));
            values.put("proxy", "");
            values.put("port", "");
            values.put("mmsProxy".toLowerCase(), "");
            values.put("mmsPort".toLowerCase(), "");
            values.put("user", "");
            values.put("server", "");
            values.put("password", "");
            values.put("mmsc", "");
            Cursor c = null;
            // 获取新添加的apn的ID
            try {
                Uri newRow = resolver.insert(APN_LIST_URI, values);
                if (newRow != null) {
                    c = resolver.query(newRow, null, null, null, null);
                    if (c != null) {
                        //int idindex = c.getColumnIndex("_id");
                        c.moveToFirst();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (c != null)
                c.close();
        }

    }

    public String signature(Map<String, String> map) {
        StringBuilder sign = new StringBuilder();
        List<Map.Entry<String, String>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
        for (Map.Entry<String, String> mapping : list) {
            sign.append(mapping.getKey()).append(mapping.getValue());
        }
        sign.append(getMacAddress());
        return MD5.encryptTo16BitString(sign.toString());
    }

    public String makeToken() {
        String t = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return t.substring(t.length() - 16);
    }

    @SuppressLint("HardwareIds")
    public Map<String, String> makeParams(String token, String method) {
        Map<String, String> params = new HashMap<>();
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager) {
            try {
                String imei = "";
                if (telephonyManager.getDeviceId() != null)
                    imei = telephonyManager.getDeviceId();
                params.put("appKey".toLowerCase(), imei);
                params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
                params.put("token", token);
                params.put("method", method);
            } catch (SecurityException e) {
                writeErrorLog(e);
            }
        }
        return params;
    }

    public void registerExploder() {
        if (registerFinished) {
            registerFinished = false;
            new RegisterExploder().start();
        }
    }

    private void readExploder() {
        if (registerFinished) {
            registerFinished = false;
            new GetExploder().start();
        }
    }

    public boolean isRegisterFinished() {
        return registerFinished;
    }

    public boolean isTunnel() {
        return tunnel;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }

    public String getListFile() {
        return tunnel ? FilePath.FILE_TUNNEL_DELAY_LIST : FilePath.FILE_OPEN_AIR_DELAY_LIST;
    }

    public void getVersion(Handler handler) {
        if (!getVersion) {
            getVersion = true;
            new Thread(() -> OkHttpUtils.get()
                    .url(ConstantUtils.VERSION_URL)
                    .build().execute(new Callback<UpdateVersionBean>() {
                        @Override
                        public UpdateVersionBean parseNetworkResponse(Response response, int i) throws Exception {
                            if (response.body() != null) {
                                String string = Objects.requireNonNull(response.body()).string();
                                return new Gson().fromJson(string, UpdateVersionBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            writeErrorLog(e);
                            getVersion = false;
                            if (handler != null)
                                handler.obtainMessage(UpdateAppActivity.UPDATE_NO_NEW).sendToTarget();
                        }

                        @Override
                        public void onResponse(UpdateVersionBean updateVersionBean, int i) {
                            getVersion = false;
                            if (handler != null)
                                if (updateVersionBean != null)
                                    handler.obtainMessage(UpdateAppActivity.UPDATE_HAS_NEW, updateVersionBean).sendToTarget();
                                else
                                    handler.obtainMessage(UpdateAppActivity.UPDATE_NO_NEW).sendToTarget();
                        }
                    })).start();
        }
    }

    @SuppressLint("HardwareIds")
    public void uploadLog(String fileName) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager) {
            try {
                String id = "";
                if (telephonyManager.getDeviceId() != null)
                    id = telephonyManager.getDeviceId();
                final File file = new File(fileName);
                if (file.exists()) {
                    uploading = true;
                    BaseApplication.writeFile(getString(R.string.message_upload_log) + ", " + fileName);
                    OkHttpUtils.post()
                            .url(ConstantUtils.UPLOAD_LOG_URL)
                            .addFile("file", file.getName().replace(".log", ".txt"), file)
                            .addHeader("MAC", getMacAddress())
                            .addHeader("IMEI", id)
                            .build().execute(new Callback<UploadListResultBean>() {

                                @Override
                                public UploadListResultBean parseNetworkResponse(Response response, int i) throws Exception {
                                    if (response.body() != null) {
                                        String string = Objects.requireNonNull(response.body()).string();
                                        return BaseApplication.jsonFromString(string, UploadListResultBean.class);
                                    }
                                    return null;
                                }

                                @Override
                                public void onError(Call call, Exception e, int i) {
                                    uploading = false;
                                }

                                @Override
                                public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                    uploading = false;
                                    if (null != uploadListResultBean) {
                                        if (uploadListResultBean.isStatus()) {
                                            settingBean.setUploadedLog(true);
                                            saveBean(settingBean);
                                        } else {
                                            myToast(BaseApplication.this, uploadListResultBean.getDescription());
                                        }
                                    }
                                }
                            });
                }
            } catch (SecurityException e) {
                writeErrorLog(e);
            }
        }
    }

    public void initFontScale() {
        Configuration configuration = getResources().getConfiguration();
        final float[] scale = {1f, 1.15f, 1.3f, 1.45f};
        if (settingBean.getFontScale() > 0 && settingBean.getFontScale() < scale.length)
            configuration.fontScale = scale[settingBean.getFontScale()];
        else
            configuration.fontScale = scale[0];
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    public void setMobileDataState(Context context, boolean enabled) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            setDataEnabled.invoke(telephonyService, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getMobileDataState(Context context) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
            Object b = getDataEnabled.invoke(telephonyService);
            if (null != b) {
                return (boolean) b;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isNetSystemUsable(Context context) {
        boolean isNetUsable = false;
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //NetworkCapabilities networkCapabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
//            if (networkCapabilities != null) {
//                isNetUsable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
//            }
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null)
                isNetUsable = networkInfo.isAvailable();
        }
        return isNetUsable;
    }

    private class RegisterExploder extends Thread {
        @SuppressLint("HardwareIds")
        @Override
        public void run() {
            super.run();
            token = makeToken();
            boolean disableWifi = false;
            Map<String, String> params = makeParams(token, MethodUtils.METHOD_EDIT_EXPLODER);
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
                wm.setWifiEnabled(true);
                disableWifi = true;
                while (wm.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        writeErrorLog(e);
                    }
                }
            }
            if (null != params) {
                params.put("mac", getMacAddress());
                if (disableWifi)
                    wm.setWifiEnabled(false);
                //writeFile(getMacAddress());
                if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(BaseApplication.this, Manifest.permission.READ_PHONE_STATE)
                        && null != telephonyManager) {
                    try {
                        if (null != telephonyManager.getSimSerialNumber()) {
                            params.put("iccid", telephonyManager.getSimSerialNumber());
                        }
                        if (null != telephonyManager.getSubscriberId()) {
                            params.put("imsi", telephonyManager.getSubscriberId());
                        }
                        if (null != telephonyManager.getLine1Number()) {
                            params.put("mobilePhone".toLowerCase(), telephonyManager.getLine1Number());
                        }
                    } catch (Exception e) {
                        writeErrorLog(e);
                    }
                }
//                myToast(BaseApplication.this, getMacAddress());
                params.put("signature", signature(params));
            }
            OkHttpUtils.post()
                    .url(ConstantUtils.HOST_URL)
                    .params(params)
                    .build().execute(new Callback<RegisterExploderBean>() {
                        @Override
                        public RegisterExploderBean parseNetworkResponse(Response response, int i) throws Exception {
                            if (response.body() != null) {
                                String string = Objects.requireNonNull(response.body()).string();
                                return jsonFromString(string, RegisterExploderBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            writeErrorLog(e);
                            registerFinished = true;
                        }

                        @SuppressLint("MissingPermission")
                        @Override
                        public void onResponse(RegisterExploderBean registerExploderBean, int i) {
                            if (null != registerExploderBean && registerExploderBean.isStatus() && registerExploderBean.getToken().equals(token)) {
                                if (null != registerExploderBean.getResult()) {
                                    if (settingBean == null)
                                        settingBean = new LocalSettingBean();
                                    settingBean.setRegistered(true);
                                    settingBean.setIMEI(((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
                                    settingBean.setExploderID(registerExploderBean.getResult().getExploder().getCodeID());
                                    BluetoothAdapter.getDefaultAdapter().setName(registerExploderBean.getResult().getExploder().getCodeID());
                                    saveBean(settingBean);
                                } else
                                    myToast(BaseApplication.this, registerExploderBean.getDescription());
                            }
                            registerFinished = true;
                        }
                    });
        }
    }

    public static void customDialog(AlertDialog dialog, boolean setText) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(26);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(26);
        try {
            @SuppressLint("DiscouragedPrivateApi") Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(dialog);
            if (mAlertController != null) {
                Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
                mMessage.setAccessible(true);
                TextView mMessageView = (TextView) mMessage.get(mAlertController);
                if (mMessageView != null) {
                    BaseApplication.writeFile(mMessageView.getText().toString());
                    if (setText) {
                        mMessageView.setTextSize(30);
                        mMessageView.setTextColor(Color.RED);
                        mMessageView.setHeight(100);
                    }
                }
            }
            if (setText) {
                WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                layoutParams.height = 200;
                layoutParams.width = 330;
                dialog.getWindow().setAttributes(layoutParams);
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private class GetExploder extends Thread {
        @Override
        public void run() {
            super.run();
            token = makeToken();
            Map<String, String> params = makeParams(token, MethodUtils.METHOD_GET_EXPLODER);
            if (null != params) {
//                myToast(BaseApplication.this, getMacAddress());
                params.put("signature", signature(params));
            }
            OkHttpUtils.post()
                    .url(ConstantUtils.HOST_URL)
                    .params(params)
                    .build().execute(new Callback<ExploderBean>() {
                        @Override
                        public ExploderBean parseNetworkResponse(Response response, int i) throws Exception {
                            if (response.body() != null) {
                                String string = Objects.requireNonNull(response.body()).string();
                                return jsonFromString(string, ExploderBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            writeErrorLog(e);
                            registerFinished = true;
                        }

                        @Override
                        public void onResponse(ExploderBean exploderBean, int i) {
                            registerFinished = true;
                            if (null != exploderBean && exploderBean.isStatus() && exploderBean.getToken().equals(token)) {
                                if (null != exploderBean.getResult()) {
                                    settingBean.setExploderID(exploderBean.getResult().getCodeID());
                                    saveBean(settingBean);
                                } else
                                    writeFile(exploderBean.getDescription());
                            }
                        }
                    });
        }
    }

    public boolean isUploading() {
        return uploading;
    }

    public static boolean isRemote() {
        return remote;
    }
}
