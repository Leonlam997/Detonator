package com.leon.detonator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.FilePath;

import java.util.ArrayList;
import java.util.List;

public class HideTestActivity extends BaseActivity implements View.OnClickListener {
    private MyButton[] btnFunctions;
    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case BaseApplication.HANDLER_REGISTER_ERROR:
                if (msg.obj == null)
                    myApp.myToast(HideTestActivity.this, msg.arg1 == 0 ? R.string.message_registered_fail : msg.arg1 == 3 ? R.string.message_upload_log_fail : R.string.progress_upload);
                else
                    myApp.myToast(HideTestActivity.this, (String) msg.obj);
                btnFunctions[3].setEnabled(true);
                break;
            case BaseApplication.HANDLER_REGISTER_SUCCESS:
                myApp.myToast(HideTestActivity.this, msg.arg1 == 0 ? R.string.message_registered_success : R.string.message_upload_success);
                btnFunctions[3].setEnabled(true);
                break;
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hide_test);
        myApp = (BaseApplication) getApplication();
        setTitle(R.string.hide_test_title);
        btnFunctions = new MyButton[8];
        btnFunctions[0] = findViewById(R.id.btn_semi_product);
        btnFunctions[1] = findViewById(R.id.btn_write_number);
        btnFunctions[2] = findViewById(R.id.btn_upload_log);
        btnFunctions[3] = findViewById(R.id.btn_unregister);
        btnFunctions[4] = findViewById(R.id.btn_universal);
        btnFunctions[5] = findViewById(R.id.btn_serial);
        btnFunctions[6] = findViewById(R.id.btn_voltage);
        btnFunctions[7] = findViewById(R.id.btn_import);
        for (MyButton button : btnFunctions)
            button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        for (int i = 0; i < btnFunctions.length; i++)
            if (v.getId() == btnFunctions[i].getId()) {
                function(i);
                break;
            }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode < KeyEvent.KEYCODE_1 + btnFunctions.length)
            function(keyCode - KeyEvent.KEYCODE_1);
        return super.onKeyUp(keyCode, event);
    }

    public void function(int which) {
        switch (which) {
            case 0:
                startActivity(new Intent(HideTestActivity.this, SemiProductActivity.class));
                break;
            case 1:
                startActivity(new Intent(HideTestActivity.this, WriteSNActivity.class));
                break;
            case 2:
                if (btnFunctions[2].isEnabled())
                    if (BaseApplication.isNetSystemUsable(this)) {
                        runOnUiThread(() -> {
                            setProgressVisibility(true);
                            btnFunctions[2].setEnabled(false);
                        });
                        myApp.myToast(HideTestActivity.this, R.string.message_upload_log);
                        myApp.uploadLog(myHandler);
                    } else
                        myApp.myToast(HideTestActivity.this, R.string.message_check_network);
                break;
            case 3:
                if (btnFunctions[3].isEnabled())
                    if (BaseApplication.isNetSystemUsable(this)) {
                        BaseApplication.settings.setRegistered(false);
                        runOnUiThread(() -> {
                            setProgressVisibility(true);
                            btnFunctions[3].setEnabled(false);
                        });
                        BaseApplication.saveSettings();
                        myApp.registerExploder(myHandler);
                        myApp.myToast(HideTestActivity.this, R.string.message_register_detonator);
                    } else
                        myApp.myToast(HideTestActivity.this, R.string.message_check_network);
                break;
            case 4:
                startActivity(new Intent(HideTestActivity.this, UniversalTest.class));
                break;
            case 5:
                startActivity(new Intent(HideTestActivity.this, PsamDemoActivity.class));
                break;
            case 6:
                startActivity(new Intent(HideTestActivity.this, SelectVoltageActivity.class));
                break;
            case 7:
                List<DetonatorBean> list = new ArrayList<>();
                myApp.readFromFile(BaseApplication.settings.isTunnel() ? FilePath.FILE_TUNNEL_DELAY_LIST : FilePath.FILE_OPEN_AIR_DELAY_LIST, list, DetonatorBean.class);
                if (list.size() == 0)
                    myApp.myToast(HideTestActivity.this, R.string.message_import_fail);
                else {
                    SchemeBean bean = new SchemeBean();
                    bean.setName(getString(R.string.button_restore_list));
                    DbUtil.updateScheme(bean);
                    for (DetonatorBean b : list)
                        b.setSchemeId(bean.getId());
                    DbUtil.updateDetonatorList(list);
                    myApp.myToast(HideTestActivity.this, R.string.message_import_success);
                }
                break;
        }
    }
}