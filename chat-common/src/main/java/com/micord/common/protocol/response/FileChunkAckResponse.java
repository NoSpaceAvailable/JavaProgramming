package com.micord.common.protocol.response;

public class FileChunkAckResponse {
    private boolean success;
    private String message;
    private String fileId;
    private int chunkIndex;
    private long bytesReceived;

    public FileChunkAckResponse() {}

    public FileChunkAckResponse(boolean success, String message, String fileId,
                                 int chunkIndex, long bytesReceived) {
        this.success = success;
        this.message = message;
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.bytesReceived = bytesReceived;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public long getBytesReceived() { return bytesReceived; }
    public void setBytesReceived(long bytesReceived) { this.bytesReceived = bytesReceived; }
}
