package com.micord.common.protocol.request;

public class DmHistoryRequest {
    private long peerUserId;
    private long beforeMessageId;
    private int limit;

    public DmHistoryRequest() {
        this.limit = 50;
    }

    public DmHistoryRequest(long peerUserId, long beforeMessageId, int limit) {
        this.peerUserId = peerUserId;
        this.beforeMessageId = beforeMessageId;
        this.limit = limit;
    }

    public long getPeerUserId() { return peerUserId; }
    public void setPeerUserId(long peerUserId) { this.peerUserId = peerUserId; }

    public long getBeforeMessageId() { return beforeMessageId; }
    public void setBeforeMessageId(long beforeMessageId) { this.beforeMessageId = beforeMessageId; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
