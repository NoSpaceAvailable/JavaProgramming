package com.micord.common.protocol.response;

public class LeaveRoomResponse {
    private boolean success;
    private String message;
    private long roomId;

    public LeaveRoomResponse() {}

    public LeaveRoomResponse(boolean success, String message, long roomId) {
        this.success = success;
        this.message = message;
        this.roomId = roomId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }
}
