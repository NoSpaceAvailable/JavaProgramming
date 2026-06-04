package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.model.UserStatus;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.RoomMembersRequest;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.protocol.response.RoomMembersResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.RoomRepository;
import com.micord.server.repository.UserRepository;

import java.util.List;

public class RoomMembersHandler implements RequestHandler {
    private final RoomRepository roomRepository = new RoomRepository();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        RoomMembersRequest req = JsonUtil.fromJson(message.getPayload(), RoomMembersRequest.class);

        if (!roomRepository.isMember(req.getRoomId(), user.getId())) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Not a member of this room"), message.getRequestId()));
            return;
        }

        List<User> members = userRepository.findByRoomId(req.getRoomId());
        SessionManager sessions = SessionManager.getInstance();
        for (User m : members) {
            UserStatus status = sessions.isOnline(m.getId()) ? sessions.getStatus(m.getId()) : UserStatus.OFFLINE;
            m.setStatus(status);
        }

        RoomMembersResponse response = new RoomMembersResponse(req.getRoomId(), members);
        client.sendMessage(JsonUtil.wrap(MessageType.ROOM_MEMBERS_RESPONSE, response, message.getRequestId()));
    }
}
