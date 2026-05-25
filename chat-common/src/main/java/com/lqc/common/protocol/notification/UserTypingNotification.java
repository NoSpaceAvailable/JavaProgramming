package com.lqc.common.protocol.notification;

/**
 * Broadcast to a conversation's participants while {@code userId} is typing.
 * {@code roomId} is set for a room; {@code recipientId} is set for a DM
 * (the id of the user who should see the indicator).
 */
public class UserTypingNotification {
    private Long roomId;
    private Long recipientId;
    private long userId;
    private String displayName;

    public UserTypingNotification() {}

    public UserTypingNotification(Long roomId, Long recipientId, long userId, String displayName) {
        this.roomId = roomId;
        this.recipientId = recipientId;
        this.userId = userId;
        this.displayName = displayName;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
