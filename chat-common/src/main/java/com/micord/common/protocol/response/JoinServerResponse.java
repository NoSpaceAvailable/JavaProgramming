package com.micord.common.protocol.response;

import com.micord.common.model.Room;
import com.micord.common.model.Server;

import java.util.List;

public class JoinServerResponse {
    private boolean success;
    private String message;
    private Server server;
    private List<Room> channels;

    public JoinServerResponse() {}

    public JoinServerResponse(boolean success, String message, Server server, List<Room> channels) {
        this.success = success;
        this.message = message;
        this.server = server;
        this.channels = channels;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }

    public List<Room> getChannels() { return channels; }
    public void setChannels(List<Room> channels) { this.channels = channels; }
}
