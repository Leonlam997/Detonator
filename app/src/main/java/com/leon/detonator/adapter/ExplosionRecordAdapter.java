package com.leon.detonator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.util.ConstantUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Leon on 2018/1/25.
 */

public class ExplosionRecordAdapter extends BaseAdapter {
    private final List<ExplosionRecordBean> list;
    private final LayoutInflater inflater;

    public ExplosionRecordAdapter(Context context, List<ExplosionRecordBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<ExplosionRecordBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public List<ExplosionRecordBean> getList() {
        return list;
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
        ExplosionRecordBean explodeRecordBean = (ExplosionRecordBean) this.getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_explode_record, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.text_serial_no);
            viewHolder.name = convertView.findViewById(R.id.text_name);
            viewHolder.explodeDate = convertView.findViewById(R.id.text_explode_date);
            viewHolder.amount = convertView.findViewById(R.id.text_amount);
            viewHolder.uploaded = convertView.findViewById(R.id.text_uploaded);
            viewHolder.isSelected = convertView.findViewById(R.id.cb_selected);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();
        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_PART, Locale.getDefault());
        int textSize = 28;
        int color = explodeRecordBean.getUploadServer() != -1 ? Color.BLACK : Color.RED;
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", position + 1));
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.serialNo.setTextColor(color);
        viewHolder.name.setText(explodeRecordBean.getName());
        viewHolder.name.setTextSize(textSize);
        viewHolder.name.setTextColor(color);
        viewHolder.explodeDate.setText(formatter.format(explodeRecordBean.getExplodeTime()));
        viewHolder.explodeDate.setTextSize(textSize - BaseApplication.settings.getFontScale() * 2);
        viewHolder.explodeDate.setTextColor(color);
        viewHolder.amount.setText(String.format(Locale.getDefault(), "%d", explodeRecordBean.getAmount()));
        viewHolder.amount.setTextSize(textSize);
        viewHolder.amount.setTextColor(color);
        viewHolder.uploaded.setText(explodeRecordBean.getUploadServer() != -1 ? inflater.getContext().getString(R.string.text_uploaded) : inflater.getContext().getString(R.string.text_not_uploaded));
        viewHolder.uploaded.setTextSize(textSize);
        viewHolder.uploaded.setTextColor(color);
        viewHolder.isSelected.setChecked(explodeRecordBean.isSelected());
        viewHolder.isSelected.setClickable(false);
        return convertView;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView name;
        TextView explodeDate;
        TextView amount;
        TextView uploaded;
        CheckBox isSelected;
    }
}
