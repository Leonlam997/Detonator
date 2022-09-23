package com.leon.detonator.activity;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.TextView;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;

public class ServerSelectActivity extends BaseActivity {
    private RadioButton[] radioButtons;
    private LocalSettingBean settingBean;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_select);

        setTitle(R.string.settings_server);
        myApp = (BaseApplication) getApplication();
        settingBean = BaseApplication.readSettings();
        radioButtons = new RadioButton[ConstantUtils.UPLOAD_HOST.length];
        TextView[] textViews = new TextView[ConstantUtils.UPLOAD_HOST.length];
        radioButtons[0] = findViewById(R.id.rb_0);
        radioButtons[1] = findViewById(R.id.rb_1);
        radioButtons[2] = findViewById(R.id.rb_2);
        radioButtons[3] = findViewById(R.id.rb_3);
        radioButtons[4] = findViewById(R.id.rb_4);
        radioButtons[5] = findViewById(R.id.rb_5);
        textViews[0] = findViewById(R.id.tv_0);
        textViews[1] = findViewById(R.id.tv_1);
        textViews[2] = findViewById(R.id.tv_2);
        textViews[3] = findViewById(R.id.tv_3);
        textViews[4] = findViewById(R.id.tv_4);
        textViews[5] = findViewById(R.id.tv_5);
        textViews[0].setText("");
        for (int i = 0; i < ConstantUtils.UPLOAD_HOST.length; i++) {
            radioButtons[i].setText(ConstantUtils.UPLOAD_HOST[i][0]);
            radioButtons[i].setChecked(i == settingBean.getServerHost());
            //textViews[i + 1].setText(ConstantUtils.ZHONGBAO_HOST[i][1]);
        }
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < radioButtons.length; i++)
            if (radioButtons[i].isChecked()) {
                settingBean.setServerHost(i);
                break;
            }
        myApp.saveSettings(settingBean);
        super.onDestroy();
    }
}
