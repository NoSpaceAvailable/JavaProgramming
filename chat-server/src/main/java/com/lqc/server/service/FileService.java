package com.lqc.server.service;

import com.lqc.common.model.FileAttachment;
import com.lqc.common.model.Message;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.notification.NewMessageNotification;
import com.lqc.common.protocol.response.FileChunkResponse;
import com.lqc.common.protocol.response.FileDownloadStartResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.manager.SessionManager;
import com.lqc.server.repository.FileAttachmentRepository;
import com.lqc.server.repository.MessageRepository;
import com.lqc.server.repository.RoomRepository;
import com.lqc.server.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    public static final int CHUNK_SIZE = 65536; // 64KB raw bytes per chunk

    private static final FileService INSTANCE = new FileService();
    public static FileService getInstance() { return INSTANCE; }

    private final ConcurrentHashMap<String, UploadSession> uploads = new ConcurrentHashMap<>();
    private final FileAttachmentRepository fileRepo = new FileAttachmentRepository();
    private final MessageRepository messageRepo = new MessageRepository();
    private final RoomRepository roomRepo = new RoomRepository();

    private final Path storageRoot;
    private final long maxFileBytes;

    private FileService() {
        String configured = ConfigUtil.get("file.storage.path", "./uploads");
        this.storageRoot = Path.of(configured).toAbsolutePath().normalize();
        this.maxFileBytes = ConfigUtil.getInt("file.max.size.mb", 50) * 1024L * 1024L;
        try {
            Files.createDirectories(storageRoot);
            Files.createDirectories(storageRoot.resolve("tmp"));
            logger.info("File storage root: {} (max {} bytes)", storageRoot, maxFileBytes);
        } catch (IOException e) {
            logger.error("Failed to create file storage directories", e);
        }
    }

    public long getMaxFileBytes() { return maxFileBytes; }

    public record StartResult(boolean success, String message, String fileId) {}
    public record ChunkResult(boolean success, String message, long bytesReceived) {}
    public record CompleteResult(boolean success, String message, Message message_, FileAttachment attachment) {}

    public StartResult startUpload(long uploaderId, String fileName, long fileSize,
                                    String mimeType, Long roomId, Long recipientId) {
        if (fileName == null || fileName.isBlank()) {
            return new StartResult(false, "Filename required", null);
        }
        if (fileSize <= 0) {
            return new StartResult(false, "Invalid file size", null);
        }
        if (fileSize > maxFileBytes) {
            return new StartResult(false, "File exceeds max size (" + (maxFileBytes / (1024 * 1024)) + " MB)", null);
        }
        if (roomId == null && recipientId == null) {
            return new StartResult(false, "roomId or recipientId required", null);
        }
        if (roomId != null && !roomRepo.isMember(roomId, uploaderId)) {
            return new StartResult(false, "Not a member of this room", null);
        }

        String fileId = UUID.randomUUID().toString();
        Path tmp = storageRoot.resolve("tmp").resolve(fileId);
        try {
            Files.createFile(tmp);
        } catch (IOException e) {
            logger.error("Failed to create temp upload file", e);
            return new StartResult(false, "Storage error", null);
        }

        UploadSession session = new UploadSession(fileId, uploaderId, sanitize(fileName),
                fileSize, mimeType, roomId, recipientId, tmp);
        uploads.put(fileId, session);
        return new StartResult(true, "OK", fileId);
    }

    public ChunkResult writeChunk(long uploaderId, String fileId, int chunkIndex, byte[] data) {
        UploadSession session = uploads.get(fileId);
        if (session == null) return new ChunkResult(false, "Unknown upload", 0);
        if (session.uploaderId != uploaderId) return new ChunkResult(false, "Not your upload", 0);

        synchronized (session) {
            try (RandomAccessFile raf = new RandomAccessFile(session.tmpPath.toFile(), "rw")) {
                long offset = (long) chunkIndex * CHUNK_SIZE;
                if (offset + data.length > session.fileSize) {
                    return new ChunkResult(false, "Chunk exceeds declared file size", session.bytesReceived);
                }
                raf.seek(offset);
                raf.write(data);
                session.bytesReceived = Math.max(session.bytesReceived, offset + data.length);
                return new ChunkResult(true, "OK", session.bytesReceived);
            } catch (IOException e) {
                logger.error("Failed to write chunk for upload {}", fileId, e);
                return new ChunkResult(false, "Storage error", session.bytesReceived);
            }
        }
    }

    public CompleteResult completeUpload(long uploaderId, String senderName,
                                          String fileId, String expectedChecksum) {
        UploadSession session = uploads.remove(fileId);
        if (session == null) return new CompleteResult(false, "Unknown upload", null, null);
        if (session.uploaderId != uploaderId) {
            return new CompleteResult(false, "Not your upload", null, null);
        }

        try {
            String actual = sha256(session.tmpPath);
            if (expectedChecksum != null && !expectedChecksum.isBlank()
                    && !expectedChecksum.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(session.tmpPath);
                return new CompleteResult(false, "Checksum mismatch", null, null);
            }

            Path finalPath = storageRoot.resolve(fileId + "-" + session.fileName);
            Files.move(session.tmpPath, finalPath);

            Message msg = messageRepo.saveFileMessage(
                    session.roomId, session.uploaderId, senderName,
                    session.recipientId, session.fileName);
            FileAttachment att = fileRepo.create(msg.getId(), session.fileName,
                    finalPath.toString(), session.fileSize,
                    session.mimeType == null ? "application/octet-stream" : session.mimeType,
                    actual);
            msg.setAttachment(att);

            broadcastFileMessage(msg, att);
            return new CompleteResult(true, "OK", msg, att);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Failed to finalize upload {}", fileId, e);
            try { Files.deleteIfExists(session.tmpPath); } catch (IOException ignored) {}
            return new CompleteResult(false, "Storage error", null, null);
        }
    }

    public void cancelUpload(long uploaderId, String fileId) {
        UploadSession session = uploads.remove(fileId);
        if (session == null || session.uploaderId != uploaderId) return;
        try { Files.deleteIfExists(session.tmpPath); } catch (IOException ignored) {}
    }

    public boolean canDownload(long userId, FileAttachment att) {
        Optional<Message> msgOpt = messageRepo.findById(att.getMessageId());
        if (msgOpt.isEmpty()) return false;
        Message m = msgOpt.get();
        if (m.getRoomId() != null) return roomRepo.isMember(m.getRoomId(), userId);
        return m.getSenderId() == userId
                || (m.getRecipientId() != null && m.getRecipientId() == userId);
    }

    public void streamDownload(ClientHandler client, FileAttachment att) {
        Path path = Path.of(att.getFilePath());
        long size = att.getFileSize();
        int total = (int) ((size + CHUNK_SIZE - 1) / CHUNK_SIZE);

        FileDownloadStartResponse start = new FileDownloadStartResponse(true, "OK");
        start.setAttachmentId(att.getId());
        start.setFileName(att.getFileName());
        start.setFileSize(size);
        start.setMimeType(att.getMimeType());
        start.setTotalChunks(total);
        start.setChunkSize(CHUNK_SIZE);
        client.sendMessage(JsonUtil.wrap(MessageType.FILE_DOWNLOAD_START, start));

        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] buf = new byte[CHUNK_SIZE];
            for (int i = 0; i < total; i++) {
                int read = raf.read(buf);
                if (read <= 0) break;
                byte[] slice = read == buf.length ? buf : java.util.Arrays.copyOf(buf, read);
                FileChunkResponse chunk = new FileChunkResponse();
                chunk.setAttachmentId(att.getId());
                chunk.setChunkIndex(i);
                chunk.setTotalChunks(total);
                chunk.setData(java.util.Base64.getEncoder().encodeToString(slice));
                chunk.setLast(i == total - 1);
                client.sendMessage(JsonUtil.wrap(MessageType.FILE_DOWNLOAD_CHUNK, chunk));
            }
            client.sendMessage(JsonUtil.wrap(MessageType.FILE_DOWNLOAD_COMPLETE, start));
        } catch (IOException e) {
            logger.error("Error streaming file {}", att.getId(), e);
        }
    }

    private void broadcastFileMessage(Message m, FileAttachment att) {
        NewMessageNotification n = new NewMessageNotification();
        n.setMessageId(m.getId());
        n.setRoomId(m.getRoomId());
        n.setRecipientId(m.getRecipientId());
        n.setSenderId(m.getSenderId());
        n.setSenderName(m.getSenderName());
        n.setContent(m.getContent());
        n.setMessageType("FILE");
        n.setTimestamp(m.getCreatedAt() != null
                ? m.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis());
        n.setFileAttachmentId(att.getId());
        n.setFileName(att.getFileName());
        n.setFileSize(att.getFileSize());
        n.setMimeType(att.getMimeType());

        var notif = JsonUtil.wrap(MessageType.NEW_MESSAGE_NOTIFICATION, n);
        SessionManager sessions = SessionManager.getInstance();
        if (m.getRoomId() != null) {
            for (Long memberId : roomRepo.getMemberIds(m.getRoomId())) {
                ClientHandler h = sessions.getSession(memberId);
                if (h != null && h.isConnected()) h.sendMessage(notif);
            }
        } else {
            ClientHandler r = sessions.getSession(m.getRecipientId());
            if (r != null && r.isConnected()) r.sendMessage(notif);
            ClientHandler s = sessions.getSession(m.getSenderId());
            if (s != null && s.isConnected()) s.sendMessage(notif);
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(path)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private static final class UploadSession {
        final String fileId;
        final long uploaderId;
        final String fileName;
        final long fileSize;
        final String mimeType;
        final Long roomId;
        final Long recipientId;
        final Path tmpPath;
        long bytesReceived;

        UploadSession(String fileId, long uploaderId, String fileName, long fileSize,
                       String mimeType, Long roomId, Long recipientId, Path tmpPath) {
            this.fileId = fileId;
            this.uploaderId = uploaderId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.roomId = roomId;
            this.recipientId = recipientId;
            this.tmpPath = tmpPath;
        }
    }
}
