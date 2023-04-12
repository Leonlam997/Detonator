package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.StringRes;

import com.google.gson.Gson;
import com.leon.detonator.adapter.ExplosionRecordAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.CheckRegister;
import com.leon.detonator.bean.BaiSeCheck;
import com.leon.detonator.bean.BaiSeUpload;
import com.leon.detonator.bean.BaiSeUploadResult;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.bean.UploadExplodeRecordsBean;
import com.leon.detonator.bean.UploadServerBean;
import com.leon.detonator.dialog.EnterpriseDialog;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.mina.client.MinaClient;
import com.leon.detonator.mina.client.MinaHandler;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ExplosionRecordActivity extends BaseActivity {
    private CheckBox cbSelected;
    private ListView tableListView;
    private ExplosionRecordAdapter adapter;
    private List<ExplosionRecordBean> list;
    private List<UploadServerBean> uploadList;
    private EnterpriseBean enterpriseBean;
    private EnterpriseDialog enterpriseDialog;
    private String token;
    private int uploadIndex, receiveCount, successCount, forceDelete;
    private MinaClient minaClient;
    private LocalSettingBean settingBean;
    private MyProgressDialog pDialog;
    private BaseApplication myApp;

    private final Handler msgHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case MinaHandler.MINA_DATA:
                    msgHandler.removeMessages(4);
                    BaseApplication.writeFile((String) message.obj);
                    if (((String) message.obj).contains("R")) {
                        uploadServer(true);
                    } else {
                        if (((String) message.obj).startsWith("#") && ((String) message.obj).endsWith("$"))
                            receiveCount++;
                        if (receiveCount >= 3) {
                            successCount++;
                            moveFile();
                            if (settingBean.getServerHost() == 2)
                                uploadBaiSe();
                            else
                                uploadRecord();
                        }
                    }
                    break;
                case MinaHandler.MINA_NORMAL:
//                    msgHandler.removeMessages(4);
                    if (null != message.obj) {
                        BaseApplication.writeFile((String) message.obj);
                        myApp.myToast(ExplosionRecordActivity.this, (String) message.obj);
                    }
                    break;
                case MinaHandler.MINA_ERROR:
                    msgHandler.removeMessages(4);
                    if (null != message.obj)
                        BaseApplication.writeFile((String) message.obj);
                    disableButton(false);
                    showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_upload_fail_number), uploadIndex + 1));
                case 4:
                    disableButton(false);
                    BaseApplication.writeFile(getString(R.string.message_network_timeout));
                    myApp.myToast(ExplosionRecordActivity.this, R.string.message_network_timeout);
                    break;
            }
            return false;
        }
    });

    private final Handler checkExploderHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case 1:
                    disableButton(false);
                    break;
                case 2:
                    settingBean = BaseApplication.readSettings();
                    if (!settingBean.isRegistered() || null == settingBean.getExploderID() || settingBean.getExploderID().isEmpty()) {
                        showMessage(R.string.message_not_registered);
                    } else if (0 == settingBean.getServerHost() && (null == enterpriseBean || enterpriseBean.getCode().isEmpty())) {
                        showMessage(R.string.message_input_enterprise_code);
                        startActivity(new Intent(ExplosionRecordActivity.this, EnterpriseActivity.class));
                    } else {
                        int count = 0;
                        for (ExplosionRecordBean b : list)
                            if (b.isSelected())
                                count++;
                        if (0 == count)
                            myApp.myToast(ExplosionRecordActivity.this, R.string.message_no_select_record);
                        else {
                            BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                                    .setTitle(R.string.dialog_title_upload)
                                    .setMessage(String.format(Locale.CHINA, getResources().getString(R.string.dialog_confirm_upload_all), count, ConstantUtils.UPLOAD_HOST[settingBean.getServerHost()][0]))
                                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> checkExploderHandler.sendEmptyMessage(3))
                                    .setNegativeButton(R.string.btn_cancel, null)
                                    .show());
                        }
                    }
                    break;
                case 3:
                    int selected = 0;
                    for (ExplosionRecordBean b : list)
                        if (b.isSelected()) {
                            selected++;
                        }
                    uploadIndex = -1;
                    pDialog = new MyProgressDialog(ExplosionRecordActivity.this);
                    pDialog.setInverseBackgroundForced(false);
                    pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    pDialog.setTitle(R.string.progress_title);
                    pDialog.setMessage(getResources().getString(R.string.progress_upload));
                    pDialog.setMax(selected);
                    pDialog.setProgress(0);
                    pDialog.show();
                    uploadRecord();
                    break;
                case 4:
                    myApp.myToast(ExplosionRecordActivity.this, (String) message.obj);
                    BaseApplication.writeFile((String) message.obj);
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explode_record);

        setTitle(R.string.detonate_rec);

        myApp = (BaseApplication) getApplication();
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));

        initData();

        tableListView = findViewById(R.id.lv_record_list);

        adapter = new ExplosionRecordAdapter(this, list);
        tableListView.setAdapter(adapter);
        cbSelected = findViewById(R.id.cb_selected);
        cbSelected.setOnClickListener(v -> {
            for (ExplosionRecordBean item : list) {
                item.setSelected(cbSelected.isChecked());
            }
            adapter.updateList(list);
        });

        tableListView.setOnItemClickListener((parent, view, position, id) -> {
            list.get(position).setSelected(!list.get(position).isSelected());
            checkboxStatus();
            adapter.updateList(list);
        });

        tableListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Intent intent = new Intent();
            intent.setClass(ExplosionRecordActivity.this, DetonatorListActivity.class);
            intent.putExtra(KeyUtils.KEY_CREATE_DELAY_LIST, ConstantUtils.HISTORY_LIST);
            try {
                ArrayList<DetonatorInfoBean> temp = new ArrayList<>();
                myApp.readFromFile(list.get(position).getRecordPath(), temp, DetonatorInfoBean.class);
                intent.putExtra(KeyUtils.KEY_RECORD_LIST, temp);
                intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, list.get(position).getLat());
                intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, list.get(position).getLng());
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
            startActivity(intent);
            return false;
        });
        tableListView.requestFocus();

        findViewById(R.id.btn_upload).setOnClickListener(v -> launchWhich(KeyEvent.KEYCODE_1));

        findViewById(R.id.btn_delete).setOnClickListener(v -> launchWhich(KeyEvent.KEYCODE_2));
    }

    private void checkboxStatus() {
        cbSelected.setChecked(true);
        for (ExplosionRecordBean item : list) {
            if (!item.isSelected()) {
                cbSelected.setChecked(false);
                break;
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (launchWhich(keyCode))
            return true;
        return super.onKeyUp(keyCode, event);
    }

    private void showMessage(String s) {
        checkExploderHandler.obtainMessage(4, s).sendToTarget();
    }

    private void showMessage(@StringRes int s) {
        checkExploderHandler.obtainMessage(4, getString(s)).getTarget();
    }

    private boolean launchWhich(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                forceDelete = 0;
                successCount = 0;
                if (BaseApplication.isNetSystemUsable(ExplosionRecordActivity.this)) {
                    if (!settingBean.isRegistered()) {
                        myApp.registerExploder();
                        disableButton(true);
                        new CheckRegister() {
                            @Override
                            public void onError() {
                                checkExploderHandler.sendEmptyMessage(1);
                            }

                            @Override
                            public void onSuccess() {
                                checkExploderHandler.sendEmptyMessage(2);
                            }
                        }.setActivity(this).start();
                    } else if (findViewById(R.id.btn_upload).isEnabled()) {
                        checkExploderHandler.sendEmptyMessage(2);
                    }
                } else {
                    myApp.myToast(ExplosionRecordActivity.this, R.string.message_check_network);
                }
                break;
            case KeyEvent.KEYCODE_2:
                boolean canDelete = false;
                for (ExplosionRecordBean bean : list) {
                    if (bean.isSelected()) {
                        canDelete = true;
                        break;
                    }
                }
                if (!canDelete) {
                    showMessage(R.string.message_no_select_record);
                    break;
                }
                if (forceDelete != 4) {
                    for (ExplosionRecordBean bean : list) {
                        if (bean.isSelected() && !bean.isUploaded()) {
                            showMessage(R.string.message_cannot_delete_not_upload);
                            canDelete = false;
                            break;
                        }
                    }
                }
                forceDelete = 0;
                if (canDelete) {
                    BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_delete_record)
                            .setMessage(R.string.dialog_confirm_delete_record)
                            .setPositiveButton(R.string.btn_confirm, (dialog1, which1) -> {
                                Iterator<ExplosionRecordBean> it = list.iterator();
                                while (it.hasNext()) {
                                    ExplosionRecordBean b = it.next();
                                    if (b.isSelected()) {
                                        File file = new File(b.getRecordPath());
                                        if (file.exists() && !file.delete()) {
                                            showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
                                        }
                                        BaseApplication.writeFile(getString(R.string.dialog_title_delete_record) + ", " + b.getRecordPath());
                                        Iterator<UploadServerBean> it1 = uploadList.iterator();
                                        while (it1.hasNext()) {
                                            UploadServerBean b1 = it1.next();
                                            if (b1.getFile().equals(file.getName())) {
                                                it1.remove();
                                                break;
                                            }
                                        }
                                        it.remove();
                                    }
                                }
                                try {
                                    myApp.writeToFile(FilePath.FILE_UPLOAD_LIST, uploadList);
                                } catch (Exception e) {
                                    BaseApplication.writeErrorLog(e);
                                }
                                adapter.updateList(list);
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show());
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (tableListView.hasFocus() && tableListView.getSelectedItemPosition() >= 0 && tableListView.getSelectedItemPosition() < list.size()) {
                    list.get(tableListView.getSelectedItemPosition()).setSelected(true);
                    checkboxStatus();
                    adapter.updateList(list);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (tableListView.hasFocus() && tableListView.getSelectedItemPosition() >= 0 && tableListView.getSelectedItemPosition() < list.size()) {
                    list.get(tableListView.getSelectedItemPosition()).setSelected(false);
                    checkboxStatus();
                    adapter.updateList(list);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_STAR:
                if (0 == forceDelete % 2 && forceDelete < 4)
                    forceDelete++;
                else
                    forceDelete = 0;
                break;
            case KeyEvent.KEYCODE_POUND:
                if (1 == forceDelete % 2 && forceDelete < 4)
                    forceDelete++;
                else
                    forceDelete = 0;
                break;
            case KeyEvent.KEYCODE_0:
                for (ExplosionRecordBean bean : list)
                    bean.setSelected(!bean.isSelected());
                checkboxStatus();
                break;
            default:
                forceDelete = 0;
                break;
        }
        return false;
    }

    private void uploadRecord() {
        switch (settingBean.getServerHost()) {
            case 0:
                enterpriseDialog = new EnterpriseDialog(ExplosionRecordActivity.this);
                enterpriseDialog.setClickConfirm(view -> {
                    enterpriseDialog.dismiss();
                    if (-1 == getNextIndex()) {
                        showMessage(R.string.message_upload_all_success);
                    } else {
                        BaseApplication.writeFile(getString(R.string.button_upload) + ", " + ConstantUtils.UPLOAD_HOST[0][0] + ", " + list.get(uploadIndex).getRecordPath());
                        disableButton(true);
                        startUpload();
                    }
                });
                enterpriseDialog.setClickModify(view -> {
                    enterpriseDialog.dismiss();
                    disableButton(false);
                    startActivity(new Intent(ExplosionRecordActivity.this, EnterpriseActivity.class));
                });
                enterpriseDialog.show();
                break;
            case 2:
                if (pDialog.getProgress() > 0)
                    uploadServer(false);
                else {
                    enterpriseDialog = new EnterpriseDialog(ExplosionRecordActivity.this);
                    enterpriseDialog.setClickConfirm(view -> {
                        disableButton(true);
                        enterpriseDialog.dismiss();
                        uploadServer(false);
                    });
                    enterpriseDialog.setClickModify(view -> {
                        enterpriseDialog.dismiss();
                        pDialog.dismiss();
                        startActivity(new Intent(ExplosionRecordActivity.this, BaiSeDataActivity.class));
                    });
                    enterpriseDialog.show();
                }
                break;
            default:
                uploadServer(false);
        }
    }

    private void uploadBaiSe() {
        try {
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
            BaiSeUpload baiSeUpload = myApp.readBaiSeUpload();
            baiSeUpload.setLngLat(String.format(Locale.CHINA, "%f,%f", list.get(uploadIndex).getLng(), list.get(uploadIndex).getLat()));
            baiSeUpload.setGpsCoordinateSystems(ConstantUtils.GPS_SYSTEM);
            baiSeUpload.setDeviceNO(settingBean.getExploderID());
            baiSeUpload.setBurstTime(df.format(list.get(uploadIndex).getExplodeDate()));
            baiSeUpload.setDetonatorCount(list.get(uploadIndex).getAmount());
            myApp.saveBean(baiSeUpload);
            BaseApplication.writeFile(getString(R.string.button_upload) + ", " + ConstantUtils.UPLOAD_HOST[2][0] + ", " + list.get(uploadIndex).getRecordPath());
            BaseApplication.writeFile(new Gson().toJson(baiSeUpload));
            OkHttpUtils.postString().addHeader("access-token", ConstantUtils.ACCESS_TOKEN)
                    .url(ConstantUtils.BAI_SE_UPLOAD_URL)
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .content(new Gson().toJson(baiSeUpload))
                    .build().execute(new Callback<BaiSeUploadResult>() {
                        @Override
                        public BaiSeUploadResult parseNetworkResponse(Response response, int i) throws Exception {
                            ResponseBody body = response.body();
                            if (body != null) {
                                String string = body.string();
                                return new Gson().fromJson(string, BaiSeUploadResult.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            myApp.myToast(ExplosionRecordActivity.this, R.string.message_network_timeout);
                        }

                        @Override
                        public void onResponse(BaiSeUploadResult baiSeUploadResult, int i) {
                            if (baiSeUploadResult != null) {
                                if (baiSeUploadResult.isSuccess()) {
                                    BaiSeCheck baiSeCheck = myApp.readBaiSeCheck();
                                    baiSeCheck.setChecked(false);
                                    myApp.saveBean(baiSeCheck);
                                    uploadRecord();
                                } else if (baiSeUploadResult.getMessage() != null) {
                                    myApp.myToast(ExplosionRecordActivity.this, baiSeUploadResult.getMessage());
                                }
                            }
                        }
                    });

        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void uploadServer(boolean resend) {
        if (!resend && -1 == getNextIndex()) {
            if (successCount >= pDialog.getMax())
                showMessage(R.string.message_upload_all_success);
            else
                showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_upload_result), successCount, pDialog.getMax() - successCount));
            disableButton(false);
        } else {
            BaseApplication.writeFile(getString(R.string.button_upload) + ", " + ConstantUtils.UPLOAD_HOST[1][0] + ", " + list.get(uploadIndex).getRecordPath());
            receiveCount = 0;
            disableButton(true);
            msgHandler.sendEmptyMessageDelayed(4, ConstantUtils.UPLOAD_TIMEOUT);
            new Thread(() -> {
                try {
                    List<DetonatorInfoBean> detonators = new ArrayList<>();
                    myApp.readFromFile(list.get(uploadIndex).getRecordPath(), detonators, DetonatorInfoBean.class);
                    if (null == minaClient)
                        minaClient = new MinaClient();
                    minaClient.setDetonatorList(detonators);
                    minaClient.setExplodeTime(list.get(uploadIndex).getExplodeDate());
                    minaClient.setHandler(msgHandler);
                    minaClient.setHost(ConstantUtils.UPLOAD_HOST[settingBean.getServerHost()][1]);
                    minaClient.setLng(list.get(uploadIndex).getLng());
                    minaClient.setLat(list.get(uploadIndex).getLat());
                    String sn = settingBean.getExploderID();
                    minaClient.setSn(sn.substring(1, 5) + sn.substring(sn.length() - 4));
                    minaClient.uploadRecord();
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }).start();
        }
    }

    private void startUpload() {
        StringBuilder str = new StringBuilder();
        List<DetonatorInfoBean> detonators = new ArrayList<>();
        myApp.readFromFile(list.get(uploadIndex).getRecordPath(), detonators, DetonatorInfoBean.class);
        for (DetonatorInfoBean bean : detonators) {
            str.append(bean.getAddress()).append(",");
        }
        str.deleteCharAt(str.length() - 1);
        token = myApp.makeToken();
        Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_UPLOAD_RECORDS);
        if (null != params) {
            params.put("dsc", str.toString());
            params.put("dwdm", enterpriseBean.getCode());
            params.put("bprysfz", enterpriseBean.getId());
            SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
            params.put("bpsj", df.format(list.get(uploadIndex).getExplodeDate()));
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
                            showMessage(R.string.message_check_network);
                            disableButton(false);
                        }

                        @Override
                        public void onResponse(UploadExplodeRecordsBean uploadExplodeRecordsBean, int i) {
                            if (null != uploadExplodeRecordsBean) {
                                if (uploadExplodeRecordsBean.getToken().equals(token)) {
                                    if (uploadExplodeRecordsBean.isStatus()) {
                                        if (null != uploadExplodeRecordsBean.getResult()) {
                                            if (!uploadExplodeRecordsBean.getResult().isSuccess()) {
                                                showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_upload_fail_number), uploadIndex + 1));
                                            } else {
                                                moveFile();
                                            }
                                        }
                                    } else {
                                        showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_upload_fail_number), uploadIndex + 1)
                                                + uploadExplodeRecordsBean.getDescription());
                                    }
                                    if (-1 != getNextIndex()) {
                                        startUpload();
                                    } else {
                                        disableButton(false);
                                    }
                                } else {
                                    showMessage(R.string.message_token_error);
                                    disableButton(false);
                                }
                            } else {
                                showMessage(R.string.message_return_data_error);
                                disableButton(false);
                            }
                        }
                    });
        }
    }

    private void moveFile() {
        if (pDialog.getProgress() < pDialog.getMax()) {
            pDialog.incrementProgressBy(1);
        }
        File file = new File(list.get(uploadIndex).getRecordPath());
        if (file.exists()) {
            String oldFileName = file.getName();
            String newFileName = list.get(uploadIndex).getRecordPath().replace("N", "U");
            if (file.renameTo(new File(newFileName))) {
                showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_upload_success_number), uploadIndex + 1));
                try {
                    for (UploadServerBean bean : uploadList) {
                        if (bean.getFile().equals(oldFileName)) {
                            bean.setFile(oldFileName.replace("N", "U"));
                            if (settingBean.getServerHost() != 0) {
                                bean.setServer(ConstantUtils.UPLOAD_HOST[settingBean.getServerHost()][1]);
                            }
                            bean.setUploaded(true);
                            bean.setUploadTime(new Date());
                            bean.setUploadServer(false);
                            break;
                        }
                    }
                    myApp.writeToFile(FilePath.FILE_UPLOAD_LIST, uploadList);
                    myApp.uploadExplodeList();
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
                list.get(uploadIndex).setRecordPath(newFileName);
                list.get(uploadIndex).setUploaded(true);
            } else {
                showMessage(getResources().getString(R.string.message_file_not_found));
            }
        } else {
            showMessage(getResources().getString(R.string.message_file_not_found));
        }
        adapter.updateList(list);
    }

    private int getNextIndex() {
        for (int i = uploadIndex + 1; i < list.size(); i++)
            if (list.get(i).isSelected()) {
                uploadIndex = i;
                return i;
            }
        return -1;
    }

    private void disableButton(boolean b) {
        if (!b && null != pDialog && pDialog.isShowing())
            pDialog.dismiss();
        setProgressVisibility(b);
        findViewById(R.id.btn_upload).setEnabled(!b);
        findViewById(R.id.btn_delete).setEnabled(!b);
    }

    @Override
    public void finish() {
        if (!findViewById(R.id.btn_upload).isEnabled()) {
            runOnUiThread(() -> {
                BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_cancel_upload)
                        .setMessage(R.string.dialog_confirm_exit_upload)
                        .setPositiveButton(R.string.btn_confirm, (dialog1, which) -> ExplosionRecordActivity.super.finish())
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show());
            });
        } else
            super.finish();
    }

    @Override
    protected void onDestroy() {
        msgHandler.removeCallbacksAndMessages(null);
        checkExploderHandler.removeCallbacksAndMessages(null);
        if (null != pDialog && pDialog.isShowing())
            pDialog.dismiss();
        if (null != minaClient) {
            new Thread(() -> minaClient.closeConnect()).start();
        }
        super.onDestroy();
    }

    private void initData() {
        settingBean = BaseApplication.readSettings();
        if (0 == settingBean.getServerHost())
            enterpriseBean = myApp.readEnterprise();
        list = new ArrayList<>();
        try {
            File[] files = new File(FilePath.FILE_DETONATE_RECORDS + "/").listFiles();
            if (null != files) {
                Arrays.sort(files, (f1, f2) -> {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0)
                        return 1;
                    else if (diff == 0)
                        return 0;
                    else
                        return -1;
                });

                for (File file : files) {
                    SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
                    List<DetonatorInfoBean> temp = new ArrayList<>();
                    myApp.readFromFile(file.getAbsolutePath(), temp, DetonatorInfoBean.class);
                    String[] info = file.getName().split("_");
                    if (5 == info.length) {
                        info[4] = info[4].substring(0, info[4].length() - 4);
                        list.add(new ExplosionRecordBean(formatter.parse(info[1]), temp.size(), info[2].equals("U"), Double.parseDouble(info[3]), Double.parseDouble(info[4]), file.getAbsolutePath()));
                    }
                }
            }
            uploadList = new ArrayList<>();
            myApp.readFromFile(FilePath.FILE_UPLOAD_LIST, uploadList, UploadServerBean.class);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }
}
