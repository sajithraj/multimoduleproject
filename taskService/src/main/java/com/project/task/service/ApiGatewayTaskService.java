package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.task.data.TaskData;
import com.project.task.mapper.TaskMapper;
import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ApiGatewayTaskService - Handles all API Gateway requests
 * REST API operations: GET, POST, PUT, DELETE
 */
@Slf4j
public class ApiGatewayTaskService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TaskMapper TASK_MAPPER = TaskMapper.INSTANCE;

    /**
     * Safely get request ID from context
     */
    private String getRequestId(Context context) {
        return context != null ? context.getAwsRequestId() : "unknown-request-id";
    }

    /**
     * Build standardized API response
     */
    private APIGatewayProxyResponseEvent buildApiResponseWithData(int statusCode, Map<String, Object> data) {
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(statusCode);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
            response.setHeaders(headers);

            response.setBody(OBJECT_MAPPER.writeValueAsString(data));
            return response;
        } catch (Exception e) {
            log.error("Error building API response: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Internal server error");
        }
    }

    /**
     * Build error response
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", System.currentTimeMillis());
        return buildApiResponseWithData(statusCode, error);
    }

    // ========================================
    // API Gateway Endpoints
    // ========================================

    /**
     * Handle /ping endpoint - Health check
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
     * Handle GET /task - Get all tasks
     */
    public APIGatewayProxyResponseEvent processGetAllTasks(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /task - retrieve all tasks");

        List<Task> tasks = TaskData.getAllTasks();
        log.info("Retrieved {} tasks from store", tasks.size());

        Map<String, Object> response = new HashMap<>();
        response.put("service", "task-service");
        response.put("requestId", getRequestId(context));
        response.put("version", "1.0.0");
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "GET /task successfully invoked");
        response.put("count", tasks.size());
        response.put("data", tasks);

        return buildApiResponseWithData(200, response);
    }

    /**
     * Handle GET /task/{id} - Get task by ID
     */
    public APIGatewayProxyResponseEvent processGetTaskById(
            APIGatewayProxyRequestEvent event,
            Context context) {

        Map<String, String> pathParams = event.getPathParameters();
        String id = pathParams != null ? pathParams.get("id") : null;

        log.info("Processing GET /task/{{id}} - retrieve task by ID: {}", id);

        if (id == null || id.isEmpty()) {
            return buildErrorResponse(400, "Task ID is required");
        }

        Task task = TaskData.getTaskById(id);

        if (task == null) {
            log.warn("Task not found: {}", id);
            return buildErrorResponse(404, "Task not found: " + id);
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
    }

    /**
     * Handle POST /task - Create new task
     */
    public APIGatewayProxyResponseEvent processCreateTask(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing POST /task - create new task");

        String requestBody = event.getBody();
        if (requestBody == null || requestBody.isEmpty()) {
            return buildErrorResponse(400, "Request body is required");
        }

        log.debug("Request body: {}", requestBody);

        try {
            // Parse request DTO
            TaskRequestDTO requestDTO = OBJECT_MAPPER.readValue(requestBody, TaskRequestDTO.class);

            // Map DTO to Entity
            Task newTask = TASK_MAPPER.toEntity(requestDTO);

            // Generate ID if not provided
            if (newTask.getId() == null || newTask.getId().isEmpty()) {
                newTask.setId(UUID.randomUUID().toString());
            }

            // Save task
            TaskData.saveTask(newTask);

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

        } catch (Exception e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
            return buildErrorResponse(400, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Handle PUT /task/{id} - Update task
     */
    public APIGatewayProxyResponseEvent processUpdateTask(
            APIGatewayProxyRequestEvent event,
            Context context) {

        Map<String, String> pathParams = event.getPathParameters();
        String id = pathParams != null ? pathParams.get("id") : null;

        log.info("Processing PUT /task/{{id}} - update task: {}", id);

        if (id == null || id.isEmpty()) {
            return buildErrorResponse(400, "Task ID is required");
        }

        String requestBody = event.getBody();
        if (requestBody == null || requestBody.isEmpty()) {
            return buildErrorResponse(400, "Request body is required");
        }

        log.debug("Update payload: {}", requestBody);

        // Check if task exists
        Task existingTask = TaskData.getTaskById(id);
        if (existingTask == null) {
            return buildErrorResponse(404, "Task not found: " + id);
        }

        try {
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

            // Save updated task
            TaskData.saveTask(existingTask);

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
            log.error("Invalid status value: {}", e.getMessage());
            return buildErrorResponse(400, "Invalid status. Must be: TODO, IN_PROGRESS, COMPLETED, CANCELLED");
        } catch (Exception e) {
            log.error("Error updating task: {}", e.getMessage(), e);
            return buildErrorResponse(400, "Invalid request: " + e.getMessage());
        }
    }

    /**
     * Handle DELETE /task/{id} - Delete task
     */
    public APIGatewayProxyResponseEvent processDeleteTask(
            APIGatewayProxyRequestEvent event,
            Context context) {

        Map<String, String> pathParams = event.getPathParameters();
        String id = pathParams != null ? pathParams.get("id") : null;

        log.info("Processing DELETE /task/{{id}} - delete task: {}", id);

        if (id == null || id.isEmpty()) {
            return buildErrorResponse(400, "Task ID is required");
        }

        Task deletedTask = TaskData.deleteTask(id);

        if (deletedTask == null) {
            log.warn("Task not found for deletion: {}", id);
            return buildErrorResponse(404, "Task not found: " + id);
        }

        log.info("Deleted task: {} (ID: {})", deletedTask.getName(), id);

        Map<String, Object> response = new HashMap<>();
        response.put("service", "task-service");
        response.put("requestId", getRequestId(context));
        response.put("version", "1.0.0");
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "DELETE /task/" + id + " successfully invoked");
        response.put("data", deletedTask);

        return buildApiResponseWithData(200, response);
    }
}

