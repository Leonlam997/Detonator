package com.leon.detonator.activity;

import android.os.Bundle;
import android.widget.RadioButton;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;

public class SelectVoltageActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_voltage);
        setTitle(R.string.hide_set_voltage);
        if (BaseApplication.settings.getWorkVoltage() == 12)
            ((RadioButton) findViewById(R.id.rb_12)).setChecked(true);
        else
            ((RadioButton) findViewById(R.id.rb_16)).setChecked(true);
        if (BaseApplication.settings.getChargeVoltage() == 18)
            ((RadioButton) findViewById(R.id.rb_18)).setChecked(true);
        else if (BaseApplication.settings.getChargeVoltage() == 20)
            ((RadioButton) findViewById(R.id.rb_20)).setChecked(true);
        else
            ((RadioButton) findViewById(R.id.rb_22)).setChecked(true);
    }

    @Override
    protected void onDestroy() {
        BaseApplication.settings.setWorkVoltage(((RadioButton) findViewById(R.id.rb_12)).isChecked() ? 12 : 16);
        BaseApplication.settings.setChargeVoltage(((RadioButton) findViewById(R.id.rb_18)).isChecked() ? 18 : (((RadioButton) findViewById(R.id.rb_20)).isChecked() ? 20 : 22));
        ((BaseApplication) getApplication()).saveSettings();
        super.onDestroy();
    }
}