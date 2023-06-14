package com.leon.detonator.bean;

public class BaiSeInfoBean extends BaiSeProjectBean {
    private long id;
    private boolean selected;

    public BaiSeInfoBean() {
        id = -1;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
