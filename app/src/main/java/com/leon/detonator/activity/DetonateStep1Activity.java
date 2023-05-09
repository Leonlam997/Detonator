package com.leon.detonator.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.google.gson.Gson;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.CheckRegister;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.BaiSeCheck;
import com.leon.detonator.bean.BaiSeCheckResult;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.ZbqyBean;
import com.leon.detonator.dialog.EnterpriseDialog;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.ErrorCode;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DetonateStep1Activity extends BaseActivity {
    private MapView mapView = null;
    private BaiduMap baiduMap;
    private LocationClient locationClient;
    private boolean firstLocate;
    private EnterpriseDialog enterpriseDialog;
    private EnterpriseBean enterpriseBean;
    private String token;
    private List<DetonatorInfoBean> list = new ArrayList<>();
    private MyButton btnOnline, btnOffline;
    private LatLng lastLatLng;
    private Location lastKnownLocation;
    private DownloadDetonatorBean offlineBean;
    private LocalSettingBean settingBean;
    private BaiSeCheck baiSeCheck;
    private BaseApplication myApp;
    private final Handler checkExploderHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case 1:
                    if ((0 != settingBean.getServerHost() && 2 != settingBean.getServerHost()) ||
                            (settingBean.isRegistered() && validLocation().latitude != 0 && validLocation().longitude != 0))
                        enabledButton(true);
                    break;
                case 2:
                    if (0 == settingBean.getServerHost()) {
                        if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
                            showMessage(R.string.message_fill_enterprise);
                            startActivity(new Intent(DetonateStep1Activity.this, EnterpriseActivity.class));
                        } else {
                            BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep1Activity.this, R.style.AlertDialog)
                                    .setTitle(R.string.dialog_title_download)
                                    .setMessage(R.string.dialog_confirm_online_download)
                                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> onlineDownload())
                                    .setNegativeButton(R.string.btn_cancel, null)
                                    .show(), true);
                        }
                    } else {
                        if (baiSeCheck == null || baiSeCheck.getData().getUserIdCard().isEmpty()) {
                            showMessage(R.string.message_fill_enterprise);
                            startActivity(new Intent(DetonateStep1Activity.this, BaiSeDataActivity.class));
                        } else {
                            launchWhich(KeyEvent.KEYCODE_POUND);
                        }
                    }
                    break;
                case 3:
                    String coordinate;
                    if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0) {
                        coordinate = String.format(Locale.getDefault(), getString(R.string.map_position), lastLatLng.longitude, lastLatLng.latitude);
                        ((TextView) findViewById(R.id.tv_coordinate)).setTextColor(getColor(R.color.colorCoordinateText));
                        ((TextView) findViewById(R.id.tv_coordinate1)).setTextColor(Color.BLUE);
                    } else if ((int) settingBean.getLatitude() != 0 && (int) settingBean.getLongitude() != 0) {
                        ((TextView) findViewById(R.id.tv_coordinate)).setTextColor(Color.RED);
                        ((TextView) findViewById(R.id.tv_coordinate1)).setTextColor(Color.BLACK);
                        coordinate = String.format(Locale.getDefault(), getString(R.string.map_position), settingBean.getLongitude(), settingBean.getLatitude());
                    } else
                        coordinate = getString(R.string.map_position_init);
                    ((TextView) findViewById(R.id.tv_coordinate)).setText(coordinate);
                    ((TextView) findViewById(R.id.tv_coordinate1)).setText(coordinate);
                    break;
                case 4:
                    enterpriseDialog = new EnterpriseDialog(DetonateStep1Activity.this);
                    enterpriseDialog.setClickConfirm(view -> {
                        if (BaseApplication.isNetSystemUsable(DetonateStep1Activity.this)) {
                            try {
                                enabledButton(false);
                                baiSeCheck.getData().setAppVersion(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                                baiSeCheck.getData().setLngLat(String.format(Locale.getDefault(), "%f,%f", validLocation().longitude, validLocation().latitude));
                                baiSeCheck.getData().setGpsCoordinateSystems(ConstantUtils.GPS_SYSTEM);
                                baiSeCheck.getData().setDeviceNO(settingBean.getExploderID());
                                BaseApplication.writeFile(new Gson().toJson(baiSeCheck));
                                OkHttpUtils.postString().addHeader("access-token", ConstantUtils.ACCESS_TOKEN)
                                        .url(ConstantUtils.BAI_SE_CHECK_URL)
                                        .mediaType(MediaType.parse("application/json; charset=utf-8"))
                                        .content(new Gson().toJson(baiSeCheck.getData()))
                                        .build().execute(new Callback<BaiSeCheckResult>() {

                                            @Override
                                            public BaiSeCheckResult parseNetworkResponse(Response response, int i) throws Exception {
                                                ResponseBody body = response.body();
                                                if (body != null) {
                                                    String string = body.string();
                                                    return new Gson().fromJson(string, BaiSeCheckResult.class);
                                                }
                                                return null;
                                            }

                                            @Override
                                            public void onError(Call call, Exception e, int i) {
                                                myApp.myToast(DetonateStep1Activity.this, R.string.message_network_timeout);
                                            }

                                            @Override
                                            public void onResponse(BaiSeCheckResult baiSeCheckResult, int i) {
                                                if (baiSeCheckResult != null) {
                                                    if (baiSeCheckResult.isSuccess() && baiSeCheckResult.getData().isIsPass()) {
                                                        myApp.myToast(DetonateStep1Activity.this, R.string.message_bai_se_check_success);
                                                        baiSeCheck.setChecked(true);
                                                        myApp.saveBean(baiSeCheck);
                                                        checkExploderHandler.sendEmptyMessage(1);
                                                    } else if (baiSeCheckResult.getData() != null) {
                                                        if (baiSeCheckResult.getData().getMsg() != null)
                                                            myApp.myToast(DetonateStep1Activity.this, baiSeCheckResult.getData().getMsg());
                                                        else
                                                            myApp.myToast(DetonateStep1Activity.this, R.string.message_bai_se_check_fail);
                                                        enabledButton(true);
                                                    }
                                                }
                                            }
                                        });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                        }
                        enterpriseDialog.dismiss();
                    });
                    enterpriseDialog.setClickModify(view -> {
                        enterpriseDialog.dismiss();
                        startActivity(new Intent(DetonateStep1Activity.this, BaiSeDetectorActivity.class));
                    });
                    enterpriseDialog.show();
                    break;
                default:
                    myApp.myToast(DetonateStep1Activity.this, (String) message.obj);
                    break;
            }
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            SDKInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        setContentView(R.layout.activity_detonate_step1);

        if (getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false))
            setTitle(R.string.check_locate, R.string.subtitle_unite);
        else
            setTitle(R.string.check_locate);

        myApp = (BaseApplication) getApplication();
        list = new ArrayList<>();
        try {
            myApp.readFromFile(myApp.getListFile(), list, DetonatorInfoBean.class);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        settingBean = BaseApplication.readSettings();
        enterpriseBean = myApp.readEnterprise();
        offlineBean = myApp.readDownloadList(false);
        btnOffline = findViewById(R.id.btn_offline_auth);
        if (0 == settingBean.getServerHost() || 2 == settingBean.getServerHost()) {
            findViewById(R.id.gl_btn2).setVisibility(View.GONE);
            findViewById(R.id.gl_btn).setVisibility(View.VISIBLE);
            btnOnline = findViewById(R.id.btn_online_auth);
            if (2 == settingBean.getServerHost()) {
                baiSeCheck = myApp.readBaiSeCheck();
                btnOnline.setTextId(R.string.button_online_detect);
                btnOffline.setTextId(R.string.button_auth);
            }
        } else {
            findViewById(R.id.gl_btn).setVisibility(View.GONE);
            findViewById(R.id.gl_btn2).setVisibility(View.VISIBLE);
            btnOnline = findViewById(R.id.btn_detect);
        }
        btnOnline.setOnClickListener(view -> launchWhich(KeyEvent.KEYCODE_1));
        btnOffline.setOnClickListener(view -> launchWhich(KeyEvent.KEYCODE_2));
        firstLocate = true;
        initLocation();
//        btnOnline.setEnabled((2 == settingBean.getServerHost() && baiSeCheck.isChecked()) || (2 != settingBean.getServerHost() && (list.size() > 0
//                && ((lastKnownLocation != null && (int) lastKnownLocation.getLatitude() != 0 && (int) lastKnownLocation.getLongitude() != 0)
//                || ((int) settingBean.getLongitude() != 0 && (int) settingBean.getLatitude() != 0)))));
//        btnOffline.setEnabled(2 == settingBean.getServerHost() || ((offlineBean != null && offlineBean.getResult() != null && (offlineBean.getResult().getLgs() != null &&
//                offlineBean.getResult().getLgs().getLg() != null &&
//                offlineBean.getResult().getLgs().getLg().size() > 0)) && list.size() > 0
//                && ((lastKnownLocation != null && (int) lastKnownLocation.getLatitude() != 0 && (int) lastKnownLocation.getLongitude() != 0)
//                || ((int) settingBean.getLongitude() != 0 && (int) settingBean.getLatitude() != 0))));
        enabledButton(false);
        mapView = findViewById(R.id.map_view);
        mapView.getChildAt(2).setPadding(0, 0, 10, 100);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        MapStatus mMapStatus = new MapStatus.Builder().target(new LatLng(settingBean.getLatitude() != 0 ? settingBean.getLatitude() : 22.551083,
                settingBean.getLongitude() != 0 ? settingBean.getLongitude() : 110.950548)).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
        baiduMap.setCompassEnable(true);
        baiduMap.setCompassPosition(new Point(10, 10));
        //定位初始化
        locationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        locationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        locationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        locationClient.start();
        btnOnline.requestFocus();
    }

    private void initLocation() {
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(DetonateStep1Activity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(DetonateStep1Activity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List<String> providers = lm.getAllProviders();
            for (String provider : providers) {
                lastKnownLocation = lm.getLastKnownLocation(provider);
                if (null != lastKnownLocation && (int) lastKnownLocation.getLongitude() != 0 && (int) lastKnownLocation.getLatitude() != 0)
                    break;
            }
            checkExploderHandler.sendEmptyMessage(3);
//            myApp.myToast(DetonateStep1Activity.this, lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude());
        }
    }

    private void launchWhich(int which) {
        boolean enterDetect = false;
        if (KeyEvent.KEYCODE_1 == which && btnOnline.isEnabled()) {
            if (0 != settingBean.getServerHost() && 2 != settingBean.getServerHost()) {
                enterDetect = true;
            } else {
                checkRegister(2);
            }
        } else if (KeyEvent.KEYCODE_2 == which && btnOffline.isEnabled()) {
            if (0 == settingBean.getServerHost()) {
                if (!checkLocation()) {
                    showMessage(R.string.message_not_allow_area);
                } else {
                    checkList(offlineBean.getResult().getLgs().getLg(), false);
                }
            } else if (2 == settingBean.getServerHost()) {
                checkRegister(4);
            }
        } else if (KeyEvent.KEYCODE_POUND == which) {
            enterDetect = true;
        }
        if (enterDetect) {
            Intent intent = new Intent().setClass(DetonateStep1Activity.this, DetonateStep2Activity.class);
            intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, false);
            intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
            intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, validLocation().latitude);
            intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, validLocation().longitude);
            BaseApplication.writeFile("Locate:" + validLocation().latitude + ", " + validLocation().longitude);
            startActivity(intent);
            finish();
        }
    }

    private void checkRegister(int i) {
        settingBean = BaseApplication.readSettings();
        if (!settingBean.isRegistered()) {
            myApp.registerExploder();
            enabledButton(false);
            new CheckRegister() {
                @Override
                public void onError() {
                }

                @Override
                public void onSuccess() {
                    checkExploderHandler.sendEmptyMessage(i);
                }
            }.setActivity(this).start();
        } else {
            checkExploderHandler.sendEmptyMessage(i);
        }
    }

    private LatLng validLocation() {
        if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0)
            return lastLatLng;
        return new LatLng(settingBean.getLatitude(), settingBean.getLongitude());
    }

    private boolean checkLocation() {
        if (null != offlineBean) {
            List<ZbqyBean> list = offlineBean.getResult().getZbqys().getZbqy();
            for (ZbqyBean bean : list) {
                try {
                    if (BaseApplication.distance(Double.parseDouble(bean.getZbqywd()), Double.parseDouble(bean.getZbqyjd()), validLocation().latitude, validLocation().longitude)
                            < Double.parseDouble(bean.getZbqybj())) {
                        return true;
                    }
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
        }
        return false;
    }

    private void showMessage(String s) {
        Message m = checkExploderHandler.obtainMessage(5);
        m.obj = s;
        checkExploderHandler.sendMessage(m);
    }

    private void showMessage(@StringRes int s) {
        Message m = checkExploderHandler.obtainMessage(5);
        m.obj = getString(s);
        checkExploderHandler.sendMessage(m);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launchWhich(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        locationClient.stop();
        checkExploderHandler.removeCallbacksAndMessages(null);
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
        super.onDestroy();
    }

    private void onlineDownload() {
        enterpriseDialog = new EnterpriseDialog(DetonateStep1Activity.this);
        enterpriseDialog.setClickConfirm(view -> {
            enterpriseDialog.dismiss();
            if (BaseApplication.isNetSystemUsable(DetonateStep1Activity.this)) {
                enabledButton(false);
                StringBuilder str = new StringBuilder();
                for (DetonatorInfoBean bean : list) {
                    str.append(bean.getAddress()).append(",");
                }
                str.deleteCharAt(str.length() - 1);
                token = myApp.makeToken();
                Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_ONLINE_DOWNLOAD);
                if (null != params) {
                    params.put("dsc", str.toString());
                    params.put("dwdm", enterpriseBean.getCode());
                    params.put("jd", validLocation().longitude + "");
                    params.put("wd", validLocation().latitude + "");
                    if (enterpriseBean.isCommercial()) {
                        params.put("htid", enterpriseBean.getContract());
                        params.put("xmbh", enterpriseBean.getProject());
                    }
                    params.put("signature", myApp.signature(params));
                    OkHttpUtils.post()
                            .url(ConstantUtils.HOST_URL)
                            .params(params)
                            .build().execute(new Callback<DownloadDetonatorBean>() {
                                @Override
                                public DownloadDetonatorBean parseNetworkResponse(Response response, int i) throws Exception {
                                    if (response.body() != null) {
                                        String string = Objects.requireNonNull(response.body()).string();
                                        return BaseApplication.jsonFromString(string, DownloadDetonatorBean.class);
                                    }
                                    return null;
                                }

                                @Override
                                public void onError(Call call, Exception e, int i) {
                                    showMessage(R.string.message_check_network);
                                    enabledButton(true);
                                }

                                @Override
                                public void onResponse(DownloadDetonatorBean onlineBean, int i) {
                                    enabledButton(true);
                                    if (null != onlineBean) {
                                        if (onlineBean.getToken().equals(token)) {
                                            if (onlineBean.isStatus()) {
                                                if (null != onlineBean.getResult()) {
                                                    if (onlineBean.getResult().getCwxx().equals("0")) {
                                                        List<LgBean> detonators = onlineBean.getResult().getLgs().getLg();
                                                        if (null != detonators) {
                                                            myApp.saveDownloadList(onlineBean, true);
                                                            checkList(detonators, true);
                                                        }
                                                    } else {
                                                        String error = ErrorCode.downloadErrorCode.get(onlineBean.getResult().getCwxx());
                                                        if (null == error) {
                                                            error = getString(R.string.message_download_unknown_error) + onlineBean.getResult().getCwxx();
                                                        }
                                                        showMessage(error);
                                                    }
                                                }
                                            } else {
                                                showMessage(onlineBean.getDescription());
                                            }
                                        } else {
                                            showMessage(R.string.message_token_error);
                                        }
                                    } else {
                                        showMessage(R.string.message_return_data_error);
                                    }
                                }
                            });
                }
            } else
                myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
        });
        enterpriseDialog.setClickModify(view -> {
            enterpriseDialog.dismiss();
            startActivity(new Intent(DetonateStep1Activity.this, EnterpriseActivity.class));
        });
        enterpriseDialog.show();
    }

    private void checkList(List<LgBean> detonators, boolean online) {
        boolean correct = true;
        List<ArrayList<DetonatorInfoBean>> lists = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            lists.add(new ArrayList<>());
        for (DetonatorInfoBean bean : list) {
            boolean found = false;
            for (LgBean bean1 : detonators) {
                if (bean1.getFbh().equals(bean.getAddress())) {
                    found = true;
                    try {
                        if (!bean1.getGzmcwxx().equals("0") && Integer.parseInt(bean1.getGzmcwxx()) <= 3) {
                            correct = false;
                            lists.get(Integer.parseInt(bean1.getGzmcwxx()) - 1).add(bean);
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                    break;
                }
            }
            if (!found) {
                lists.get(2).add(bean);
                correct = false;
            }
        }

        Intent intent;
        if (correct) {
            showMessage(online ? R.string.message_online_check_finished : R.string.message_offline_check_finished);
            intent = new Intent().setClass(DetonateStep1Activity.this, DetonateStep2Activity.class);
            intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, online);
            intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
            intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, validLocation().latitude);
            intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, validLocation().longitude);
            startActivity(intent);
            finish();
        } else {
            intent = new Intent().setClass(DetonateStep1Activity.this, ErrorDetonatorListActivity.class);
            intent.putParcelableArrayListExtra(KeyUtils.KEY_ERROR_BLACK_LIST, lists.get(0));
            intent.putParcelableArrayListExtra(KeyUtils.KEY_ERROR_USED_LIST, lists.get(1));
            intent.putParcelableArrayListExtra(KeyUtils.KEY_ERROR_NOT_FOUND_LIST, lists.get(2));
            startActivity(intent);
        }
    }

    private void enabledButton(boolean b) {
        setProgressVisibility(!b);
        btnOnline.setEnabled(b && list.size() > 0 && (2 != settingBean.getServerHost() || (baiSeCheck != null && baiSeCheck.isChecked())));
        btnOffline.setEnabled(b && (2 == settingBean.getServerHost()
                || (list.size() > 0 && 2 != settingBean.getServerHost() && (offlineBean != null && offlineBean.getResult() != null && offlineBean.getResult().getLgs() != null
                && offlineBean.getResult().getLgs().getLg() != null
                && offlineBean.getResult().getLgs().getLg().size() > 0))));
    }

    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }
            try {
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                baiduMap.setMyLocationData(locData);
                if (0 != (int) location.getLatitude() && 0 != (int) location.getLongitude()) {
                    lastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    checkExploderHandler.sendEmptyMessage(3);
                    if (firstLocate) {
                        MapStatus mMapStatus = new MapStatus.Builder().target(lastLatLng).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
                        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
                        firstLocate = false;
                        checkExploderHandler.sendEmptyMessage(1);
                    }
                    settingBean.setLatitude(location.getLatitude());
                    settingBean.setLongitude(location.getLongitude());
                    myApp.saveBean(settingBean);
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {
            BaseApplication.writeFile("onConnectHotSpotMessage:" + s);
//            myApp.myToast(DetonateStep1Activity.this, s);
            super.onConnectHotSpotMessage(s, i);
        }

        @Override
        public void onLocDiagnosticMessage(int i, int i1, String s) {
            BaseApplication.writeFile("onLocDiagnosticMessage:" + s);
//            myApp.myToast(DetonateStep1Activity.this, s);
            super.onLocDiagnosticMessage(i, i1, s);
        }
    }
}
