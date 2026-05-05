package com.lqc.server.service;

import com.lqc.common.model.Room;
import com.lqc.server.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private final RoomRepository roomRepository = new RoomRepository();

    public record CreateResult(boolean success, String message, Room room) {}
    public record JoinResult(boolean success, String message, Room room, boolean alreadyMember) {}
    public record LeaveResult(boolean success, String message) {}

    public CreateResult create(String name, String description, long ownerId, boolean isPrivate) {
        if (name == null || name.trim().length() < 2) {
            return new CreateResult(false, "Room name must be at least 2 characters", null);
        }
        if (name.trim().length() > 100) {
            return new CreateResult(false, "Room name too long (max 100)", null);
        }
        String trimmed = name.trim();
        if (roomRepository.findByName(trimmed).isPresent()) {
            return new CreateResult(false, "A room with that name already exists", null);
        }
        Room room = roomRepository.create(trimmed,
                description == null ? null : description.trim(),
                ownerId, isPrivate);
        logger.info("Room created: {} by user {}", room.getName(), ownerId);
        return new CreateResult(true, "Room created", room);
    }

    public JoinResult join(long roomId, long userId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return new JoinResult(false, "Room not found", null, false);
        }
        Room room = roomOpt.get();
        if (room.isPrivate() && room.getOwnerId() != userId) {
            return new JoinResult(false, "Cannot join a private room without an invite", null, false);
        }
        if (roomRepository.isMember(roomId, userId)) {
            return new JoinResult(true, "Already a member", room, true);
        }
        roomRepository.addMember(roomId, userId, "MEMBER");
        return new JoinResult(true, "Joined", room, false);
    }

    public LeaveResult leave(long roomId, long userId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return new LeaveResult(false, "Room not found");
        }
        if (!roomRepository.isMember(roomId, userId)) {
            return new LeaveResult(false, "Not a member of this room");
        }
        roomRepository.removeMember(roomId, userId);
        return new LeaveResult(true, "Left room");
    }
}
