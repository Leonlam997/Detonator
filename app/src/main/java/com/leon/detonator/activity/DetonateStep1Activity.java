package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

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
import com.leon.detonator.bean.DanLinDetonatorBean;
import com.leon.detonator.bean.DanLinOnlineDownloadRequestBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.JbqyBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bean.ZbqyBean;
import com.leon.detonator.component.MarqueeTextView;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.ErrorCode;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.TripleDESUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.util.ArrayList;
import java.util.HashMap;
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
    private EnterpriseBean enterpriseBean;
    private List<DetonatorBean> list;
    private MyButton btnOnline;
    private MyButton btnOffline;
    private TextView tvCoordinate;
    private LatLng lastLatLng;
    private DanLinDetonatorBean offlineBean;
    private BaiSeInfoBean baiSeInfoBean;
    private BaiSeBlasterBean baiSeBlasterBean;
    private String address;
    private boolean firstLocate;
    private int requestCode;
    private int schemeSize;
    private static final int REQUEST_CODE_GET_ENTERPRISE = 0;
    private static final int REQUEST_CODE_GET_BAI_SE_INFO = 1;
    private static final int REQUEST_CODE_GET_BAI_SE_BLASTER = 2;
    private static final int REQUEST_CODE_EDIT_ENTERPRISE = 3;
    private static final int REQUEST_CODE_SELECT_SCHEME = 4;
    private static final int REQUEST_CODE_SELECT_SCHEME_BYPASS = 5;
    private static final int REQUEST_CODE_EDIT_BAI_SE_DETECTOR = 6;
    private static final int HANDLER_LOCATION_FINISHED = 1;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode())
            switch (requestCode) {
                case REQUEST_CODE_GET_ENTERPRISE:
                case REQUEST_CODE_EDIT_ENTERPRISE:
                    enterpriseBean = DbUtil.getCurrentEnterprise();
                    break;
                case REQUEST_CODE_GET_BAI_SE_INFO:
                    baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                case REQUEST_CODE_GET_BAI_SE_BLASTER:
                case REQUEST_CODE_EDIT_BAI_SE_DETECTOR:
                    baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                    break;
                case REQUEST_CODE_SELECT_SCHEME:
                case REQUEST_CODE_SELECT_SCHEME_BYPASS:
                    List<SchemeBean> schemeBeanList = DbUtil.getCurrentSchemeList();
                    for (SchemeBean bean : schemeBeanList)
                        if (bean.getAmount() == 0) {
                            myApp.myToast(DetonateStep1Activity.this, String.format(getString(R.string.message_scheme_empty_list), schemeBeanList.get(0).getName()));
                            return;
                        }
                    if (requestCode == REQUEST_CODE_SELECT_SCHEME_BYPASS || !isDanLin())
                        enterDetect();
                    else {
                        list = DbUtil.getCurrentDetonatorList();
                        checkList(offlineBean.getResult().getLgs().getLg(), false);
                    }
                    break;
            }
    });
    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case BaseApplication.HANDLER_REGISTER_ERROR:
                enabledButton(true);
                break;
            case BaseApplication.HANDLER_REGISTER_SUCCESS:
                if (isDanLin()) {
                    if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_enterprise);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                        requestCode = REQUEST_CODE_GET_ENTERPRISE;
                        launcher.launch(intent);
                    } else if (BaseApplication.isNetSystemUsable(DetonateStep1Activity.this))
                        onlineDownload();
                    else
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                } else {
                    if (baiSeInfoBean == null) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_enterprise);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_PROJECT);
                        requestCode = REQUEST_CODE_GET_BAI_SE_INFO;
                        launcher.launch(intent);
                    } else if (baiSeBlasterBean == null || baiSeBlasterBean.getData() == null || baiSeBlasterBean.getData().getUserIdCard().isEmpty()) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_detector);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_BLASTER);
                        requestCode = REQUEST_CODE_GET_BAI_SE_BLASTER;
                        launcher.launch(intent);
                    } else
                        baiSeBlasterCheck();
                }
                break;
            case HANDLER_LOCATION_FINISHED:
                StringBuilder coordinate = new StringBuilder();
                tvCoordinate.setShadowLayer(1, 1, 1, Color.BLACK);
                if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0) {
                    coordinate.append(String.format(Locale.getDefault(), getString(R.string.map_position), lastLatLng.longitude, lastLatLng.latitude));
                    tvCoordinate.setTextColor(getColor(R.color.colorCoordinateText));
                    tvCoordinate.setShadowLayer(1, 1, 1, Color.BLUE);
                    coordinate.append(getString(R.string.map_locate_success));
                } else if ((int) BaseApplication.settings.getLatitude() != 0 && (int) BaseApplication.settings.getLongitude() != 0) {
                    tvCoordinate.setTextColor(Color.RED);
                    coordinate.append(String.format(Locale.getDefault(), getString(R.string.map_position), BaseApplication.settings.getLongitude(), BaseApplication.settings.getLatitude()));
                    coordinate.append(getString(R.string.map_locate_fail));
                    myApp.myToast(DetonateStep1Activity.this, R.string.message_use_last_position);
                    msg.getTarget().sendEmptyMessageDelayed(HANDLER_LOCATION_FINISHED, 3000);
                    enabledButton(true);
                } else
                    coordinate.append(getString(R.string.map_position_init));
                if (address != null)
                    coordinate.append("\n").append(getString(R.string.map_address)).append(address);
                tvCoordinate.setText(coordinate);
                break;
            default:
                myApp.myToast(DetonateStep1Activity.this, (String) msg.obj);
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

        myApp = (BaseApplication) getApplication();
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
        btnOffline = findViewById(R.id.btn_offline_auth);
        btnOnline = findViewById(R.id.btn_online_auth);
        tvCoordinate = findViewById(R.id.tv_coordinate);
        if (isBaiSe()) {
            btnOffline.setVisibility(View.VISIBLE);
            baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
            baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
        } else if (isDanLin()) {
            enterpriseBean = DbUtil.getCurrentEnterprise();
            offlineBean = DbUtil.getDanLinDownloadDetonator(true);
            if ((offlineBean == null || offlineBean.getResult() == null || offlineBean.getResult().getLgs() == null
                    || offlineBean.getResult().getLgs().getLg() == null
                    || offlineBean.getResult().getLgs().getLg().size() == 0))
                myApp.myToast(DetonateStep1Activity.this, R.string.message_offline_list_not_found);
        }
        btnOnline.setOnClickListener(view -> launchWhich(KeyEvent.KEYCODE_1));
        btnOffline.setOnClickListener(view -> launchWhich(KeyEvent.KEYCODE_2));
        firstLocate = true;
        enabledButton(false);
        mapView = findViewById(R.id.map_view);
        mapView.getChildAt(2).setPadding(0, 0, 10, 100);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        MapStatus mMapStatus = new MapStatus.Builder().target(new LatLng(BaseApplication.settings.getLatitude() != 0 ? BaseApplication.settings.getLatitude() : 22.551083,
                BaseApplication.settings.getLongitude() != 0 ? BaseApplication.settings.getLongitude() : 110.950548)).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
        baiduMap.setCompassEnable(true);
        baiduMap.setCompassPosition(new Point(10, 10));
        baiduMap.setIndoorEnable(true);
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
        myHandler.sendEmptyMessageDelayed(HANDLER_LOCATION_FINISHED, 2000);
    }

    private void launchWhich(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                if (btnOnline.isEnabled()) {
                    if (isDanLin()) {
                        int check = checkLocation();
                        if (1 == check)
                            myApp.myToast(DetonateStep1Activity.this, R.string.message_not_allow_area);
                        else if (2 == check)
                            myApp.myToast(DetonateStep1Activity.this, R.string.message_forbidden_area);
                        else if (schemeSize > 1)
                            selectScheme(REQUEST_CODE_SELECT_SCHEME);
                        else
                            checkList(offlineBean.getResult().getLgs().getLg(), false);
                    } else
                        enterDetect();
                } else {
                    if (schemeSize == 0)
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_list_not_found);
                    else if (isDanLin()
                            && (offlineBean == null
                            || offlineBean.getResult() == null
                            || offlineBean.getResult().getLgs() == null
                            || offlineBean.getResult().getLgs().getLg() == null
                            || offlineBean.getResult().getLgs().getLg().size() == 0))
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_offline_list_not_found);
                    else if (isBaiSe() && (baiSeBlasterBean == null || !baiSeBlasterBean.isChecked()))
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_bai_se_check_fail);
                }
                break;
            case KeyEvent.KEYCODE_2:
                if (isBaiSe() && btnOffline.isEnabled())
                    checkRegister();
                break;
            case KeyEvent.KEYCODE_F2:
                BaseApplication.writeFile("按F2键, 方案数量:" + schemeSize);
                if (schemeSize > 1)
                    selectScheme(REQUEST_CODE_SELECT_SCHEME_BYPASS);
                else
                    enterDetect();
        }
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

    private LinearLayout newItem(@StringRes int res, String text) {
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(2, 2, 2, 2);
        layoutParams.gravity = Gravity.CENTER;
        LinearLayout layoutItem = new LinearLayout(DetonateStep1Activity.this);
        layoutItem.setLayoutParams(layoutParams);
        layoutItem.setOrientation(LinearLayout.VERTICAL);
        layoutItem.setBackground(AppCompatResources.getDrawable(DetonateStep1Activity.this, R.drawable.shape_list_item_bg));
        TextView textView = new TextView(DetonateStep1Activity.this);
        textView.setText(res);
        textView.setTextColor(getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(layoutParams);
        layoutItem.addView(textView);
        textView = new MarqueeTextView(DetonateStep1Activity.this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMarqueeRepeatLimit(Integer.MAX_VALUE);
        textView.setLayoutParams(layoutParams);
        textView.setSingleLine(true);
        layoutItem.addView(textView);
        return layoutItem;
    }

    private void onlineDownload() {
        final LinearLayout layout = new LinearLayout(DetonateStep1Activity.this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(newItem(R.string.enterprise_code, enterpriseBean.getCode()));
        layout.addView(newItem(R.string.enterprise_id, enterpriseBean.getBlasterId()));
        layout.addView(newItem(R.string.enterprise_id, enterpriseBean.getBlasterId()));
        if (enterpriseBean.isCommercial()) {
            layout.addView(newItem(R.string.enterprise_contract_code, enterpriseBean.getContract()));
            layout.addView(newItem(R.string.enterprise_project_code, enterpriseBean.getProject()));
        }
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(2, 5, 2, 2);
        layoutParams.gravity = Gravity.CENTER;
        TextView textView = new TextView(DetonateStep1Activity.this);
        textView.setText(enterpriseBean.isCommercial() ? R.string.enterprise_commercial : R.string.enterprise_not_commercial);
        textView.setTextColor(DetonateStep1Activity.this.getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(layoutParams);
        layout.addView(textView);
        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep1Activity.this, R.style.AlertDialog)
                .setTitle(R.string.settings_enterprise)
                .setView(layout)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                    enabledButton(false);
                    StringBuilder str = new StringBuilder();
                    for (DetonatorBean bean : list)
                        str.append(bean.getAddress()).append(",");
                    if (str.length() > 0)
                        str.deleteCharAt(str.length() - 1);
                    Map<String, String> params = new HashMap<>();
                    DanLinOnlineDownloadRequestBean bean = new DanLinOnlineDownloadRequestBean();
                    bean.setDwdm(enterpriseBean.getCode());
                    bean.setSbbh(BaseApplication.settings.getExploderID());
                    bean.setJd(validLocation().longitude + "");
                    bean.setWd(validLocation().latitude + "");
                    bean.setUid(str.toString());
                    if (enterpriseBean.isCommercial()) {
                        bean.setHtid(enterpriseBean.getContract());
                        bean.setXmbh(enterpriseBean.getProject());
                    }
                    try {
                        BaseApplication.writeFile(new Gson().toJson(bean));
                        String param = Base64.encodeToString(TripleDESUtil.encrypt(new Gson().toJson(bean).getBytes(), TripleDESUtil.KEY_DAN_LIN.getBytes()), Base64.NO_WRAP);
                        params.put("param", param);
                        BaseApplication.writeFile(param);
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_generate_data_error);
                        return;
                    }
                    OkHttpUtils.post()
                            .url(ConstantUtils.HOST_URL)
                            .params(params)
                            .build().execute(new Callback<String>() {

                                @Override
                                public String parseNetworkResponse(Response response, int i) throws Exception {
                                    if (response.body() != null)
                                        return Objects.requireNonNull(response.body()).string();
                                    return null;
                                }

                                @Override
                                public void onError(Call call, Exception e, int i) {
                                    myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                                    myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                }

                                @Override
                                public void onResponse(String s, int i) {
                                    if (null != s) {
                                        BaseApplication.writeFile(s);
                                        try {
                                            String param = new String(TripleDESUtil.decrypt(Base64.decode(s, Base64.NO_WRAP), TripleDESUtil.KEY_DAN_LIN.getBytes()));
                                            BaseApplication.writeFile(param);
                                            DanLinDetonatorBean danLinDetonatorBean = new DanLinDetonatorBean();
                                            danLinDetonatorBean.setResult(new Gson().fromJson(param, DanLinDetonatorBean.ResultBean.class));
                                            danLinDetonatorBean.setOffline(false);
                                            danLinDetonatorBean.setEnterpriseId(enterpriseBean.getId());
                                            if (null != danLinDetonatorBean.getResult()) {
                                                if (danLinDetonatorBean.getResult().getCwxx().equals("0")) {
                                                    List<LgBean> detonators = danLinDetonatorBean.getResult().getLgs().getLg();
                                                    if (null != detonators) {
                                                        DbUtil.updateDownloadDetonator(danLinDetonatorBean);
                                                        checkList(detonators, true);
                                                    }
                                                } else {
                                                    String error = ErrorCode.downloadErrorCode.get(danLinDetonatorBean.getResult().getCwxx());
                                                    if (null == error) {
                                                        error = getString(R.string.message_download_unknown_error) + danLinDetonatorBean.getResult().getCwxx();
                                                    }
                                                    myApp.myToast(DetonateStep1Activity.this, error);
                                                }
                                            } else {
                                                myApp.myToast(DetonateStep1Activity.this, R.string.message_return_data_error);
                                            }
                                        } catch (Exception e) {
                                            BaseApplication.writeErrorLog(e);
                                            myApp.myToast(DetonateStep1Activity.this, R.string.message_return_data_error);
                                        }
                                    }
                                    myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                }
                            });
                })
                .setNeutralButton(R.string.button_modify, (dialogInterface, i) -> {
                    myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                    requestCode = REQUEST_CODE_EDIT_ENTERPRISE;
                    launcher.launch(new Intent(DetonateStep1Activity.this, EnterpriseActivity.class));
                })
                .show());
    }

    private void baiSeBlasterCheck() {
        final LinearLayout layout = new LinearLayout(DetonateStep1Activity.this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(newItem(R.string.enterprise_id, baiSeBlasterBean.getData().getUserIdCard()));
        layout.addView(newItem(R.string.enterprise_code, baiSeInfoBean.getBurstOrgCode()));
        layout.addView(newItem(ConstantUtils.ENTERPRISE_PROJECT.equals(baiSeInfoBean.getProjectType()) ? R.string.enterprise_project_code : R.string.enterprise_contract_code, baiSeInfoBean.getBurstOrgCode()));
        BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep1Activity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_detector)
                .setView(layout)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                    if (BaseApplication.isNetSystemUsable(DetonateStep1Activity.this)) {
                        try {
                            enabledButton(false);
                            baiSeBlasterBean.getData().setProjectCode(baiSeInfoBean.getProjectCode());
                            baiSeBlasterBean.getData().setBurstOrgCode(baiSeInfoBean.getBurstOrgCode());
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
                                            myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                                            myHandler.sendEmptyMessage(HANDLER_LOCATION_FINISHED);
                                        }

                                        @Override
                                        public void onResponse(BaiSeCheckResultBean baiSeCheckResultBean, int i) {
                                            myHandler.sendEmptyMessage(HANDLER_LOCATION_FINISHED);
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
                    } else
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                })
                .setNeutralButton(R.string.button_modify, (dialogInterface, i) -> {
                    myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                    requestCode = REQUEST_CODE_EDIT_BAI_SE_DETECTOR;
                    launcher.launch(new Intent(DetonateStep1Activity.this, BaiSeDetectorActivity.class));
                })
                .show());
    }

    private void checkList(List<LgBean> detonators, boolean online) {
        boolean correct = true;
        List<ArrayList<DetonatorBean>> lists = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            lists.add(new ArrayList<>());
        for (DetonatorBean bean : list) {
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
            myApp.myToast(DetonateStep1Activity.this, online ? R.string.message_online_check_finished : R.string.message_offline_check_finished);
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
        btnOnline.setEnabled(b && schemeSize > 0 && ((isDanLin() &&
                offlineBean != null && offlineBean.getResult() != null && offlineBean.getResult().getLgs() != null
                && offlineBean.getResult().getLgs().getLg() != null
                && offlineBean.getResult().getLgs().getLg().size() > 0) || (isBaiSe() && baiSeBlasterBean != null && baiSeBlasterBean.isChecked()) || (!isDanLin() && !isBaiSe())));
        btnOffline.setEnabled(b && isBaiSe());
    }

    private LatLng validLocation() {
        if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0)
            return lastLatLng;
        return new LatLng(BaseApplication.settings.getLatitude(), BaseApplication.settings.getLongitude());
    }

    /**
     * @return 0在准爆区域 1不在准爆区域 2在禁爆区域
     */
    private int checkLocation() {
        if (null != offlineBean) {
            for (JbqyBean bean : offlineBean.getResult().getJbqys().getJbqy()) {
                try {
                    if (BaseApplication.distance(Double.parseDouble(bean.getJbqywd()), Double.parseDouble(bean.getJbqyjd()), validLocation().latitude, validLocation().longitude)
                            < Double.parseDouble(bean.getJbqybj())) {
                        return 2;
                    }
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
            for (ZbqyBean bean : offlineBean.getResult().getZbqys().getZbqy()) {
                try {
                    if (BaseApplication.distance(Double.parseDouble(bean.getZbqywd()), Double.parseDouble(bean.getZbqyjd()), validLocation().latitude, validLocation().longitude)
                            < Double.parseDouble(bean.getZbqybj())) {
                        return 0;
                    }
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
        }
        return 1;
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
                    address = location.getAddrStr();
                    lastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myHandler.sendEmptyMessage(HANDLER_LOCATION_FINISHED);
                    if (firstLocate) {
                        MapStatus mMapStatus = new MapStatus.Builder().target(lastLatLng).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
                        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
                        firstLocate = false;
                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                    }
                    BaseApplication.settings.setLatitude(location.getLatitude());
                    BaseApplication.settings.setLongitude(location.getLongitude());
                    BaseApplication.saveSettings();
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
