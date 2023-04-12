package com.leon.detonator.adapter;

import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.SettingsBean;
import com.leon.detonator.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Leon on 2018/3/14.
 */

public class SettingsAdapter extends BaseAdapter {
    private final List<SettingsBean> list;
    private final LayoutInflater inflater;
    private CheckBox cbWifi, cbBT;
    private final Handler refresh = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            switch (message.what) {
                case 1:
                    cbWifi.setEnabled(true);
                    break;
                case 2:
                    cbBT.setEnabled(true);
                    break;
            }
            return false;
        }
    });
    private WifiManager wifiManager;
    private BluetoothAdapter btAdapter;

    public SettingsAdapter(Context context, List<SettingsBean> list) {
        this.list = list;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        int ret = 0;
        if (list != null) {
            ret = list.size();
        }
        return ret;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SettingsBean settingsBean = (SettingsBean) this.getItem(position);

        SettingsAdapter.ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.layout_settings_menu, parent, false);
            viewHolder.menuIcon = convertView.findViewById(R.id.ivMenuIcon);
            viewHolder.menuText = convertView.findViewById(R.id.tvMenuName);
            viewHolder.subMenu = convertView.findViewById(R.id.ivMenuRight);
            viewHolder.cbMenu = convertView.findViewById(R.id.cbMenu);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (SettingsAdapter.ViewHolder) convertView.getTag();
        }

        viewHolder.menuIcon.setImageResource(settingsBean.getIcon());
        viewHolder.menuText.setText(settingsBean.getMenuText());
        viewHolder.subMenu.setVisibility(settingsBean.isSubMenu() ? View.VISIBLE : View.GONE);
        viewHolder.cbMenu.setVisibility(settingsBean.isCheckBox() ? View.VISIBLE : View.GONE);
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
        TextView menuText;
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
