package com.lqc.common.protocol.request;

public class FileDownloadRequest {
    private long fileId;

    public FileDownloadRequest() {}

    public FileDownloadRequest(long fileId) {
        this.fileId = fileId;
    }

    public long getFileId() { return fileId; }
    public void setFileId(long fileId) { this.fileId = fileId; }
}
