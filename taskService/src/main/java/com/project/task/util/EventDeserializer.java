package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class EventDeserializer {

    private static final Logger log = LogManager.getLogger(EventDeserializer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JodaModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final ObjectMapper SQS_MAPPER = createSqsMapper();
    private static final ObjectMapper EVENTBRIDGE_MAPPER = createEventBridgeMapper();

    private static ObjectMapper createSqsMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Register MixIn ONLY for SQS deserialization
        mapper.addMixIn(SQSEvent.class, SQSEventMixIn.class);

        return mapper;
    }

    private static ObjectMapper createEventBridgeMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Register MixIn for EventBridge/ScheduledEvent deserialization
        mapper.addMixIn(ScheduledEvent.class, ScheduledEventMixIn.class);

        return mapper;
    }

    private interface SQSEventMixIn {
        @JsonProperty("Records")
        List<SQSEvent.SQSMessage> getRecords();

        @JsonProperty("Records")
        @JsonAlias("records")
            // Accept lowercase "records" as alias during deserialization
        void setRecords(List<SQSEvent.SQSMessage> records);
    }

    private interface ScheduledEventMixIn {
        @JsonProperty("detail-type")
        String getDetailType();

        @JsonProperty("detail-type")
        void setDetailType(String detailType);
    }

    private EventDeserializer() {
    }

    public static APIGatewayProxyRequestEvent toApiGatewayEvent(Object input) {
        try {
            log.debug("Deserializing to APIGatewayProxyRequestEvent");
            return MAPPER.convertValue(input, APIGatewayProxyRequestEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize API Gateway event", e);
            throw new IllegalArgumentException("Failed to deserialize API Gateway event: " + e.getMessage(), e);
        }
    }

    public static SQSEvent toSqsEvent(Object input) {
        try {
            log.debug("Deserializing to SQSEvent from input type: {}",
                    input != null ? input.getClass().getSimpleName() : "null");

            SQSEvent event = SQS_MAPPER.convertValue(input, SQSEvent.class);

            if (event.getRecords() == null) {
                log.error("SQS Event records field is null after deserialization");
                throw new IllegalArgumentException("SQS Event deserialization failed: records field is null");
            }

            log.debug("Successfully deserialized SQSEvent with {} records", event.getRecords().size());
            return event;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to deserialize SQS event. Input type: {}, Error: {}",
                    input != null ? input.getClass().getName() : "null", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to deserialize SQS event: " + e.getMessage(), e);
        }
    }

    public static ScheduledEvent toScheduledEvent(Object input) {
        try {
            log.debug("Deserializing to ScheduledEvent");
            ScheduledEvent event = EVENTBRIDGE_MAPPER.convertValue(input, ScheduledEvent.class);
            log.debug("ScheduledEvent deserialized: source={}, detailType={}", event.getSource(), event.getDetailType());
            return event;
        } catch (Exception e) {
            log.error("Failed to deserialize EventBridge event", e);
            throw new IllegalArgumentException("Failed to deserialize EventBridge event: " + e.getMessage(), e);
        }
    }

}
