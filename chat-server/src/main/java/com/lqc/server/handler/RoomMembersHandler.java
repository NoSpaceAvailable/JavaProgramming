package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.RoomMembersRequest;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.protocol.response.RoomMembersResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.repository.UserRepository;

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
