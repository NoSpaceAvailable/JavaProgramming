package com.micord.common.protocol.response;

import com.micord.common.model.Room;

import java.util.List;

public class ChannelListResponse {
    private long serverId;
    private List<Room> channels;

    public ChannelListResponse() {}

    public ChannelListResponse(long serverId, List<Room> channels) {
        this.serverId = serverId;
        this.channels = channels;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public List<Room> getChannels() { return channels; }
    public void setChannels(List<Room> channels) { this.channels = channels; }
}
