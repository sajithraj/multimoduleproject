package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.project.task.model.InvocationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Utility to detect the type of Lambda invocation based on the input event.
 * Handles both typed AWS events and raw Map/LinkedHashMap from JSON deserialization.
 */
public final class InvocationTypeDetector {

    private static final Logger log = LogManager.getLogger(InvocationTypeDetector.class);

    private InvocationTypeDetector() {
        // Utility class - prevent instantiation
    }

    /**
     * Detect the invocation type from the input event.
     *
     * @param input The Lambda input event
     * @return The detected invocation type
     * @throws IllegalArgumentException if the event type is unsupported
     */
    public static InvocationType detect(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("Input event cannot be null");
        }

        // Handle typed AWS events (direct deserialization)
        if (input instanceof APIGatewayProxyRequestEvent) {
            log.debug("Detected API Gateway invocation (typed)");
            return InvocationType.API_GATEWAY;
        }

        if (input instanceof SQSEvent) {
            log.debug("Detected SQS invocation (typed)");
            return InvocationType.SQS;
        }

        if (input instanceof ScheduledEvent) {
            log.debug("Detected EventBridge invocation (typed)");
            return InvocationType.EVENT_BRIDGE;
        }

        // Handle Map/LinkedHashMap (JSON deserialization when handler uses Object)
        if (input instanceof Map) {
            return detectFromMap((Map<?, ?>) input);
        }

        String eventClass = input.getClass().getName();
        log.error("Unsupported event type: {}", eventClass);
        throw new IllegalArgumentException("Unsupported event type: " + eventClass);
    }

    /**
     * Detect event type from Map structure (when Lambda deserializes JSON to Map).
     */
    @SuppressWarnings("unchecked")
    private static InvocationType detectFromMap(Map<?, ?> eventMap) {
        // Check for API Gateway event structure
        if (eventMap.containsKey("httpMethod") && eventMap.containsKey("resource")) {
            log.debug("Detected API Gateway invocation (from Map structure)");
            return InvocationType.API_GATEWAY;
        }

        // Check for SQS event structure
        if (eventMap.containsKey("Records")) {
            Object records = eventMap.get("Records");
            if (records instanceof java.util.List && !((java.util.List<?>) records).isEmpty()) {
                Object firstRecord = ((java.util.List<?>) records).get(0);
                if (firstRecord instanceof Map) {
                    Map<?, ?> recordMap = (Map<?, ?>) firstRecord;

                    // SQS has eventSource = "aws:sqs"
                    if ("aws:sqs".equals(recordMap.get("eventSource"))) {
                        log.debug("Detected SQS invocation (from Map structure)");
                        return InvocationType.SQS;
                    }
                }
            }
        }

        // Check for EventBridge/Scheduled event structure
        if (eventMap.containsKey("source") && eventMap.containsKey("detail-type")) {
            log.debug("Detected EventBridge invocation (from Map structure)");
            return InvocationType.EVENT_BRIDGE;
        }

        // If we can't determine the type, log the keys for debugging
        log.error("Unable to determine event type from Map. Keys present: {}", eventMap.keySet());
        throw new IllegalArgumentException("Unsupported event structure. Keys: " + eventMap.keySet());
    }
}
