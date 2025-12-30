package com.project.task.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class UnifiedTaskHandlerTest {

    private UnifiedTaskHandler handler;
    private ObjectMapper objectMapper;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();
        objectMapper = new ObjectMapper();

        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-12345");
        when(mockContext.getFunctionName()).thenReturn("task-service-function");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
    }

    private Map<String, Object> convertToMap(Object event) {
        return objectMapper.convertValue(event, Map.class);
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
        event.setResource("/ping");

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("ping-test-001");
        event.setRequestContext(requestContext);

        // Convert to Map as AWS Lambda does
        Map<String, Object> eventMap = convertToMap(event);
        Object response = handler.handleRequest(eventMap, mockContext);

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
        event.setResource("/nonexistent");

        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("not-found-test");
        event.setRequestContext(requestContext);

        // Convert to Map as AWS Lambda does
        Map<String, Object> eventMap = convertToMap(event);
        Object response = handler.handleRequest(eventMap, mockContext);

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
        message.setEventSource("aws:sqs");
        message.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:task-queue");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("SentTimestamp", String.valueOf(System.currentTimeMillis()));
        message.setAttributes(attributes);

        event.setRecords(Arrays.asList(message));

        // Convert to Map as AWS Lambda does
        Map<String, Object> eventMap = convertToMap(event);
        Object response = handler.handleRequest(eventMap, mockContext);

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

        // Create event as Map (mimicking AWS Lambda input)
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("id", "scheduled-event-123");
        eventMap.put("detail-type", "Scheduled Event");  // Note: hyphenated
        eventMap.put("source", "aws.events");
        eventMap.put("time", DateTime.now().toString());
        eventMap.put("region", "us-east-1");
        eventMap.put("account", "123456789012");
        eventMap.put("resources", Arrays.asList());

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskType", "scheduled");
        eventMap.put("detail", detail);

        Object response = handler.handleRequest(eventMap, mockContext);

        assertNotNull(response);
        assertEquals("OK", response);

        System.out.println("✓ Test passed");
    }

    @Test
    public void testHandleEventBridgeEvent_Custom() {
        System.out.println("\n=== Test: EventBridge Custom Event ===");

        // Create event as Map (mimicking AWS Lambda input)
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("id", "custom-event-456");
        eventMap.put("detail-type", "custom-event-OrderCompleted");  // Note: hyphenated and custom prefix
        eventMap.put("source", "com.custom.orders");  // Note: com.custom prefix
        eventMap.put("time", DateTime.now().toString());
        eventMap.put("region", "us-east-1");
        eventMap.put("account", "123456789012");
        eventMap.put("resources", Arrays.asList());

        // Detail should contain TaskRequestDTO fields
        Map<String, Object> detail = new HashMap<>();
        detail.put("name", "Process Completed Order");
        detail.put("description", "Handle order completion workflow");
        detail.put("status", "TODO");
        eventMap.put("detail", detail);

        Object response = handler.handleRequest(eventMap, mockContext);

        assertNotNull(response);
        assertEquals("OK", response);

        System.out.println("✓ Test passed");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    public void testHandleNullInput() {
        System.out.println("\n=== Test: Null Input ===");

        // Null input should return error response
        Object response = handler.handleRequest(null, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response;

        assertEquals("Task processing failed", errorResponse.get("errorMessage"));
        assertEquals("IllegalArgumentException", errorResponse.get("errorType"));
        assertEquals("Input event cannot be null", errorResponse.get("errorReason"));
        assertNotNull(errorResponse.get("requestId"));
        assertNotNull(errorResponse.get("timestamp"));

        System.out.println("✓ Test passed - error response validated");
    }

    @Test
    public void testHandleInvalidEventType() {
        System.out.println("\n=== Test: Invalid Event Type ===");

        Map<String, Object> invalidEvent = new HashMap<>();
        invalidEvent.put("unknown", "event");

        // Invalid event should return error response
        Object response = handler.handleRequest(invalidEvent, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = (Map<String, Object>) response;

        assertEquals("Task processing failed", errorResponse.get("errorMessage"));
        assertEquals("IllegalArgumentException", errorResponse.get("errorType"));
        assertTrue(((String) errorResponse.get("errorReason")).contains("Unsupported event structure"));
        assertNotNull(errorResponse.get("requestId"));
        assertNotNull(errorResponse.get("timestamp"));

        System.out.println("✓ Test passed - error response validated");
    }
}

