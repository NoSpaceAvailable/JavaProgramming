package com.lqc.common.protocol.response;

public class FileChunkResponse {
    private long attachmentId;
    private int chunkIndex;
    private int totalChunks;
    private String data; // Base64-encoded
    private boolean last;

    public FileChunkResponse() {}

    public long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(long attachmentId) { this.attachmentId = attachmentId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}
