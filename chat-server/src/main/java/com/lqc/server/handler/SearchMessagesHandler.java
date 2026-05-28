package com.lqc.server.handler;

import com.lqc.common.model.Message;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.SearchMessagesRequest;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.protocol.response.SearchMessagesResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.service.MessageService;

import java.util.List;

public class SearchMessagesHandler implements RequestHandler {
    private static final int SEARCH_LIMIT = 50;
    private final MessageService messageService = new MessageService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        SearchMessagesRequest req = JsonUtil.fromJson(message.getPayload(), SearchMessagesRequest.class);

        String query = req.getQuery() == null ? "" : req.getQuery().trim();
        if (query.isEmpty()) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Search query is empty"), message.getRequestId()));
            return;
        }

        List<Message> results;
        if (req.getRoomId() != null) {
            if (!roomRepository.isMember(req.getRoomId(), user.getId())) {
                client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                        new ErrorResponse("Not a member of this room"), message.getRequestId()));
                return;
            }
            results = messageService.searchRoom(req.getRoomId(), query, SEARCH_LIMIT);
        } else if (req.getPeerId() != null) {
            results = messageService.searchDm(user.getId(), req.getPeerId(), query, SEARCH_LIMIT);
        } else {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("No conversation specified"), message.getRequestId()));
            return;
        }

        SearchMessagesResponse response = new SearchMessagesResponse(
                req.getRoomId(), req.getPeerId(), query, results);
        client.sendMessage(JsonUtil.wrap(MessageType.SEARCH_MESSAGES_RESPONSE, response, message.getRequestId()));
    }
}
