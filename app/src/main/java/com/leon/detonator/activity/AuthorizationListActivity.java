package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.leon.detonator.R;
import com.leon.detonator.adapter.OfflineListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.CheckRegister;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.DownloadDetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.LgBean;
import com.leon.detonator.component.MarqueeTextView;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.serial.DataReceiveListener;
import com.leon.detonator.serial.SerialCommand;
import com.leon.detonator.serial.SerialPortUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.ErrorCode;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

public class AuthorizationListActivity extends BaseActivity {
    private List<DetonatorBean> detonatorList;
    private List<DetonatorBean> list;
    private AlertDialog enterpriseDialog;
    private EnterpriseBean enterpriseBean;
    private BaseApplication myApp;
    private SerialPortUtil serialPortUtil;
    private DataReceiveListener myReceiveListener;
    private ListView listView;
    private MyButton btnAdd;
    private MyButton btnDownload;
    private MyButton btnDelete;
    private EditText etStart;
    private OfflineListAdapter adapter;
    private SoundPool soundPool;
    private String token;
    private int soundSuccess;
    private int soundFail;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode()) {
            enterpriseBean = DbUtil.getCurrentEnterprise();
            prepareDownload();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization_list);

        setTitle(R.string.auth_list);
        myApp = (BaseApplication) getApplication();
        btnAdd = findViewById(R.id.btn_add);
        btnDownload = findViewById(R.id.btn_offline_download);
        btnDelete = findViewById(R.id.btn_delete);
        listView = findViewById(R.id.lv_auth);
        findViewById(R.id.table_title).setBackgroundColor(getColor(R.color.colorTableTitleBackground));
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            if (list.get(i).getRow() != 0 && ErrorCode.downloadErrorCode.get(list.get(i).getRow() + "") != null)
                BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.detonator_error_info)
                        .setMessage(ErrorCode.downloadErrorCode.get(list.get(i).getRow() + ""))
                        .setPositiveButton(R.string.button_confirm, null)
                        .show());
        });
        btnAdd.setOnClickListener(view -> manualAppend());
        btnDownload.setOnClickListener(view -> {
            if (!BaseApplication.settings.isRegistered()) {
                myApp.registerExploder();
                enabledButton(true);
                new CheckRegister(AuthorizationListActivity.this) {
                    @Override
                    public void onError() {
                        enabledButton(false);
                    }

                    @Override
                    public void onSuccess() {
                        prepareDownload();
                    }
                }.start();
            } else {
                prepareDownload();
            }
        });
        btnDelete.setOnClickListener(view -> deleteDetonator());
        try {
            serialPortUtil = SerialPortUtil.getInstance(this);
            myReceiveListener = DataReceiveListener.getInstance(this, new Handler(msg -> {
                byte[] received = (byte[]) msg.obj;
                if (received != null && received.length > 0)
                    if (received[0] == SerialCommand.ALERT_SHORT_CIRCUIT) {
                        myApp.shortCircuit(AuthorizationListActivity.this, msg.getTarget());
                    } else if (received.length > 20 && 0 == received[SerialCommand.CODE_CHAR_AT + 1]) {
                        myApp.playSoundVibrate(soundPool, soundSuccess);
                        String tempAddress = new String(Arrays.copyOfRange(received, SerialCommand.CODE_CHAR_AT + 2, SerialCommand.CODE_CHAR_AT + 15));
                        if (Pattern.matches(ConstantUtils.SHELL_PATTERN, tempAddress) && etStart != null)
                            etStart.setText(tempAddress);
                    } else if (received.length >= SerialCommand.CODE_CHAR_AT) {
                        myApp.playSoundVibrate(soundPool, soundFail);
                        myApp.myToast(AuthorizationListActivity.this, R.string.message_scan_timeout);
                    }
                return false;
            }));
            serialPortUtil.setOnDataReceiveListener(myReceiveListener);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initData();
        initSound();
        enabledButton(true);
    }

    private void deleteDetonator() {
        if (btnDelete.isEnabled())
            runOnUiThread(() -> {
                final View deleteView = LayoutInflater.from(AuthorizationListActivity.this).inflate(R.layout.layout_dialog_batch_modify, listView, false);
                final EditText etFrom = deleteView.findViewById(R.id.et_from);
                final EditText etTo = deleteView.findViewById(R.id.et_to);
                final CheckBox cbSelectAll = deleteView.findViewById(R.id.cb_selected_all);
                deleteView.findViewById(R.id.ll_delay).setVisibility(View.GONE);
                deleteView.findViewById(R.id.ll_row).setVisibility(View.GONE);
                deleteView.findViewById(R.id.ll_hole).setVisibility(View.GONE);
                deleteView.findViewById(R.id.ll_inside).setVisibility(View.GONE);
                etFrom.requestFocus();
                cbSelectAll.setOnCheckedChangeListener((compoundButton, b) -> {
                    etFrom.setEnabled(!b);
                    etTo.setEnabled(!b);
                    if (b) {
                        etFrom.setText("1");
                        etTo.setText(String.format(Locale.getDefault(), "%d", list.size()));
                        etFrom.setTextColor(getColor(R.color.colorDisabledText));
                        etTo.setTextColor(getColor(R.color.colorDisabledText));
                    } else {
                        etTo.requestFocus();
                        etTo.setSelection(etTo.getText().length());
                        etFrom.setTextColor(getColor(R.color.colorLabelText));
                        etTo.setTextColor(getColor(R.color.colorLabelText));
                    }
                });
                TextWatcher watcher = new TextWatcher() {
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
                etFrom.addTextChangedListener(watcher);
                etTo.addTextChangedListener(watcher);
                BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_batch_delete)
                        .setView(deleteView)
                        .setPositiveButton(R.string.button_confirm, (dialog, which1) -> {
                            try {
                                if (etTo.getText().toString().isEmpty())
                                    etTo.setText(etFrom.getText());
                                int from = Integer.parseInt(etFrom.getText().toString());
                                int to = Integer.parseInt(etTo.getText().toString());
                                if (from <= 0 || from > list.size() || to <= 0 || to > list.size() || to < from) {
                                    myApp.myToast(AuthorizationListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                                    return;
                                }
                                for (int i = 0; i < list.size(); i++)
                                    list.get(i).setSelected(i >= from - 1 && i < to);
                                runOnUiThread(() -> BaseApplication.customDialog(new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                                        .setTitle(R.string.dialog_title_delete_detonator)
                                        .setMessage(R.string.dialog_confirm_delete_detonator)
                                        .setPositiveButton(R.string.button_confirm, (dialog1, which) -> {
                                            list.removeIf(DetonatorBean::isSelected);
                                            adapter.updateList(list);
                                            DbUtil.updateAuthDetonatorList(list);
                                        })
                                        .setNegativeButton(R.string.button_cancel, null)
                                        .setOnKeyListener((dialog1, keyCode, event) -> {
                                            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK)
                                                dialog1.dismiss();
                                            return false;
                                        })
                                        .show()));
                            } catch (Exception e) {
                                myApp.myToast(AuthorizationListActivity.this, String.format(Locale.getDefault(), getString(R.string.message_number_out_of_range), list.size()));
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .show());
            });
    }

    private void initData() {
        enterpriseBean = DbUtil.getCurrentEnterprise();
        detonatorList = DbUtil.getCurrentDetonatorList();
        list = DbUtil.getAuthDetonatorList();
        adapter = new OfflineListAdapter(AuthorizationListActivity.this, list);
        listView.setAdapter(adapter);
    }

    private LinearLayout newItem(@StringRes int res, String text) {
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(2, 2, 2, 2);
        layoutParams.gravity = Gravity.CENTER;
        LinearLayout layoutItem = new LinearLayout(AuthorizationListActivity.this);
        layoutItem.setLayoutParams(layoutParams);
        layoutItem.setOrientation(LinearLayout.VERTICAL);
        layoutItem.setBackground(AppCompatResources.getDrawable(AuthorizationListActivity.this, R.drawable.shape_list_item_bg));
        TextView textView = new TextView(AuthorizationListActivity.this);
        textView.setText(res);
        textView.setTextColor(getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(layoutParams);
        layoutItem.addView(textView);
        textView = new MarqueeTextView(AuthorizationListActivity.this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.colorLabelText));
        textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMarqueeRepeatLimit(Integer.MAX_VALUE);
        textView.setLayoutParams(layoutParams);
        textView.setSingleLine(true);
        layoutItem.addView(textView);
        return layoutItem;
    }

    private void prepareDownload() {
        if (null == enterpriseBean || enterpriseBean.getCode().isEmpty()) {
            myApp.myToast(AuthorizationListActivity.this, R.string.message_select_enterprise);
            Intent intent = new Intent(AuthorizationListActivity.this, InfoListActivity.class);
            intent.putExtra(KeyUtils.KEY_INFO_TYPE, ConstantUtils.INFO_ENTERPRISE);
            launcher.launch(new Intent(AuthorizationListActivity.this, EnterpriseActivity.class));
        } else {
            final LinearLayout layout = new LinearLayout(AuthorizationListActivity.this);
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(newItem(R.string.enterprise_code, enterpriseBean.getCode()));
            layout.addView(newItem(R.string.enterprise_id, enterpriseBean.getBlasterId()));
            if (enterpriseBean.isCommercial()) {
                layout.addView(newItem(R.string.enterprise_contract_code, enterpriseBean.getContract()));
                layout.addView(newItem(R.string.enterprise_project_code, enterpriseBean.getProject()));
            }
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(2, 5, 2, 2);
            layoutParams.gravity = Gravity.CENTER;
            TextView textView = new TextView(AuthorizationListActivity.this);
            textView.setText(enterpriseBean.isCommercial() ? R.string.enterprise_commercial : R.string.enterprise_not_commercial);
            textView.setTextColor(AuthorizationListActivity.this.getColor(R.color.colorLabelText));
            textView.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(layoutParams);
            layout.addView(textView);
            enterpriseDialog = new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.settings_enterprise)
                    .setView(layout)
                    .setNegativeButton(R.string.button_cancel, null)
                    .setPositiveButton(R.string.button_confirm, (dialogInterface, i) -> {
                        enabledButton(false);
                        StringBuilder str = new StringBuilder();
                        for (DetonatorBean bean : list) {
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
                                            enabledButton(true);
                                        }

                                        @Override
                                        public void onResponse(DownloadDetonatorBean downloadDetonatorBean, int i) {
                                            enabledButton(true);
                                            if (null != downloadDetonatorBean) {
                                                if (downloadDetonatorBean.getToken().equals(token)) {
                                                    if (downloadDetonatorBean.isStatus()) {
                                                        if (null != downloadDetonatorBean.getResult()) {
                                                            if (downloadDetonatorBean.getResult().getCwxx().equals("0")) {
                                                                List<LgBean> detonators = downloadDetonatorBean.getResult().getLgs().getLg();
                                                                if (null != detonators) {
                                                                    DbUtil.updateDownloadDetonator(true, downloadDetonatorBean);
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
                                                    } else
                                                        myApp.myToast(AuthorizationListActivity.this, downloadDetonatorBean.getDescription());
                                                } else
                                                    myApp.myToast(AuthorizationListActivity.this, R.string.message_token_error);
                                            } else
                                                myApp.myToast(AuthorizationListActivity.this, R.string.message_return_data_error);
                                        }
                                    });
                        }
                    })
                    .setNeutralButton(R.string.button_modify, (dialogInterface, i) -> {
                        enabledButton(true);
                        launcher.launch(new Intent(AuthorizationListActivity.this, EnterpriseActivity.class));
                    })
                    .show();
            BaseApplication.customDialog(enterpriseDialog);
        }
    }

    private void manualAppend() {
        final View inputCodeView = LayoutInflater.from(AuthorizationListActivity.this).inflate(R.layout.layout_dialog_add_detonator, listView, false);
        final EditText etAmount = inputCodeView.findViewById(R.id.et_amount);
        final CheckBox cbImport = inputCodeView.findViewById(R.id.cb_import);
        etStart = inputCodeView.findViewById(R.id.et_start);
        cbImport.setEnabled(detonatorList.size() > 0 && checkImport());
        cbImport.setOnCheckedChangeListener((compoundButton, b) -> {
            etStart.setEnabled(!b);
            etAmount.setEnabled(!b);
        });
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
        etAmount.setHint("100");
        etStart.requestFocus();
        final AlertDialog addDialog = new AlertDialog.Builder(AuthorizationListActivity.this, R.style.AlertDialog)
                .setTitle(R.string.dialog_title_manual_input)
                .setView(inputCodeView)
                .setPositiveButton(R.string.button_confirm, (dialogInterface, ii) -> confirmInput(cbImport.isChecked(), etAmount.getText().toString()))
                .setNegativeButton(R.string.button_cancel, null)
                .setOnDismissListener(dialogInterface -> etStart = null)
                .setOnKeyListener((dialogInterface, i, keyEvent) -> {
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP)
                        switch (keyEvent.getKeyCode()) {
                            case ConstantUtils.KEYCODE_CENTER_SCAN:
                            case ConstantUtils.KEYCODE_RIGHT_SCAN:
                            case ConstantUtils.KEYCODE_LEFT_SCAN:
                                serialPortUtil.sendCmd("", SerialCommand.CODE_SCAN_CODE, ConstantUtils.SCAN_CODE_TIME);
                                break;
                            case KeyEvent.KEYCODE_DPAD_CENTER:
                                confirmInput(cbImport.isChecked(), etAmount.getText().toString());
                                break;
                        }
                    return false;
                })
                .show();
        addDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
        addDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
    }

    private boolean checkImport() {
        for (DetonatorBean bean : detonatorList)
            if (!list.contains(bean))
                return true;
        return false;
    }

    private void confirmInput(boolean isImport, String amount) {
        if (isImport) {
            if (detonatorList.size() > 0) {
                boolean success = false;
                for (DetonatorBean bean : detonatorList)
                    if (!list.contains(bean)) {
                        success = true;
                        bean.setDownloaded(false);
                        list.add(bean);
                    }
                if (success) {
                    list.sort(Comparator.comparing(DetonatorBean::getAddress));
                    myApp.myToast(AuthorizationListActivity.this, R.string.message_restore_success);
                    adapter.updateList(list);
                    btnDownload.setEnabled(true);
                    btnDelete.setEnabled(true);
                    DbUtil.updateAuthDetonatorList(list);
                }
            }
        } else if (Pattern.matches(ConstantUtils.SHELL_PATTERN, etStart.getText().toString().toUpperCase())) {
            try {
                int j = amount.isEmpty() ? 100 : Integer.parseInt(amount);
                for (int i = 0; i < j; i++) {
                    DetonatorBean bean = new DetonatorBean(etStart.getText().toString().substring(0, 8).toUpperCase()
                            + String.format(Locale.getDefault(), "%05d", (Integer.parseInt(etStart.getText().toString().substring(8)) + i) % 100000));
                    bean.setDownloaded(false);
                    list.add(bean);
                }
                adapter.updateList(list);
                DbUtil.updateAuthDetonatorList(list);
                btnDownload.setEnabled(true);
                btnDelete.setEnabled(true);
            } catch (Exception e) {
                myApp.myToast(AuthorizationListActivity.this, R.string.message_amount_input_error);
            }
        } else
            myApp.myToast(AuthorizationListActivity.this, R.string.message_detonator_input_error);
    }

    private void enabledButton(boolean enabled) {
        runOnUiThread(() -> {
            setProgressVisibility(!enabled);
            btnDownload.setEnabled(enabled && list.size() > 0);
            btnDelete.setEnabled(enabled && list.size() > 0);
            btnAdd.setEnabled(enabled);
        });
    }

    private void checkList(List<LgBean> detonators) {
        for (LgBean bean : detonators) {
            for (DetonatorBean bean1 : list) {
                if (bean.getFbh().equals(bean1.getAddress())) {
                    bean1.setDownloaded(true);
                    bean1.setRow(Integer.parseInt(bean.getGzmcwxx()));
                }
            }
        }
        adapter.updateList(list);
        DbUtil.updateAuthDetonatorList(list);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if ((enterpriseDialog == null || !enterpriseDialog.isShowing()) && event.getAction() == KeyEvent.ACTION_UP)
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                    btnAdd.callOnClick();
                    return true;
                case KeyEvent.KEYCODE_TAB:
                    btnDownload.callOnClick();
                    return true;
                case ConstantUtils.KEYCODE_SUB:
                    btnDelete.callOnClick();
                    return true;
                case KeyEvent.KEYCODE_2:
                    listView.requestFocus();
                    if (listView.getSelectedItemPosition() > 0) {
                        listView.setSelection(listView.getSelectedItemPosition() - 1);
                    } else
                        listView.setSelection(list.size() - 1);
                    break;
                case KeyEvent.KEYCODE_8:
                    listView.requestFocus();
                    if (listView.getSelectedItemPosition() < list.size() - 1) {
                        listView.setSelection(listView.getSelectedItemPosition() + 1);
                    } else
                        listView.setSelection(0);
                    break;
            }
        else if (event.getAction() == KeyEvent.ACTION_DOWN)
            switch (event.getKeyCode()) {
                case ConstantUtils.KEYCODE_ADD:
                case KeyEvent.KEYCODE_TAB:
                case ConstantUtils.KEYCODE_SUB:
                    return true;
            }
        return super.dispatchKeyEvent(event);
    }

    private void initSound() {
        soundPool = myApp.getSoundPool();
        if (null != soundPool) {
            soundSuccess = soundPool.load(this, R.raw.found, 1);
            soundFail = soundPool.load(this, R.raw.fail, 1);
        }
    }

    @Override
    protected void onDestroy() {
        if (null != soundPool) {
            soundPool.autoPause();
            soundPool.unload(soundSuccess);
            soundPool.unload(soundFail);
            soundPool.release();
            soundPool = null;
        }
        myReceiveListener.closeAllHandler();
        super.onDestroy();
    }
}
