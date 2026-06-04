package com.micord.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> {
                        try {
                            return LocalDateTime.parse(json.getAsString());
                        } catch (DateTimeParseException e) {
                            throw new JsonParseException("Cannot parse LocalDateTime: " + json, e);
                        }
                    })
            .create();

    private JsonUtil() {}

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static ProtocolMessage wrap(MessageType type, Object payload) {
        return new ProtocolMessage(
                type,
                toJson(payload),
                UUID.randomUUID().toString(),
                System.currentTimeMillis()
        );
    }

    public static ProtocolMessage wrap(MessageType type, Object payload, String requestId) {
        return new ProtocolMessage(
                type,
                toJson(payload),
                requestId,
                System.currentTimeMillis()
        );
    }
}
