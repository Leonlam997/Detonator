package com.leon.detonator.bean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.BaseJSONBean;
import com.leon.detonator.util.ConstantUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SchemeBean implements BaseJSONBean {
    private Date createTime;
    private String name;
    private int amount;
    private boolean selected;
    private boolean tunnel;

    public SchemeBean() {
        createTime = new Date();
        name = "";
    }

    public String fileName() {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_SAVE, Locale.getDefault());
        return df.format(createTime);
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

    public boolean isTunnel() {
        return tunnel;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_SAVE, Locale.getDefault());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("date", df.format(createTime));
        jsonObject.put("amount", amount);
        jsonObject.put("selected", selected);
        jsonObject.put("tunnel", tunnel);
        return jsonObject;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_SAVE, Locale.getDefault());
        name = jsonObject.getString("name");
        try {
            String date = jsonObject.getString("date");
            if (!date.isEmpty())
                createTime = df.parse(date);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        amount = jsonObject.getInt("amount");
        selected = jsonObject.getBoolean("selected");
        tunnel = jsonObject.getBoolean("tunnel");
    }

    @NonNull
    @Override
    public String toString() {
        return name + ", " + fileName() + ", " + amount + ", " + tunnel + ", " + selected;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SchemeBean))
            return false;
        return ((SchemeBean) obj).fileName().equals(fileName());
    }
}
