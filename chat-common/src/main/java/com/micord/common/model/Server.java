package com.micord.common.model;

import java.time.LocalDateTime;

/** A community server (guild) that owns text channels and has members with roles. */
public class Server {
    private long id;
    private String name;
    private long ownerId;
    private String inviteCode;
    private String myRole; // the requesting user's role in this server (OWNER/ADMIN/MODERATOR/MEMBER)
    private LocalDateTime createdAt;

    public Server() {}

    public Server(long id, String name, long ownerId, String inviteCode) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.inviteCode = inviteCode;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getMyRole() { return myRole; }
    public void setMyRole(String myRole) { this.myRole = myRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}
