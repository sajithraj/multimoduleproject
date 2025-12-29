package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility to deserialize LinkedHashMap to AWS event types.
 * When Lambda uses RequestHandler<Object, Object>, it deserializes JSON to LinkedHashMap.
 * This utility converts those maps back to typed AWS events.
 */
public final class EventDeserializer {

    private static final Logger log = LogManager.getLogger(EventDeserializer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private EventDeserializer() {
        // Utility class
    }

    /**
     * Convert Object (typically LinkedHashMap) to APIGatewayProxyRequestEvent.
     */
    public static APIGatewayProxyRequestEvent toApiGatewayEvent(Object input) {
        try {
            log.debug("Deserializing to APIGatewayProxyRequestEvent");
            return MAPPER.convertValue(input, APIGatewayProxyRequestEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize API Gateway event", e);
            throw new IllegalArgumentException("Failed to deserialize API Gateway event: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Object (typically LinkedHashMap) to SQSEvent.
     */
    public static SQSEvent toSqsEvent(Object input) {
        try {
            log.debug("Deserializing to SQSEvent");
            return MAPPER.convertValue(input, SQSEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize SQS event", e);
            throw new IllegalArgumentException("Failed to deserialize SQS event: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Object (typically LinkedHashMap) to ScheduledEvent.
     */
    public static ScheduledEvent toScheduledEvent(Object input) {
        try {
            log.debug("Deserializing to ScheduledEvent");
            return MAPPER.convertValue(input, ScheduledEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize EventBridge event", e);
            throw new IllegalArgumentException("Failed to deserialize EventBridge event: " + e.getMessage(), e);
        }
    }
}

