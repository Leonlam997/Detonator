package com.leon.detonator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.util.ConstantUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchemeAdapter extends BaseAdapter {
    private final List<SchemeBean> list;
    private final LayoutInflater inflater;
    private final ICheckedListener listener;

    public SchemeAdapter(Context context, List<SchemeBean> list, ICheckedListener listener) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void updateList(List<SchemeBean> list) {
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
        SchemeBean bean = (SchemeBean) getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.layout_scheme_list, parent, false);
            viewHolder.serialNo = convertView.findViewById(R.id.text_serial_no);
            viewHolder.name = convertView.findViewById(R.id.text_name);
            viewHolder.createTime = convertView.findViewById(R.id.text_time);
            viewHolder.amount = convertView.findViewById(R.id.text_amount);
            viewHolder.isSelected = convertView.findViewById(R.id.rb_selected);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_PART, Locale.CHINA);
        int textSize = 30;
        viewHolder.serialNo.setText(String.format(Locale.CHINA, "%d", position + 1));
        viewHolder.serialNo.setTextSize(textSize);
        viewHolder.name.setText(bean.getName());
        viewHolder.name.setTextSize(textSize);
        viewHolder.createTime.setText(formatter.format(bean.getCreateTime()));
        viewHolder.createTime.setTextSize(textSize);
        viewHolder.amount.setText(String.format(Locale.CHINA, "%d", bean.getAmount()));
        viewHolder.amount.setTextSize(textSize);
        viewHolder.isSelected.setVisibility(View.VISIBLE);
        viewHolder.isSelected.setChecked(bean.isSelected());
        viewHolder.isSelected.setOnClickListener(v -> {
            if (listener != null)
                listener.checked(position);
        });
        return convertView;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView name;
        TextView createTime;
        TextView amount;
        RadioButton isSelected;
    }
}
