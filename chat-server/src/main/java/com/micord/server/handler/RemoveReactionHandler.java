package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.EmojiReactionRequest;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.ReactionService;

public class RemoveReactionHandler implements RequestHandler {
    private final ReactionService reactionService = new ReactionService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        EmojiReactionRequest req = JsonUtil.fromJson(message.getPayload(), EmojiReactionRequest.class);
        ReactionService.Result r = reactionService.remove(
                user.getId(), user.getDisplayName(), req.getMessageId(), req.getEmoji());
        if (!r.success()) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse(r.message()), message.getRequestId()));
        }
    }
}
