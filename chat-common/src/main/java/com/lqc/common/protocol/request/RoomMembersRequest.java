package com.lqc.common.protocol.request;

public class RoomMembersRequest {
    private long roomId;

    public RoomMembersRequest() {}

    public RoomMembersRequest(long roomId) {
        this.roomId = roomId;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }
}
