package com.leon.detonator.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.leon.detonator.R;
import com.leon.detonator.adapter.ExplosionRecordAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.SynchronizeExplodeRecord;
import com.leon.detonator.base.UploadExplodeRecord;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ExplosionRecordActivity extends BaseActivity {
    private ListView listView;
    private ExplosionRecordAdapter adapter;
    private List<ExplosionRecordBean> list;
    private List<ExplosionRecordBean> selectedList;
    private MyButton btnDelete;
    private MyButton btnUpload;
    private MyButton btnDetail;
    private int successCount;
    private int forceDelete;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (UploadExplodeRecord.uploading)
            if (Activity.RESULT_OK == result.getResultCode())
                UploadExplodeRecord.myHandler.obtainMessage(UploadExplodeRecord.HANDLER_SUCCESS).sendToTarget();
            else
                UploadExplodeRecord.myHandler.obtainMessage(UploadExplodeRecord.HANDLER_FAIL).sendToTarget();
    });

    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case UploadExplodeRecord.HANDLER_SUCCESS:
                if (msg.obj != null) {
                    for (ExplosionRecordBean bean : list)
                        if (bean.getId() == (long) msg.obj) {
                            myApp.myToast(ExplosionRecordActivity.this, String.format(Locale.getDefault(), getString(R.string.message_upload_success_number), bean.getName()));
                            break;
                        }
                    successCount++;
                    adapter.updateList(list);
                    if (!SynchronizeExplodeRecord.uploading)
                        new SynchronizeExplodeRecord(myApp).start();
                } else {
                    if (successCount >= selectedList.size())
                        myApp.myToast(ExplosionRecordActivity.this, R.string.message_upload_all_success);
                    else
                        myApp.myToast(ExplosionRecordActivity.this, String.format(Locale.getDefault(), getString(R.string.message_upload_result), successCount, selectedList.size() - successCount));
                    enabledButton(true);
                }
                break;
            case UploadExplodeRecord.HANDLER_FAIL:
                if (msg.obj == null)
                    enabledButton(true);
                else {
                    for (ExplosionRecordBean bean : list)
                        if (bean.getId() == (long) msg.obj) {
                            myApp.myToast(ExplosionRecordActivity.this, String.format(Locale.getDefault(), getString(R.string.message_upload_fail_number), bean.getName()));
                            break;
                        }
                }
                break;
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explode_record);
        setTitle(R.string.detonate_rec);
        myApp = (BaseApplication) getApplication();
        list = DbUtil.getExplosionRecordList();
        if (list.size() > 0)
            list.removeIf(bean -> BaseApplication.settings.isTunnel() != bean.isTunnel() || bean.isDeleted());
        listView = findViewById(R.id.lv_record_list);
        adapter = new ExplosionRecordAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            list.get(position).setSelected(!list.get(position).isSelected());
            adapter.updateList(list);
            enabledButton(true);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            detailList(position);
            return false;
        });
        listView.requestFocus();
        btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(v -> launchWhich(ConstantUtils.KEYCODE_ADD));
        btnDetail = findViewById(R.id.btn_detail);
        btnDetail.setOnClickListener(v -> launchWhich(KeyEvent.KEYCODE_TAB));
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> launchWhich(ConstantUtils.KEYCODE_SUB));
        btnUpload.setEnabled(false);
        btnDelete.setEnabled(false);
        btnDetail.setEnabled(false);
    }

    private void detailList(int i) {
        Intent intent = new Intent();
        intent.setClass(ExplosionRecordActivity.this, DetonatorListActivity.class);
        intent.putExtra(KeyUtils.KEY_DETONATOR_LIST, ConstantUtils.HISTORY_LIST);
        intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, list.get(i).getLat());
        intent.putExtra(KeyUtils.KEY_TABLE_ID, list.get(i).getId());
        intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, list.get(i).getLng());
        startActivity(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && launchWhich(event.getKeyCode()))
            return true;
        else if (event.getAction() == KeyEvent.ACTION_DOWN)
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                case KeyEvent.KEYCODE_TAB:
                case ConstantUtils.KEYCODE_SUB:
                    return true;
            }
        return super.dispatchKeyEvent(event);
    }

    private boolean launchWhich(int which) {
        switch (which) {
            case ConstantUtils.KEYCODE_ADD:
                forceDelete = 0;
                if (btnUpload.isEnabled())
                    if (!UploadExplodeRecord.uploading) {
                        successCount = 0;
                        selectedList = new ArrayList<>();
                        for (ExplosionRecordBean b : list)
                            if (b.isSelected())
                                selectedList.add(b);
                        if (0 == selectedList.size())
                            myApp.myToast(ExplosionRecordActivity.this, R.string.message_no_select_record);
                        else {
                            enabledButton(false);
                            new UploadExplodeRecord(ExplosionRecordActivity.this, selectedList, myHandler, launcher).start();
                        }
                    } else
                        myApp.myToast(ExplosionRecordActivity.this, R.string.progress_upload);
                return true;
            case KeyEvent.KEYCODE_TAB:
                forceDelete = 0;
                if (btnDetail.isEnabled()) {
                    for (int i = 0; i < list.size(); i++)
                        if (list.get(i).isSelected()) {
                            detailList(i);
                            break;
                        }
                }
                return true;
            case ConstantUtils.KEYCODE_SUB:
                if (btnDelete.isEnabled()) {
                    boolean canDelete = false;
                    for (ExplosionRecordBean bean : list) {
                        if (bean.isSelected()) {
                            canDelete = true;
                            break;
                        }
                    }
                    if (!canDelete) {
                        myApp.myToast(ExplosionRecordActivity.this, R.string.message_no_select_record);
                        break;
                    }
                    if (forceDelete != 4) {
                        for (ExplosionRecordBean bean : list) {
                            if (bean.isSelected() && bean.getUploadTime() == null) {
                                myApp.myToast(ExplosionRecordActivity.this, R.string.message_cannot_delete_not_upload);
                                canDelete = false;
                                break;
                            }
                        }
                    }
                    forceDelete = 0;
                    if (canDelete)
                        BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_delete_record)
                                .setMessage(R.string.dialog_confirm_delete_record)
                                .setPositiveButton(R.string.button_confirm, (dialog1, which1) -> {
                                    Iterator<ExplosionRecordBean> it = list.iterator();
                                    List<Long> ids = new ArrayList<>();
                                    while (it.hasNext()) {
                                        ExplosionRecordBean b = it.next();
                                        if (b.isSelected()) {
                                            BaseApplication.writeFile(getString(R.string.dialog_title_delete_record) + ", " + b.getName());
                                            ids.add(b.getId());
                                            it.remove();
                                        }
                                    }
                                    if (ids.size() > 0)
                                        DbUtil.deleteRecord(ids);
                                    adapter.updateList(list);
                                })
                                .setNegativeButton(R.string.button_cancel, null)
                                .show());
                }
                return true;
            case KeyEvent.KEYCODE_2:
                forceDelete = 0;
                listView.requestFocus();
                if (listView.getSelectedItemPosition() > 0)
                    listView.setSelection(listView.getSelectedItemPosition() - 1);
                else
                    listView.setSelection(0);
                break;
            case KeyEvent.KEYCODE_8:
                forceDelete = 0;
                listView.requestFocus();
                if (listView.getSelectedItemPosition() < list.size())
                    listView.setSelection(listView.getSelectedItemPosition() + 1);
                else
                    listView.setSelection(list.size() - 1);
                break;
            case KeyEvent.KEYCODE_F1:
                if (0 == forceDelete % 2 && forceDelete < 4)
                    forceDelete++;
                else
                    forceDelete = 0;
                break;
            case KeyEvent.KEYCODE_F2:
                if (1 == forceDelete % 2 && forceDelete < 4)
                    forceDelete++;
                else
                    forceDelete = 0;
                break;
            case KeyEvent.KEYCODE_0:
                for (ExplosionRecordBean bean : list)
                    bean.setSelected(!bean.isSelected());
                break;
            default:
                forceDelete = 0;
                break;
        }
        return false;
    }

    private void enabledButton(boolean b) {
        runOnUiThread(() -> {
            setProgressVisibility(!b);
            if (!b) {
                btnUpload.setEnabled(false);
                btnDelete.setEnabled(false);
                btnDetail.setEnabled(false);
            } else {
                boolean noSelect = true;
                btnUpload.setEnabled(false);
                btnDelete.setEnabled(false);
                btnDetail.setEnabled(false);
                for (ExplosionRecordBean bean : list)
                    if (bean.isSelected()) {
                        if (noSelect) {
                            btnUpload.setEnabled(true);
                            btnDelete.setEnabled(true);
                            btnDetail.setEnabled(true);
                            noSelect = false;
                        } else {
                            btnDetail.setEnabled(false);
                            break;
                        }
                    }
            }
        });
    }

    @Override
    public void finish() {
        if (UploadExplodeRecord.uploading) {
            runOnUiThread(() -> {
                BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_cancel_upload)
                        .setMessage(R.string.dialog_confirm_exit_upload)
                        .setPositiveButton(R.string.button_confirm, (dialog1, which) -> ExplosionRecordActivity.super.finish())
                        .setNegativeButton(R.string.button_cancel, null)
                        .show());
            });
        } else
            super.finish();
    }

    @Override
    protected void onDestroy() {
        if (UploadExplodeRecord.uploading)
            UploadExplodeRecord.uploading = false;
        myHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
