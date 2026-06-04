package com.micord.server.handler;

import com.micord.common.model.Message;
import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.PrivateMessageRequest;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.protocol.response.SendMessageResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.UserRepository;
import com.micord.server.service.MessageService;

public class PrivateMessageHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        PrivateMessageRequest req = JsonUtil.fromJson(message.getPayload(), PrivateMessageRequest.class);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        boolean isGif = "GIF".equals(req.getMessageType());
        if (content.isEmpty() && !isGif) {
            client.sendMessage(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_RESPONSE,
                    new SendMessageResponse(false, "Message cannot be empty", 0),
                    message.getRequestId()));
            return;
        }
        if (content.length() > 2000) {
            client.sendMessage(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_RESPONSE,
                    new SendMessageResponse(false, "Message too long (max 2000 chars)", 0),
                    message.getRequestId()));
            return;
        }
        if (req.getRecipientId() == user.getId()) {
            client.sendMessage(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_RESPONSE,
                    new SendMessageResponse(false, "Cannot DM yourself", 0),
                    message.getRequestId()));
            return;
        }
        if (userRepository.findById(req.getRecipientId()).isEmpty()) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Recipient not found"), message.getRequestId()));
            return;
        }

        String msgType = isGif ? "GIF" : null;
        Message saved = messageService.sendPrivateMessage(
                user.getId(), user.getDisplayName(), req.getRecipientId(), content, msgType);
        client.sendMessage(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_RESPONSE,
                new SendMessageResponse(true, "OK", saved.getId()), message.getRequestId()));
    }
}
