package com.project.task.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.*;
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
 * Simplified Unit tests for UnifiedTaskHandler
 * Tests basic functionality for all three event sources
 */
public class UnifiedTaskHandlerTest {

    private UnifiedTaskHandler handler;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();

        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-12345");
        when(mockContext.getFunctionName()).thenReturn("task-service-function");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
    }

    // ========================================
    // API Gateway Event Tests
    // ========================================

    @Test
    public void testHandleApiGatewayEvent_Ping() {
        System.out.println("\n=== Test: API Gateway /ping ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/ping");

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("ping-test-001");
        event.setRequestContext(requestContext);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof APIGatewayProxyResponseEvent);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(200), apiResponse.getStatusCode());
        assertTrue(apiResponse.getBody().contains("healthy"));

        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleApiGatewayEvent_NotFound() {
        System.out.println("\n=== Test: API Gateway 404 ===");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/nonexistent");

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("not-found-test");
        event.setRequestContext(requestContext);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof APIGatewayProxyResponseEvent);
        APIGatewayProxyResponseEvent apiResponse = (APIGatewayProxyResponseEvent) response;
        assertEquals(Integer.valueOf(404), apiResponse.getStatusCode());

        System.out.println("✓ Test passed");
    }

    // ========================================
    // SQS Event Tests
    // ========================================

    @Test
    public void testHandleSqsEvent_Success() {
        System.out.println("\n=== Test: SQS Event - Success ===");

        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("sqs-message-123");
        message.setReceiptHandle("receipt-handle-abc");
        message.setBody("{\"name\":\"Process Order\",\"description\":\"Order ORD-001 processing\",\"status\":\"TODO\"}");
        message.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:task-queue");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("SentTimestamp", String.valueOf(System.currentTimeMillis()));
        message.setAttributes(attributes);

        event.setRecords(Arrays.asList(message));

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof SQSBatchResponse);
        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals(0, batchResponse.getBatchItemFailures().size());

        System.out.println("✓ Test passed");
    }

    // ========================================
    // EventBridge Event Tests
    // ========================================

    @Test
    public void testHandleEventBridgeEvent_Scheduled() {
        System.out.println("\n=== Test: EventBridge Scheduled Event ===");

        ScheduledEvent event = new ScheduledEvent();
        event.setId("scheduled-event-123");
        event.setDetailType("Scheduled Event");
        event.setSource("aws.events");
        event.setTime(DateTime.now());

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskType", "scheduled");
        event.setDetail(detail);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertEquals("OK", response);

        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleEventBridgeEvent_Custom() {
        System.out.println("\n=== Test: EventBridge Custom Event ===");

        ScheduledEvent event = new ScheduledEvent();
        event.setId("custom-event-456");
        event.setDetailType("OrderCompleted");
        event.setSource("com.project.orders");
        event.setTime(DateTime.now());

        // Detail should contain TaskRequestDTO fields
        Map<String, Object> detail = new HashMap<>();
        detail.put("name", "Process Completed Order");
        detail.put("description", "Handle order completion workflow");
        detail.put("status", "TODO");
        event.setDetail(detail);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertEquals("OK", response);

        System.out.println("✓ Test passed");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test(expected = RuntimeException.class)
    public void testHandleNullInput() {
        System.out.println("\n=== Test: Null Input ===");

        // Null input should throw RuntimeException
        handler.handleRequest(null, mockContext);

        System.out.println("✓ Test passed - exception thrown as expected");
    }

    @Test(expected = RuntimeException.class)
    public void testHandleInvalidEventType() {
        System.out.println("\n=== Test: Invalid Event Type ===");

        Map<String, Object> invalidEvent = new HashMap<>();
        invalidEvent.put("unknown", "event");

        // Invalid event should throw RuntimeException
        handler.handleRequest(invalidEvent, mockContext);

        System.out.println("✓ Test passed - exception thrown as expected");
    }
}

