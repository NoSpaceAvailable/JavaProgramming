package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.ServerMembersChangedNotification;
import com.micord.common.protocol.request.ChangeRoleRequest;
import com.micord.common.protocol.response.ServerActionResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.ServerRepository;
import com.micord.server.service.ServerService;

public class ChangeRoleHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();
    private final ServerRepository serverRepository = new ServerRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User actor = client.getAuthenticatedUser();
        ChangeRoleRequest req = JsonUtil.fromJson(message.getPayload(), ChangeRoleRequest.class);
        ServerService.ModResult result = serverService.changeRole(
                req.getServerId(), actor.getId(), req.getUserId(), req.getRole());

        client.sendMessage(JsonUtil.wrap(MessageType.SERVER_ACTION_RESPONSE,
                new ServerActionResponse(result.success(), result.message(), req.getServerId()),
                message.getRequestId()));

        if (result.success()) {
            ServerNotifier.broadcastMembersChanged(serverRepository, req.getServerId());
        }
    }
}
