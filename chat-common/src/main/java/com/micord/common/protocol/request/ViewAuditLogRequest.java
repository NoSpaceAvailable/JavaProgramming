package com.micord.common.protocol.request;

/** Requests recent audit-log entries for a server (Admin+ only). */
public class ViewAuditLogRequest {
    private long serverId;

    public ViewAuditLogRequest() {}

    public ViewAuditLogRequest(long serverId) {
        this.serverId = serverId;
    }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
}
