package com.micord.common.protocol.request;

/** Sent by a message's sender to delete it. */
public class DeleteMessageRequest {
    private long messageId;

    public DeleteMessageRequest() {}

    public DeleteMessageRequest(long messageId) {
        this.messageId = messageId;
    }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }
}
