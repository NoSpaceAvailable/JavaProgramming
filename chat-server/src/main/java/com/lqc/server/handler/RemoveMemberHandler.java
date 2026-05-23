package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.RemovedFromRoomNotification;
import com.lqc.common.protocol.notification.UserLeftNotification;
import com.lqc.common.protocol.request.RemoveMemberRequest;
import com.lqc.common.protocol.response.RemoveMemberResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.service.RoomService;

public class RemoveMemberHandler implements RequestHandler {
    private final RoomService roomService = new RoomService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User requester = client.getAuthenticatedUser();
        RemoveMemberRequest req = JsonUtil.fromJson(message.getPayload(), RemoveMemberRequest.class);
        RoomService.RemoveResult result = roomService.removeMember(req.getRoomId(), requester.getId(), req.getUserId());

        RemoveMemberResponse response = new RemoveMemberResponse(
                result.success(), result.message(), req.getRoomId(), req.getUserId());
        client.sendMessage(JsonUtil.wrap(MessageType.REMOVE_MEMBER_RESPONSE, response, message.getRequestId()));

        if (!result.success()) {
            return;
        }

        User target = result.target();
        SessionManager sessions = SessionManager.getInstance();

        // Tell remaining members so their member panels refresh.
        UserLeftNotification left = new UserLeftNotification(
                req.getRoomId(), target.getId(), target.getDisplayName());
        ProtocolMessage leftMsg = JsonUtil.wrap(MessageType.USER_LEFT_NOTIFICATION, left);
        for (Long memberId : roomRepository.getMemberIds(req.getRoomId())) {
            ClientHandler h = sessions.getSession(memberId);
            if (h != null && h.isConnected()) h.sendMessage(leftMsg);
        }

        // Tell the removed user so the room disappears from their sidebar.
        ClientHandler targetHandler = sessions.getSession(target.getId());
        if (targetHandler != null && targetHandler.isConnected()) {
            RemovedFromRoomNotification removed = new RemovedFromRoomNotification(
                    req.getRoomId(), result.room().getName(), requester.getDisplayName());
            targetHandler.sendMessage(JsonUtil.wrap(MessageType.REMOVED_FROM_ROOM_NOTIFICATION, removed));
        }
    }
}
