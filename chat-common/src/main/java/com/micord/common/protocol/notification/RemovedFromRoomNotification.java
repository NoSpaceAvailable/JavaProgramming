package com.micord.common.protocol.notification;

/** Pushed to a user when an owner removes them from a room, so their sidebar updates. */
public class RemovedFromRoomNotification {
    private long roomId;
    private String roomName;
    private String removerName;

    public RemovedFromRoomNotification() {}

    public RemovedFromRoomNotification(long roomId, String roomName, String removerName) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.removerName = removerName;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRemoverName() { return removerName; }
    public void setRemoverName(String removerName) { this.removerName = removerName; }
}
