package com.lqc.server.handler;

import com.lqc.common.model.Message;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.GetHistoryRequest;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.protocol.response.MessageHistoryResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.repository.MessageRepository;
import com.lqc.server.repository.RoomRepository;

import java.util.List;

public class GetHistoryHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();
    private final MessageRepository messageRepository = new MessageRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        GetHistoryRequest request = JsonUtil.fromJson(message.getPayload(), GetHistoryRequest.class);
        long userId = client.getAuthenticatedUser().getId();

        if (!roomRepository.isMember(request.getRoomId(), userId)) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("You are not a member of this room"), message.getRequestId()));
            return;
        }

        int limit = request.getLimit() <= 0 ? 50 : Math.min(request.getLimit(), 100);
        List<Message> messages = messageRepository.findRoomMessages(
                request.getRoomId(), request.getBeforeMessageId(), limit + 1);
        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = messages.subList(1, messages.size());
        }

        client.sendMessage(JsonUtil.wrap(MessageType.GET_HISTORY_RESPONSE,
                new MessageHistoryResponse(request.getRoomId(), messages, hasMore),
                message.getRequestId()));
    }
}
