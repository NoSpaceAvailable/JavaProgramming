package com.micord.server.service;

import com.micord.common.model.Room;
import com.micord.common.model.Server;
import com.micord.server.repository.RoomRepository;
import com.micord.server.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ServerService {
    private static final Logger logger = LoggerFactory.getLogger(ServerService.class);
    private final ServerRepository serverRepository = new ServerRepository();
    private final RoomRepository roomRepository = new RoomRepository();

    public record CreateResult(boolean success, String message, Server server, Room defaultChannel) {}
    public record JoinResult(boolean success, String message, Server server, List<Room> channels) {}
    public record ChannelResult(boolean success, String message, Server server, Room channel) {}

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
        if (!serverRepository.isMember(serverId, userId)) {
            return new ChannelResult(false, "You are not a member of this server", null, null);
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
        return new ChannelResult(true, "Channel created", opt.get(), channel);
    }

    public List<Room> listChannels(long serverId, long userId) {
        if (!serverRepository.isMember(serverId, userId)) {
            return List.of();
        }
        return roomRepository.findChannelsByServerId(serverId);
    }

    /** Discord-style channel names: lowercase, spaces -> hyphens. */
    private String normalizeChannelName(String raw) {
        String n = raw.trim().toLowerCase().replaceAll("\\s+", "-");
        return n.length() > 100 ? n.substring(0, 100) : n;
    }
}
