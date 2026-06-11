package com.micord.common.protocol.request;

/** Creates a new text channel within a server. */
public class CreateChannelRequest {
    private long serverId;
    private String name;

    public CreateChannelRequest() {}

    public CreateChannelRequest(long serverId, String name) {
        this.serverId = serverId;
        this.name = name;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
