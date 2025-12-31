package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ApiGatewayTaskServiceHandler {

    private static final Logger log = LogManager.getLogger(ApiGatewayTaskServiceHandler.class);
    private final ApiGatewayTaskService taskService;

    private static final java.util.Map<String, String> DEFAULT_HEADERS = java.util.Collections.unmodifiableMap(new java.util.HashMap<>() {{
        put("Content-Type", "application/json");
        put("Access-Control-Allow-Origin", "*");
        put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        put("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }});

    public ApiGatewayTaskServiceHandler(ApiGatewayTaskService taskService) {
        this.taskService = taskService;
    }

    public APIGatewayProxyResponseEvent route(
            APIGatewayProxyRequestEvent event,
            Context context) {

        String path = event.getPath();
        String method = event.getHttpMethod();

        log.info("Routing API request: method={}, path={}", method, path);

        try {
            // Route based on path and method
            return switch (path) {
                case "/ping" -> handlePing(event, context);
                case "/task" -> handleTaskCollection(event, context);
                default -> {
                    // Handle dynamic paths like /task/{id}
                    if (path.startsWith("/task/")) {
                        yield handleTaskById(event, context);
                    }
                    yield handleNotFound(path, method);
                }
            };

        } catch (Exception e) {
            log.error("Error routing API request: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Internal server error");
        }
    }

    private APIGatewayProxyResponseEvent handlePing(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Handling GET /ping request");
        return taskService.processPing(event, context);
    }

    private APIGatewayProxyResponseEvent handleTaskCollection(
            APIGatewayProxyRequestEvent event,
            Context context) {

        String method = event.getHttpMethod();
        log.info("Handling {} /task request", method);

        return switch (method) {
            case "GET" -> taskService.processGetAllTasks(event, context);
            case "POST" -> {
                // Validate body for POST
                if (event.getBody() == null || event.getBody().isEmpty()) {
                    yield buildErrorResponse(400, "Request body is required");
                }
                yield taskService.processCreateTask(event, context);
            }
            default -> buildErrorResponse(405, "Method not allowed. Use GET or POST.");
        };
    }

    private APIGatewayProxyResponseEvent handleTaskById(
            APIGatewayProxyRequestEvent event,
            Context context) {

        String method = event.getHttpMethod();
        String path = event.getPath();

        // Extract ID from path
        String id = path.substring(path.lastIndexOf('/') + 1);
        log.info("Handling {} /task/{{id}} request, id={}", method, id);

        return switch (method) {
            case "GET" -> taskService.processGetTaskById(event, context);
            case "PUT" -> {
                if (event.getBody() == null || event.getBody().isEmpty()) {
                    yield buildErrorResponse(400, "Request body is required");
                }
                yield taskService.processUpdateTask(event, context);
            }
            case "DELETE" -> taskService.processDeleteTask(event, context);
            default -> buildErrorResponse(405, "Method not allowed. Use GET, PUT, or DELETE.");
        };
    }

    private APIGatewayProxyResponseEvent handleNotFound(String path, String method) {
        log.warn("No route found for request: {} {}", method, path);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("service", "task-service");
        errorBody.put("error", "Not Found");
        errorBody.put("message", "No route found for " + method + " " + path);
        errorBody.put("availableRoutes", new String[]{
                "GET /ping - Health check",
                "GET /task - Get all tasks",
                "POST /task - Create new task",
                "GET /task/{id} - Get task by ID",
                "PUT /task/{id} - Update task",
                "DELETE /task/{id} - Delete task"
        });

        return buildResponse(404, errorBody);
    }

    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("service", "task-service");
        errorBody.put("success", false);
        errorBody.put("error", message);

        return buildResponse(statusCode, errorBody);
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(DEFAULT_HEADERS)
                .withBody(com.project.task.util.JsonUtil.toJson(body));
    }

}
