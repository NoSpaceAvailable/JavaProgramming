package com.lqc.server.repository;

import com.lqc.common.model.Message;
import com.lqc.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lqc.common.model.FileAttachment;
import com.lqc.common.model.Reaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);

    private final FileAttachmentRepository fileAttachmentRepository = new FileAttachmentRepository();
    private final ReactionRepository reactionRepository = new ReactionRepository();

    public Optional<Message> findById(long messageId) {
        String sql = "SELECT m.id, m.room_id, m.sender_id, m.recipient_id, m.content, m.message_type, " +
                "m.created_at, u.display_name AS sender_name " +
                "FROM messages m INNER JOIN users u ON m.sender_id = u.id WHERE m.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapMessage(rs));
        } catch (SQLException e) {
            logger.error("Error finding message {}", messageId, e);
        }
        return Optional.empty();
    }

    public Message saveFileMessage(Long roomId, long senderId, String senderName,
                                    Long recipientId, String content) {
        String sql;
        boolean isDm = roomId == null;
        if (isDm) {
            sql = "INSERT INTO messages (sender_id, recipient_id, content, message_type) " +
                    "VALUES (?, ?, ?, 'FILE') RETURNING id, created_at";
        } else {
            sql = "INSERT INTO messages (room_id, sender_id, content, message_type) " +
                    "VALUES (?, ?, ?, 'FILE') RETURNING id, created_at";
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (isDm) {
                stmt.setLong(1, senderId);
                stmt.setLong(2, recipientId);
                stmt.setString(3, content);
            } else {
                stmt.setLong(1, roomId);
                stmt.setLong(2, senderId);
                stmt.setString(3, content);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Message m = new Message(senderId, senderName, content);
                m.setMessageType(Message.MessageType.FILE);
                m.setId(rs.getLong("id"));
                m.setRoomId(roomId);
                m.setRecipientId(recipientId);
                Timestamp t = rs.getTimestamp("created_at");
                if (t != null) m.setCreatedAt(t.toLocalDateTime());
                return m;
            }
            throw new SQLException("Failed to insert file message");
        } catch (SQLException e) {
            logger.error("Error saving file message", e);
            throw new RuntimeException("Failed to save file message", e);
        }
    }

    public Message saveRoomMessage(long roomId, long senderId, String senderName, String content) {
        return saveRoomMessage(roomId, senderId, senderName, content, null);
    }

    public Message saveRoomMessage(long roomId, long senderId, String senderName, String content, String msgType) {
        String type = msgType != null ? msgType : "TEXT";
        String sql = "INSERT INTO messages (room_id, sender_id, content, message_type) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, senderId);
            stmt.setString(3, content);
            stmt.setString(4, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Message m = new Message(senderId, senderName, content);
                m.setMessageType(Message.MessageType.valueOf(type));
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
        return savePrivateMessage(senderId, senderName, recipientId, content, null);
    }

    public Message savePrivateMessage(long senderId, String senderName, long recipientId, String content, String msgType) {
        String type = msgType != null ? msgType : "TEXT";
        String sql = "INSERT INTO messages (sender_id, recipient_id, content, message_type) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, recipientId);
            stmt.setString(3, content);
            stmt.setString(4, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Message m = new Message(senderId, senderName, content);
                m.setMessageType(Message.MessageType.valueOf(type));
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

    public List<Message> searchRoomMessages(long roomId, String query, int limit) {
        String sql = "SELECT m.id, m.room_id, m.sender_id, m.recipient_id, m.content, m.message_type, " +
                "m.created_at, u.display_name AS sender_name " +
                "FROM messages m INNER JOIN users u ON m.sender_id = u.id " +
                "WHERE m.room_id = ? AND m.content ILIKE ? " +
                "ORDER BY m.id DESC LIMIT ?";
        String like = "%" + escapeLike(query) + "%";
        return queryMessages(sql, stmt -> {
            stmt.setLong(1, roomId);
            stmt.setString(2, like);
            stmt.setInt(3, limit);
        });
    }

    public List<Message> searchDmMessages(long userA, long userB, String query, int limit) {
        String sql = "SELECT m.id, m.room_id, m.sender_id, m.recipient_id, m.content, m.message_type, " +
                "m.created_at, u.display_name AS sender_name " +
                "FROM messages m INNER JOIN users u ON m.sender_id = u.id " +
                "WHERE m.room_id IS NULL AND " +
                "((m.sender_id = ? AND m.recipient_id = ?) OR (m.sender_id = ? AND m.recipient_id = ?)) " +
                "AND m.content ILIKE ? " +
                "ORDER BY m.id DESC LIMIT ?";
        String like = "%" + escapeLike(query) + "%";
        return queryMessages(sql, stmt -> {
            stmt.setLong(1, userA);
            stmt.setLong(2, userB);
            stmt.setLong(3, userB);
            stmt.setLong(4, userA);
            stmt.setString(5, like);
            stmt.setInt(6, limit);
        });
    }

    /** Escapes LIKE wildcards so a user's literal % or _ doesn't act as a pattern. */
    private static String escapeLike(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public List<long[]> getRecentDmPeerIds(long userId) {
        String sql = "SELECT sub.peer_id FROM (" +
                "SELECT CASE WHEN m.sender_id = ? THEN m.recipient_id ELSE m.sender_id END AS peer_id, " +
                "MAX(m.id) AS last_msg_id " +
                "FROM messages m WHERE m.room_id IS NULL AND (m.sender_id = ? OR m.recipient_id = ?) " +
                "GROUP BY 1" +
                ") sub ORDER BY sub.last_msg_id DESC";
        List<long[]> peers = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                peers.add(new long[]{rs.getLong("peer_id")});
            }
        } catch (SQLException e) {
            logger.error("Error fetching recent DM peers for user {}", userId, e);
        }
        return peers;
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
        hydrateAttachmentsAndReactions(messages);
        return messages;
    }

    private void hydrateAttachmentsAndReactions(List<Message> messages) {
        if (messages.isEmpty()) return;
        List<Long> ids = messages.stream().map(Message::getId).toList();
        Map<Long, FileAttachment> attachments = fileAttachmentRepository.findByMessageIds(ids);
        Map<Long, List<Reaction>> reactions = reactionRepository.findByMessageIds(ids);
        for (Message m : messages) {
            FileAttachment a = attachments.get(m.getId());
            if (a != null) m.setAttachment(a);
            List<Reaction> r = reactions.get(m.getId());
            if (r != null) m.setReactions(r);
        }
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
