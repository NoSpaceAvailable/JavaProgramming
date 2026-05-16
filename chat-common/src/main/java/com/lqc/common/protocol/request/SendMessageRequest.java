package com.lqc.common.protocol.request;

public class SendMessageRequest {
    private long roomId;
    private String content;
    private String messageType;

    public SendMessageRequest() {}

    public SendMessageRequest(long roomId, String content) {
        this.roomId = roomId;
        this.content = content;
    }

    public SendMessageRequest(long roomId, String content, String messageType) {
        this.roomId = roomId;
        this.content = content;
        this.messageType = messageType;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
