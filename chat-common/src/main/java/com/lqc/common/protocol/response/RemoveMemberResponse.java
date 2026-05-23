package com.lqc.common.protocol.response;

/** Reply to the owner after attempting to remove a member. */
public class RemoveMemberResponse {
    private boolean success;
    private String message;
    private long roomId;
    private long userId;

    public RemoveMemberResponse() {}

    public RemoveMemberResponse(boolean success, String message, long roomId, long userId) {
        this.success = success;
        this.message = message;
        this.roomId = roomId;
        this.userId = userId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
