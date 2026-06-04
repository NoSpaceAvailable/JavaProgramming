package com.micord.common.protocol.response;

public class AvatarResponse {
    private long userId;
    private String avatarData;
    private String mimeType;

    public AvatarResponse() {}

    public AvatarResponse(long userId, String avatarData, String mimeType) {
        this.userId = userId;
        this.avatarData = avatarData;
        this.mimeType = mimeType;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getAvatarData() { return avatarData; }
    public void setAvatarData(String avatarData) { this.avatarData = avatarData; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
