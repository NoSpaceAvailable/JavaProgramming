package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.CreateServerRequest;
import com.micord.common.protocol.response.CreateServerResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.ServerService;

public class CreateServerHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        CreateServerRequest req = JsonUtil.fromJson(message.getPayload(), CreateServerRequest.class);
        ServerService.CreateResult result = serverService.createServer(req.getName(), user.getId());

        CreateServerResponse response = new CreateServerResponse(
                result.success(), result.message(), result.server(), result.defaultChannel());
        client.sendMessage(JsonUtil.wrap(MessageType.CREATE_SERVER_RESPONSE, response, message.getRequestId()));
    }
}
