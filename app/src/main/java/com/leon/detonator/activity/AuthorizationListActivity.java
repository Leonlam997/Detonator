package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.leon.detonator.R;
import com.leon.detonator.adapter.OfflineListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.bean.OfflineDetonatorBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.EnterpriseDialog;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.ErrorCode;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

public class AuthorizationListActivity extends BaseActivity {
    private List<SchemeBean> schemeList;
    private List<DetonatorBean> list;
    private EnterpriseBean enterpriseBean;
    private OfflineListAdapter adapter;
    private ListView listView;
    private MyButton btnAdd;
    private MyButton btnDownload;
    private MyButton btnDelete;
    private String token;
    private int requestCode;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode())
            if (requestCode == 0)
                enterpriseBean = DbUtil.getCurrentEnterprise();
            else if (requestCode == 1)
                importDetonator();
    });

    private final Handler myHandler = new Handler(message -> {
        switch (message.what) {
            case BaseApplication.HANDLER_REGISTER_ERROR:
                enableButton(true);
                break;
            case BaseApplication.HANDLER_REGISTER_SUCCESS:
                if (null == enterpriseBean) {
                    Intent intent = new Intent(AuthorizationListActivity.this, InfoListActivity.class);
                    intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                    requestCode = 0;
                    launcher.launch(intent);
                } else
                    showDialog();
                break;
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization_list);
        setTitle(R.string.auth_list);
        enterpriseBean = DbUtil.getCurrentEnterprise();
        list = DbUtil.getAuthDetonatorList();
        if (list.size() > 0) {
            OfflineDetonatorBean bean = DbUtil.getOfflineDownloadDetonator();
            if (bean != null)
                checkList(bean.getResult().getLgs().getLg());
        }
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        listView = findViewById(R.id.lv_offline_list);
        schemeList = DbUtil.getSchemeList();
        Log.d("ZBEST", "list size=" + schemeList.size());
        btnAdd = findViewById(R.id.btn_add);
        btnDownload = findViewById(R.id.btn_offline_download);
        btnDelete = findViewById(R.id.btn_delete);
        btnAdd.setOnClickListener(v -> function(0));
        btnDownload.setOnClickListener(v -> function(1));
        btnDelete.setOnClickListener(v -> function(2));
        adapter = new OfflineListAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            list.get(position).setSelected(!list.get(position).isSelected());
            adapter.updateList(list);
            myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
        });
        btnAdd.requestFocus();
        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
    }

    private void function(int l) {
        switch (l) {
            case 0:
                final View inputCodeView = LayoutInflater.from(AuthorizationListActivity.this).inflate(R.layout.layout_dialog_add_detonator, listView, false);
                final EditText etStart = inputCodeView.findViewById(R.id.et_start);
                final EditText etAmount = inputCodeView.findViewById(R.id.et_amount);
                final EditText etBox = inputCodeView.findViewById(R.id.et_box);
                final CheckBox cbImport = inputCodeView.findViewById(R.id.cb_import);
                final CheckBox cbMultiple = inputCodeView.findViewById(R.id.cb_multiple);
                final TextView tvStart = inputCodeView.findViewById(R.id.tv_start);
                etAmount.setHint("1");
                etBox.setHint("10");
                cbImport.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    etBox.setEnabled(!isChecked && cbMultiple.isChecked());
                    etAmount.setEnabled(!isChecked && cbMultiple.isChecked());
                    etStart.setEnabled(!isChecked);
                    cbMultiple.setEnabled(!isChecked);
                });
                cbMultiple.setOnCheckedChangeListener(((buttonView, isChecked) -> {
                    tvStart.setText(isChecked ? R.string.input_start_shell : R.string.text_shell_code);
                    etBox.setEnabled(isChecked);
                    etAmount.setEnabled(isChecked);
                }));
                etBox.setEnabled(false);
                etAmount.setEnabled(false);
                if (schemeList.size() == 1)
                    cbImport.setText(R.string.text_import);
                cbImport.setEnabled(schemeList.size() > 0);
                etStart.setKeyListener(new NumberKeyListener() {
                    @NonNull
                    @Override
                    protected char[] getAcceptedChars() {
                        return ConstantUtils.INPUT_DETONATOR_ACCEPT.toCharArray();
                    }

                    @Override
                    public int getInputType() {
                        return InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    }
                });
                BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_manual_input)
                        .setView(inputCodeView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.button_confirm, (dialogInterface, ii) -> {
                            etStart.setText(etStart.getText().toString().toUpperCase());
                            if (cbImport.isChecked()) {
                                if (schemeList.size() == 1) {
                                    if (!schemeList.get(0).isSelected()) {
                                        schemeList.get(0).setSelected(true);
                                        DbUtil.updateScheme(schemeList.get(0));
                                    }
                                    importDetonator();
                                } else {
                                    Intent intent = new Intent(AuthorizationListActivity.this, SchemeActivity.class);
                                    intent.putExtra(KeyUtils.KEY_SELECT_SCHEME, true);
                                    requestCode = 1;
                                    launcher.launch(intent);
                                    return;
                                }
                            } else if (Pattern.matches(ConstantUtils.SHELL_PATTERN, etStart.getText().toString())) {
                                int amount = !cbMultiple.isChecked() || etAmount.getText().toString().isEmpty() ? 1 : Integer.parseInt(etAmount.getText().toString());
                                int box = cbMultiple.isChecked() ? (etBox.getText().toString().isEmpty() ? 10 : Integer.parseInt(etBox.getText().toString())) : 1;
                                if (box < 0 || box > 100) {
                                    myApp.myToast(AuthorizationListActivity.this, R.string.message_amount_in_box_out_of_range);
                                    return;
                                }
                                for (int i = 0; i < amount; i++)
                                    for (int j = 0; j < box; j++) {
                                        DetonatorBean bean = new DetonatorBean(etStart.getText().toString().substring(0, 8)
                                                + String.format(Locale.getDefault(), "%05d", (Integer.parseInt(etStart.getText().toString().substring(8)) + i * 100L + j) % 100000));
                                        bean.setDownloaded(false);
                                        if (!list.contains(bean))
                                            list.add(bean);
                                    }
                            } else {
                                myApp.myToast(AuthorizationListActivity.this, R.string.message_detonator_input_error);
                                return;
                            }
                            Collections.sort(list, (o1, o2) -> o1.getAddress().compareTo(o2.getAddress()));
                            adapter.updateList(list);
                            DbUtil.updateAuthDetonatorList(list);
                            myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .show(), false);
                break;
            case 1:
                if (btnDownload.isEnabled())
                    if (!BaseApplication.settings.isRegistered()) {
                        enableButton(false);
                        myApp.registerExploder(myHandler);
                    } else
                        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_SUCCESS);
                break;
            case 2:
                if (btnDelete.isEnabled())
                    batchDelete();
                break;
        }
    }

    private void batchDelete() {
        final View modifyView = LayoutInflater.from(AuthorizationListActivity.this).inflate(R.layout.layout_dialog_batch_modify, listView, false);
        final EditText etFrom = modifyView.findViewById(R.id.et_from);
        final EditText etTo = modifyView.findViewById(R.id.et_to);
        final EditText etDelay = modifyView.findViewById(R.id.et_delay);
        final CheckBox cbSelectAll = modifyView.findViewById(R.id.cb_selected_all);
        modifyView.findViewById(R.id.ll_delay).setVisibility(View.GONE);
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etFrom.setEnabled(!isChecked);
            etTo.setEnabled(!isChecked);
            if (isChecked) {
                etFrom.setText("1");
                etTo.setText(String.format(Locale.getDefault(), "%d", list.size()));
                etFrom.setTextColor(getColor(R.color.colorHintText));
                etTo.setTextColor(getColor(R.color.colorHintText));
                etDelay.requestFocus();
                etDelay.setSelection(etDelay.getText().length());
            } else {
                etFrom.setTextColor(getColor(R.color.colorLabelText));
                etTo.setTextColor(getColor(R.color.colorLabelText));
                etTo.requestFocus();
                etTo.setSelection(etTo.getText().length());
            }
        });
        final TextWatcher watcher1 = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().isEmpty())
                    try {
                        int num = Integer.parseInt(charSequence.toString());
                        if (num <= 0 || num > list.size())
                            myApp.myToast(AuthorizationListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                    } catch (Exception e) {
                        myApp.myToast(AuthorizationListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                    }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        etFrom.addTextChangedListener(watcher1);
        etTo.addTextChangedListener(watcher1);
        BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.button_batch_delete)
                .setView(modifyView)
                .setCancelable(false)
                .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                    int from;
                    int to;
                    try {
                        if (etTo.getText().toString().isEmpty())
                            etTo.setText(etFrom.getText());
                        from = Integer.parseInt(etFrom.getText().toString());
                        to = Integer.parseInt(etTo.getText().toString());
                        if (from <= 0 || from > list.size() || to <= 0 || to > list.size()) {
                            myApp.myToast(AuthorizationListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            return;
                        }
                    } catch (Exception e) {
                        myApp.myToast(AuthorizationListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                        return;
                    }
                    int finalFrom = Math.min(from - 1, to - 1);
                    int finalTo = Math.max(from, to);
                    BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                            .setTitle(R.string.dialog_title_delete_detonator)
                            .setMessage(R.string.dialog_confirm_delete_detonator)
                            .setPositiveButton(R.string.button_confirm, (dialog1, which) -> {
                                list.subList(finalFrom, finalTo).clear();
                                adapter.updateList(list);
                                DbUtil.updateAuthDetonatorList(list);
                                myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                            })
                            .setNegativeButton(R.string.button_cancel, null)
                            .show(), true);
                }).
                setNegativeButton(R.string.button_cancel, null)
                .show(), false);
    }

    private void importDetonator() {
        List<DetonatorBean> detonatorList = DbUtil.getCurrentDetonatorList();
        if (detonatorList.size() > 0) {
            boolean added = false;
            for (DetonatorBean bean : detonatorList)
                if (!list.contains(bean)) {
                    bean.setDownloaded(false);
                    bean.setRow(0);
                    list.add(bean);
                    added = true;
                }
            if (added) {
                myApp.myToast(AuthorizationListActivity.this, R.string.message_import_success);
                adapter.updateList(list);
                DbUtil.updateAuthDetonatorList(list);
            }
        }
        myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
    }

    private void enableButton(boolean b) {
        setProgressVisibility(!b);
        btnAdd.setEnabled(b);
        btnDelete.setEnabled(b && list.size() > 0);
        btnDownload.setEnabled(b && list.size() > 0);
    }

    private void showDialog() {
        EnterpriseDialog enterpriseDialog = new EnterpriseDialog(AuthorizationListActivity.this);
        enterpriseDialog.show();
        MyButton btnConfirm = enterpriseDialog.findViewById(R.id.btn_dialog_confirm);
        MyButton btnModify = enterpriseDialog.findViewById(R.id.btn_dialog_modify);
        btnConfirm.setOnClickListener(view -> {
            enableButton(false);
            enterpriseDialog.dismiss();
            StringBuilder str = new StringBuilder();
            for (DetonatorBean bean : list)
                str.append(bean.getAddress()).append(",");
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
                        .build().execute(new Callback<OfflineDetonatorBean>() {
                            @Override
                            public OfflineDetonatorBean parseNetworkResponse(Response response, int i) throws Exception {
                                if (response.body() != null) {
                                    String string = Objects.requireNonNull(response.body()).string();
                                    return BaseApplication.jsonFromString(string, OfflineDetonatorBean.class);
                                }
                                return null;
                            }

                            @Override
                            public void onError(Call call, Exception e, int i) {
                                myApp.myToast(AuthorizationListActivity.this, R.string.message_offline_download_fail);
                                myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                            }

                            @Override
                            public void onResponse(OfflineDetonatorBean offlineDetonatorBean, int i) {
                                myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
                                if (null != offlineDetonatorBean) {
                                    if (offlineDetonatorBean.getToken().equals(token)) {
                                        if (offlineDetonatorBean.isStatus()) {
                                            if (null != offlineDetonatorBean.getResult()) {
                                                if (offlineDetonatorBean.getResult().getCwxx().equals("0")) {
                                                    List<LgBean> detonators = offlineDetonatorBean.getResult().getLgs().getLg();
                                                    if (null != detonators) {
                                                        DbUtil.updateDownloadDetonator(true, offlineDetonatorBean);
                                                        checkList(detonators);
                                                        adapter.updateList(list);
                                                    }
                                                    myApp.myToast(AuthorizationListActivity.this, R.string.message_offline_download_success);
                                                } else {
                                                    String error = ErrorCode.downloadErrorCode.get(offlineDetonatorBean.getResult().getCwxx());
                                                    if (null == error) {
                                                        error = getString(R.string.message_download_unknown_error) + offlineDetonatorBean.getResult().getCwxx();
                                                    }
                                                    myApp.myToast(AuthorizationListActivity.this, error);
                                                }
                                            }
                                        } else {
                                            myApp.myToast(AuthorizationListActivity.this, offlineDetonatorBean.getDescription());
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
        btnModify.setOnClickListener(view -> {
            enterpriseDialog.dismiss();
            myHandler.sendEmptyMessage(BaseApplication.HANDLER_REGISTER_ERROR);
            if (enterpriseBean == null) {
                Intent intent = new Intent(AuthorizationListActivity.this, InfoListActivity.class);
                intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
                launcher.launch(intent);
            } else
                launcher.launch(new Intent(AuthorizationListActivity.this, EnterpriseActivity.class));
        });
    }

    private void checkList(List<LgBean> detonators) {
        for (LgBean bean : detonators)
            for (DetonatorBean bean1 : list)
                if (bean.getFbh().equals(bean1.getAddress())) {
                    bean1.setDownloaded(true);
                    bean1.setRow(Integer.parseInt(bean.getGzmcwxx()));
                    break;
                }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        function(keyCode - KeyEvent.KEYCODE_1);
        return super.onKeyUp(keyCode, event);
    }
}
