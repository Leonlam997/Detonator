package com.leon.detonator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.leon.detonator.R;
import com.minew.modulekit.MTModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MTModuleListAdapter extends BaseAdapter {
    private final List<MTModule> list;
    private final LayoutInflater inflater;
    private final String mac;

    public MTModuleListAdapter(Context context, List<MTModule> list, String mac) {
        this.list = new ArrayList<>(list);
        this.mac = mac;
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<MTModule> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {

        ViewHolder viewHolder;
        if (view == null) {

            viewHolder = new ViewHolder();

            view = inflater.inflate(R.layout.layout_mtmodule_list, parent, false);
            viewHolder.tvName = view.findViewById(R.id.text_name);
            viewHolder.tvAddress = view.findViewById(R.id.text_address);
            viewHolder.tvSignal = view.findViewById(R.id.text_signal);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        int textSize = 23;
        viewHolder.tvName.setText(list.get(i).getName());
        viewHolder.tvName.setTextSize(textSize);
        viewHolder.tvAddress.setText(list.get(i).getMacAddress());
        viewHolder.tvAddress.setTextSize(textSize);
        viewHolder.tvSignal.setText(String.format(Locale.getDefault(), "%ddb", list.get(i).getRssi()));
        viewHolder.tvSignal.setTextSize(textSize);
        if (null != mac && list.get(i).getMacAddress().contains(mac)) {
            viewHolder.tvName.setTextColor(inflater.getContext().getColor(R.color.colorLastConnected));
            viewHolder.tvAddress.setTextColor(inflater.getContext().getColor(R.color.colorLastConnected));
            viewHolder.tvSignal.setTextColor(inflater.getContext().getColor(R.color.colorLastConnected));
        } else {
            viewHolder.tvName.setTextColor(Color.BLACK);
            viewHolder.tvAddress.setTextColor(Color.BLACK);
            viewHolder.tvSignal.setTextColor(Color.BLACK);
        }

        return view;
    }

    private static class ViewHolder {
        private TextView tvName;
        private TextView tvAddress;
        private TextView tvSignal;
    }
}
