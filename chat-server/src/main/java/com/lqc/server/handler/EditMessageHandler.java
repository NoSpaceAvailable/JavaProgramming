package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.EditMessageRequest;
import com.lqc.common.protocol.response.EditMessageResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.MessageService;

public class EditMessageHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        EditMessageRequest req = JsonUtil.fromJson(message.getPayload(), EditMessageRequest.class);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        boolean ok = !content.isEmpty()
                && messageService.editMessage(req.getMessageId(), user.getId(), content);

        EditMessageResponse response = new EditMessageResponse(ok,
                ok ? "Edited" : "Could not edit message", req.getMessageId());
        client.sendMessage(JsonUtil.wrap(MessageType.EDIT_MESSAGE_RESPONSE, response, message.getRequestId()));
    }
}
