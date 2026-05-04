package com.lqc.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class JsonUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (value, type, context) ->
                    value == null ? null : context.serialize(value.format(DATE_TIME_FORMATTER)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                    json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER))
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
