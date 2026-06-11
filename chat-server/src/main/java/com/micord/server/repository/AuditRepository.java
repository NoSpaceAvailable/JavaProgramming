package com.micord.server.repository;

import com.micord.common.model.AuditEntry;
import com.micord.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuditRepository.class);

    public void log(long serverId, Long actorId, String actorName, String action, String detail) {
        String sql = "INSERT INTO audit_log (server_id, actor_id, actor_name, action, detail) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            if (actorId != null) stmt.setLong(2, actorId); else stmt.setNull(2, Types.BIGINT);
            stmt.setString(3, actorName);
            stmt.setString(4, action);
            stmt.setString(5, detail);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error writing audit entry for server {}", serverId, e);
        }
    }

    public List<AuditEntry> findByServerId(long serverId, int limit) {
        String sql = "SELECT id, server_id, actor_name, action, detail, created_at " +
                "FROM audit_log WHERE server_id = ? ORDER BY id DESC LIMIT ?";
        List<AuditEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, serverId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                AuditEntry e = new AuditEntry();
                e.setId(rs.getLong("id"));
                e.setServerId(rs.getLong("server_id"));
                e.setActorName(rs.getString("actor_name"));
                e.setAction(rs.getString("action"));
                e.setDetail(rs.getString("detail"));
                Timestamp t = rs.getTimestamp("created_at");
                if (t != null) e.setCreatedAt(t.toLocalDateTime());
                entries.add(e);
            }
        } catch (SQLException ex) {
            logger.error("Error reading audit log for server {}", serverId, ex);
        }
        return entries;
    }
}
