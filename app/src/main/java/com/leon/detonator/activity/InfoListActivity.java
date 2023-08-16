package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.leon.detonator.R;
import com.leon.detonator.adapter.InfoAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.List;

public class InfoListActivity extends BaseActivity {
    private int infoType;
    private List<EnterpriseBean> enterpriseList;
    private List<BaiSeInfoBean> projectList;
    private List<BaiSeBlasterBean> blasterList;
    private InfoAdapter<EnterpriseBean> enterpriseAdapter;
    private InfoAdapter<BaiSeInfoBean> projectAdapter;
    private InfoAdapter<BaiSeBlasterBean> blasterAdapter;
    private ListView listView;
    private MyButton btnDelete;
    private MyButton btnModify;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode()) {
            switch (infoType) {
                case ConstantUtils.INFO_ENTERPRISE:
                    enterpriseList = DbUtil.getEnterpriseList();
                    enterpriseAdapter.updateList(enterpriseList);
                    break;
                case ConstantUtils.INFO_PROJECT:
                    projectList = DbUtil.getBaiSeInfoList();
                    projectAdapter.updateList(projectList);
                    break;
                case ConstantUtils.INFO_BLASTER:
                    blasterList = DbUtil.getBaiSeBlasterList();
                    blasterAdapter.updateList(blasterList);
                    break;
            }
            checkButton();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_list);
        infoType = getIntent().getIntExtra(KeyUtils.KEY_INFO_TYPE, -1);
        String info1;
        String info2;
        listView = findViewById(R.id.lv_list);
        findViewById(R.id.rb_selected).setVisibility(View.INVISIBLE);
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                setTitle(R.string.settings_enterprise);
                enterpriseList = DbUtil.getEnterpriseList();
                enterpriseAdapter = new InfoAdapter<>(InfoListActivity.this, enterpriseList);
                listView.setAdapter(enterpriseAdapter);
                for (int i = 0; i < enterpriseList.size(); i++)
                    if (enterpriseList.get(i).isSelected()) {
                        listView.smoothScrollToPosition(i);
                        listView.setSelection(i);
                        break;
                    }
                info1 = getString(R.string.enterprise_code);
                info2 = getString(R.string.enterprise_detector_id);
                break;
            case ConstantUtils.INFO_PROJECT:
                setTitle(R.string.settings_enterprise);
                projectList = DbUtil.getBaiSeInfoList();
                projectAdapter = new InfoAdapter<>(InfoListActivity.this, projectList);
                listView.setAdapter(projectAdapter);
                for (int i = 0; i < projectList.size(); i++)
                    if (projectList.get(i).isSelected()) {
                        listView.smoothScrollToPosition(i);
                        listView.setSelection(i);
                        break;
                    }
                info1 = getString(R.string.enterprise_name);
                info2 = getString(R.string.enterprise_contract_project_name);
                break;
            case ConstantUtils.INFO_BLASTER:
                setTitle(R.string.bai_se_detector);
                blasterList = DbUtil.getBaiSeBlasterList();
                blasterAdapter = new InfoAdapter<>(InfoListActivity.this, blasterList);
                listView.setAdapter(blasterAdapter);
                for (int i = 0; i < blasterList.size(); i++)
                    if (blasterList.get(i).isSelected()) {
                        listView.smoothScrollToPosition(i);
                        listView.setSelection(i);
                        break;
                    }
                info1 = getString(R.string.enterprise_detector_id);
                info2 = getString(R.string.detector_name);
                break;
            default:
                finish();
                return;
        }
        ((TextView) findViewById(R.id.tv_info1)).setText(info1.substring(0, info1.length() - 1));
        ((TextView) findViewById(R.id.tv_info2)).setText(info2.substring(0, info2.length() - 1));
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        listView.setOnItemClickListener((adapterView, view, i, l) -> modifyItem(i));
        listView.requestFocus();
        findViewById(R.id.btn_new).setOnClickListener(view -> addItem());
        btnModify = findViewById(R.id.btn_modify);
        btnModify.setOnClickListener(view -> modifyItem(getSelectedIndex()));
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(view -> deleteItem(getSelectedIndex()));
        checkButton();
    }

    private void addItem() {
        Intent intent;
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                intent = new Intent(InfoListActivity.this, EnterpriseActivity.class);
                break;
            case ConstantUtils.INFO_PROJECT:
                intent = new Intent(InfoListActivity.this, BaiSeDataActivity.class);
                break;
            case ConstantUtils.INFO_BLASTER:
                BaiSeInfoBean bean = DbUtil.getCurrentBaiSeInfo();
                if (bean == null) {
                    myApp.myToast(InfoListActivity.this, R.string.message_select_enterprise);
                    return;
                } else
                    intent = new Intent(InfoListActivity.this, BaiSeDetectorActivity.class);
                break;
            default:
                return;
        }
        intent.putExtra(KeyUtils.KEY_NEW_INFO, true);
        launcher.launch(intent);
    }

    private void modifyItem(int i) {
        if (i == -1)
            return;
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                if (enterpriseList.get(i).isSelected())
                    launcher.launch(new Intent(InfoListActivity.this, EnterpriseActivity.class));
                else
                    selectItem(i);
                break;
            case ConstantUtils.INFO_PROJECT:
                if (projectList.get(i).isSelected())
                    launcher.launch(new Intent(InfoListActivity.this, BaiSeDataActivity.class));
                else
                    selectItem(i);
                break;
            case ConstantUtils.INFO_BLASTER:
                if (blasterList.get(i).isSelected())
                    launcher.launch(new Intent(InfoListActivity.this, BaiSeDetectorActivity.class));
                else
                    selectItem(i);
                break;
        }
    }

    private void deleteItem(int i) {
        if (i == -1)
            return;
        if (btnDelete.isEnabled())
            runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(InfoListActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_delete_info)
                    .setMessage(infoType == ConstantUtils.INFO_BLASTER ? R.string.dialog_confirm_delete_blaster : R.string.dialog_confirm_delete_enterprise)
                    .setPositiveButton(R.string.button_confirm, (dialog1, which1) -> {
                        switch (infoType) {
                            case ConstantUtils.INFO_ENTERPRISE:
                                DbUtil.deleteEnterprise(enterpriseList.get(i).getId());
                                enterpriseList.remove(i);
                                enterpriseAdapter.updateList(enterpriseList);
                                break;
                            case ConstantUtils.INFO_PROJECT:
                                DbUtil.deleteBaiSeProject(projectList.get(i).getId());
                                projectList.remove(i);
                                projectAdapter.updateList(projectList);
                                break;
                            case ConstantUtils.INFO_BLASTER:
                                DbUtil.deleteBaiSeBlaster(blasterList.get(i).getId());
                                blasterList.remove(i);
                                blasterAdapter.updateList(blasterList);
                                break;
                            default:
                                return;
                        }
                        setResult(RESULT_CANCELED);
                        runOnUiThread(() -> {
                            btnModify.setEnabled(false);
                            btnDelete.setEnabled(false);
                        });
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show(), true));
    }

    private void checkButton() {
        runOnUiThread(() -> {
            btnModify.setEnabled(getSelectedIndex() != -1);
            btnDelete.setEnabled(getSelectedIndex() != -1);
        });
    }

    private void selectItem(int pos) {
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                if (!enterpriseList.get(pos).isSelected()) {
                    for (EnterpriseBean bean : enterpriseList)
                        bean.setSelected(false);
                    enterpriseList.get(pos).setSelected(true);
                    DbUtil.updateEnterprise(enterpriseList.get(pos));
                    enterpriseAdapter.updateList(enterpriseList);
                }
                break;
            case ConstantUtils.INFO_PROJECT:
                if (!projectList.get(pos).isSelected()) {
                    for (BaiSeInfoBean bean : projectList)
                        bean.setSelected(false);
                    projectList.get(pos).setSelected(true);
                    DbUtil.updateBaiSeInfo(projectList.get(pos));
                    projectAdapter.updateList(projectList);
                }
                break;
            case ConstantUtils.INFO_BLASTER:
                if (!blasterList.get(pos).isSelected()) {
                    for (BaiSeBlasterBean bean : blasterList)
                        bean.setSelected(false);
                    blasterList.get(pos).setSelected(true);
                    DbUtil.updateBaiSeBlaster(blasterList.get(pos));
                    blasterAdapter.updateList(blasterList);
                }
                break;
        }
        setResult(RESULT_OK);
        listView.setSelection(pos);
        if (!btnModify.isEnabled())
            runOnUiThread(() -> {
                btnModify.setEnabled(true);
                btnDelete.setEnabled(true);
            });
    }

    private int getSelectedIndex() {
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                for (int i = 0; i < enterpriseList.size(); i++)
                    if (enterpriseList.get(i).isSelected())
                        return i;
                break;
            case ConstantUtils.INFO_PROJECT:
                for (int i = 0; i < projectList.size(); i++)
                    if (projectList.get(i).isSelected())
                        return i;
                break;
            case ConstantUtils.INFO_BLASTER:
                for (int i = 0; i < blasterList.size(); i++)
                    if (blasterList.get(i).isSelected())
                        return i;
                break;
        }
        return -1;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                addItem();
                break;
            case KeyEvent.KEYCODE_2:
                if (btnModify.isEnabled())
                    modifyItem(getSelectedIndex());
                break;
            case KeyEvent.KEYCODE_3:
                if (btnDelete.isEnabled())
                    deleteItem(getSelectedIndex());
        }
        return super.onKeyUp(keyCode, event);
    }
}