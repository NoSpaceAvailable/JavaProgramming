package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.response.ServerListResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.ServerRepository;

public class ListServersHandler implements RequestHandler {
    private final ServerRepository serverRepository = new ServerRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        ServerListResponse response = new ServerListResponse(serverRepository.findServersByUserId(user.getId()));
        client.sendMessage(JsonUtil.wrap(MessageType.LIST_SERVERS_RESPONSE, response, message.getRequestId()));
    }
}
