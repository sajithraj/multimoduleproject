package com.project.task.router;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.project.task.model.InvocationType;
import com.project.task.service.TaskService;
import com.project.task.util.InvocationTypeDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Routes Lambda invocations to appropriate handlers based on event source type.
 */
public class EventRouter {

    private static final Logger log = LogManager.getLogger(EventRouter.class);
    private static final TaskService SERVICE = new TaskService();

    /**
     * Route the event to the appropriate handler.
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
            case API_GATEWAY -> handleApiGateway((APIGatewayProxyRequestEvent) input, context);
            case SQS -> handleSqs((SQSEvent) input, context);
            case EVENT_BRIDGE -> handleEventBridge((ScheduledEvent) input, context);
        };
    }

    /**
     * Handle API Gateway events.
     */
    private APIGatewayProxyResponseEvent handleApiGateway(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Handling API Gateway request: method={}, path={}",
                event.getHttpMethod(), event.getPath());

        return SERVICE.processApiRequest(event, context);
    }

    /**
     * Handle SQS events.
     */
    private String handleSqs(SQSEvent event, Context context) {
        int messageCount = event.getRecords().size();
        log.info("Handling SQS event with {} messages", messageCount);

        event.getRecords().forEach(message -> {
            log.debug("Processing SQS message: messageId={}", message.getMessageId());
            SERVICE.processSqsMessage(message, context);
        });

        log.info("Successfully processed {} SQS messages", messageCount);

        // IMPORTANT: Return success to avoid message re-drive
        return "OK";
    }

    /**
     * Handle EventBridge events.
     */
    private String handleEventBridge(ScheduledEvent event, Context context) {
        log.info("Handling EventBridge event: source={}, detailType={}",
                event.getSource(), event.getDetailType());

        SERVICE.processEventBridgeEvent(event, context);

        log.info("Successfully processed EventBridge event");
        return "OK";
    }
}

