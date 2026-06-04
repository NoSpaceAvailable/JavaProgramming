package com.micord.common.protocol.response;

public class FileUploadCompleteResponse {
    private boolean success;
    private String message;
    private long messageId;
    private long attachmentId;

    public FileUploadCompleteResponse() {}

    public FileUploadCompleteResponse(boolean success, String message, long messageId, long attachmentId) {
        this.success = success;
        this.message = message;
        this.messageId = messageId;
        this.attachmentId = attachmentId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(long attachmentId) { this.attachmentId = attachmentId; }
}
