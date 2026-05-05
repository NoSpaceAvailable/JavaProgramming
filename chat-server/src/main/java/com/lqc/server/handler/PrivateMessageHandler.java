package com.lqc.server.handler;

import com.lqc.common.model.Message;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.PrivateMessageRequest;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.protocol.response.SendMessageResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.repository.UserRepository;
import com.lqc.server.service.MessageService;

public class PrivateMessageHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        PrivateMessageRequest req = JsonUtil.fromJson(message.getPayload(), PrivateMessageRequest.class);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isEmpty()) {
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

        Message saved = messageService.sendPrivateMessage(
                user.getId(), user.getDisplayName(), req.getRecipientId(), content);
        client.sendMessage(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_RESPONSE,
                new SendMessageResponse(true, "OK", saved.getId()), message.getRequestId()));
    }
}
