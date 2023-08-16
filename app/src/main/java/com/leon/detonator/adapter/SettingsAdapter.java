package com.leon.detonator.adapter;

import static android.content.Context.WIFI_SERVICE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.SettingsBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leon on 2018/3/14.
 */

public class SettingsAdapter extends BaseAdapter {
    private final List<SettingsBean> list;
    private final LayoutInflater inflater;
    private WifiManager wifiManager;
    private BluetoothAdapter btAdapter;
    private CheckBox cbWifi;
    private CheckBox cbBT;
    private final Handler refresh = new Handler(message -> {
        switch (message.what) {
            case 1:
                cbWifi.setEnabled(true);
                break;
            case 2:
                cbBT.setEnabled(true);
                break;
        }
        return false;
    });

    public SettingsAdapter(Context context, List<SettingsBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<SettingsBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SettingsBean settingsBean = (SettingsBean) this.getItem(position);
        SettingsAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_settings, parent, false);
            viewHolder.menuIcon = convertView.findViewById(R.id.iv_menu_icon);
            viewHolder.title = convertView.findViewById(R.id.tv_title);
            viewHolder.subMenu = convertView.findViewById(R.id.iv_more);
            viewHolder.cbMenu = convertView.findViewById(R.id.cb_menu);
            viewHolder.subtitle = convertView.findViewById(R.id.tv_subtitle);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (SettingsAdapter.ViewHolder) convertView.getTag();
        viewHolder.menuIcon.setImageResource(settingsBean.getIcon());
        viewHolder.title.setText(settingsBean.getTitle());
        viewHolder.subMenu.setVisibility(settingsBean.isMore() ? View.VISIBLE : View.GONE);
        viewHolder.cbMenu.setVisibility(settingsBean.isCheckBox() ? View.VISIBLE : View.GONE);
        viewHolder.subtitle.setVisibility(settingsBean.getSubtitle() == null || settingsBean.getSubtitle().isEmpty() ? View.INVISIBLE : View.VISIBLE);
        viewHolder.subtitle.setText(settingsBean.getSubtitle());
        if (settingsBean.isCheckBox()) {
            if (0 == position) {
                wifiManager = (WifiManager) inflater.getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
                cbWifi = viewHolder.cbMenu;
                viewHolder.cbMenu.setChecked(null != wifiManager && wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
                viewHolder.cbMenu.setOnClickListener(view -> {
                    if (null != wifiManager) {
                        BaseApplication.writeFile(inflater.getContext().getString(R.string.settings_wifi) + ", " + cbWifi.isChecked());
                        wifiManager.setWifiEnabled(cbWifi.isChecked());
                        cbWifi.setEnabled(false);
                        new CheckWifi(cbWifi.isChecked() ? WifiManager.WIFI_STATE_ENABLED : WifiManager.WIFI_STATE_DISABLED).start();
                    }
                });
            } else if (1 == position) {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                cbBT = viewHolder.cbMenu;
                viewHolder.cbMenu.setChecked(btAdapter.isEnabled());
                viewHolder.cbMenu.setOnClickListener(view -> {
                    BaseApplication.writeFile(inflater.getContext().getString(R.string.settings_bt) + ", " + cbBT.isChecked());
                    if (cbBT.isChecked())
                        btAdapter.enable();
                    else
                        btAdapter.disable();
                    cbBT.setEnabled(false);
                    new CheckBT(cbBT.isChecked()).start();
                });
            }
        }
        return convertView;
    }

    private static class ViewHolder {
        ImageView menuIcon;
        TextView title;
        TextView subtitle;
        ImageView subMenu;
        CheckBox cbMenu;
    }

    private class CheckWifi extends Thread {
        private final int status;

        CheckWifi(int status) {
            this.status = status;
        }

        @Override
        public void run() {
            while (wifiManager.getWifiState() != status) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
            refresh.sendEmptyMessage(1);
            super.run();
        }
    }

    private class CheckBT extends Thread {
        private final boolean status;

        CheckBT(boolean status) {
            this.status = status;
        }

        @Override
        public void run() {
            while (status != btAdapter.isEnabled()) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
            refresh.sendEmptyMessage(2);
            super.run();
        }
    }
}
