package com.lqc.client.net;

import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.util.JsonUtil;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerConnection {
    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);
    private static ServerConnection instance;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Object writeLock = new Object();
    private Thread listenerThread;
    private volatile boolean connected;
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> disconnectListeners = new CopyOnWriteArrayList<>();

    private ServerConnection() {}

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        connected = true;
        startListening();
        logger.info("Connected to server at {}:{}", host, port);
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    int length = in.readInt();
                    byte[] buffer = new byte[length];
                    in.readFully(buffer);
                    String json = new String(buffer, StandardCharsets.UTF_8);
                    ProtocolMessage message = JsonUtil.fromJson(json, ProtocolMessage.class);
                    logger.debug("Received: {}", message.getType());
                    notifyListeners(message);
                }
            } catch (EOFException e) {
                logger.info("Server disconnected");
            } catch (IOException e) {
                if (connected) logger.error("Read error", e);
            } finally {
                connected = false;
                notifyDisconnect();
            }
        }, "ServerListener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void notifyDisconnect() {
        Platform.runLater(() -> {
            for (Runnable r : disconnectListeners) {
                try { r.run(); } catch (Exception e) { logger.warn("disconnect listener threw", e); }
            }
        });
    }

    public void addDisconnectListener(Runnable r) { disconnectListeners.add(r); }
    public void removeDisconnectListener(Runnable r) { disconnectListeners.remove(r); }

    public void send(ProtocolMessage message) {
        try {
            synchronized (writeLock) {
                byte[] bytes = JsonUtil.toJson(message).getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();
            }
        } catch (IOException e) {
            logger.error("Failed to send message", e);
            connected = false;
        }
    }

    private void notifyListeners(ProtocolMessage message) {
        Platform.runLater(() -> {
            for (MessageListener listener : listeners) {
                listener.onMessageReceived(message);
            }
        });
    }

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.debug("Error closing socket", e);
        }
    }

    public boolean isConnected() { return connected; }
}
