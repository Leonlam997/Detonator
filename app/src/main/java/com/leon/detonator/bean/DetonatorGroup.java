package com.leon.detonator.bean;

import androidx.recyclerview.widget.RecyclerView;

public class DetonatorGroup {
    private int group;
    private int start;
    private int end;
    private int startDelay;
    private int endDelay;
    private boolean show;
    private RecyclerView recyclerView;

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
    }

    public int getEndDelay() {
        return endDelay;
    }

    public void setEndDelay(int endDelay) {
        this.endDelay = endDelay;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }
}
