package com.leon.detonator.Base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
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
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Bean.DownloadDetonatorBean;
import com.leon.detonator.Bean.EnterpriseBean;
import com.leon.detonator.Bean.EnterpriseProjectBean;
import com.leon.detonator.Bean.EnterpriseUserBean;
import com.leon.detonator.Bean.ExploderBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Bean.RegisterExploderBean;
import com.leon.detonator.Bean.UploadDetonatorBean;
import com.leon.detonator.Bean.UploadListResultBean;
import com.leon.detonator.Bean.UploadServerBean;
import com.leon.detonator.R;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.MD5;
import com.leon.detonator.Util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
            mToast = Toast.makeText(mContext, msg.getData().getString("Message"), Toast.LENGTH_LONG);
            LinearLayout layout = (LinearLayout) mToast.getView();
            TextView tv = (TextView) layout.getChildAt(0);
            tv.setTextSize(26);
            tv.setTextColor(mContext.getColor(R.color.colorToastText));
            mToast.setGravity(Gravity.CENTER, 0, 80);
            mToast.show();
            return false;
        }
    });
    private LocalSettingBean settingBean;

    public static void writeErrorLog(Exception e) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
            PrintWriter p = new PrintWriter(new FileOutputStream(FilePath.FILE_ERROR_LOG, true));
            e.printStackTrace(p);
            p.println(df.format(new Date()));
            p.flush();
            p.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public static void writeFile(String s) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
            PrintWriter p = new PrintWriter(new FileOutputStream(FilePath.FILE_DEBUG_LOG, true));
            p.println(s);
            p.println(df.format(new Date()));
            p.flush();
            p.close();
        } catch (Exception e1) {
            e1.printStackTrace();
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
        if (null == wakeLock) {
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
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            settingBean = readSettings();
            if (isNetSystemUsable(this)) {
                if (!settingBean.isRegistered()) {
                    registerExploder();
                } else if (null == readSettings().getExploderID() || (null != readSettings().getExploderID() && readSettings().getExploderID().isEmpty())) {
                    readExploder();
                }
            }
            File file = new File(FilePath.APP_PATH);
            if ((!file.exists() && !file.mkdir()) || (file.exists() && !file.isDirectory() && file.delete() && !file.mkdir())) {
                myToast(this, R.string.message_create_folder_fail);
            }
            file = new File(FilePath.FILE_SERIAL_LOG);
            if (file.exists() && file.length() > 4 * 1024 * 1024) {
                if (file.length() > 20 * 1024 * 1024) {
                    if (!file.delete())
                        myToast(this, R.string.message_delete_fail);
                } else
                    trimFile(FilePath.FILE_SERIAL_LOG);
            }
            file = new File(FilePath.FILE_ERROR_LOG);
            if (file.exists() && file.length() > 4 * 1024 * 1024) {
                trimFile(FilePath.FILE_ERROR_LOG);
            }
            file = new File(FilePath.FILE_DEBUG_LOG);
            if (file.exists() && file.length() > 4 * 1024 * 1024) {
                trimFile(FilePath.FILE_DEBUG_LOG);
            }
            String apnName = "CMIOT";
            if (!checkApnIsExist(apnName)) {
                addApn(apnName);
            }
            initFontScale();
            if (!getMobileDataState(this)) {
                setMobileDataState(this, true);
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void trimFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            String read;
            StringBuilder data = new StringBuilder();
            while ((read = br.readLine()) != null)
                data.append(read).append('\n');
            br.close();
            if (new File(fileName).delete()) {
                BufferedWriter bfw = new BufferedWriter(new FileWriter(fileName, false));
                bfw.write(data.toString(), data.length() / 2, data.length() / 2);
                bfw.flush();
                bfw.close();
            }
        } catch (Exception e) {
            writeErrorLog(e);
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
                    myToast(this, String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
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
            Message message = toastHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("Message", msg);
            mContext = context;
            message.setData(bundle);
            toastHandler.sendMessage(message);
        }
    }

    public void myToast(@NonNull Context context, @StringRes int msg) {
        Message message = toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("Message", getResources().getString(msg));
        mContext = context;
        message.setData(bundle);
        toastHandler.sendMessage(message);
    }

    public void saveSettings(LocalSettingBean settings) {
        try {
            settingBean = settings;
            FileWriter fw = new FileWriter(FilePath.FILE_LOCAL_SETTINGS);
            fw.append(new Gson().toJson(settings));
            fw.close();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
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
                while ((content = br.readLine()) != null) {
                    temp.append(content);
                }
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

    public void saveEnterprise(EnterpriseBean enterprise) {
        try {
            FileWriter fw = new FileWriter(FilePath.FILE_ENTERPRISE_INFO);
            fw.append(new Gson().toJson(enterprise));
            fw.close();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
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

    public synchronized void uploadExplodeList() {
        final List<UploadServerBean> list = new ArrayList<>();
        readFromFile(FilePath.FILE_UPLOAD_LIST, list, UploadServerBean.class);
        for (final UploadServerBean b : list) {
            if (!b.isUploadServer()) {
                File file = new File(FilePath.FILE_DETONATE_RECORDS + "/" + b.getFile());
                if (file.exists()) {
                    token = makeToken();
                    Map<String, String> params = makeParams(token, MethodUtils.METHOD_UPLOAD_EXPLODE_LIST);
                    if (null != params) {
                        try {
                            SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
                            List<DetonatorInfoBean> temp = new ArrayList<>();
                            readFromFile(file.getAbsolutePath(), temp, DetonatorInfoBean.class);
                            String[] info = file.getName().split("_");
                            if (5 == info.length) {
                                info[4] = info[4].substring(0, info[4].length() - 4);
                                List<UploadDetonatorBean> detonatorList = new ArrayList<>();
                                for (DetonatorInfoBean b1 : temp) {
                                    UploadDetonatorBean b2 = new UploadDetonatorBean();
                                    b2.setDSC(b1.getAddress());
                                    b2.setBlastDelayTime(b1.getDelayTime());
                                    b2.setBlastHole(b1.getHole());
                                    b2.setBlastRow(b1.getRow());
                                    b2.setBlastInside(b1.getInside());
                                    detonatorList.add(b2);
                                }
                                params.put("EnvironmentType".toLowerCase(), info[0].startsWith("O") ? "OpenAir" : "DownHole");
                                params.put("BlastTime".toLowerCase(), info[1]);
                                params.put("BlastLat".toLowerCase(), info[3]);
                                params.put("BlastLng".toLowerCase(), info[4]);
                                if (null != b.getServer() && !b.getServer().isEmpty()) {
                                    String[] server = b.getServer().split(":");
                                    if (server.length == 2) {
                                        params.put("zbServerIp".toLowerCase(), server[0]);
                                        params.put("zbServerPort".toLowerCase(), server[1]);
                                    }
                                }
                                params.put("isZbUploadSuccess".toLowerCase(), b.isUploaded() + "");
                                params.put("zbUploadSuccessTime".toLowerCase(), null == b.getUploadTime() ? "" : formatter.format(b.getUploadTime()));
                                params.put("detonator", new Gson().toJson(detonatorList));
                                params.put("signature", signature(params));
                                OkHttpUtils.post()
                                        .url(ConstantUtils.HOST_URL)
                                        .params(params)
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

                                            }

                                            @Override
                                            public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                                if (null != uploadListResultBean) {
                                                    if (uploadListResultBean.getToken().equals(token)) {
                                                        if (uploadListResultBean.isStatus()) {
                                                            b.setUploadServer(true);
                                                            try {
                                                                writeToFile(FilePath.FILE_UPLOAD_LIST, list);
                                                            } catch (Exception e) {
                                                                writeErrorLog(e);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        });
                            }
                        } catch (Exception e) {
                            writeErrorLog(e);
                        }
                    }
                }
            }
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

                                }

                                @Override
                                public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                    if (null != uploadListResultBean) {
                                        if (uploadListResultBean.isStatus())
                                            myToast(BaseApplication.this, R.string.message_upload_success);
                                        else
                                            myToast(BaseApplication.this, uploadListResultBean.getDescription());
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
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities =
                manager.getNetworkCapabilities(manager.getActiveNetwork());
        if (networkCapabilities != null) {
            isNetUsable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
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
                            registerFinished = true;
                        }

                        @Override
                        public void onResponse(RegisterExploderBean registerExploderBean, int i) {
                            registerFinished = true;

                            if (null != registerExploderBean && registerExploderBean.isStatus() && registerExploderBean.getToken().equals(token)) {
                                if (null != registerExploderBean.getResult()) {
                                    settingBean.setRegistered(true);
                                    settingBean.setIMEI(((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
                                    settingBean.setExploderID(registerExploderBean.getResult().getExploder().getCodeID());
                                    if (ActivityCompat.checkSelfPermission(BaseApplication.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                        BluetoothAdapter.getDefaultAdapter().setName(registerExploderBean.getResult().getExploder().getCodeID());
                                        saveSettings(settingBean);
                                    }
                                } else {
                                    myToast(BaseApplication.this, registerExploderBean.getDescription());
                                }
                            }
                        }
                    });
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
                                return BaseApplication.jsonFromString(string, ExploderBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            registerFinished = true;
                        }

                        @Override
                        public void onResponse(ExploderBean exploderBean, int i) {
                            registerFinished = true;
                            if (null != exploderBean && exploderBean.isStatus() && exploderBean.getToken().equals(token)) {
                                if (null != exploderBean.getResult()) {
                                    settingBean.setExploderID(exploderBean.getResult().getCodeID());
                                    saveSettings(settingBean);
                                }
                            }
                        }
                    });
        }
    }
}
