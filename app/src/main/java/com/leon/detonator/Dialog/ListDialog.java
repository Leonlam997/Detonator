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

import com.leon.detonator.Adapter.ListDialogAdapter;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.R;

import java.util.List;

public class ListDialog extends Dialog {
    private final Context mContext;
    private ListDialogAdapter adapter;
    private List<DetonatorInfoBean> list;
    private View.OnClickListener listener1, listener2;
    private final Handler refresh = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    adapter.notifyDataSetChanged();
                    break;
                case 2:
                    if (findViewById(R.id.btn_dialog1) != null)
                        findViewById(R.id.btn_dialog1).setOnClickListener(listener1);
                    break;
                case 3:
                    if (findViewById(R.id.btn_dialog2) != null)
                        findViewById(R.id.btn_dialog2).setOnClickListener(listener2);
                    break;
            }
            return false;
        }
    });
    private boolean tunnel;

    public ListDialog(@NonNull Context context, List<DetonatorInfoBean> list) {
        super(context);
        mContext = context;
        this.list = list;
    }

    public void setList(List<DetonatorInfoBean> list) {
        this.list = list;
        if (isShowing()) {
            refresh.sendEmptyMessage(1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list_dialog);
        if (getWindow() != null) {
            final WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = 380;
            params.height = 260;
            getWindow().setAttributes(params);
        }

        findViewById(R.id.table_title).setBackgroundColor(mContext.getColor(R.color.colorTableTitleBackground));
        adapter = new ListDialogAdapter(mContext, list);
        ListView tableListView = findViewById(R.id.lv_list);
        String title = mContext.getResources().getText(R.string.dialog_title_wrong_list) + "(" + list.size() + ")";
        ((TextView) findViewById(R.id.dialog_title)).setText(title);
        adapter.setTunnel(tunnel);
        if (tunnel) {
            findViewById(R.id.line_row).setVisibility(View.GONE);
            findViewById(R.id.text_row).setVisibility(View.GONE);

            ((TextView) findViewById(R.id.text_hole)).setText(R.string.table_section);
            ((TextView) findViewById(R.id.text_inside)).setText(R.string.table_section_inside);
        }
        tableListView.setAdapter(adapter);
        if (listener1 != null)
            findViewById(R.id.btn_dialog1).setOnClickListener(listener1);
        if (listener2 != null)
            findViewById(R.id.btn_dialog2).setOnClickListener(listener2);
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

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                if (listener1 != null)
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

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }
}
