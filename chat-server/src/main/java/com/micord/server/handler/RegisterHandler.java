package com.micord.server.handler;

import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.RegisterRequest;
import com.micord.common.protocol.response.RegisterResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RegisterHandler.class);
    private final AuthService authService = new AuthService();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        RegisterRequest request = JsonUtil.fromJson(message.getPayload(), RegisterRequest.class);
        AuthService.RegisterResult result = authService.register(
                request.getUsername(), request.getPassword(), request.getDisplayName());

        RegisterResponse response = new RegisterResponse(result.success(), result.message());
        client.sendMessage(JsonUtil.wrap(MessageType.REGISTER_RESPONSE, response, message.getRequestId()));

        if (result.success()) {
            logger.info("User {} registered successfully", request.getUsername());
        }
    }
}
