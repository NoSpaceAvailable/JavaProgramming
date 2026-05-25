package com.lqc.common.protocol.request;

/**
 * Sent while the user is typing. Exactly one target is set:
 * {@code roomId} for a room, or {@code recipientId} for a direct message.
 */
public class TypingRequest {
    private Long roomId;
    private Long recipientId;

    public TypingRequest() {}

    public TypingRequest(Long roomId, Long recipientId) {
        this.roomId = roomId;
        this.recipientId = recipientId;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
}
