package com.lqc.server.handler;

import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.server.ClientHandler;

public interface RequestHandler {
    void handle(ClientHandler client, ProtocolMessage message);
}
