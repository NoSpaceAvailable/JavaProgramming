package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.AddedToRoomNotification;
import com.micord.common.protocol.notification.UserJoinedNotification;
import com.micord.common.protocol.request.InviteToRoomRequest;
import com.micord.common.protocol.response.InviteToRoomResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.RoomRepository;
import com.micord.server.service.RoomService;

public class InviteToRoomHandler implements RequestHandler {
    private final RoomService roomService = new RoomService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User inviter = client.getAuthenticatedUser();
        InviteToRoomRequest req = JsonUtil.fromJson(message.getPayload(), InviteToRoomRequest.class);
        RoomService.InviteResult result = roomService.invite(req.getRoomId(), inviter.getId(), req.getUserId());

        InviteToRoomResponse response = new InviteToRoomResponse(
                result.success(), result.message(), req.getRoomId(), req.getUserId(),
                result.target() != null ? result.target().getDisplayName() : null);
        client.sendMessage(JsonUtil.wrap(MessageType.INVITE_TO_ROOM_RESPONSE, response, message.getRequestId()));

        if (!result.success() || result.alreadyMember()) {
            return;
        }

        User target = result.target();
        SessionManager sessions = SessionManager.getInstance();

        // Tell existing members the new user joined so their member panels refresh.
        UserJoinedNotification joined = new UserJoinedNotification(
                req.getRoomId(), target.getId(), target.getDisplayName());
        ProtocolMessage joinedMsg = JsonUtil.wrap(MessageType.USER_JOINED_NOTIFICATION, joined);
        for (Long memberId : roomRepository.getMemberIds(req.getRoomId())) {
            if (memberId == target.getId()) continue;
            ClientHandler h = sessions.getSession(memberId);
            if (h != null && h.isConnected()) h.sendMessage(joinedMsg);
        }

        // Tell the invited user so the room appears in their sidebar.
        ClientHandler targetHandler = sessions.getSession(target.getId());
        if (targetHandler != null && targetHandler.isConnected()) {
            AddedToRoomNotification added = new AddedToRoomNotification(result.room(), inviter.getDisplayName());
            targetHandler.sendMessage(JsonUtil.wrap(MessageType.ADDED_TO_ROOM_NOTIFICATION, added));
        }
    }
}
