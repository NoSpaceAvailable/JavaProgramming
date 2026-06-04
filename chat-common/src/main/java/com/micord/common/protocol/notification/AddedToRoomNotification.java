package com.micord.common.protocol.notification;

import com.micord.common.model.Room;

/** Pushed to a user when someone adds them to a room, so their sidebar updates. */
public class AddedToRoomNotification {
    private Room room;
    private String inviterName;

    public AddedToRoomNotification() {}

    public AddedToRoomNotification(Room room, String inviterName) {
        this.room = room;
        this.inviterName = inviterName;
    }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getInviterName() { return inviterName; }
    public void setInviterName(String inviterName) { this.inviterName = inviterName; }
}
