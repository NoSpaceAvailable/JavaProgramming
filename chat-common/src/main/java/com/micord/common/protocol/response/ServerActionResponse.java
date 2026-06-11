package com.micord.common.protocol.response;

/** Generic acknowledgement for server moderation actions (change role / kick / ban). */
public class ServerActionResponse {
    private boolean success;
    private String message;
    private long serverId;

    public ServerActionResponse() {}

    public ServerActionResponse(boolean success, String message, long serverId) {
        this.success = success;
        this.message = message;
        this.serverId = serverId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }
}
