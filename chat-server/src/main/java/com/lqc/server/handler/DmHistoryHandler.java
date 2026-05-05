package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.DmHistoryRequest;
import com.lqc.common.protocol.response.MessageHistoryResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.MessageService;

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
