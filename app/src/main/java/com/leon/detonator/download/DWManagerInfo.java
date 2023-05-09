package com.leon.detonator.download;

import java.io.Serializable;

public class DWManagerInfo implements Serializable {
    private int dwId=0;
    private int threadId=0;
    private int downloadLength=0;
    private String downloadPath="";

    public int getDwId() {
        return dwId;
    }

    public void setDwId(int dwId) {
        this.dwId = dwId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public int getDownloadLength() {
        return downloadLength;
    }

    public void setDownloadLength(int downloadLength) {
        this.downloadLength = downloadLength;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }
}
