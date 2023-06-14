package com.leon.detonator.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.RadioButton;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.ConstantUtils;

public class ServerSelectActivity extends BaseActivity {
    private RadioButton[] radioButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_select);

        setTitle(R.string.settings_server);
        radioButtons = new RadioButton[ConstantUtils.UPLOAD_HOST.length];
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
            radioButtons[i].setOnCheckedChangeListener((compoundButton, b) -> {
                if (b)
                    BaseApplication.settings.setServerHost(finalI);
            });
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_2:
                for (int i = 0; i < radioButtons.length; i++)
                    if (radioButtons[i].isChecked()) {
                        radioButtons[i].setChecked(false);
                        if (i > 0)
                            radioButtons[i - 1].setChecked(true);
                        else
                            radioButtons[radioButtons.length - 1].setChecked(true);
                        break;
                    }
                break;
            case KeyEvent.KEYCODE_8:
                for (int i = 0; i < radioButtons.length; i++)
                    if (radioButtons[i].isChecked()) {
                        radioButtons[i].setChecked(false);
                        if (i < radioButtons.length - 1)
                            radioButtons[i + 1].setChecked(true);
                        else
                            radioButtons[0].setChecked(true);
                        break;
                    }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        BaseApplication.saveSettings();
        super.onDestroy();
    }
}
