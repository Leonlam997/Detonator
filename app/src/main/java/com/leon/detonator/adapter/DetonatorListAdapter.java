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

/**
 * Created by Leon on 2018/1/23.
 */

public class DetonatorListAdapter extends BaseAdapter {
    private final List<DetonatorInfoBean> list;
    private final LayoutInflater inflater;
    private boolean canSelect = false;
    private boolean enabled;
    private boolean tunnel;

    public DetonatorListAdapter(Context context, List<DetonatorInfoBean> list) {
        this.list = new ArrayList<>(list);
        enabled = true;
        tunnel = false;
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<DetonatorInfoBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public boolean isCanSelect() {
        return canSelect;
    }

    public void setCanSelect(boolean select) {
        this.canSelect = select;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        notifyDataSetChanged();
    }

    public List<DetonatorInfoBean> getList() {
        return list;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
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

        DetonatorInfoBean detonatorInfoBean = (DetonatorInfoBean) this.getItem(position);

        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_delay_list, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.text_serialNo);
            viewHolder.address = convertView.findViewById(R.id.text_address);
            viewHolder.delayTime = convertView.findViewById(R.id.text_delay);
            viewHolder.row = convertView.findViewById(R.id.text_row);
            viewHolder.hole = convertView.findViewById(R.id.text_hole);
            viewHolder.inside = convertView.findViewById(R.id.text_inside);
            viewHolder.isSelected = convertView.findViewById(R.id.cb_selected);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        int textSize = 30;
        viewHolder.serialNo.setText(String.format(Locale.CHINA, "%d", position + 1));
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.address.setText(detonatorInfoBean.getAddress());
        viewHolder.address.setTextSize(textSize + 4);
        viewHolder.delayTime.setText(String.format(Locale.CHINA, "%dms", detonatorInfoBean.getDelayTime()));
        viewHolder.delayTime.setTextSize(textSize);
        if (tunnel) {
            convertView.findViewById(R.id.line_row).setVisibility(View.GONE);
            convertView.findViewById(R.id.text_row).setVisibility(View.GONE);
        } else {
            viewHolder.row.setText(String.format(Locale.CHINA, "%d", detonatorInfoBean.getRow()));
            viewHolder.row.setTextSize(textSize);
        }
        viewHolder.hole.setText(String.format(Locale.CHINA, "%d", detonatorInfoBean.getHole()));
        viewHolder.hole.setTextSize(textSize);
        viewHolder.inside.setText(String.format(Locale.CHINA, "%d", detonatorInfoBean.getInside()));
        viewHolder.inside.setTextSize(textSize);
        if (canSelect) {
            viewHolder.isSelected.setChecked(detonatorInfoBean.isSelected());
            viewHolder.isSelected.setClickable(false);
            viewHolder.isSelected.setEnabled(enabled);
        } else {
            convertView.findViewById(R.id.v_selected).setVisibility(View.GONE);
            convertView.findViewById(R.id.rl_cb).setVisibility(View.GONE);
            viewHolder.isSelected.setVisibility(View.GONE);
        }
        if (detonatorInfoBean.isDownloaded()) {
            viewHolder.serialNo.setTextColor(Color.BLACK);
            viewHolder.address.setTextColor(Color.BLACK);
            viewHolder.delayTime.setTextColor(Color.BLACK);
            viewHolder.row.setTextColor(Color.BLACK);
            viewHolder.hole.setTextColor(Color.BLACK);
            viewHolder.inside.setTextColor(Color.BLACK);
        } else {
            viewHolder.serialNo.setTextColor(Color.RED);
            viewHolder.address.setTextColor(Color.RED);
            viewHolder.delayTime.setTextColor(Color.RED);
            viewHolder.row.setTextColor(Color.RED);
            viewHolder.hole.setTextColor(Color.RED);
            viewHolder.inside.setTextColor(Color.RED);
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView address;
        TextView delayTime;
        TextView row;
        TextView hole;
        TextView inside;
        CheckBox isSelected;
    }
}
