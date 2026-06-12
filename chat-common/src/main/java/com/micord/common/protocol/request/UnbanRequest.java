package com.micord.common.protocol.request;

/** Lifts a ban so the user can rejoin the server (Admin+). */
public class UnbanRequest {
    private long serverId;
    private long userId;

    public UnbanRequest() {}

    public UnbanRequest(long serverId, long userId) {
        this.serverId = serverId;
        this.userId = userId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
