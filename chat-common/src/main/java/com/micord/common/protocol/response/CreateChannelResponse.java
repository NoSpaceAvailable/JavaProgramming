package com.micord.common.protocol.response;

import com.micord.common.model.Room;

public class CreateChannelResponse {
    private boolean success;
    private String message;
    private Room channel;

    public CreateChannelResponse() {}

    public CreateChannelResponse(boolean success, String message, Room channel) {
        this.success = success;
        this.message = message;
        this.channel = channel;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Room getChannel() { return channel; }
    public void setChannel(Room channel) { this.channel = channel; }
}
