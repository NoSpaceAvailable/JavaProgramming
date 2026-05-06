package com.lqc.client.net;

import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.FileChunkRequest;
import com.lqc.common.protocol.request.FileDownloadRequest;
import com.lqc.common.protocol.request.FileUploadCompleteRequest;
import com.lqc.common.protocol.request.FileUploadStartRequest;
import com.lqc.common.protocol.response.FileChunkAckResponse;
import com.lqc.common.protocol.response.FileChunkResponse;
import com.lqc.common.protocol.response.FileDownloadStartResponse;
import com.lqc.common.protocol.response.FileUploadCompleteResponse;
import com.lqc.common.protocol.response.FileUploadStartResponse;
import com.lqc.common.util.JsonUtil;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Drives chunked uploads and downloads from the client side. Sessions are tracked by
 * fileId (upload) or attachmentId (download) and progress callbacks run on the FX thread.
 */
public class FileTransferClient implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(FileTransferClient.class);
    private static final FileTransferClient INSTANCE = new FileTransferClient();
    public static FileTransferClient getInstance() { return INSTANCE; }

    private final ServerConnection connection = ServerConnection.getInstance();
    private final ConcurrentHashMap<String, UploadSession> uploads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, DownloadSession> downloads = new ConcurrentHashMap<>();
    private boolean registered;

    public synchronized void ensureRegistered() {
        if (registered) return;
        connection.addListener(this);
        registered = true;
    }

    public void upload(Path path, Long roomId, Long recipientId, String mimeType,
                        UploadCallback callback) {
        ensureRegistered();
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            callback.onError("Cannot read file: " + e.getMessage());
            return;
        }
        UploadSession s = new UploadSession(path, size, mimeType, callback);
        // We don't know fileId until the server replies. Store keyed by sentinel for now.
        uploads.put(s.localKey, s);

        FileUploadStartRequest req = new FileUploadStartRequest();
        req.setFileName(path.getFileName().toString());
        req.setFileSize(size);
        req.setMimeType(mimeType);
        req.setRoomId(roomId);
        req.setRecipientId(recipientId);
        ProtocolMessage msg = JsonUtil.wrap(MessageType.FILE_UPLOAD_START, req);
        s.startRequestId = msg.getRequestId();
        connection.send(msg);
    }

    public void download(long attachmentId, String fileName, Path savePath, DownloadCallback callback) {
        ensureRegistered();
        DownloadSession s = new DownloadSession(attachmentId, fileName, savePath, callback);
        downloads.put(attachmentId, s);
        connection.send(JsonUtil.wrap(MessageType.FILE_DOWNLOAD_REQUEST,
                new FileDownloadRequest(attachmentId)));
    }

    @Override
    public void onMessageReceived(ProtocolMessage message) {
        switch (message.getType()) {
            case FILE_UPLOAD_START_RESPONSE -> onUploadStartResponse(message);
            case FILE_UPLOAD_CHUNK_ACK -> onChunkAck(message);
            case FILE_UPLOAD_COMPLETE_RESPONSE -> onUploadCompleteResponse(message);
            case FILE_DOWNLOAD_START -> onDownloadStart(message);
            case FILE_DOWNLOAD_CHUNK -> onDownloadChunk(message);
            case FILE_DOWNLOAD_COMPLETE -> onDownloadComplete(message);
            default -> { /* ignore */ }
        }
    }

    private void onUploadStartResponse(ProtocolMessage message) {
        FileUploadStartResponse r = JsonUtil.fromJson(message.getPayload(), FileUploadStartResponse.class);
        UploadSession session = findUploadByRequestId(message.getRequestId());
        if (session == null) return;
        if (!r.isSuccess() || r.getFileId() == null) {
            uploads.remove(session.localKey);
            Platform.runLater(() -> session.callback.onError(r.getMessage()));
            return;
        }
        session.fileId = r.getFileId();
        session.chunkSize = r.getChunkSize() > 0 ? r.getChunkSize() : 65536;
        // Re-key by fileId for ack lookup.
        uploads.remove(session.localKey);
        uploads.put(session.fileId, session);
        sendNextChunk(session);
    }

    private void onChunkAck(ProtocolMessage message) {
        FileChunkAckResponse ack = JsonUtil.fromJson(message.getPayload(), FileChunkAckResponse.class);
        UploadSession session = uploads.get(ack.getFileId());
        if (session == null) return;
        if (!ack.isSuccess()) {
            uploads.remove(session.fileId);
            Platform.runLater(() -> session.callback.onError(ack.getMessage()));
            return;
        }
        Platform.runLater(() -> session.callback.onProgress(ack.getBytesReceived(), session.fileSize));
        if (ack.getBytesReceived() >= session.fileSize) {
            sendComplete(session);
        } else {
            sendNextChunk(session);
        }
    }

    private void onUploadCompleteResponse(ProtocolMessage message) {
        FileUploadCompleteResponse r = JsonUtil.fromJson(message.getPayload(), FileUploadCompleteResponse.class);
        UploadSession session = findUploadByCompleteRequestId(message.getRequestId());
        if (session == null) return;
        uploads.remove(session.fileId);
        if (r.isSuccess()) {
            Platform.runLater(() -> session.callback.onComplete(r.getMessageId(), r.getAttachmentId()));
        } else {
            Platform.runLater(() -> session.callback.onError(r.getMessage()));
        }
    }

    private void onDownloadStart(ProtocolMessage message) {
        FileDownloadStartResponse r = JsonUtil.fromJson(message.getPayload(), FileDownloadStartResponse.class);
        DownloadSession session = downloads.get(r.getAttachmentId());
        if (session == null) {
            // No matching attachmentId? Could be an error response (id=0).
            DownloadSession failing = downloads.values().stream()
                    .filter(s -> !s.started)
                    .findFirst().orElse(null);
            if (failing != null && !r.isSuccess()) {
                downloads.remove(failing.attachmentId);
                Platform.runLater(() -> failing.callback.onError(r.getMessage()));
            }
            return;
        }
        if (!r.isSuccess()) {
            downloads.remove(session.attachmentId);
            Platform.runLater(() -> session.callback.onError(r.getMessage()));
            return;
        }
        session.totalChunks = r.getTotalChunks();
        session.fileSize = r.getFileSize();
        session.started = true;
        try {
            Files.deleteIfExists(session.savePath);
            Files.createFile(session.savePath);
        } catch (IOException e) {
            downloads.remove(session.attachmentId);
            Platform.runLater(() -> session.callback.onError("Cannot create file: " + e.getMessage()));
        }
    }

    private void onDownloadChunk(ProtocolMessage message) {
        FileChunkResponse chunk = JsonUtil.fromJson(message.getPayload(), FileChunkResponse.class);
        DownloadSession session = downloads.get(chunk.getAttachmentId());
        if (session == null) return;
        try (RandomAccessFile raf = new RandomAccessFile(session.savePath.toFile(), "rw")) {
            byte[] data = Base64.getDecoder().decode(chunk.getData());
            raf.seek((long) chunk.getChunkIndex() * 65536);
            raf.write(data);
            session.bytesWritten += data.length;
            Platform.runLater(() -> session.callback.onProgress(session.bytesWritten, session.fileSize));
        } catch (IOException e) {
            downloads.remove(session.attachmentId);
            Platform.runLater(() -> session.callback.onError("Write error: " + e.getMessage()));
        }
    }

    private void onDownloadComplete(ProtocolMessage message) {
        FileDownloadStartResponse r = JsonUtil.fromJson(message.getPayload(), FileDownloadStartResponse.class);
        DownloadSession session = downloads.remove(r.getAttachmentId());
        if (session == null) return;
        Platform.runLater(() -> session.callback.onComplete(session.savePath));
    }

    private void sendNextChunk(UploadSession session) {
        new Thread(() -> {
            try (RandomAccessFile raf = new RandomAccessFile(session.path.toFile(), "r")) {
                long offset = (long) session.nextChunkIndex * session.chunkSize;
                if (offset >= session.fileSize) {
                    sendComplete(session);
                    return;
                }
                raf.seek(offset);
                int toRead = (int) Math.min(session.chunkSize, session.fileSize - offset);
                byte[] buf = new byte[toRead];
                raf.readFully(buf);
                session.digest.update(buf);

                FileChunkRequest req = new FileChunkRequest();
                req.setFileId(session.fileId);
                req.setChunkIndex(session.nextChunkIndex);
                req.setData(Base64.getEncoder().encodeToString(buf));
                session.nextChunkIndex++;
                connection.send(JsonUtil.wrap(MessageType.FILE_UPLOAD_CHUNK, req));
            } catch (IOException e) {
                logger.error("Upload read error", e);
                uploads.remove(session.fileId);
                Platform.runLater(() -> session.callback.onError("Read error: " + e.getMessage()));
            }
        }, "upload-" + session.fileId).start();
    }

    private void sendComplete(UploadSession session) {
        String checksum = HexFormat.of().formatHex(session.digest.digest());
        ProtocolMessage msg = JsonUtil.wrap(MessageType.FILE_UPLOAD_COMPLETE,
                new FileUploadCompleteRequest(session.fileId, checksum));
        session.completeRequestId = msg.getRequestId();
        connection.send(msg);
    }

    private UploadSession findUploadByRequestId(String requestId) {
        if (requestId == null) return null;
        for (UploadSession s : uploads.values()) {
            if (requestId.equals(s.startRequestId)) return s;
        }
        return null;
    }

    private UploadSession findUploadByCompleteRequestId(String requestId) {
        if (requestId == null) return null;
        for (UploadSession s : uploads.values()) {
            if (requestId.equals(s.completeRequestId)) return s;
        }
        return null;
    }

    public interface UploadCallback {
        void onProgress(long bytesSent, long total);
        void onComplete(long messageId, long attachmentId);
        void onError(String message);
    }

    public interface DownloadCallback {
        void onProgress(long bytesReceived, long total);
        void onComplete(Path savedTo);
        void onError(String message);
    }

    /** Convenience for callers that only want a final completion notice. */
    public static UploadCallback simpleUpload(Consumer<String> onError, Runnable onDone) {
        return new UploadCallback() {
            @Override public void onProgress(long sent, long total) { }
            @Override public void onComplete(long messageId, long attachmentId) { onDone.run(); }
            @Override public void onError(String m) { onError.accept(m); }
        };
    }

    private static final class UploadSession {
        final String localKey = java.util.UUID.randomUUID().toString();
        final Path path;
        final long fileSize;
        final String mimeType;
        final UploadCallback callback;
        final MessageDigest digest;
        String fileId;
        int chunkSize = 65536;
        int nextChunkIndex = 0;
        String startRequestId;
        String completeRequestId;

        UploadSession(Path path, long fileSize, String mimeType, UploadCallback callback) {
            this.path = path;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.callback = callback;
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class DownloadSession {
        final long attachmentId;
        final String fileName;
        final Path savePath;
        final DownloadCallback callback;
        long fileSize;
        int totalChunks;
        long bytesWritten;
        boolean started;

        DownloadSession(long attachmentId, String fileName, Path savePath, DownloadCallback callback) {
            this.attachmentId = attachmentId;
            this.fileName = fileName;
            this.savePath = savePath;
            this.callback = callback;
        }
    }
}
