package com.micord.common.protocol.request;

public class PrivateMessageRequest {
    private long recipientId;
    private String content;
    private String messageType;

    public PrivateMessageRequest() {}

    public PrivateMessageRequest(long recipientId, String content) {
        this.recipientId = recipientId;
        this.content = content;
    }

    public PrivateMessageRequest(long recipientId, String content, String messageType) {
        this.recipientId = recipientId;
        this.content = content;
        this.messageType = messageType;
    }

    public long getRecipientId() { return recipientId; }
    public void setRecipientId(long recipientId) { this.recipientId = recipientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
