package com.micord.server.repository;

import com.micord.common.model.Server;
import com.micord.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerRepository {
    private static final Logger logger = LoggerFactory.getLogger(ServerRepository.class);
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars
    private static final SecureRandom RANDOM = new SecureRandom();

    public Server create(String name, long ownerId) {
        String inviteCode = generateUniqueInviteCode();
        String sql = "INSERT INTO servers (name, owner_id, invite_code) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setLong(2, ownerId);
            stmt.setString(3, inviteCode);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                Server server = new Server(keys.getLong(1), name, ownerId, inviteCode);
                logger.info("Created server '{}' (id={}, invite={})", name, server.getId(), inviteCode);
                return server;
            }
            throw new SQLException("Failed to get generated key for server");
        } catch (SQLException e) {
            logger.error("Error creating server '{}'", name, e);
            throw new RuntimeException("Failed to create server", e);
        }
    }

    public Optional<Server> findById(long serverId) {
        return queryOne("SELECT id, name, owner_id, invite_code, created_at FROM servers WHERE id = ?",
                stmt -> stmt.setLong(1, serverId));
    }

    public Optional<Server> findByInviteCode(String code) {
        return queryOne("SELECT id, name, owner_id, invite_code, created_at FROM servers WHERE invite_code = ?",
                stmt -> stmt.setString(1, code));
    }

    /** Servers the user is a member of, each annotated with the user's role. */
    public List<Server> findServersByUserId(long userId) {
        String sql = "SELECT s.id, s.name, s.owner_id, s.invite_code, s.created_at, sm.role " +
                "FROM servers s INNER JOIN server_members sm ON s.id = sm.server_id " +
                "WHERE sm.user_id = ? ORDER BY s.name";
        List<Server> servers = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Server s = mapServer(rs);
                s.setMyRole(rs.getString("role"));
                servers.add(s);
            }
        } catch (SQLException e) {
            logger.error("Error finding servers for user {}", userId, e);
        }
        return servers;
    }

    public void addMember(long serverId, long userId, String role) {
        String sql = "INSERT INTO server_members (server_id, user_id, role) VALUES (?, ?, ?) " +
                "ON CONFLICT (server_id, user_id) DO NOTHING";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding member {} to server {}", userId, serverId, e);
        }
    }

    public boolean isMember(long serverId, long userId) {
        String sql = "SELECT 1 FROM server_members WHERE server_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking server membership for user {} in server {}", userId, serverId, e);
            return false;
        }
    }

    public List<Long> getMemberIds(long serverId) {
        String sql = "SELECT user_id FROM server_members WHERE server_id = ?";
        List<Long> ids = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getLong("user_id"));
        } catch (SQLException e) {
            logger.error("Error getting members for server {}", serverId, e);
        }
        return ids;
    }

    public String getRole(long serverId, long userId) {
        String sql = "SELECT role FROM server_members WHERE server_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("role");
        } catch (SQLException e) {
            logger.error("Error getting role for user {} in server {}", userId, serverId, e);
        }
        return null;
    }

    public void setRole(long serverId, long userId, String role) {
        String sql = "UPDATE server_members SET role = ? WHERE server_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setLong(2, serverId);
            stmt.setLong(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error setting role {} for user {} in server {}", role, userId, serverId, e);
        }
    }

    public void removeMember(long serverId, long userId) {
        String sql = "DELETE FROM server_members WHERE server_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing member {} from server {}", userId, serverId, e);
        }
    }

    public void ban(long serverId, long userId, long bannedBy, String reason) {
        String sql = "INSERT INTO server_bans (server_id, user_id, banned_by, reason) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (server_id, user_id) DO UPDATE SET reason = EXCLUDED.reason, banned_by = EXCLUDED.banned_by";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            stmt.setLong(3, bannedBy);
            stmt.setString(4, reason);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error banning user {} from server {}", userId, serverId, e);
        }
    }

    public void unban(long serverId, long userId) {
        String sql = "DELETE FROM server_bans WHERE server_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error unbanning user {} from server {}", userId, serverId, e);
        }
    }

    public List<com.micord.common.model.User> getBannedUsers(long serverId) {
        String sql = "SELECT u.id, u.username, u.display_name, u.avatar_url " +
                "FROM server_bans b INNER JOIN users u ON b.user_id = u.id " +
                "WHERE b.server_id = ? ORDER BY u.display_name";
        List<com.micord.common.model.User> users = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                com.micord.common.model.User u = new com.micord.common.model.User(
                        rs.getLong("id"), rs.getString("username"), rs.getString("display_name"));
                u.setAvatarUrl(rs.getString("avatar_url"));
                users.add(u);
            }
        } catch (SQLException e) {
            logger.error("Error listing bans for server {}", serverId, e);
        }
        return users;
    }

    public boolean isBanned(long serverId, long userId) {
        String sql = "SELECT 1 FROM server_bans WHERE server_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setLong(2, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking ban for user {} in server {}", userId, serverId, e);
            return false;
        }
    }

    /** Members of a server as User objects with {@code serverRole} populated. */
    public List<com.micord.common.model.User> getServerMembers(long serverId) {
        String sql = "SELECT u.id, u.username, u.display_name, u.avatar_url, u.status, sm.role " +
                "FROM server_members sm INNER JOIN users u ON sm.user_id = u.id " +
                "WHERE sm.server_id = ? ORDER BY " +
                "CASE sm.role WHEN 'OWNER' THEN 0 WHEN 'ADMIN' THEN 1 WHEN 'MODERATOR' THEN 2 ELSE 3 END, u.display_name";
        List<com.micord.common.model.User> members = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                com.micord.common.model.User u = new com.micord.common.model.User(
                        rs.getLong("id"), rs.getString("username"), rs.getString("display_name"));
                u.setAvatarUrl(rs.getString("avatar_url"));
                try {
                    u.setStatus(com.micord.common.model.UserStatus.valueOf(rs.getString("status")));
                } catch (Exception ignored) {
                    u.setStatus(com.micord.common.model.UserStatus.OFFLINE);
                }
                u.setServerRole(rs.getString("role"));
                members.add(u);
            }
        } catch (SQLException e) {
            logger.error("Error getting members for server {}", serverId, e);
        }
        return members;
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (findByInviteCode(code).isEmpty()) return code;
        }
        throw new RuntimeException("Could not generate a unique invite code");
    }

    private interface Binder { void bind(PreparedStatement stmt) throws SQLException; }

    private Optional<Server> queryOne(String sql, Binder binder) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            binder.bind(stmt);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapServer(rs));
        } catch (SQLException e) {
            logger.error("Error querying server", e);
        }
        return Optional.empty();
    }

    private Server mapServer(ResultSet rs) throws SQLException {
        Server s = new Server();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        s.setOwnerId(rs.getLong("owner_id"));
        s.setInviteCode(rs.getString("invite_code"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) s.setCreatedAt(t.toLocalDateTime());
        return s;
    }
}
