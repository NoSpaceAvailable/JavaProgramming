package com.micord.common.protocol.response;

import com.micord.common.model.User;

import java.util.List;

/** Users currently banned from a server. */
public class BanListResponse {
    private long serverId;
    private List<User> bannedUsers;

    public BanListResponse() {}

    public BanListResponse(long serverId, List<User> bannedUsers) {
        this.serverId = serverId;
        this.bannedUsers = bannedUsers;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public List<User> getBannedUsers() { return bannedUsers; }
    public void setBannedUsers(List<User> bannedUsers) { this.bannedUsers = bannedUsers; }
}
