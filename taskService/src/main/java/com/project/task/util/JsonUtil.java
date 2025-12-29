package com.project.task.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JSON utility for serialization and deserialization.
 */
public final class JsonUtil {

    private static final Logger log = LogManager.getLogger(JsonUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
    }

    private JsonUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert object to JSON string.
     *
     * @param obj The object to serialize
     * @return JSON string
     * @throws RuntimeException if serialization fails
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Parse JSON string to object.
     *
     * @param json  The JSON string
     * @param clazz The target class
     * @param <T>   The type
     * @return Deserialized object
     * @throws RuntimeException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getName(), e.getMessage());
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * Get the ObjectMapper instance.
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}

