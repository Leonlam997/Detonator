package com.leon.detonator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridLayout;

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

public class HideActivity extends BaseActivity implements View.OnClickListener {
    private MyButton[] buttons;
    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case BaseApplication.HANDLER_REGISTER_ERROR:
                if (msg.obj == null)
                    myApp.myToast(HideActivity.this, msg.arg1 == 0 ? R.string.message_registered_fail : msg.arg1 == 3 ? R.string.message_upload_log_fail : R.string.progress_upload);
                else
                    myApp.myToast(HideActivity.this, (String) msg.obj);
                break;
            case BaseApplication.HANDLER_REGISTER_SUCCESS:
                myApp.myToast(HideActivity.this, msg.arg1 == 0 ? R.string.message_registered_success : R.string.message_upload_success);
                break;
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hide);
        setTitle(R.string.hide_test_title);
        GridLayout gridLayout = findViewById(R.id.gl_hide_btn);
        buttons = new MyButton[gridLayout.getChildCount()];
        for (int i = 0; i < gridLayout.getChildCount(); i++)
            buttons[i] = (MyButton) gridLayout.getChildAt(i);
        for (MyButton button : buttons)
            button.setOnClickListener(this);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode - KeyEvent.KEYCODE_1 >= 0 && keyCode - KeyEvent.KEYCODE_1 < buttons.length)
            buttons[keyCode - KeyEvent.KEYCODE_1].callOnClick();
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        for (int i = 0; i < buttons.length; i++)
            if (v.getId() == buttons[i].getId()) {
                switch (i) {
                    case 0:
                        startActivity(new Intent(HideActivity.this, SemiProductActivity.class));
                        break;
                    case 1:
                        startActivity(new Intent(HideActivity.this, WriteSNActivity.class));
                        break;
                    case 2:
                        if (BaseApplication.isNetSystemUsable(this)) {
                            myApp.myToast(HideActivity.this, R.string.message_upload_log);
                            myApp.uploadLog(myHandler);
                        }
                        break;
                    case 3:
                        BaseApplication.settings.setRegistered(false);
                        myApp.saveSettings();
                        if (BaseApplication.isNetSystemUsable(this)) {
                            myApp.registerExploder(myHandler);
                            myApp.myToast(HideActivity.this, R.string.message_register_detonator);
                        } else
                            myApp.myToast(HideActivity.this, R.string.message_check_network);
                        break;
                    case 4:
                        List<DetonatorBean> list = new ArrayList<>();
                        myApp.readFromFile(BaseApplication.isTunnel ? FilePath.FILE_TUNNEL_DELAY_LIST : FilePath.FILE_OPEN_AIR_DELAY_LIST, list, DetonatorBean.class);
                        if (list.size() == 0)
                            myApp.myToast(HideActivity.this, R.string.message_import_fail);
                        else {
                            SchemeBean bean = new SchemeBean();
                            bean.setName(getString(R.string.button_restore_list));
                            bean.setSelected(DbUtil.getSchemeList().size()==0);
                            DbUtil.updateScheme(bean);
                            for (DetonatorBean b : list)
                                b.setSchemeId(bean.getId());
                            DbUtil.updateDetonatorList(list);
                            myApp.myToast(HideActivity.this, R.string.message_import_success);
                        }
                        break;
                    case 5:
                        List<DetonatorBean> list1 = DbUtil.getCurrentDetonatorList();
                        if (list1.size() == 0)
                            myApp.myToast(HideActivity.this, R.string.message_export_empty);
                        else {
                            myApp.writeToFile(BaseApplication.isTunnel ? FilePath.FILE_TUNNEL_DELAY_LIST : FilePath.FILE_OPEN_AIR_DELAY_LIST, list1);
                            myApp.myToast(HideActivity.this, R.string.message_export_success);
                        }
                        break;
                }
                break;
            }
    }
}