package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.leon.detonator.R;
import com.leon.detonator.adapter.OfflineListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.CheckRegister;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.dialog.EnterpriseDialog;
import com.leon.detonator.fragment.OfflineControlFragment;
import com.leon.detonator.fragment.OfflineListFragment;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.ErrorCode;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Response;

public class AuthorizationListActivity extends BaseActivity {
    private final int[] title = {R.string.tab_title_detonators, R.string.tab_title_control};
    private List<DetonatorInfoBean> list;
    private TabLayout tabList;
    private ViewPager pagerList;
    private List<Fragment> fragments;
    private OfflineControlFragment controlFragment;
    private OfflineListFragment listFragment;
    private OfflineListAdapter adapter;
    private EnterpriseDialog enterpriseDialog;
    private String token;
    private EnterpriseBean enterpriseBean;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization_list);

        setTitle(R.string.auth_list);
        myApp = (BaseApplication) getApplication();
        enterpriseBean = myApp.readEnterprise();
        initPage();
    }

    private final Handler checkExploderHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case 1:
                    disableClick(false);
                    break;
                case 2:
                    if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
                        myApp.myToast(AuthorizationListActivity.this, R.string.message_fill_enterprise);
                        startActivity(new Intent(AuthorizationListActivity.this, EnterpriseActivity.class));
                    } else {
                        BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                                .setTitle(R.string.dialog_title_download)
                                .setMessage(R.string.dialog_confirm_offline_download)
                                .setPositiveButton(R.string.btn_confirm, (dialog1, which) -> offlineDownload())
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show(), true);
                    }
                    break;
            }
            return false;
        }
    });

    private void initPage() {
        tabList = findViewById(R.id.tab_title);
        pagerList = findViewById(R.id.view_pager);
        try {
            list = new ArrayList<>();
            myApp.readFromFile(FilePath.FILE_OFFLINE_LIST, list, DetonatorInfoBean.class);
            if (list.size() > 0) {
                checkList(myApp.readDownloadList(false).getResult().getLgs().getLg());
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        fragments = new ArrayList<>();

        tabList.addTab(tabList.newTab());
        adapter = new OfflineListAdapter(this, list);

        listFragment = new OfflineListFragment();
        listFragment.setListAdapter(adapter);
        listFragment.setClickDownload(view -> {
            if (!BaseApplication.readSettings().isRegistered()) {
                myApp.registerExploder();
                disableClick(true);
                new CheckRegister() {
                    @Override
                    public void onError() {
                        checkExploderHandler.sendEmptyMessage(1);
                    }

                    @Override
                    public void onSuccess() {
                        checkExploderHandler.sendEmptyMessage(2);
                    }
                }.setActivity(AuthorizationListActivity.this).start();
            } else {
                checkExploderHandler.sendEmptyMessage(2);
            }
        });
        listFragment.setClickDelete(view -> BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_delete_detonator)
                .setMessage(R.string.dialog_confirm_delete_detonator)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    Iterator<DetonatorInfoBean> iterator = list.iterator();
                    DownloadDetonatorBean bean = myApp.readDownloadList(false);
                    while (iterator.hasNext()) {
                        DetonatorInfoBean bean1 = iterator.next();
                        if (bean1.isSelected()) {
                            if (bean1.isDownloaded()) {
                                for (LgBean b : bean.getResult().getLgs().getLg()) {
                                    if (b.getFbh().equals(bean1.getAddress())) {
                                        bean.getResult().getLgs().getLg().remove(b);
                                        break;
                                    }
                                }
                            }
                            iterator.remove();
                        }
                    }
                    myApp.saveDownloadList(bean, false);
                    saveList();
                    controlFragment.setNewButtonEnabled(list.size() > 0);
                    adapter.updateList(list);
                    listFragment.checkStatus(false);
                    listFragment.setCheckAll(false);
                    resetTabTitle(false);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                    }
                    return false;
                })
                .show(), true));
        List<DetonatorInfoBean> detonatorList = new ArrayList<>();
        myApp.readFromFile(myApp.getListFile(), detonatorList, DetonatorInfoBean.class);
        listFragment.setClickImport(v -> {
            if (detonatorList.size() > 0) {
                boolean success = false;
                for (DetonatorInfoBean bean : detonatorList)
                    if (!list.contains(bean)) {
                        success = true;
                        bean.setDownloaded(false);
                        list.add(bean);
                    }
                if (success) {
                    adapter.updateList(list);
                    myApp.myToast(AuthorizationListActivity.this, R.string.message_restore_success);
                }
                resetTabTitle(false);
                saveList();
                listFragment.checkStatus(false);
                controlFragment.setNewButtonEnabled(true);
            }
        });
        listFragment.setImportEnabled(detonatorList.size() > 0);

        controlFragment = new OfflineControlFragment();
        controlFragment.setNewButtonEnabled(list.size() > 0);

        controlFragment.setClickAdd(view -> {
            for (DetonatorInfoBean bean : controlFragment.getList()) {
                boolean add = true;
                for (DetonatorInfoBean bean1 : list)
                    if (bean.getAddress().equals(bean1.getAddress())) {
                        add = false;
                        break;
                    }
                if (add)
                    list.add(bean);
            }
            controlFragment.setNewButtonEnabled(list.size() > 0);
            adapter.updateList(list);
            resetTabTitle(false);
            listFragment.checkStatus(false);
            saveList();
        });
        controlFragment.setClickNew(view -> BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_new_list)
                .setMessage(R.string.dialog_clear_list)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    list.clear();
                    saveList();
                    File file = new File(FilePath.FILE_OFFLINE_DOWNLOAD_LIST);
                    if (file.exists() && !file.delete()) {
                        myApp.myToast(AuthorizationListActivity.this, R.string.message_delete_fail);
                    }
                    listFragment.checkStatus(false);
                    controlFragment.setNewButtonEnabled(false);
                    adapter.updateList(list);
                    resetTabTitle(false);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                    }
                    return false;
                })
                .show(), true));
        fragments.add(listFragment);
        fragments.add(controlFragment);

        pagerList.setAdapter(new ListPagerAdapter(getSupportFragmentManager()));
        tabList.setupWithViewPager(pagerList);

        resetTabTitle(true);
        tabList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                resetTabTitle(false);
                pagerList.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void disableClick(boolean disabled) {
        setProgressVisibility(disabled);
        tabList.setEnabled(!disabled);
        pagerList.setEnabled(!disabled);
        listFragment.checkStatus(disabled);
    }

    private void offlineDownload() {
        enterpriseDialog = new EnterpriseDialog(AuthorizationListActivity.this);
        enterpriseDialog.setClickConfirm(view -> {
            disableClick(true);
            enterpriseDialog.dismiss();
            StringBuilder str = new StringBuilder();
            for (DetonatorInfoBean bean : list) {
                str.append(bean.getAddress()).append(",");
            }
            str.deleteCharAt(str.length() - 1);
            token = myApp.makeToken();
            Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_OFFLINE_DOWNLOAD);
            if (null != params) {
                params.put("dsc", str.toString());
                params.put("dwdm", enterpriseBean.getCode());
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
                                myApp.myToast(AuthorizationListActivity.this, R.string.message_offline_download_fail);
                                disableClick(false);
                            }

                            @Override
                            public void onResponse(DownloadDetonatorBean downloadDetonatorBean, int i) {
                                disableClick(false);
                                if (null != downloadDetonatorBean) {
                                    if (downloadDetonatorBean.getToken().equals(token)) {
                                        if (downloadDetonatorBean.isStatus()) {
                                            if (null != downloadDetonatorBean.getResult()) {
                                                if (downloadDetonatorBean.getResult().getCwxx().equals("0")) {
                                                    List<LgBean> detonators = downloadDetonatorBean.getResult().getLgs().getLg();
                                                    if (null != detonators) {
                                                        myApp.saveDownloadList(downloadDetonatorBean, false);
                                                        checkList(detonators);
                                                    }
                                                    myApp.myToast(AuthorizationListActivity.this, R.string.message_offline_download_success);
                                                } else {
                                                    String error = ErrorCode.downloadErrorCode.get(downloadDetonatorBean.getResult().getCwxx());
                                                    if (null == error) {
                                                        error = getString(R.string.message_download_unknown_error) + downloadDetonatorBean.getResult().getCwxx();
                                                    }
                                                    myApp.myToast(AuthorizationListActivity.this, error);
                                                }
                                            }
                                        } else {
                                            myApp.myToast(AuthorizationListActivity.this, downloadDetonatorBean.getDescription());
                                        }
                                    } else {
                                        myApp.myToast(AuthorizationListActivity.this, R.string.message_token_error);
                                    }
                                } else {
                                    myApp.myToast(AuthorizationListActivity.this, R.string.message_return_data_error);
                                }
                            }
                        });
            }
        });
        enterpriseDialog.setClickModify(view -> {
            enterpriseDialog.dismiss();
            disableClick(false);
            startActivity(new Intent(AuthorizationListActivity.this, EnterpriseActivity.class));
        });
        enterpriseDialog.show();
    }

    private void checkList(List<LgBean> detonators) {
        for (LgBean bean : detonators) {
            for (DetonatorInfoBean bean1 : list) {
                if (bean.getFbh().equals(bean1.getAddress())) {
                    bean1.setDownloaded(true);
                    bean1.setRow(Integer.parseInt(bean.getGzmcwxx()));
                }
            }
        }
        adapter.updateList(list);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((null == enterpriseDialog || !enterpriseDialog.isShowing()) && KeyEvent.ACTION_UP == event.getAction() && 0 == tabList.getSelectedTabPosition()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_1:
                    if (listFragment.getDownloadEnabled())
                        listFragment.getClickDownload().onClick(listFragment.getView());
                    break;
                case KeyEvent.KEYCODE_2:
                    if (listFragment.getImportEnabled())
                        listFragment.getClickImport().onClick(listFragment.getView());
                    break;
                case KeyEvent.KEYCODE_3:
                    if (listFragment.getDeleteEnabled())
                        listFragment.getClickDelete().onClick(listFragment.getView());
                    break;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void resetTabTitle(boolean init) {
        for (int i = 0; i < title.length; i++) {
            TextView tv = (TextView) LayoutInflater.from(this).inflate(R.layout.layout_tab_view, tabList, false);
            String text = getString(title[i]) + (0 == i ? "(" + list.size() + ")" : "");
            tv.setText(text);
            TabLayout.Tab tab = tabList.getTabAt(i);
            if (tab != null) {
                if (tab.isSelected())
                    tv.setTextColor(getColor(R.color.text_blue));
                else
                    tv.setTextColor(getColor(R.color.text_black));
                if (!init && null != tab.getCustomView()) {
                    final ViewParent customParent = tab.getCustomView().getParent();
                    if (null != customParent) {
                        ((ViewGroup) customParent).removeView(tab.getCustomView());
                    }
                }
                tab.setCustomView(tv);
            }
        }
    }

    private void saveList() {
        try {
            myApp.writeToFile(FilePath.FILE_OFFLINE_LIST, list);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private class ListPagerAdapter extends FragmentPagerAdapter {
        ListPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

    }


}
