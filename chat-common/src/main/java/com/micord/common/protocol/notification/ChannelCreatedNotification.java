package com.micord.common.protocol.notification;

import com.micord.common.model.Room;

/** Pushed to a server's online members when a new channel is created. */
public class ChannelCreatedNotification {
    private long serverId;
    private Room channel;

    public ChannelCreatedNotification() {}

    public ChannelCreatedNotification(long serverId, Room channel) {
        this.serverId = serverId;
        this.channel = channel;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public Room getChannel() { return channel; }
    public void setChannel(Room channel) { this.channel = channel; }
}
