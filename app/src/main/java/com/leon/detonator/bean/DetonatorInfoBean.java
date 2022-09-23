package com.leon.detonator.bean;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.leon.detonator.base.BaseJSONBean;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Leon on 2018/1/23.
 */

public class DetonatorInfoBean implements Parcelable, BaseJSONBean, Comparable<DetonatorInfoBean> {
    public static final Creator<DetonatorInfoBean> CREATOR = new Creator<DetonatorInfoBean>() {
        @Override
        public DetonatorInfoBean createFromParcel(Parcel source) {
            return new DetonatorInfoBean(source);
        }

        @Override
        public DetonatorInfoBean[] newArray(int size) {
            return new DetonatorInfoBean[size];
        }
    };
    private String address;      //管壳码
    private int delayTime;      //延期
    private int row;             //排号
    private int hole;            //孔号或段号
    private int inside;         //孔内或段内
    private boolean selected;   //是否选中
    private boolean downloaded; //是否已下载

    public DetonatorInfoBean() {
        address = "";
        delayTime = 0;
        row = 1;
        hole = 1;
        inside = 1;
        selected = false;
        downloaded = false;
    }

    public DetonatorInfoBean(@NonNull DetonatorInfoBean bean) {
        address = bean.getAddress();
        delayTime = bean.getDelayTime();
        row = bean.getRow();
        hole = bean.getHole();
        inside = bean.getInside();
        selected = bean.isSelected();
        downloaded = bean.isDownloaded();
    }

    public DetonatorInfoBean(String address) {
        this.address = address;
        this.delayTime = 0;
        this.row = 0;
        this.hole = 0;
        this.inside = 0;
        this.selected = false;
        this.downloaded = true;
    }

    public DetonatorInfoBean(String address, int delay, int row, int hole, int inside, boolean downloaded) {
        this.address = address;
        this.delayTime = delay;
        this.row = row;
        this.hole = hole;
        this.inside = inside;
        this.selected = false;
        this.downloaded = downloaded;
    }

    private DetonatorInfoBean(Parcel source) {
        this.address = source.readString();
        this.delayTime = source.readInt();
        this.row = source.readInt();
        this.hole = source.readInt();
        this.inside = source.readInt();
        this.selected = false;
        this.downloaded = source.readInt() == 1;
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

    public String toString() {
        return this.address + "," +
                this.delayTime + "," +
                this.row + "," +
                this.hole + "," +
                this.inside;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeInt(delayTime);
        dest.writeInt(row);
        dest.writeInt(hole);
        dest.writeInt(inside);
        dest.writeInt(downloaded ? 1 : 0);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("address", this.address);
        jsonObject.put("delayTime", this.delayTime);
        jsonObject.put("row", this.row);
        jsonObject.put("hole", this.hole);
        jsonObject.put("inside", this.inside);
        jsonObject.put("downloaded", this.downloaded);
        return jsonObject;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        this.address = jsonObject.getString("address");
        this.delayTime = jsonObject.getInt("delayTime");
        this.row = jsonObject.getInt("row");
        this.hole = jsonObject.getInt("hole");
        this.inside = jsonObject.getInt("inside");
        this.downloaded = jsonObject.getBoolean("downloaded");
    }

    @Override
    public int compareTo(@NonNull DetonatorInfoBean o) {
        return this.getDelayTime() - o.getDelayTime();
    }
}
