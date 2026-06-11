package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.ViewAuditLogRequest;
import com.micord.common.protocol.response.AuditLogResponse;
import com.micord.common.protocol.response.ErrorResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.repository.AuditRepository;
import com.micord.server.repository.ServerRepository;
import com.micord.server.service.ServerService;

public class ViewAuditLogHandler implements RequestHandler {
    private final ServerRepository serverRepository = new ServerRepository();
    private final AuditRepository auditRepository = new AuditRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        User user = client.getAuthenticatedUser();
        ViewAuditLogRequest req = JsonUtil.fromJson(message.getPayload(), ViewAuditLogRequest.class);

        if (ServerService.rank(serverRepository.getRole(req.getServerId(), user.getId())) < ServerService.rank("ADMIN")) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Only the owner or admins can view the audit log"), message.getRequestId()));
            return;
        }
        client.sendMessage(JsonUtil.wrap(MessageType.VIEW_AUDIT_LOG_RESPONSE,
                new AuditLogResponse(req.getServerId(), auditRepository.findByServerId(req.getServerId(), 100)),
                message.getRequestId()));
    }
}
