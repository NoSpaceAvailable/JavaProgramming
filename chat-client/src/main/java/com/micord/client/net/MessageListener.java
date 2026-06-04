package com.micord.client.net;

import com.micord.common.protocol.ProtocolMessage;

public interface MessageListener {
    void onMessageReceived(ProtocolMessage message);
}
