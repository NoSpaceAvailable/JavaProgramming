package com.lqc.common.protocol.request;

/**
 * Searches messages within one conversation. Exactly one target is set:
 * {@code roomId} for a room, or {@code peerId} for a direct message.
 */
public class SearchMessagesRequest {
    private Long roomId;
    private Long peerId;
    private String query;

    public SearchMessagesRequest() {}

    public SearchMessagesRequest(Long roomId, Long peerId, String query) {
        this.roomId = roomId;
        this.peerId = peerId;
        this.query = query;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getPeerId() { return peerId; }
    public void setPeerId(Long peerId) { this.peerId = peerId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
