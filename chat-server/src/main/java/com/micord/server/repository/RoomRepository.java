package com.micord.server.repository;

import com.micord.common.model.Room;
import com.micord.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomRepository.class);

    public List<Room> findRoomsByUserId(long userId) {
        String sql = "SELECT r.id, r.name, r.description, r.owner_id, r.is_private, r.server_id, r.created_at " +
                "FROM rooms r INNER JOIN room_members rm ON r.id = rm.room_id " +
                "WHERE rm.user_id = ? AND r.server_id IS NULL ORDER BY r.name";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding rooms for user {}", userId, e);
        }
        return rooms;
    }

    public List<Room> findAllPublicRooms() {
        String sql = "SELECT id, name, description, owner_id, is_private, server_id, created_at " +
                "FROM rooms WHERE is_private = FALSE AND server_id IS NULL ORDER BY name";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding public rooms", e);
        }
        return rooms;
    }

    public Optional<Room> findById(long roomId) {
        String sql = "SELECT id, name, description, owner_id, is_private, server_id, created_at FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding room by id {}", roomId, e);
        }
        return Optional.empty();
    }

    public Optional<Room> findByName(String name) {
        String sql = "SELECT id, name, description, owner_id, is_private, server_id, created_at " +
                "FROM rooms WHERE LOWER(name) = LOWER(?) AND server_id IS NULL";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRoom(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding room by name {}", name, e);
        }
        return Optional.empty();
    }

    public Room create(String name, String description, long ownerId, boolean isPrivate) {
        String sql = "INSERT INTO rooms (name, description, owner_id, is_private) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setLong(3, ownerId);
            stmt.setBoolean(4, isPrivate);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                Room room = new Room(keys.getLong(1), name, ownerId);
                room.setDescription(description);
                room.setPrivate(isPrivate);
                addMember(room.getId(), ownerId, "OWNER");
                logger.info("Created room: {} (id={})", name, room.getId());
                return room;
            }
            throw new SQLException("Failed to get generated key for room");
        } catch (SQLException e) {
            logger.error("Error creating room: {}", name, e);
            throw new RuntimeException("Failed to create room", e);
        }
    }

    public void addMember(long roomId, long userId, String role) {
        String sql = "INSERT INTO room_members (room_id, user_id, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding member {} to room {}", userId, roomId, e);
        }
    }

    public void removeMember(long roomId, long userId) {
        String sql = "DELETE FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing member {} from room {}", userId, roomId, e);
        }
    }

    public boolean isMember(long roomId, long userId) {
        String sql = "SELECT 1 FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            stmt.setLong(2, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking membership for user {} in room {}", userId, roomId, e);
            return false;
        }
    }

    public List<Long> getMemberIds(long roomId) {
        String sql = "SELECT user_id FROM room_members WHERE room_id = ?";
        List<Long> ids = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            logger.error("Error getting members for room {}", roomId, e);
        }
        return ids;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getLong("id"));
        room.setName(rs.getString("name"));
        room.setDescription(rs.getString("description"));
        room.setOwnerId(rs.getLong("owner_id"));
        room.setPrivate(rs.getBoolean("is_private"));
        long serverId = rs.getLong("server_id");
        if (!rs.wasNull()) room.setServerId(serverId);
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) room.setCreatedAt(createdAt.toLocalDateTime());
        return room;
    }

    // ---- Channels (a room with a non-null server_id) ----

    public Room createChannel(long serverId, String name, long ownerId) {
        String sql = "INSERT INTO rooms (name, owner_id, is_private, server_id) VALUES (?, ?, FALSE, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setLong(2, ownerId);
            stmt.setLong(3, serverId);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                Room channel = new Room(keys.getLong(1), name, ownerId);
                channel.setServerId(serverId);
                logger.info("Created channel '{}' (id={}) in server {}", name, channel.getId(), serverId);
                return channel;
            }
            throw new SQLException("Failed to get generated key for channel");
        } catch (SQLException e) {
            logger.error("Error creating channel '{}' in server {}", name, serverId, e);
            throw new RuntimeException("Failed to create channel", e);
        }
    }

    /** Removes a user from every channel (room) of a server — used when they are kicked/banned. */
    public void removeMemberFromServerChannels(long serverId, long userId) {
        String sql = "DELETE FROM room_members WHERE user_id = ? AND room_id IN " +
                "(SELECT id FROM rooms WHERE server_id = ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, serverId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing user {} from channels of server {}", userId, serverId, e);
        }
    }

    public List<Room> findChannelsByServerId(long serverId) {
        String sql = "SELECT id, name, description, owner_id, is_private, server_id, created_at " +
                "FROM rooms WHERE server_id = ? ORDER BY created_at, id";
        List<Room> channels = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) channels.add(mapRoom(rs));
        } catch (SQLException e) {
            logger.error("Error finding channels for server {}", serverId, e);
        }
        return channels;
    }
}
