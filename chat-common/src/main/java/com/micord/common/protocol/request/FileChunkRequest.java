package com.micord.common.protocol.request;

public class FileChunkRequest {
    private String fileId;
    private int chunkIndex;
    private String data; // Base64-encoded

    public FileChunkRequest() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
