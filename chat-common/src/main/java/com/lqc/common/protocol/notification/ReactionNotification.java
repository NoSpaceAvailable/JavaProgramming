package com.lqc.common.protocol.notification;

public class ReactionNotification {
    private long messageId;
    private long roomId;
    private long userId;
    private String displayName;
    private String emoji;
    private boolean added; // true = added, false = removed

    public ReactionNotification() {}

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public boolean isAdded() { return added; }
    public void setAdded(boolean added) { this.added = added; }
}
