package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.FileUploadCompleteRequest;
import com.micord.common.protocol.response.FileUploadCompleteResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.service.FileService;

public class FileUploadCompleteHandler implements RequestHandler {
    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        FileUploadCompleteRequest req = JsonUtil.fromJson(message.getPayload(), FileUploadCompleteRequest.class);
        FileService.CompleteResult result = FileService.getInstance().completeUpload(
                user.getId(), user.getDisplayName(), req.getFileId(), req.getChecksum());
        FileUploadCompleteResponse response = new FileUploadCompleteResponse(
                result.success(), result.message(),
                result.message_() != null ? result.message_().getId() : 0,
                result.attachment() != null ? result.attachment().getId() : 0);
        client.sendMessage(JsonUtil.wrap(MessageType.FILE_UPLOAD_COMPLETE_RESPONSE,
                response, message.getRequestId()));
    }
}
