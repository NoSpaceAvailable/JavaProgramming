package com.micord.server.manager;

import com.micord.common.model.UserStatus;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.server.ClientHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentHashMap<Long, ClientHandler> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UserStatus> statuses = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() { return INSTANCE; }

    public void addSession(long userId, ClientHandler handler) {
        sessions.put(userId, handler);
        statuses.put(userId, UserStatus.ONLINE);
    }

    public void removeSession(long userId) {
        sessions.remove(userId);
        statuses.put(userId, UserStatus.OFFLINE);
    }

    public ClientHandler getSession(long userId) {
        return sessions.get(userId);
    }

    public boolean isOnline(long userId) {
        return sessions.containsKey(userId);
    }

    public UserStatus getStatus(long userId) {
        return statuses.getOrDefault(userId, UserStatus.OFFLINE);
    }

    public void updateStatus(long userId, UserStatus status) {
        statuses.put(userId, status);
    }

    public Collection<ClientHandler> getAllOnlineSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public void broadcastToAll(ProtocolMessage message, long excludeUserId) {
        for (var entry : sessions.entrySet()) {
            if (entry.getKey() != excludeUserId) {
                entry.getValue().sendMessage(message);
            }
        }
    }
}
