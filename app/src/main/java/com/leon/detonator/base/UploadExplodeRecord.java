package com.leon.detonator.base;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.gson.Gson;
import com.leon.detonator.R;
import com.leon.detonator.activity.BaiSeDataActivity;
import com.leon.detonator.activity.EnterpriseActivity;
import com.leon.detonator.activity.InfoListActivity;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.BaiSeUploadResultBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.UploadExplodeRecordsBean;
import com.leon.detonator.component.MarqueeTextView;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.mina.client.MinaClient;
import com.leon.detonator.mina.client.MinaHandler;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadExplodeRecord extends Thread {
    private final BaseApplication myApp;
    private final BaseActivity activity;
    private final ActivityResultLauncher<Intent> launcher;
    private final List<ExplosionRecordBean> list;
    private List<DetonatorBean> detonators;
    private EnterpriseBean enterpriseBean;
    private BaiSeInfoBean baiSeInfoBean;
    private MinaClient minaClient;
    public static Handler myHandler;
    private final Handler handler;
    private static final int HANDLER_TIMEOUT = 1;
    public static final int HANDLER_SUCCESS = 200;
    public static final int HANDLER_FAIL = 201;
    public static boolean uploading;
    private int uploadIndex;
    private int receiveCount;
    private boolean cancel;

    public UploadExplodeRecord(BaseActivity activity, List<ExplosionRecordBean> list, Handler handler, ActivityResultLauncher<Intent> launcher) {
        this.list = list;
        this.handler = handler;
        this.activity = activity;
        this.launcher = launcher;
        uploadIndex = -1;
        myApp = (BaseApplication) activity.getApplication();
        myHandler = new Handler(handler.getLooper(), msg -> {
            switch (msg.what) {
                case BaseApplication.HANDLER_REGISTER_SUCCESS:
                case HANDLER_SUCCESS:
                    prepareUpload();
                    break;
                case BaseApplication.HANDLER_REGISTER_ERROR:
                case HANDLER_FAIL:
                    uploadFail(false);
                    break;
                case MinaHandler.MINA_DATA:
                    msg.getTarget().removeMessages(HANDLER_TIMEOUT);
                    BaseApplication.writeFile((String) msg.obj);
                    if (((String) msg.obj).contains("R"))
                        uploadZBao();
                    else {
                        if (((String) msg.obj).startsWith("#") && ((String) msg.obj).endsWith("$"))
                            receiveCount++;
                        if (receiveCount >= 3) {
                            if (BaseApplication.settings.getServerHost() == 2)
                                uploadBaiSe();
                            else if (BaseApplication.settings.getServerHost() == 3)
                                uploadDanLing();
                            else
                                uploadSuccess();
                        }
                    }
                    break;
                case MinaHandler.MINA_NORMAL:
                    if (null != msg.obj) {
                        BaseApplication.writeFile((String) msg.obj);
                        myApp.myToast(activity, (String) msg.obj);
                    }
                    break;
                case MinaHandler.MINA_ERROR:
                    msg.getTarget().removeMessages(HANDLER_TIMEOUT);
                    if (null != msg.obj)
                        BaseApplication.writeFile((String) msg.obj);
                    uploadFail(false);
                    break;
                case HANDLER_TIMEOUT:
                    myApp.myToast(activity, R.string.message_check_network);
                    uploadFail(false);
                    break;
            }
            return false;
        });
    }

    @Override
    public void run() {
        super.run();
        if (BaseApplication.isNetSystemUsable(activity)) {
            uploading = true;
            if (!BaseApplication.settings.isRegistered())
                myApp.registerExploder(myHandler);
            else
                prepareUpload();
        } else {
            myApp.myToast(activity, R.string.message_check_network);
            handler.obtainMessage(HANDLER_FAIL, -1).sendToTarget();
        }
        while (uploading) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        if (minaClient != null)
            minaClient.closeConnect();
    }

    private void prepareUpload() {
        if (uploading)
            if (!BaseApplication.settings.isRegistered() || null == BaseApplication.settings.getExploderID() || BaseApplication.settings.getExploderID().isEmpty()) {
                uploadFail(false);
                myApp.myToast(activity, R.string.message_not_registered);
            } else
                switch (BaseApplication.settings.getServerHost()) {
                    case 0:
                    case 3:
                        enterpriseBean = DbUtil.getCurrentEnterprise();
                        if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
                            myApp.myToast(activity, R.string.message_select_enterprise);
                            Intent intent = new Intent(activity, InfoListActivity.class);
                            intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                            launcher.launch(intent);
                        } else
                            enterpriseDialog();
                        break;
                    case 2:
                        baiSeInfoBean = DbUtil.getCurrentBaiSeInfo();
                        if (null == baiSeInfoBean || baiSeInfoBean.getBurstOrgCode().isEmpty()) {
                            myApp.myToast(activity, R.string.message_select_enterprise);
                            Intent intent = new Intent(activity, InfoListActivity.class);
                            intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_PROJECT);
                            launcher.launch(intent);
                        } else
                            baiSeDialog();
                        break;
                    default:
                        activity.runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(activity, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_upload)
                                .setMessage(String.format(Locale.getDefault(), activity.getString(R.string.dialog_confirm_upload), ConstantUtils.UPLOAD_HOST[BaseApplication.settings.getServerHost()][0]))
                                .setPositiveButton(R.string.button_confirm, (dialog, w) -> uploadNextRecord())
                                .setNegativeButton(R.string.button_cancel, (dialog, w) -> uploadFail(true))
                                .show()));
                        break;
                }
    }

    private LinearLayout newItem(@StringRes int res, String text) {
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(2, 2, 2, 2);
        layoutParams.gravity = Gravity.CENTER;
        LinearLayout layoutItem = new LinearLayout(activity);
        layoutItem.setLayoutParams(layoutParams);
        layoutItem.setOrientation(LinearLayout.VERTICAL);
        layoutItem.setBackground(AppCompatResources.getDrawable(activity, R.drawable.shape_list_item_bg));
        TextView textView = new TextView(activity);
        textView.setText(res);
        textView.setTextColor(activity.getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(layoutParams);
        layoutItem.addView(textView);
        textView = new MarqueeTextView(activity);
        textView.setText(text);
        textView.setTextColor(activity.getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMarqueeRepeatLimit(Integer.MAX_VALUE);
        textView.setLayoutParams(layoutParams);
        textView.setSingleLine(true);
        layoutItem.addView(textView);
        return layoutItem;
    }

    private void enterpriseDialog() {
        final LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(newItem(R.string.enterprise_code, enterpriseBean.getCode()));
        layout.addView(newItem(R.string.enterprise_id, enterpriseBean.getBlasterId()));
        if (enterpriseBean.isCommercial()) {
            layout.addView(newItem(R.string.enterprise_contract_code, enterpriseBean.getContract()));
            layout.addView(newItem(R.string.enterprise_project_code, enterpriseBean.getProject()));
        }
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(2, 5, 2, 2);
        layoutParams.gravity = Gravity.CENTER;
        TextView textView = new TextView(activity);
        textView.setText(enterpriseBean.isCommercial() ? R.string.enterprise_commercial : R.string.enterprise_not_commercial);
        textView.setTextColor(activity.getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(layoutParams);
        layout.addView(textView);
        activity.runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(activity, R.style.AlertDialog)
                .setTitle(R.string.settings_enterprise)
                .setView(layout)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> uploadNextRecord())
                .setNeutralButton(R.string.button_modify, (dialogInterface, i) -> launcher.launch(new Intent(activity, EnterpriseActivity.class)))
                .show()));
    }

    private void baiSeDialog() {
        final LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(newItem(R.string.enterprise_name, baiSeInfoBean.getBurstOrgName()));
        layout.addView(newItem(R.string.enterprise_code, baiSeInfoBean.getBurstOrgCode()));
        layout.addView(newItem(ConstantUtils.ENTERPRISE_PROJECT.equals(baiSeInfoBean.getProjectType()) ? R.string.enterprise_project_name : R.string.enterprise_contract_name, baiSeInfoBean.getProjectName()));
        layout.addView(newItem(ConstantUtils.ENTERPRISE_PROJECT.equals(baiSeInfoBean.getProjectType()) ? R.string.enterprise_project_code : R.string.enterprise_contract_code, baiSeInfoBean.getProjectCode()));
        layout.addView(newItem(R.string.enterprise_blaster_name, baiSeInfoBean.getBursterName()));
        layout.addView(newItem(R.string.enterprise_id, baiSeInfoBean.getIdCard()));
        activity.runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(activity, R.style.AlertDialog)
                .setTitle(R.string.settings_enterprise)
                .setView(layout)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> uploadNextRecord())
                .setNeutralButton(R.string.button_modify, (dialogInterface, i) -> launcher.launch(new Intent(activity, BaiSeDataActivity.class)))
                .show()));
    }

    private void uploadNextRecord() {
        if (uploading)
            if (++uploadIndex >= list.size()) {
                handler.sendEmptyMessage(HANDLER_SUCCESS);
                uploading = false;
            } else {
                detonators = DbUtil.getDetonatorList(list.get(uploadIndex).getId());
                if (BaseApplication.settings.getServerHost() == 0)
                    uploadDanLing();
                else
                    uploadZBao();
            }
    }

    private void uploadDanLing() {
        StringBuilder str = new StringBuilder();
        for (DetonatorBean bean : detonators)
            str.append(bean.getAddress()).append(",");
        str.deleteCharAt(str.length() - 1);
        String token = myApp.makeToken();
        Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_UPLOAD_RECORDS);
        if (null != params) {
            params.put("dsc", str.toString());
            params.put("dwdm", enterpriseBean.getCode());
            params.put("bprysfz", enterpriseBean.getBlasterId());
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
            params.put("bpsj", df.format(list.get(uploadIndex).getExplodeTime()));
            params.put("jd", list.get(uploadIndex).getLng() + "");
            params.put("wd", list.get(uploadIndex).getLat() + "");

            if (enterpriseBean.isCommercial()) {
                params.put("htid", enterpriseBean.getContract());
                params.put("xmbh", enterpriseBean.getProject());
            }
            params.put("signature", myApp.signature(params));
            OkHttpUtils.post()
                    .url(ConstantUtils.HOST_URL)
                    .params(params)
                    .build().execute(new Callback<UploadExplodeRecordsBean>() {
                        @Override
                        public UploadExplodeRecordsBean parseNetworkResponse(Response response, int i) throws Exception {
                            if (response.body() != null) {
                                String string = Objects.requireNonNull(response.body()).string();
                                return BaseApplication.jsonFromString(string, UploadExplodeRecordsBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            if (uploading)
                                myApp.myToast(activity, R.string.message_check_network);
                            uploadFail(false);
                        }

                        @Override
                        public void onResponse(UploadExplodeRecordsBean uploadExplodeRecordsBean, int i) {
                            if (null != uploadExplodeRecordsBean) {
                                if (uploadExplodeRecordsBean.getToken().equals(token)) {
                                    if (uploadExplodeRecordsBean.isStatus()) {
                                        if (null != uploadExplodeRecordsBean.getResult()) {
                                            if (uploadExplodeRecordsBean.getResult().isSuccess()) {
                                                uploadSuccess();
                                                return;
                                            } else if (uploading)
                                                handler.obtainMessage(HANDLER_FAIL, list.get(uploadIndex).getId()).sendToTarget();
                                        }
                                    } else if (uploading) {
                                        myApp.myToast(activity, uploadExplodeRecordsBean.getDescription());
                                        handler.obtainMessage(HANDLER_FAIL, list.get(uploadIndex).getId()).sendToTarget();
                                    }
                                    uploadNextRecord();
                                } else {
                                    if (uploading)
                                        myApp.myToast(activity, R.string.message_token_error);
                                    uploadFail(false);
                                }
                            } else {
                                if (uploading)
                                    myApp.myToast(activity, R.string.message_return_data_error);
                                uploadFail(false);
                            }
                        }
                    });
        }
    }

    private void uploadZBao() {
        BaseApplication.writeFile(activity.getString(R.string.button_upload) + ", " + ConstantUtils.UPLOAD_HOST[1][0] + ", " + list.get(uploadIndex).getName());
        receiveCount = 0;
        myHandler.sendEmptyMessageDelayed(HANDLER_TIMEOUT, ConstantUtils.UPLOAD_TIMEOUT);
        new Thread(() -> {
            try {
                if (null == minaClient)
                    minaClient = new MinaClient();
                minaClient.setDetonatorList(detonators);
                minaClient.setExplodeTime(list.get(uploadIndex).getExplodeTime());
                minaClient.setHandler(myHandler);
                minaClient.setHost(ConstantUtils.UPLOAD_HOST[BaseApplication.settings.getServerHost()][1]);
                minaClient.setLng(list.get(uploadIndex).getLng());
                minaClient.setLat(list.get(uploadIndex).getLat());
                String sn = BaseApplication.settings.getExploderID();
                minaClient.setSn(sn.substring(1, 5) + sn.substring(sn.length() - 4));
                minaClient.uploadRecord();
            } catch (Exception e) {
                uploadFail(false);
                BaseApplication.writeErrorLog(e);
            }
        }).start();
    }

    private void uploadBaiSe() {
        try {
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
            baiSeInfoBean.setLngLat(String.format(Locale.getDefault(), "%f,%f", list.get(uploadIndex).getLng(), list.get(uploadIndex).getLat()));
            baiSeInfoBean.setGpsCoordinateSystems(ConstantUtils.GPS_SYSTEM);
            baiSeInfoBean.setDeviceNO(BaseApplication.settings.getExploderID());
            baiSeInfoBean.setBurstTime(df.format(list.get(uploadIndex).getExplodeTime()));
            baiSeInfoBean.setDetonatorCount(list.get(uploadIndex).getAmount());
            DbUtil.updateBaiSeInfo(baiSeInfoBean);
            BaseApplication.writeFile(activity.getString(R.string.button_upload) + ", " + ConstantUtils.UPLOAD_HOST[2][0] + ", " + list.get(uploadIndex).getName());
            BaseApplication.writeFile(new Gson().toJson(baiSeInfoBean));
            OkHttpUtils.postString().addHeader("access-token", ConstantUtils.ACCESS_TOKEN)
                    .url(ConstantUtils.BAI_SE_UPLOAD_URL)
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .content(new Gson().toJson(baiSeInfoBean))
                    .build().execute(new Callback<BaiSeUploadResultBean>() {
                        @Override
                        public BaiSeUploadResultBean parseNetworkResponse(Response response, int i) throws Exception {
                            ResponseBody body = response.body();
                            if (body != null) {
                                String string = body.string();
                                return new Gson().fromJson(string, BaiSeUploadResultBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            if (uploading)
                                myApp.myToast(activity, R.string.message_check_network);
                            uploadFail(false);
                        }

                        @Override
                        public void onResponse(BaiSeUploadResultBean baiSeUploadResultBean, int i) {
                            if (baiSeUploadResultBean != null) {
                                if (baiSeUploadResultBean.isSuccess()) {
                                    BaiSeBlasterBean baiSeBlasterBean = DbUtil.getCurrentBaiSeBlaster();
                                    if (baiSeBlasterBean != null) {
                                        baiSeBlasterBean.setChecked(false);
                                        DbUtil.updateBaiSeBlaster(baiSeBlasterBean);
                                    }
                                    uploadSuccess();
                                } else if (baiSeUploadResultBean.getMessage() != null) {
                                    if (uploading)
                                        myApp.myToast(activity, baiSeUploadResultBean.getMessage());
                                    uploadFail(false);
                                }
                            } else
                                uploadFail(false);
                        }
                    });
        } catch (Exception e) {
            uploadFail(false);
            BaseApplication.writeErrorLog(e);
        }
    }

    private void uploadSuccess() {
        if (uploading) {
            handler.obtainMessage(HANDLER_SUCCESS, list.get(uploadIndex).getId()).sendToTarget();
            list.get(uploadIndex).setUploadServer(BaseApplication.settings.getServerHost());
            list.get(uploadIndex).setUploadTime(new Date());
            list.get(uploadIndex).setSynchronize(false);
            DbUtil.updateExplosionRecord(list.get(uploadIndex));
            uploadNextRecord();
        }
    }

    private void uploadFail(boolean cancel) {
        if (uploading) {
            if (cancel)
                handler.obtainMessage(HANDLER_FAIL, -1).sendToTarget();
            else
                handler.sendEmptyMessage(HANDLER_FAIL);
            uploading = false;
        }
    }
}
