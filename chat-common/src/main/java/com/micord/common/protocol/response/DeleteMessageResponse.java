package com.micord.common.protocol.response;

/** Reply to the sender after attempting to delete a message. */
public class DeleteMessageResponse {
    private boolean success;
    private String message;
    private long messageId;

    public DeleteMessageResponse() {}

    public DeleteMessageResponse(boolean success, String message, long messageId) {
        this.success = success;
        this.message = message;
        this.messageId = messageId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }
}
