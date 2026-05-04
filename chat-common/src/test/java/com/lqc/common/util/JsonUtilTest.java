package com.lqc.common.util;

import com.lqc.common.model.Room;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilTest {
    @Test
    void serializesAndDeserializesLocalDateTime() {
        Room room = new Room(1L, "general", 2L);
        room.setCreatedAt(LocalDateTime.of(2026, 5, 4, 23, 41, 20));

        String json = JsonUtil.toJson(room);
        Room parsed = JsonUtil.fromJson(json, Room.class);

        assertEquals(room.getCreatedAt(), parsed.getCreatedAt());
    }
}
