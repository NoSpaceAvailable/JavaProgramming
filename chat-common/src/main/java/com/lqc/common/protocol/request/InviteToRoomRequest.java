package com.lqc.common.protocol.request;

/** Sent by a room member to add another user (by id) into the room. */
public class InviteToRoomRequest {
    private long roomId;
    private long userId;

    public InviteToRoomRequest() {}

    public InviteToRoomRequest(long roomId, long userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
