package com.micord.common.protocol.request;

/** Sent by a message's sender to change its text content. */
public class EditMessageRequest {
    private long messageId;
    private String content;

    public EditMessageRequest() {}

    public EditMessageRequest(long messageId, String content) {
        this.messageId = messageId;
        this.content = content;
    }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
