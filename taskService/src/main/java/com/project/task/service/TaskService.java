package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.task.model.Task;
import com.project.task.model.TaskRequest;
import com.project.task.model.TaskResponse;
import com.project.task.util.EventParser;
import com.project.task.util.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business service that processes tasks from different event sources.
 * Production-ready features:
 * - Thread-safe in-memory storage using ConcurrentHashMap
 * - Atomic ID generation
 * - Proper CRUD operations
 * - Input validation
 * - Error handling
 */
public class TaskService {

    private static final Logger log = LogManager.getLogger(TaskService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Thread-safe in-memory storage
    private static final Map<String, Task> TASK_STORE = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    // Initialize with sample data
    static {
        initializeSampleData();
    }

    /**
     * Initialize sample tasks for testing.
     */
    private static void initializeSampleData() {
        Task task1 = Task.builder()
                .id("task-1")
                .name("Complete documentation")
                .description("Write comprehensive API documentation")
                .status(Task.TaskStatus.IN_PROGRESS)
                .build();

        Task task2 = Task.builder()
                .id("task-2")
                .name("Review code changes")
                .description("Review pull request #123")
                .status(Task.TaskStatus.TODO)
                .build();

        Task task3 = Task.builder()
                .id("task-3")
                .name("Deploy to production")
                .description("Deploy v1.0.0 to production environment")
                .status(Task.TaskStatus.COMPLETED)
                .build();

        TASK_STORE.put(task1.getId(), task1);
        TASK_STORE.put(task2.getId(), task2);
        TASK_STORE.put(task3.getId(), task3);

        log.info("Initialized task store with {} sample tasks", TASK_STORE.size());
    }

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

    // ========================================
    // API Gateway Endpoint Handlers
    // ========================================

    /**
     * Handle /ping endpoint - Health check.
     */
    public APIGatewayProxyResponseEvent processPing(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /ping health check");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "task-service");
        response.put("requestId", getRequestId(context));
        response.put("version", "1.0.0");
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "GET /ping successfully invoked");

        return buildApiResponseWithData(200, response);
    }

    /**
     * Handle GET /task - Get all tasks.
     */
    public APIGatewayProxyResponseEvent processGetAllTasks(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /task - retrieve all tasks");

        try {
            // Get all tasks from store
            List<Task> allTasks = new ArrayList<>(TASK_STORE.values());

            log.info("Retrieved {} tasks from store", allTasks.size());

            Map<String, Object> response = new HashMap<>();
            response.put("service", "task-service");
            response.put("requestId", getRequestId(context));
            response.put("version", "1.0.0");
            response.put("status", "success");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "GET /task successfully invoked");
            response.put("data", allTasks);
            response.put("count", allTasks.size());

            return buildApiResponseWithData(200, response);

        } catch (Exception e) {
            log.error("Error retrieving tasks: {}", e.getMessage(), e);
            return buildApiErrorResponse(500, "GET /task", "Failed to retrieve tasks: " + e.getMessage());
        }
    }

    /**
     * Handle GET /task/{id} - Get task by ID.
     */
    public APIGatewayProxyResponseEvent processGetTaskById(
            String id,
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /task/{{id}} - retrieve task by ID: {}", id);

        try {
            // Retrieve task from store
            Task task = TASK_STORE.get(id);

            if (task == null) {
                log.warn("Task not found with ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("service", "task-service");
                errorResponse.put("requestId", getRequestId(context));
                errorResponse.put("version", "1.0.0");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", System.currentTimeMillis());
                errorResponse.put("message", "GET /task/" + id + " - Task not found");
                errorResponse.put("error", "Task with ID '" + id + "' does not exist");

                return buildApiResponseWithData(404, errorResponse);
            }

            log.info("Found task: {}", task.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("service", "task-service");
            response.put("requestId", getRequestId(context));
            response.put("version", "1.0.0");
            response.put("status", "success");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "GET /task/" + id + " successfully invoked");
            response.put("data", task);

            return buildApiResponseWithData(200, response);

        } catch (Exception e) {
            log.error("Error retrieving task by ID: {}", e.getMessage(), e);
            return buildApiErrorResponse(500, "GET /task/" + id, "Failed to retrieve task: " + e.getMessage());
        }
    }

    /**
     * Handle POST /task - Create new task.
     */
    public APIGatewayProxyResponseEvent processCreateTask(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing POST /task - create new task");

        try {
            String requestBody = event.getBody();
            log.debug("Request body: {}", requestBody);

            // Parse request body to Task object
            Map<String, Object> requestMap = OBJECT_MAPPER.readValue(requestBody, Map.class);

            // Generate unique ID
            String generatedId = "task-" + ID_GENERATOR.getAndIncrement();

            // Build task from request - only essential fields
            Task.TaskBuilder taskBuilder = Task.builder()
                    .id(generatedId)
                    .name((String) requestMap.get("name"));

            // Optional fields
            if (requestMap.containsKey("description")) {
                taskBuilder.description((String) requestMap.get("description"));
            }
            if (requestMap.containsKey("status")) {
                taskBuilder.status(Task.TaskStatus.valueOf(((String) requestMap.get("status")).toUpperCase()));
            }

            // Build and save task
            Task newTask = taskBuilder.build();
            TASK_STORE.put(newTask.getId(), newTask);

            log.info("Created new task with ID: {}, name: {}", newTask.getId(), newTask.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("service", "task-service");
            response.put("requestId", getRequestId(context));
            response.put("version", "1.0.0");
            response.put("status", "success");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "POST /task successfully invoked");
            response.put("data", newTask);

            return buildApiResponseWithData(201, response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error creating task: {}", e.getMessage(), e);
            return buildApiErrorResponse(400, "POST /task", "Invalid input: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
            return buildApiErrorResponse(400, "POST /task", "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating task: {}", e.getMessage(), e);
            return buildApiErrorResponse(500, "POST /task", "Failed to create task: " + e.getMessage());
        }
    }

    /**
     * Handle PUT /task/{id} - Update task.
     */
    public APIGatewayProxyResponseEvent processUpdateTask(
            String id,
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing PUT /task/{{id}} - update task: {}", id);

        try {
            // Check if task exists
            Task existingTask = TASK_STORE.get(id);
            if (existingTask == null) {
                log.warn("Task not found with ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("service", "task-service");
                errorResponse.put("requestId", getRequestId(context));
                errorResponse.put("version", "1.0.0");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", System.currentTimeMillis());
                errorResponse.put("message", "PUT /task/" + id + " - Task not found");
                errorResponse.put("error", "Task with ID '" + id + "' does not exist");

                return buildApiResponseWithData(404, errorResponse);
            }

            String requestBody = event.getBody();
            log.debug("Update payload: {}", requestBody);

            // Parse update request
            Map<String, Object> updates = OBJECT_MAPPER.readValue(requestBody, Map.class);

            // Apply updates - only essential fields
            if (updates.containsKey("name")) {
                existingTask.setName((String) updates.get("name"));
            }
            if (updates.containsKey("description")) {
                existingTask.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("status")) {
                existingTask.setStatus(Task.TaskStatus.valueOf(((String) updates.get("status")).toUpperCase()));
            }

            // Update timestamp
            existingTask.setUpdatedAt(System.currentTimeMillis());

            log.info("Updated task: {}", existingTask.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("service", "task-service");
            response.put("requestId", getRequestId(context));
            response.put("version", "1.0.0");
            response.put("status", "success");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "PUT /task/" + id + " successfully invoked");
            response.put("data", existingTask);

            return buildApiResponseWithData(200, response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error updating task: {}", e.getMessage(), e);
            return buildApiErrorResponse(400, "PUT /task/" + id, "Invalid input: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
            return buildApiErrorResponse(400, "PUT /task/" + id, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error updating task: {}", e.getMessage(), e);
            return buildApiErrorResponse(500, "PUT /task/" + id, "Failed to update task: " + e.getMessage());
        }
    }

    /**
     * Handle DELETE /task/{id} - Delete task.
     */
    public APIGatewayProxyResponseEvent processDeleteTask(
            String id,
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing DELETE /task/{{id}} - delete task: {}", id);

        try {
            // Check if task exists and remove it
            Task deletedTask = TASK_STORE.remove(id);

            if (deletedTask == null) {
                log.warn("Task not found with ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("service", "task-service");
                errorResponse.put("requestId", getRequestId(context));
                errorResponse.put("version", "1.0.0");
                errorResponse.put("status", "error");
                errorResponse.put("timestamp", System.currentTimeMillis());
                errorResponse.put("message", "DELETE /task/" + id + " - Task not found");
                errorResponse.put("error", "Task with ID '" + id + "' does not exist");

                return buildApiResponseWithData(404, errorResponse);
            }

            log.info("Deleted task: {} (ID: {})", deletedTask.getName(), id);

            Map<String, Object> deletionInfo = new HashMap<>();
            deletionInfo.put("id", id);
            deletionInfo.put("name", deletedTask.getName());
            deletionInfo.put("deletedAt", System.currentTimeMillis());

            Map<String, Object> response = new HashMap<>();
            response.put("service", "task-service");
            response.put("requestId", getRequestId(context));
            response.put("version", "1.0.0");
            response.put("status", "success");
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "DELETE /task/" + id + " successfully invoked");
            response.put("data", deletionInfo);

            return buildApiResponseWithData(200, response);

        } catch (Exception e) {
            log.error("Error deleting task: {}", e.getMessage(), e);
            return buildApiErrorResponse(500, "DELETE /task/" + id, "Failed to delete task: " + e.getMessage());
        }
    }

    // ========================================
    // OLD METHODS - Keeping for backward compatibility
    // ========================================

    /**
     * Handle GET /get endpoint - Get all resources.
     *
     * @deprecated Use processGetAllTasks() instead
     */
    @Deprecated
    public APIGatewayProxyResponseEvent processGetAll(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /get - retrieve all resources");

        try {
            // TODO: Implement your logic to fetch all resources
            // Example: Query DynamoDB, call external API, etc.

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Retrieved all resources");
            responseData.put("data", new Object[]{
                    createSampleResource("res-1", "Resource 1"),
                    createSampleResource("res-2", "Resource 2"),
                    createSampleResource("res-3", "Resource 3")
            });
            responseData.put("count", 3);

            return buildApiResponseWithData(200, responseData);

        } catch (Exception e) {
            log.error("Error retrieving resources: {}", e.getMessage(), e);
            return buildApiResponse(500, TaskResponse.builder()
                    .success(false)
                    .message("Error retrieving resources: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Handle GET /id/{id} endpoint - Get resource by ID.
     */
    public APIGatewayProxyResponseEvent processGetById(
            String id,
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /id/{{id}} - retrieve resource: id={}", id);

        try {
            // Validate ID
            if (id == null || id.isEmpty()) {
                return buildApiResponse(400, TaskResponse.builder()
                        .success(false)
                        .message("ID parameter is required")
                        .build());
            }

            // TODO: Implement your logic to fetch resource by ID
            // Example: Query DynamoDB with ID, call external API, etc.

            Map<String, Object> resource = createSampleResource(id, "Resource " + id);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Resource retrieved successfully");
            responseData.put("data", resource);

            return buildApiResponseWithData(200, responseData);

        } catch (Exception e) {
            log.error("Error retrieving resource by ID: {}", e.getMessage(), e);
            return buildApiResponse(500, TaskResponse.builder()
                    .success(false)
                    .message("Error retrieving resource: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Handle POST /post endpoint - Create new resource.
     */
    public APIGatewayProxyResponseEvent processPost(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing POST /post - create new resource");

        try {
            String body = event.getBody();
            log.debug("Request body: {}", body);

            // TODO: Implement your logic to create resource
            // Example: Parse body, validate, save to DynamoDB, etc.

            String newId = UUID.randomUUID().toString();

            TaskResponse response = TaskResponse.builder()
                    .success(true)
                    .message("Resource created successfully")
                    .taskId(newId)
                    .data(createSampleResource(newId, "New Resource"))
                    .build();

            return buildApiResponse(201, response);

        } catch (Exception e) {
            log.error("Error creating resource: {}", e.getMessage(), e);
            return buildApiResponse(500, TaskResponse.builder()
                    .success(false)
                    .message("Error creating resource: " + e.getMessage())
                    .build());
        }
    }

    // ========================================
    // EventBridge Event Handlers
    // ========================================

    /**
     * Process scheduled task (cron/rate expressions).
     */
    public void processScheduledTask(ScheduledEvent event, Context context) {
        log.info("Processing scheduled task");

        try {
            // TODO: Implement scheduled task logic
            // Example: Generate reports, cleanup old data, send notifications

            log.info("Scheduled task completed successfully");

        } catch (Exception e) {
            log.error("Error processing scheduled task: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduled task failed", e);
        }
    }

    /**
     * Process custom business event (OrderCreated, etc.).
     */
    public void processCustomEvent(ScheduledEvent event, Context context) {
        log.info("Processing custom event: detailType={}", event.getDetailType());

        try {
            Map<String, Object> detail = event.getDetail();
            log.debug("Event detail: {}", detail);

            // TODO: Implement custom event logic
            // Example: Process order, update inventory, send email

            log.info("Custom event processed successfully");

        } catch (Exception e) {
            log.error("Error processing custom event: {}", e.getMessage(), e);
            throw new RuntimeException("Custom event processing failed", e);
        }
    }

    /**
     * Process order-related events.
     */
    public void processOrderEvent(ScheduledEvent event, Context context) {
        log.info("Processing order event: detailType={}", event.getDetailType());

        // TODO: Implement order-specific logic
        // Example: Update order status, notify customer, trigger fulfillment

        log.info("Order event processed successfully");
    }

    /**
     * Process payment-related events.
     */
    public void processPaymentEvent(ScheduledEvent event, Context context) {
        log.info("Processing payment event: detailType={}", event.getDetailType());

        // TODO: Implement payment-specific logic
        // Example: Record payment, update invoice, send receipt

        log.info("Payment event processed successfully");
    }

    /**
     * Process user-related events.
     */
    public void processUserEvent(ScheduledEvent event, Context context) {
        log.info("Processing user event: detailType={}", event.getDetailType());

        // TODO: Implement user-specific logic
        // Example: Send welcome email, create profile, update preferences

        log.info("User event processed successfully");
    }

    /**
     * Process AWS system events.
     */
    public void processSystemEvent(ScheduledEvent event, Context context) {
        log.info("Processing system event: source={}", event.getSource());

        // TODO: Implement system event logic
        // Example: Handle EC2 state changes, S3 events, CloudWatch alarms

        log.info("System event processed successfully");
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Safely get request ID from context (handles null context)
     */
    private String getRequestId(Context context) {
        return context != null ? context.getAwsRequestId() : "unknown-request-id";
    }

    /**
     * Create sample resource (placeholder).
     */
    private Map<String, Object> createSampleResource(String id, String name) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("id", id);
        resource.put("name", name);
        resource.put("status", "active");
        resource.put("createdAt", System.currentTimeMillis());
        return resource;
    }

    /**
     * Create sample task (placeholder).
     */
    private Map<String, Object> createSampleTask(String id, String title, String status) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("title", title);
        task.put("status", status);
        task.put("createdAt", System.currentTimeMillis());
        task.put("updatedAt", System.currentTimeMillis());
        return task;
    }

    /**
     * Build standardized error response.
     */
    private APIGatewayProxyResponseEvent buildApiErrorResponse(int statusCode, String endpoint, String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("service", "task-service");
        errorResponse.put("version", "1.0.0");
        errorResponse.put("status", "error");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("message", endpoint + " invocation failed");
        errorResponse.put("error", errorMessage);

        return buildApiResponseWithData(statusCode, errorResponse);
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
        responseData.put("requestId", getRequestId(context));
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
     * Build API Gateway response with arbitrary data (for endpoints like /ping, /get).
     * Overloaded version that accepts any Object for flexible response structures.
     */
    private APIGatewayProxyResponseEvent buildApiResponseWithData(int statusCode, Object data) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(JsonUtil.toJson(data));
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

