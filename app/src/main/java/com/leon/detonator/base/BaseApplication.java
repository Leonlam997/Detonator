package com.leon.detonator.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.leon.detonator.R;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.ExploderBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.RegisterExploderBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bean.UpdateVersionBean;
import com.leon.detonator.bean.UploadListResultBean;
import com.leon.detonator.bluetooth.BluetoothService;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.util.CRC16;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.MD5;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
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

public class BaseApplication extends Application {
    private static PowerManager.WakeLock wakeLock = null;
    public static LocalSettingBean settings;
    private TextToSpeech textToSpeech;
    private BluetoothService btService;
    private Context mContext;
    private String token;
    private Toast mToast;
    private StringBuilder btData;
    private final static boolean remote = false;
    private boolean registerFinished = true;
    private boolean uploading;
    private boolean getVersion;
    private boolean btSender;

    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case ConstantUtils.BT_CONNECTED:
                myToast(BaseApplication.this, String.format(getString(R.string.bt_connected_device), msg.obj));
                btData = new StringBuilder();
                break;
            case ConstantUtils.BT_DATA:
                if (!btSender && msg.arg1 > 0) {
                    btData.append(new String(Arrays.copyOfRange((byte[]) msg.obj, 0, msg.arg1)));
                    msg.getTarget().removeMessages(ConstantUtils.BT_RESEND);
                    if (!btData.toString().endsWith("]")) {
                        msg.getTarget().sendEmptyMessageDelayed(ConstantUtils.BT_RESEND, ConstantUtils.BT_RESEND_TIMEOUT);
                    } else if (!btData.toString().startsWith(CRC16.getTableCRC(btData.substring(4).getBytes()))) {
                        btData = new StringBuilder();
                        btService.write(ConstantUtils.BT_RESEND_ACK.getBytes());
                    } else {
                        btService.write(ConstantUtils.BT_SUCCESS_ACK.getBytes());
                        btService.cancelAllBtThread();
                        btService.acceptWait();
                        myToast(BaseApplication.this, R.string.message_receive_success);
                        saveData(btData.toString());
                        btData = null;
                    }
                }
                break;
            case ConstantUtils.BT_RESEND:
                btData = new StringBuilder();
                btService.write(ConstantUtils.BT_RESEND_ACK.getBytes());
                break;
            case ConstantUtils.BT_ERROR:
                btData = null;
                msg.getTarget().removeMessages(ConstantUtils.BT_ERROR);
                myToast(BaseApplication.this, (String) msg.obj);
                btService.cancelAllBtThread();
                btService.acceptWait();
                break;
            case ConstantUtils.APP_TOAST:
                if (mToast != null)
                    mToast.cancel();
                BaseApplication.writeFile((String) msg.obj);
                mToast = Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG);
                LinearLayout layout = (LinearLayout) mToast.getView();
                TextView tv = (TextView) layout.getChildAt(0);
                layout.setBackground(AppCompatResources.getDrawable(mContext, R.drawable.shape_toast_bg));
                tv.setTextSize(20);
                tv.setTextColor(getColor(R.color.colorToastText));
                mToast.setGravity(Gravity.CENTER, 0, 280);
                mToast.show();
                speakText((String) msg.obj);
                return false;
        }
        return false;
    });

    public void shortCircuit(BaseActivity activity, Handler handler) {
        handler.removeCallbacksAndMessages(null);
        DataReceiveListener.getInstance(activity, handler).closeAllHandler();
        writeFile(getString(R.string.dialog_short_circuit) + ", " + activity.getLocalClassName());
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
            } else {
                soundPool.release();
            }
        }
        activity.runOnUiThread(() -> customDialog(new AlertDialog.Builder(this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_warning)
                .setCancelable(false)
                .setMessage(R.string.dialog_short_circuit)
                .setPositiveButton(R.string.button_confirm, (dialog, which) -> activity.finish())
                .show()));
    }

    private void saveData(String data) {
        String[] scheme = data.substring(4).split("\n");
        if (scheme.length == 3)
            try {
                SchemeBean bean = new SchemeBean();
                List<DetonatorBean> list = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(scheme[2]);
                for (int i = 0; i < jsonArray.length(); i++) {
                    DetonatorBean bean1 = new DetonatorBean();
                    bean1.fromJSON(jsonArray.getJSONObject(i));
                    list.add(bean1);
                }
                bean.setName(scheme[0]);
                DbUtil.updateScheme(this, bean, Boolean.parseBoolean(scheme[1]));
                for (DetonatorBean bean1 : list)
                    bean1.setSchemeId(bean.getId());
                DbUtil.updateDetonatorList(this, list);
                myToast(BaseApplication.this, R.string.message_save_list_success);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
    }

    public static void writeErrorLog(Exception e) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
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
                SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
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

    private static LocalSettingBean readSettings() {
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
            settings = readSettings();
            btService = new BluetoothService(myHandler);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
            filter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
            registerReceiver(new BluetoothBroadcast(), filter);
            textToSpeech = new TextToSpeech(this, null);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    public void writeToFile(String path, List<? extends BaseJSONBean> list) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (BaseJSONBean bean : list)
            jsonArray.put(bean.toJSON());
        try {
            File file = new File(path);
            if (file.exists() && !file.delete()) {
                myToast(this, String.format(Locale.getDefault(), getResources().getString(R.string.message_delete_file_fail), file.getName()));
                return;
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
                while ((tempString = reader.readLine()) != null)
                    sb.append(tempString);
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
            myHandler.obtainMessage(ConstantUtils.APP_TOAST, msg).sendToTarget();
        }
    }

    public void myToast(@NonNull Context context, @StringRes int msg) {
        myToast(context, getResources().getString(msg));
    }

    public static void saveSettings() {
        try {
            FileWriter fw;
            fw = new FileWriter(FilePath.FILE_LOCAL_SETTINGS);
            fw.append(new Gson().toJson(settings));
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
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            soundPool.play(soundID, maxVolume / (currentVolume * 1.0f), currentVolume / (maxVolume * 1.0f), 0, 0, 1);
            switch (mAudioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    if (1 != Settings.System.getInt(getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, 0))
                        break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    vibrator.cancel();
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    public void playSound(SoundPool soundPool, int soundID, int loop) {
        if (soundPool != null && soundID > 0) {
            AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            soundPool.play(soundID, maxVolume / (currentVolume * 1.0f), currentVolume / (maxVolume * 1.0f), 0, loop, 1);
        }
    }

    public String signature(Map<String, String> map) {
        StringBuilder sign = new StringBuilder();
        List<Map.Entry<String, String>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByKey());
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

    public String getListFile() {
        return BaseApplication.readSettings().isTunnel() ? FilePath.FILE_TUNNEL_DELAY_LIST : FilePath.FILE_OPEN_AIR_DELAY_LIST;
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
                        }

                        @Override
                        public void onResponse(UpdateVersionBean updateVersionBean, int i) {
                            getVersion = false;
                            if (updateVersionBean != null && handler != null) {
                                handler.obtainMessage(1, updateVersionBean).sendToTarget();
                            }
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
                                    writeErrorLog(e);
                                    uploading = false;
                                }

                                @Override
                                public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                    uploading = false;
                                    if (null != uploadListResultBean) {
                                        if (uploadListResultBean.isStatus()) {
                                            settings.setUploadedLog(true);
                                            saveSettings();
                                        } else
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

    public static boolean isNetSystemUsable(Context context) {
        boolean isNetUsable = false;
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null)
                isNetUsable = networkInfo.isAvailable();
        }
        return isNetUsable;
    }

    public static void customDialog(AlertDialog dialog) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextSize(ConstantUtils.DIALOG_BUTTON_TEXT_SIZE);
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextSize(ConstantUtils.DIALOG_BUTTON_TEXT_SIZE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextSize(ConstantUtils.DIALOG_BUTTON_TEXT_SIZE);
        dialog.setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_TAB && dialog.getButton(DialogInterface.BUTTON_NEUTRAL).isShown())
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).callOnClick();
                else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && dialog.getButton(DialogInterface.BUTTON_NEGATIVE).isShown())
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).callOnClick();
            return false;
        });
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

                        @Override
                        public void onResponse(RegisterExploderBean registerExploderBean, int i) {
                            if (null != registerExploderBean && registerExploderBean.isStatus() && registerExploderBean.getToken().equals(token)) {
                                if (null != registerExploderBean.getResult()) {
                                    if (settings == null)
                                        settings = new LocalSettingBean();
                                    settings.setRegistered(true);
                                    settings.setIMEI(((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
                                    settings.setExploderID(registerExploderBean.getResult().getExploder().getCodeID());
                                    if (ActivityCompat.checkSelfPermission(BaseApplication.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                        BluetoothAdapter.getDefaultAdapter().setName(registerExploderBean.getResult().getExploder().getCodeID());
                                        saveSettings();
                                    }
                                } else
                                    myToast(BaseApplication.this, registerExploderBean.getDescription());
                            }
                            registerFinished = true;
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
                                    writeFile("GetExploder" + exploderBean.getDescription());
                            }
                        }
                    });
        }
    }

    public void speakText(String text) {
        if (textToSpeech.isSpeaking())
            textToSpeech.stop();
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null);
    }

    public void setBtSender(boolean btSender) {
        this.btSender = btSender;
    }

    public boolean isUploading() {
        return uploading;
    }

    public static boolean isRemote() {
        return remote;
    }

    public static void setDiscoverableTimeout(BluetoothAdapter adapter, int timeout) {
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private class BluetoothBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        btService.acceptWait();
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        btService.cancelAllBtThread();
                        break;
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                        if (blueState == BluetoothAdapter.STATE_OFF) {
                            btService.cancelAllBtThread();
                        } else if (blueState == BluetoothAdapter.STATE_ON) {
                            setDiscoverableTimeout(BluetoothAdapter.getDefaultAdapter(), 100);
                            btService.acceptWait();
                        }
                        break;
                    default:
                        Class<BluetoothAdapter> bluetoothAdapterClass = BluetoothAdapter.class;//得到BluetoothAdapter的Class对象
                        try {//得到连接状态的方法
                            Method method = bluetoothAdapterClass.getDeclaredMethod("getConnectionState", (Class<?>) null);
                            //打开权限
                            method.setAccessible(true);
                            Object state = method.invoke(BluetoothAdapter.getDefaultAdapter(), (Object[]) null);
                            if (null != state) {
                                if ((int) state == BluetoothAdapter.STATE_CONNECTED) {
                                    btService.acceptWait();
                                }
                            }
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                        break;
                }
            }
        }
    }
}
