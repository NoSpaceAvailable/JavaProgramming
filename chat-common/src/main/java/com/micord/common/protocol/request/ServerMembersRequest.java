package com.micord.common.protocol.request;

/** Requests the member list (with roles) of a server. */
public class ServerMembersRequest {
    private long serverId;

    public ServerMembersRequest() {}

    public ServerMembersRequest(long serverId) {
        this.serverId = serverId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
}
