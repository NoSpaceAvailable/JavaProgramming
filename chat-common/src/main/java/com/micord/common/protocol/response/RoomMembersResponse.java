package com.micord.common.protocol.response;

import com.micord.common.model.User;

import java.util.List;

public class RoomMembersResponse {
    private long roomId;
    private List<User> members;

    public RoomMembersResponse() {}

    public RoomMembersResponse(long roomId, List<User> members) {
        this.roomId = roomId;
        this.members = members;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { this.members = members; }
}
