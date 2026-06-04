package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.UserJoinedNotification;
import com.micord.common.protocol.request.JoinRoomRequest;
import com.micord.common.protocol.response.JoinRoomResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.RoomRepository;
import com.micord.server.service.RoomService;

public class JoinRoomHandler implements RequestHandler {
    private final RoomService roomService = new RoomService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        JoinRoomRequest req = JsonUtil.fromJson(message.getPayload(), JoinRoomRequest.class);
        RoomService.JoinResult result = roomService.join(req.getRoomId(), user.getId());

        JoinRoomResponse response = new JoinRoomResponse(
                result.success(), result.message(), result.room());
        client.sendMessage(JsonUtil.wrap(MessageType.JOIN_ROOM_RESPONSE, response, message.getRequestId()));

        if (result.success() && !result.alreadyMember()) {
            UserJoinedNotification notif = new UserJoinedNotification(
                    req.getRoomId(), user.getId(), user.getDisplayName());
            ProtocolMessage notifMsg = JsonUtil.wrap(MessageType.USER_JOINED_NOTIFICATION, notif);
            for (Long memberId : roomRepository.getMemberIds(req.getRoomId())) {
                if (memberId == user.getId()) continue;
                ClientHandler h = SessionManager.getInstance().getSession(memberId);
                if (h != null && h.isConnected()) h.sendMessage(notifMsg);
            }
        }
    }
}
