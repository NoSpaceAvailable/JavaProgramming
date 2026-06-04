package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.FileUploadStartRequest;
import com.micord.common.protocol.response.FileUploadStartResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.FileService;

public class FileUploadStartHandler implements RequestHandler {
    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        FileUploadStartRequest req = JsonUtil.fromJson(message.getPayload(), FileUploadStartRequest.class);
        FileService.StartResult result = FileService.getInstance().startUpload(
                user.getId(), req.getFileName(), req.getFileSize(),
                req.getMimeType(), req.getRoomId(), req.getRecipientId());
        FileUploadStartResponse response = new FileUploadStartResponse(
                result.success(), result.message(), result.fileId(), FileService.CHUNK_SIZE);
        client.sendMessage(JsonUtil.wrap(MessageType.FILE_UPLOAD_START_RESPONSE,
                response, message.getRequestId()));
    }
}
