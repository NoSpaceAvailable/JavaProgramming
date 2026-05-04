package com.lqc.server;

import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.StatusChangeNotification;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.handler.RequestDispatcher;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Object writeLock = new Object();
    private User authenticatedUser;
    private volatile boolean connected;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            connected = true;

            while (connected) {
                int length = in.readInt();
                byte[] buffer = new byte[length];
                in.readFully(buffer);
                String json = new String(buffer, StandardCharsets.UTF_8);

                ProtocolMessage message = JsonUtil.fromJson(json, ProtocolMessage.class);
                logger.debug("Received {} from {}", message.getType(),
                        authenticatedUser != null ? authenticatedUser.getUsername() : socket.getRemoteSocketAddress());

                RequestDispatcher.dispatch(this, message);
            }
        } catch (EOFException e) {
            logger.info("Client disconnected: {}",
                    authenticatedUser != null ? authenticatedUser.getUsername() : socket.getRemoteSocketAddress());
        } catch (IOException e) {
            if (connected) {
                logger.error("Client handler error", e);
            }
        } finally {
            disconnect();
        }
    }

    public void sendMessage(ProtocolMessage message) {
        try {
            synchronized (writeLock) {
                byte[] bytes = JsonUtil.toJson(message).getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("Failed to send message to {}",
                    authenticatedUser != null ? authenticatedUser.getUsername() : "unknown", e);
            disconnect();
        }
    }

    public void forceDisconnect() {
        disconnect();
    }

    private void disconnect() {
        if (!connected) return;
        connected = false;

        if (authenticatedUser != null) {
            SessionManager.getInstance().removeSession(authenticatedUser.getId());
            new UserRepository().updateStatus(authenticatedUser.getId(), UserStatus.OFFLINE);

            StatusChangeNotification statusNotif = new StatusChangeNotification(
                    authenticatedUser.getId(), authenticatedUser.getDisplayName(), UserStatus.OFFLINE);
            SessionManager.getInstance().broadcastToAll(
                    JsonUtil.wrap(MessageType.STATUS_CHANGE_NOTIFICATION, statusNotif),
                    authenticatedUser.getId());

            logger.info("User {} disconnected", authenticatedUser.getUsername());
        }

        try {
            socket.close();
        } catch (IOException e) {
            logger.debug("Error closing socket", e);
        }
    }

    public User getAuthenticatedUser() { return authenticatedUser; }
    public void setAuthenticatedUser(User user) { this.authenticatedUser = user; }
    public boolean isConnected() { return connected; }
}
