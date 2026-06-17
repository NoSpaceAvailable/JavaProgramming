package com.micord.common.protocol.request;

/** Requests the list of users banned from a server (Admin+). */
public class ListBansRequest {
    private long serverId;

    public ListBansRequest() {}

    public ListBansRequest(long serverId) {
        this.serverId = serverId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
}
