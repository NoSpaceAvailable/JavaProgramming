package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.GetHistoryRequest;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.protocol.response.MessageHistoryResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.service.MessageService;

public class GetHistoryHandler implements RequestHandler {
    private final MessageService messageService = new MessageService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        GetHistoryRequest req = JsonUtil.fromJson(message.getPayload(), GetHistoryRequest.class);

        int limit = req.getLimit() <= 0 ? 50 : Math.min(req.getLimit(), 200);
        if (!roomRepository.isMember(req.getRoomId(), user.getId())) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Not a member of this room"), message.getRequestId()));
            return;
        }

        MessageHistoryResponse response = messageService.getRoomHistory(
                req.getRoomId(), req.getBeforeMessageId(), limit);
        client.sendMessage(JsonUtil.wrap(MessageType.GET_HISTORY_RESPONSE, response, message.getRequestId()));
    }
}
