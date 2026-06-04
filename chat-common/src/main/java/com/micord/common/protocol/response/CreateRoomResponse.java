package com.micord.common.protocol.response;

import com.micord.common.model.Room;

public class CreateRoomResponse {
    private boolean success;
    private String message;
    private Room room;

    public CreateRoomResponse() {}

    public CreateRoomResponse(boolean success, String message, Room room) {
        this.success = success;
        this.message = message;
        this.room = room;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
}
