package com.project.task.router;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.*;
import com.project.task.model.EventDetectionResult;
import com.project.task.model.EventbridgeInvocationType;
import com.project.task.model.InvocationType;
import com.project.task.service.ApiGatewayTaskService;
import com.project.task.service.ApiGatewayTaskServiceHandler;
import com.project.task.service.EventBridgeTaskService;
import com.project.task.service.SQSTaskService;
import com.project.task.util.EventbridgeInvocationTypeDetector;
import com.project.task.util.InvocationTypeDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnifiedEventRouter {

    private static final Logger log = LogManager.getLogger(UnifiedEventRouter.class);
    private static final ApiGatewayTaskService API_SERVICE = new ApiGatewayTaskService();
    private static final SQSTaskService SQS_SERVICE = new SQSTaskService();
    private static final EventBridgeTaskService EB_SERVICE = new EventBridgeTaskService();
    private static final ApiGatewayTaskServiceHandler API_ROUTER = new ApiGatewayTaskServiceHandler(API_SERVICE);

    public Object route(Object input, Context context) {
        log.info("Routing event to appropriate handler");

        EventDetectionResult result = InvocationTypeDetector.detectAndDeserialize(input);
        InvocationType type = result.invocationType();
        Object event = result.deserializedEvent();

        log.info("Invocation type detected: {}", type.getDisplayName());

        return switch (type) {
            case API_GATEWAY -> handleApiGateway((APIGatewayProxyRequestEvent) event, context);
            case SQS -> handleSqs((SQSEvent) event, context);
            case EVENT_BRIDGE -> handleEventBridge((ScheduledEvent) event, context);
        };
    }

    private APIGatewayProxyResponseEvent handleApiGateway(APIGatewayProxyRequestEvent event, Context context) {
        log.info("Handling API Gateway request: method={}, path={}",
                event.getHttpMethod(), event.getPath());

        return API_ROUTER.route(event, context);
    }

    private SQSBatchResponse handleSqs(SQSEvent event, Context context) {
        if (event.getRecords() == null) {
            log.error("SQS Event has null Records. This should not happen.");
            throw new IllegalArgumentException("Invalid SQS Event: Records field is null");
        }

        int messageCount = event.getRecords().size();
        log.info("Handling SQS event with {} messages", messageCount);

        return SQS_SERVICE.processSQSMessages(event, context);
    }

    private String handleEventBridge(ScheduledEvent event, Context context) {
        log.info("Handling EventBridge Scheduled Event: source={}, detailType={}",
                event.getSource(), event.getDetailType());
        EventbridgeInvocationType type = EventbridgeInvocationTypeDetector.detectEventBridgeType(event.getSource(), event.getDetailType());
        return switch (type) {
            case EVENT_BRIDGE_SCHEDULED_EVENT -> EB_SERVICE.processScheduledEvent(event, context);
            case EVENT_BRIDGE_CUSTOM_EVENT -> EB_SERVICE.processCustomEvent(event, context);
            case EVENT_BRIDGE_S3_EVENT -> EB_SERVICE.processS3Event(event, context);
        };
    }

}
