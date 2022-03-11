package com.leon.detonator.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.R;

import java.util.List;
import java.util.Locale;

public class ListDialogAdapter extends BaseAdapter {
    private final List<DetonatorInfoBean> list;
    private final LayoutInflater inflater;
    private boolean tunnel;

    public ListDialogAdapter(Context context, List<DetonatorInfoBean> list) {
        this.list = list;
        inflater = LayoutInflater.from(context);
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

            convertView = inflater.inflate(R.layout.layout_list_table, parent, false);
            viewHolder.row = convertView.findViewById(R.id.text_row);
            viewHolder.hole = convertView.findViewById(R.id.text_hole);
            viewHolder.inside = convertView.findViewById(R.id.text_inside);
            viewHolder.address = convertView.findViewById(R.id.text_address);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (tunnel) {
            convertView.findViewById(R.id.line_row).setVisibility(View.GONE);
            convertView.findViewById(R.id.text_row).setVisibility(View.GONE);
        } else {
            viewHolder.row.setText(String.format(Locale.CHINA, "%d", detonatorInfoBean.getRow()));
            viewHolder.row.setTextSize(32);
        }
        viewHolder.hole.setText(String.format(Locale.CHINA, "%d", detonatorInfoBean.getHole()));
        viewHolder.hole.setTextSize(32);
        viewHolder.inside.setText(String.format(Locale.CHINA, "%d", detonatorInfoBean.getInside()));
        viewHolder.inside.setTextSize(32);
        viewHolder.address.setText(detonatorInfoBean.getAddress());
        viewHolder.address.setTextSize(32);
        if (detonatorInfoBean.isDownloaded()) {
            viewHolder.row.setTextColor(Color.BLACK);
            viewHolder.hole.setTextColor(Color.BLACK);
            viewHolder.inside.setTextColor(Color.BLACK);
            viewHolder.address.setTextColor(Color.BLACK);
        } else {
            viewHolder.row.setTextColor(Color.RED);
            viewHolder.hole.setTextColor(Color.RED);
            viewHolder.inside.setTextColor(Color.RED);
            viewHolder.address.setTextColor(Color.RED);
        }
        return convertView;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }

    private static class ViewHolder {
        TextView row;
        TextView hole;
        TextView inside;
        TextView address;
    }
}
