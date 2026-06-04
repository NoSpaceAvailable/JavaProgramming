package com.micord.server.handler;

import com.micord.common.protocol.ProtocolMessage;
import com.micord.server.ClientHandler;

public interface RequestHandler {
    void handle(ClientHandler client, ProtocolMessage message);
}
