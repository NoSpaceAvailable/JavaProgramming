package com.micord.common.protocol.notification;

/** Pushed to a server's online members when its membership or roles change. */
public class ServerMembersChangedNotification {
    private long serverId;

    public ServerMembersChangedNotification() {}

    public ServerMembersChangedNotification(long serverId) {
        this.serverId = serverId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
}
