package com.micord.server.service;

import com.micord.common.model.Room;
import com.micord.common.model.Server;
import com.micord.common.model.User;
import com.micord.server.repository.AuditRepository;
import com.micord.server.repository.RoomRepository;
import com.micord.server.repository.ServerRepository;
import com.micord.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ServerService {
    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);
    private final ServerRepository serverRepository = new ServerRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final UserRepository userRepository = new UserRepository();
    private final AuditRepository auditRepository = new AuditRepository();

    public record CreateResult(boolean success, String message, Server server, Room defaultChannel) {}
    public record JoinResult(boolean success, String message, Server server, List<Room> channels) {}
    public record ChannelResult(boolean success, String message, Server server, Room channel) {}
    public record ModResult(boolean success, String message, User target) {}

    /** Role hierarchy rank; higher number = more authority. */
    public static int rank(String role) {
        if (role == null) return -1;
        return switch (role) {
            case "OWNER" -> 3;
            case "ADMIN" -> 2;
            case "MODERATOR" -> 1;
            case "MEMBER" -> 0;
            default -> -1;
        };
    }

    public CreateResult createServer(String name, long ownerId) {
        if (name == null || name.trim().length() < 2) {
            return new CreateResult(false, "Server name must be at least 2 characters", null, null);
        }
        if (name.trim().length() > 100) {
            return new CreateResult(false, "Server name too long (max 100)", null, null);
        }
        Server server = serverRepository.create(name.trim(), ownerId);
        serverRepository.addMember(server.getId(), ownerId, "OWNER");
        server.setMyRole("OWNER");

        // Every server starts with a default "general" text channel.
        Room channel = roomRepository.createChannel(server.getId(), "general", ownerId);
        roomRepository.addMember(channel.getId(), ownerId, "OWNER");

        auditRepository.log(server.getId(), ownerId, null, "CREATE_SERVER", "Created server " + server.getName());
        logger.info("User {} created server '{}' (id={})", ownerId, name, server.getId());
        return new CreateResult(true, "Server created", server, channel);
    }

    public JoinResult joinByInvite(String inviteCode, long userId) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return new JoinResult(false, "Invite code is empty", null, null);
        }
        Optional<Server> opt = serverRepository.findByInviteCode(inviteCode.trim());
        if (opt.isEmpty()) {
            return new JoinResult(false, "Invalid invite code", null, null);
        }
        Server server = opt.get();
        if (serverRepository.isBanned(server.getId(), userId)) {
            return new JoinResult(false, "You are banned from " + server.getName(), null, null);
        }
        boolean already = serverRepository.isMember(server.getId(), userId);
        if (!already) {
            serverRepository.addMember(server.getId(), userId, "MEMBER");
        }
        // Make sure the user is a member of every channel so existing room logic works.
        List<Room> channels = roomRepository.findChannelsByServerId(server.getId());
        for (Room ch : channels) {
            roomRepository.addMember(ch.getId(), userId, "MEMBER");
        }
        server.setMyRole(server.getOwnerId() == userId ? "OWNER" : "MEMBER");
        String msg = already ? "Already a member of " + server.getName() : "Joined " + server.getName();
        return new JoinResult(true, msg, server, channels);
    }

    public ChannelResult createChannel(long serverId, long userId, String name) {
        Optional<Server> opt = serverRepository.findById(serverId);
        if (opt.isEmpty()) {
            return new ChannelResult(false, "Server not found", null, null);
        }
        if (rank(serverRepository.getRole(serverId, userId)) < rank("ADMIN")) {
            return new ChannelResult(false, "Only the owner or admins can create channels", null, null);
        }
        if (name == null || name.trim().length() < 1) {
            return new ChannelResult(false, "Channel name is required", null, null);
        }
        String channelName = normalizeChannelName(name);
        Room channel = roomRepository.createChannel(serverId, channelName, opt.get().getOwnerId());
        // Add all current server members so they can see/post in the new channel.
        for (Long memberId : serverRepository.getMemberIds(serverId)) {
            roomRepository.addMember(channel.getId(), memberId, memberId == opt.get().getOwnerId() ? "OWNER" : "MEMBER");
        }
        User actor = userRepository.findById(userId).orElse(null);
        auditRepository.log(serverId, userId, actor != null ? actor.getDisplayName() : null,
                "CREATE_CHANNEL", "Created channel #" + channelName);
        return new ChannelResult(true, "Channel created", opt.get(), channel);
    }

    public List<Room> listChannels(long serverId, long userId) {
        if (!serverRepository.isMember(serverId, userId)) {
            return List.of();
        }
        return roomRepository.findChannelsByServerId(serverId);
    }

    public ModResult changeRole(long serverId, long actorId, long targetId, String newRole) {
        if (!"OWNER".equals(serverRepository.getRole(serverId, actorId))) {
            return new ModResult(false, "Only the server owner can change roles", null);
        }
        if (targetId == actorId) {
            return new ModResult(false, "You cannot change your own role", null);
        }
        if (!(newRole != null && (newRole.equals("ADMIN") || newRole.equals("MODERATOR") || newRole.equals("MEMBER")))) {
            return new ModResult(false, "Invalid role", null);
        }
        String targetRole = serverRepository.getRole(serverId, targetId);
        if (targetRole == null) {
            return new ModResult(false, "User is not a member of this server", null);
        }
        if ("OWNER".equals(targetRole)) {
            return new ModResult(false, "Cannot change the owner's role", null);
        }
        serverRepository.setRole(serverId, targetId, newRole);
        User target = userRepository.findById(targetId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);
        auditRepository.log(serverId, actorId, actor != null ? actor.getDisplayName() : null,
                "CHANGE_ROLE", (target != null ? target.getDisplayName() : "User " + targetId) + " → " + newRole);
        return new ModResult(true, "Role updated", target);
    }

    public ModResult kick(long serverId, long actorId, long targetId) {
        return removeFromServer(serverId, actorId, targetId, false, null, "MODERATOR");
    }

    public ModResult ban(long serverId, long actorId, long targetId, String reason) {
        return removeFromServer(serverId, actorId, targetId, true, reason, "ADMIN");
    }

    private ModResult removeFromServer(long serverId, long actorId, long targetId,
                                       boolean ban, String reason, String minRole) {
        String actorRole = serverRepository.getRole(serverId, actorId);
        if (rank(actorRole) < rank(minRole)) {
            return new ModResult(false, "You don't have permission to do that", null);
        }
        if (targetId == actorId) {
            return new ModResult(false, "You cannot remove yourself", null);
        }
        String targetRole = serverRepository.getRole(serverId, targetId);
        if (targetRole == null) {
            return new ModResult(false, "User is not a member of this server", null);
        }
        if (rank(targetRole) >= rank(actorRole)) {
            return new ModResult(false, "You can only act on members with a lower role", null);
        }
        serverRepository.removeMember(serverId, targetId);
        roomRepository.removeMemberFromServerChannels(serverId, targetId);
        User target = userRepository.findById(targetId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);
        String targetName = target != null ? target.getDisplayName() : "User " + targetId;
        if (ban) {
            serverRepository.ban(serverId, targetId, actorId, reason);
            auditRepository.log(serverId, actorId, actor != null ? actor.getDisplayName() : null,
                    "BAN", "Banned " + targetName + (reason != null && !reason.isBlank() ? " (" + reason + ")" : ""));
        } else {
            auditRepository.log(serverId, actorId, actor != null ? actor.getDisplayName() : null,
                    "KICK", "Kicked " + targetName);
        }
        return new ModResult(true, (ban ? "Banned " : "Kicked ") + targetName, target);
    }

    /** Discord-style channel names: lowercase, spaces -> hyphens. */
    private String normalizeChannelName(String raw) {
        String n = raw.trim().toLowerCase().replaceAll("\\s+", "-");
        return n.length() > 100 ? n.substring(0, 100) : n;
    }
}
