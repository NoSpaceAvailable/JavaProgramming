package com.micord.common.protocol.request;

/** Owner-only: change a member's role within a server (ADMIN/MODERATOR/MEMBER). */
public class ChangeRoleRequest {
    private long serverId;
    private long userId;
    private String role;

    public ChangeRoleRequest() {}

    public ChangeRoleRequest(long serverId, long userId, String role) {
        this.serverId = serverId;
        this.userId = userId;
        this.role = role;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
