package com.micord.common.protocol.response;

import com.micord.common.model.AuditEntry;

import java.util.List;

public class AuditLogResponse {
    private long serverId;
    private List<AuditEntry> entries;

    public AuditLogResponse() {}

    public AuditLogResponse(long serverId, List<AuditEntry> entries) {
        this.serverId = serverId;
        this.entries = entries;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public List<AuditEntry> getEntries() { return entries; }
    public void setEntries(List<AuditEntry> entries) { this.entries = entries; }
}
