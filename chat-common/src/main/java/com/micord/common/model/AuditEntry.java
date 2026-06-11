package com.micord.common.model;

import java.time.LocalDateTime;

/** A single recorded moderation/administration action within a server. */
public class AuditEntry {
    private long id;
    private long serverId;
    private String actorName;
    private String action;
    private String detail;
    private LocalDateTime createdAt;

    public AuditEntry() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
