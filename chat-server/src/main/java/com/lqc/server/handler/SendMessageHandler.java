package com.lqc.server.handler;

import com.lqc.common.model.Message;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.NewMessageNotification;
import com.lqc.common.protocol.request.SendMessageRequest;
import com.lqc.common.protocol.response.SendMessageResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.MessageRepository;
import com.lqc.server.repository.RoomRepository;

import java.time.ZoneId;

public class SendMessageHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();
    private final MessageRepository messageRepository = new MessageRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage protocolMessage) {
        User user = client.getAuthenticatedUser();
        SendMessageRequest request = JsonUtil.fromJson(protocolMessage.getPayload(), SendMessageRequest.class);
        String content = request.getContent() == null ? "" : request.getContent().trim();

        if (request.getRoomId() <= 0) {
            sendResponse(client, protocolMessage, new SendMessageResponse(false, "Select a room first"));
            return;
        }
        if (content.isEmpty()) {
            sendResponse(client, protocolMessage, new SendMessageResponse(false, "Message cannot be empty"));
            return;
        }
        if (content.length() > 4000) {
            sendResponse(client, protocolMessage, new SendMessageResponse(false, "Message must be 4000 characters or fewer"));
            return;
        }
        if (!roomRepository.isMember(request.getRoomId(), user.getId())) {
            sendResponse(client, protocolMessage, new SendMessageResponse(false, "You are not a member of this room"));
            return;
        }

        Message message = messageRepository.createRoomMessage(
                request.getRoomId(), user.getId(), user.getDisplayName(), content);
        sendResponse(client, protocolMessage, new SendMessageResponse(true, "Message sent", message));

        NewMessageNotification notification = toNotification(message);
        for (Long memberId : roomRepository.getMemberIds(request.getRoomId())) {
            ClientHandler member = SessionManager.getInstance().getSession(memberId);
            if (member != null) {
                member.sendMessage(JsonUtil.wrap(MessageType.NEW_MESSAGE_NOTIFICATION, notification));
            }
        }
    }

    private void sendResponse(ClientHandler client, ProtocolMessage request, SendMessageResponse response) {
        client.sendMessage(JsonUtil.wrap(MessageType.SEND_MESSAGE_RESPONSE, response, request.getRequestId()));
    }

    private NewMessageNotification toNotification(Message message) {
        NewMessageNotification notification = new NewMessageNotification();
        notification.setMessageId(message.getId());
        notification.setRoomId(message.getRoomId());
        notification.setSenderId(message.getSenderId());
        notification.setSenderName(message.getSenderName());
        notification.setContent(message.getContent());
        notification.setMessageType(message.getMessageType().name());
        notification.setTimestamp(message.getCreatedAt() == null
                ? System.currentTimeMillis()
                : message.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return notification;
    }
}
