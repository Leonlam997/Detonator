package com.leon.detonator.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.leon.detonator.Bean.WifiListBean;
import com.leon.detonator.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/3/20.
 */

public class WifiListAdapter extends BaseAdapter {
    private final List<WifiListBean> list;
    private final LayoutInflater inflater;
    private final int[] wifiSignal = {R.mipmap.ic_wifi_signal_1,
            R.mipmap.ic_wifi_signal_2,
            R.mipmap.ic_wifi_signal_3,
            R.mipmap.ic_wifi_signal_4};
    private OnButtonClickListener onButtonClickListener;


    public WifiListAdapter(Context context, List<WifiListBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<WifiListBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
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
        WifiListBean bean = (WifiListBean) this.getItem(position);

        WifiListAdapter.ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.layout_settings_wifilist, parent, false);
            viewHolder.ivSelected = convertView.findViewById(R.id.ivSelected);
            viewHolder.tvSSID = convertView.findViewById(R.id.tvSSID);
            viewHolder.pbScan = convertView.findViewById(R.id.pbScan);
            viewHolder.tvRescan = convertView.findViewById(R.id.tvRescan);
            viewHolder.pbConnect = convertView.findViewById(R.id.pbConnect);
            viewHolder.ivSaved = convertView.findViewById(R.id.ivSaved);
            viewHolder.ivLock = convertView.findViewById(R.id.ivLock);
            viewHolder.ivSignal = convertView.findViewById(R.id.ivLevel);
            viewHolder.cbSwitch = convertView.findViewById(R.id.cbWifi);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (WifiListAdapter.ViewHolder) convertView.getTag();
        }

        viewHolder.tvSSID.setText(bean.getSSID());
        viewHolder.ivSelected.setVisibility(bean.isConnected() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.pbScan.setVisibility(bean.isScanning() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.tvRescan.setVisibility((!bean.isScanning() && bean.isRescanLine()) ? View.VISIBLE : View.INVISIBLE);
        viewHolder.pbConnect.setVisibility(bean.isConnecting() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.ivSaved.setVisibility(bean.isSaved() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.ivLock.setVisibility(bean.isEncrypted() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.ivSignal.setVisibility((position == 0 || bean.isRescanLine()) ? View.INVISIBLE : View.VISIBLE);
        viewHolder.cbSwitch.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);

        if (position == 0) {
            convertView.setBackgroundColor(Color.WHITE);
            viewHolder.cbSwitch.setEnabled(!bean.isChangingStatus());
            viewHolder.cbSwitch.setChecked(bean.isEnabled());
        } else {
            if (bean.isRescanLine()) {
                convertView.setBackgroundColor(ContextCompat.getColor(parent.getContext(), R.color.colorCommonBackground));
                viewHolder.tvRescan.setOnClickListener(v -> {
                    if (onButtonClickListener != null) {
                        onButtonClickListener.OnButtonClick(2);
                    }
                });
            } else {
                convertView.setBackgroundColor(Color.WHITE);
                viewHolder.ivSignal.setImageResource(wifiSignal[bean.getSignalLevel() >= wifiSignal.length ? wifiSignal.length - 1 : bean.getSignalLevel()]);
            }
        }

        viewHolder.cbSwitch.setOnClickListener(v -> {
            if (onButtonClickListener != null) {
                onButtonClickListener.OnButtonClick(((CheckBox) v).isChecked() ? 1 : 0);
            }
        });

        return convertView;
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        onButtonClickListener = listener;
    }

    public interface OnButtonClickListener {
        void OnButtonClick(int which);
    }

    private static class ViewHolder {
        ImageView ivSelected;
        TextView tvSSID;
        ProgressBar pbScan;
        TextView tvRescan;
        ProgressBar pbConnect;
        ImageView ivSaved;
        ImageView ivLock;
        ImageView ivSignal;
        CheckBox cbSwitch;
    }
}
