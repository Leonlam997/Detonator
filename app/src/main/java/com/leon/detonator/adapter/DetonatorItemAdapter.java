package com.leon.detonator.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.util.ConstantUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetonatorItemAdapter extends RecyclerView.Adapter<DetonatorItemAdapter.ViewHolder> {
    private final List<DetonatorBean> list;
    private final int start;
    private final OnItemClickListener listener;

    public DetonatorItemAdapter(List<DetonatorBean> list, int start, OnItemClickListener listener) {
        this.list = new ArrayList<>(list);
        this.start = start;
        this.listener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_detonator, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.llDetonator.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                DetonatorGroupAdapter.touchX = (int) motionEvent.getX();
            return false;
        });
        viewHolder.llDetonator.setOnClickListener(view1 -> {
            if (listener != null)
                listener.onItemClick(view1, viewHolder.getAdapterPosition() + start);
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetonatorBean detonatorBean = list.get(position);
        holder.serialNo.setText(String.format(Locale.getDefault(), "%d", start + position + 1));
        holder.serialNo.setTextSize(position >= 99 ? ConstantUtils.ITEM_TEXT_SIZE - 2 : ConstantUtils.ITEM_TEXT_SIZE);
        holder.address.setText(detonatorBean.getAddress());
        holder.address.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        holder.delayTime.setText(String.format(Locale.getDefault(), "%dms", detonatorBean.getDelayTime()));
        holder.delayTime.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        if (BaseApplication.settings.isTunnel()) {
            holder.row.setText(String.format(Locale.getDefault(), "%d-%d", detonatorBean.getHole(), detonatorBean.getInside()));
        } else {
            holder.row.setText(String.format(Locale.getDefault(), "%d-%d-%d", detonatorBean.getRow(), detonatorBean.getHole(), detonatorBean.getInside()));
        }
        holder.row.setTextSize(ConstantUtils.ITEM_TEXT_SIZE);
        int color = detonatorBean.isDownloaded() ? Color.BLACK : Color.RED;
        holder.serialNo.setTextColor(color);
        holder.address.setTextColor(color);
        holder.delayTime.setTextColor(color);
        holder.row.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public int getStart() {
        return start;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout llDetonator;
        public TextView serialNo;
        public TextView address;
        public TextView delayTime;
        public TextView row;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llDetonator = itemView.findViewById(R.id.ll_detonator);
            serialNo = itemView.findViewById(R.id.text_serial_no);
            address = itemView.findViewById(R.id.text_address);
            delayTime = itemView.findViewById(R.id.text_delay);
            row = itemView.findViewById(R.id.text_row);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.weight = 1.3f;
            row.setLayoutParams(params);
            itemView.findViewById(R.id.text_hole).setVisibility(View.GONE);
            itemView.findViewById(R.id.text_inside).setVisibility(View.GONE);
            itemView.findViewById(R.id.line_hole).setVisibility(View.GONE);
            itemView.findViewById(R.id.line_inside).setVisibility(View.GONE);
            itemView.findViewById(R.id.v_divider).setVisibility(View.VISIBLE);
        }
    }
}
