package com.lqc.server.repository;

import com.lqc.common.model.Message;
import com.lqc.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public Message saveRoomMessage(long roomId, long senderId, String senderName, String content) {
        String sql = "INSERT INTO messages (room_id, sender_id, content, message_type) " +
                "VALUES (?, ?, ?, 'TEXT') RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, senderId);
            stmt.setString(3, content);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Message m = new Message(senderId, senderName, content);
                m.setId(rs.getLong("id"));
                m.setRoomId(roomId);
                Timestamp t = rs.getTimestamp("created_at");
                if (t != null) m.setCreatedAt(t.toLocalDateTime());
                return m;
            }
            throw new SQLException("Failed to insert room message");
        } catch (SQLException e) {
            logger.error("Error saving room message (room={}, sender={})", roomId, senderId, e);
            throw new RuntimeException("Failed to save room message", e);
        }
    }

    public Message savePrivateMessage(long senderId, String senderName, long recipientId, String content) {
        String sql = "INSERT INTO messages (sender_id, recipient_id, content, message_type) " +
                "VALUES (?, ?, ?, 'TEXT') RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, recipientId);
            stmt.setString(3, content);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Message m = new Message(senderId, senderName, content);
                m.setId(rs.getLong("id"));
                m.setRecipientId(recipientId);
                Timestamp t = rs.getTimestamp("created_at");
                if (t != null) m.setCreatedAt(t.toLocalDateTime());
                return m;
            }
            throw new SQLException("Failed to insert DM");
        } catch (SQLException e) {
            logger.error("Error saving DM (sender={}, recipient={})", senderId, recipientId, e);
            throw new RuntimeException("Failed to save DM", e);
        }
    }

    public List<Message> getRoomHistory(long roomId, long beforeMessageId, int limit) {
        String sql = "SELECT m.id, m.room_id, m.sender_id, m.recipient_id, m.content, m.message_type, " +
                "m.created_at, u.display_name AS sender_name " +
                "FROM messages m INNER JOIN users u ON m.sender_id = u.id " +
                "WHERE m.room_id = ? AND (? = 0 OR m.id < ?) " +
                "ORDER BY m.id DESC LIMIT ?";
        return queryMessages(sql, stmt -> {
            stmt.setLong(1, roomId);
            stmt.setLong(2, beforeMessageId);
            stmt.setLong(3, beforeMessageId);
            stmt.setInt(4, limit);
        });
    }

    public List<Message> getDmHistory(long userA, long userB, long beforeMessageId, int limit) {
        String sql = "SELECT m.id, m.room_id, m.sender_id, m.recipient_id, m.content, m.message_type, " +
                "m.created_at, u.display_name AS sender_name " +
                "FROM messages m INNER JOIN users u ON m.sender_id = u.id " +
                "WHERE m.room_id IS NULL AND " +
                "((m.sender_id = ? AND m.recipient_id = ?) OR (m.sender_id = ? AND m.recipient_id = ?)) " +
                "AND (? = 0 OR m.id < ?) " +
                "ORDER BY m.id DESC LIMIT ?";
        return queryMessages(sql, stmt -> {
            stmt.setLong(1, userA);
            stmt.setLong(2, userB);
            stmt.setLong(3, userB);
            stmt.setLong(4, userA);
            stmt.setLong(5, beforeMessageId);
            stmt.setLong(6, beforeMessageId);
            stmt.setInt(7, limit);
        });
    }

    private interface StmtBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    private List<Message> queryMessages(String sql, StmtBinder binder) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            binder.bind(stmt);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
            Collections.reverse(messages);
        } catch (SQLException e) {
            logger.error("Error querying messages", e);
        }
        return messages;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        long roomId = rs.getLong("room_id");
        if (!rs.wasNull()) m.setRoomId(roomId);
        m.setSenderId(rs.getLong("sender_id"));
        m.setSenderName(rs.getString("sender_name"));
        long recipientId = rs.getLong("recipient_id");
        if (!rs.wasNull()) m.setRecipientId(recipientId);
        m.setContent(rs.getString("content"));
        String type = rs.getString("message_type");
        if (type != null) {
            try {
                m.setMessageType(Message.MessageType.valueOf(type));
            } catch (IllegalArgumentException ignored) {
                m.setMessageType(Message.MessageType.TEXT);
            }
        }
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) m.setCreatedAt(t.toLocalDateTime());
        return m;
    }
}
