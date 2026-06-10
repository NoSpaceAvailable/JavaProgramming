package com.micord.server.handler;

import com.micord.common.model.User;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.ProfileUpdatedNotification;
import com.micord.common.protocol.request.UpdateProfileRequest;
import com.micord.common.protocol.response.UpdateProfileResponse;
import com.micord.common.util.JsonUtil;
import com.micord.server.ClientHandler;
import com.micord.server.manager.SessionManager;
import com.micord.server.repository.UserRepository;
import com.micord.server.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class UpdateProfileHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateProfileHandler.class);
    private static final long MAX_AVATAR_BYTES = 3 * 1024 * 1024;
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void handle(ClientHandler client, ProtocolMessage message) {
        UpdateProfileRequest req = JsonUtil.fromJson(message.getPayload(), UpdateProfileRequest.class);
        User user = client.getAuthenticatedUser();

        String displayName = req.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = user.getDisplayName();
        } else {
            displayName = displayName.trim();
            if (displayName.length() > 100) {
                client.sendMessage(JsonUtil.wrap(MessageType.UPDATE_PROFILE_RESPONSE,
                        new UpdateProfileResponse(false, "Display name too long"), message.getRequestId()));
                return;
            }
        }

        String avatarUrl = user.getAvatarUrl();
        boolean avatarUpdated = false;
        if (req.getAvatarData() != null && !req.getAvatarData().isEmpty()) {
            try {
                byte[] data = Base64.getDecoder().decode(req.getAvatarData());
                if (data.length > MAX_AVATAR_BYTES) {
                    client.sendMessage(JsonUtil.wrap(MessageType.UPDATE_PROFILE_RESPONSE,
                            new UpdateProfileResponse(false, "Avatar too large (max 3 MB)"), message.getRequestId()));
                    return;
                }
                String ext = "png";
                if (req.getAvatarMimeType() != null) {
                    if (req.getAvatarMimeType().contains("jpeg") || req.getAvatarMimeType().contains("jpg")) ext = "jpg";
                    else if (req.getAvatarMimeType().contains("gif")) ext = "gif";
                    else if (req.getAvatarMimeType().contains("webp")) ext = "webp";
                }
                String storagePath = ConfigUtil.get("file.storage.path", "./uploads");
                Path avatarDir = Path.of(storagePath).toAbsolutePath().normalize().resolve("avatars");
                Files.createDirectories(avatarDir);
                Path avatarFile = avatarDir.resolve(user.getId() + "." + ext);
                Files.write(avatarFile, data);
                avatarUrl = avatarFile.toString();
                avatarUpdated = true;
            } catch (IllegalArgumentException e) {
                client.sendMessage(JsonUtil.wrap(MessageType.UPDATE_PROFILE_RESPONSE,
                        new UpdateProfileResponse(false, "Invalid avatar data"), message.getRequestId()));
                return;
            } catch (IOException e) {
                logger.error("Failed to save avatar for user {}", user.getId(), e);
                client.sendMessage(JsonUtil.wrap(MessageType.UPDATE_PROFILE_RESPONSE,
                        new UpdateProfileResponse(false, "Failed to save avatar"), message.getRequestId()));
                return;
            }
        }

        boolean updated = userRepository.updateProfile(user.getId(), displayName, avatarUrl);
        if (updated) {
            user.setDisplayName(displayName);
            user.setAvatarUrl(avatarUrl);
            client.sendMessage(JsonUtil.wrap(MessageType.UPDATE_PROFILE_RESPONSE,
                    new UpdateProfileResponse(true, displayName, avatarUrl), message.getRequestId()));
            SessionManager.getInstance().broadcastToAll(
                    JsonUtil.wrap(MessageType.PROFILE_UPDATED_NOTIFICATION,
                            new ProfileUpdatedNotification(user.getId(), displayName, avatarUpdated)),
                    user.getId());
            logger.info("User {} updated profile", user.getUsername());
        } else {
            client.sendMessage(JsonUtil.wrap(MessageType.UPDATE_PROFILE_RESPONSE,
                    new UpdateProfileResponse(false, "Update failed"), message.getRequestId()));
        }
    }
}
