package com.lqc.common.protocol.request;

public class SendMessageRequest {
    private long roomId;
    private String content;

    public SendMessageRequest() {}

    public SendMessageRequest(long roomId, String content) {
        this.roomId = roomId;
        this.content = content;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
