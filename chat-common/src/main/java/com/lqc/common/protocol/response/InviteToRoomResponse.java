package com.lqc.common.protocol.response;

/** Reply to the inviter after attempting to add a user to a room. */
public class InviteToRoomResponse {
    private boolean success;
    private String message;
    private long roomId;
    private long userId;
    private String displayName;

    public InviteToRoomResponse() {}

    public InviteToRoomResponse(boolean success, String message, long roomId, long userId, String displayName) {
        this.success = success;
        this.message = message;
        this.roomId = roomId;
        this.userId = userId;
        this.displayName = displayName;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
