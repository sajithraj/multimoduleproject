package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.project.task.model.TaskRequest;
import com.project.task.model.TaskResponse;
import com.project.task.util.EventParser;
import com.project.task.util.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Business service that processes tasks from different event sources.
 * This is where you implement your actual business logic.
 */
public class TaskService {

    private static final Logger log = LogManager.getLogger(TaskService.class);

    /**
     * Process API Gateway request.
     *
     * @param event   API Gateway request event
     * @param context Lambda context
     * @return API Gateway response
     */
    public APIGatewayProxyResponseEvent processApiRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing API Gateway request");

        try {
            // Parse event to TaskRequest
            TaskRequest taskRequest = EventParser.parseApiGatewayEvent(event);
            logTaskRequest(taskRequest);

            // Validate input (placeholder)
            validateRequest(taskRequest);

            // Process business logic
            TaskResponse taskResponse = executeBusinessLogic(taskRequest, context);

            // Build success response
            return buildApiResponse(200, taskResponse);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return buildApiResponse(400, TaskResponse.builder()
                    .success(false)
                    .message("Validation error: " + e.getMessage())
                    .build());

        } catch (Exception e) {
            log.error("Error processing API request: {}", e.getMessage(), e);
            return buildApiResponse(500, TaskResponse.builder()
                    .success(false)
                    .message("Internal server error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Process SQS message.
     * IMPORTANT: Implement idempotency check to avoid duplicate processing.
     *
     * @param message SQS message
     * @param context Lambda context
     */
    public void processSqsMessage(SQSEvent.SQSMessage message, Context context) {
        log.info("Processing SQS message: messageId={}", message.getMessageId());

        try {
            // Parse message
            String messageBody = message.getBody();
            log.debug("Message body: {}", messageBody);

            // TODO: Implement idempotency check here
            // Example: Check if messageId was already processed in DynamoDB

            // Create TaskRequest from SQS message
            TaskRequest taskRequest = TaskRequest.builder()
                    .sourceType(com.project.task.model.EventSourceType.SQS)
                    .eventId(message.getMessageId())
                    .requestBody(messageBody)
                    .metadata(createSqsMetadata(message))
                    .build();

            logTaskRequest(taskRequest);

            // Process business logic
            TaskResponse taskResponse = executeBusinessLogic(taskRequest, context);

            log.info("SQS message processed successfully: taskId={}", taskResponse.getTaskId());

            // TODO: Persist result / emit event / call downstream system

        } catch (Exception e) {
            log.error("Error processing SQS message: {}", e.getMessage(), e);
            // Re-throw to trigger message retry
            throw new RuntimeException("SQS message processing failed", e);
        }
    }

    /**
     * Process EventBridge event.
     *
     * @param event   EventBridge event
     * @param context Lambda context
     */
    public void processEventBridgeEvent(ScheduledEvent event, Context context) {
        log.info("Processing EventBridge event: eventId={}", event.getId());

        try {
            // Parse event to TaskRequest
            TaskRequest taskRequest = EventParser.parseEventBridgeEvent(event);
            logTaskRequest(taskRequest);

            // Process business logic
            TaskResponse taskResponse = executeBusinessLogic(taskRequest, context);

            log.info("EventBridge event processed successfully: taskId={}", taskResponse.getTaskId());

            // TODO: Orchestrate async workflows / trigger step functions

        } catch (Exception e) {
            log.error("Error processing EventBridge event: {}", e.getMessage(), e);
            throw new RuntimeException("EventBridge event processing failed", e);
        }
    }

    /**
     * Execute business logic (placeholder for actual implementation).
     * TODO: Implement your actual business logic here.
     */
    private TaskResponse executeBusinessLogic(TaskRequest request, Context context) {
        log.info("Executing business logic for: sourceType={}, eventId={}",
                request.getSourceType(), request.getEventId());

        // Generate unique task ID
        String taskId = UUID.randomUUID().toString();

        // TODO: Implement actual business logic here
        // Examples:
        // - Save to DynamoDB
        // - Call external APIs
        // - Process data transformations
        // - Send notifications (SNS/SES)
        // - Trigger Step Functions
        // - Store in S3

        log.debug("Business logic placeholder executed - taskId={}", taskId);

        // Build response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("requestId", context.getAwsRequestId());
        responseData.put("sourceType", request.getSourceType().name());
        responseData.put("receivedAt", request.getTimestamp());
        responseData.put("processedAt", System.currentTimeMillis());
        responseData.put("functionName", context.getFunctionName());

        return TaskResponse.builder()
                .success(true)
                .message("Task received and queued for processing")
                .taskId(taskId)
                .data(responseData)
                .build();
    }

    /**
     * Validate request (placeholder).
     */
    private void validateRequest(TaskRequest request) {
        if (request.getRequestBody() == null || request.getRequestBody().isEmpty()) {
            log.warn("Request body is empty");
        }
        // TODO: Add your validation logic here
    }

    /**
     * Build API Gateway response.
     */
    private APIGatewayProxyResponseEvent buildApiResponse(int statusCode, TaskResponse taskResponse) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(JsonUtil.toJson(taskResponse));
    }

    /**
     * Create metadata map from SQS message.
     */
    private Map<String, Object> createSqsMetadata(SQSEvent.SQSMessage message) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("receiptHandle", message.getReceiptHandle());
        metadata.put("eventSourceARN", message.getEventSourceArn());
        metadata.put("attributes", message.getAttributes());
        metadata.put("messageAttributes", message.getMessageAttributes());
        return metadata;
    }

    /**
     * Log task request details.
     */
    private void logTaskRequest(TaskRequest request) {
        log.info("Task Request Details:");
        log.info("  Source Type: {}", request.getSourceType().getDisplayName());
        log.info("  Event ID: {}", request.getEventId());
        log.info("  Request Body: {}", truncate(request.getRequestBody(), 200));
        log.info("  Metadata: {}", request.getMetadata() != null ?
                request.getMetadata().size() + " entries" : "none");
        log.info("  Timestamp: {}", request.getTimestamp());
    }

    /**
     * Truncate string for logging.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}

