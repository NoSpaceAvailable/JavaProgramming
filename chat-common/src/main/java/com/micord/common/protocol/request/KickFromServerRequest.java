package com.micord.common.protocol.request;

/** Removes a member from a server (Moderator+). */
public class KickFromServerRequest {
    private long serverId;
    private long userId;

    public KickFromServerRequest() {}

    public KickFromServerRequest(long serverId, long userId) {
        this.serverId = serverId;
        this.userId = userId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
