package com.micord.common.protocol.request;

public class JoinRoomRequest {
    private long roomId;

    public JoinRoomRequest() {}

    public JoinRoomRequest(long roomId) {
        this.roomId = roomId;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }
}
