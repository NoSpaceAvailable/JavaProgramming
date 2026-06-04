package com.micord.common.protocol.request;

public class FileUploadCompleteRequest {
    private String fileId;
    private String checksum; // SHA-256

    public FileUploadCompleteRequest() {}

    public FileUploadCompleteRequest(String fileId, String checksum) {
        this.fileId = fileId;
        this.checksum = checksum;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
}
