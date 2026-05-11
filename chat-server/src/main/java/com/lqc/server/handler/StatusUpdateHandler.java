package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.StatusChangeNotification;
import com.lqc.common.protocol.request.StatusUpdateRequest;
import com.lqc.common.protocol.response.StatusUpdateResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.UserRepository;

public class StatusUpdateHandler implements RequestHandler {
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        StatusUpdateRequest req = JsonUtil.fromJson(message.getPayload(), StatusUpdateRequest.class);
        UserStatus desired = req.getStatus();
        if (desired == null || desired == UserStatus.OFFLINE) {
            client.sendMessage(JsonUtil.wrap(MessageType.STATUS_UPDATE_RESPONSE,
                    new StatusUpdateResponse(false, "Invalid status", null),
                    message.getRequestId()));
            return;
        }
        userRepository.updateStatus(user.getId(), desired);
        SessionManager.getInstance().updateStatus(user.getId(), desired);

        client.sendMessage(JsonUtil.wrap(MessageType.STATUS_UPDATE_RESPONSE,
                new StatusUpdateResponse(true, "OK", desired), message.getRequestId()));

        StatusChangeNotification notif = new StatusChangeNotification(
                user.getId(), user.getDisplayName(), desired);
        SessionManager.getInstance().broadcastToAll(
                JsonUtil.wrap(MessageType.STATUS_CHANGE_NOTIFICATION, notif), user.getId());
    }
}
