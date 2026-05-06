package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.EmojiReactionRequest;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.ReactionService;

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
