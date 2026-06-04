package com.micord.common.protocol.notification;

/** Broadcast when a message's content was edited, so clients update it in place. */
public class MessageEditedNotification {
    private long messageId;
    private Long roomId;
    private Long recipientId;
    private String content;

    public MessageEditedNotification() {}

    public MessageEditedNotification(long messageId, Long roomId, Long recipientId, String content) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.recipientId = recipientId;
        this.content = content;
    }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
