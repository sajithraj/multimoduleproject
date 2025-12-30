package com.project.task.router;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.project.task.service.ApiGatewayTaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes API Gateway requests to appropriate handlers based on path and method.
 * Standardized endpoints: /ping, /task, /task/{id}
 */
public class ApiGatewayRouter {

    private static final Logger log = LogManager.getLogger(ApiGatewayRouter.class);
    private final ApiGatewayTaskService taskService;

    public ApiGatewayRouter(ApiGatewayTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Route API Gateway request to appropriate handler.
     *
     * @param event   API Gateway request event
     * @param context Lambda context
     * @return API Gateway response
     */
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
                case "/task" -> handleTaskCollection(event, context);  // GET all or POST new
                default -> {
                    // Handle dynamic paths like /task/{id}
                    if (path.startsWith("/task/")) {
                        yield handleTaskById(event, context);  // GET, PUT, or DELETE by ID
                    }
                    yield handleNotFound(path, method);
                }
            };

        } catch (Exception e) {
            log.error("Error routing API request: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Internal server error");
        }
    }

    /**
     * Handle /ping endpoint - Health check
     */
    private APIGatewayProxyResponseEvent handlePing(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Handling GET /ping request");
        return taskService.processPing(event, context);
    }

    /**
     * Handle /task endpoint - Collection operations
     * GET: List all tasks
     * POST: Create new task
     */
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

    /**
     * Handle /task/{id} endpoint - Individual task operations
     * GET: Get task by ID
     * PUT: Update task
     * DELETE: Delete task
     */
    private APIGatewayProxyResponseEvent handleTaskById(
            APIGatewayProxyRequestEvent event,
            Context context) {

        String method = event.getHttpMethod();
        String path = event.getPath();

        // Extract ID from path
        String id = path.substring(path.lastIndexOf('/') + 1);
        log.info("Handling {} /task/{{id}} request, id={}", method, id);

        // Override with path parameters if available
        Map<String, String> pathParams = event.getPathParameters();
        if (pathParams != null && pathParams.containsKey("id")) {
            id = pathParams.get("id");
        }

        return switch (method) {
            case "GET" -> taskService.processGetTaskById(event, context);
            case "PUT" -> {
                // Validate body for PUT
                if (event.getBody() == null || event.getBody().isEmpty()) {
                    yield buildErrorResponse(400, "Request body is required");
                }
                yield taskService.processUpdateTask(event, context);
            }
            case "DELETE" -> taskService.processDeleteTask(event, context);
            default -> buildErrorResponse(405, "Method not allowed. Use GET, PUT, or DELETE.");
        };
    }

    /**
     * Handle 404 - Not Found
     */
    private APIGatewayProxyResponseEvent handleNotFound(String path, String method) {
        log.warn("No route found for: {} {}", method, path);

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

    /**
     * Build error response
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("service", "task-service");
        errorBody.put("success", false);
        errorBody.put("error", message);

        return buildResponse(statusCode, errorBody);
    }

    /**
     * Build API Gateway response
     */
    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(com.project.task.util.JsonUtil.toJson(body));
    }
}

