package com.lqc.server.handler;

import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.response.ErrorResponse;
import com.lqc.common.util.JsonUtil;
import com.lqc.server.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class RequestDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);
    private static final Map<MessageType, RequestHandler> handlers = new EnumMap<>(MessageType.class);

    static {
        handlers.put(MessageType.LOGIN_REQUEST, new LoginHandler());
        handlers.put(MessageType.REGISTER_REQUEST, new RegisterHandler());

        handlers.put(MessageType.CREATE_ROOM_REQUEST, new CreateRoomHandler());
        handlers.put(MessageType.JOIN_ROOM_REQUEST, new JoinRoomHandler());
        handlers.put(MessageType.LEAVE_ROOM_REQUEST, new LeaveRoomHandler());
        handlers.put(MessageType.INVITE_TO_ROOM_REQUEST, new InviteToRoomHandler());
        handlers.put(MessageType.REMOVE_MEMBER_REQUEST, new RemoveMemberHandler());
        handlers.put(MessageType.LIST_ROOMS_REQUEST, new ListRoomsHandler());
        handlers.put(MessageType.LIST_PUBLIC_ROOMS_REQUEST, new ListPublicRoomsHandler());
        handlers.put(MessageType.ROOM_MEMBERS_REQUEST, new RoomMembersHandler());
        handlers.put(MessageType.LIST_USERS_REQUEST, new ListUsersHandler());

        handlers.put(MessageType.SEND_MESSAGE_REQUEST, new SendMessageHandler());
        handlers.put(MessageType.PRIVATE_MESSAGE_REQUEST, new PrivateMessageHandler());
        handlers.put(MessageType.GET_HISTORY_REQUEST, new GetHistoryHandler());
        handlers.put(MessageType.DM_HISTORY_REQUEST, new DmHistoryHandler());
        handlers.put(MessageType.TYPING_REQUEST, new TypingHandler());

        handlers.put(MessageType.FILE_UPLOAD_START, new FileUploadStartHandler());
        handlers.put(MessageType.FILE_UPLOAD_CHUNK, new FileUploadChunkHandler());
        handlers.put(MessageType.FILE_UPLOAD_COMPLETE, new FileUploadCompleteHandler());
        handlers.put(MessageType.FILE_DOWNLOAD_REQUEST, new FileDownloadHandler());

        handlers.put(MessageType.ADD_REACTION_REQUEST, new AddReactionHandler());
        handlers.put(MessageType.REMOVE_REACTION_REQUEST, new RemoveReactionHandler());

        handlers.put(MessageType.STATUS_UPDATE_REQUEST, new StatusUpdateHandler());

        handlers.put(MessageType.UPDATE_PROFILE_REQUEST, new UpdateProfileHandler());
        handlers.put(MessageType.AVATAR_REQUEST, new AvatarHandler());
    }

    public static void dispatch(ClientHandler client, ProtocolMessage message) {
        RequestHandler handler = handlers.get(message.getType());
        if (handler == null) {
            logger.warn("No handler for message type: {}", message.getType());
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Unknown message type: " + message.getType())));
            return;
        }

        // All handlers except LOGIN and REGISTER require authentication
        if (client.getAuthenticatedUser() == null
                && message.getType() != MessageType.LOGIN_REQUEST
                && message.getType() != MessageType.REGISTER_REQUEST) {
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Not authenticated")));
            return;
        }

        try {
            handler.handle(client, message);
        } catch (Exception e) {
            logger.error("Error handling {}", message.getType(), e);
            client.sendMessage(JsonUtil.wrap(MessageType.ERROR_RESPONSE,
                    new ErrorResponse("Internal server error")));
        }
    }
}
