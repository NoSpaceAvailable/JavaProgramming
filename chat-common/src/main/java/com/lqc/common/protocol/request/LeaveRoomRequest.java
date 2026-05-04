package com.lqc.common.protocol.request;

public class LeaveRoomRequest {
    private long roomId;

    public LeaveRoomRequest() {}

    public LeaveRoomRequest(long roomId) {
        this.roomId = roomId;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }
}
