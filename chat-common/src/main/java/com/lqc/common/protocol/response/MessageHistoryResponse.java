package com.lqc.common.protocol.response;

import com.lqc.common.model.Message;

import java.util.List;

public class MessageHistoryResponse {
    private long roomId;
    private List<Message> messages;
    private boolean hasMore;

    public MessageHistoryResponse() {}

    public MessageHistoryResponse(long roomId, List<Message> messages, boolean hasMore) {
        this.roomId = roomId;
        this.messages = messages;
        this.hasMore = hasMore;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
