package com.project.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.project.service.client.ExternalApiClient;
import com.project.service.exception.ExternalApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import software.amazon.lambda.powertools.logging.Logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LogManager.getLogger(ApiHandler.class);

    // Reusable unmodifiable headers map to avoid per-request allocations
    private static final Map<String, String> DEFAULT_HEADERS;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("Content-Type", "application/json");
        m.put("Access-Control-Allow-Origin", "*");
        DEFAULT_HEADERS = Collections.unmodifiableMap(m);
    }

    @Logging(logEvent = true)
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        try {
            ThreadContext.put("requestId", context != null ? context.getAwsRequestId() : "unknown");
            ThreadContext.put("path", request != null ? String.valueOf(request.getPath()) : "unknown");
            ThreadContext.put("httpMethod", request != null ? String.valueOf(request.getHttpMethod()) : "unknown");

            log.info("Received request: path={}, method={}, requestId= {}",
                    request != null ? request.getPath() : "null",
                    request != null ? request.getHttpMethod() : "null",
                    context != null ? context.getAwsRequestId() : "null");

            try {
                String response = callExternalApi();
                return buildSuccessResponse(response, 200);

            } catch (ExternalApiException e) {
                log.error("External API error: {}", e.getMessage(), e);
                return buildErrorResponse("External API error: " + e.getMessage(), 502);

            } catch (Exception e) {
                log.error("Unexpected error processing request", e);
                return buildErrorResponse("Internal server error", 500);
            }

        } finally {
            ThreadContext.clearAll();
        }
    }

    private String callExternalApi() {
        return ExternalApiClient.getInstance().callExternalApi();
    }

    private APIGatewayProxyResponseEvent buildSuccessResponse(String body, int statusCode) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(body);
        response.setHeaders(DEFAULT_HEADERS);
        return response;
    }

    private APIGatewayProxyResponseEvent buildErrorResponse(String message, int statusCode) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody("{\"error\": \"" + message + "\"}");
        response.setHeaders(DEFAULT_HEADERS);
        return response;
    }

}
