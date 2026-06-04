package com.micord.common.protocol.request;

public class GetHistoryRequest {
    private long roomId;
    private long beforeMessageId;
    private int limit;

    public GetHistoryRequest() {
        this.limit = 50;
    }

    public GetHistoryRequest(long roomId, long beforeMessageId, int limit) {
        this.roomId = roomId;
        this.beforeMessageId = beforeMessageId;
        this.limit = limit;
    }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getBeforeMessageId() { return beforeMessageId; }
    public void setBeforeMessageId(long beforeMessageId) { this.beforeMessageId = beforeMessageId; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
