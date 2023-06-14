package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
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
import com.leon.detonator.base.CheckRegister;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeCheckResultBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.ZbqyBean;
import com.leon.detonator.component.MarqueeTextView;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
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
    private MapView mapView = null;
    private BaiduMap baiduMap;
    private LocationClient locationClient;
    private EnterpriseBean enterpriseBean;
    private String token;
    private List<DetonatorBean> list;
    private MyButton btnOnline;
    private MyButton btnOffline;
    private LatLng lastLatLng;
    private DownloadDetonatorBean offlineBean;
    private BaiSeInfoBean baiSeInfoBean;
    private BaiSeBlasterBean baiSeBlasterBean;
    private BaseApplication myApp;
    private String address;
    private boolean firstLocate;
    private int requestCode;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode())
            switch (requestCode) {
                case 0:
                    enterpriseBean = DbUtil.getCurrentEnterprise(DetonateStep1Activity.this);
                    break;
                case 1:
                    baiSeInfoBean = DbUtil.getCurrentBaiSeInfo(DetonateStep1Activity.this);
                    break;
                case 2:
                    baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster(DetonateStep1Activity.this);
                    break;
                case 3:
                    enterpriseBean = DbUtil.getCurrentEnterprise(DetonateStep1Activity.this);
                    onlineDownload();
                    break;
                case 4:
                    baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster(DetonateStep1Activity.this);
                    baiSeBlasterCheck();
                    break;
            }
    });
    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case 1:
                enabledButton(true);
                break;
            case 2:
                if (0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
                    if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_enterprise);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                        requestCode = 0;
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
                        requestCode = 1;
                        launcher.launch(intent);
                    } else if (baiSeBlasterBean == null || baiSeBlasterBean.getData() == null || baiSeBlasterBean.getData().getUserIdCard().isEmpty()) {
                        myApp.myToast(DetonateStep1Activity.this, R.string.message_select_detector);
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_BLASTER);
                        requestCode = 2;
                        launcher.launch(intent);
                    } else
                        launchWhich(KeyEvent.KEYCODE_F2);
                }
                break;
            case 3:
                StringBuilder coordinate = new StringBuilder();
                if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0) {
                    coordinate.append(String.format(Locale.getDefault(), getString(R.string.map_position), lastLatLng.longitude, lastLatLng.latitude));
                    ((TextView) findViewById(R.id.tv_coordinate)).setTextColor(getColor(R.color.colorCoordinateText));
                    ((TextView) findViewById(R.id.tv_coordinate1)).setTextColor(Color.BLUE);
                    coordinate.append(getString(R.string.map_locate_success));
                } else if ((int) BaseApplication.settings.getLatitude() != 0 && (int) BaseApplication.settings.getLongitude() != 0) {
                    ((TextView) findViewById(R.id.tv_coordinate)).setTextColor(Color.RED);
                    ((TextView) findViewById(R.id.tv_coordinate1)).setTextColor(Color.BLACK);
                    coordinate.append(String.format(Locale.getDefault(), getString(R.string.map_position), BaseApplication.settings.getLongitude(), BaseApplication.settings.getLatitude()));
                    coordinate.append(getString(R.string.map_locate_fail));
                    myApp.myToast(DetonateStep1Activity.this, R.string.message_use_last_position);
                    msg.getTarget().sendEmptyMessageDelayed(3, 8000);
                } else
                    coordinate.append(getString(R.string.map_position_init));
                if (address != null)
                    coordinate.append("\n").append(getString(R.string.map_address)).append(address);
                ((TextView) findViewById(R.id.tv_coordinate)).setText(coordinate);
                ((TextView) findViewById(R.id.tv_coordinate1)).setText(coordinate);
                break;
            case 4:
                baiSeBlasterCheck();
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
        list = DbUtil.getCurrentDetonatorList(DetonateStep1Activity.this);
        btnOffline = findViewById(R.id.btn_offline_auth);
        btnOnline = findViewById(R.id.btn_online_auth);
        if (0 == BaseApplication.settings.getServerHost() || 2 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
            findViewById(R.id.btn_offline_auth).setVisibility(View.VISIBLE);
            if (2 == BaseApplication.settings.getServerHost()) {
                baiSeInfoBean = DbUtil.getCurrentBaiSeInfo(DetonateStep1Activity.this);
                baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster(DetonateStep1Activity.this);
                btnOnline.setTextId(R.string.button_online_detect);
                btnOffline.setTextId(R.string.button_auth);
            } else {
                btnOnline.setTextId(R.string.button_online_auth);
                enterpriseBean = DbUtil.getCurrentEnterprise(DetonateStep1Activity.this);
                offlineBean = DbUtil.getDownloadDetonator(DetonateStep1Activity.this, true);
            }
        } else {
            btnOffline.setVisibility(View.GONE);
            btnOnline.setTextId(R.string.button_online_detect);
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
        myHandler.sendEmptyMessageDelayed(3, 2000);
    }

    private void launchWhich(int which) {
        boolean enterDetect = false;
        if (KeyEvent.KEYCODE_1 == which && btnOnline.isEnabled()) {
            if (0 != BaseApplication.settings.getServerHost() && 2 != BaseApplication.settings.getServerHost() && 3 != BaseApplication.settings.getServerHost()) {
                enterDetect = true;
            } else {
                checkRegister(2);
            }
        } else if (KeyEvent.KEYCODE_2 == which && btnOffline.isEnabled()) {
            if (0 == BaseApplication.settings.getServerHost() || 3 == BaseApplication.settings.getServerHost()) {
                if (!checkLocation())
                    myApp.myToast(DetonateStep1Activity.this, R.string.message_not_allow_area);
                else
                    checkList(offlineBean.getResult().getLgs().getLg(), false);
            } else if (2 == BaseApplication.settings.getServerHost()) {
                checkRegister(4);
            }
        } else if (KeyEvent.KEYCODE_F2 == which)
            enterDetect = true;
        if (enterDetect) {
            Intent intent = new Intent().setClass(DetonateStep1Activity.this, DetonateStep2Activity.class);
            intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, false);
            intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false));
            intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, validLocation().latitude);
            intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, validLocation().longitude);
            intent.putParcelableArrayListExtra(KeyUtils.KEY_LIST, (ArrayList<DetonatorBean>) list);
            BaseApplication.writeFile("Locate:" + validLocation().latitude + ", " + validLocation().longitude);
            startActivity(intent);
            finish();
        }
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
                                        myApp.myToast(DetonateStep1Activity.this, R.string.message_check_network);
                                        myHandler.sendEmptyMessage(1);
                                    }

                                    @Override
                                    public void onResponse(DownloadDetonatorBean onlineBean, int i) {
                                        myHandler.sendEmptyMessage(1);
                                        if (null != onlineBean) {
                                            if (onlineBean.getToken().equals(token)) {
                                                if (onlineBean.isStatus()) {
                                                    if (null != onlineBean.getResult()) {
                                                        if (onlineBean.getResult().getCwxx().equals("0")) {
                                                            List<LgBean> detonators = onlineBean.getResult().getLgs().getLg();
                                                            if (null != detonators) {
                                                                DbUtil.addDownloadDetonator(DetonateStep1Activity.this, false, onlineBean);
                                                                checkList(detonators, true);
                                                            }
                                                        } else {
                                                            String error = ErrorCode.downloadErrorCode.get(onlineBean.getResult().getCwxx());
                                                            if (null == error) {
                                                                error = getString(R.string.message_download_unknown_error) + onlineBean.getResult().getCwxx();
                                                            }
                                                            myApp.myToast(DetonateStep1Activity.this, error);
                                                        }
                                                    }
                                                } else
                                                    myApp.myToast(DetonateStep1Activity.this, onlineBean.getDescription());
                                            } else
                                                myApp.myToast(DetonateStep1Activity.this, R.string.message_token_error);
                                        } else
                                            myApp.myToast(DetonateStep1Activity.this, R.string.message_return_data_error);
                                    }
                                });
                    }
                })
                .setNeutralButton(R.string.button_modify, (dialogInterface, i) -> {
                    enabledButton(true);
                    requestCode = 3;
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
                                            myHandler.sendEmptyMessage(1);
                                        }

                                        @Override
                                        public void onResponse(BaiSeCheckResultBean baiSeCheckResultBean, int i) {
                                            myHandler.sendEmptyMessage(1);
                                            if (baiSeCheckResultBean != null) {
                                                if (baiSeCheckResultBean.isSuccess() && baiSeCheckResultBean.getData().isIsPass()) {
                                                    myApp.myToast(DetonateStep1Activity.this, R.string.message_bai_se_check_success);
                                                    baiSeBlasterBean.setChecked(true);
                                                    DbUtil.updateBaiSeBlaster(DetonateStep1Activity.this, baiSeBlasterBean);
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
                    enabledButton(true);
                    if (baiSeBlasterBean == null) {
                        Intent intent = new Intent(DetonateStep1Activity.this, InfoListActivity.class);
                        intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_BLASTER);
                        requestCode = 4;
                        launcher.launch(intent);
                    } else {
                        requestCode = 4;
                        launcher.launch(new Intent(DetonateStep1Activity.this, BaiSeDetectorActivity.class));
                    }
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
            intent.putParcelableArrayListExtra(KeyUtils.KEY_LIST, (ArrayList<DetonatorBean>) list);
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
        btnOnline.setEnabled(b && list.size() > 0 && (2 != BaseApplication.settings.getServerHost() || (baiSeBlasterBean != null && baiSeBlasterBean.isChecked())));
        btnOffline.setEnabled(b && (2 == BaseApplication.settings.getServerHost()
                || (list.size() > 0 && 2 != BaseApplication.settings.getServerHost() && (offlineBean != null && offlineBean.getResult() != null && offlineBean.getResult().getLgs() != null
                && offlineBean.getResult().getLgs().getLg() != null
                && offlineBean.getResult().getLgs().getLg().size() > 0))));
    }

    private void checkRegister(int i) {
        if (!BaseApplication.settings.isRegistered()) {
            myApp.registerExploder();
            enabledButton(false);
            new CheckRegister(this) {
                @Override
                public void onError() {
                    myHandler.sendEmptyMessage(1);
                }

                @Override
                public void onSuccess() {
                    myHandler.sendEmptyMessage(i);
                }
            }.start();
        } else {
            myHandler.sendEmptyMessage(i);
        }
    }

    private LatLng validLocation() {
        if (null != lastLatLng && (int) lastLatLng.latitude != 0 && (int) lastLatLng.longitude != 0)
            return lastLatLng;
        return new LatLng(BaseApplication.settings.getLatitude(), BaseApplication.settings.getLongitude());
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
                    address = location.getAddrStr();
                    lastLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myHandler.sendEmptyMessage(3);
                    if (firstLocate) {
                        MapStatus mMapStatus = new MapStatus.Builder().target(lastLatLng).zoom(17).build();  //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
                        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                        baiduMap.setMapStatus(mMapStatusUpdate);//改变地图状态
                        firstLocate = false;
                        myHandler.sendEmptyMessage(1);
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
