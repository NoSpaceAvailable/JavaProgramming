package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.CreateRoomRequest;
import com.micord.common.protocol.response.CreateRoomResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.RoomService;

public class CreateRoomHandler implements RequestHandler {
    private final RoomService roomService = new RoomService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        CreateRoomRequest req = JsonUtil.fromJson(message.getPayload(), CreateRoomRequest.class);
        RoomService.CreateResult result = roomService.create(
                req.getName(), req.getDescription(), user.getId(), req.isPrivate());

        CreateRoomResponse response = new CreateRoomResponse(
                result.success(), result.message(), result.room());
        client.sendMessage(JsonUtil.wrap(MessageType.CREATE_ROOM_RESPONSE, response, message.getRequestId()));
    }
}
