package com.leon.detonator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.R;
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

        ExplosionRecordBean explodeRecordBean = (ExplosionRecordBean) this.getItem(position);

        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.layout_explode_record_list, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.text_serial_no);
            viewHolder.explodeDate = convertView.findViewById(R.id.text_explode_date);
            viewHolder.amount = convertView.findViewById(R.id.text_amount);
            viewHolder.uploaded = convertView.findViewById(R.id.text_uploaded);
            viewHolder.isSelected = convertView.findViewById(R.id.cb_selected);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_CHINESE, Locale.getDefault());

        int textSize = 28;
        String text = (position + 1) + "";
        viewHolder.serialNo.setText(text);
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.serialNo.setTextColor(explodeRecordBean.isUploaded() ? Color.BLACK : Color.RED);
        viewHolder.explodeDate.setText(formatter.format(explodeRecordBean.getExplodeDate()));
        viewHolder.explodeDate.setTextColor(explodeRecordBean.isUploaded() ? Color.BLACK : Color.RED);
        viewHolder.explodeDate.setTextSize(textSize);
        text = explodeRecordBean.getAmount() + "";
        viewHolder.amount.setText(text);
        viewHolder.amount.setTextSize(textSize);
        viewHolder.amount.setTextColor(explodeRecordBean.isUploaded() ? Color.BLACK : Color.RED);
        viewHolder.uploaded.setText(explodeRecordBean.isUploaded() ? "已上传" : "未上传");
        viewHolder.uploaded.setTextColor(explodeRecordBean.isUploaded() ? Color.BLACK : Color.RED);
        viewHolder.uploaded.setTextSize(textSize);
        viewHolder.isSelected.setChecked(explodeRecordBean.isSelected());
        viewHolder.isSelected.setClickable(false);

        return convertView;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView explodeDate;
        TextView amount;
        TextView uploaded;
        CheckBox isSelected;
    }
}
