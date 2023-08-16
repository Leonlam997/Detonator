package com.leon.detonator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Leon on 2018/1/23.
 */

public class DetonatorListAdapter extends BaseAdapter {
    private final List<DetonatorBean> list;
    private final LayoutInflater inflater;

    public DetonatorListAdapter(Context context, List<DetonatorBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<DetonatorBean> list) {
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
        DetonatorBean detonatorBean = (DetonatorBean) this.getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_detonator, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.text_serial_no);
            viewHolder.address = convertView.findViewById(R.id.text_address);
            viewHolder.delayTime = convertView.findViewById(R.id.text_delay);
            viewHolder.row = convertView.findViewById(R.id.text_row);
            viewHolder.hole = convertView.findViewById(R.id.text_hole);
            viewHolder.inside = convertView.findViewById(R.id.text_inside);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();
        int textSize = 30;
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", position + 1));
        viewHolder.serialNo.setTextSize(position >= 99 ? textSize - 2 : textSize);
        viewHolder.address.setText(detonatorBean.getAddress());
        viewHolder.address.setTextSize(textSize + 4);
        viewHolder.delayTime.setText(String.format(Locale.getDefault(), "%d", detonatorBean.getDelayTime()));
        viewHolder.delayTime.setTextSize(textSize);
        if (BaseApplication.isTunnel) {
            convertView.findViewById(R.id.line_inside).setVisibility(View.GONE);
            convertView.findViewById(R.id.text_inside).setVisibility(View.GONE);
            viewHolder.row.setText(String.format(Locale.getDefault(), "%d", detonatorBean.getHole()));
            viewHolder.row.setTextSize(detonatorBean.getHole() > 99 ? textSize - 2 : textSize);
            viewHolder.hole.setText(String.format(Locale.getDefault(), "%d", detonatorBean.getInside()));
            viewHolder.hole.setTextSize(detonatorBean.getInside() > 99 ? textSize - 2 : textSize);
        } else {
            viewHolder.row.setText(String.format(Locale.getDefault(), "%d", detonatorBean.getRow()));
            viewHolder.row.setTextSize(detonatorBean.getRow() > 99 ? textSize - 2 : textSize);
            viewHolder.hole.setText(String.format(Locale.getDefault(), "%d", detonatorBean.getHole()));
            viewHolder.hole.setTextSize(detonatorBean.getHole() > 99 ? textSize - 2 : textSize);
            viewHolder.inside.setText(String.format(Locale.getDefault(), "%d", detonatorBean.getInside()));
            viewHolder.inside.setTextSize(detonatorBean.getInside() > 99 ? textSize - 2 : textSize);
        }
        int color = detonatorBean.isDownloaded() ? Color.BLACK : Color.RED;
        viewHolder.serialNo.setTextColor(color);
        viewHolder.address.setTextColor(color);
        viewHolder.delayTime.setTextColor(color);
        viewHolder.row.setTextColor(color);
        viewHolder.hole.setTextColor(color);
        viewHolder.inside.setTextColor(color);
        return convertView;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView address;
        TextView delayTime;
        TextView row;
        TextView hole;
        TextView inside;
    }
}
