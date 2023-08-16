package com.leon.detonator.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeCheckResultBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.OfflineDetonatorBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bean.ZbqyBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.EnterpriseDialog;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.ErrorCode;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

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
    private List<DetonatorBean> list;
    private LocationClient locationClient;
    private OfflineDetonatorBean offlineBean;
    private BaiSeBlasterBean baiSeBlasterBean;
    private BaiSeInfoBean baiSeInfoBean;
    private EnterpriseBean enterpriseBean;
    private TextView tvCoordinate;
    private MyButton btnOnline;
    private MyButton btnOffline;
    private LatLng lastLatLng;
    private BaiduMap baiduMap;
    private MapView mapView;
    private String token;
    private boolean firstLocate;
    private int requestCode;
    private int schemeSize;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode())
            switch (requestCode) {
                case 0:
                    enterpriseBean = DbUtil.getCurrentEnterprise();
                    break;
                case 1:
                    baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                case 2:
                    baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                    break;
                case 3:
                case 4:
                    List<SchemeBean> schemeBeanList = DbUtil.getCurrentSchemeList();
                    for (SchemeBean bean : schemeBeanList)
                        if (bean.getAmount() == 0) {
                            myApp.myToast(DetonateStep1Activity.this, String.format(getString(R.string.message_scheme_empty_list), schemeBeanList.get(0).getName()));
                            return;
                        }
                    if (requestCode == 5)
                        enterDetect();
                    else if (0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
                        list = DbUtil.getCurrentDetonatorList();
                        if (requestCode == 3)
                            checkRegister();
                        else
                            checkList(offlineBean.getResult().getLgs().getLg(), false);
                    } else
                        enterDetect();
                    break;
            }
    });

    private final Handler myHandler = new Handler(message -> {
        switch (message.what) {
            case BaseApplication.HANDLER_REGISTER_ERROR:
                enabledButton(true);
                break;
            case BaseApplication.HANDLER_REGISTER_SUCCESS:
                if (0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
                    if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_enterprise);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                        requestCode = 0;
                        launcher.launch(intent);
                    } else
                        showDialog(false);
                } else {
                    if (baiSeInfoBean == null) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_enterprise);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_PROJECT);
                        requestCode = 1;
                        launcher.launch(intent);
                    } else if (baiSeBlasterBean == null || baiSeBlasterBean.getData() == null || baiSeBlasterBean.getData().getUserIdCard().isEmpty()) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_detector);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_BLASTER);
                        requestCode = 2;
                        launcher.launch(intent);
                    } else
                        showDialog(true);
                }
                break;
            case 1:
                String coordinate;
                tvCoordinate.setShadowLayer(1, 1, 1, Color.BLACK);
                if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0) {
                    coordinate = String.format(Locale.getDefault(), getString(R.string.map_position) + getString(R.string.map_locate_success), lastLatLng.longitude, lastLatLng.latitude);
                    tvCoordinate.setTextColor(getColor(R.color.colorCoordinateText));
                    tvCoordinate.setShadowLayer(1, 1, 1, Color.BLUE);
                } else if ((int) BaseApplication.settings.getLatitude() != 0 && (int) BaseApplication.settings.getLongitude() != 0) {
                    tvCoordinate.setTextColor(Color.RED);
                    coordinate = String.format(Locale.getDefault(), getString(R.string.map_position) + getString(R.string.map_last_position), BaseApplication.settings.getLongitude(), BaseApplication.settings.getLatitude());
                    myApp.myToast(DetonateStep1Activity.this, R.string.message_use_last_position);
                    message.getTarget().sendEmptyMessageDelayed(1, 3000);
                } else {
                    tvCoordinate.setTextColor(Color.RED);
                    coordinate = getString(R.string.map_position_init);
                }
                tvCoordinate.setText(coordinate);
                break;
        }
        return false;
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
        enterpriseBean = DbUtil.getCurrentEnterprise();
        offlineBean = DbUtil.getOfflineDownloadDetonator();
        btnOnline = findViewById(R.id.btn_online_auth);
        btnOffline = findViewById(R.id.btn_offline_auth);
        tvCoordinate = findViewById(R.id.tv_coordinate);
        List<SchemeBean> schemeBeanList = DbUtil.getSchemeList();
        schemeSize = schemeBeanList.size();
        if (schemeSize == 1) {
            if (!schemeBeanList.get(0).isSelected()) {
                schemeBeanList.get(0).setSelected(true);
                DbUtil.updateScheme(schemeBeanList.get(0));
            }
            list = DbUtil.getCurrentDetonatorList();
            if (list.size() == 0)
                myApp.myToast(DetonateStep1Activity.this, String.format(getString(R.string.message_scheme_empty_list), schemeBeanList.get(0).getName()));
        } else if (schemeSize == 0)
            myApp.myToast(DetonateStep1Activity.this, R.string.message_list_not_found);
        if (0 == BaseApplication.settings.getServerHost() || 2 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
            if (2 == BaseApplication.settings.getServerHost()) {
                baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                btnOnline.setTextId(schemeSize == 1 ? R.string.button_online_detect : R.string.button_select_scheme);
                btnOffline.setTextId(R.string.button_auth);
            } else
                btnOffline.setEnabled(offlineBean != null);
        } else {
            btnOnline.setTextId(schemeSize == 1 ? R.string.button_online_detect : R.string.button_select_scheme);
            btnOffline.setVisibility(View.GONE);
        }
        btnOnline.setOnClickListener(view -> launchWhich(KeyEvent.KEYCODE_1));
        btnOffline.setOnClickListener(view -> launchWhich(KeyEvent.KEYCODE_2));
        firstLocate = true;
        enabledButton(false);
        mapView = findViewById(R.id.map_view);
        mapView.getChildAt(2).setPadding(0, 0, 10, 60);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        MapStatus mMapStatus = new MapStatus.Builder().target(new LatLng(BaseApplication.settings.getLatitude() != 0 ? BaseApplication.settings.getLatitude() : 22.551083,
                BaseApplication.settings.getLongitude() != 0 ? BaseApplication.settings.getLongitude() : 110.950548)).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
        baiduMap.setCompassEnable(true);
        baiduMap.setCompassPosition(new Point(10, 10));
        //定位初始化
        try {
            locationClient = new LocationClient(this);
            //通过LocationClientOption设置LocationClient相关参数
            LocationClientOption option = new LocationClientOption();
            option.setCoorType("bd09ll"); // 设置坐标类型
            option.setScanSpan(1000);
            //设置locationClientOption
            locationClient.setLocOption(option);
            //注册LocationListener监听器
            MyLocationListener myLocationListener = new MyLocationListener();
            locationClient.registerLocationListener(myLocationListener);
            //开启地图定位图层
            locationClient.start();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        btnOnline.requestFocus();
        myHandler.sendEmptyMessageDelayed(1, 2000);
    }

    private void launchWhich(int which) {
        if (KeyEvent.KEYCODE_1 == which && btnOnline.isEnabled()) {
            if (schemeSize > 1)
                selectScheme(3);
            else if (0 != BaseApplication.settings.getServerHost() && 3 != BaseApplication.settings.getServerHost())
                enterDetect();
            else
                checkRegister();
        } else if (KeyEvent.KEYCODE_2 == which && btnOffline.isEnabled()) {
            if (0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
                if (!checkLocation())
                    myApp.myToast(DetonateStep1Activity.this, R.string.message_not_allow_area);
                else if (schemeSize > 1)
                    selectScheme(4);
                else
                    checkList(offlineBean.getResult().getLgs().getLg(), false);
            } else if (2 == BaseApplication.settings.getServerHost())
                checkRegister();
        } else if (KeyEvent.KEYCODE_POUND == which)
            if (schemeSize > 1)
                selectScheme(5);
            else
                enterDetect();
    }

    private void selectScheme(int code) {
        Intent intent = new Intent(DetonateStep1Activity.this, SchemeActivity.class);
        intent.putExtra(KeyUtils.KEY_SELECT_SCHEME, true);
        requestCode = code;
        launcher.launch(intent);
    }

    private void enterDetect() {
        Intent intent = new Intent().setClass(DetonateStep1Activity.this, DetonateStep2Activity.class);
        intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, false);
        intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
        intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, validLocation().latitude);
        intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, validLocation().longitude);
        BaseApplication.writeFile("Locate:" + validLocation().latitude + ", " + validLocation().longitude);
        startActivity(intent);
        finish();
    }

    private void checkRegister() {
        if (!BaseApplication.settings.isRegistered()) {
            enabledButton(false);
            myApp.registerExploder(myHandler);
        } else
            myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_SUCCESS);
    }

    private LatLng validLocation() {
        if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0)
            return lastLatLng;
        return new LatLng(BaseApplication.settings.getLatitude(), BaseApplication.settings.getLongitude());
    }

    private boolean checkLocation() {
        if (null != offlineBean) {
            List<ZbqyBean> list = offlineBean.getResult().getZbqys().getZbqy();
            for (ZbqyBean bean : list)
                try {
                    if (BaseApplication.distance(Double.parseDouble(bean.getZbqywd()), Double.parseDouble(bean.getZbqyjd()), validLocation().latitude, validLocation().longitude)
                            < Double.parseDouble(bean.getZbqybj()))
                        return true;
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
        }
        return false;
    }

    private void showDialog(boolean baiSe) {
        EnterpriseDialog enterpriseDialog;
        enterpriseDialog = new EnterpriseDialog(DetonateStep1Activity.this, baiSe);
        enterpriseDialog.show();
        enterpriseDialog.findViewById(R.id.btn_dialog_confirm).setOnClickListener(view -> {
            enterpriseDialog.dismiss();
            if (BaseApplication.isNetSystemUsable(DetonateStep1Activity.this)) {
                enabledButton(false);
                if (baiSe) {
                    try {
                        baiSeBlasterBean.getData().setAppVersion(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                        baiSeBlasterBean.getData().setLngLat(String.format(Locale.getDefault(), "%f,%f", validLocation().longitude, validLocation().latitude));
                        baiSeBlasterBean.getData().setGpsCoordinateSystems(ConstantUtils.GPS_SYSTEM);
                        baiSeBlasterBean.getData().setDeviceNO(BaseApplication.settings.getExploderID());
                        BaseApplication.writeFile(new Gson().toJson(baiSeBlasterBean));
                        OkHttpUtils.postString().addHeader("access-token", ConstantUtils.ACCESS_TOKEN)
                                .url(ConstantUtils.BAI_SE_CHECK_URL)
                                .mediaType(MediaType.parse("application/json; charset=utf-8"))
                                .content(new Gson().toJson(baiSeBlasterBean.getData()))
                                .build().execute(new Callback<BaiSeCheckResultBean>() {

                                    @Override
                                    public BaiSeCheckResultBean parseNetworkResponse(Response response, int i) throws Exception {
                                        ResponseBody body = response.body();
                                        if (body != null) {
                                            String string = body.string();
                                            return new Gson().fromJson(string, BaiSeCheckResultBean.class);
                                        }
                                        return null;
                                    }

                                    @Override
                                    public void onError(Call call, Exception e, int i) {
                                        myApp.myToast(DetonateStep1Activity.this, R.string.message_network_timeout);
                                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                    }

                                    @Override
                                    public void onResponse(BaiSeCheckResultBean baiSeCheckResultBean, int i) {
                                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                        if (baiSeCheckResultBean != null) {
                                            if (baiSeCheckResultBean.isSuccess() && baiSeCheckResultBean.getData().isIsPass()) {
                                                myApp.myToast(DetonateStep1Activity.this, R.string.message_bai_se_check_success);
                                                baiSeBlasterBean.setChecked(true);
                                                DbUtil.updateBaiSeBlaster(baiSeBlasterBean);
                                            } else if (baiSeCheckResultBean.getData() != null) {
                                                if (baiSeCheckResultBean.getData().getMsg() != null)
                                                    myApp.myToast(DetonateStep1Activity.this, baiSeCheckResultBean.getData().getMsg());
                                                else
                                                    myApp.myToast(DetonateStep1Activity.this, R.string.message_bai_se_check_fail);
                                            }
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    StringBuilder str = new StringBuilder();
                    for (DetonatorBean bean : list)
                        str.append(bean.getAddress()).append(",");
                    if (str.length() > 0)
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
                                .build().execute(new Callback<OfflineDetonatorBean>() {
                                    @Override
                                    public OfflineDetonatorBean parseNetworkResponse(Response response, int i) throws Exception {
                                        if (response.body() != null) {
                                            String string = Objects.requireNonNull(response.body()).string();
                                            return BaseApplication.jsonFromString(string, OfflineDetonatorBean.class);
                                        }
                                        return null;
                                    }

                                    @Override
                                    public void onError(Call call, Exception e, int i) {
                                        myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                    }

                                    @Override
                                    public void onResponse(OfflineDetonatorBean onlineBean, int i) {
                                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                        if (null != onlineBean) {
                                            if (onlineBean.getToken().equals(token)) {
                                                if (onlineBean.isStatus()) {
                                                    if (null != onlineBean.getResult()) {
                                                        if (onlineBean.getResult().getCwxx().equals("0")) {
                                                            List<LgBean> detonators = onlineBean.getResult().getLgs().getLg();
                                                            if (null != detonators) {
                                                                checkList(detonators, true);
                                                                DbUtil.updateDownloadDetonator(false, onlineBean);
                                                            }
                                                        } else {
                                                            String error = ErrorCode.downloadErrorCode.get(onlineBean.getResult().getCwxx());
                                                            if (null == error) {
                                                                error = getString(R.string.message_download_unknown_error) + onlineBean.getResult().getCwxx();
                                                            }
                                                            myApp.myToast(DetonateStep1Activity.this, error);
                                                        }
                                                    }
                                                } else {
                                                    myApp.myToast(DetonateStep1Activity.this, onlineBean.getDescription());
                                                }
                                            } else {
                                                myApp.myToast(DetonateStep1Activity.this, R.string.message_token_error);
                                            }
                                        } else {
                                            myApp.myToast(DetonateStep1Activity.this, R.string.message_return_data_error);
                                        }
                                    }
                                });
                    }
                }
            } else
                myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
        });
        enterpriseDialog.findViewById(R.id.btn_dialog_modify).setOnClickListener(view -> {
            enterpriseDialog.dismiss();
            if (baiSe) {
                requestCode = 2;
                if (baiSeInfoBean == null || baiSeBlasterBean == null) {
                    if (baiSeInfoBean == null) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_enterprise);
                        requestCode = 1;
                    }
                    Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                    intent.putExtra(KeyUtils.KEY_INFO_TYPE, baiSeInfoBean == null ? ConstantUtils.INFO_PROJECT : ConstantUtils.INFO_BLASTER);
                    launcher.launch(intent);
                } else
                    launcher.launch(new Intent(DetonateStep1Activity.this, BaiSeDetectorActivity.class));
            } else {
                requestCode = 0;
                if (enterpriseBean == null) {
                    Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                    intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                    launcher.launch(intent);
                } else
                    launcher.launch(new Intent(DetonateStep1Activity.this, EnterpriseActivity.class));
            }
        });
    }

    private void checkList(List<LgBean> detonators, boolean online) {
        boolean correct = true;
        List<ArrayList<DetonatorBean>> lists = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            lists.add(new ArrayList<>());
        for (DetonatorBean bean : list) {
            boolean found = false;
            for (LgBean bean1 : detonators)
                if (bean1.getFbh().equals(bean.getAddress())) {
                    found = true;
                    try {
                        if (Integer.parseInt(bean1.getGzmcwxx()) > 0 && Integer.parseInt(bean1.getGzmcwxx()) <= 3) {
                            correct = false;
                            lists.get(Integer.parseInt(bean1.getGzmcwxx()) - 1).add(bean);
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                    break;
                }
            if (!found) {
                lists.get(2).add(bean);
                correct = false;
            }
        }
        if (correct) {
            myApp.myToast(DetonateStep1Activity.this, online ? R.string.message_online_check_finished : R.string.message_offline_check_finished);
            Intent intent = new Intent().setClass(DetonateStep1Activity.this, DetonateStep2Activity.class);
            intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, online);
            intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
            intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, validLocation().latitude);
            intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, validLocation().longitude);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent().setClass(DetonateStep1Activity.this, ErrorDetonatorListActivity.class);
            intent.putParcelableArrayListExtra(KeyUtils.KEY_ERROR_BLACK_LIST, lists.get(0));
            intent.putParcelableArrayListExtra(KeyUtils.KEY_ERROR_USED_LIST, lists.get(1));
            intent.putParcelableArrayListExtra(KeyUtils.KEY_ERROR_NOT_FOUND_LIST, lists.get(2));
            startActivity(intent);
        }
    }

    private void enabledButton(boolean b) {
        setProgressVisibility(!b);
        btnOnline.setEnabled(b && schemeSize > 0 && (2 != BaseApplication.settings.getServerHost() || (baiSeBlasterBean != null && baiSeBlasterBean.isChecked())));
        btnOffline.setEnabled(b && (2 == BaseApplication.settings.getServerHost()
                || (schemeSize > 0 && (offlineBean != null && offlineBean.getResult() != null && offlineBean.getResult().getLgs() != null
                && offlineBean.getResult().getLgs().getLg() != null
                && offlineBean.getResult().getLgs().getLg().size() > 0))));
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
        if (locationClient != null)
            locationClient.stop();
        myHandler.removeCallbacksAndMessages(null);
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
        super.onDestroy();
    }

    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null)
                return;
            try {
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                baiduMap.setMyLocationData(locData);
                if (0 != (int) location.getLatitude() && 0 != (int) location.getLongitude()) {
                    lastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myHandler.removeMessages(1);
                    myHandler.sendEmptyMessage(1);
                    if (firstLocate) {
                        MapStatus mMapStatus = new MapStatus.Builder().target(lastLatLng).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
                        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
                        firstLocate = false;
                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                    }
                    BaseApplication.settings.setLatitude(location.getLatitude());
                    BaseApplication.settings.setLongitude(location.getLongitude());
                    myApp.saveSettings();
                }
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }

        @Override
        public void onLocDiagnosticMessage(int locType, int diagnosticType, String s) {
            BaseApplication.writeFile("onLocDiagnosticMessage:" + diagnosticType + ", " + s);
            super.onLocDiagnosticMessage(locType, diagnosticType, s);
        }
    }
}
