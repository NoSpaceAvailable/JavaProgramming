package com.lqc.common.protocol.request;

public class AvatarRequest {
    private long userId;

    public AvatarRequest() {}

    public AvatarRequest(long userId) {
        this.userId = userId;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
