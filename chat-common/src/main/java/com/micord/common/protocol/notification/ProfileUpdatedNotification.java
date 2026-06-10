package com.micord.common.protocol.notification;

public class ProfileUpdatedNotification {
    private long userId;
    private String displayName;
    private boolean avatarUpdated;

    public ProfileUpdatedNotification() {}

    public ProfileUpdatedNotification(long userId, String displayName, boolean avatarUpdated) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUpdated = avatarUpdated;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isAvatarUpdated() { return avatarUpdated; }
    public void setAvatarUpdated(boolean avatarUpdated) { this.avatarUpdated = avatarUpdated; }
}
