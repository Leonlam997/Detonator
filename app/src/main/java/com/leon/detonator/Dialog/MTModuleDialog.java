package com.leon.detonator.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.leon.detonator.Adapter.MTModuleListAdapter;
import com.leon.detonator.R;
import com.minew.modulekit.MTModule;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MTModuleDialog extends Dialog {
    private final List<MTModule> list;
    private final Context context;
    private final String mac;
    private MTModuleListAdapter adapter;
    private MTModule selectedMTModule;
    private View.OnClickListener listener1, listener2;
    private final Handler refresh = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {

            switch (message.what) {
                case 1:
                    //adapter.notifyDataSetChanged();
                    String title = context.getResources().getText(R.string.dialog_device_title) + "(" + list.size() + ")";
                    ((TextView) findViewById(R.id.dialog_title)).setText(title);
                    selectList();
                    break;
                case 2:
                    if (findViewById(R.id.btn_dialog1) != null)
                        findViewById(R.id.btn_dialog1).setOnClickListener(listener1);
                    break;
                case 3:
                    if (findViewById(R.id.btn_dialog2) != null)
                        findViewById(R.id.btn_dialog2).setOnClickListener(listener2);
                    break;
                case 4:
                    findViewById(R.id.btn_dialog1).setEnabled(true);
                    break;
                case 5:
                    findViewById(R.id.btn_dialog1).setEnabled(false);
                    break;
            }
            return false;
        }
    });

    public MTModuleDialog(@NonNull Context context, List<MTModule> list, String mac) {
        super(context);
        this.context = context;
        this.list = filterList(list);
        this.mac = mac;
    }

    private List<MTModule> filterList(List<MTModule> list) {
        List<MTModule> newList = new ArrayList<>();
        for (MTModule m : list) {
            if (m.getName().contains("Minew"))
                newList.add(m);
        }
        return newList;
    }

    public void setList(List<MTModule> list) {
        this.list.clear();
        this.list.addAll(filterList(list));
        adapter.updateList(this.list);
        if (isShowing()) {
            refresh.sendEmptyMessage(1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mtmodule_dialog);
        if (getWindow() != null) {
            final WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = 380;
            params.height = 260;
            getWindow().setAttributes(params);
        }
        String title = context.getResources().getText(R.string.dialog_device_title) + "(" + list.size() + ")";
        ((TextView) findViewById(R.id.dialog_title)).setText(title);
        findViewById(R.id.table_title).setBackgroundColor(context.getColor(R.color.colorTableTitleBackground));
        adapter = new MTModuleListAdapter(context, list, mac);
        ListView listView = findViewById(R.id.lv_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            if (i >= 0 && i < list.size()) {
                selectedMTModule = list.get(i);
                refresh.sendEmptyMessage(4);
            } else
                refresh.sendEmptyMessage(5);
        });

        findViewById(R.id.btn_dialog1).setEnabled(false);
        if (null != selectedMTModule || null != mac)
            selectList();

        if (listener1 != null)
            findViewById(R.id.btn_dialog1).setOnClickListener(listener1);
        if (listener2 != null)
            findViewById(R.id.btn_dialog2).setOnClickListener(listener2);
    }

    private void selectList() {
        for (int i = 0; i < list.size(); i++)
            if (null == selectedMTModule && null != mac && list.get(i).getMacAddress().equals(mac)) {
                ((ListView) findViewById(R.id.lv_list)).setSelection(i);
                selectedMTModule = list.get(i);
                findViewById(R.id.btn_dialog1).setEnabled(true);
                break;
            } else if (null != selectedMTModule && list.get(i).getMacAddress().equals(selectedMTModule.getMacAddress())) {
                ((ListView) findViewById(R.id.lv_list)).setSelection(i);
                break;
            }
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                if (listener1 != null && findViewById(R.id.btn_dialog1).isEnabled())
                    listener1.onClick(findViewById(R.id.btn_dialog1));
                break;
            case KeyEvent.KEYCODE_2:
                if (listener2 != null)
                    listener2.onClick(findViewById(R.id.btn_dialog2));
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void setListener1(View.OnClickListener listener1) {
        this.listener1 = listener1;
        refresh.sendEmptyMessage(2);
    }

    public void setListener2(View.OnClickListener listener2) {
        this.listener2 = listener2;
        refresh.sendEmptyMessage(3);
    }

    public MTModule getSelectedMTModule() {
        return selectedMTModule;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (null != getWindow()) {
            View decorView = getWindow().getDecorView();
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
