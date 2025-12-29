package com.project.task.router;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.project.task.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes API Gateway requests to appropriate handlers based on path and method.
 * Supports multiple endpoints: /ping, /id/{id}, /get, /post
 */
public class ApiGatewayRouter {

    private static final Logger log = LogManager.getLogger(ApiGatewayRouter.class);
    private final TaskService taskService;

    public ApiGatewayRouter(TaskService taskService) {
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
                case "/get" -> handleGet(event, context);
                case "/post" -> handlePost(event, context);
                default -> {
                    // Handle dynamic paths like /id/{id}
                    if (path.startsWith("/id/")) {
                        yield handleGetById(event, context);
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

        log.info("Handling /ping request");
        return taskService.processPing(event, context);
    }

    /**
     * Handle /get endpoint - Get all resources
     */
    private APIGatewayProxyResponseEvent handleGet(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Handling GET /get request");

        // Validate method
        if (!"GET".equals(event.getHttpMethod())) {
            return buildErrorResponse(405, "Method not allowed. Use GET.");
        }

        return taskService.processGetAll(event, context);
    }

    /**
     * Handle /id/{id} endpoint - Get resource by ID
     */
    private APIGatewayProxyResponseEvent handleGetById(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Handling GET /id/{{id}} request");

        // Validate method
        if (!"GET".equals(event.getHttpMethod())) {
            return buildErrorResponse(405, "Method not allowed. Use GET.");
        }

        // Extract ID from path
        String path = event.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);

        log.debug("Extracted ID: {}", id);

        // Get from path parameters if available
        Map<String, String> pathParams = event.getPathParameters();
        if (pathParams != null && pathParams.containsKey("id")) {
            id = pathParams.get("id");
        }

        return taskService.processGetById(id, event, context);
    }

    /**
     * Handle /post endpoint - Create new resource
     */
    private APIGatewayProxyResponseEvent handlePost(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Handling POST /post request");

        // Validate method
        if (!"POST".equals(event.getHttpMethod())) {
            return buildErrorResponse(405, "Method not allowed. Use POST.");
        }

        // Validate body
        if (event.getBody() == null || event.getBody().isEmpty()) {
            return buildErrorResponse(400, "Request body is required");
        }

        return taskService.processPost(event, context);
    }

    /**
     * Handle 404 - Not Found
     */
    private APIGatewayProxyResponseEvent handleNotFound(String path, String method) {
        log.warn("No route found for: {} {}", method, path);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "Not Found");
        errorBody.put("message", "No route found for " + method + " " + path);
        errorBody.put("availableRoutes", new String[]{
                "GET /ping",
                "GET /get",
                "GET /id/{id}",
                "POST /post"
        });

        return buildResponse(404, errorBody);
    }

    /**
     * Build error response
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String message) {
        Map<String, Object> errorBody = new HashMap<>();
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

