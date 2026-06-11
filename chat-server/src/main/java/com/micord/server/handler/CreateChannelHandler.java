package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.ChannelCreatedNotification;
import com.micord.common.protocol.request.CreateChannelRequest;
import com.micord.common.protocol.response.CreateChannelResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.ServerRepository;
import com.micord.server.service.ServerService;

public class CreateChannelHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();
    private final ServerRepository serverRepository = new ServerRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        CreateChannelRequest req = JsonUtil.fromJson(message.getPayload(), CreateChannelRequest.class);
        ServerService.ChannelResult result = serverService.createChannel(req.getServerId(), user.getId(), req.getName());

        CreateChannelResponse response = new CreateChannelResponse(
                result.success(), result.message(), result.channel());
        client.sendMessage(JsonUtil.wrap(MessageType.CREATE_CHANNEL_RESPONSE, response, message.getRequestId()));

        if (!result.success()) return;

        // Push the new channel to every online server member so their channel list updates.
        ChannelCreatedNotification notif = new ChannelCreatedNotification(req.getServerId(), result.channel());
        ProtocolMessage notifMsg = JsonUtil.wrap(MessageType.CHANNEL_CREATED_NOTIFICATION, notif);
        SessionManager sessions = SessionManager.getInstance();
        for (Long memberId : serverRepository.getMemberIds(req.getServerId())) {
            ClientHandler h = sessions.getSession(memberId);
            if (h != null && h.isConnected()) h.sendMessage(notifMsg);
        }
    }
}
