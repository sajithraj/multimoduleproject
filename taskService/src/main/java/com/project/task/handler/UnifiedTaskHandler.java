package com.project.task.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.project.task.router.UnifiedEventRouter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import software.amazon.lambda.powertools.logging.Logging;

import java.util.UUID;

public class UnifiedTaskHandler implements RequestHandler<Object, Object> {

    private static final Logger log = LogManager.getLogger(UnifiedTaskHandler.class);
    private static final UnifiedEventRouter ROUTER = new UnifiedEventRouter();

    @Override
    @Logging(logEvent = true)
    public Object handleRequest(Object input, Context context) {
        String requestId = context != null ? context.getAwsRequestId() : UUID.randomUUID().toString();
        ThreadContext.put("requestId", requestId);
        ThreadContext.put("service", "TaskService");

        try {
            log.info("Lambda invoked: functionName={}, requestId={}, remainingTime={}ms",
                    context != null ? context.getFunctionName() : "unknown",
                    requestId,
                    context != null ? context.getRemainingTimeInMillis() : 0);

            // Log input event details for debugging
            if (input != null) {
                log.info("Input event type: {}", input.getClass().getName());
                log.debug("Input event content: {}", input);
            } else {
                log.warn("Input event is null");
            }

            // Route to appropriate handler
            Object response = ROUTER.route(input, context);

            log.info("Lambda execution completed successfully");
            return response;

        } catch (Exception e) {
            log.error("Lambda execution failed: {}", e.getMessage(), e);

            // Return structured error response instead of just throwing
            return createErrorResponse(e, context);

        } finally {
            ThreadContext.clearAll();
        }
    }

    private Object createErrorResponse(Exception e, Context context) {
        java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("errorMessage", "Task processing failed");
        errorResponse.put("errorType", e.getClass().getSimpleName());
        errorResponse.put("errorReason", e.getMessage());
        errorResponse.put("requestId", context != null ? context.getAwsRequestId() : "unknown");
        errorResponse.put("timestamp", System.currentTimeMillis());

        return errorResponse;
    }

}
