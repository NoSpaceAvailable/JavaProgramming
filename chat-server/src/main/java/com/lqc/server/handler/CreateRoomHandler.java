package com.lqc.server.handler;

import com.lqc.common.model.Room;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.CreateRoomRequest;
import com.lqc.common.protocol.response.RoomActionResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.repository.RoomRepository;

import java.util.List;

public class CreateRoomHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        CreateRoomRequest request = JsonUtil.fromJson(message.getPayload(), CreateRoomRequest.class);

        String name = request.getName() == null ? "" : request.getName().trim();
        String description = request.getDescription() == null ? "" : request.getDescription().trim();

        if (name.isEmpty()) {
            sendResponse(client, message, new RoomActionResponse(false, "Room name is required"));
            return;
        }
        if (name.length() > 100) {
            sendResponse(client, message, new RoomActionResponse(false, "Room name must be 100 characters or fewer"));
            return;
        }
        if (description.length() > 500) {
            sendResponse(client, message, new RoomActionResponse(false, "Description must be 500 characters or fewer"));
            return;
        }

        Room room = roomRepository.create(name, description.isEmpty() ? null : description,
                user.getId(), request.isPrivate());
        List<Room> rooms = roomRepository.findRoomsByUserId(user.getId());
        sendResponse(client, message, new RoomActionResponse(true, "Room created", room, rooms));
    }

    private void sendResponse(ClientHandler client, ProtocolMessage request, RoomActionResponse response) {
        client.sendMessage(JsonUtil.wrap(MessageType.CREATE_ROOM_RESPONSE, response, request.getRequestId()));
    }
}
