package com.micord.common.protocol.notification;

/**
 * Broadcast to a conversation's participants while {@code senderId} is typing.
 * {@code roomId} is set for a room; {@code recipientId} is set for a DM
 * (the id of the user who should see the indicator).
 */
public class UserTypingNotification {
    private Long roomId;
    private Long recipientId;
    private long senderId;
    private String senderName;
    private boolean typing;

    public UserTypingNotification() {}

    public UserTypingNotification(Long roomId, Long recipientId, long senderId, String senderName, boolean typing) {
        this.roomId = roomId;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.typing = typing;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public boolean isTyping() { return typing; }
    public void setTyping(boolean typing) { this.typing = typing; }
}
