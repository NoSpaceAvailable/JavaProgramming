package com.lqc.server.handler;

import com.lqc.common.model.Room;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.UserLeftNotification;
import com.lqc.common.protocol.request.LeaveRoomRequest;
import com.lqc.common.protocol.response.RoomActionResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

public class LeaveRoomHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        LeaveRoomRequest request = JsonUtil.fromJson(message.getPayload(), LeaveRoomRequest.class);
        Optional<Room> room = roomRepository.findById(request.getRoomId());

        if (room.isEmpty()) {
            sendResponse(client, message, new RoomActionResponse(false, "Room not found"));
            return;
        }
        if (!roomRepository.isMember(request.getRoomId(), user.getId())) {
            sendResponse(client, message, new RoomActionResponse(false, "You are not in this room"));
            return;
        }
        if (room.get().getOwnerId() == user.getId()) {
            sendResponse(client, message, new RoomActionResponse(false, "Room owners cannot leave their own room"));
            return;
        }

        List<Long> memberIds = roomRepository.getMemberIds(request.getRoomId());
        boolean left = roomRepository.removeMember(request.getRoomId(), user.getId());
        if (!left) {
            sendResponse(client, message, new RoomActionResponse(false, "Could not leave room"));
            return;
        }

        UserLeftNotification notification = new UserLeftNotification(
                request.getRoomId(), user.getId(), user.getDisplayName());
        for (Long memberId : memberIds) {
            ClientHandler member = SessionManager.getInstance().getSession(memberId);
            if (member != null && memberId != user.getId()) {
                member.sendMessage(JsonUtil.wrap(MessageType.USER_LEFT_NOTIFICATION, notification));
            }
        }

        List<Room> rooms = roomRepository.findRoomsByUserId(user.getId());
        sendResponse(client, message, new RoomActionResponse(true, "Left room", room.get(), rooms));
    }

    private void sendResponse(ClientHandler client, ProtocolMessage request, RoomActionResponse response) {
        client.sendMessage(JsonUtil.wrap(MessageType.LEAVE_ROOM_RESPONSE, response, request.getRequestId()));
    }
}
