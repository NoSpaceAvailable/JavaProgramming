package com.micord.server.repository;

import com.micord.common.model.Reaction;
import com.micord.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReactionRepository.class);

    public boolean add(long messageId, long userId, String emoji) {
        String sql = "INSERT INTO reactions (message_id, user_id, emoji) VALUES (?, ?, ?) " +
                "ON CONFLICT (message_id, user_id, emoji) DO NOTHING";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.setLong(2, userId);
            stmt.setString(3, emoji);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error adding reaction", e);
            return false;
        }
    }

    public boolean remove(long messageId, long userId, String emoji) {
        String sql = "DELETE FROM reactions WHERE message_id = ? AND user_id = ? AND emoji = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.setLong(2, userId);
            stmt.setString(3, emoji);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error removing reaction", e);
            return false;
        }
    }

    public Map<Long, List<Reaction>> findByMessageIds(List<Long> messageIds) {
        Map<Long, List<Reaction>> result = new HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) return result;
        String placeholders = String.join(",", messageIds.stream().map(id -> "?").toList());
        String sql = "SELECT id, message_id, user_id, emoji, created_at " +
                "FROM reactions WHERE message_id IN (" + placeholders + ") ORDER BY created_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < messageIds.size(); i++) {
                stmt.setLong(i + 1, messageIds.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Reaction r = new Reaction();
                r.setId(rs.getLong("id"));
                r.setMessageId(rs.getLong("message_id"));
                r.setUserId(rs.getLong("user_id"));
                r.setEmoji(rs.getString("emoji"));
                Timestamp t = rs.getTimestamp("created_at");
                if (t != null) r.setCreatedAt(t.toLocalDateTime());
                result.computeIfAbsent(r.getMessageId(), k -> new ArrayList<>()).add(r);
            }
        } catch (SQLException e) {
            logger.error("Error fetching reactions", e);
        }
        return result;
    }
}
