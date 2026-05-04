package com.lqc.client.net;

import com.lqc.common.protocol.ProtocolMessage;

public interface MessageListener {
    void onMessageReceived(ProtocolMessage message);
}
