package com.leon.detonator.base;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by Administrator on 2018/1/24.
 */

public abstract class BaseActivity extends AppCompatActivity {
    private static boolean isWifiConnected = false;
    private final int CHANGE_VOLTAGE = 1,
            CHANGE_CURRENT = 2,
            CHANGE_BATTERY = 3,
            CHANGE_BLUETOOTH = 4,
            CHANGE_GPS = 5,
            CHANGE_WIFI = 6,
            CHANGE_NETWORK = 7;
    private LinearLayout parentLinearLayout;
    private View statusBar, actionBar;
    private TextView tvBattery, tvCurrentText, tvVoltageText, tvCurrent, tvVoltage;
    private ImageView ivBattery, ivBluetooth, ivWifi, ivNetwork, ivGPS;
    private final Handler changeStatus = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_VOLTAGE:
                    tvCurrentText.setVisibility(View.VISIBLE);
                    tvVoltageText.setVisibility(View.VISIBLE);
                    tvCurrent.setVisibility(View.VISIBLE);
                    tvVoltage.setVisibility(View.VISIBLE);
                    tvVoltage.setText((String) msg.obj);
                    break;
                case CHANGE_CURRENT:
                    tvCurrentText.setVisibility(View.VISIBLE);
                    tvVoltageText.setVisibility(View.VISIBLE);
                    tvCurrent.setVisibility(View.VISIBLE);
                    tvVoltage.setVisibility(View.VISIBLE);
                    tvCurrent.setText((String) msg.obj);
                    break;
                case CHANGE_BATTERY:
                    tvBattery.setText(String.format(Locale.getDefault(), "%d%%", msg.arg1));
                    if (1 == msg.arg2)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_charging);
                    else if (msg.arg1 >= 80)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_1);
                    else if (msg.arg1 >= 60)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_2);
                    else if (msg.arg1 >= 40)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_3);
                    else if (msg.arg1 >= 20)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_4);
                    else if (msg.arg1 >= 10)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_5);
                    else if (msg.arg1 >= 0)
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_6);
                    else
                        ivBattery.setBackgroundResource(R.mipmap.ic_battery_7);
                    break;
                case CHANGE_BLUETOOTH:
                    if (0 != msg.arg1)
                        ivBluetooth.setBackgroundResource(msg.arg1);
                    break;
                case CHANGE_GPS:
                    if (0 != msg.arg1)
                        ivGPS.setBackgroundResource(msg.arg1);
                    break;
                case CHANGE_WIFI:
                    if (0 != msg.arg1)
                        ivWifi.setBackgroundResource(msg.arg1);
                    break;
                case CHANGE_NETWORK:
                    if (0 != msg.arg1)
                        ivNetwork.setBackgroundResource(msg.arg1);
                    break;
                default:
                    break;
            }
            return false;
        }
    });
    private Context mContext;
    private BatteryBroadcast batteryBroadcast;
    private BluetoothBroadcast bluetoothBroadcast;
    private GpsStatusBroadcast gpsStatusBroadcast;
    private RssiBroadcast rssiBroadcast;
    private NetworkBroadcast networkBroadcast;
    private TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initContentView();
        initFontScale();
        mContext = this;
    }

    public void hideActionBar() {
        statusBar.setBackgroundResource(R.drawable.bmp_login_status_bar_background);
        parentLinearLayout.removeView(actionBar);
    }

    private void initContentView() {
        ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
        viewGroup.removeAllViews();
        parentLinearLayout = new LinearLayout(this);
        parentLinearLayout.setOrientation(LinearLayout.VERTICAL);
        viewGroup.addView(parentLinearLayout);
        statusBar = LayoutInflater.from(this).inflate(R.layout.layout_status_bar, parentLinearLayout, true);
        actionBar = LayoutInflater.from(this).inflate(R.layout.layout_action_bar, parentLinearLayout, false);
        parentLinearLayout.addView(actionBar);
        initStatusBar();
        initActionBar();
    }

    private void initActionBar() {
        findViewById(R.id.btn_back).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initStatusBar() {
        statusBar.setBackgroundColor(getColor(R.color.colorActionbarBackground));
        tvBattery = findViewById(R.id.tvBattery);
        ivBattery = findViewById(R.id.ivBattery);
        ivBluetooth = findViewById(R.id.ivBluetooth);
        ivWifi = findViewById(R.id.ivWifiSignal);
        ivNetwork = findViewById(R.id.ivNetworkSignal);
        ivGPS = findViewById(R.id.ivGps);

        tvCurrentText = findViewById(R.id.tvCurrentText);
        tvVoltageText = findViewById(R.id.tvVoltageText);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvCurrentText.setVisibility(View.INVISIBLE);
        tvVoltageText.setVisibility(View.INVISIBLE);
        tvCurrent.setVisibility(View.INVISIBLE);
        tvVoltage.setVisibility(View.INVISIBLE);

        batteryBroadcast = new BatteryBroadcast();
        registerReceiver(batteryBroadcast, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            bluetoothBroadcast = new BluetoothBroadcast();
            IntentFilter ifbtb = new IntentFilter();
            ifbtb.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            ifbtb.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            ifbtb.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            ifbtb.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            ifbtb.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
            ifbtb.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
            registerReceiver(bluetoothBroadcast, ifbtb);
            ivBluetooth.setBackgroundResource(isWifiConnected ? R.mipmap.ic_bluetooth_connected : mBluetoothAdapter.isEnabled() ? R.mipmap.ic_alert_bluetooth : R.mipmap.ic_none);
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            ivGPS.setBackgroundResource(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? R.mipmap.ic_gps : R.mipmap.ic_none);
            gpsStatusBroadcast = new GpsStatusBroadcast();
            registerReceiver(gpsStatusBroadcast, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        rssiBroadcast = new RssiBroadcast();
        IntentFilter ifrssi = new IntentFilter();
        ifrssi.addAction(WifiManager.RSSI_CHANGED_ACTION);
        ifrssi.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(rssiBroadcast, ifrssi);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        networkBroadcast = new NetworkBroadcast();
        if (telephonyManager.getSimOperatorName() != null) {
            telephonyManager.listen(networkBroadcast, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } else {
            ivNetwork.setBackgroundResource(R.mipmap.ic_none);
        }
    }

    public void setProgressVisibility(boolean visibility) {
        findViewById(R.id.pb_processing).setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
    }

    public void setBackButtonVisibility(boolean visibility) {
        findViewById(R.id.btn_back).setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    public void setTitle(@StringRes int title, @StringRes int subtitle) {
        if (title != 0) {
            ((TextView) findViewById(R.id.tv_title)).setText(title);
            ((TextView) findViewById(R.id.tv_title_shape)).setText(title);
        }
        if (subtitle != 0) {
            findViewById(R.id.tv_subtitle).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_subtitle)).setText(subtitle);
            findViewById(R.id.tv_subtitle_shape).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_subtitle_shape)).setText(subtitle);
        }
    }

    public void setTitle(@StringRes int title) {
        if (title != 0) {
            ((TextView) findViewById(R.id.tv_title)).setText(title);
            ((TextView) findViewById(R.id.tv_title_shape)).setText(title);
        }
        findViewById(R.id.tv_subtitle).setVisibility(View.GONE);
        findViewById(R.id.tv_subtitle_shape).setVisibility(View.GONE);
    }

    public void setTitle(String title) {
        ((TextView) findViewById(R.id.tv_title)).setText(title);
        ((TextView) findViewById(R.id.tv_title_shape)).setText(title);
        findViewById(R.id.tv_subtitle).setVisibility(View.GONE);
        findViewById(R.id.tv_subtitle_shape).setVisibility(View.GONE);
    }

    @Override
    public void setContentView(int layoutResID) {
        LayoutInflater.from(this).inflate(layoutResID, parentLinearLayout, true);
    }

    @Override
    public void setContentView(View view) {
        parentLinearLayout.addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        parentLinearLayout.addView(view, params);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void initFontScale() {
        LocalSettingBean settingBean = BaseApplication.readSettings();
        Configuration configuration = getResources().getConfiguration();
        final float[] scale = {1f, 1.15f, 1.3f, 1.45f};
        if (settingBean.getFontScale() > 0 && settingBean.getFontScale() < scale.length)
            configuration.fontScale = scale[settingBean.getFontScale()];
        else
            configuration.fontScale = scale[0];
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryBroadcast);
        unregisterReceiver(bluetoothBroadcast);
        if (gpsStatusBroadcast != null)
            unregisterReceiver(gpsStatusBroadcast);
        unregisterReceiver(rssiBroadcast);
        if (telephonyManager.getSimOperatorName() != null)
            telephonyManager.listen(networkBroadcast, PhoneStateListener.LISTEN_NONE);
    }

    public void setVoltage(float vol) {
        Message msg = Message.obtain();
        msg.what = CHANGE_VOLTAGE;
        msg.obj = String.format(Locale.getDefault(), "%.2fV", vol);
        changeStatus.sendMessage(msg);
    }

    public void setCurrent(float current) {
        Message msg = Message.obtain();
        msg.what = CHANGE_CURRENT;
        if (current >= 1000)
            msg.obj = String.format(Locale.getDefault(), "%.2fmA", current / 1000);
        else
            msg.obj = String.format(Locale.getDefault(), "%.2fμA", current);
        changeStatus.sendMessage(msg);
    }

    /**
     * 监听电池状态变化广播
     */
    private class BatteryBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int current = bundle.getInt(BatteryManager.EXTRA_LEVEL);
            int total = bundle.getInt(BatteryManager.EXTRA_SCALE);
            int battryValue = current * 100 / total;

            Message msg = Message.obtain();
            msg.what = CHANGE_BATTERY;
            msg.arg1 = battryValue;
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            msg.arg2 = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) ? 1 : 0;
            changeStatus.sendMessage(msg);
        }
    }

    /**
     * 监听蓝牙状态变化广播
     */
    private class BluetoothBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action != null) {
                Message msg = Message.obtain();
                msg.what = CHANGE_BLUETOOTH;
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        msg.arg1 = R.mipmap.ic_bluetooth_connected;
                        isWifiConnected = true;
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        msg.arg1 = R.mipmap.ic_alert_bluetooth;
                        isWifiConnected = false;
                        break;
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                        switch (blueState) {
                            case BluetoothAdapter.STATE_OFF:
                                msg.arg1 = R.mipmap.ic_none;
                                isWifiConnected = false;
                                break;
                            case BluetoothAdapter.STATE_ON:
                                msg.arg1 = R.mipmap.ic_alert_bluetooth;
                                break;
                            default:
                                break;
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
                                    msg.arg1 = R.mipmap.ic_bluetooth_connected;
                                }
                            }
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                        break;
                }
                changeStatus.sendMessage(msg);
            }
        }
    }

    /**
     * 监听GPS 状态变化广播
     */
    private class GpsStatusBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null)
                if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    Message msg = Message.obtain();
                    msg.what = CHANGE_GPS;
                    msg.arg1 = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? R.mipmap.ic_gps : R.mipmap.ic_none;
                    changeStatus.sendMessage(msg);
                }
        }
    }

    private class RssiBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // Wifi的连接速度及信号强度：
                int strength;
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
                Message msg = Message.obtain();
                msg.what = CHANGE_WIFI;
                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    WifiInfo info = wifiManager.getConnectionInfo();
                    String ssid = info.getSSID();
                    if (ssid.startsWith("\"") && ssid.endsWith("\""))
                        ssid = ssid.substring(1, ssid.length() - 1);

                    if (ssid.length() > 0 && !"0x".equals(ssid) && !"<unknown ssid>".equals(ssid)) {
                        int[] wifiSignal = {R.mipmap.ic_wifi_1, R.mipmap.ic_wifi_2, R.mipmap.ic_wifi_3, R.mipmap.ic_wifi_4};

                        strength = WifiManager.calculateSignalLevel(info.getRssi(), wifiSignal.length);
                    /*
                    int level = info.getRssi();
                    //根据获得的信号强度发送信息
                    if (level <= 0 && level >= -50) strength = 3;
                    else if (level < -50 && level >= -70) strength = 2;
                    else if (level < -70 && level >= -90) strength = 1;
                        //else if (level < -80 && level >= -100)
                    else strength = 0;
                    */
                        msg.arg1 = wifiSignal[strength];
                        // 链接速度
                        //int speed = info.getLinkSpeed();
                        // 链接速度单位
                        //String units = WifiInfo.LINK_SPEED_UNITS;
                        // Wifi源名称
                        //String ssid = info.getSSID();
                    }
                } else {
                    msg.arg1 = R.mipmap.ic_none;
                }
                changeStatus.sendMessage(msg);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }


        }
    }

    private class NetworkBroadcast extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                int level;
                int dbm = 0;
                int[] networkSignal = {R.mipmap.ic_none, R.mipmap.ic_signal_1, R.mipmap.ic_signal_2, R.mipmap.ic_signal_3, R.mipmap.ic_signal_4, R.mipmap.ic_signal_5};

                if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                    //4G网络 最佳范围   >-90dBm 越大越好
                    String signalInfo = signalStrength.toString();
                    String[] params = signalInfo.split(" ");
                    dbm = Integer.parseInt(params[9]);
                } else if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_HSDPA ||
                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_HSPA ||
                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_HSUPA ||
                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS) {
                    //3G网络最佳范围  >-90dBm  越大越好  ps:中国移动3G获取不到  返回的无效dbm值是正数（85dbm）
                    //在这个范围的已经确定是3G，但不同运营商的3G有不同的获取方法，故在此需做判断 判断运营商与网络类型的工具类在最下方
                    String yys = telephonyManager.getSimOperatorName();//获取当前运营商

                    if (yys.equals("中国联通")) dbm = signalStrength.getCdmaDbm();
                    else if (yys.equals("中国电信")) dbm = signalStrength.getEvdoDbm();
                    //else if (yys.equals("中国移动")) //中国移动3G不可获取，故在此返回0
                } else {
                    //2G网络最佳范围>-90dBm 越大越好
                    dbm = -113 + 2 * signalStrength.getGsmSignalStrength();
                }
                if (dbm > -75) level = 5;
                else if (dbm > -85) level = 4;
                else if (dbm > -90) level = 3;
                else if (dbm > -95) level = 2;
                else level = 1;
                Message msg = Message.obtain();
                msg.what = CHANGE_NETWORK;
                msg.arg1 = networkSignal[level];
                changeStatus.sendMessage(msg);
            }
        }
    }

}

