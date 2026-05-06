package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.FileUploadStartRequest;
import com.lqc.common.protocol.response.FileUploadStartResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.FileService;

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
