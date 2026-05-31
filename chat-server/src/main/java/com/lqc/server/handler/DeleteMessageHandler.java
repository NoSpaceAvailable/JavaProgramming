package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.DeleteMessageRequest;
import com.lqc.common.protocol.response.DeleteMessageResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.MessageService;

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
