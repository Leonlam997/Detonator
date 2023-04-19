package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.ListView;

import com.leon.detonator.adapter.VersionAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.VersionBean;
import com.leon.detonator.R;
import com.leon.detonator.util.FilePath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VersionManageActivity extends BaseActivity {
    private List<VersionBean> list;
    private CheckBox cbSelected;
    private MyButton btnInstall, btnDelete;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_manage);

        setTitle(R.string.upgrade_version);
        myApp = (BaseApplication) getApplication();
        initData();
        ListView listView = findViewById(R.id.lv_version_list);
        final VersionAdapter adapter = new VersionAdapter(this, list);
        listView.setAdapter(adapter);
        btnInstall = findViewById(R.id.btn_install);
        btnDelete = findViewById(R.id.btn_delete);
        cbSelected = findViewById(R.id.cb_selected);
        cbSelected.setChecked(false);
        btnInstall.setEnabled(false);
        btnDelete.setEnabled(false);
        cbSelected.setOnClickListener(v -> {
            for (VersionBean item : list) {
                item.setSelected(cbSelected.isChecked());
            }
            checkboxStatus();
            adapter.updateList(list);
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            list.get(position).setSelected(!list.get(position).isSelected());
            checkboxStatus();
            adapter.updateList(list);
        });
        btnInstall.setOnClickListener(v -> installVersion());
        btnDelete.setOnClickListener(v -> deleteVersion());
    }

    private void installVersion() {
        if (btnInstall.isEnabled())
            for (final VersionBean bean : list) {
                if (bean.isSelected()) {
                    runOnUiThread(() ->  BaseApplication.customDialog(new AlertDialog.Builder(VersionManageActivity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_upload)
                                .setMessage(String.format(Locale.CHINA, getString(R.string.dialog_confirm_install), bean.getVersion()))
                                .setPositiveButton(R.string.btn_confirm, (dialog1, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(new File(String.format(Locale.CHINA, FilePath.FILE_UPDATE_APK, bean.getVersion()))), "application/vnd.android.package-archive");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                })
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()));
                    break;
                }
            }
    }

    private void deleteVersion() {
        if (btnDelete.isEnabled())
            runOnUiThread(() -> {
                int count = 0;
                for (VersionBean bean : list)
                    if (bean.isSelected()) {
                        count++;
                    }
                if (0 != count) {
                    BaseApplication.customDialog(new AlertDialog.Builder(VersionManageActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_upload)
                            .setMessage(String.format(Locale.CHINA, getString(R.string.dialog_confirm_delete_version), count))
                            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                                for (VersionBean bean : list)
                                    if (bean.isSelected()) {
                                        File file = new File(String.format(Locale.CHINA, FilePath.FILE_UPDATE_APK, bean.getVersion()));
                                        if (file.exists() && !file.delete()) {
                                            myApp.myToast(VersionManageActivity.this,
                                                    String.format(Locale.CHINA, getString(R.string.message_delete_file_fail), file.getName()));
                                        }
                                    }
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show());
                }
            });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                installVersion();
                break;
            case KeyEvent.KEYCODE_2:
                deleteVersion();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void checkboxStatus() {
        int count = 0;
        for (VersionBean item : list) {
            if (item.isSelected()) {
                count++;
            }
        }
        cbSelected.setChecked(false);
        btnInstall.setEnabled(false);
        btnDelete.setEnabled(true);
        if (0 == count) {
            btnDelete.setEnabled(false);
        } else if (1 == count) {
            btnInstall.setEnabled(true);
        }
        if (count != 0 && count == list.size()) {
            cbSelected.setChecked(true);
        }
    }

    private void initData() {
        File[] files = new File(FilePath.FILE_UPDATE_PATH + "/").listFiles();
        list = new ArrayList<>();
        if (files != null && files.length > 0) {
            Arrays.sort(files, (f1, f2) -> {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0)
                    return 1;
                else if (diff == 0)
                    return 0;
                else
                    return -1;
            });
            for (File f : files) {
                VersionBean bean = new VersionBean();
                bean.setDownloadDate(new Date(f.lastModified()));
                bean.setSize(f.length());
                bean.setVersion(f.getName().replace(".apk", ""));
                bean.setSelected(false);
                list.add(bean);
            }
        }
    }
}