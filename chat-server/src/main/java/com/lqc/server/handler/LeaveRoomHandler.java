package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.UserLeftNotification;
import com.lqc.common.protocol.request.LeaveRoomRequest;
import com.lqc.common.protocol.response.LeaveRoomResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.service.RoomService;

public class LeaveRoomHandler implements RequestHandler {
    private final RoomService roomService = new RoomService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        LeaveRoomRequest req = JsonUtil.fromJson(message.getPayload(), LeaveRoomRequest.class);

        // Capture member list BEFORE removing the user so we can notify them too.
        java.util.List<Long> memberIds = roomRepository.getMemberIds(req.getRoomId());
        RoomService.LeaveResult result = roomService.leave(req.getRoomId(), user.getId());

        LeaveRoomResponse response = new LeaveRoomResponse(
                result.success(), result.message(), req.getRoomId());
        client.sendMessage(JsonUtil.wrap(MessageType.LEAVE_ROOM_RESPONSE, response, message.getRequestId()));

        if (result.success()) {
            UserLeftNotification notif = new UserLeftNotification(
                    req.getRoomId(), user.getId(), user.getDisplayName());
            ProtocolMessage notifMsg = JsonUtil.wrap(MessageType.USER_LEFT_NOTIFICATION, notif);
            for (Long memberId : memberIds) {
                if (memberId == user.getId()) continue;
                ClientHandler h = SessionManager.getInstance().getSession(memberId);
                if (h != null && h.isConnected()) h.sendMessage(notifMsg);
            }
        }
    }
}
