package com.leon.detonator.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.baidu.mapapi.model.LatLng;
import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.CheckRegister;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Bean.EnterpriseBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Bean.UploadExplodeRecordsBean;
import com.leon.detonator.Bean.UploadServerBean;
import com.leon.detonator.Dialog.EnterpriseDialog;
import com.leon.detonator.Mina.client.MinaClient;
import com.leon.detonator.Mina.client.MinaHandler;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.KeyUtils;
import com.leon.detonator.Util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Response;

public class DetonateStep4Activity extends BaseActivity {
    private final int STEP_READ_VOLTAGE = 1,
            STEP_PROGRESS = 2,
            STEP_EXPLODE = 3,
            STEP_EXPLODE_2 = 4,
            STEP_UPLOAD_TIMEOUT = 5,
            STEP_CHECK_EXPLODER_ERROR = 6,
            STEP_CHECK_EXPLODER_SUCCESS = 7,
            STEP_MESSAGE = 8;

    private int explodeTime, countDown = 0, receiveCount = 0, soundTicktock, soundAlert;
    private boolean uniteExplode;
    private SoundPool soundPool;
    private SerialPortUtil serialPortUtil;
    private TextView tvExplode;
    private ProgressBar pbExplode;
    private MyButton btnUpload, btnExit;
    private LatLng latLng;
    private EnterpriseBean enterpriseBean;
    private EnterpriseDialog enterpriseDialog;
    private String token, recordFileName;
    private List<DetonatorInfoBean> list;
    private MinaClient minaClient;
    private LocalSettingBean settingBean;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detonate_step4);

        uniteExplode = getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_UNITE, false);
        if (uniteExplode)
            setTitle(R.string.start_explode, R.string.subtitle_unite);
        else
            setTitle(R.string.start_explode);
        setProgressVisibility(true);

        myApp = (BaseApplication) getApplication();
        initSound();
        settingBean = BaseApplication.readSettings();
        enterpriseBean = myApp.readEnterprise();
        latLng = new LatLng(getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0), getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
        try {
            list = new ArrayList<>();
            myApp.readFromFile(
                    FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1][ConstantUtils.LIST_TYPE.END.ordinal()],
                    list, DetonatorInfoBean.class);
            if (list.size() > 0) {
                Collections.sort(list);
                explodeTime = list.get(list.size() - 1).getDelayTime();
            } else {
                showMessage(R.string.message_empty_list);
                finish();
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }

        tvExplode = findViewById(R.id.tv_explode_percentage);
        pbExplode = findViewById(R.id.pb_explode);
        btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setEnabled(false);
        btnUpload.setOnClickListener(v -> function(KeyEvent.KEYCODE_1));
        btnExit = findViewById(R.id.btn_exit);
        btnExit.setEnabled(false);
        btnExit.setOnClickListener(v -> function(KeyEvent.KEYCODE_2));
        if (uniteExplode) {
            try {
                serialPortUtil = SerialPortUtil.getInstance();
                serialPortUtil.setOnDataReceiveListener(buffer -> {
                    String data = new String(buffer);
                    if (data.contains(SerialCommand.RESPOND_SUCCESS)) {
                        data = data.replace(SerialCommand.RESPOND_SUCCESS, "");
                        try {
                            if (data.contains(SerialCommand.RESPOND_VOLTAGE) && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_VOLTAGE)) {
                                final int vol = Integer.parseInt(data.substring(data.indexOf(SerialCommand.RESPOND_VOLTAGE) + SerialCommand.RESPOND_VOLTAGE.length(), data.indexOf("\r")));
                                setVoltage(vol / 100.0f);
                                serialPortUtil.sendCmd(SerialCommand.CMD_READ_CURRENT);
                            } else if (data.contains(SerialCommand.RESPOND_CURRENT) && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_CURRENT)) {
                                data = data.substring(data.indexOf(SerialCommand.RESPOND_CURRENT) + SerialCommand.RESPOND_CURRENT.length(), data.indexOf("\r"));
                                if (data.length() > 0) {
                                    try {
                                        int c = Integer.parseInt(data);
                                        int count;
                                        count = (int) ((c + 2) / 3.4f) + 1;
                                        float current = 0.0f;
                                        if (c == 1)
                                            current = 25;
                                        else if (c > 0 && count > 0)
                                            current = count * 25 + ((c + 2) - (count - 1) * 3.4f) / 3.4f;
                                        setCurrent(current);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                    }
                });
                delaySendCmdHandler.sendEmptyMessageDelayed(STEP_EXPLODE, 100);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        } else {
            delaySendCmdHandler.sendEmptyMessageDelayed(STEP_PROGRESS, 100);
        }
    }    private final Handler refreshProgressBar = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            refreshProgressBar.removeMessages(1);
            if (countDown < explodeTime / 100) {
                countDown++;
                int percent = countDown * ConstantUtils.UPLOAD_TIMEOUT / explodeTime;
                tvExplode.setText(String.format(Locale.CHINA, "%d%%", percent));
                pbExplode.setProgress(percent);
                refreshProgressBar.sendEmptyMessageDelayed(1, 100);
            } else {
                tvExplode.setText(String.format(Locale.CHINA, "%d%%", 100));
                pbExplode.setProgress(100);
                if (soundTicktock > 0)
                    soundPool.stop(soundTicktock);
                showMessage(R.string.message_explode_success);
                moveFile();
                //delaySendCmdHandler.sendEmptyMessage(STEP_READ_VOLTAGE);
                setProgressVisibility(false);
                btnExit.setEnabled(true);
                btnUpload.setEnabled(true);
            }
            return false;
        }
    });

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundTicktock = soundPool.load(this, R.raw.ticktock, 1);
            soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                if (sampleId == soundTicktock && pbExplode.getProgress() < 100) {
                    myApp.playSound(soundPool, soundTicktock, -1);
                }
            });
            if (0 == soundTicktock)
                myApp.myToast(this, R.string.message_media_load_error);
            soundAlert = soundPool.load(this, R.raw.alert, 1);
            if (0 == soundAlert)
                myApp.myToast(this, R.string.message_media_load_error);
        } else
            myApp.myToast(this, R.string.message_media_init_error);
    }    private final Handler delaySendCmdHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case STEP_READ_VOLTAGE:
                    serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_READ_VOLTAGE, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                    break;
                case STEP_PROGRESS:
                    BaseApplication.writeFile("进入起爆界面");
                    if (!uniteExplode)
                        countDown = (int) ((System.currentTimeMillis() - getIntent().getLongExtra(KeyUtils.KEY_EXPLODE_ELAPSED, 0)) / 100);
                    else if (serialPortUtil != null) {
                        serialPortUtil.closeSerialPort();
                        serialPortUtil = null;
                    }
                    refreshProgressBar.sendEmptyMessage(1);
                    break;
                case STEP_EXPLODE:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.NEW_EXPLODE, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_EXPLODE_2, settingBean.isNewLG() ? 200 : 500);
                    break;
                case STEP_EXPLODE_2:
                    serialPortUtil.sendCmd("", SerialCommand.ACTION_TYPE.NEW_EXPLODE, 0);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_PROGRESS, 100);
                    break;
                case STEP_CHECK_EXPLODER_ERROR:
                    disableButton(false);
                    break;
                case STEP_CHECK_EXPLODER_SUCCESS:
                    settingBean = BaseApplication.readSettings();
                    if (!settingBean.isRegistered() || null == settingBean.getExploderID() || settingBean.getExploderID().isEmpty()) {
                        showMessage(R.string.message_not_registered);
                    } else if (0 == settingBean.getServerHost() && (null == enterpriseBean || enterpriseBean.getCode().isEmpty())) {
                        showMessage(R.string.message_fill_enterprise);
                        startActivity(new Intent(DetonateStep4Activity.this, EnterpriseActivity.class));
                    } else {
                        new AlertDialog.Builder(DetonateStep4Activity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_upload)
                                .setMessage(String.format(Locale.CHINA, getResources().getString(R.string.dialog_confirm_upload), ConstantUtils.UPLOAD_HOST[settingBean.getServerHost()][0]))
                                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> uploadRecord())
                                .setNegativeButton(R.string.btn_cancel, null)
                                .create().show();
                    }
                    break;
                case MinaHandler.MINA_DATA:
                    delaySendCmdHandler.removeMessages(STEP_UPLOAD_TIMEOUT);
                    if (((String) msg.obj).contains("R")) {
                        uploadRecord();
                    } else {
                        if (((String) msg.obj).startsWith("#") && ((String) msg.obj).endsWith("$"))
                            receiveCount++;
                        if (receiveCount >= 3) {
                            renameRecordFile();
                            myApp.myToast(DetonateStep4Activity.this, R.string.message_upload_success);
                            btnExit.setEnabled(true);
                            finish();
                            break;
                        }
                    }
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_UPLOAD_TIMEOUT, ConstantUtils.UPLOAD_TIMEOUT);
                    break;
                case MinaHandler.MINA_NORMAL:
                    delaySendCmdHandler.removeMessages(STEP_UPLOAD_TIMEOUT);
                    myApp.myToast(DetonateStep4Activity.this, (String) msg.obj);
                    delaySendCmdHandler.sendEmptyMessageDelayed(STEP_UPLOAD_TIMEOUT, ConstantUtils.UPLOAD_TIMEOUT);
                    break;
                case MinaHandler.MINA_ERROR:
                    delaySendCmdHandler.removeMessages(STEP_UPLOAD_TIMEOUT);
                    disableButton(false);
                    myApp.myToast(DetonateStep4Activity.this, (String) msg.obj);
                    if (null != minaClient) {
                        new Thread(() -> minaClient.closeConnect()).start();
                    }
                    break;
                case STEP_UPLOAD_TIMEOUT:
                    disableButton(false);
                    myApp.myToast(DetonateStep4Activity.this, R.string.message_network_timeout);
                    break;
                case STEP_MESSAGE:
                    myApp.myToast(DetonateStep4Activity.this, (String) msg.obj);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private void function(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                if (!BaseApplication.readSettings().isRegistered()) {
                    myApp.registerExploder();
                    disableButton(true);
                    new CheckRegister() {
                        @Override
                        public void onError() {
                            delaySendCmdHandler.sendEmptyMessage(STEP_CHECK_EXPLODER_ERROR);
                        }

                        @Override
                        public void onSuccess() {
                            delaySendCmdHandler.sendEmptyMessage(STEP_CHECK_EXPLODER_SUCCESS);
                        }
                    }.setActivity(this).start();
                } else if (btnUpload.isEnabled())
                    delaySendCmdHandler.sendEmptyMessage(STEP_CHECK_EXPLODER_SUCCESS);
                break;
            case KeyEvent.KEYCODE_2:
                finish();
                break;
        }
    }

    private void showMessage(String s) {
        Message m = delaySendCmdHandler.obtainMessage(STEP_MESSAGE);
        m.obj = s;
        delaySendCmdHandler.sendMessage(m);
    }

    private void showMessage(@StringRes int s) {
        Message m = delaySendCmdHandler.obtainMessage(STEP_MESSAGE);
        m.obj = getResources().getString(s);
        delaySendCmdHandler.sendMessage(m);
    }

    private void uploadRecord() {
        if (0 != settingBean.getServerHost()) {
            BaseApplication.writeFile("上传当前爆破记录!");
            receiveCount = 0;
            disableButton(true);
            delaySendCmdHandler.sendEmptyMessageDelayed(STEP_UPLOAD_TIMEOUT, ConstantUtils.UPLOAD_TIMEOUT);
            new Thread(() -> {
                if (null == minaClient)
                    minaClient = new MinaClient();
                minaClient.setDetonatorList(list);
                minaClient.setExplodeTime(new Date());
                minaClient.setHandler(delaySendCmdHandler);
                minaClient.setLng(latLng.longitude);
                minaClient.setLat(latLng.latitude);
                minaClient.setHost(ConstantUtils.UPLOAD_HOST[settingBean.getServerHost()][1]);
                String sn = BaseApplication.readSettings().getExploderID();
                minaClient.setSn(sn.substring(1, 5) + sn.substring(sn.length() - 4));
                minaClient.uploadRecord();
            }).start();
        } else {
            enterpriseDialog = new EnterpriseDialog(this, enterpriseBean);
            enterpriseDialog.setClickConfirm(view -> {
                disableButton(true);
                enterpriseDialog.dismiss();
                StringBuilder str = new StringBuilder();
                for (DetonatorInfoBean bean : list) {
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
                    params.put("bpsj", df.format(new Date()));
                    if (null != latLng) {
                        params.put("jd", latLng.longitude + "");
                        params.put("wd", latLng.latitude + "");
                    }
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
                                    disableButton(false);
                                    if (null != uploadExplodeRecordsBean) {
                                        if (uploadExplodeRecordsBean.getToken().equals(token)) {
                                            if (uploadExplodeRecordsBean.isStatus()) {
                                                if (null != uploadExplodeRecordsBean.getResult()) {
                                                    if (uploadExplodeRecordsBean.getResult().isSuccess()) {
                                                        renameRecordFile();
                                                        finish();
                                                    } else {
                                                        showMessage(R.string.message_upload_fail);
                                                        disableButton(false);
                                                    }
                                                }
                                            } else {
                                                showMessage(uploadExplodeRecordsBean.getDescription());
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
            });
            enterpriseDialog.setClickModify(view -> {
                enterpriseDialog.dismiss();
                startActivity(new Intent(DetonateStep4Activity.this, EnterpriseActivity.class));
            });
            enterpriseDialog.show();
        }
    }

    private void renameRecordFile() {
        File file = new File(FilePath.FILE_DETONATE_RECORDS + "/" + recordFileName);
        if (!file.exists()) {
            showMessage(R.string.message_file_not_found);
            return;
        } else if (!file.renameTo(new File(FilePath.FILE_DETONATE_RECORDS + "/" + recordFileName.replace("N", "U")))) {
            showMessage(R.string.message_copy_file_fail);
            return;
        }
        List<UploadServerBean> uploadList = new ArrayList<>();
        myApp.readFromFile(FilePath.FILE_UPLOAD_LIST, uploadList, UploadServerBean.class);
        for (UploadServerBean bean : uploadList) {
            if (bean.getFile().equals(recordFileName)) {
                bean.setFile(recordFileName.replace("N", "U"));
                if (settingBean.getServerHost() != 0) {
                    bean.setServer(ConstantUtils.UPLOAD_HOST[settingBean.getServerHost()][1]);
                }
                bean.setUploaded(true);
                bean.setUploadTime(new Date());
                bean.setUploadServer(false);
                break;
            }
        }
        try {
            myApp.writeToFile(FilePath.FILE_UPLOAD_LIST, uploadList);
            myApp.uploadExplodeList();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void moveFile() {
        File file = new File(FilePath.FILE_DETONATE_RECORDS);
        if ((!file.exists() && !file.mkdir()) || (file.exists() && !file.isDirectory() && file.delete() && !file.mkdir())) {
            showMessage(R.string.message_create_folder_fail);
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
        List<DetonatorInfoBean> listAll = new ArrayList<>();
        String[] fileList = FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1];
        myApp.readFromFile(myApp.getListFile(), listAll, DetonatorInfoBean.class);
        file = new File(fileList[ConstantUtils.LIST_TYPE.END.ordinal()]);
        File newFile = new File(FilePath.FILE_DETONATE_RECORDS +
                (myApp.isTunnel() ? "/T_" : "/O_")
                + df.format(new Date()) + "_N_" + String.format(Locale.CHINA, "%.6f_%.6f", latLng.latitude, latLng.longitude) + ".rec");
        if (file.exists() && !file.renameTo(newFile)) {
            showMessage(R.string.message_copy_file_fail);
            return;
        }
        recordFileName = newFile.getName();
        List<UploadServerBean> uploadList = new ArrayList<>();
        try {
            myApp.readFromFile(FilePath.FILE_UPLOAD_LIST, uploadList, UploadServerBean.class);
            UploadServerBean bean = new UploadServerBean();
            bean.setFile(newFile.getName());
            uploadList.add(bean);
            myApp.writeToFile(FilePath.FILE_UPLOAD_LIST, uploadList);
            //myApp.uploadExplodeList();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }

        if (listAll.size() == list.size()) {
            file = new File(myApp.getListFile());
            if (file.exists() && !file.delete()) {
                showMessage(String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
            }
        } else {
            Iterator<DetonatorInfoBean> it = listAll.iterator();
            while (it.hasNext()) {
                DetonatorInfoBean bean1 = it.next();
                for (DetonatorInfoBean bean2 : list)
                    if (bean1.getAddress().equals(bean2.getAddress())) {
                        it.remove();
                        break;
                    }
            }
            try {
                myApp.writeToFile(myApp.getListFile(), listAll);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        try {
            for (int i = ConstantUtils.LIST_TYPE.ALL.ordinal() + 1; i <= ConstantUtils.LIST_TYPE.END.ordinal(); i++) {
                file = new File(fileList[i]);
                if (file.exists() && !file.delete())
                    myApp.myToast(DetonateStep4Activity.this, String.format(Locale.CHINA, getResources().getString(R.string.message_delete_file_fail), file.getName()));
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void disableButton(boolean b) {
        setProgressVisibility(b);
        btnExit.setEnabled(!b);
        btnUpload.setEnabled(!b);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        function(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void finish() {
        if (btnExit.isEnabled())
            super.finish();
    }

    private void closeAllHandler() {
        refreshProgressBar.removeCallbacksAndMessages(null);
        delaySendCmdHandler.removeCallbacksAndMessages(null);
        if (null != minaClient) {
            new Thread(() -> minaClient.closeConnect()).start();
        }

        if (serialPortUtil != null) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
    }

    @Override
    protected void onDestroy() {
        closeAllHandler();
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundTicktock);
            soundPool.unload(soundAlert);
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }




}
