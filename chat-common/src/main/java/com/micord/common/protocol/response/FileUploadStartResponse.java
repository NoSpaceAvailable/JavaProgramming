package com.micord.common.protocol.response;

public class FileUploadStartResponse {
    private boolean success;
    private String message;
    private String fileId;
    private int chunkSize;

    public FileUploadStartResponse() {}

    public FileUploadStartResponse(boolean success, String message, String fileId, int chunkSize) {
        this.success = success;
        this.message = message;
        this.fileId = fileId;
        this.chunkSize = chunkSize;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
}
