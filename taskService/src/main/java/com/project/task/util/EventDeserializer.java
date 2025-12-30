package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Utility to deserialize LinkedHashMap to AWS event types.
 * When Lambda uses RequestHandler<Object, Object>, it deserializes JSON to LinkedHashMap.
 * This utility converts those maps back to typed AWS events efficiently.
 * <p>
 * Performance Optimization:
 * - Uses Jackson MixIn for field name mapping (Records -> records)
 * - Direct Map → Object conversion (no intermediate JSON string serialization)
 * - Single pass deserialization
 */
public final class EventDeserializer {

    private static final Logger log = LogManager.getLogger(EventDeserializer.class);

    // General-purpose ObjectMapper (no SQS-specific configuration)
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JodaModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Dedicated ObjectMapper for SQS deserialization with MixIn
    // This prevents side effects on other serialization/deserialization operations
    private static final ObjectMapper SQS_MAPPER = createSqsMapper();

    private static ObjectMapper createSqsMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Register MixIn ONLY for SQS deserialization
        // This mapper instance is used exclusively for SQSEvent conversion
        mapper.addMixIn(SQSEvent.class, SQSEventMixIn.class);

        return mapper;
    }

    /**
     * MixIn interface to map "Records" (AWS JSON) to "records" (Java field)
     * This is used ONLY by SQS_MAPPER, not the general MAPPER
     */
    private interface SQSEventMixIn {
        @JsonProperty("Records")
        List<SQSEvent.SQSMessage> getRecords();

        @JsonProperty("Records")
        void setRecords(List<SQSEvent.SQSMessage> records);
    }

    private EventDeserializer() {
        // Utility class
    }

    /**
     * Convert Object (typically LinkedHashMap) to APIGatewayProxyRequestEvent.
     * Performance: Map → APIGatewayProxyRequestEvent (single conversion)
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
     * Uses dedicated SQS_MAPPER with MixIn to efficiently map "Records" -> "records".
     * <p>
     * Performance: Map → SQSEvent (single conversion, no intermediate JSON strings)
     * <p>
     * Note: Uses a separate ObjectMapper instance to avoid affecting other serialization operations.
     *
     * @param input LinkedHashMap from Lambda runtime containing SQS event data
     * @return Properly deserialized SQSEvent with Records field mapped
     * @throws IllegalArgumentException if deserialization fails or records is null
     */
    public static SQSEvent toSqsEvent(Object input) {
        try {
            log.debug("Deserializing to SQSEvent from input type: {}",
                    input != null ? input.getClass().getSimpleName() : "null");

            // Use dedicated SQS_MAPPER (with MixIn) for efficient conversion
            // This doesn't affect other operations that might serialize/deserialize SQSEvent
            SQSEvent event = SQS_MAPPER.convertValue(input, SQSEvent.class);

            // Validate
            if (event.getRecords() == null) {
                log.error("SQS Event records field is null after deserialization");
                throw new IllegalArgumentException("SQS Event deserialization failed: records field is null");
            }

            log.debug("Successfully deserialized SQSEvent with {} records", event.getRecords().size());
            return event;

        } catch (IllegalArgumentException e) {
            // Re-throw our validation exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to deserialize SQS event. Input type: {}, Error: {}",
                    input != null ? input.getClass().getName() : "null", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to deserialize SQS event: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Object (typically LinkedHashMap) to ScheduledEvent.
     * Performance: Map → ScheduledEvent (single conversion)
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

