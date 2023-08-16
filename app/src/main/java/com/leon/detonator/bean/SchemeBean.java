package com.leon.detonator.bean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

public class SchemeBean {
    private long id;
    private Date createTime;
    private String name;
    private int amount;
    private boolean selected;

    public SchemeBean() {
        id = -1;
        createTime = new Date();
        name = "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @NonNull
    @Override
    public String toString() {
        return id + ", " + name + ", " + amount + ", " + selected;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SchemeBean))
            return false;
        return ((SchemeBean) obj).getId() == id;
    }
}
