package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.OtherSettingsBean;
import com.leon.detonator.util.ConstantUtils;

import java.util.ArrayList;
import java.util.List;

public class OtherSettingsAdapter extends BaseAdapter {
    private final List<OtherSettingsBean> list;
    private final LayoutInflater inflater;

    public OtherSettingsAdapter(Context context, List<OtherSettingsBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<OtherSettingsBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public List<OtherSettingsBean> getList() {
        return list;
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
    public View getView(int i, View view, ViewGroup viewGroup) {
        OtherSettingsBean bean = (OtherSettingsBean) this.getItem(i);
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.layout_item_other_settings, viewGroup, false);
            viewHolder.title = view.findViewById(R.id.tv_title);
            viewHolder.checked = view.findViewById(R.id.cb_switch);
            view.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) view.getTag();
        viewHolder.title.setText(bean.getItem());
        viewHolder.title.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.checked.setChecked(bean.isChecked());
        return view;
    }


    private static class ViewHolder {
        TextView title;
        CheckBox checked;
    }
}
