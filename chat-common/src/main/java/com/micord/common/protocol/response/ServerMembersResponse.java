package com.micord.common.protocol.response;

import com.micord.common.model.User;

import java.util.List;

/** Members of a server with their roles (User.serverRole) and live status. */
public class ServerMembersResponse {
    private long serverId;
    private List<User> members;

    public ServerMembersResponse() {}

    public ServerMembersResponse(long serverId, List<User> members) {
        this.serverId = serverId;
        this.members = members;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { this.members = members; }
}
