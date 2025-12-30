package com.project.task.router;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.*;
import com.project.task.handler.EventBridgeHandler;
import com.project.task.model.InvocationType;
import com.project.task.service.ApiGatewayTaskService;
import com.project.task.service.EventBridgeTaskService;
import com.project.task.service.SQSTaskService;
import com.project.task.util.EventDeserializer;
import com.project.task.util.InvocationTypeDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Routes Lambda invocations to appropriate handlers based on event source type.
 * Supports API Gateway (multiple endpoints), SQS, and EventBridge (scheduled + custom events).
 */
public class EventRouter {

    private static final Logger log = LogManager.getLogger(EventRouter.class);
    private static final ApiGatewayTaskService API_SERVICE = new ApiGatewayTaskService();
    private static final SQSTaskService SQS_SERVICE = new SQSTaskService();
    private static final EventBridgeTaskService EB_SERVICE = new EventBridgeTaskService();
    private static final ApiGatewayRouter API_ROUTER = new ApiGatewayRouter(API_SERVICE);
    private static final EventBridgeHandler EB_HANDLER = new EventBridgeHandler(EB_SERVICE);

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
     * Supports multiple endpoints: /ping, /task, /task/{id}
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

        // Safety check for null records
        if (event.getRecords() == null) {
            log.error("SQS Event has null Records. This should not happen.");
            throw new IllegalArgumentException("Invalid SQS Event: Records field is null");
        }

        int messageCount = event.getRecords().size();
        log.info("Handling SQS event with {} messages", messageCount);

        // Delegate to SQS service for batch processing
        return SQS_SERVICE.processSQSMessages(event, context);
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

