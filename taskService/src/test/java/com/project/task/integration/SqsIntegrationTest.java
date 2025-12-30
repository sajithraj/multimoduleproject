package com.project.task.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.project.task.handler.UnifiedTaskHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests for SQS flows
 */
public class SqsIntegrationTest {

    private UnifiedTaskHandler handler;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();

        when(mockContext.getAwsRequestId()).thenReturn("sqs-test-id");
        when(mockContext.getFunctionName()).thenReturn("task-service-sqs-integration");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);
    }

    @Test
    public void testSQS_SingleMessage() {
        System.out.println("\n=== Test: SQS Single Message ===");

        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("single-msg-001");
        message.setBody("{\"name\":\"Test Task\",\"description\":\"Single message test\",\"status\":\"TODO\"}");
        message.setReceiptHandle("receipt-001");
        event.setRecords(Arrays.asList(message));

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof SQSBatchResponse);
        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals(0, batchResponse.getBatchItemFailures().size());

        System.out.println("✓ Single message test passed");
    }

    @Test
    public void testSQS_BatchProcessing() {
        System.out.println("\n=== Test: SQS Batch Processing ===");

        SQSEvent event = new SQSEvent();
        List<SQSEvent.SQSMessage> messages = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setMessageId("batch-msg-" + i);
            message.setBody("{\"name\":\"Batch Task " + i + "\",\"description\":\"Order processing task\",\"status\":\"TODO\"}");
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

        System.out.println("✓ Batch processing test passed");
    }

    @Test
    public void testSQS_LargePayload() {
        System.out.println("\n=== Test: SQS Large Payload ===");

        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("large-payload-msg");

        // Create a large description with 100 items
        StringBuilder largeDescription = new StringBuilder("Processing items: ");
        for (int i = 0; i < 100; i++) {
            if (i > 0) largeDescription.append(", ");
            largeDescription.append("item-").append(i);
        }

        String largeBody = "{\"name\":\"Large Batch Task\",\"description\":\"" +
                largeDescription.toString() + "\",\"status\":\"TODO\"}";

        message.setBody(largeBody);
        event.setRecords(Arrays.asList(message));

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals(0, batchResponse.getBatchItemFailures().size());

        System.out.println("✓ Large payload test passed");
    }

    @Test
    public void testSQS_EmptyBatch() {
        System.out.println("\n=== Test: SQS Empty Batch ===");

        SQSEvent event = new SQSEvent();
        event.setRecords(new ArrayList<>());

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Response should not be null even for empty batch", response);
        assertTrue(response instanceof SQSBatchResponse);
        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertEquals(0, batchResponse.getBatchItemFailures().size());

        System.out.println("✓ Empty batch test passed");
    }

    @Test
    public void testSQS_DuplicateMessages() {
        System.out.println("\n=== Test: SQS Duplicate Messages ===");

        SQSEvent event = new SQSEvent();
        List<SQSEvent.SQSMessage> messages = new ArrayList<>();

        SQSEvent.SQSMessage message1 = new SQSEvent.SQSMessage();
        message1.setMessageId("duplicate-msg-001");
        message1.setBody("{\"orderId\":\"DUP-ORDER-001\",\"amount\":500}");
        message1.setReceiptHandle("receipt-dup-001");

        SQSEvent.SQSMessage message2 = new SQSEvent.SQSMessage();
        message2.setMessageId("duplicate-msg-001");
        message2.setBody("{\"orderId\":\"DUP-ORDER-001\",\"amount\":500}");
        message2.setReceiptHandle("receipt-dup-002");

        messages.add(message1);
        messages.add(message2);
        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull(response);
        assertTrue(response instanceof SQSBatchResponse);

        System.out.println("✓ Duplicate messages test passed");
    }

    @Test
    public void testSQS_MessageWithMissingFields() {
        System.out.println("\n=== Test: SQS Message With Missing Fields ===");

        SQSEvent event = new SQSEvent();
        List<SQSEvent.SQSMessage> messages = new ArrayList<>();

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("incomplete-msg-001");
        message.setBody("{}");

        messages.add(message);
        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Should handle incomplete messages gracefully", response);
        assertTrue(response instanceof SQSBatchResponse);

        System.out.println("✓ Missing fields test passed");
    }

    @Test
    public void testSQS_MixedContent() {
        System.out.println("\n=== Test: SQS Mixed Content Types ===");

        SQSEvent event = new SQSEvent();
        List<SQSEvent.SQSMessage> messages = new ArrayList<>();

        String[] messageBodies = {
                "{\"type\":\"order\",\"data\":{\"orderId\":\"ORD-001\"}}",
                "{\"type\":\"payment\",\"data\":{\"paymentId\":\"PAY-001\"}}",
                "{\"type\":\"notification\",\"data\":{\"message\":\"Test\"}}",
                "[1,2,3,4,5]",
                "\"simple-string\""
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
        System.out.println("✓ Mixed content test passed");
    }

    @Test
    public void testSQS_BatchResponseStructure() {
        System.out.println("\n=== Test: SQS Batch Response Structure ===");

        SQSEvent event = new SQSEvent();
        List<SQSEvent.SQSMessage> messages = new ArrayList<>();

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("test-msg-001");
        message.setBody("{\"testData\":\"value\"}");
        message.setReceiptHandle("receipt-001");
        messages.add(message);

        event.setRecords(messages);

        Object response = handler.handleRequest(event, mockContext);

        assertNotNull("Response must not be null", response);
        assertTrue("Response must be SQSBatchResponse", response instanceof SQSBatchResponse);

        SQSBatchResponse batchResponse = (SQSBatchResponse) response;
        assertNotNull("BatchItemFailures must not be null", batchResponse.getBatchItemFailures());
        assertTrue("BatchItemFailures should be a valid list",
                batchResponse.getBatchItemFailures() instanceof List);

        System.out.println("✓ Response structure test passed");
    }
}

