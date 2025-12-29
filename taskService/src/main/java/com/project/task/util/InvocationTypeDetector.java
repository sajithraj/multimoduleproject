package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.project.task.model.InvocationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility to detect the type of Lambda invocation based on the input event.
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

        if (input instanceof APIGatewayProxyRequestEvent) {
            log.debug("Detected API Gateway invocation");
            return InvocationType.API_GATEWAY;
        }

        if (input instanceof SQSEvent) {
            log.debug("Detected SQS invocation");
            return InvocationType.SQS;
        }

        if (input instanceof ScheduledEvent) {
            log.debug("Detected EventBridge invocation");
            return InvocationType.EVENT_BRIDGE;
        }

        String eventClass = input.getClass().getName();
        log.error("Unsupported event type: {}", eventClass);
        throw new IllegalArgumentException("Unsupported event type: " + eventClass);
    }
}

