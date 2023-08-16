package com.leon.detonator.activity;

import android.os.Bundle;
import android.widget.RadioButton;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.ConstantUtils;

public class ServerSelectActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_select);
        setTitle(R.string.settings_server);
        RadioButton[] radioButtons = new RadioButton[ConstantUtils.UPLOAD_HOST.length];
        radioButtons[0] = findViewById(R.id.rb_0);
        radioButtons[1] = findViewById(R.id.rb_1);
        radioButtons[2] = findViewById(R.id.rb_2);
        radioButtons[3] = findViewById(R.id.rb_3);
        radioButtons[4] = findViewById(R.id.rb_4);
        radioButtons[5] = findViewById(R.id.rb_5);
        radioButtons[6] = findViewById(R.id.rb_6);
        radioButtons[7] = findViewById(R.id.rb_7);
        for (int i = 0; i < ConstantUtils.UPLOAD_HOST.length; i++) {
            radioButtons[i].setText(ConstantUtils.UPLOAD_HOST[i][0]);
            radioButtons[i].setChecked(i == BaseApplication.settings.getServerHost());
            int finalI = i;
            radioButtons[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked)
                    BaseApplication.settings.setServerHost(finalI);
            });
        }
    }

    @Override
    protected void onDestroy() {
        myApp.saveSettings();
        super.onDestroy();
    }
}
