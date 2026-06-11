package com.micord.common.protocol.response;

import com.micord.common.model.Room;
import com.micord.common.model.Server;

public class CreateServerResponse {
    private boolean success;
    private String message;
    private Server server;
    private Room defaultChannel;

    public CreateServerResponse() {}

    public CreateServerResponse(boolean success, String message, Server server, Room defaultChannel) {
        this.success = success;
        this.message = message;
        this.server = server;
        this.defaultChannel = defaultChannel;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }

    public Room getDefaultChannel() { return defaultChannel; }
    public void setDefaultChannel(Room defaultChannel) { this.defaultChannel = defaultChannel; }
}
