package com.leon.detonator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfflineListAdapter extends BaseAdapter {
    private final List<DetonatorInfoBean> list;
    private final LayoutInflater inflater;

    public OfflineListAdapter(Context context, List<DetonatorInfoBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<DetonatorInfoBean> list) {
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
        DetonatorInfoBean detonatorInfoBean = (DetonatorInfoBean) this.getItem(i);
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.layout_offline_list, parent, false);
            viewHolder.serialNo = view.findViewById(R.id.text_serial_no);
            viewHolder.address = view.findViewById(R.id.text_address);
            viewHolder.isSelected = view.findViewById(R.id.cb_selected);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.isSelected.setChecked(detonatorInfoBean.isSelected());
        viewHolder.isSelected.setClickable(false);
        int textSize = 34;
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", i + 1));
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.address.setText(detonatorInfoBean.getAddress());
        viewHolder.address.setTextSize(textSize);
        if (list.get(i).isDownloaded()) {
            if (0 == list.get(i).getRow()) {
                viewHolder.serialNo.setTextColor(inflater.getContext().getColor(R.color.colorAuthorized));
                viewHolder.address.setTextColor(inflater.getContext().getColor(R.color.colorAuthorized));
            } else {
                viewHolder.serialNo.setTextColor(inflater.getContext().getColor(R.color.colorNotAuthorized));
                viewHolder.address.setTextColor(inflater.getContext().getColor(R.color.colorNotAuthorized));
            }
        } else {
            viewHolder.serialNo.setTextColor(Color.BLACK);
            viewHolder.address.setTextColor(Color.BLACK);
        }
        return view;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView address;
        CheckBox isSelected;
    }
}
