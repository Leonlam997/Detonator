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
import com.leon.detonator.util.ConstantUtils;

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

    public List<DetonatorBean> getList() {
        return list;
    }

    public void updateList(List<DetonatorBean> list) {
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
            viewHolder.serialNo = view.findViewById(R.id.tv_sn);
            viewHolder.address = view.findViewById(R.id.text_address);
            viewHolder.auth = view.findViewById(R.id.text_auth);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", i + 1));
        viewHolder.serialNo.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.address.setText(detonatorBean.getAddress());
        viewHolder.address.setTextSize(ConstantUtils.ITEM_TEXT_SIZE + 2);
        viewHolder.auth.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        int color = Color.BLACK;
        if (list.get(i).isDownloaded()) {
            if (0 == list.get(i).getRow()) {
                color = inflater.getContext().getColor(R.color.colorAuthorized);
                viewHolder.auth.setText(R.string.detonator_auth);
            } else {
                color = inflater.getContext().getColor(R.color.colorNotAuthorized);
                viewHolder.auth.setText(R.string.detonator_error);
            }
        } else
            viewHolder.auth.setText(R.string.detonator_not_auth);
        viewHolder.serialNo.setTextColor(color);
        viewHolder.address.setTextColor(color);
        viewHolder.auth.setTextColor(color);
        return view;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView address;
        TextView auth;
    }
}
