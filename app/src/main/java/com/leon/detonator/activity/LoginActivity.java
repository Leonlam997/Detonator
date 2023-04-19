package com.leon.detonator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;

import com.google.gson.Gson;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.EnterpriseProjectBean;
import com.leon.detonator.bean.EnterpriseUserBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Response;

public class LoginActivity extends BaseActivity {
    private final int pageSize = 1;
    private final int userID = 0;
    private EditText etUser, etPwd;
    private ImageButton btnLogin;
    private BaseApplication myApp;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode() && null != result.getData()) {
            myApp.myToast(LoginActivity.this,
                    String.format(Locale.CHINA, getString(R.string.message_user_login_success), result.getData().getStringExtra(KeyUtils.KEY_USER_NAME)));
            Intent intent = new Intent(LoginActivity.this, SelectModeActivity.class);
            startActivity(intent);
            finish();
        }
    });
    private final Handler respondUI = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case 2:
                    btnLogin.setEnabled(true);
                    break;
                case 3:
                    btnLogin.setEnabled(false);
                    break;
                default:
                    myApp.myToast(LoginActivity.this, (String) msg.obj);
            }
            return false;
        }
    });
    private String token;
    private List<EnterpriseUserBean.ResultBean.PageListBean> userList;
    private List<EnterpriseProjectBean.ResultBean.PageListBean> projectList;
    private int pageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        hideActionBar();
        myApp = (BaseApplication) getApplication();
        btnLogin = findViewById(R.id.btn_login);
        etUser = findViewById(R.id.et_username);
        etPwd = findViewById(R.id.et_password);
        etUser.requestFocus();
        projectList = new ArrayList<>();

        ((CheckBox) findViewById(R.id.cb_show_psw)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            etPwd.setInputType(isChecked ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) :
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            etPwd.setSelection(etPwd.getText().length());
        });

        findViewById(R.id.btn_face).setOnClickListener(view -> launcher.launch(new Intent(LoginActivity.this, CameraActivity.class)));

        btnLogin.setOnClickListener(v -> {
            if (etUser.getText().toString().isEmpty()) {
                Intent intent = new Intent(LoginActivity.this, SelectModeActivity.class);
                startActivity(intent);
                finish();
                //sendMsg(1, "用户名不能为空！");
            } else if (etPwd.getText().toString().isEmpty()) {
                sendMsg(R.string.message_not_allow_empty_password);
            }
//                } else {
//                    boolean invalid = true;
//                    List<EnterpriseUserBean.ResultBean.PageListBean> userList = myApp.readUserList();
//                    if (userList != null) {
//                        for (EnterpriseUserBean.ResultBean.PageListBean bean : userList) {
//                            if (bean.getAccount().toUpperCase().equals(etUser.getText().toString().toUpperCase()) && bean.getPassword().equals(MD5.encryptTo16BitString(etPwd.getText().toString()))) {
//                                if (bean.isIsLock()) {
//                                    sendMsg(1, "用户：\"" + etUser.getText().toString() + "\"已经被锁！");
//                                } else {
//                                    userID = bean.getUserID();
//                                    LocalSettingBean settingBeans = BaseApplication.readSettings();
//                                    settingBeans.setUserID(userID);
//                                    myApp.saveBean(settingBeans);
//                                    projectList = new ArrayList<>();
//                                    sendMsg(3, "");
//                                    new GetEnterpriseProject().start();
//                                }
//                                invalid = false;
//                                break;
//                            }
//                        }
//                    }
//                    if (invalid)
//                        sendMsg(1, "用户名或密码不正确！");
//                }
        });

        //btnLogin.setEnabled(false);
        pageIndex = 0;
        userList = new ArrayList<>();
        //new GetEnterpriseUser().start();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            Intent intent = new Intent(LoginActivity.this, SelectModeActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void sendMsg(int what, String hint) {
        Message msg = respondUI.obtainMessage(what);
        if (!hint.isEmpty()) {
            msg.obj = hint;
        }
        respondUI.sendMessage(msg);
    }

    private void sendMsg(@StringRes int hint) {
        Message msg = respondUI.obtainMessage(1);
        msg.obj = getString(hint);
        respondUI.sendMessage(msg);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private class GetEnterpriseUser extends Thread {
        @Override
        public void run() {
            super.run();
            token = myApp.makeToken();
            Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_GET_USERS);
            if (null != params) {
                params.put("pageindex", String.valueOf(++pageIndex));
                params.put("pagesize", String.valueOf(pageSize));
                params.put("signature", myApp.signature(params));
            }

            OkHttpUtils.post()
                    .url(ConstantUtils.HOST_URL)
                    .params(params)
                    .build().execute(new Callback<EnterpriseUserBean>() {
                        @Override
                        public EnterpriseUserBean parseNetworkResponse(Response response, int i) throws Exception {
                            if (response.body() != null) {
                                String string = Objects.requireNonNull(response.body()).string();
                                return BaseApplication.jsonFromString(string, EnterpriseUserBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            sendMsg(R.string.message_check_network);
                        }

                        @Override
                        public void onResponse(EnterpriseUserBean userBean, int i) {
                            if (userBean != null && userBean.isStatus() && userBean.getToken().equals(token)) {
                                if (userBean.getResult() != null) {
                                    userList.addAll(userBean.getResult().getPageList());
                                    if (userBean.getResult().getPageTotal() > userBean.getResult().getPageIndex()) {
                                        new GetEnterpriseProject().start();
                                    } else {
                                        try {
                                            FileWriter fw = new FileWriter(FilePath.FILE_USER_INFO);
                                            fw.append(new Gson().toJson(userList));
                                            fw.close();
                                        } catch (Exception e) {
                                            BaseApplication.writeErrorLog(e);
                                        }
                                        sendMsg(2, "");
                                    }
                                }
                            } else {
                                if (userBean != null)
                                    sendMsg(1, userBean.getDescription());
                                else
                                    sendMsg(R.string.message_token_error);
                            }
                        }
                    });
        }
    }

    private class GetEnterpriseProject extends Thread {
        @Override
        public void run() {
            super.run();
            token = myApp.makeToken();
            Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_GET_PROJECT);
            if (null != params) {
                params.put("pageindex", String.valueOf(++pageIndex));
                params.put("pagesize", String.valueOf(pageSize));
                params.put("userid", String.valueOf(userID));
                params.put("signature", myApp.signature(params));
            }
            OkHttpUtils.post()
                    .url(ConstantUtils.HOST_URL)
                    .params(params)
                    .build().execute(new Callback<EnterpriseProjectBean>() {
                        @Override
                        public EnterpriseProjectBean parseNetworkResponse(Response response, int i) throws Exception {
                            if (response.body() != null) {
                                String string = Objects.requireNonNull(response.body()).string();
                                return BaseApplication.jsonFromString(string, EnterpriseProjectBean.class);
                            }
                            return null;
                        }

                        @Override
                        public void onError(Call call, Exception e, int i) {
                            sendMsg(R.string.message_check_network);
                        }

                        @Override
                        public void onResponse(EnterpriseProjectBean enterpriseProjectBean, int i) {
                            if (enterpriseProjectBean != null && enterpriseProjectBean.isStatus() && enterpriseProjectBean.getToken().equals(token)) {
                                if (enterpriseProjectBean.getResult() != null) {
                                    if (enterpriseProjectBean.getResult().getPageList() != null && enterpriseProjectBean.getResult().getPageList().size() > 0)
                                        projectList.addAll(enterpriseProjectBean.getResult().getPageList());
                                    if (enterpriseProjectBean.getResult().getPageTotal() > enterpriseProjectBean.getResult().getPageIndex()) {
                                        new GetEnterpriseProject().start();
                                    } else {
                                        if (projectList != null && projectList.size() > 0) {
                                            try {
                                                FileWriter fw = new FileWriter(FilePath.FILE_PROJECT_INFO + userID + ".dat");
                                                fw.append(new Gson().toJson(projectList));
                                                fw.close();
                                            } catch (Exception e) {
                                                BaseApplication.writeErrorLog(e);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (enterpriseProjectBean != null)
                                    sendMsg(1, enterpriseProjectBean.getDescription());
                                else
                                    sendMsg(R.string.message_token_error);
                                sendMsg(2, "");
                            }
                        }
                    });
        }

    }
}
