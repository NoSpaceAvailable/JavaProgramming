package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.model.UserStatus;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.response.UserListResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.UserRepository;

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
