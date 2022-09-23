package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.bean.VersionBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VersionAdapter extends BaseAdapter {
    private final List<VersionBean> list;
    private final LayoutInflater inflater;

    public VersionAdapter(Context context, List<VersionBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<VersionBean> list) {
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        VersionBean bean = list.get(position);

        if (convertView == null) {

            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.layout_version_list, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.text_serialNo);
            viewHolder.downloadDate = convertView.findViewById(R.id.text_download_date);
            viewHolder.version = convertView.findViewById(R.id.text_version);
            viewHolder.size = convertView.findViewById(R.id.text_size);
            viewHolder.isSelected = convertView.findViewById(R.id.cb_selected);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_CHINESE, Locale.CHINA);
        int textSize = 28;
        String text = (position + 1) + "";
        viewHolder.serialNo.setText(text);
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.downloadDate.setText(formatter.format(bean.getDownloadDate()));
        viewHolder.downloadDate.setTextSize(textSize);
        viewHolder.size.setText(String.format(Locale.CHINA, "%.1fMB", bean.getSize() / 1000f / 1000f));
        viewHolder.size.setTextSize(textSize);
        viewHolder.version.setText(bean.getVersion());
        viewHolder.version.setTextSize(textSize);
        viewHolder.isSelected.setChecked(bean.isSelected());
        viewHolder.isSelected.setClickable(false);
        return convertView;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView downloadDate;
        TextView version;
        TextView size;
        CheckBox isSelected;
    }
}
