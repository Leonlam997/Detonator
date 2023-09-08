package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ListView;

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
                    for (EnterpriseBean bean: enterpriseList)
                        if (bean.isSelected()){
                            setResult(RESULT_OK);
                            break;
                        }
                    break;
                case ConstantUtils.INFO_PROJECT:
                    projectList = DbUtil.getBaiSeInfoList();
                    projectAdapter.updateList(projectList);
                    for (BaiSeInfoBean bean: projectList)
                        if (bean.isSelected()){
                            setResult(RESULT_OK);
                            break;
                        }
                    break;
                case ConstantUtils.INFO_BLASTER:
                    blasterList = DbUtil.getBaiSeBlasterList();
                    blasterAdapter.updateList(blasterList);
                    for (BaiSeBlasterBean bean: blasterList)
                        if (bean.isSelected()){
                            setResult(RESULT_OK);
                            break;
                        }
                    break;
            }
            checkButton();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_list);
        infoType = getIntent().getIntExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
        listView = findViewById(R.id.lv_info);
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
                break;
            default:
                finish();
        }
        listView.setOnItemClickListener((adapterView, view, i, l) -> modifyItem(i));
        listView.requestFocus();
        findViewById(R.id.btn_new).setOnClickListener(view -> addItem());
        btnModify = findViewById(R.id.btn_modify);
        btnModify.setOnClickListener(view -> modifyItem(getSelectedIndex()));
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(view -> deleteItem(getSelectedIndex()));
        checkButton();
    }

    private void checkButton() {
        runOnUiThread(() -> {
            btnModify.setEnabled(getSelectedIndex() != -1);
            btnDelete.setEnabled(getSelectedIndex() != -1);
        });
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
                    ((BaseApplication) getApplication()).myToast(InfoListActivity.this, R.string.message_select_bai_se_project);
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
                    .show()));
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                    addItem();
                    return true;
                case KeyEvent.KEYCODE_TAB:
                    modifyItem(getSelectedIndex());
                    return true;
                case ConstantUtils.KEYCODE_SUB:
                    deleteItem(getSelectedIndex());
                    return true;
                case KeyEvent.KEYCODE_2:
                    listView.requestFocus();
                    if (listView.getSelectedItemPosition() > 0)
                        selectItem(listView.getSelectedItemPosition() - 1);
                    else
                        listView.setSelection(getSelectedIndex());
                    break;
                case KeyEvent.KEYCODE_8:
                    listView.requestFocus();
                    if (listView.getSelectedItemPosition() < getListSize() - 1)
                        selectItem(listView.getSelectedItemPosition() + 1);
                    else
                        listView.setSelection(getSelectedIndex());
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_DOWN)
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                case KeyEvent.KEYCODE_TAB:
                case ConstantUtils.KEYCODE_SUB:
                    return true;
            }
        return super.dispatchKeyEvent(event);
    }

    private int getListSize() {
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                return enterpriseList.size();
            case ConstantUtils.INFO_PROJECT:
                return projectList.size();
            case ConstantUtils.INFO_BLASTER:
                return blasterList.size();
        }
        return 0;
    }

    private int getSelectedIndex() {
        switch (infoType) {
            case ConstantUtils.INFO_ENTERPRISE:
                for (int i = 0; i < enterpriseList.size(); i++)
                    if (enterpriseList.get(i).isSelected()) {
                        return i;
                    }
                break;
            case ConstantUtils.INFO_PROJECT:
                for (int i = 0; i < projectList.size(); i++)
                    if (projectList.get(i).isSelected()) {
                        return i;
                    }
                break;
            case ConstantUtils.INFO_BLASTER:
                for (int i = 0; i < blasterList.size(); i++)
                    if (blasterList.get(i).isSelected()) {
                        return i;
                    }
                break;
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        if (getSelectedIndex() != -1)
            setResult(RESULT_OK);
        super.onDestroy();
    }
}