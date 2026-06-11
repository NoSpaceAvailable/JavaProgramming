package com.micord.server.handler;

import com.micord.common.model.Room;
import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.ListChannelsRequest;
import com.micord.common.protocol.response.ChannelListResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.ServerService;

import java.util.List;

public class ListChannelsHandler implements RequestHandler {
    private final ServerService serverService = new ServerService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        ListChannelsRequest req = JsonUtil.fromJson(message.getPayload(), ListChannelsRequest.class);
        List<Room> channels = serverService.listChannels(req.getServerId(), user.getId());
        ChannelListResponse response = new ChannelListResponse(req.getServerId(), channels);
        client.sendMessage(JsonUtil.wrap(MessageType.LIST_CHANNELS_RESPONSE, response, message.getRequestId()));
    }
}
