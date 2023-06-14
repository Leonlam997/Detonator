package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.SettingsBean;

import java.util.List;

/**
 * Created by Leon on 2018/3/14.
 */

public class SettingsAdapter extends BaseAdapter {
    private final List<SettingsBean> list;
    private final LayoutInflater inflater;

    public SettingsAdapter(Context context, List<SettingsBean> list) {
        this.list = list;
        inflater = LayoutInflater.from(context);
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
        SettingsBean settingsBean = (SettingsBean) this.getItem(position);
        SettingsAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_settings, parent, false);
            viewHolder.menuIcon = convertView.findViewById(R.id.iv_menu);
            viewHolder.title = convertView.findViewById(R.id.tv_title);
            viewHolder.more = convertView.findViewById(R.id.iv_more);
            viewHolder.subtitle = convertView.findViewById(R.id.tv_subtitle);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (SettingsAdapter.ViewHolder) convertView.getTag();
        viewHolder.menuIcon.setImageResource(settingsBean.getIcon());
        viewHolder.title.setText(settingsBean.getTitle());
        viewHolder.subtitle.setText(settingsBean.getSubtitle());
        viewHolder.more.setVisibility(settingsBean.isMore() ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private static class ViewHolder {
        ImageView menuIcon;
        TextView title;
        ImageView more;
        TextView subtitle;
    }
}
