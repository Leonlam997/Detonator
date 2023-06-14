package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.util.ConstantUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Leon on 2018/1/23.
 */

public class DetonatorListAdapter extends BaseAdapter {
    private final List<DetonatorBean> list;
    private final LayoutInflater inflater;
    private boolean canSelect = false;
    private boolean enabled;

    public DetonatorListAdapter(Context context, List<DetonatorBean> list) {
        this.list = new ArrayList<>(list);
        enabled = true;
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<DetonatorBean> list) {
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
        DetonatorBean detonatorBean = (DetonatorBean) this.getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_item_detonator, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.tv_sn);
            viewHolder.address = convertView.findViewById(R.id.text_address);
            viewHolder.delayTime = convertView.findViewById(R.id.text_delay);
            viewHolder.row = convertView.findViewById(R.id.text_row);
            viewHolder.hole = convertView.findViewById(R.id.text_hole);
            viewHolder.inside = convertView.findViewById(R.id.text_inside);
            viewHolder.isSelected = convertView.findViewById(R.id.cb_selected);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", position + 1));
        viewHolder.serialNo.setTextSize(position >= 99 ? ConstantUtils.ITEM_TEXT_SIZE - 2 : ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.address.setText(String.format("%s%s", inflater.getContext().getString(R.string.text_tube_num), detonatorBean.getAddress()));
        viewHolder.address.setTextSize(ConstantUtils.ITEM_TEXT_SIZE + (canSelect ? 0 : 2));
        viewHolder.delayTime.setText(String.format(Locale.getDefault(), "%s %d%s", inflater.getContext().getString(R.string.text_delay), detonatorBean.getDelayTime(), inflater.getContext().getString(R.string.delay_unit)));
        viewHolder.delayTime.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        if (BaseApplication.settings.isTunnel())
            convertView.findViewById(R.id.text_row).setVisibility(View.GONE);
        else {
            viewHolder.row.setText(String.format(Locale.getDefault(), "%s %d", inflater.getContext().getString(R.string.text_row_num), detonatorBean.getRow()));
            viewHolder.row.setTextSize(ConstantUtils.ITEM_TEXT_SIZE - (detonatorBean.getRow() < 100 ? 0 : 2));
        }
        viewHolder.hole.setText(String.format(Locale.getDefault(), "%s %d", inflater.getContext().getString(BaseApplication.settings.isTunnel() ? R.string.text_section_num : R.string.text_hole_num), detonatorBean.getHole()));
        viewHolder.hole.setTextSize(ConstantUtils.ITEM_TEXT_SIZE - (detonatorBean.getHole() < 100 ? 0 : 2));
        viewHolder.inside.setText(String.format(Locale.getDefault(), "%s %d", inflater.getContext().getString(BaseApplication.settings.isTunnel() ? R.string.text_section_inside_num : R.string.text_inside_num), detonatorBean.getInside()));
        viewHolder.inside.setTextSize(ConstantUtils.ITEM_TEXT_SIZE - (detonatorBean.getInside() < 100 ? 0 : 2));
        if (canSelect) {
            viewHolder.isSelected.setChecked(detonatorBean.isSelected());
            viewHolder.isSelected.setClickable(false);
            viewHolder.isSelected.setEnabled(enabled);
        } else
            viewHolder.isSelected.setVisibility(View.GONE);
        if (detonatorBean.isDownloaded()) {
            viewHolder.serialNo.setTextColor(inflater.getContext().getColor(R.color.colorDownloaded));
            viewHolder.address.setTextColor(inflater.getContext().getColor(R.color.colorDownloaded));
            viewHolder.delayTime.setTextColor(inflater.getContext().getColor(R.color.colorDownloaded));
            viewHolder.row.setTextColor(inflater.getContext().getColor(R.color.colorDownloaded));
            viewHolder.hole.setTextColor(inflater.getContext().getColor(R.color.colorDownloaded));
            viewHolder.inside.setTextColor(inflater.getContext().getColor(R.color.colorDownloaded));
        } else {
            viewHolder.serialNo.setTextColor(inflater.getContext().getColor(R.color.colorNotDownloaded));
            viewHolder.address.setTextColor(inflater.getContext().getColor(R.color.colorNotDownloaded));
            viewHolder.delayTime.setTextColor(inflater.getContext().getColor(R.color.colorNotDownloaded));
            viewHolder.row.setTextColor(inflater.getContext().getColor(R.color.colorNotDownloaded));
            viewHolder.hole.setTextColor(inflater.getContext().getColor(R.color.colorNotDownloaded));
            viewHolder.inside.setTextColor(inflater.getContext().getColor(R.color.colorNotDownloaded));
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
