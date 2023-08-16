package com.leon.detonator.base;

import com.google.gson.Gson;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.UploadDetonatorBean;
import com.leon.detonator.bean.UploadListResultBean;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

public class SynchronizeExplodeRecord extends Thread {
    private List<ExplosionRecordBean> list;
    private final BaseApplication myApp;
    public static boolean uploading;
    private String token;
    private int index;

    public SynchronizeExplodeRecord(BaseApplication app) {
        myApp = app;
    }

    @Override
    public void run() {
        super.run();
        index = 0;
        try {
            list = DbUtil.getExplosionRecordList();
            uploading = true;
            uploadNext();
            while (uploading)
                Thread.sleep(50);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void uploadNext() {
        for (; index < list.size(); index++) {
            ExplosionRecordBean bean = list.get(index);
            if (!bean.isSynchronize()) {
                token = myApp.makeToken();
                Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_UPLOAD_EXPLODE_LIST);
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
                    List<DetonatorBean> temp = DbUtil.getDetonatorList(bean.getId());
                    List<UploadDetonatorBean> detonatorList = new ArrayList<>();
                    for (DetonatorBean b1 : temp) {
                        UploadDetonatorBean b2 = new UploadDetonatorBean();
                        b2.setDSC(b1.getAddress());
                        b2.setBlastDelayTime(b1.getDelayTime());
                        b2.setBlastHole(b1.getHole());
                        b2.setBlastRow(b1.getRow());
                        b2.setBlastInside(b1.getInside());
                        detonatorList.add(b2);
                    }
                    BaseApplication.writeFile("Upload: " + bean.getName());
                    params.put("EnvironmentType".toLowerCase(), bean.isTunnel() ? "DownHole" : "OpenAir");
                    params.put("BlastTime".toLowerCase(), formatter.format(bean.getExplodeTime()));
                    params.put("BlastLat".toLowerCase(), bean.getLat() + "");
                    params.put("BlastLng".toLowerCase(), bean.getLng() + "");
                    if (bean.getUploadServer() > 0 && bean.getUploadServer() < ConstantUtils.UPLOAD_HOST.length) {
                        String[] server = ConstantUtils.UPLOAD_HOST[bean.getUploadServer()][1].split(":");
                        if (server.length == 2) {
                            params.put("zbServerIp".toLowerCase(), server[0]);
                            params.put("zbServerPort".toLowerCase(), server[1]);
                        }
                    }
                    final boolean success = bean.getUploadServer() != -1;
                    params.put("isZbUploadSuccess".toLowerCase(), success + "");
                    params.put("zbUploadSuccessTime".toLowerCase(), null == bean.getUploadTime() ? "" : formatter.format(bean.getUploadTime()));
                    params.put("detonator", new Gson().toJson(detonatorList));
                    params.put("signature", myApp.signature(params));
                    OkHttpUtils.post()
                            .url(ConstantUtils.HOST_URL)
                            .params(params)
                            .build().execute(new Callback<UploadListResultBean>() {
                                @Override
                                public UploadListResultBean parseNetworkResponse(Response response, int i) throws Exception {
                                    if (response.body() != null) {
                                        String string = response.body().string();
                                        return BaseApplication.jsonFromString(string, UploadListResultBean.class);
                                    }
                                    return null;
                                }

                                @Override
                                public void onError(Call call, Exception e, int i) {
                                    BaseApplication.writeFile("OkHttpUtils onError");
                                    BaseApplication.writeErrorLog(e);
                                    uploading = false;
                                }

                                @Override
                                public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                    if (null != uploadListResultBean) {
                                        if (uploadListResultBean.getToken().equals(token)) {
                                            if (uploadListResultBean.isStatus()) {
                                                bean.setSynchronize(true);
                                                DbUtil.updateExplosionRecord(bean);
                                                index++;
                                                uploadNext();
                                            } else
                                                uploading = false;
                                        } else
                                            uploading = false;
                                        BaseApplication.writeFile(uploadListResultBean.getDescription());
                                    }
                                }
                            });
                } catch (Exception e) {
                    BaseApplication.writeFile("uploadNext");
                    BaseApplication.writeErrorLog(e);
                    uploading = false;
                }
                return;
            }
        }
        uploading = false;
    }
}
