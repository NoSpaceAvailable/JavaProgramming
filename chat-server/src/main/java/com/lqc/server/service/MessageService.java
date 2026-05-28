package com.lqc.server.service;

import com.lqc.common.model.Message;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.notification.NewMessageNotification;
import com.lqc.common.protocol.response.MessageHistoryResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.MessageRepository;
import com.lqc.server.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository = new MessageRepository();
    private final RoomRepository roomRepository = new RoomRepository();

    public Message sendRoomMessage(long roomId, long senderId, String senderName, String content) {
        return sendRoomMessage(roomId, senderId, senderName, content, null);
    }

    public Message sendRoomMessage(long roomId, long senderId, String senderName, String content, String msgType) {
        Message m = messageRepository.saveRoomMessage(roomId, senderId, senderName, content, msgType);
        broadcastRoomMessage(roomId, m);
        return m;
    }

    public Message sendPrivateMessage(long senderId, String senderName, long recipientId, String content) {
        return sendPrivateMessage(senderId, senderName, recipientId, content, null);
    }

    public Message sendPrivateMessage(long senderId, String senderName, long recipientId, String content, String msgType) {
        Message m = messageRepository.savePrivateMessage(senderId, senderName, recipientId, content, msgType);
        deliverPrivateMessage(m, senderId, recipientId);
        return m;
    }

    public MessageHistoryResponse getRoomHistory(long roomId, long beforeMessageId, int limit) {
        List<Message> msgs = messageRepository.getRoomHistory(roomId, beforeMessageId, limit);
        return new MessageHistoryResponse(roomId, msgs, msgs.size() == limit);
    }

    public MessageHistoryResponse getDmHistory(long currentUserId, long peerUserId, long beforeMessageId, int limit) {
        List<Message> msgs = messageRepository.getDmHistory(currentUserId, peerUserId, beforeMessageId, limit);
        MessageHistoryResponse r = new MessageHistoryResponse(0L, msgs, msgs.size() == limit);
        return r;
    }

    public List<Message> searchRoom(long roomId, String query, int limit) {
        return messageRepository.searchRoomMessages(roomId, query, limit);
    }

    public List<Message> searchDm(long currentUserId, long peerUserId, String query, int limit) {
        return messageRepository.searchDmMessages(currentUserId, peerUserId, query, limit);
    }

    private void broadcastRoomMessage(long roomId, Message m) {
        NewMessageNotification notif = buildNotification(m);
        SessionManager sessions = SessionManager.getInstance();
        for (Long memberId : roomRepository.getMemberIds(roomId)) {
            ClientHandler handler = sessions.getSession(memberId);
            if (handler != null && handler.isConnected()) {
                handler.sendMessage(JsonUtil.wrap(MessageType.NEW_MESSAGE_NOTIFICATION, notif));
            }
        }
    }

    private void deliverPrivateMessage(Message m, long senderId, long recipientId) {
        NewMessageNotification notif = buildNotification(m);
        SessionManager sessions = SessionManager.getInstance();
        ClientHandler recipientHandler = sessions.getSession(recipientId);
        if (recipientHandler != null && recipientHandler.isConnected()) {
            recipientHandler.sendMessage(JsonUtil.wrap(MessageType.NEW_MESSAGE_NOTIFICATION, notif));
        }
        ClientHandler senderHandler = sessions.getSession(senderId);
        if (senderHandler != null && senderHandler.isConnected()) {
            senderHandler.sendMessage(JsonUtil.wrap(MessageType.NEW_MESSAGE_NOTIFICATION, notif));
        }
    }

    private NewMessageNotification buildNotification(Message m) {
        NewMessageNotification n = new NewMessageNotification();
        n.setMessageId(m.getId());
        n.setRoomId(m.getRoomId());
        n.setRecipientId(m.getRecipientId());
        n.setSenderId(m.getSenderId());
        n.setSenderName(m.getSenderName());
        n.setContent(m.getContent());
        n.setMessageType(m.getMessageType() != null ? m.getMessageType().name() : "TEXT");
        n.setTimestamp(m.getCreatedAt() != null
                ? m.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis());
        return n;
    }
}
