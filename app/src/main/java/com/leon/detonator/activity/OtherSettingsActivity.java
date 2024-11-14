package com.leon.detonator.activity;

import android.os.Bundle;
import android.widget.ListView;

import com.leon.detonator.R;
import com.leon.detonator.adapter.OtherSettingsAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.OtherSettingsBean;

import java.util.ArrayList;
import java.util.List;

public class OtherSettingsActivity extends BaseActivity {
    private OtherSettingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_settings);
        ListView listView = findViewById(R.id.lv_list);
        List<OtherSettingsBean> list = new ArrayList<>();
        OtherSettingsBean bean = new OtherSettingsBean();
        bean.setItem(getString(R.string.other_settings_sound));
        bean.setChecked(BaseApplication.settings.isTtsSpeak());
        list.add(bean);
        bean = new OtherSettingsBean();
        bean.setItem(getString(R.string.other_settings_count_down));
        bean.setChecked(BaseApplication.settings.isCountDown());
        list.add(bean);
        adapter = new OtherSettingsAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            list.get(i).setChecked(!list.get(i).isChecked());
            adapter.updateList(list);
        });
    }

    @Override
    protected void onDestroy() {
        List<OtherSettingsBean> list = adapter.getList();
        BaseApplication.settings.setTtsSpeak(list.get(0).isChecked());
        BaseApplication.settings.setCountDown(list.get(1).isChecked());
        BaseApplication.saveSettings();
        super.onDestroy();
    }
}