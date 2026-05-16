package com.lqc.common.protocol.request;

public class UpdateProfileRequest {
    private String displayName;
    private String avatarData;
    private String avatarMimeType;

    public UpdateProfileRequest() {}

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarData() { return avatarData; }
    public void setAvatarData(String avatarData) { this.avatarData = avatarData; }

    public String getAvatarMimeType() { return avatarMimeType; }
    public void setAvatarMimeType(String avatarMimeType) { this.avatarMimeType = avatarMimeType; }
}
