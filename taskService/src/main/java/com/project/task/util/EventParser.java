package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.task.model.EventSourceType;
import com.project.task.model.TaskRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to parse different event types and convert them to TaskRequest.
 */
public final class EventParser {

    private static final Logger log = LogManager.getLogger(EventParser.class);

    private EventParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parse API Gateway event to TaskRequest.
     */
    public static TaskRequest parseApiGatewayEvent(APIGatewayProxyRequestEvent event) {
        log.info("Parsing API Gateway event");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("httpMethod", event.getHttpMethod());
        metadata.put("path", event.getPath());
        metadata.put("headers", event.getHeaders());
        metadata.put("queryStringParameters", event.getQueryStringParameters());
        metadata.put("pathParameters", event.getPathParameters());

        TaskRequest request = TaskRequest.builder()
                .sourceType(EventSourceType.API_GATEWAY)
                .eventId(event.getRequestContext() != null ?
                        event.getRequestContext().getRequestId() : "unknown")
                .requestBody(event.getBody())
                .metadata(metadata)
                .build();

        log.debug("Parsed API Gateway event: eventId={}, method={}, path={}",
                request.getEventId(), event.getHttpMethod(), event.getPath());

        return request;
    }

    /**
     * Parse SQS event to TaskRequest.
     * Note: Processes only the first message in the batch for simplicity.
     */
    public static TaskRequest parseSqsEvent(SQSEvent event) {
        log.info("Parsing SQS event with {} messages", event.getRecords().size());

        if (event.getRecords().isEmpty()) {
            throw new IllegalArgumentException("SQS event contains no messages");
        }

        SQSEvent.SQSMessage message = event.getRecords().get(0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("receiptHandle", message.getReceiptHandle());
        metadata.put("attributes", message.getAttributes());
        metadata.put("messageAttributes", message.getMessageAttributes());
        metadata.put("eventSourceARN", message.getEventSourceArn());
        metadata.put("totalMessages", event.getRecords().size());

        TaskRequest request = TaskRequest.builder()
                .sourceType(EventSourceType.SQS)
                .eventId(message.getMessageId())
                .requestBody(message.getBody())
                .metadata(metadata)
                .build();

        log.debug("Parsed SQS event: eventId={}, body={}",
                request.getEventId(), truncate(message.getBody(), 100));

        return request;
    }

    /**
     * Parse EventBridge event to TaskRequest.
     */
    public static TaskRequest parseEventBridgeEvent(ScheduledEvent event) {
        log.info("Parsing EventBridge event");

        String requestBody;
        try {
            requestBody = JsonUtil.toJson(event.getDetail());
        } catch (Exception e) {
            log.warn("Failed to serialize EventBridge detail: {}", e.getMessage());
            requestBody = event.getDetail() != null ? event.getDetail().toString() : "{}";
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", event.getSource());
        metadata.put("detailType", event.getDetailType());
        metadata.put("region", event.getRegion());
        metadata.put("account", event.getAccount());
        metadata.put("time", event.getTime());
        metadata.put("resources", event.getResources());

        TaskRequest request = TaskRequest.builder()
                .sourceType(EventSourceType.EVENT_BRIDGE)
                .eventId(event.getId())
                .requestBody(requestBody)
                .metadata(metadata)
                .build();

        log.debug("Parsed EventBridge event: eventId={}, source={}, detailType={}",
                request.getEventId(), event.getSource(), event.getDetailType());

        return request;
    }

    /**
     * Helper method to truncate long strings for logging.
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}

