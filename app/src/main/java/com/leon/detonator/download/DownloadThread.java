package com.leon.detonator.download;

import androidx.annotation.NonNull;

import com.leon.detonator.base.BaseApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadThread extends Thread {
    private static final String TAG = "DemoContinueDownload";
    private final String downloadUrl;              //下载的URL
    private final int block;                //每条线程下载的大小
    private final int threadId;            //初始化线程id设置
    private int downLength;             //该线程已下载的数据长度
    private boolean finish = false;         //该线程是否完成下载的标志
    private final DownloadService downloader;
    private final String saveFileName;

    public DownloadThread(DownloadService downloader, String downloadUrl, String saveFileName, int block, int downLength, int threadId) {
        this.downloader = downloader;
        this.downloadUrl = downloadUrl;
        this.saveFileName = saveFileName;
        this.block = block;
        this.downLength = downLength;
        this.threadId = threadId;
    }

    @Override
    public void run() {
        if (downLength < block) {
            int startPos = block * (threadId - 1) + downLength;//开始位置
            int endPos = block * threadId - 1;//结束位置
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
                    .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
                    .build();
            Request request = new Request.Builder().get().url(downloadUrl)//请求接口，如果需要传参拼接到接口后面
                    .addHeader("Referer", downloadUrl)
                    .addHeader("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, " +
                            "application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, " +
                            "application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, " +
                            "application/vnd.ms-powerpoint, application/msword, */*")
                    .addHeader("connection", "keep-alive")
                    .addHeader("Range", "bytes=" + startPos + "-" + endPos)
                    .addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET " +
                            "CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET " +
                            "CLR 3.5.30729)")
                    .build(); //创建Request对象
            //Log.i(TAG, ">>>>>>线程" + threadId + "开始下载...Range: bytes=" + startPos + "-" + endPos);
            Call call = client.newCall(request);
            //异步请求
            call.enqueue(new Callback() {
                //失败的请求
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    BaseApplication.writeErrorLog(e);
                    finish = true;
                }

                //结束的回调
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    InputStream is = null;
                    RandomAccessFile threadFile = null;
                    try {
                        if ((response.code() == 200 || response.code() == 206) && response.body() != null) {
                            is = response.body().byteStream();
                            byte[] buffer = new byte[1024];
                            int offset;
                            threadFile = new RandomAccessFile(saveFileName, "rwd");
                            threadFile.seek(startPos);
                            while (!downloader.getExited() && (offset = is.read(buffer, 0, 1024)) != -1) {
                                threadFile.write(buffer, 0, offset);
                                downLength += offset;
//                                downloader.update(threadId, downLength);
                                downloader.append(offset);
                            }
                            finish = true;
                            threadFile.close();
                        }
                    } catch (Exception e) {
                        downLength = -1;               //设置该线程已经下载的长度为-1
                        BaseApplication.writeErrorLog(e);
                        finish = true;
                    } finally {
                        try {
                            if (threadFile != null)
                                threadFile.close();
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                        try {
                            if (is != null)
                                is.close();
                        } catch (Exception e) {
                            BaseApplication.writeErrorLog(e);
                        }
                    }

                }
            });
        }
    }

    /**
     * 下载是否完成
     */
    public boolean isFinish() {
        return finish;
    }

    /**
     * 已经下载的内容大小
     *
     * @return 如果返回值为-1,代表下载失败
     */
    public long getDownLength() {
        return downLength;
    }
}
