package com.lqc.common.protocol.notification;

/** Broadcast when a message was deleted, so clients remove it. */
public class MessageDeletedNotification {
    private long messageId;
    private Long roomId;
    private Long recipientId;

    public MessageDeletedNotification() {}

    public MessageDeletedNotification(long messageId, Long roomId, Long recipientId) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.recipientId = recipientId;
    }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
}
