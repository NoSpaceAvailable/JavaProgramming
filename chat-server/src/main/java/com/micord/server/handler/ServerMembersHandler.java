package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.model.UserStatus;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.ServerMembersRequest;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.protocol.response.ServerMembersResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.ServerRepository;

import java.util.List;

public class ServerMembersHandler implements RequestHandler {
    private final ServerRepository serverRepository = new ServerRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        ServerMembersRequest req = JsonUtil.fromJson(message.getPayload(), ServerMembersRequest.class);
        if (!serverRepository.isMember(req.getServerId(), user.getId())) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Not a member of this server"), message.getRequestId()));
            return;
        }
        List<User> members = serverRepository.getServerMembers(req.getServerId());
        SessionManager sessions = SessionManager.getInstance();
        for (User m : members) {
            m.setStatus(sessions.isOnline(m.getId()) ? sessions.getStatus(m.getId()) : UserStatus.OFFLINE);
        }
        client.sendMessage(JsonUtil.wrap(MessageType.SERVER_MEMBERS_RESPONSE,
                new ServerMembersResponse(req.getServerId(), members), message.getRequestId()));
    }
}
