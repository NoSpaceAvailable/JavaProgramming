package com.lqc.server.handler;

import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.response.RoomListResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.repository.RoomRepository;

public class ListRoomsHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        long userId = client.getAuthenticatedUser().getId();
        client.sendMessage(JsonUtil.wrap(
                MessageType.LIST_ROOMS_RESPONSE,
                new RoomListResponse(roomRepository.findPublicRoomsNotJoinedByUserId(userId)),
                message.getRequestId()));
    }
}
