package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.response.UserListResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.UserRepository;

import java.util.List;

public class ListUsersHandler implements RequestHandler {
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User self = client.getAuthenticatedUser();
        List<User> users = userRepository.findAllExcept(self.getId());
        SessionManager sessions = SessionManager.getInstance();
        for (User u : users) {
            UserStatus status = sessions.isOnline(u.getId()) ? sessions.getStatus(u.getId()) : UserStatus.OFFLINE;
            u.setStatus(status);
        }
        client.sendMessage(JsonUtil.wrap(MessageType.LIST_USERS_RESPONSE,
                new UserListResponse(users), message.getRequestId()));
    }
}
