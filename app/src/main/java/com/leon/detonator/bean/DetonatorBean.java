package com.leon.detonator.bean;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.leon.detonator.base.BaseJSONBean;
import com.leon.detonator.util.ConstantUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Leon on 2018/1/23.
 */

public class DetonatorBean implements Parcelable, BaseJSONBean, Comparable<DetonatorBean> {
    public static final Creator<DetonatorBean> CREATOR = new Creator<DetonatorBean>() {
        @Override
        public DetonatorBean createFromParcel(Parcel source) {
            return new DetonatorBean(source);
        }

        @Override
        public DetonatorBean[] newArray(int size) {
            return new DetonatorBean[size];
        }
    };
    /**
     * 方案编号
     */
    private long schemeId;
    /**
     * 雷管编号
     */
    private int id;
    /**
     * 雷管管壳码
     */
    private String address;
    /**
     * 雷管UID码
     */
    private String uID;
    /**
     * 雷管延期
     */
    private int delayTime;
    /**
     * 雷管排号
     * 离线下载时用于记录错误代码
     * 0 雷管正常
     * 1 雷管在黑名单中
     * 2 雷管已使用
     * 3 申请的雷管UID不存在
     */
    private int row;
    /**
     * 雷管孔号或段号
     */
    private int hole;
    /**
     * 雷管孔内或段内号
     */
    private int inside;
    /**
     * 是否选中
     */
    private boolean selected;
    /**
     * 是否已下载
     */
    private boolean downloaded;

    public DetonatorBean() {
        address = "";
        uID = "";
        row = 1;
        hole = 1;
        inside = 1;
    }

    public DetonatorBean(@NonNull DetonatorBean bean) {
        id = bean.getId();
        schemeId = bean.getSchemeId();
        address = bean.getAddress();
        delayTime = bean.getDelayTime();
        uID = bean.getUID();
        row = bean.getRow();
        hole = bean.getHole();
        inside = bean.getInside();
        selected = bean.isSelected();
        downloaded = bean.isDownloaded();
    }

    public DetonatorBean(String address) {
        this.address = address;
        this.uID = "";
        this.downloaded = true;
    }

    public DetonatorBean(long schemeId, String address, int delay, int row, int hole, int inside, boolean downloaded) {
        this.schemeId = schemeId;
        this.address = address;
        this.delayTime = delay;
        this.uID = "";
        this.row = row;
        this.hole = hole;
        this.inside = inside;
        this.selected = false;
        this.downloaded = downloaded;
    }

    private DetonatorBean(Parcel source) {
        this.schemeId = source.readLong();
        this.id = source.readInt();
        this.address = source.readString();
        this.uID = source.readString();
        this.delayTime = source.readInt();
        this.row = source.readInt();
        this.hole = source.readInt();
        this.inside = source.readInt();
        this.downloaded = source.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(schemeId);
        dest.writeInt(id);
        dest.writeString(address);
        dest.writeString(uID);
        dest.writeInt(delayTime);
        dest.writeInt(row);
        dest.writeInt(hole);
        dest.writeInt(inside);
        dest.writeInt(downloaded ? 1 : 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(long schemeId) {
        this.schemeId = schemeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String addr) {
        this.address = addr;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(int delay) {
        this.delayTime = delay;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getHole() {
        return hole;
    }

    public void setHole(int hole) {
        this.hole = hole;
    }

    public int getInside() {
        return inside;
    }

    public void setInside(int inside) {
        this.inside = inside;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean isSelected) {
        this.selected = isSelected;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getUID() {
        return uID;
    }

    public void setUID(String uID) {
        this.uID = uID;
    }

    @NonNull
    public String toString() {
        return this.address + "," +
                this.delayTime + "," +
                this.uID + "," +
                this.row + "," +
                this.hole + "," +
                this.inside;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("address", this.address);
        jsonObject.put("delayTime", this.delayTime);
//        jsonObject.put("uID", this.uID);
        jsonObject.put("row", this.row);
        jsonObject.put("hole", this.hole);
        jsonObject.put("inside", this.inside);
        jsonObject.put("downloaded", this.downloaded);
        return jsonObject;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        this.address = jsonObject.getString("address");
//        this.uID = jsonObject.getString("uID");
        this.delayTime = jsonObject.getInt("delayTime");
        this.row = jsonObject.getInt("row");
        this.hole = jsonObject.getInt("hole");
        this.inside = jsonObject.getInt("inside");
        this.downloaded = jsonObject.getBoolean("downloaded");
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof DetonatorBean)
            return address.length() >= ConstantUtils.UID_LEN && ((DetonatorBean) obj).getAddress().endsWith(address);
        else
            return false;
    }

    @Override
    public int compareTo(@NonNull DetonatorBean o) {
        if (this.getRow() != o.getRow())
            return this.getRow() - o.getRow();
        else if (this.getHole() != o.getHole())
            return this.getHole() - o.getHole();
        else
            return this.getInside() - o.getInside();
    }
}
