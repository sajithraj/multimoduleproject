package com.project.task.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.project.task.handler.UnifiedTaskHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ApiGatewayIntegrationTest extends BaseIntegrationTest {

    private UnifiedTaskHandler handler;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();

        when(mockContext.getAwsRequestId()).thenReturn("api-gateway-test-id");
        when(mockContext.getFunctionName()).thenReturn("task-service-api-integration");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
    }

    @Test
    public void testApiGateway_HealthCheck() {
        System.out.println("\n=== Test: API Gateway GET /ping ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/ping");
        event.setResource("/ping");

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("ping-integration-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertTrue(apiResponse.getBody().contains("healthy"));
        assertTrue(apiResponse.getBody().contains("task-service"));

        System.out.println("✓ Ping test passed");
    }

    @Test
    public void testApiGateway_GetAllTasks() {
        System.out.println("\n=== Test: API Gateway GET /task ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/task");
        event.setResource("/task");

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("get-all-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());
        assertTrue(apiResponse.getBody().contains("task-service"));

        System.out.println("✓ GET all tasks test passed");
    }

    @Test
    public void testApiGateway_GetTaskById() {
        System.out.println("\n=== Test: API Gateway GET /task/{id} ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/task/task-1");
        event.setResource("/task/task-1");

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "task-1");
        event.setPathParameters(pathParams);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("get-by-id-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());

        System.out.println("✓ GET task by ID test passed");
    }

    @Test
    public void testApiGateway_CreateTask() {
        System.out.println("\n=== Test: API Gateway POST /task ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/task");
        event.setResource("/task");
        event.setBody("{\"name\":\"New Task\",\"description\":\"Integration test task\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("post-create-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals("Should return 201 Created", Integer.valueOf(201), apiResponse.getStatusCode());
        assertNotNull("Response body should not be null", apiResponse.getBody());

        System.out.println("✓ POST create task test passed");
    }

    @Test
    public void testApiGateway_UpdateTask() {
        System.out.println("\n=== Test: API Gateway PUT /task/{id} ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("PUT");
        event.setPath("/task/task-1");
        event.setResource("/task/task-1");
        event.setBody("{\"name\":\"Updated Task\",\"status\":\"COMPLETED\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "task-1");
        event.setPathParameters(pathParams);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("put-update-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());

        System.out.println("✓ PUT update task test passed");
    }

    @Test
    public void testApiGateway_DeleteTask() {
        System.out.println("\n=== Test: API Gateway DELETE /task/{id} ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("DELETE");
        event.setPath("/task/task-2");
        event.setResource("/task/task-2");

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "task-2");
        event.setPathParameters(pathParams);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("delete-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());

        System.out.println("✓ DELETE task test passed");
    }

    @Test
    public void testApiGateway_InvalidJson() {
        System.out.println("\n=== Test: API Gateway POST with invalid JSON ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/task");
        event.setResource("/task");
        event.setBody("invalid-json{{{");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("error-handling-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(convertToMap(event), mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertTrue("Status should indicate error",
                apiResponse.getStatusCode() >= 400);

        System.out.println("✓ Error handling test passed");
    }

    @Test
    public void testApiGateway_Performance() {
        System.out.println("\n=== Test: API Gateway Performance ===");

        long startTime = System.currentTimeMillis();
        int requestCount = 10;
        int successCount = 0;

        for (int i = 0; i < requestCount; i++) {
            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
            event.setHttpMethod("GET");
            event.setPath("/ping");
            event.setResource("/ping");

            APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                    new APIGatewayProxyRequestEvent.ProxyRequestContext();
            ctx.setRequestId("perf-test-" + i);
            event.setRequestContext(ctx);

            Object response = handler.handleRequest(convertToMap(event), mockContext);
            if (response != null) {
                successCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        double avgTime = duration / (double) requestCount;

        System.out.println("Processed " + successCount + "/" + requestCount +
                " requests in " + duration + "ms");
        System.out.println("Average time: " + String.format("%.2f", avgTime) + "ms");

        assertEquals(requestCount, successCount);
        System.out.println("✓ Performance test passed");
    }
}


