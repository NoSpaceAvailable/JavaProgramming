package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.AvatarRequest;
import com.micord.common.protocol.response.AvatarResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class AvatarHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(AvatarHandler.class);
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        AvatarRequest req = JsonUtil.fromJson(message.getPayload(), AvatarRequest.class);
        User target = userRepository.findById(req.getUserId()).orElse(null);

        if (target == null || target.getAvatarUrl() == null || target.getAvatarUrl().isEmpty()) {
            client.sendMessage(JsonUtil.wrap(MessageType.AVATAR_RESPONSE,
                    new AvatarResponse(req.getUserId(), null, null), message.getRequestId()));
            return;
        }

        Path avatarPath = Path.of(target.getAvatarUrl());
        if (!Files.exists(avatarPath)) {
            client.sendMessage(JsonUtil.wrap(MessageType.AVATAR_RESPONSE,
                    new AvatarResponse(req.getUserId(), null, null), message.getRequestId()));
            return;
        }

        try {
            byte[] data = Files.readAllBytes(avatarPath);
            String mime = Files.probeContentType(avatarPath);
            if (mime == null) mime = "image/png";
            client.sendMessage(JsonUtil.wrap(MessageType.AVATAR_RESPONSE,
                    new AvatarResponse(req.getUserId(), Base64.getEncoder().encodeToString(data), mime),
                    message.getRequestId()));
        } catch (IOException e) {
            logger.error("Failed to read avatar for user {}", req.getUserId(), e);
            client.sendMessage(JsonUtil.wrap(MessageType.AVATAR_RESPONSE,
                    new AvatarResponse(req.getUserId(), null, null), message.getRequestId()));
        }
    }
}
