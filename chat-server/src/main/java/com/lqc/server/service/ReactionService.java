package com.lqc.server.service;

import com.lqc.common.model.Message;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.notification.ReactionNotification;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.MessageRepository;
import com.lqc.server.repository.ReactionRepository;
import com.lqc.server.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ReactionService {
    private static final Logger logger = LoggerFactory.getLogger(ReactionService.class);

    private final ReactionRepository reactionRepository = new ReactionRepository();
    private final MessageRepository messageRepository = new MessageRepository();
    private final RoomRepository roomRepository = new RoomRepository();

    public record Result(boolean success, String message) {}

    public Result add(long userId, String displayName, long messageId, String emoji) {
        return mutate(userId, displayName, messageId, emoji, true);
    }

    public Result remove(long userId, String displayName, long messageId, String emoji) {
        return mutate(userId, displayName, messageId, emoji, false);
    }

    private Result mutate(long userId, String displayName, long messageId, String emoji, boolean add) {
        if (emoji == null || emoji.isBlank()) return new Result(false, "Emoji required");
        if (emoji.length() > 50) return new Result(false, "Emoji too long");

        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isEmpty()) return new Result(false, "Message not found");
        Message m = msgOpt.get();

        if (m.getRoomId() != null && !roomRepository.isMember(m.getRoomId(), userId)) {
            return new Result(false, "Not a member of this room");
        }
        if (m.getRoomId() == null) {
            // DM: only the two participants can react.
            if (m.getSenderId() != userId
                    && (m.getRecipientId() == null || m.getRecipientId() != userId)) {
                return new Result(false, "Not a participant of this DM");
            }
        }

        boolean changed = add
                ? reactionRepository.add(messageId, userId, emoji)
                : reactionRepository.remove(messageId, userId, emoji);
        if (!changed) {
            return new Result(true, add ? "Already reacted" : "No such reaction");
        }

        ReactionNotification n = new ReactionNotification();
        n.setMessageId(messageId);
        n.setRoomId(m.getRoomId());
        n.setRecipientId(m.getRecipientId());
        n.setUserId(userId);
        n.setDisplayName(displayName);
        n.setEmoji(emoji);
        n.setAdded(add);

        var packet = JsonUtil.wrap(MessageType.REACTION_NOTIFICATION, n);
        SessionManager sessions = SessionManager.getInstance();
        if (m.getRoomId() != null) {
            for (Long memberId : roomRepository.getMemberIds(m.getRoomId())) {
                ClientHandler h = sessions.getSession(memberId);
                if (h != null && h.isConnected()) h.sendMessage(packet);
            }
        } else {
            ClientHandler s = sessions.getSession(m.getSenderId());
            if (s != null && s.isConnected()) s.sendMessage(packet);
            if (m.getRecipientId() != null) {
                ClientHandler r = sessions.getSession(m.getRecipientId());
                if (r != null && r.isConnected()) r.sendMessage(packet);
            }
        }
        return new Result(true, "OK");
    }
}
