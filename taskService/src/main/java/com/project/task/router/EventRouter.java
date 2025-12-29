package com.project.task.router;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.project.task.handler.EventBridgeHandler;
import com.project.task.model.InvocationType;
import com.project.task.service.TaskService;
import com.project.task.util.EventDeserializer;
import com.project.task.util.InvocationTypeDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes Lambda invocations to appropriate handlers based on event source type.
 * Supports API Gateway (multiple endpoints), SQS, and EventBridge (scheduled + custom events).
 */
public class EventRouter {

    private static final Logger log = LogManager.getLogger(EventRouter.class);
    private static final TaskService SERVICE = new TaskService();
    private static final ApiGatewayRouter API_ROUTER = new ApiGatewayRouter(SERVICE);
    private static final EventBridgeHandler EB_HANDLER = new EventBridgeHandler(SERVICE);

    /**
     * Route the event to the appropriate handler.
     * Handles both typed AWS events and LinkedHashMap (deserialized JSON).
     *
     * @param input   The Lambda input event
     * @param context Lambda context
     * @return Response object (type varies by event source)
     */
    public Object route(Object input, Context context) {
        log.info("Routing event to appropriate handler");

        InvocationType type = InvocationTypeDetector.detect(input);
        log.info("Invocation type detected: {}", type.getDisplayName());

        return switch (type) {
            case API_GATEWAY -> handleApiGateway(input, context);
            case SQS -> handleSqs(input, context);
            case EVENT_BRIDGE -> handleEventBridge(input, context);
        };
    }

    /**
     * Handle API Gateway events - routes to specific endpoint handlers.
     * Supports multiple endpoints: /ping, /get, /id/{id}, /post
     */
    private APIGatewayProxyResponseEvent handleApiGateway(Object input, Context context) {
        // Convert LinkedHashMap to typed AWS event
        APIGatewayProxyRequestEvent event = (input instanceof APIGatewayProxyRequestEvent)
                ? (APIGatewayProxyRequestEvent) input
                : EventDeserializer.toApiGatewayEvent(input);

        log.info("Handling API Gateway request: method={}, path={}",
                event.getHttpMethod(), event.getPath());

        return API_ROUTER.route(event, context);
    }

    /**
     * Handle SQS events with batch item failure handling.
     * Failed messages will be returned to SQS for retry/DLQ routing.
     *
     * @return SQSBatchResponse with failed message IDs (if any)
     */
    private SQSBatchResponse handleSqs(Object input, Context context) {
        // Convert LinkedHashMap to typed AWS event
        SQSEvent event = (input instanceof SQSEvent)
                ? (SQSEvent) input
                : EventDeserializer.toSqsEvent(input);

        int messageCount = event.getRecords().size();
        log.info("Handling SQS event with {} messages", messageCount);

        List<SQSBatchResponse.BatchItemFailure> batchItemFailures = new ArrayList<>();

        // Process each message and track failures
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            String messageId = message.getMessageId();
            log.debug("Processing SQS message: messageId={}", messageId);

            try {
                SERVICE.processSqsMessage(message, context);
                log.debug("Successfully processed message: messageId={}", messageId);

            } catch (Exception e) {
                log.error("Failed to process SQS message: messageId={}, error={}",
                        messageId, e.getMessage(), e);

                // Add to batch item failures - will be retried or sent to DLQ
                batchItemFailures.add(SQSBatchResponse.BatchItemFailure.builder()
                        .withItemIdentifier(messageId)
                        .build());
            }
        }

        int successCount = messageCount - batchItemFailures.size();
        int failureCount = batchItemFailures.size();

        log.info("SQS batch processing complete: total={}, success={}, failures={}",
                messageCount, successCount, failureCount);

        if (!batchItemFailures.isEmpty()) {
            log.warn("Returning {} failed message IDs to SQS for retry/DLQ", failureCount);
        }

        // Return batch response with failures (empty list if all succeeded)
        return SQSBatchResponse.builder()
                .withBatchItemFailures(batchItemFailures)
                .build();
    }

    /**
     * Handle EventBridge events - supports both scheduled tasks and custom events.
     * Automatically detects event type and routes appropriately.
     */
    private String handleEventBridge(Object input, Context context) {
        // Convert LinkedHashMap to typed AWS event
        ScheduledEvent event = (input instanceof ScheduledEvent)
                ? (ScheduledEvent) input
                : EventDeserializer.toScheduledEvent(input);

        log.info("Handling EventBridge event: source={}, detailType={}",
                event.getSource(), event.getDetailType());

        return EB_HANDLER.handle(event, context);
    }
}

