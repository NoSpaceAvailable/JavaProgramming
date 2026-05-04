package com.lqc.common.protocol;

public class ProtocolMessage {
    private MessageType type;
    private String payload;
    private String requestId;
    private long timestamp;

    public ProtocolMessage() {}

    public ProtocolMessage(MessageType type, String payload, String requestId, long timestamp) {
        this.type = type;
        this.payload = payload;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
