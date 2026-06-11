package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.JoinServerRequest;
import com.micord.common.protocol.response.JoinServerResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.ServerService;

public class JoinServerHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        JoinServerRequest req = JsonUtil.fromJson(message.getPayload(), JoinServerRequest.class);
        ServerService.JoinResult result = serverService.joinByInvite(req.getInviteCode(), user.getId());

        JoinServerResponse response = new JoinServerResponse(
                result.success(), result.message(), result.server(), result.channels());
        client.sendMessage(JsonUtil.wrap(MessageType.JOIN_SERVER_RESPONSE, response, message.getRequestId()));
    }
}
