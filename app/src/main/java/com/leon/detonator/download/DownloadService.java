package com.leon.detonator.download;

import android.content.Context;

import com.leon.detonator.base.BaseApplication;

import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService {
    private int downloadedSize = 0;               //已下载的文件长度
    private final int threadCount = 3;
    private int fileSize = 0;
    private final Map<Integer, Integer> data = new ConcurrentHashMap<>();  //缓存个条线程的下载的长度
    private final DBService dbService;
    private final DownloadThread[] threads;        //根据线程数设置下载的线程池
    private boolean exited = false;
    private final String downloadUrl;
    private final String fileName;

    public DownloadService(Context context, String downloadUrl, String fileName) {
        dbService = new DBService(context);
        this.threads = new DownloadThread[threadCount];
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    /**
     * 退出下载
     */
    public void exit() {
        exited = true;    //将退出的标志设置为true;
    }

    public boolean getExited() {
        return exited;
    }

    /**
     * 累计已下载的大小
     * 使用同步锁来解决并发的访问问题
     */
    protected synchronized void append(int size) {
        //把实时下载的长度加入到总的下载长度中
        downloadedSize += size;
    }

    /**
     * 更新指定线程最后下载的位置
     *
     * @param threadId 线程id
     * @param pos      最后下载的位置
     */
    protected synchronized void update(int threadId, int pos) {
        try {
            data.put(threadId, pos);
            DWManagerInfo dwInfo = new DWManagerInfo();
            dwInfo.setDownloadPath(downloadUrl);
            dwInfo.setThreadId(threadId);
            dwInfo.setDownloadLength(pos);
            dbService.updateItem(dwInfo);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        //把指定线程id的线程赋予最新的下载长度,以前的值会被覆盖掉
        data.put(threadId, pos);
        //更新数据库中制定线程的下载长度
    }

    private String generateFile(long fileLength, boolean generateFile) throws Exception {
        RandomAccessFile file = null;
        try {
            if (generateFile) {
                if (downloadedSize == 0 || downloadedSize >= fileLength) {
                    file = new RandomAccessFile(fileName, "rwd");
                    file.setLength(fileLength);
                    file.close();
                }
            }
            return fileName;
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new Exception("GenerateTempFile error: " + e.getMessage(), e);
        } finally {
            try {
                if (file != null)
                    file.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }

    }

    public int getRemainDownloadLen(int threadCount, long fileLength) {
        int block = 0;
        try {
            block = (int) fileLength % threadCount == 0 ? (int) fileLength / threadCount :
                    (int) fileLength / threadCount + 1;
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        return block;
    }

    public void download(boolean generateFile, DownloadProgressListener downloadProgressListener) throws Exception {
        try {
            fileSize = getDownloadFileSize(downloadUrl);
            //把所有的DB内已经存在的size放入全局的data中，以作缓存
            List<DWManagerInfo> dwInfoList = dbService.getData(downloadUrl);
            if (dwInfoList.size() > 0) {
                for (DWManagerInfo dwInfo : dwInfoList) {
                    downloadedSize += dwInfo.getDownloadLength();
                    data.put(dwInfo.getThreadId(), dwInfo.getDownloadLength());
                }
            } else {
                for (int i = 0; i < threadCount; i++) {
                    data.put(i + 1, 0);
                }
            }
            int block = getRemainDownloadLen(3, fileSize);
            String saveFileName = generateFile(fileSize, generateFile);//生成一个Random空文件并把文件长度设置好
            for (int i = 0; i < threads.length; i++) {//开启线程进行下载
                int downLength = 0;
                if (data.size() > 0) {
                    Integer j = data.get(i + 1);
                    if (j != null)
                        downLength = j;
                }
                //通过特定的线程id获取该线程已经下载的数据长度
                //判断线程是否已经完成下载,否则继续下载
                if (downLength < block && downloadedSize < fileSize) {
                    //初始化特定id的线程
                    threads[i] = new DownloadThread(this, downloadUrl, saveFileName, block, downLength, i + 1);
                    //设置线程优先级,Thread.NORM_PRIORITY = 5;
                    //Thread.MIN_PRIORITY = 1;Thread.MAX_PRIORITY = 10,数值越大优先级越高
                    threads[i].setPriority(7);
                    threads[i].start();    //启动线程
                } else {
                    threads[i] = null;   //表明线程已完成下载任务
                }
            }
            dbService.delete(downloadUrl);
            dbService.save(downloadUrl, data);
            //把下载的实时数据写入数据库中
            boolean notFinish = true;
            //下载未完成
            while (notFinish) {
                // 循环判断所有线程是否完成下载
                Thread.sleep(300);
                notFinish = false;
                for (int i = 0; i < threadCount; i++) {
                    if (threads[i] != null && !threads[i].isFinish()) {
                        //如果发现线程未完成下载
                        notFinish = true;
                        //设置标志为下载没有完成,以便于外层while循环不断check;
                        break;
                    }
                }
                if (downloadProgressListener != null) {
                    downloadProgressListener.onDownloadSize(downloadedSize);
                }
                //通知目前已经下载完成的数据长度
            }
            if (downloadedSize == fileSize) {
                dbService.delete(downloadUrl);
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new Exception(">>>>>>download error: " + e.getMessage(), e);
        }

    }

    public int getDownloadFileSize(String downloadUrl) throws Exception {
        int size = -1;
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
                .readTimeout(10, TimeUnit.SECONDS).build();//设置读取超时时间
        Request request = new Request.Builder().url(downloadUrl)//请求接口，如果需要传参拼接到接口后面
                .build(); //创建Request对象
        Response response = null;
        try {
            Call call = client.newCall(request);
            response = call.execute();
            if (200 == response.code() && response.body() != null) {
                try {
                    size = (int) response.body().contentLength();
                    //fileSizeListener.onHttpResponse((int) size);
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new Exception(">>>>>>getDownloadFileSize from->" + downloadUrl + "\nerror: " + e.getMessage(), e);
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        return size;
    }
}
