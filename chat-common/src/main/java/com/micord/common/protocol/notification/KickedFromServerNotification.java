package com.micord.common.protocol.notification;

/** Pushed to a user who was kicked or banned from a server, so their UI removes it. */
public class KickedFromServerNotification {
    private long serverId;
    private String serverName;
    private String reason;
    private boolean banned;

    public KickedFromServerNotification() {}

    public KickedFromServerNotification(long serverId, String serverName, String reason, boolean banned) {
        this.serverId = serverId;
        this.serverName = serverName;
        this.reason = reason;
        this.banned = banned;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
