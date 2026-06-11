package com.micord.common.protocol.request;

/** Bans a member from a server (Admin+); blocks rejoining via invite code. */
public class BanFromServerRequest {
    private long serverId;
    private long userId;
    private String reason;

    public BanFromServerRequest() {}

    public BanFromServerRequest(long serverId, long userId, String reason) {
        this.serverId = serverId;
        this.userId = userId;
        this.reason = reason;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
