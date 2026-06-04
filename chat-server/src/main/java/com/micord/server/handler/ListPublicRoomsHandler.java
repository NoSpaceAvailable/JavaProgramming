package com.micord.server.handler;

import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.response.RoomListResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.RoomRepository;

public class ListPublicRoomsHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        RoomListResponse response = new RoomListResponse(roomRepository.findAllPublicRooms());
        client.sendMessage(JsonUtil.wrap(MessageType.LIST_PUBLIC_ROOMS_RESPONSE, response, message.getRequestId()));
    }
}
