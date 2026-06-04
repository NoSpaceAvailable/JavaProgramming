package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.DmHistoryRequest;
import com.micord.common.protocol.response.MessageHistoryResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.MessageService;

public class DmHistoryHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        DmHistoryRequest req = JsonUtil.fromJson(message.getPayload(), DmHistoryRequest.class);

        int limit = req.getLimit() <= 0 ? 50 : Math.min(req.getLimit(), 200);
        MessageHistoryResponse response = messageService.getDmHistory(
                user.getId(), req.getPeerUserId(), req.getBeforeMessageId(), limit);
        // Re-use GET_HISTORY_RESPONSE to avoid a new MessageType.
        client.sendMessage(JsonUtil.wrap(MessageType.GET_HISTORY_RESPONSE, response, message.getRequestId()));
    }
}
