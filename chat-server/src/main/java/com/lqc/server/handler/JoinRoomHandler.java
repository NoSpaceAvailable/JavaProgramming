package com.lqc.server.handler;

import com.lqc.common.model.Room;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.UserJoinedNotification;
import com.lqc.common.protocol.request.JoinRoomRequest;
import com.lqc.common.protocol.response.RoomActionResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

public class JoinRoomHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        JoinRoomRequest request = JsonUtil.fromJson(message.getPayload(), JoinRoomRequest.class);
        Optional<Room> room = roomRepository.findById(request.getRoomId());

        if (room.isEmpty()) {
            sendResponse(client, message, new RoomActionResponse(false, "Room not found"));
            return;
        }
        if (room.get().isPrivate()) {
            sendResponse(client, message, new RoomActionResponse(false, "Cannot join private rooms directly"));
            return;
        }
        if (roomRepository.isMember(request.getRoomId(), user.getId())) {
            sendResponse(client, message, new RoomActionResponse(false, "You are already in this room"));
            return;
        }

        boolean joined = roomRepository.addMember(request.getRoomId(), user.getId(), "MEMBER");
        if (!joined) {
            sendResponse(client, message, new RoomActionResponse(false, "Could not join room"));
            return;
        }

        UserJoinedNotification notification = new UserJoinedNotification(
                request.getRoomId(), user.getId(), user.getDisplayName());
        for (Long memberId : roomRepository.getMemberIds(request.getRoomId())) {
            ClientHandler member = SessionManager.getInstance().getSession(memberId);
            if (member != null && memberId != user.getId()) {
                member.sendMessage(JsonUtil.wrap(MessageType.USER_JOINED_NOTIFICATION, notification));
            }
        }

        List<Room> rooms = roomRepository.findRoomsByUserId(user.getId());
        sendResponse(client, message, new RoomActionResponse(true, "Joined room", room.get(), rooms));
    }

    private void sendResponse(ClientHandler client, ProtocolMessage request, RoomActionResponse response) {
        client.sendMessage(JsonUtil.wrap(MessageType.JOIN_ROOM_RESPONSE, response, request.getRequestId()));
    }
}
