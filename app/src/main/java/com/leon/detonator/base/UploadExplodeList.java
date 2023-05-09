package com.leon.detonator.base;

import com.google.gson.Gson;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bean.UploadDetonatorBean;
import com.leon.detonator.bean.UploadListResultBean;
import com.leon.detonator.bean.UploadServerBean;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Response;

public class UploadExplodeList extends Thread {
    private static UploadExplodeList instance;
    private BaseApplication app;
    private String token;
    private int index;
    private boolean keepAwake;
    private List<UploadServerBean> list;

    public static UploadExplodeList getInstance() {
        if (instance == null)
            instance = new UploadExplodeList();
        return instance;
    }

    public UploadExplodeList setApp(BaseApplication app) {
        this.app = app;
        return this;
    }

    @Override
    public void run() {
        if (app != null) {
            index = 0;
            list = new ArrayList<>();
            app.readFromFile(FilePath.FILE_UPLOAD_LIST, list, UploadServerBean.class);
            uploadNext();
            keepAwake = true;
        }
        while (keepAwake) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        super.run();
        interrupt();
    }

    private void uploadNext() {
        for (; index < list.size(); index++) {
            UploadServerBean bean = list.get(index);
            if (!bean.isUploadServer()) {
                File file = new File(FilePath.FILE_DETONATE_RECORDS + "/" + bean.getFile());
                if (file.exists()) {
                    token = app.makeToken();
                    Map<String, String> params = app.makeParams(token, MethodUtils.METHOD_UPLOAD_EXPLODE_LIST);
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
                        List<DetonatorInfoBean> temp = new ArrayList<>();
                        app.readFromFile(file.getAbsolutePath(), temp, DetonatorInfoBean.class);
                        String[] info = file.getName().split("_");
                        if (5 == info.length) {
                            info[4] = info[4].substring(0, info[4].length() - 4);
                            List<UploadDetonatorBean> detonatorList = new ArrayList<>();
                            for (DetonatorInfoBean b1 : temp) {
                                UploadDetonatorBean b2 = new UploadDetonatorBean();
                                b2.setDSC(b1.getAddress());
                                b2.setBlastDelayTime(b1.getDelayTime());
                                b2.setBlastHole(b1.getHole());
                                b2.setBlastRow(b1.getRow());
                                b2.setBlastInside(b1.getInside());
                                detonatorList.add(b2);
                            }
                            BaseApplication.writeFile("Upload: " + file.getName());
                            params.put("EnvironmentType".toLowerCase(), info[0].startsWith("O") ? "OpenAir" : "DownHole");
                            params.put("BlastTime".toLowerCase(), info[1]);
                            params.put("BlastLat".toLowerCase(), info[3]);
                            params.put("BlastLng".toLowerCase(), info[4]);
                            if (null != bean.getServer() && !bean.getServer().isEmpty()) {
                                String[] server = bean.getServer().split(":");
                                if (server.length == 2) {
                                    params.put("zbServerIp".toLowerCase(), server[0]);
                                    params.put("zbServerPort".toLowerCase(), server[1]);
                                }
                            }
                            params.put("isZbUploadSuccess".toLowerCase(), bean.isUploaded() + "");
                            params.put("zbUploadSuccessTime".toLowerCase(), null == bean.getUploadTime() ? "" : formatter.format(bean.getUploadTime()));
                            params.put("detonator", new Gson().toJson(detonatorList));
                            params.put("signature", app.signature(params));
                            OkHttpUtils.post()
                                    .url(ConstantUtils.HOST_URL)
                                    .params(params)
                                    .build().execute(new Callback<UploadListResultBean>() {
                                        @Override
                                        public UploadListResultBean parseNetworkResponse(Response response, int i) throws Exception {
                                            if (response.body() != null) {
                                                String string = Objects.requireNonNull(response.body()).string();
                                                return BaseApplication.jsonFromString(string, UploadListResultBean.class);
                                            }
                                            return null;
                                        }

                                        @Override
                                        public void onError(Call call, Exception e, int i) {
                                            BaseApplication.writeErrorLog(e);
                                            finished();
                                        }

                                        @Override
                                        public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                            if (null != uploadListResultBean) {
                                                if (uploadListResultBean.getToken().equals(token)) {
                                                    if (uploadListResultBean.isStatus()) {
                                                        bean.setUploadServer(true);
                                                        try {
                                                            app.writeToFile(FilePath.FILE_UPLOAD_LIST, list);
                                                        } catch (Exception e) {
                                                            BaseApplication.writeErrorLog(e);
                                                        }
                                                        index++;
                                                        uploadNext();
                                                    } else
                                                        finished();
                                                } else
                                                    finished();
                                                BaseApplication.writeFile(uploadListResultBean.getDescription());
                                            }
                                        }
                                    });
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                    return;
                }
            }
        }
        finished();
    }

    private void finished() {
        instance = null;
        keepAwake = false;
    }
}
