package com.leon.detonator.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.DetonatorGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetonatorGroupAdapter extends RecyclerView.Adapter<DetonatorGroupAdapter.ViewHolder> {
    private final List<DetonatorBean> list;
    private final OnItemClickListener listener;
    private final List<DetonatorGroup> groupList;
    public static int touchX;

    public DetonatorGroupAdapter(List<DetonatorBean> list, OnItemClickListener listener) {
        this.list = new ArrayList<>(list);
        this.listener = listener;
        groupList = new ArrayList<>();
        initGroup();
    }

    public void scrollToPosition(RecyclerView view, int index) {
        for (int i = 0; i < groupList.size(); i++)
            if (groupList.get(i).getStart() >= index && groupList.get(i).getEnd() <= index) {
                if (!groupList.get(i).isShow()) {
                    groupList.get(i).setShow(true);
                    notifyItemChanged(i);
                }
                view.scrollToPosition(i);
                groupList.get(i).getRecyclerView().scrollToPosition(index - groupList.get(i).getStart());
                return;
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<DetonatorBean> list) {
        this.list.clear();
        this.list.addAll(list);
        initGroup();
        notifyDataSetChanged();
    }

    private void initGroup() {
        groupList.clear();
        if (list.size() > 0) {
            DetonatorGroup group = new DetonatorGroup();
            group.setGroup(BaseApplication.settings.isTunnel() ? list.get(0).getHole() : list.get(0).getRow());
            group.setStartDelay(list.get(0).getDelayTime());
            group.setStart(0);
            group.setShow(false);
            for (int i = 1; i < list.size(); i++) {
                if ((BaseApplication.settings.isTunnel() && list.get(i).getHole() != group.getGroup()) || (!BaseApplication.settings.isTunnel() && list.get(i).getRow() != group.getGroup())) {
                    group.setEndDelay(list.get(i - 1).getDelayTime());
                    group.setEnd(i - 1);
                    groupList.add(group);
                    group = new DetonatorGroup();
                    group.setGroup(BaseApplication.settings.isTunnel() ? list.get(i).getHole() : list.get(i).getRow());
                    group.setStartDelay(list.get(i).getDelayTime());
                    group.setStart(i);
                    group.setShow(false);
                }
            }
            group.setEndDelay(list.get(list.size() - 1).getDelayTime());
            group.setEnd(list.size() - 1);
            groupList.add(group);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_detonator_group, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.llGroup.setOnClickListener(view1 -> {
            boolean isShow = viewHolder.rvDetonator.getVisibility() == View.VISIBLE;
            viewHolder.ivFlag.setImageDrawable(isShow ? ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_right) : ContextCompat.getDrawable(parent.getContext(), R.drawable.ic_down));
            viewHolder.rvDetonator.setVisibility(isShow ? View.GONE : View.VISIBLE);
            groupList.get(viewHolder.getAdapterPosition()).setShow(!isShow);
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetonatorGroup bean = groupList.get(position);
        holder.tvGroupName.setText(String.format(Locale.getDefault(), holder.rvDetonator.getContext().getString(R.string.text_group_title), bean.getGroup(), holder.rvDetonator.getContext().getString(BaseApplication.settings.isTunnel() ? R.string.unit_section : R.string.unit_row), bean.getStartDelay(), bean.getEndDelay(), bean.getEnd() - bean.getStart() + 1));
        DetonatorItemAdapter adapter = new DetonatorItemAdapter(list.subList(bean.getStart(), bean.getEnd() + 1), bean.getStart(), listener);
        holder.rvDetonator.setAdapter(adapter);
        holder.rvDetonator.setLayoutManager(new LinearLayoutManager(holder.rvDetonator.getContext()));
        holder.ivFlag.setImageDrawable(bean.isShow() ? ContextCompat.getDrawable(holder.rvDetonator.getContext(), R.drawable.ic_down) : ContextCompat.getDrawable(holder.rvDetonator.getContext(), R.drawable.ic_right));
        holder.rvDetonator.setVisibility(bean.isShow() ? View.VISIBLE : View.GONE);
        bean.setRecyclerView(holder.rvDetonator);
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RecyclerView rvDetonator;
        public TextView tvGroupName;
        public ImageView ivFlag;
        public LinearLayout llGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rvDetonator = itemView.findViewById(R.id.rv_detonator);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            ivFlag = itemView.findViewById(R.id.iv_flag);
            llGroup = itemView.findViewById(R.id.ll_group);
            llGroup.setBackgroundColor(rvDetonator.getContext().getColor(BaseApplication.settings.isTunnel() ? R.color.colorTunnelGroupBackground : R.color.colorOpenAirGroupBackground));
        }
    }
}
