package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.SystemInfoBean;

import java.util.ArrayList;
import java.util.List;

public class SystemInfoAdapter extends BaseAdapter {
    private final List<SystemInfoBean> list;
    private final LayoutInflater inflater;

    public SystemInfoAdapter(Context context, List<SystemInfoBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<SystemInfoBean> list) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SystemInfoBean bean = (SystemInfoBean) this.getItem(position);
        SystemInfoAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_system_info, parent, false);
            viewHolder.title = convertView.findViewById(R.id.info_title);
            viewHolder.subtitle = convertView.findViewById(R.id.info_subtitle);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (SystemInfoAdapter.ViewHolder) convertView.getTag();
        viewHolder.title.setText(bean.getTitle());
        viewHolder.subtitle.setText(bean.getSubtitle());
        return convertView;
    }

    private static class ViewHolder {
        TextView title;
        TextView subtitle;
    }
}
