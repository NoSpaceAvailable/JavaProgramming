package com.lqc.server.handler;

import com.lqc.common.model.Room;
import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.StatusChangeNotification;
import com.lqc.common.protocol.request.LoginRequest;
import com.lqc.common.protocol.response.LoginResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.MessageRepository;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.repository.UserRepository;
import com.lqc.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LoginHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private final AuthService authService = new AuthService();
    private final UserRepository userRepository = new UserRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final MessageRepository messageRepository = new MessageRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        LoginRequest request = JsonUtil.fromJson(message.getPayload(), LoginRequest.class);
        AuthService.LoginResult result = authService.login(request.getUsername(), request.getPassword());

        if (!result.success()) {
            client.sendMessage(JsonUtil.wrap(MessageType.LOGIN_RESPONSE,
                    new LoginResponse(false, result.message()), message.getRequestId()));
            return;
        }

        User user = result.user();

        // Disconnect existing session if user is already logged in
        ClientHandler existing = SessionManager.getInstance().getSession(user.getId());
        if (existing != null && existing.isConnected()) {
            existing.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new com.lqc.common.protocol.response.ErrorResponse("Logged in from another location")));
            existing.forceDisconnect();
        }

        client.setAuthenticatedUser(user);
        SessionManager.getInstance().addSession(user.getId(), client);
        userRepository.updateStatus(user.getId(), UserStatus.ONLINE);

        List<Room> rooms = roomRepository.findRoomsByUserId(user.getId());

        List<long[]> peerIdPairs = messageRepository.getRecentDmPeerIds(user.getId());
        List<Long> peerIds = peerIdPairs.stream().map(a -> a[0]).toList();
        Map<Long, User> peerMap = userRepository.findByIds(peerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<User> orderedPeers = new ArrayList<>();
        for (Long pid : peerIds) {
            User p = peerMap.get(pid);
            if (p != null) orderedPeers.add(p);
        }

        LoginResponse response = new LoginResponse(true, user.getId(), user.getDisplayName(), rooms);
        response.setRecentDmPeers(orderedPeers);
        client.sendMessage(JsonUtil.wrap(MessageType.LOGIN_RESPONSE, response, message.getRequestId()));

        // Broadcast online status to other users
        StatusChangeNotification statusNotif = new StatusChangeNotification(
                user.getId(), user.getDisplayName(), UserStatus.ONLINE);
        SessionManager.getInstance().broadcastToAll(
                JsonUtil.wrap(MessageType.STATUS_CHANGE_NOTIFICATION, statusNotif), user.getId());

        logger.info("User {} logged in successfully", user.getUsername());
    }
}
