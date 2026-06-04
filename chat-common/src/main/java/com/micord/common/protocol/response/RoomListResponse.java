package com.micord.common.protocol.response;

import com.micord.common.model.Room;

import java.util.List;

public class RoomListResponse {
    private List<Room> rooms;

    public RoomListResponse() {}

    public RoomListResponse(List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
}
