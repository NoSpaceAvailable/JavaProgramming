package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.response.RoomListResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.RoomRepository;

public class ListRoomsHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        RoomListResponse response = new RoomListResponse(roomRepository.findRoomsByUserId(user.getId()));
        client.sendMessage(JsonUtil.wrap(MessageType.LIST_ROOMS_RESPONSE, response, message.getRequestId()));
    }
}
