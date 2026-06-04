package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.DeleteMessageRequest;
import com.micord.common.protocol.response.DeleteMessageResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.MessageService;

public class DeleteMessageHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        DeleteMessageRequest req = JsonUtil.fromJson(message.getPayload(), DeleteMessageRequest.class);

        boolean ok = messageService.deleteMessage(req.getMessageId(), user.getId());

        DeleteMessageResponse response = new DeleteMessageResponse(ok,
                ok ? "Deleted" : "Could not delete message", req.getMessageId());
        client.sendMessage(JsonUtil.wrap(MessageType.DELETE_MESSAGE_RESPONSE, response, message.getRequestId()));
    }
}
