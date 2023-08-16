package com.leon.detonator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.DetonatorBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfflineListAdapter extends BaseAdapter {
    private final List<DetonatorBean> list;
    private final LayoutInflater inflater;

    public OfflineListAdapter(Context context, List<DetonatorBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<DetonatorBean> list) {
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
        DetonatorBean detonatorBean = (DetonatorBean) this.getItem(i);
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.layout_item_offline, parent, false);
            viewHolder.serialNo = view.findViewById(R.id.text_serial_no);
            viewHolder.address = view.findViewById(R.id.text_address);
            viewHolder.status = view.findViewById(R.id.text_status);
            view.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) view.getTag();
        int textSize = 30;
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", i + 1));
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.address.setText(detonatorBean.getAddress());
        viewHolder.address.setTextSize(textSize);
        viewHolder.status.setText(detonatorBean.isDownloaded() ? (0 == detonatorBean.getRow() ? R.string.detonator_auth : R.string.detonator_error) : R.string.detonator_not_auth);
        int color = detonatorBean.isDownloaded() ? (0 == detonatorBean.getRow() ? inflater.getContext().getColor(R.color.colorAuthorized) : inflater.getContext().getColor(R.color.colorNotAuthorized)) : Color.BLACK;
        viewHolder.serialNo.setTextColor(color);
        viewHolder.address.setTextColor(color);
        viewHolder.status.setTextColor(color);
        return view;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView address;
        TextView status;
    }
}
