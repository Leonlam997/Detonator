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
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.util.ConstantUtils;

import java.text.SimpleDateFormat;
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
            viewHolder.info3 = view.findViewById(R.id.tv_info3);
            viewHolder.isSelected = view.findViewById(R.id.rb_selected);
            view.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) view.getTag();
        viewHolder.serialNo.setText(String.format(Locale.getDefault(), "%d", i + 1));
        viewHolder.serialNo.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.info1.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.info2.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.info3.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        viewHolder.isSelected.setVisibility(View.VISIBLE);
        if (getItem(i) instanceof EnterpriseBean) {
            EnterpriseBean bean = (EnterpriseBean) getItem(i);
            viewHolder.info1.setText(String.format("%s %s", inflater.getContext().getString(R.string.enterprise_code), bean.getCode()));
            viewHolder.info2.setText(String.format("%s %s", inflater.getContext().getString(R.string.enterprise_detector_id), bean.getBlasterId()));
            viewHolder.info3.setText(inflater.getContext().getString(bean.isCommercial() ? R.string.enterprise_commercial : R.string.enterprise_not_commercial));
            viewHolder.isSelected.setChecked(bean.isSelected());
        } else if (getItem(i) instanceof BaiSeInfoBean) {
            BaiSeInfoBean bean = (BaiSeInfoBean) getItem(i);
            boolean project = ConstantUtils.ENTERPRISE_PROJECT.equals(bean.getProjectType());
            viewHolder.info1.setText(String.format("%s %s", inflater.getContext().getString(R.string.enterprise_name), bean.getBurstOrgName()));
            viewHolder.info2.setText(String.format("%s %s", inflater.getContext().getString(project ? R.string.enterprise_project_name : R.string.enterprise_contract_name), bean.getProjectName()));
            viewHolder.info3.setText(String.format("%s %s", inflater.getContext().getString(R.string.enterprise_blaster_name), bean.getBursterName()));
            viewHolder.isSelected.setChecked(bean.isSelected());
        } else if (getItem(i) instanceof BaiSeBlasterBean) {
            BaiSeBlasterBean bean = (BaiSeBlasterBean) getItem(i);
            viewHolder.info1.setText(String.format("%s %s", inflater.getContext().getString(R.string.detector_name), bean.getName()));
            viewHolder.info2.setText(String.format("%s %s", inflater.getContext().getString(R.string.enterprise_detector_id), bean.getData().getUserIdCard()));
            viewHolder.info3.setVisibility(View.GONE);
            viewHolder.isSelected.setChecked(bean.isSelected());
        } else if (getItem(i) instanceof SchemeBean) {
            SchemeBean bean = (SchemeBean) getItem(i);
            SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_PART, Locale.getDefault());
            viewHolder.info1.setText(String.format("%s %s", inflater.getContext().getString(R.string.text_scheme_name), bean.getName()));
            viewHolder.info2.setText(String.format("%s %s", inflater.getContext().getString(R.string.text_create_date), formatter.format(bean.getCreateTime())));
            viewHolder.info2.setTextSize(ConstantUtils.ITEM_TEXT_SIZE-2);
            viewHolder.info3.setText(String.format(Locale.getDefault(), "%s %d", inflater.getContext().getString(R.string.text_amount), bean.getAmount()));
            viewHolder.isSelected.setChecked(bean.isSelected());
        }
        return view;
    }

    private static class ViewHolder {
        TextView serialNo;
        TextView info1;
        TextView info2;
        TextView info3;
        RadioButton isSelected;
    }
}
