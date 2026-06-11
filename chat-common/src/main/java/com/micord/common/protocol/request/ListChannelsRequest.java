package com.micord.common.protocol.request;

/** Lists the text channels of a server the user belongs to. */
public class ListChannelsRequest {
    private long serverId;

    public ListChannelsRequest() {}

    public ListChannelsRequest(long serverId) {
        this.serverId = serverId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
}
