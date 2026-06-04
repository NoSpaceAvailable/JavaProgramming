package com.micord.server;

import com.micord.server.database.DatabaseConfig;
import com.micord.server.database.DatabaseInitializer;
import com.micord.server.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private final int port;
    private final ExecutorService threadPool;
    private volatile boolean running;

    public ChatServer(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        initDatabase();
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Chat server started on port {}", port);
            System.out.println("Chat server started on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New connection from {}", clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            logger.error("Server error", e);
        } finally {
            shutdown();
        }
    }

    private void initDatabase() {
        String dbUrl = ConfigUtil.get("db.url", "jdbc:postgresql://localhost:5432/chat_app");
        String dbUser = ConfigUtil.get("db.username", "postgres");
        String dbPassword = ConfigUtil.get("db.password", "postgres");
        int poolSize = ConfigUtil.getInt("db.pool.size", 10);

        DatabaseConfig.initialize(dbUrl, dbUser, dbPassword, poolSize);
        DatabaseInitializer.initialize();
        logger.info("Database initialized");
    }

    public void shutdown() {
        running = false;
        threadPool.shutdown();
        DatabaseConfig.shutdown();
        logger.info("Server shut down");
    }

    public static void main(String[] args) {
        int port = ConfigUtil.getInt("server.port", 9000);
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new ChatServer(port).start();
    }
}
