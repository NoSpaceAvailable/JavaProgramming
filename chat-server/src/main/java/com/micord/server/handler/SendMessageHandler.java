package com.micord.server.handler;

import com.micord.common.model.Message;
import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.SendMessageRequest;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.protocol.response.SendMessageResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.RoomRepository;
import com.micord.server.service.MessageService;

public class SendMessageHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        SendMessageRequest req = JsonUtil.fromJson(message.getPayload(), SendMessageRequest.class);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        boolean isGif = "GIF".equals(req.getMessageType());
        if (content.isEmpty() && !isGif) {
            client.sendMessage(JsonUtil.wrap(MessageType.SEND_MESSAGE_RESPONSE,
                    new SendMessageResponse(false, "Message cannot be empty", 0),
                    message.getRequestId()));
            return;
        }
        if (content.length() > 2000) {
            client.sendMessage(JsonUtil.wrap(MessageType.SEND_MESSAGE_RESPONSE,
                    new SendMessageResponse(false, "Message too long (max 2000 chars)", 0),
                    message.getRequestId()));
            return;
        }
        if (!roomRepository.isMember(req.getRoomId(), user.getId())) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Not a member of this room"), message.getRequestId()));
            return;
        }

        String msgType = isGif ? "GIF" : null;
        Message saved = messageService.sendRoomMessage(
                req.getRoomId(), user.getId(), user.getDisplayName(), content, msgType);
        client.sendMessage(JsonUtil.wrap(MessageType.SEND_MESSAGE_RESPONSE,
                new SendMessageResponse(true, "OK", saved.getId()), message.getRequestId()));
    }
}
