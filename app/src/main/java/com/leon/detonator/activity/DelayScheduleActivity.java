package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DelayScheduleActivity extends BaseActivity implements View.OnClickListener {
    private List<DetonatorInfoBean> list;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delay_schedule);

        setTitle(R.string.delay_scheme);
        myApp = (BaseApplication) getApplication();
        list = new ArrayList<>();
        findViewById(R.id.btn_new_schedule).setOnClickListener(this);
        findViewById(R.id.btn_new_schedule).requestFocus();
        findViewById(R.id.btn_check_schedule).setOnClickListener(this);
        findViewById(R.id.btn_check_detonator).setOnClickListener(this);
        TextView[] textViews = new TextView[]{
                findViewById(R.id.tv_new_schedule),
                findViewById(R.id.tv_check_schedule),
                findViewById(R.id.tv_single_detect)};
        String[] title = new String[]{
                getString(R.string.button_add_detonator),
                getString(R.string.button_check_schedule),
                getString(R.string.button_check_detonator)};
        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setText(String.format(Locale.CHINA, "%d.%s", i + 1, title[i]));
            textViews[i].setOnClickListener(this);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_new_schedule:
            case R.id.tv_new_schedule:
                launchTask(KeyEvent.KEYCODE_1);
                break;
            case R.id.btn_check_schedule:
            case R.id.tv_check_schedule:
                launchTask(KeyEvent.KEYCODE_2);
                break;
            case R.id.btn_check_detonator:
            case R.id.tv_single_detect:
                launchTask(KeyEvent.KEYCODE_3);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launchTask(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void launchTask(int which) {
        Intent intent = new Intent();
        switch (which) {
            case KeyEvent.KEYCODE_1:
                intent.setClass(DelayScheduleActivity.this, DetonatorListActivity.class);
                intent.putExtra(KeyUtils.KEY_CREATE_DELAY_LIST, ConstantUtils.RESUME_LIST);
                startActivity(intent);
                break;
            case KeyEvent.KEYCODE_2:
                myApp.readFromFile(myApp.getListFile(), list, DetonatorInfoBean.class);
                if (list.size() <= 0) {
                    myApp.myToast(DelayScheduleActivity.this, R.string.message_list_not_found);
                } else {
                    intent.setClass(DelayScheduleActivity.this, DetonatorListActivity.class);
                    intent.putExtra(KeyUtils.KEY_CREATE_DELAY_LIST, ConstantUtils.MODIFY_LIST);
                    startActivity(intent);
                }
                break;
            case KeyEvent.KEYCODE_3:
                intent.setClass(DelayScheduleActivity.this, CheckLineActivity.class);
                startActivity(intent);
                break;
        }
    }
}
