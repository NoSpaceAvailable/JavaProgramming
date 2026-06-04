package com.micord.server.service;

import com.micord.common.model.Room;
import com.micord.common.model.User;
import com.micord.server.repository.RoomRepository;
import com.micord.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private final RoomRepository roomRepository = new RoomRepository();
    private final UserRepository userRepository = new UserRepository();

    public record CreateResult(boolean success, String message, Room room) {}
    public record JoinResult(boolean success, String message, Room room, boolean alreadyMember) {}
    public record LeaveResult(boolean success, String message) {}
    public record InviteResult(boolean success, String message, Room room, User target, boolean alreadyMember) {}
    public record RemoveResult(boolean success, String message, Room room, User target) {}

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

    /**
     * Adds {@code targetUserId} to a room on behalf of {@code inviterId}.
     * The inviter must already be a member; this is the only way to get into a
     * private room. Bypasses the private-room check that {@link #join} enforces.
     */
    public InviteResult invite(long roomId, long inviterId, long targetUserId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return new InviteResult(false, "Room not found", null, null, false);
        }
        if (!roomRepository.isMember(roomId, inviterId)) {
            return new InviteResult(false, "Only room members can invite others", null, null, false);
        }
        Optional<User> targetOpt = userRepository.findById(targetUserId);
        if (targetOpt.isEmpty()) {
            return new InviteResult(false, "User not found", null, null, false);
        }
        Room room = roomOpt.get();
        User target = targetOpt.get();
        if (roomRepository.isMember(roomId, targetUserId)) {
            return new InviteResult(true, target.getDisplayName() + " is already a member", room, target, true);
        }
        roomRepository.addMember(roomId, targetUserId, "MEMBER");
        logger.info("User {} invited user {} to room {}", inviterId, targetUserId, roomId);
        return new InviteResult(true, "Added " + target.getDisplayName(), room, target, false);
    }

    /**
     * Removes (kicks) {@code targetUserId} from a room. Only the room owner may
     * do this, and the owner cannot remove themselves (they should delete/leave instead).
     */
    public RemoveResult removeMember(long roomId, long requesterId, long targetUserId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return new RemoveResult(false, "Room not found", null, null);
        }
        Room room = roomOpt.get();
        if (room.getOwnerId() != requesterId) {
            return new RemoveResult(false, "Only the room owner can remove members", null, null);
        }
        if (targetUserId == requesterId) {
            return new RemoveResult(false, "The owner cannot remove themselves", null, null);
        }
        Optional<User> targetOpt = userRepository.findById(targetUserId);
        if (targetOpt.isEmpty()) {
            return new RemoveResult(false, "User not found", null, null);
        }
        if (!roomRepository.isMember(roomId, targetUserId)) {
            return new RemoveResult(false, "User is not a member of this room", null, null);
        }
        roomRepository.removeMember(roomId, targetUserId);
        logger.info("Owner {} removed user {} from room {}", requesterId, targetUserId, roomId);
        return new RemoveResult(true, "Removed " + targetOpt.get().getDisplayName(), room, targetOpt.get());
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
