package com.lqc.server.repository;

import com.lqc.common.model.FileAttachment;
import com.lqc.server.database.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FileAttachmentRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentRepository.class);

    public FileAttachment create(long messageId, String fileName, String filePath,
                                  long fileSize, String mimeType, String checksum) {
        String sql = "INSERT INTO file_attachments (message_id, file_name, file_path, file_size, mime_type, checksum) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, uploaded_at";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.setString(2, fileName);
            stmt.setString(3, filePath);
            stmt.setLong(4, fileSize);
            stmt.setString(5, mimeType);
            stmt.setString(6, checksum);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                FileAttachment a = new FileAttachment();
                a.setId(rs.getLong("id"));
                a.setMessageId(messageId);
                a.setFileName(fileName);
                a.setFilePath(filePath);
                a.setFileSize(fileSize);
                a.setMimeType(mimeType);
                a.setChecksum(checksum);
                Timestamp t = rs.getTimestamp("uploaded_at");
                if (t != null) a.setUploadedAt(t.toLocalDateTime());
                return a;
            }
            throw new SQLException("Failed to insert file attachment");
        } catch (SQLException e) {
            logger.error("Error creating file attachment for message {}", messageId, e);
            throw new RuntimeException("Failed to create file attachment", e);
        }
    }

    public Optional<FileAttachment> findById(long attachmentId) {
        String sql = "SELECT id, message_id, file_name, file_path, file_size, mime_type, checksum, uploaded_at " +
                "FROM file_attachments WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, attachmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (SQLException e) {
            logger.error("Error finding attachment {}", attachmentId, e);
        }
        return Optional.empty();
    }

    public Map<Long, FileAttachment> findByMessageIds(List<Long> messageIds) {
        Map<Long, FileAttachment> result = new HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) return result;
        String placeholders = String.join(",", messageIds.stream().map(id -> "?").toList());
        String sql = "SELECT id, message_id, file_name, file_path, file_size, mime_type, checksum, uploaded_at " +
                "FROM file_attachments WHERE message_id IN (" + placeholders + ")";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < messageIds.size(); i++) {
                stmt.setLong(i + 1, messageIds.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FileAttachment a = map(rs);
                result.put(a.getMessageId(), a);
            }
        } catch (SQLException e) {
            logger.error("Error fetching attachments for messages", e);
        }
        return result;
    }

    private FileAttachment map(ResultSet rs) throws SQLException {
        FileAttachment a = new FileAttachment();
        a.setId(rs.getLong("id"));
        a.setMessageId(rs.getLong("message_id"));
        a.setFileName(rs.getString("file_name"));
        a.setFilePath(rs.getString("file_path"));
        a.setFileSize(rs.getLong("file_size"));
        a.setMimeType(rs.getString("mime_type"));
        a.setChecksum(rs.getString("checksum"));
        Timestamp t = rs.getTimestamp("uploaded_at");
        if (t != null) a.setUploadedAt(t.toLocalDateTime());
        return a;
    }
}
