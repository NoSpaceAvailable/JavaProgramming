package com.micord.server.handler;

import com.micord.common.model.FileAttachment;
import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.FileDownloadRequest;
import com.micord.common.protocol.response.FileDownloadStartResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.FileAttachmentRepository;
import com.micord.server.service.FileService;

import java.util.Optional;

public class FileDownloadHandler implements RequestHandler {
    private final FileAttachmentRepository fileRepo = new FileAttachmentRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        FileDownloadRequest req = JsonUtil.fromJson(message.getPayload(), FileDownloadRequest.class);
        Optional<FileAttachment> attOpt = fileRepo.findById(req.getFileId());
        if (attOpt.isEmpty()) {
            client.sendMessage(JsonUtil.wrap(MessageType.FILE_DOWNLOAD_START,
                    new FileDownloadStartResponse(false, "File not found"),
                    message.getRequestId()));
            return;
        }
        FileAttachment att = attOpt.get();
        FileService service = FileService.getInstance();
        if (!service.canDownload(user.getId(), att)) {
            client.sendMessage(JsonUtil.wrap(MessageType.FILE_DOWNLOAD_START,
                    new FileDownloadStartResponse(false, "Access denied"),
                    message.getRequestId()));
            return;
        }
        service.streamDownload(client, att);
    }
}
