package com.leon.detonator.Bean;

import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.BaseJSONBean;
import com.leon.detonator.Util.ConstantUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadServerBean implements BaseJSONBean {
    private String file;
    private String server;
    private boolean uploaded;
    private Date uploadTime;
    private boolean uploadServer;

    public UploadServerBean() {
        server = "";
        uploaded = false;
        uploadServer = false;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    /******
     *
     * @return 是否已经上传中爆网
     */
    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    /******
     *
     * @return 是否已经上传到广西中爆服务器
     */
    public boolean isUploadServer() {
        return uploadServer;
    }

    public void setUploadServer(boolean uploadServer) {
        this.uploadServer = uploadServer;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
        jsonObject.put("file", this.file);
        jsonObject.put("server", this.server);
        jsonObject.put("uploaded", this.uploaded);
        jsonObject.put("uploadTime", null == this.uploadTime ? "" : df.format(this.uploadTime));
        jsonObject.put("uploadFlag", this.uploadServer);
        return jsonObject;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        SimpleDateFormat df = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.CHINA);
        this.file = jsonObject.getString("file");
        this.server = jsonObject.getString("server");
        this.uploaded = jsonObject.getBoolean("uploaded");
        try {
            String date = jsonObject.getString("uploadTime");
            if (!date.isEmpty())
                this.uploadTime = df.parse(date);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        this.uploadServer = jsonObject.getBoolean("uploadFlag");
    }
}
