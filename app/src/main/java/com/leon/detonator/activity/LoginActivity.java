package com.leon.detonator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.gson.Gson;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.EnterpriseProjectBean;
import com.leon.detonator.bean.EnterpriseUserBean;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.KeyUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Response;

public class LoginActivity extends BaseActivity {
    private List<EnterpriseProjectBean.ResultBean.PageListBean> projectList;
    private List<EnterpriseUserBean.ResultBean.PageListBean> userList;
    private ImageButton btnLogin;
    private EditText etUser;
    private EditText etPwd;
    private String token;
    private final int pageSize = 1;
    private final int userID = 0;
    private int pageIndex;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode() && null != result.getData()) {
            myApp.myToast(LoginActivity.this,
                    String.format(Locale.getDefault(), getString(R.string.message_user_login_success), result.getData().getStringExtra(KeyUtils.KEY_USER_NAME)));
            Intent intent = new Intent(LoginActivity.this, SelectModeActivity.class);
            startActivity(intent);
            finish();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        hideActionBar();
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
                //myApp.myToast(LoginActivity.this, "用户名不能为空！");
            } else if (etPwd.getText().toString().isEmpty()) {
                myApp.myToast(LoginActivity.this, R.string.message_not_allow_empty_password);
            }
//                } else {
//                    boolean invalid = true;
//                    List<EnterpriseUserBean.ResultBean.PageListBean> userList = myApp.readUserList();
//                    if (userList != null) {
//                        for (EnterpriseUserBean.ResultBean.PageListBean bean : userList) {
//                            if (bean.getAccount().toUpperCase().equals(etUser.getText().toString().toUpperCase()) && bean.getPassword().equals(MD5.encryptTo16BitString(etPwd.getText().toString()))) {
//                                if (bean.isIsLock()) {
//                                    myApp.myToast(LoginActivity.this, "用户：\"" + etUser.getText().toString() + "\"已经被锁！");
//                                } else {
//                                    userID = bean.getUserID();
//                                    LocalSettingBean BaseApplication.settingss = BaseApplication.readSettings();
//                                    BaseApplication.settingss.setUserID(userID);
//                                    myApp.saveSettings(BaseApplication.settingss);
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
//                        myApp.myToast(LoginActivity.this, "用户名或密码不正确！");
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
                            myApp.myToast(LoginActivity.this, R.string.message_check_network);
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
                                        btnLogin.setEnabled(true);
                                    }
                                }
                            } else {
                                if (userBean != null)
                                    myApp.myToast(LoginActivity.this, userBean.getDescription());
                                else
                                    myApp.myToast(LoginActivity.this, R.string.message_token_error);
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
                            myApp.myToast(LoginActivity.this, R.string.message_check_network);
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
                                    myApp.myToast(LoginActivity.this, enterpriseProjectBean.getDescription());
                                else
                                    myApp.myToast(LoginActivity.this, R.string.message_token_error);
                                btnLogin.setEnabled(true);
                            }
                        }
                    });
        }

    }
}
