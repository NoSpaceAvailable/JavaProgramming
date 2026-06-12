package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.UnbanRequest;
import com.micord.common.protocol.response.ServerActionResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.ServerService;

public class UnbanHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User actor = client.getAuthenticatedUser();
        UnbanRequest req = JsonUtil.fromJson(message.getPayload(), UnbanRequest.class);
        ServerService.ModResult result = serverService.unban(req.getServerId(), actor.getId(), req.getUserId());
        client.sendMessage(JsonUtil.wrap(MessageType.SERVER_ACTION_RESPONSE,
                new ServerActionResponse(result.success(), result.message(), req.getServerId()),
                message.getRequestId()));
    }
}
