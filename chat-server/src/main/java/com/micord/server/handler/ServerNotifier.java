package com.micord.server.handler;

import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.ServerMembersChangedNotification;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.ServerRepository;

/** Shared push helpers for server-scoped notifications. */
final class ServerNotifier {
    private ServerNotifier() {}

    /** Tells every online member of a server to refresh its member list. */
    static void broadcastMembersChanged(ServerRepository serverRepository, long serverId) {
        ProtocolMessage msg = JsonUtil.wrap(MessageType.SERVER_MEMBERS_CHANGED_NOTIFICATION,
                new ServerMembersChangedNotification(serverId));
        SessionManager sessions = SessionManager.getInstance();
        for (Long memberId : serverRepository.getMemberIds(serverId)) {
            ClientHandler h = sessions.getSession(memberId);
            if (h != null && h.isConnected()) h.sendMessage(msg);
        }
    }
}
