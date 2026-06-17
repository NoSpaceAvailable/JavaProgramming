package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.ListBansRequest;
import com.micord.common.protocol.response.BanListResponse;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.ServerRepository;
import com.micord.server.service.ServerService;

import java.util.List;

public class ListBansHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();
    private final ServerRepository serverRepository = new ServerRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        ListBansRequest req = JsonUtil.fromJson(message.getPayload(), ListBansRequest.class);
        if (ServerService.rank(serverRepository.getRole(req.getServerId(), user.getId())) < ServerService.rank("ADMIN")) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Only the owner or admins can view bans"), message.getRequestId()));
            return;
        }
        List<User> banned = serverService.listBans(req.getServerId(), user.getId());
        client.sendMessage(JsonUtil.wrap(MessageType.LIST_BANS_RESPONSE,
                new BanListResponse(req.getServerId(), banned), message.getRequestId()));
    }
}
