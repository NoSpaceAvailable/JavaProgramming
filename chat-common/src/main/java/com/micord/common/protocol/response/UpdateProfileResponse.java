package com.micord.common.protocol.response;

public class UpdateProfileResponse {
    private boolean success;
    private String message;
    private String displayName;
    private String avatarUrl;

    public UpdateProfileResponse() {}

    public UpdateProfileResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UpdateProfileResponse(boolean success, String displayName, String avatarUrl) {
        this.success = success;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
