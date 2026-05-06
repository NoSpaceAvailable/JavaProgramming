package com.lqc.server.handler;

import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.FileChunkRequest;
import com.lqc.common.protocol.response.FileChunkAckResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import com.lqc.server.service.FileService;

import java.util.Base64;

public class FileUploadChunkHandler implements RequestHandler {
    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        FileChunkRequest req = JsonUtil.fromJson(message.getPayload(), FileChunkRequest.class);
        byte[] data;
        try {
            data = Base64.getDecoder().decode(req.getData() == null ? "" : req.getData());
        } catch (IllegalArgumentException e) {
            client.sendMessage(JsonUtil.wrap(MessageType.FILE_UPLOAD_CHUNK_ACK,
                    new FileChunkAckResponse(false, "Invalid base64", req.getFileId(),
                            req.getChunkIndex(), 0),
                    message.getRequestId()));
            return;
        }
        FileService.ChunkResult result = FileService.getInstance().writeChunk(
                user.getId(), req.getFileId(), req.getChunkIndex(), data);
        client.sendMessage(JsonUtil.wrap(MessageType.FILE_UPLOAD_CHUNK_ACK,
                new FileChunkAckResponse(result.success(), result.message(),
                        req.getFileId(), req.getChunkIndex(), result.bytesReceived()),
                message.getRequestId()));
    }
}
