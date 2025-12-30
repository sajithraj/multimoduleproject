package com.project.task.util;

import com.project.task.model.EventDetectionResult;
import com.project.task.model.InvocationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public final class InvocationTypeDetector {

    private static final Logger log = LogManager.getLogger(InvocationTypeDetector.class);

    private InvocationTypeDetector() {
    }

    public static EventDetectionResult detectAndDeserialize(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("Input event cannot be null");
        }

        // AWS Lambda always sends input as Map (LinkedHashMap)
        if (input instanceof Map) {
            return detectAndDeserializeFromMap((Map<?, ?>) input);
        }

        // This should never happen in practice, but handle it for completeness
        String eventClass = input.getClass().getName();
        log.error("Unexpected non-Map event type: {}", eventClass);
        throw new IllegalArgumentException("Unsupported event type: " + eventClass);
    }

//    public static InvocationType detect(Object input) {
//        if (input == null) {
//            throw new IllegalArgumentException("Input event cannot be null");
//        }
//
//        // AWS Lambda always sends input as Map (LinkedHashMap)
//        if (input instanceof Map) {
//            return detectFromMap((Map<?, ?>) input);
//        }
//
//        // This should never happen in practice, but handle it for completeness
//        String eventClass = input.getClass().getName();
//        log.error("Unexpected non-Map event type: {}", eventClass);
//        throw new IllegalArgumentException("Unsupported event type: " + eventClass);
//    }

    private static EventDetectionResult detectAndDeserializeFromMap(Map<?, ?> eventMap) {
        // Check for API Gateway event structure
        if (eventMap.containsKey("httpMethod") && eventMap.containsKey("resource")) {
            log.debug("Detected API Gateway invocation - deserializing");
            return new EventDetectionResult(
                    InvocationType.API_GATEWAY,
                    EventDeserializer.toApiGatewayEvent(eventMap)
            );
        }

        // Check for EventBridge/Scheduled event structure
        if (eventMap.containsKey("source") && eventMap.containsKey("detail-type")) {
            String source = (String) eventMap.get("source");
            String detailType = (String) eventMap.get("detail-type");
            if (source == null || detailType == null) {
                log.error("EventBridge event missing source or detail-type");
                throw new IllegalArgumentException("EventBridge event missing source or detail-type");
            }
            InvocationType type = detectEventBridgeType(source, detailType);
            log.debug("Detected {} - deserializing", type.getDisplayName());
            return new EventDetectionResult(
                    type,
                    EventDeserializer.toScheduledEvent(eventMap)
            );
        }

        // Check for SQS event structure (Records can be lowercase when converted via ObjectMapper)
        Object records = eventMap.get("Records");
        if (records == null) {
            records = eventMap.get("records");  // Check lowercase too
        }

        if (records instanceof java.util.List) {
            if (((java.util.List<?>) records).isEmpty()) {
                log.error("SQS event Records list is empty");
                throw new IllegalArgumentException("Invalid SQS Event: Records list is empty");
            }
            Object firstRecord = ((java.util.List<?>) records).getFirst();
            if (firstRecord instanceof Map<?, ?> recordMap) {

                // SQS has eventSource = "aws:sqs"
                if ("aws:sqs".equals(recordMap.get("eventSource"))) {
                    log.debug("Detected SQS invocation - deserializing");
                    return new EventDetectionResult(
                            InvocationType.SQS,
                            EventDeserializer.toSqsEvent(eventMap)
                    );
                }
            }
        }


        // If we can't determine the type, log the keys for debugging
        log.error("Unable to determine event type from Map. Keys present: {}", eventMap.keySet());
        throw new IllegalArgumentException("Unsupported event structure. Keys: " + eventMap.keySet());
    }

//    private static InvocationType detectFromMap(Map<?, ?> eventMap) {
//        // Check for API Gateway event structure
//        if (eventMap.containsKey("httpMethod") && eventMap.containsKey("resource")) {
//            log.debug("Detected API Gateway invocation (from Map structure)");
//            return InvocationType.API_GATEWAY;
//        }
//
//        // Check for EventBridge/Scheduled event structure
//        if (eventMap.containsKey("source") && eventMap.containsKey("detail-type")) {
//            String source = (String) eventMap.get("source");
//            String detailType = (String) eventMap.get("detail-type");
//            if (source == null || detailType == null) {
//                log.error("EventBridge event missing source or detail-type");
//                throw new IllegalArgumentException("EventBridge event missing source or detail-type");
//            }
//            return detectEventBridgeType(source, detailType);
//        }
//
//        // Check for SQS event structure (Records can be lowercase when converted via ObjectMapper)
//        Object records = eventMap.get("Records");
//        if (records == null) {
//            records = eventMap.get("records");  // Check lowercase too
//        }
//
//        if (records instanceof java.util.List) {
//            if (((java.util.List<?>) records).isEmpty()) {
//                log.error("SQS event Records list is empty");
//                throw new IllegalArgumentException("Invalid SQS Event: Records list is empty");
//            }
//            Object firstRecord = ((java.util.List<?>) records).getFirst();
//            if (firstRecord instanceof Map<?, ?> recordMap) {
//
//                // SQS has eventSource = "aws:sqs"
//                if ("aws:sqs".equals(recordMap.get("eventSource"))) {
//                    log.debug("Detected SQS invocation (from Map structure)");
//                    return InvocationType.SQS;
//                }
//            }
//        }
//
//
//
//        // If we can't determine the type, log the keys for debugging
//        log.error("Unable to determine event type from Map. Keys present: {}", eventMap.keySet());
//        throw new IllegalArgumentException("Unsupported event structure. Keys: " + eventMap.keySet());
//    }

    private static InvocationType detectEventBridgeType(String source, String detailType) {
        if ("aws.events".equals(source) && "Scheduled Event".equals(detailType)) {
            log.debug("Detected EventBridge Scheduled Event (CloudWatch Events)");
            return InvocationType.EVENT_BRIDGE_SCHEDULED;
        }

        if (source.startsWith("com.custom") && detailType.startsWith("custom-event")) {
            log.debug("Detected EventBridge Custom Event");
            return InvocationType.EVENT_BRIDGE_CUSTOM;
        }

        log.error("Unable to determine event bridge based on the source {} & detail type {} .", source, detailType);
        throw new IllegalArgumentException("Unable to determine event bridge based on the source " + source + " & detail type " + detailType);
    }

}
