package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.util.ConstantUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Administrator on 2018/1/25.
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
    public View getView(int position, View convertView, ViewGroup parent) {
        ExplosionRecordBean bean = (ExplosionRecordBean) this.getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_explode_record, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.tv_sn);
            viewHolder.explodeDate = convertView.findViewById(R.id.tv_explode_time);
            viewHolder.amount = convertView.findViewById(R.id.tv_amount);
            viewHolder.uploaded = convertView.findViewById(R.id.tv_uploaded);
            viewHolder.isSelected = convertView.findViewById(R.id.cb_selected);
            viewHolder.name = convertView.findViewById(R.id.tv_name);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();
        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_PART, Locale.getDefault());
        viewHolder.name.setText(String.format("%s %s", inflater.getContext().getString(R.string.text_scheme_name), bean.getName()));
        viewHolder.name.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.name.setTextColor(inflater.getContext().getColor(bean.getUploadServer() != -1 ? R.color.colorDownloaded : R.color.colorNotDownloaded));
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", position + 1));
        viewHolder.serialNo.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.serialNo.setTextColor(inflater.getContext().getColor(bean.getUploadServer() != -1 ? R.color.colorDownloaded : R.color.colorNotDownloaded));
        viewHolder.explodeDate.setText(String.format("%s %s", inflater.getContext().getString(R.string.text_explode_date), formatter.format(bean.getExplodeTime())));
        viewHolder.explodeDate.setTextColor(inflater.getContext().getColor(bean.getUploadServer() != -1 ? R.color.colorDownloaded : R.color.colorNotDownloaded));
        viewHolder.explodeDate.setTextSize(ConstantUtils.ITEM_TEXT_SIZE - 2);
        viewHolder.amount.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.amount.setText(String.format(Locale.getDefault(), "%s %d", inflater.getContext().getString(R.string.text_amount), bean.getAmount()));
        viewHolder.amount.setTextColor(inflater.getContext().getColor(bean.getUploadServer() != -1 ? R.color.colorDownloaded : R.color.colorNotDownloaded));
        viewHolder.uploaded.setText(bean.getUploadServer() != -1 ? R.string.text_uploaded : R.string.text_no_uploaded);
        viewHolder.uploaded.setTextColor(inflater.getContext().getColor(bean.getUploadServer() != -1 ? R.color.colorDownloaded : R.color.colorNotDownloaded));
        viewHolder.uploaded.setTextSize(ConstantUtils.ITEM_TEXT_SIZE - 2);
        viewHolder.isSelected.setChecked(bean.isSelected());
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
