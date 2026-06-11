package com.micord.server.handler;

import com.micord.common.model.Server;
import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.KickedFromServerNotification;
import com.micord.common.protocol.request.BanFromServerRequest;
import com.micord.common.protocol.response.ServerActionResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.ServerRepository;
import com.micord.server.service.ServerService;

public class BanFromServerHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();
    private final ServerRepository serverRepository = new ServerRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User actor = client.getAuthenticatedUser();
        BanFromServerRequest req = JsonUtil.fromJson(message.getPayload(), BanFromServerRequest.class);
        ServerService.ModResult result = serverService.ban(
                req.getServerId(), actor.getId(), req.getUserId(), req.getReason());

        client.sendMessage(JsonUtil.wrap(MessageType.SERVER_ACTION_RESPONSE,
                new ServerActionResponse(result.success(), result.message(), req.getServerId()),
                message.getRequestId()));

        if (!result.success()) return;

        String serverName = serverRepository.findById(req.getServerId()).map(Server::getName).orElse("the server");
        SessionManager sessions = SessionManager.getInstance();
        ClientHandler targetHandler = sessions.getSession(req.getUserId());
        if (targetHandler != null && targetHandler.isConnected()) {
            targetHandler.sendMessage(JsonUtil.wrap(MessageType.KICKED_FROM_SERVER_NOTIFICATION,
                    new KickedFromServerNotification(req.getServerId(), serverName, req.getReason(), true)));
        }
        ServerNotifier.broadcastMembersChanged(serverRepository, req.getServerId());
    }
}
