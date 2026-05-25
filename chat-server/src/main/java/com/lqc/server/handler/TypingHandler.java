package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.UserTypingNotification;
import com.lqc.common.protocol.request.TypingRequest;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.RoomRepository;

/** Relays a transient "user is typing" signal to the relevant participants. Nothing is persisted. */
public class TypingHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User sender = client.getAuthenticatedUser();
        TypingRequest req = JsonUtil.fromJson(message.getPayload(), TypingRequest.class);
        SessionManager sessions = SessionManager.getInstance();

        if (req.getRoomId() != null) {
            if (!roomRepository.isMember(req.getRoomId(), sender.getId())) return;
            UserTypingNotification notif = new UserTypingNotification(
                    req.getRoomId(), null, sender.getId(), sender.getDisplayName());
            ProtocolMessage msg = JsonUtil.wrap(MessageType.USER_TYPING_NOTIFICATION, notif);
            for (Long memberId : roomRepository.getMemberIds(req.getRoomId())) {
                if (memberId == sender.getId()) continue;
                ClientHandler h = sessions.getSession(memberId);
                if (h != null && h.isConnected()) h.sendMessage(msg);
            }
        } else if (req.getRecipientId() != null) {
            UserTypingNotification notif = new UserTypingNotification(
                    null, req.getRecipientId(), sender.getId(), sender.getDisplayName());
            ClientHandler h = sessions.getSession(req.getRecipientId());
            if (h != null && h.isConnected()) {
                h.sendMessage(JsonUtil.wrap(MessageType.USER_TYPING_NOTIFICATION, notif));
            }
        }
    }
}
