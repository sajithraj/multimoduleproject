package com.project.task.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.project.task.router.EventRouter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import software.amazon.lambda.powertools.logging.Logging;

import java.util.UUID;

/**
 * Unified Lambda handler for API Gateway, SQS, and EventBridge events.
 * <p>
 * This handler acts as a thin entry point that:
 * 1. Sets up logging context
 * 2. Delegates routing to EventRouter
 * 3. Cleans up context after execution
 * <p>
 * The actual business logic is implemented in TaskService.
 */
public class UnifiedTaskHandler implements RequestHandler<Object, Object> {

    private static final Logger log = LogManager.getLogger(UnifiedTaskHandler.class);
    private static final EventRouter ROUTER = new EventRouter();

    /**
     * Handle Lambda invocation.
     *
     * @param input   The input event (API Gateway, SQS, or EventBridge)
     * @param context Lambda execution context
     * @return Response object (varies by event source)
     */
    @Override
    @Logging(logEvent = true)
    public Object handleRequest(Object input, Context context) {
        String requestId = context != null ? context.getAwsRequestId() : UUID.randomUUID().toString();
        ThreadContext.put("requestId", requestId);

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
            throw new RuntimeException("Task processing failed", e);

        } finally {
            ThreadContext.clearAll();
        }
    }
}

