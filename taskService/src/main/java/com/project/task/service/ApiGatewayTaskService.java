package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.task.data.TaskData;
import com.project.task.mapper.TaskMapper;
import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ApiGatewayTaskService {

    private static final Logger log = LogManager.getLogger(ApiGatewayTaskService.class);

    // Use shared JSON util and headers to avoid per-request allocations
    private static final java.util.Map<String, String> DEFAULT_HEADERS = java.util.Collections.unmodifiableMap(new java.util.HashMap<>() {{
        put("Content-Type", "application/json");
        put("Access-Control-Allow-Origin", "*");
        put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        put("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }});

    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    @Inject
    public ApiGatewayTaskService(TaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    private String getRequestId(Context context) {
        return context != null ? context.getAwsRequestId() : "unknown-request-id";
    }

    private APIGatewayProxyResponseEvent buildApiResponseWithData(int statusCode, Map<String, Object> data) {
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(statusCode);
            response.setHeaders(DEFAULT_HEADERS);
            response.setBody(objectMapper.writeValueAsString(data));
            return response;
        } catch (Exception e) {
            log.error("Error building API response: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Internal server error");
        }
    }

    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", System.currentTimeMillis());
        return buildApiResponseWithData(statusCode, error);
    }

    public APIGatewayProxyResponseEvent processPing(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /ping health check");

        Map<String, Object> response = buildStandardResponse(context, "healthy", "GET /ping successfully invoked");
        return buildApiResponseWithData(200, response);
    }

    public APIGatewayProxyResponseEvent processGetAllTasks(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Processing GET /task - retrieve all tasks");

        List<Task> tasks = TaskData.getAllTasks();
        log.info("Retrieved {} tasks from store", tasks.size());

        Map<String, Object> response = buildStandardResponse(context, "success", "GET /task successfully invoked");
        response.put("count", tasks.size());
        response.put("data", tasks);
        return buildApiResponseWithData(200, response);
    }

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

        Map<String, Object> response = buildStandardResponse(context, "success", "GET /task/" + id + " successfully invoked");
        response.put("data", task);
        return buildApiResponseWithData(200, response);
    }

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
            TaskRequestDTO requestDTO = objectMapper.readValue(requestBody, TaskRequestDTO.class);

            Task newTask = taskMapper.toEntity(requestDTO);
            TaskData.saveTask(newTask);

            log.info("Created new task with ID: {}, name: {}", newTask.getId(), newTask.getName());

            Map<String, Object> response = buildStandardResponse(context, "success", "POST /task successfully invoked");
            response.put("data", newTask);
            return buildApiResponseWithData(201, response);

        } catch (Exception e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
            return buildErrorResponse(400, "Invalid JSON: " + e.getMessage());
        }
    }

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

        if (!TaskData.taskExists(id)) {
            return buildErrorResponse(404, "Task not found: " + id);
        }

        try {
            Task existingTask = TaskData.getTaskById(id);
            TaskRequestDTO updateDTO = objectMapper.readValue(requestBody, TaskRequestDTO.class);

            taskMapper.updateEntityFromDto(updateDTO, existingTask);
            existingTask.setUpdatedAt(System.currentTimeMillis());

            TaskData.saveTask(existingTask);

            log.info("Updated task: {}", existingTask.getName());

            Map<String, Object> response = buildStandardResponse(context, "success", "PUT /task/" + id + " successfully invoked");
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

        Map<String, Object> response = buildStandardResponse(context, "success", "DELETE /task/" + id + " successfully invoked");
        response.put("data", deletedTask);
        return buildApiResponseWithData(200, response);
    }

    private Map<String, Object> buildStandardResponse(Context context, String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "task-service");
        response.put("requestId", getRequestId(context));
        response.put("version", "1.0.0");
        response.put("status", status);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", message);
        return response;
    }

}
