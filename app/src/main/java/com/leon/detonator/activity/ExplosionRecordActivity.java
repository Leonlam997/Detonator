package com.leon.detonator.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.leon.detonator.R;
import com.leon.detonator.adapter.ExplosionRecordAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.UploadExplodeRecord;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ExplosionRecordActivity extends BaseActivity {
    private List<ExplosionRecordBean> selectedList;
    private List<ExplosionRecordBean> list;
    private ExplosionRecordAdapter adapter;
    private MyProgressDialog pDialog;
    private CheckBox cbSelected;
    private MyButton btnUpload;
    private MyButton btnImport;
    private MyButton btnDelete;
    private ListView listView;
    private int successCount;
    private int forceDelete;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (UploadExplodeRecord.uploading)
            if (Activity.RESULT_OK == result.getResultCode())
                UploadExplodeRecord.myHandler.sendEmptyMessage(UploadExplodeRecord.HANDLER_SUCCESS);
            else
                UploadExplodeRecord.myHandler.sendEmptyMessage(UploadExplodeRecord.HANDLER_FAIL);
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
                    pDialog.incrementProgressBy(1);
                } else {
                    if (successCount >= selectedList.size())
                        myApp.myToast(ExplosionRecordActivity.this, R.string.message_upload_all_success);
                    else
                        myApp.myToast(ExplosionRecordActivity.this, String.format(Locale.getDefault(), getString(R.string.message_upload_result), successCount, selectedList.size() - successCount));
                    enableButton(true);
                }
                break;
            case UploadExplodeRecord.HANDLER_FAIL:
                if (msg.obj == null || (int) msg.obj == -1)
                    enableButton(true);
                else {
                    pDialog.incrementProgressBy(1);
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
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        list = DbUtil.getExplosionRecordList();
        listView = findViewById(R.id.lv_record_list);
        adapter = new ExplosionRecordAdapter(this, list);
        listView.setAdapter(adapter);
        btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(v -> launchWhich(KeyEvent.KEYCODE_1));
        btnImport = findViewById(R.id.btn_restore);
        btnImport.setOnClickListener(v -> launchWhich(KeyEvent.KEYCODE_2));
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> launchWhich(KeyEvent.KEYCODE_3));
        cbSelected = findViewById(R.id.cb_selected);
        cbSelected.setClickable(true);
        cbSelected.setOnClickListener(v -> {
            if (list.size() > 0) {
                for (ExplosionRecordBean bean : list)
                    bean.setSelected(cbSelected.isChecked());
                adapter.updateList(list);
                btnUpload.setEnabled(cbSelected.isChecked());
                btnImport.setEnabled(cbSelected.isChecked());
                btnDelete.setEnabled(cbSelected.isChecked());
            } else
                cbSelected.setChecked(false);
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            list.get(position).setSelected(!list.get(position).isSelected());
            checkboxStatus();
            enableButton(true);
            adapter.updateList(list);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Intent intent = new Intent();
            intent.setClass(ExplosionRecordActivity.this, DetonatorListActivity.class);
            intent.putExtra(KeyUtils.KEY_CREATE_DELAY_LIST, ConstantUtils.HISTORY_LIST);
            intent.putExtra(KeyUtils.KEY_TABLE_ID, list.get(position).getId());
            intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, list.get(position).getLat());
            intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, list.get(position).getLng());
            startActivity(intent);
            return false;
        });
        listView.requestFocus();
        for (ExplosionRecordBean bean : list)
            bean.setSelected(bean.getUploadServer() == -1);
        checkboxStatus();
        enableButton(true);
    }

    private void checkboxStatus() {
        cbSelected.setChecked(true);
        for (ExplosionRecordBean bean : list) {
            if (!bean.isSelected()) {
                cbSelected.setChecked(false);
                break;
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launchWhich(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void launchWhich(int which) {
        switch (which) {
            case KeyEvent.KEYCODE_1:
                successCount = 0;
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
                        else
                            runOnUiThread(() -> {
                                enableButton(false);
                                pDialog = new MyProgressDialog(ExplosionRecordActivity.this);
                                pDialog.setInverseBackgroundForced(false);
                                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                pDialog.setTitle(R.string.progress_title);
                                pDialog.setMessage(getString(R.string.progress_upload));
                                pDialog.setMax(selectedList.size());
                                pDialog.setProgress(0);
                                pDialog.show();
                                new UploadExplodeRecord(ExplosionRecordActivity.this, selectedList, myHandler, launcher).start();
                            });
                    } else
                        myApp.myToast(ExplosionRecordActivity.this, R.string.progress_upload);
                break;
            case KeyEvent.KEYCODE_2:
                if (btnImport.isEnabled()) {
                    int i = 0;
                    for (ExplosionRecordBean bean : list)
                        if (bean.isSelected())
                            i++;
                    if (i > 0) {
                        int finalI = i;
                        BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_restore)
                                .setMessage(String.format(Locale.getDefault(), getString(R.string.dialog_confirm_import), i))
                                .setPositiveButton(R.string.button_confirm, (dialog1, which1) -> {
                                    enableButton(false);
                                    pDialog = new MyProgressDialog(ExplosionRecordActivity.this);
                                    pDialog.setInverseBackgroundForced(false);
                                    pDialog.setCancelable(false);
                                    pDialog.setCanceledOnTouchOutside(false);
                                    pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    pDialog.setTitle(R.string.progress_title);
                                    pDialog.setMessage(getString(R.string.progress_import));
                                    pDialog.setMax(finalI);
                                    pDialog.setProgress(0);
                                    pDialog.show();
                                    new Thread(() -> {
                                        for (ExplosionRecordBean bean : list)
                                            if (bean.isSelected()) {
                                                SchemeBean schemeBean = new SchemeBean();
                                                schemeBean.setName(bean.getName());
                                                schemeBean.setCreateTime(new Date());
                                                schemeBean.setId(-1);
                                                DbUtil.updateScheme(schemeBean);
                                                List<DetonatorBean> detonatorBeanList = DbUtil.getDetonatorList(bean.getId());
                                                for (DetonatorBean b : detonatorBeanList)
                                                    b.setSchemeId(schemeBean.getId());
                                                DbUtil.updateDetonatorList(detonatorBeanList);
                                                pDialog.incrementProgressBy(1);
                                            }
                                        runOnUiThread(() -> enableButton(true));
                                        myApp.myToast(ExplosionRecordActivity.this, R.string.message_import_success);
                                    }).start();
                                })
                                .setNegativeButton(R.string.button_cancel, null)
                                .show(), true);
                    }
                }
                break;
            case KeyEvent.KEYCODE_3:
                if (btnDelete.isEnabled()) {
                    boolean canDelete = true;
                    if (forceDelete != 4) {
                        for (ExplosionRecordBean bean : list) {
                            if (bean.isSelected() && bean.getUploadServer() == -1) {
                                myApp.myToast(ExplosionRecordActivity.this, R.string.message_cannot_delete_not_upload);
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
                                .setPositiveButton(R.string.button_confirm, (dialog1, which1) -> {
                                    List<Long> idList = new ArrayList<>();
                                    Iterator<ExplosionRecordBean> it = list.iterator();
                                    while (it.hasNext()) {
                                        ExplosionRecordBean b = it.next();
                                        if (b.isSelected()) {
                                            idList.add(b.getId());
                                            it.remove();
                                        }
                                    }
                                    if (idList.size() > 0) {
                                        long[] id = new long[idList.size()];
                                        for (int i = 0; i < idList.size(); i++)
                                            id[i] = idList.get(i);
                                        DbUtil.deleteScheme(id);
                                    }
                                    adapter.updateList(list);
                                    enableButton(true);
                                })
                                .setNegativeButton(R.string.button_cancel, null)
                                .show(), true);
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (listView.hasFocus() && listView.getSelectedItemPosition() >= 0 && listView.getSelectedItemPosition() < list.size()) {
                    list.get(listView.getSelectedItemPosition()).setSelected(true);
                    checkboxStatus();
                    adapter.updateList(list);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (listView.hasFocus() && listView.getSelectedItemPosition() >= 0 && listView.getSelectedItemPosition() < list.size()) {
                    list.get(listView.getSelectedItemPosition()).setSelected(false);
                    checkboxStatus();
                    adapter.updateList(list);
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
    }

    private void enableButton(boolean enable) {
        if (enable && null != pDialog && pDialog.isShowing())
            pDialog.dismiss();
        setProgressVisibility(!enable);
        btnUpload.setEnabled(false);
        btnDelete.setEnabled(false);
        btnImport.setEnabled(false);
        if (enable)
            for (ExplosionRecordBean bean : list)
                if (bean.isSelected()) {
                    btnUpload.setEnabled(true);
                    btnDelete.setEnabled(true);
                    btnImport.setEnabled(true);
                    break;
                }
    }

    @Override
    public void finish() {
        if (UploadExplodeRecord.uploading) {
            runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(ExplosionRecordActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_cancel_upload)
                    .setMessage(R.string.dialog_confirm_exit_upload)
                    .setPositiveButton(R.string.button_confirm, (dialog1, which) -> ExplosionRecordActivity.super.finish())
                    .setNegativeButton(R.string.button_cancel, null)
                    .show(), true));
        } else
            super.finish();
    }

    @Override
    protected void onDestroy() {
        if (null != pDialog && pDialog.isShowing())
            pDialog.dismiss();
        UploadExplodeRecord.uploading = false;
        myHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
