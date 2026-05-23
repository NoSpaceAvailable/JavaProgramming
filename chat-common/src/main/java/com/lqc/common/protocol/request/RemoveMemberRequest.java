package com.lqc.common.protocol.request;

/** Sent by a room owner to remove (kick) a member from the room. */
public class RemoveMemberRequest {
    private long roomId;
    private long userId;

    public RemoveMemberRequest() {}

    public RemoveMemberRequest(long roomId, long userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
