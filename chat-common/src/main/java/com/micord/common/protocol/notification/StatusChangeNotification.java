package com.micord.common.protocol.notification;

import com.micord.common.model.UserStatus;

public class StatusChangeNotification {
    private long userId;
    private String displayName;
    private UserStatus status;

    public StatusChangeNotification() {}

    public StatusChangeNotification(long userId, String displayName, UserStatus status) {
        this.userId = userId;
        this.displayName = displayName;
        this.status = status;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
