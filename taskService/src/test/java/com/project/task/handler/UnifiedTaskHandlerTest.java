package com.project.task.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.project.task.model.TaskResponse;
import com.project.task.util.JsonUtil;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UnifiedTaskHandler.
 * Tests all three event sources: API Gateway, SQS, and EventBridge.
 */
public class UnifiedTaskHandlerTest {

    private UnifiedTaskHandler handler;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();

        // Setup mock context
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-12345");
        when(mockContext.getFunctionName()).thenReturn("task-service-function");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
    }

    // ========================================
    // API Gateway Event Tests
    // ========================================

    @Test
    public void testHandleApiGatewayEvent_Success() throws Exception {
        System.out.println("\n=== Test: API Gateway Event - Success ===");

        // Create API Gateway event
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/tasks");
        event.setBody("{\"taskName\":\"Process Order\",\"priority\":\"HIGH\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("api-gateway-request-123");
        event.setRequestContext(requestContext);

        // Handle request
        Object response = handler.handleRequest(event, mockContext);

        // Assertions
        assertNotNull("Response should not be null", response);
        assertTrue("Response should be APIGatewayProxyResponseEvent",
                response instanceof APIGatewayProxyResponseEvent);

        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals("Status code should be 200", Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull("Response body should not be null", apiResponse.getBody());

        // Parse response body
        TaskResponse taskResponse = JsonUtil.fromJson(apiResponse.getBody(), TaskResponse.class);
        assertTrue("Task should be successful", taskResponse.isSuccess());
        assertNotNull("Task ID should be generated", taskResponse.getTaskId());

        System.out.println("API Gateway Response: " + apiResponse.getBody());
        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleApiGatewayEvent_WithQueryParameters() throws Exception {
        System.out.println("\n=== Test: API Gateway Event - With Query Parameters ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/tasks");

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("status", "pending");
        queryParams.put("limit", "10");
        event.setQueryStringParameters(queryParams);

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("api-gateway-query-456");
        event.setRequestContext(requestContext);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull("Response body should not be null", apiResponse.getBody());

        // Note: This returns a list response with 'count' field, not TaskResponse structure
        System.out.println("Query Parameters Test Response: " + apiResponse.getBody());
        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleApiGatewayEvent_EmptyBody() throws Exception {
        System.out.println("\n=== Test: API Gateway Event - Empty Body ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/tasks");
        event.setBody(null);

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("api-gateway-empty-body");
        event.setRequestContext(requestContext);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());

        System.out.println("Empty Body Test Response: " + apiResponse.getBody());
        System.out.println("✓ Test passed");
    }

    // ========================================
    // SQS Event Tests
    // ========================================

    @Test
    public void testHandleSqsEvent_Success() {
        System.out.println("\n=== Test: SQS Event - Success ===");

        // Create SQS event
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("sqs-message-123");
        message.setReceiptHandle("receipt-handle-abc");
        message.setBody("{\"orderId\":\"ORD-001\",\"action\":\"PROCESS\"}");
        message.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:task-queue");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("SentTimestamp", String.valueOf(System.currentTimeMillis()));
        message.setAttributes(attributes);

        event.setRecords(Arrays.asList(message));

        // Handle request
        Object response = handler.handleRequest(event, mockContext);

        // For SQS, response is SQSBatchResponse with empty failures on success
        assertNotNull("SQS response should not be null", response);
        assertTrue("SQS response should be SQSBatchResponse",
                response instanceof com.amazonaws.services.lambda.runtime.events.SQSBatchResponse);

        com.amazonaws.services.lambda.runtime.events.SQSBatchResponse batchResponse =
                (com.amazonaws.services.lambda.runtime.events.SQSBatchResponse) response;
        assertEquals("Should have no batch item failures", 0,
                batchResponse.getBatchItemFailures().size());

        System.out.println("SQS message processed successfully");
        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleSqsEvent_MultipleMessages() {
        System.out.println("\n=== Test: SQS Event - Multiple Messages ===");

        SQSEvent event = new SQSEvent();

        SQSEvent.SQSMessage message1 = new SQSEvent.SQSMessage();
        message1.setMessageId("sqs-message-001");
        message1.setBody("{\"taskId\":\"TASK-001\"}");

        SQSEvent.SQSMessage message2 = new SQSEvent.SQSMessage();
        message2.setMessageId("sqs-message-002");
        message2.setBody("{\"taskId\":\"TASK-002\"}");

        event.setRecords(Arrays.asList(message1, message2));

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("SQS response should not be null", response);
        assertTrue("SQS response should be SQSBatchResponse",
                response instanceof com.amazonaws.services.lambda.runtime.events.SQSBatchResponse);

        com.amazonaws.services.lambda.runtime.events.SQSBatchResponse batchResponse =
                (com.amazonaws.services.lambda.runtime.events.SQSBatchResponse) response;
        assertEquals("Should have no batch item failures", 0,
                batchResponse.getBatchItemFailures().size());

        System.out.println("SQS batch processed successfully");
        System.out.println("✓ Test passed");
    }

    // ========================================
    // EventBridge Event Tests
    // ========================================

    @Test
    public void testHandleEventBridgeEvent_Success() {
        System.out.println("\n=== Test: EventBridge Event - Success ===");

        // Create EventBridge (ScheduledEvent) event
        ScheduledEvent event = new ScheduledEvent();
        event.setId("eventbridge-event-123");
        event.setSource("com.project.tasks");
        event.setDetailType("Scheduled Task");
        event.setRegion("us-east-1");
        event.setAccount("123456789012");
        event.setTime(DateTime.now());

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskType", "daily-report");
        detail.put("schedule", "0 9 * * *");
        event.setDetail(detail);

        event.setResources(Arrays.asList("arn:aws:events:us-east-1:123456789012:rule/daily-task"));

        // Handle request
        Object response = handler.handleRequest(event, mockContext);

        // For EventBridge, response is "OK" string
        assertNotNull("EventBridge response should not be null", response);
        assertEquals("EventBridge response should be 'OK'", "OK", response);

        System.out.println("EventBridge event processed successfully");
        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleEventBridgeEvent_CustomEvent() {
        System.out.println("\n=== Test: EventBridge Event - Custom Event ===");

        ScheduledEvent event = new ScheduledEvent();
        event.setId("custom-event-789");
        event.setSource("com.project.custom");
        event.setDetailType("Custom Business Event");
        event.setRegion("us-east-1");

        Map<String, Object> detail = new HashMap<>();
        detail.put("eventType", "ORDER_COMPLETED");
        detail.put("orderId", "ORD-12345");
        detail.put("customerId", "CUST-67890");
        event.setDetail(detail);

        Object response = handler.handleRequest(event, mockContext);
        assertEquals("OK", response);

        System.out.println("Custom event processed successfully");
        System.out.println("✓ Test passed");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    public void testHandleUnknownEventType() {
        System.out.println("\n=== Test: Unknown Event Type ===");

        // Create an unknown event type
        String unknownEvent = "This is not a valid event";

        try {
            handler.handleRequest(unknownEvent, mockContext);
            fail("Should have thrown exception for unknown event type");
        } catch (Exception e) {
            assertTrue("Should throw RuntimeException",
                    e instanceof RuntimeException);
            System.out.println("Correctly threw exception for unknown event type");
        }

        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleNullContext() throws Exception {
        System.out.println("\n=== Test: Null Context ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/tasks");

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("null-context-test");
        event.setRequestContext(requestContext);

        // Handle with null context
        Object response = handler.handleRequest(event, null);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());

        System.out.println("Handled null context gracefully");
        System.out.println("✓ Test passed");
    }

    // ========================================
    // Integration Test - All Event Types
    // ========================================

    @Test
    public void testAllEventTypesIntegration() throws Exception {
        System.out.println("\n=== Integration Test: All Event Types ===");

        int testsPassed = 0;
        int totalTests = 3;

        // Test 1: API Gateway
        try {
            APIGatewayProxyRequestEvent apiEvent = new APIGatewayProxyRequestEvent();
            apiEvent.setHttpMethod("POST");
            apiEvent.setPath("/tasks");
            apiEvent.setBody("{\"test\":\"integration\"}");
            APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                    new APIGatewayProxyRequestEvent.ProxyRequestContext();
            ctx.setRequestId("integration-api");
            apiEvent.setRequestContext(ctx);

            Object apiResponse = handler.handleRequest(apiEvent, mockContext);
            assertNotNull(apiResponse);
            testsPassed++;
            System.out.println("  ✓ API Gateway integration test passed");
        } catch (Exception e) {
            System.err.println("  ✗ API Gateway integration test failed: " + e.getMessage());
        }

        // Test 2: SQS
        try {
            SQSEvent sqsEvent = new SQSEvent();
            SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
            msg.setMessageId("integration-sqs");
            msg.setBody("{\"test\":\"integration\"}");
            sqsEvent.setRecords(Arrays.asList(msg));

            Object sqsResponse = handler.handleRequest(sqsEvent, mockContext);
            assertNotNull(sqsResponse);
            assertTrue("SQS response should be SQSBatchResponse",
                    sqsResponse instanceof com.amazonaws.services.lambda.runtime.events.SQSBatchResponse);
            testsPassed++;
            System.out.println("  ✓ SQS integration test passed");
        } catch (Exception e) {
            System.err.println("  ✗ SQS integration test failed: " + e.getMessage());
        }

        // Test 3: EventBridge
        try {
            ScheduledEvent ebEvent = new ScheduledEvent();
            ebEvent.setId("integration-eb");
            ebEvent.setSource("integration.test");
            ebEvent.setDetailType("Test");
            ebEvent.setDetail(new HashMap<>());

            Object ebResponse = handler.handleRequest(ebEvent, mockContext);
            assertEquals("OK", ebResponse);
            testsPassed++;
            System.out.println("  ✓ EventBridge integration test passed");
        } catch (Exception e) {
            System.err.println("  ✗ EventBridge integration test failed: " + e.getMessage());
        }

        System.out.println("\nIntegration Test Summary: " + testsPassed + "/" + totalTests + " tests passed");
        assertEquals("All integration tests should pass", totalTests, testsPassed);
        System.out.println("✓ Integration test passed");
    }
}

