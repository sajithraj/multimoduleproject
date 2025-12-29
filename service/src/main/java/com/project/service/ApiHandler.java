package com.project.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.service.client.ExternalApiClient;
import com.project.service.exception.ExternalApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import software.amazon.lambda.powertools.logging.Logging;

/**
 * AWS Lambda Handler for API Gateway events.
 * <p>
 * Handles incoming HTTP requests from API Gateway, calls external API with OAuth2 token,
 * and returns formatted HTTP responses.
 * <p>
 * Features:
 * - Structured logging with Powertools
 * - MDC for request tracking
 * - Exception handling
 * - JSON request/response processing
 */
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LogManager.getLogger(ApiHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles API Gateway proxy request event.
     *
     * @param request API Gateway request
     * @param context Lambda context
     * @return API Gateway response
     */
    @Logging(logEvent = true)
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        try {
            ThreadContext.put("requestId", context != null ? context.getAwsRequestId() : "unknown");
            ThreadContext.put("path", request != null ? String.valueOf(request.getPath()) : "unknown");
            ThreadContext.put("httpMethod", request != null ? String.valueOf(request.getHttpMethod()) : "unknown");

            log.info("Received request: path={}, method={}, requestId={}",
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

    /**
     * Calls external API with OAuth2 authentication.
     *
     * @return API response body
     * @throws ExternalApiException if API call fails
     */
    private String callExternalApi() {
        return ExternalApiClient.getInstance().callExternalApi();
    }

    /**
     * Builds successful HTTP response.
     *
     * @param body       Response body
     * @param statusCode HTTP status code
     * @return API Gateway response
     */
    private APIGatewayProxyResponseEvent buildSuccessResponse(String body, int statusCode) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(body);
        response.setHeaders(getDefaultHeaders());
        return response;
    }

    /**
     * Builds error HTTP response.
     *
     * @param message    Error message
     * @param statusCode HTTP status code
     * @return API Gateway response
     */
    private APIGatewayProxyResponseEvent buildErrorResponse(String message, int statusCode) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody("{\"error\": \"" + message + "\"}");
        response.setHeaders(getDefaultHeaders());
        return response;
    }

    /**
     * Returns default HTTP headers for all responses.
     *
     * @return Map of headers
     */
    private java.util.Map<String, String> getDefaultHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return headers;
    }
}

