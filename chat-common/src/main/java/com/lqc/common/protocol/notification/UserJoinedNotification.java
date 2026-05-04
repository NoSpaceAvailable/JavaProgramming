package com.lqc.common.protocol.notification;

public class UserJoinedNotification {
    private long roomId;
    private long userId;
    private String displayName;

    public UserJoinedNotification() {}

    public UserJoinedNotification(long roomId, long userId, String displayName) {
        this.roomId = roomId;
        this.userId = userId;
        this.displayName = displayName;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
