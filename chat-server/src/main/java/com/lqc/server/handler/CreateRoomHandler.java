package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.CreateRoomRequest;
import com.lqc.common.protocol.response.CreateRoomResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.RoomService;

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
