package com.leon.detonator.base;

import com.google.gson.Gson;
import com.leon.detonator.bean.DanLinDetonatorBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.EnterpriseBean;
import com.leon.detonator.bean.ExplosionRecordBean;
import com.leon.detonator.bean.UploadDetonatorBean;
import com.leon.detonator.bean.UploadListResultBean;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.MethodUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Response;

public class SynchronizeExplodeRecord extends Thread {
    public static boolean uploading;
    private List<ExplosionRecordBean> list;
    private final BaseApplication myApp;
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
                Map<String, String> params = myApp.makeParams(token, MethodUtils.METHOD_UPLOAD_EXPLODE_LIST_V2);
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat(ConstantUtils.DATE_FORMAT_FULL, Locale.getDefault());
                    List<DetonatorBean> temp = DbUtil.getDetonatorList(bean.getId());
                    List<UploadDetonatorBean> detonatorList = new ArrayList<>();
                    for (DetonatorBean b1 : temp) {
                        UploadDetonatorBean b2 = new UploadDetonatorBean();
                        b2.setDSC(b1.getAddress());
                        b2.setUID(b1.getUID());
                        b2.setBlastDelayTime(b1.getDelayTime());
                        b2.setBlastHole(b1.getHole());
                        b2.setBlastRow(b1.getRow());
                        b2.setBlastInside(b1.getInside());
                        detonatorList.add(b2);
                    }
                    if (bean.getUploadServer() == 0 || bean.getUploadServer() == 3) {
                        long danLinId = DbUtil.getDanLinIdByExplodeRecordId(list.get(index).getId());
                        if (danLinId >= 0) {
                            DanLinDetonatorBean danLinDetonatorBean = DbUtil.getDanLinDownloadDetonatorById(danLinId);
                            EnterpriseBean enterpriseBean = DbUtil.getEnterpriseById(danLinDetonatorBean.getEnterpriseId());
                            if (danLinDetonatorBean.isOffline()) {
                                params.put("IsMBOfflineDownloadRule".toLowerCase(), true + "");
                                params.put("IsMBOnlineDownloadRule".toLowerCase(), false + "");
                            } else {
                                params.put("IsMBOfflineDownloadRule".toLowerCase(), false + "");
                                params.put("IsMBOnlineDownloadRule".toLowerCase(), true + "");
                            }
                            params.put("MBDownloadSuccessTime".toLowerCase(), danLinDetonatorBean.getResult().getSqrq());
                            if (list.get(index).getUploadTime() != null) {
                                params.put("IsMBDetonatorUploadRule".toLowerCase(), true + "");
                                params.put("MBUploadSuccessTime".toLowerCase(), formatter.format(bean.getUploadTime()));
                            } else
                                params.put("IsMBDetonatorUploadRule".toLowerCase(), false + "");
                            params.put("MBhtid".toLowerCase(), enterpriseBean.getContract());
                            params.put("MBxmbh".toLowerCase(), enterpriseBean.getProject());
                            params.put("MBdwdm".toLowerCase(), enterpriseBean.getCode());
                            params.put("MBbprysfz".toLowerCase(), enterpriseBean.getBlasterId());
                        }
                        params.put("isZbUploadSuccess".toLowerCase(), false + "");
                    } else {
                        params.put("isZbUploadSuccess".toLowerCase(), (bean.getUploadServer() != -1) + "");
                    }
                    if (params.get("IsMBOfflineDownloadRule".toLowerCase()) == null) {
                        params.put("IsMBOfflineDownloadRule".toLowerCase(), false + "");
                        params.put("IsMBOnlineDownloadRule".toLowerCase(), false + "");
                        params.put("IsMBDetonatorUploadRule".toLowerCase(), false + "");
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
                    params.put("zbUploadSuccessTime".toLowerCase(), null == bean.getUploadTime() ? "" : formatter.format(bean.getUploadTime()));
                    params.put("detonator", new Gson().toJson(detonatorList));
                    params.put("signature", myApp.signature(params));
                    JSONObject json = new JSONObject();
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        json.put(key, value);
                    }
                    BaseApplication.writeFile(new Gson().toJson(json));
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
                                    BaseApplication.writeFile("OkHttpUtils onError");
                                    BaseApplication.writeErrorLog(e);
                                    uploading = false;
                                }

                                @Override
                                public void onResponse(UploadListResultBean uploadListResultBean, int i) {
                                    if (null != uploadListResultBean) {
                                        BaseApplication.writeFile(uploadListResultBean.getDescription());
                                        if (uploadListResultBean.getToken().equals(token)) {
                                            if (uploadListResultBean.isStatus()) {
                                                if (bean.isDeleted()) {
                                                    List<Long> list1 = new ArrayList<>();
                                                    list1.add(bean.getId());
                                                    DbUtil.deleteScheme(list1);
                                                } else {
                                                    bean.setSynchronize(true);
                                                    DbUtil.updateExplosionRecord(bean);
                                                }
                                                index++;
                                                uploadNext();
                                                return;
                                            }
                                        }
                                        uploading = false;
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
