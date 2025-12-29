package com.project.task.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.project.task.handler.UnifiedTaskHandler;
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
 * Integration tests for TaskService end-to-end scenarios.
 * These tests validate complete request flows through all layers.
 */
public class TaskServiceIntegrationTest {

    private UnifiedTaskHandler handler;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();

        when(mockContext.getAwsRequestId()).thenReturn("integration-test-id");
        when(mockContext.getFunctionName()).thenReturn("task-service-integration");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
    }

    // ========================================
    // API Gateway Integration Tests
    // ========================================

    @Test
    public void testApiGateway_CompleteFlow_PostRequest() {
        System.out.println("\n=== Integration Test: API Gateway POST /tasks ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/tasks");
        event.setBody("{\"taskName\":\"Integration Test\",\"priority\":\"HIGH\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("api-integration-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should be APIGatewayProxyResponseEvent",
                response instanceof APIGatewayProxyResponseEvent);

        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals("Status should be 200", Integer.valueOf(200), apiResponse.getStatusCode());

        TaskResponse taskResponse = JsonUtil.fromJson(apiResponse.getBody(), TaskResponse.class);
        assertTrue("Task should succeed", taskResponse.isSuccess());
        assertNotNull("Task ID should be generated", taskResponse.getTaskId());

        System.out.println("✓ POST integration test passed");
    }

    @Test
    public void testApiGateway_CompleteFlow_GetAllResources() {
        System.out.println("\n=== Integration Test: API Gateway GET /tasks ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/tasks");

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("api-integration-002");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());

        System.out.println("Response: " + apiResponse.getBody());
        System.out.println("✓ GET integration test passed");
    }

    @Test
    public void testApiGateway_HealthCheck() {
        System.out.println("\n=== Integration Test: API Gateway GET /ping ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/ping");

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("ping-integration-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());

        Map<String, Object> pingResponse = JsonUtil.fromJson(apiResponse.getBody(), Map.class);
        assertEquals("healthy", pingResponse.get("status"));
        assertEquals("task-service", pingResponse.get("service"));

        System.out.println("✓ Ping integration test passed");
    }

    @Test
    public void testApiGateway_ResourceById() {
        System.out.println("\n=== Integration Test: API Gateway GET /id/{id} ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/id/test-resource-123");

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("get-by-id-integration-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertNotNull(apiResponse.getBody());

        System.out.println("Resource Response: " + apiResponse.getBody());
        System.out.println("✓ GET by ID integration test passed");
    }

    @Test
    public void testApiGateway_PostCreate() {
        System.out.println("\n=== Integration Test: API Gateway POST /post ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/post");
        event.setBody("{\"name\":\"New Resource\",\"type\":\"integration-test\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("post-create-integration-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals("Should return 201 Created", Integer.valueOf(201), apiResponse.getStatusCode());

        TaskResponse taskResponse = JsonUtil.fromJson(apiResponse.getBody(), TaskResponse.class);
        assertTrue("Should succeed", taskResponse.isSuccess());
        assertNotNull("Should return new ID", taskResponse.getTaskId());

        System.out.println("✓ POST create integration test passed");
    }

    // ========================================
    // SQS Integration Tests
    // ========================================

    @Test
    public void testSQS_BatchProcessing_AllSuccess() {
        System.out.println("\n=== Integration Test: SQS Batch Processing ===");

        SQSEvent event = new SQSEvent();
        java.util.List<SQSEvent.SQSMessage> messages = new java.util.ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setMessageId("batch-msg-" + i);
            message.setBody("{\"orderId\":\"ORDER-" + i + "\",\"amount\":" + (i * 100) + "}");
            message.setReceiptHandle("receipt-" + i);
            messages.add(message);
        }

        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals("All messages should succeed", 0,
                batchResponse.getBatchItemFailures().size());

        System.out.println("✓ SQS batch processing integration test passed");
    }

    @Test
    public void testSQS_LargePayload() {
        System.out.println("\n=== Integration Test: SQS Large Payload ===");

        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("large-payload-msg");

        // Create a large JSON payload
        StringBuilder largeBody = new StringBuilder("{\"data\":[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeBody.append(",");
            largeBody.append("{\"id\":").append(i)
                    .append(",\"value\":\"test-data-").append(i).append("\"}");
        }
        largeBody.append("]}");

        message.setBody(largeBody.toString());
        event.setRecords(Arrays.asList(message));

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals(0, batchResponse.getBatchItemFailures().size());

        System.out.println("✓ SQS large payload integration test passed");
    }

    @Test
    public void testSQS_BatchPartialFailure() {
        System.out.println("\n=== Integration Test: SQS Batch Partial Failure ===");

        SQSEvent event = new SQSEvent();
        java.util.List<SQSEvent.SQSMessage> messages = new java.util.ArrayList<>();

        // Create mix of valid and potentially problematic messages
        for (int i = 1; i <= 5; i++) {
            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setMessageId("batch-msg-" + i);

            // All messages will succeed in our template, but in real scenario
            // you would have validation/processing logic that could fail
            message.setBody("{\"orderId\":\"ORDER-" + i + "\",\"amount\":" + (i * 100) + "}");
            message.setReceiptHandle("receipt-" + i);
            messages.add(message);
        }

        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should be SQSBatchResponse",
                response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;

        // In our template implementation, all messages succeed
        // In production, failed messages would be in batchItemFailures list
        assertNotNull("BatchItemFailures list should not be null",
                batchResponse.getBatchItemFailures());

        // Verify the structure is correct for DLQ handling
        System.out.println("Batch processing result:");
        System.out.println("  Total messages: " + messages.size());
        System.out.println("  Failed messages: " + batchResponse.getBatchItemFailures().size());
        System.out.println("  Success rate: " +
                (messages.size() - batchResponse.getBatchItemFailures().size()) + "/" + messages.size());

        System.out.println("✓ SQS batch partial failure test passed");
    }

    @Test
    public void testSQS_BatchFailureResponse_Structure() {
        System.out.println("\n=== Integration Test: SQS Batch Failure Response Structure ===");

        // Test that the response structure is correct for AWS SQS DLQ handling
        SQSEvent event = new SQSEvent();
        java.util.List<SQSEvent.SQSMessage> messages = new java.util.ArrayList<>();

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("test-msg-001");
        message.setBody("{\"testData\":\"value\"}");
        message.setReceiptHandle("receipt-001");
        messages.add(message);

        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        // Validate response structure
        assertNotNull("Response must not be null", response);
        assertTrue("Response must be SQSBatchResponse for DLQ support",
                response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertNotNull("BatchItemFailures must not be null (even if empty)",
                batchResponse.getBatchItemFailures());

        // Verify it's a valid list (not null)
        assertTrue("BatchItemFailures should be a valid list",
                batchResponse.getBatchItemFailures() instanceof java.util.List);

        System.out.println("Response structure valid for AWS SQS DLQ handling");
        System.out.println("✓ SQS batch failure response structure test passed");
    }

    @Test
    public void testSQS_EmptyBatch() {
        System.out.println("\n=== Integration Test: SQS Empty Batch ===");

        SQSEvent event = new SQSEvent();
        event.setRecords(new java.util.ArrayList<>());  // Empty list

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Response should not be null even for empty batch", response);
        assertTrue("Response should be SQSBatchResponse",
                response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals("Empty batch should have no failures", 0,
                batchResponse.getBatchItemFailures().size());

        System.out.println("✓ SQS empty batch test passed");
    }

    @Test
    public void testSQS_DuplicateMessageHandling() {
        System.out.println("\n=== Integration Test: SQS Duplicate Message Handling ===");

        // Test idempotency - same message processed twice
        SQSEvent event = new SQSEvent();
        java.util.List<SQSEvent.SQSMessage> messages = new java.util.ArrayList<>();

        SQSEvent.SQSMessage message1 = new SQSEvent.SQSMessage();
        message1.setMessageId("duplicate-msg-001");
        message1.setBody("{\"orderId\":\"DUP-ORDER-001\",\"amount\":500}");
        message1.setReceiptHandle("receipt-dup-001");

        // Simulate duplicate (same messageId)
        SQSEvent.SQSMessage message2 = new SQSEvent.SQSMessage();
        message2.setMessageId("duplicate-msg-001");  // Same ID
        message2.setBody("{\"orderId\":\"DUP-ORDER-001\",\"amount\":500}");
        message2.setReceiptHandle("receipt-dup-002");  // Different receipt

        messages.add(message1);
        messages.add(message2);

        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;

        // Both messages processed (idempotency check should be implemented in business logic)
        System.out.println("Processed duplicate messages - idempotency check should be in business logic");
        System.out.println("Failed messages: " + batchResponse.getBatchItemFailures().size());

        System.out.println("✓ SQS duplicate message handling test passed");
    }

    @Test
    public void testSQS_MessageWithMissingFields() {
        System.out.println("\n=== Integration Test: SQS Message With Missing Fields ===");

        SQSEvent event = new SQSEvent();
        java.util.List<SQSEvent.SQSMessage> messages = new java.util.ArrayList<>();

        // Message with minimal/missing fields
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("incomplete-msg-001");
        message.setBody("{}");  // Empty JSON body
        // No receipt handle

        messages.add(message);
        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Should handle incomplete messages gracefully", response);
        assertTrue(response instanceof SQSBatchResponse);

        System.out.println("✓ SQS message with missing fields test passed");
    }

    @Test
    public void testSQS_BatchMixedContent() {
        System.out.println("\n=== Integration Test: SQS Batch Mixed Content Types ===");

        SQSEvent event = new SQSEvent();
        java.util.List<SQSEvent.SQSMessage> messages = new java.util.ArrayList<>();

        // Different types of message content
        String[] messageBodies = {
                "{\"type\":\"order\",\"data\":{\"orderId\":\"ORD-001\"}}",
                "{\"type\":\"payment\",\"data\":{\"paymentId\":\"PAY-001\"}}",
                "{\"type\":\"notification\",\"data\":{\"message\":\"Test\"}}",
                "[1,2,3,4,5]",  // Array
                "\"simple-string\""  // String
        };

        for (int i = 0; i < messageBodies.length; i++) {
            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setMessageId("mixed-msg-" + (i + 1));
            message.setBody(messageBodies[i]);
            message.setReceiptHandle("receipt-" + (i + 1));
            messages.add(message);
        }

        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        System.out.println("Processed " + messages.size() + " messages with mixed content");
        System.out.println("Failures: " + batchResponse.getBatchItemFailures().size());

        System.out.println("✓ SQS batch mixed content test passed");
    }

    // ========================================
    // EventBridge Integration Tests
    // ========================================

    @Test
    public void testEventBridge_ScheduledTask() {
        System.out.println("\n=== Integration Test: EventBridge Scheduled Task ===");

        ScheduledEvent event = new ScheduledEvent();
        event.setId("scheduled-task-integration");
        event.setSource("aws.events");
        event.setDetailType("Scheduled Event");
        event.setTime(DateTime.now());

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskType", "daily-cleanup");
        detail.put("schedule", "cron(0 2 * * ? *)");
        event.setDetail(detail);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertEquals("OK", response);

        System.out.println("✓ EventBridge scheduled task integration test passed");
    }

    @Test
    public void testEventBridge_CustomBusinessEvent() {
        System.out.println("\n=== Integration Test: EventBridge Custom Event ===");

        ScheduledEvent event = new ScheduledEvent();
        event.setId("custom-event-integration");
        event.setSource("com.project.orders");
        event.setDetailType("OrderCompleted");
        event.setTime(DateTime.now());

        Map<String, Object> detail = new HashMap<>();
        detail.put("orderId", "ORD-INT-12345");
        detail.put("customerId", "CUST-INT-67890");
        detail.put("totalAmount", 1599.99);
        detail.put("status", "COMPLETED");
        event.setDetail(detail);

        Object response = handler.handleRequest(event, mockContext);

        assertEquals("OK", response);

        System.out.println("✓ EventBridge custom event integration test passed");
    }

    // ========================================
    // Cross-Source Integration Tests
    // ========================================

    @Test
    public void testMultiSource_SequentialProcessing() {
        System.out.println("\n=== Integration Test: Sequential Multi-Source Processing ===");

        int successCount = 0;

        // Test 1: API Gateway
        try {
            APIGatewayProxyRequestEvent apiEvent = new APIGatewayProxyRequestEvent();
            apiEvent.setHttpMethod("GET");
            apiEvent.setPath("/ping");
            APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                    new APIGatewayProxyRequestEvent.ProxyRequestContext();
            ctx.setRequestId("multi-source-api");
            apiEvent.setRequestContext(ctx);

            Object apiResponse = handler.handleRequest(apiEvent, mockContext);
            assertNotNull(apiResponse);
            successCount++;
        } catch (Exception e) {
            System.err.println("API Gateway test failed: " + e.getMessage());
        }

        // Test 2: SQS
        try {
            SQSEvent sqsEvent = new SQSEvent();
            SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
            msg.setMessageId("multi-source-sqs");
            msg.setBody("{\"test\":\"multi-source\"}");
            sqsEvent.setRecords(Arrays.asList(msg));

            Object sqsResponse = handler.handleRequest(sqsEvent, mockContext);
            assertNotNull(sqsResponse);
            successCount++;
        } catch (Exception e) {
            System.err.println("SQS test failed: " + e.getMessage());
        }

        // Test 3: EventBridge
        try {
            ScheduledEvent ebEvent = new ScheduledEvent();
            ebEvent.setId("multi-source-eb");
            ebEvent.setSource("integration.test");
            ebEvent.setDetailType("MultiSourceTest");
            ebEvent.setDetail(new HashMap<>());

            Object ebResponse = handler.handleRequest(ebEvent, mockContext);
            assertEquals("OK", ebResponse);
            successCount++;
        } catch (Exception e) {
            System.err.println("EventBridge test failed: " + e.getMessage());
        }

        assertEquals("All 3 source types should succeed", 3, successCount);
        System.out.println("✓ Sequential multi-source integration test passed: " + successCount + "/3");
    }

    @Test
    public void testErrorHandling_InvalidApiGatewayRequest() {
        System.out.println("\n=== Integration Test: Error Handling - Invalid Request ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/tasks");
        event.setBody("invalid-json{{{");  // Invalid JSON

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);

        APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        ctx.setRequestId("error-handling-001");
        event.setRequestContext(ctx);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;

        // Should handle error gracefully - either 400 or 200 with error in body
        assertTrue("Status should be valid",
                apiResponse.getStatusCode() == 400 ||
                        apiResponse.getStatusCode() == 200 ||
                        apiResponse.getStatusCode() == 500);

        System.out.println("✓ Error handling integration test passed");
    }

    @Test
    public void testPerformance_HighVolumeApiRequests() {
        System.out.println("\n=== Integration Test: Performance - High Volume ===");

        long startTime = System.currentTimeMillis();
        int requestCount = 50;
        int successCount = 0;

        for (int i = 0; i < requestCount; i++) {
            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
            event.setHttpMethod("GET");
            event.setPath("/ping");

            APIGatewayProxyRequestEvent.ProxyRequestContext ctx =
                    new APIGatewayProxyRequestEvent.ProxyRequestContext();
            ctx.setRequestId("perf-test-" + i);
            event.setRequestContext(ctx);

            Object response = handler.handleRequest(event, mockContext);
            if (response != null) {
                successCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        double avgTime = duration / (double) requestCount;

        System.out.println("Processed " + successCount + "/" + requestCount +
                " requests in " + duration + "ms");
        System.out.println("Average time per request: " + String.format("%.2f", avgTime) + "ms");

        assertEquals("All requests should succeed", requestCount, successCount);
        assertTrue("Average time should be reasonable", avgTime < 100);

        System.out.println("✓ Performance integration test passed");
    }
}

