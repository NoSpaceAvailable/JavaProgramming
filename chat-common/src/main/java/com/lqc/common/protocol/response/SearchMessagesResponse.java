package com.lqc.common.protocol.response;

import com.lqc.common.model.Message;

import java.util.List;

/** Matching messages for a search, echoing the query and conversation scope. */
public class SearchMessagesResponse {
    private Long roomId;
    private Long peerId;
    private String query;
    private List<Message> results;

    public SearchMessagesResponse() {}

    public SearchMessagesResponse(Long roomId, Long peerId, String query, List<Message> results) {
        this.roomId = roomId;
        this.peerId = peerId;
        this.query = query;
        this.results = results;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getPeerId() { return peerId; }
    public void setPeerId(Long peerId) { this.peerId = peerId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<Message> getResults() { return results; }
    public void setResults(List<Message> results) { this.results = results; }
}
