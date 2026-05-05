package com.lqc.server.repository;

import com.lqc.common.model.User;
import com.lqc.common.model.UserStatus;
import com.lqc.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, display_name, avatar_url, status, created_at, last_seen_at " +
                "FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
            return Optional.empty();
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, username, password_hash, display_name, avatar_url, status, created_at, last_seen_at " +
                "FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", id, e);
            return Optional.empty();
        }
    }

    public User create(String username, String passwordHash, String displayName) {
        String sql = "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, displayName);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                User user = new User(keys.getLong(1), username, displayName);
                user.setPasswordHash(passwordHash);
                logger.info("Created user: {} (id={})", username, user.getId());
                return user;
            }
            throw new SQLException("Failed to get generated key for user");
        } catch (SQLException e) {
            logger.error("Error creating user: {}", username, e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    public void updateStatus(long userId, UserStatus status) {
        String sql = "UPDATE users SET status = ?, last_seen_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating status for user {}", userId, e);
        }
    }

    public List<User> findAllExcept(long excludeUserId) {
        String sql = "SELECT id, username, password_hash, display_name, avatar_url, status, created_at, last_seen_at " +
                "FROM users WHERE id <> ? ORDER BY display_name";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, excludeUserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User user = mapUser(rs);
                user.setPasswordHash(null);
                users.add(user);
            }
        } catch (SQLException e) {
            logger.error("Error listing users", e);
        }
        return users;
    }

    public List<User> findByRoomId(long roomId) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.display_name, u.avatar_url, u.status, " +
                "u.created_at, u.last_seen_at " +
                "FROM users u INNER JOIN room_members rm ON u.id = rm.user_id " +
                "WHERE rm.room_id = ? ORDER BY u.display_name";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User user = mapUser(rs);
                user.setPasswordHash(null);
                users.add(user);
            }
        } catch (SQLException e) {
            logger.error("Error listing room members for room {}", roomId, e);
        }
        return users;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking username existence: {}", username, e);
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        String statusStr = rs.getString("status");
        user.setStatus(statusStr != null ? UserStatus.valueOf(statusStr) : UserStatus.OFFLINE);
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp lastSeen = rs.getTimestamp("last_seen_at");
        if (lastSeen != null) user.setLastSeenAt(lastSeen.toLocalDateTime());
        return user;
    }
}
