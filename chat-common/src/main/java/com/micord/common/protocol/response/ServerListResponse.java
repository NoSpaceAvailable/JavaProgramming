package com.micord.common.protocol.response;

import com.micord.common.model.Server;

import java.util.List;

public class ServerListResponse {
    private List<Server> servers;

    public ServerListResponse() {}

    public ServerListResponse(List<Server> servers) {
        this.servers = servers;
    }

    public List<Server> getServers() { return servers; }
    public void setServers(List<Server> servers) { this.servers = servers; }
}
