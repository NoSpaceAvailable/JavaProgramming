package com.lqc.common.protocol.request;

public class PrivateMessageRequest {
    private long recipientId;
    private String content;

    public PrivateMessageRequest() {}

    public PrivateMessageRequest(long recipientId, String content) {
        this.recipientId = recipientId;
        this.content = content;
    }

    public long getRecipientId() { return recipientId; }
    public void setRecipientId(long recipientId) { this.recipientId = recipientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
