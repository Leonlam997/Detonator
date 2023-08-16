package com.leon.detonator.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.SDKInitializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leon.detonator.R;
import com.leon.detonator.activity.UpdateAppActivity;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseProjectBean;
import com.leon.detonator.bean.EnterpriseUserBean;
import com.leon.detonator.bean.ExploderBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.RegisterExploderBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.bean.UploadListResultBean;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.MD5;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

@SuppressLint("HardwareIds")
public class BaseApplication extends Application {
    private final Uri APN_LIST_URI = Uri.parse("content://telephony/carriers");
    private static PowerManager.WakeLock wakeLock = null;
    public static LocalSettingBean settings;
    private Vibrator vibrator;
    private Context mContext;
    private String token;
    private Toast mToast;
    public static final int HANDLER_REGISTER_ERROR = 400;
    public static final int HANDLER_REGISTER_SUCCESS = 401;
    public final static boolean isRemote = false;
    public static boolean isTunnel;
    private boolean registerFinished = true;
    private boolean getVersion;
    private int uploadStep;

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
            writeFile((String) msg.obj);
            return false;
        }
    });

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
                writeErrorLog(e);
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
                writeErrorLog(e);
            }
        }
        return null;
    }

    private void readSettings() {
        File dataFile = new File(FilePath.FILE_LOCAL_SETTINGS);
        if (dataFile.exists())
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

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            File file = new File(FilePath.APP_PATH);
            if ((!file.exists() && !file.mkdir()) || (file.exists() && !file.isDirectory() && file.delete() && !file.mkdir()))
                myToast(this, R.string.message_create_folder_fail);
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            readSettings();
            if (settings == null)
                settings = new LocalSettingBean();
            String apnName = "CMIOT";
            SDKInitializer.setAgreePrivacy(this, true);
            LocationClient.setAgreePrivacy(true);
            if (!checkApnIsExist(apnName))
                addApn(apnName);
            initFontScale();
        } catch (Exception e) {
            writeErrorLog(e);
        }
    }

    public void writeToFile(String path, List<? extends BaseJSONBean> list) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (BaseJSONBean bean : list)
                jsonArray.put(bean.toJSON());
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
        } catch (Exception e) {
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

    public void saveSettings() {
        try {
            FileWriter fw;
            fw = new FileWriter(FilePath.FILE_LOCAL_SETTINGS);
            fw.append(new Gson().toJson(settings));
            fw.close();
        } catch (Exception e) {
            writeErrorLog(e);
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
            soundPool.play(soundID, settings.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), settings.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), 0, 0, 1);
            if (settings.isVibrate() && vibrator != null && vibrator.hasVibrator()) {
                vibrator.cancel();
                vibrator.vibrate(500);
            }
        }
    }

    public void playSound(SoundPool soundPool, int soundID, int loop) {
        if (soundPool != null && soundID > 0) {
            soundPool.play(soundID, settings.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), settings.getVolume() / (ConstantUtils.MAX_VOLUME * 1.0f), 0, loop, 1);
        }
    }

    public boolean checkApnIsExist(String ApnName) {
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(APN_LIST_URI, new String[]{"_id", "name", "apn"}, "apn like '%" + ApnName + "%'", null, null);
        if (c != null && c.moveToNext())
            c.close();
        else
            return false;
        return true;
    }

    public void addApn(String apnName) {
        TelephonyManager iPhoneManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (null != iPhoneManager) {
            String NUMERIC = iPhoneManager.getSimOperator();
            if (NUMERIC == null || NUMERIC.length() < 4)
                return;
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
                    if (c != null)
                        c.moveToFirst();
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

    public void registerExploder(Handler handler) {
        if (registerFinished) {
            registerFinished = false;
            new RegisterExploder(handler).start();
        }
    }

    private void readExploder() {
        if (registerFinished) {
            registerFinished = false;
            new GetExploder().start();
        }
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
                                handler.sendEmptyMessage(UpdateAppActivity.UPDATE_NO_NEW);
                        }

                        @Override
                        public void onResponse(UpdateVersionBean updateVersionBean, int i) {
                            getVersion = false;
                            if (handler != null)
                                if (updateVersionBean != null)
                                    handler.obtainMessage(UpdateAppActivity.UPDATE_HAS_NEW, updateVersionBean).sendToTarget();
                                else
                                    handler.sendEmptyMessage(UpdateAppActivity.UPDATE_NO_NEW);
                        }
                    })).start();
        }
    }

    public void uploadLog(Handler handler) {
        if (uploadStep != 0)
            handler.obtainMessage(HANDLER_REGISTER_ERROR, uploadStep, 0).sendToTarget();
        else {
            uploadStep = 1;
            startUpload(handler);
        }
    }

    private void startUpload(Handler handler) {
        new Thread(() -> {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (null != telephonyManager) {
                try {
                    String id = "";
                    if (telephonyManager.getDeviceId() != null)
                        id = telephonyManager.getDeviceId();
                    final File file = new File(uploadStep == 1 ? FilePath.FILE_SERIAL_LOG : FilePath.FILE_DEBUG_LOG);
                    if (file.exists()) {
                        writeFile(getString(R.string.message_upload_log) + ", " + file.getName());
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
                                            return jsonFromString(string, UploadListResultBean.class);
                                        }
                                        return null;
                                    }

                                    @Override
                                    public void onError(Call call, Exception e, int i) {
                                        handler.obtainMessage(HANDLER_REGISTER_ERROR, 3, 0).sendToTarget();
                                        uploadStep = 0;
                                    }

                                    @Override
                                    public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                        if (null != uploadListResultBean) {
                                            if (uploadListResultBean.isStatus()) {
                                                if (uploadStep == 1) {
                                                    uploadStep = 2;
                                                    startUpload(handler);
                                                } else {
                                                    uploadStep = 0;
                                                    settings.setUploadedLog(true);
                                                    saveSettings();
                                                    handler.obtainMessage(HANDLER_REGISTER_SUCCESS, 1, 0).sendToTarget();
                                                }
                                            } else {
                                                uploadStep = 0;
                                                handler.obtainMessage(HANDLER_REGISTER_ERROR, uploadListResultBean.getDescription()).sendToTarget();
                                            }
                                        }
                                    }
                                });
                    }
                } catch (SecurityException e) {
                    writeErrorLog(e);
                    handler.obtainMessage(HANDLER_REGISTER_ERROR, 3, 0).sendToTarget();
                    uploadStep = 0;
                }
            }
        }).start();
    }

    public void initFontScale() {
        Configuration configuration = getResources().getConfiguration();
        final float[] scale = {1f, 1.15f, 1.3f, 1.45f};
        if (settings.getFontScale() > 0 && settings.getFontScale() < scale.length)
            configuration.fontScale = scale[settings.getFontScale()];
        else
            configuration.fontScale = scale[0];
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    public static boolean isNetSystemUsable(Context context) {
        boolean isNetUsable = false;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null)
            isNetUsable = networkInfo.isAvailable();
        return isNetUsable;
    }

    public static boolean isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isAvailable() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static boolean isNetPingUsable() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec("ping -c 3 www.zhongbao360.com");
            int ret = process.waitFor();
            return ret == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void importOldData() {
        new Handler(msg -> {
            try {
                File[] files = new File(FilePath.APP_PATH + "/Records/").listFiles();
                if (null != files) {
                    Arrays.sort(files, (f1, f2) -> (int) (f1.lastModified() - f2.lastModified()));
                    SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
                    for (File file : files) {
                        String[] info = file.getName().split("_");
                        if (5 == info.length) {
                            SchemeBean schemeBean = new SchemeBean();
                            schemeBean.setName(getString(R.string.text_import));
                            DbUtil.updateScheme(schemeBean, info[0].equals("T"));
                            List<DetonatorBean> temp = new ArrayList<>();
                            readFromFile(file.getAbsolutePath(), temp, DetonatorBean.class);
                            for (DetonatorBean bean : temp)
                                bean.setSchemeId(schemeBean.getId());
                            DbUtil.updateDetonatorList(temp);
                            ExplosionRecordBean recordBean = new ExplosionRecordBean();
                            info[4] = info[4].substring(0, info[4].length() - 4);
                            recordBean.setId(schemeBean.getId());
                            recordBean.setExplodeTime(formatter.parse(info[1]));
                            recordBean.setLat(Double.parseDouble(info[3]));
                            recordBean.setLng(Double.parseDouble(info[4]));
                            if (info[2].equals("U")) {
                                recordBean.setUploadServer(BaseApplication.settings.getServerHost());
                                recordBean.setUploadTime(new Date(file.lastModified()));
                            } else
                                recordBean.setUploadServer(-1);
                            DbUtil.updateExplosionRecord(recordBean);
                        }
                        if (!file.delete())
                            BaseApplication.writeFile(getString(R.string.message_delete_fail));
                    }
                    if (!new File(FilePath.APP_PATH + "/Records").delete())
                        BaseApplication.writeFile(getString(R.string.message_delete_fail));
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    private class RegisterExploder extends Thread {
        private final Handler handler;

        public RegisterExploder(Handler handler) {
            this.handler = handler;
        }

        @SuppressLint("MissingPermission")
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
                if (null != telephonyManager) {
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
                            if (handler != null)
                                handler.sendEmptyMessage(HANDLER_REGISTER_ERROR);
                            registerFinished = true;
                        }

                        @SuppressLint("MissingPermission")
                        @Override
                        public void onResponse(RegisterExploderBean registerExploderBean, int i) {
                            if (null != registerExploderBean && registerExploderBean.isStatus() && registerExploderBean.getToken().equals(token)) {
                                if (null != registerExploderBean.getResult()) {
                                    settings.setRegistered(true);
                                    settings.setIMEI(((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
                                    settings.setExploderID(registerExploderBean.getResult().getExploder().getCodeID());
                                    BluetoothAdapter.getDefaultAdapter().setName(registerExploderBean.getResult().getExploder().getCodeID());
                                    saveSettings();
                                } else
                                    myToast(BaseApplication.this, registerExploderBean.getDescription());
                            }
                            if (handler != null)
                                handler.sendEmptyMessage(HANDLER_REGISTER_SUCCESS);
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
                    writeFile(mMessageView.getText().toString());
                    if (setText) {
                        mMessageView.setTextSize(30);
                        mMessageView.setTextColor(Color.RED);
                        mMessageView.setHeight(100);
                    }
                }
            }
            WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
            if (setText)
                layoutParams.height = 200;
            layoutParams.width = 330;
            dialog.getWindow().setAttributes(layoutParams);
            dialog.setOnKeyListener((dialog1, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isShown())
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).callOnClick();
                return false;
            });
        } catch (Exception e) {
            writeErrorLog(e);
        }
    }

    public void shortCircuit(BaseActivity activity, Handler handler) {
        handler.removeCallbacksAndMessages(null);
        DataReceiveListener.getInstance(activity, handler).closeAllHandler();
        SoundPool soundPool = getSoundPool();
        if (soundPool != null) {
            int soundAlert = soundPool.load(this, R.raw.alert, 1);
            if (soundAlert != 0) {
                playSoundVibrate(soundPool, soundAlert);
                new Handler(msg -> {
                    soundPool.unload(soundAlert);
                    soundPool.release();
                    return false;
                }).sendEmptyMessageDelayed(1, 1500);
            } else
                soundPool.release();
        }
        activity.runOnUiThread(() -> customDialog(new AlertDialog.Builder(activity, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_warning)
                .setCancelable(false)
                .setMessage(R.string.dialog_short_circuit)
                .setPositiveButton(R.string.button_confirm, (dialog, which) -> activity.finish())
                .show(), true));
    }

    private class GetExploder extends Thread {
        @Override
        public void run() {
            super.run();
            token = makeToken();
            Map<String, String> params = makeParams(token, MethodUtils.METHOD_GET_EXPLODER);
            if (null != params)
                params.put("signature", signature(params));
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
                                    settings.setExploderID(exploderBean.getResult().getCodeID());
                                    saveSettings();
                                } else
                                    writeFile(exploderBean.getDescription());
                            }
                        }
                    });
        }
    }
}
