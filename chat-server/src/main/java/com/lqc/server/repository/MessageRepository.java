package com.lqc.server.repository;

import com.lqc.common.model.Message;
import com.lqc.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    public Message createRoomMessage(long roomId, long senderId, String senderName, String content) {
        String sql = "INSERT INTO messages (room_id, sender_id, content, message_type) " +
                "VALUES (?, ?, ?, 'TEXT')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(conn, keys.getLong(1), senderName);
                }
            }
            throw new SQLException("Failed to get generated key for message");
        } catch (SQLException e) {
            logger.error("Error creating message in room {}", roomId, e);
            throw new RuntimeException("Failed to create message", e);
        }
    }

    public List<Message> findRoomMessages(long roomId, long beforeMessageId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = beforeMessageId > 0
                ? "SELECT m.id, m.room_id, m.sender_id, u.display_name AS sender_name, m.recipient_id, " +
                "m.content, m.message_type, m.created_at FROM messages m " +
                "JOIN users u ON u.id = m.sender_id WHERE m.room_id = ? AND m.id < ? " +
                "ORDER BY m.id DESC LIMIT ?"
                : "SELECT m.id, m.room_id, m.sender_id, u.display_name AS sender_name, m.recipient_id, " +
                "m.content, m.message_type, m.created_at FROM messages m " +
                "JOIN users u ON u.id = m.sender_id WHERE m.room_id = ? ORDER BY m.id DESC LIMIT ?";

        List<Message> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            if (beforeMessageId > 0) {
                stmt.setLong(2, beforeMessageId);
                stmt.setInt(3, safeLimit);
            } else {
                stmt.setInt(2, safeLimit);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding messages for room {}", roomId, e);
        }

        Collections.reverse(messages);
        return messages;
    }

    private Message findById(Connection conn, long messageId, String senderName) throws SQLException {
        String sql = "SELECT id, room_id, sender_id, ? AS sender_name, recipient_id, content, message_type, created_at " +
                "FROM messages WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, senderName);
            stmt.setLong(2, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapMessage(rs);
                }
            }
        }
        throw new SQLException("Message not found after insert: " + messageId);
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setRoomId(rs.getLong("room_id"));
        message.setSenderId(rs.getLong("sender_id"));
        message.setSenderName(rs.getString("sender_name"));
        long recipientId = rs.getLong("recipient_id");
        if (!rs.wasNull()) {
            message.setRecipientId(recipientId);
        }
        message.setContent(rs.getString("content"));
        message.setMessageType(Message.MessageType.valueOf(rs.getString("message_type")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            message.setCreatedAt(createdAt.toLocalDateTime());
        }
        return message;
    }
}
