package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.BaiSeBlasterBean;
import com.leon.detonator.bean.BaiSeInfoBean;
import com.leon.detonator.bean.EnterpriseBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoAdapter<T> extends BaseAdapter {
    private final List<T> list;
    private final LayoutInflater inflater;

    public InfoAdapter(Context context, List<T> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<T> list) {
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
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.layout_item_info, viewGroup, false);
            viewHolder.serialNo = view.findViewById(R.id.tv_sn);
            viewHolder.info1 = view.findViewById(R.id.tv_info1);
            viewHolder.info2 = view.findViewById(R.id.tv_info2);
            viewHolder.isSelected = view.findViewById(R.id.rb_selected);
            view.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) view.getTag();

        int textSize = 28;
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", i + 1));
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.info1.setTextSize(textSize);
        viewHolder.info2.setTextSize(textSize);
        viewHolder.isSelected.setVisibility(View.VISIBLE);
        if (getItem(i) instanceof EnterpriseBean) {
            EnterpriseBean bean = (EnterpriseBean) getItem(i);
            viewHolder.info1.setText(bean.getCode());
            viewHolder.info2.setText(bean.getBlasterId());
            viewHolder.isSelected.setChecked(bean.isSelected());
        } else if (getItem(i) instanceof BaiSeInfoBean) {
            BaiSeInfoBean bean = (BaiSeInfoBean) getItem(i);
            viewHolder.info1.setText(bean.getBurstOrgName());
            viewHolder.info2.setText(bean.getProjectName());
            viewHolder.isSelected.setChecked(bean.isSelected());
        } else if (getItem(i) instanceof BaiSeBlasterBean) {
            BaiSeBlasterBean bean = (BaiSeBlasterBean) getItem(i);
            viewHolder.info1.setText(bean.getData().getUserIdCard());
            viewHolder.info2.setText(bean.getName());
            viewHolder.isSelected.setChecked(bean.isSelected());
        }
        return view;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView info1;
        TextView info2;
        RadioButton isSelected;
    }
}
