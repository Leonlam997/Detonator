package com.leon.detonator.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.UploadExplodeRecord;
import com.leon.detonator.bean.DanLinDetonatorBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetonateStep4Activity extends BaseActivity {
    private List<ExplosionRecordBean> explosionRecordList;
    private List<DetonatorBean> list;
    private SerialPortUtil serialPortUtil;
    private SoundPool soundPool;
    private ProgressBar pbExplode;
    private TextView tvExplode;
    private MyButton btnUpload;
    private MyButton btnExit;
    private final int STEP_PROGRESS = 2;
    private final int STEP_EXPLODE = 3;
    private boolean uniteExplode;
    private int soundTicktock;
    private int explodeTime;
    private int countDown;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (UploadExplodeRecord.uploading)
            if (Activity.RESULT_OK == result.getResultCode())
                UploadExplodeRecord.myHandler.sendEmptyMessage(UploadExplodeRecord.HANDLER_SUCCESS);
            else
                UploadExplodeRecord.myHandler.sendEmptyMessage(UploadExplodeRecord.HANDLER_FAIL);
    });

    private final Handler myHandler = new Handler(msg -> {
        final int STEP_REFRESH = 1;
        switch (msg.what) {
            case STEP_REFRESH:
                msg.getTarget().removeMessages(STEP_REFRESH);
                if (countDown < explodeTime / 100) {
                    countDown++;
                    int percent = countDown * ConstantUtils.UPLOAD_TIMEOUT / explodeTime;
                    tvExplode.setText(String.format(Locale.getDefault(), "%d%%", percent));
                    pbExplode.setProgress(percent);
                    msg.getTarget().sendEmptyMessageDelayed(STEP_REFRESH, 100);
                } else {
                    tvExplode.setText(String.format(Locale.getDefault(), "%d%%", 100));
                    pbExplode.setProgress(100);
                    if (soundTicktock > 0)
                        soundPool.stop(soundTicktock);
                    myApp.myToast(DetonateStep4Activity.this, R.string.message_explode_success);
                    enabledButton(true);
                }
                break;
            case STEP_PROGRESS:
                if (!uniteExplode)
                    countDown = (int) ((System.currentTimeMillis() - getIntent().getLongExtra(KeyUtils.KEY_EXPLODE_ELAPSED, System.currentTimeMillis())) / 100);
                else if (serialPortUtil != null) {
                    serialPortUtil.closeSerialPort();
                    serialPortUtil = null;
                }
                msg.getTarget().sendEmptyMessage(STEP_REFRESH);
                break;
            case STEP_EXPLODE:
                serialPortUtil.sendCmd("", SerialCommand.CODE_EXPLODE, 0, 0);
                msg.getTarget().sendEmptyMessageDelayed(STEP_PROGRESS, 100);
                break;
            case UploadExplodeRecord.HANDLER_SUCCESS:
                if (msg.obj == null) {
                    myApp.myToast(DetonateStep4Activity.this, R.string.message_upload_success);
                    enabledButton(true);
                }
                break;
            case UploadExplodeRecord.HANDLER_FAIL:
                if (msg.obj == null)
                    myApp.myToast(DetonateStep4Activity.this, R.string.message_upload_fail);
                else if ((int) msg.obj != -1)
                    break;
                enabledButton(true);
                break;
            default:
                break;
        }
        return false;
    });

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
        BaseApplication.settings.setUploadedLog(false);
        BaseApplication.saveSettings();
        saveExplodeRecord();
        list = getIntent().getParcelableArrayListExtra(KeyUtils.KEY_LIST);
        explodeTime = maxDelay();
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
                serialPortUtil = SerialPortUtil.getInstance(this);
                serialPortUtil.setOnDataReceiveListener(buffer -> {
                    if (serialPortUtil.checkData(buffer)) {
                        if (buffer[SerialCommand.CODE_CHAR_AT] == SerialCommand.CODE_MEASURE_VALUE) {
                            setCurrent(Float.intBitsToFloat((int) Long.parseLong(new String(Arrays.copyOfRange(buffer, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 6)), 16)));
                            setVoltage(22);
                        }
                    }
                });
                myHandler.sendEmptyMessageDelayed(STEP_EXPLODE, 100);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        } else {
            myHandler.sendEmptyMessageDelayed(STEP_PROGRESS, 100);
        }
    }

    private void saveExplodeRecord() {
        new Thread(() -> {
            long[] schemeIds = DbUtil.getSelectedSchemeId();
            explosionRecordList = new ArrayList<>();
            for (long schemeId : schemeIds) {
                ExplosionRecordBean bean = DbUtil.getExplosionRecord(schemeId);
                bean.setLng(getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                bean.setLat(getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                bean.setExplodeTime(new Date());
                bean.setUploadServer(BaseApplication.settings.getServerHost());
                explosionRecordList.add(bean);
            }
            DbUtil.updateExplosionRecordList(explosionRecordList);
            if (isDanLin()) {
                List<DetonatorBean> authList = DbUtil.getAuthDetonatorList();
                DanLinDetonatorBean bean = DbUtil.getDanLinDownloadDetonator(!getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
                for (DetonatorBean bean1 : list) {
                    authList.removeIf(detonatorBean -> detonatorBean.equals(bean1));
                    for (LgBean lgBean : bean.getResult().getLgs().getLg())
                        if (lgBean.getFbh().equals(bean1.getAddress())) {
                            bean1.setUID(lgBean.getUid());
                            break;
                        }
                }
                DbUtil.updateDetonatorList(list);
                DbUtil.updateAuthDetonatorList(authList);
                DbUtil.setDanLinRecord(bean.getId(), list);
                if (authList.size() == 0 || bean.getResult().getLgs().getLg().size() == list.size())
                    DbUtil.setDanLinUsed(bean.getId());
            }
        }).start();
    }

    private int maxDelay() {
        int result = 0;
        for (DetonatorBean bean : list)
            result = Math.max(result, bean.getDelayTime());
        return result;
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundTicktock = soundPool.load(this, R.raw.ticktock, 1);
            soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                if (sampleId == soundTicktock && pbExplode.getProgress() < 100)
                    myApp.playSound(soundPool, soundTicktock, -1);
            });
        }
    }

    private void function(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                if (btnUpload.isEnabled())
                    if (!UploadExplodeRecord.uploading) {
                        enabledButton(false);
                        new UploadExplodeRecord(DetonateStep4Activity.this, explosionRecordList, myHandler, launcher).start();
                    } else
                        myApp.myToast(DetonateStep4Activity.this, R.string.progress_upload);
                break;
            case KeyEvent.KEYCODE_2:
                if (btnExit.isEnabled())
                    finish();
                break;
        }
    }

    private void enabledButton(boolean b) {
        setProgressVisibility(!b);
        btnExit.setEnabled(b);
        btnUpload.setEnabled(b);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        function(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void finish() {
        if (UploadExplodeRecord.uploading) {
            runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(DetonateStep4Activity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_cancel_upload)
                    .setMessage(R.string.dialog_confirm_exit_upload)
                    .setPositiveButton(R.string.button_confirm, (dialog1, which) -> DetonateStep4Activity.super.finish())
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()));
        } else
            super.finish();
    }

    @Override
    protected void onDestroy() {
        if (UploadExplodeRecord.uploading)
            UploadExplodeRecord.uploading = false;
        myHandler.removeCallbacksAndMessages(null);
        if (serialPortUtil != null) {
            serialPortUtil.closeSerialPort();
            serialPortUtil = null;
        }
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundTicktock);
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }
}
