package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.UserTypingNotification;
import com.micord.common.protocol.request.TypingRequest;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.RoomRepository;
import com.micord.server.repository.UserRepository;

/** Relays a transient "user is typing" signal to the relevant participants. Nothing is persisted. */
public class TypingHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User sender = client.getAuthenticatedUser();
        TypingRequest req = JsonUtil.fromJson(message.getPayload(), TypingRequest.class);
        Long roomId = req.getRoomId();
        Long recipientId = req.getRecipientId();

        if ((roomId == null || roomId <= 0) == (recipientId == null || recipientId <= 0)) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Typing target is invalid"), message.getRequestId()));
            return;
        }

        UserTypingNotification notif = new UserTypingNotification(
                roomId, recipientId, sender.getId(), sender.getDisplayName(), req.isTyping());
        SessionManager sessions = SessionManager.getInstance();

        if (roomId != null && roomId > 0) {
            if (!roomRepository.isMember(roomId, sender.getId())) {
                client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                        new ErrorResponse("Not a member of this room"), message.getRequestId()));
                return;
            }
            for (Long memberId : roomRepository.getMemberIds(roomId)) {
                if (memberId == sender.getId()) continue;
                ClientHandler handler = sessions.getSession(memberId);
                if (handler != null && handler.isConnected()) {
                    handler.sendMessage(JsonUtil.wrap(MessageType.USER_TYPING_NOTIFICATION, notif));
                }
            }
            return;
        }

        if (recipientId == sender.getId() || userRepository.findById(recipientId).isEmpty()) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Typing recipient is invalid"), message.getRequestId()));
            return;
        }
        ClientHandler recipient = sessions.getSession(recipientId);
        if (recipient != null && recipient.isConnected()) {
            recipient.sendMessage(JsonUtil.wrap(MessageType.USER_TYPING_NOTIFICATION, notif));
        }
    }
}
