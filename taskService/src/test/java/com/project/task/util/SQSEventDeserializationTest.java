package com.project.task.util;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test SQS Event Deserialization
 * Validates that the EventDeserializer can properly route SQS events
 */
public class SQSEventDeserializationTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testDeserializeSQSEvent_ExactUserPayload() {
        System.out.println("\n=== Test: Deserialize User's Exact SQS Payload ===");

        // Create the exact JSON structure the user provided
        String sqsJson = "{\n" +
                "    \"Records\": [\n" +
                "        {\n" +
                "            \"messageId\": \"059f36b4-87a3-44ab-83d2-661975830a7d\",\n" +
                "            \"receiptHandle\": \"AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...\",\n" +
                "            \"body\": \"{\\\"name\\\":\\\"task name\\\",\\\"description\\\":\\\"task 1 description\\\"}\",\n" +
                "            \"attributes\": {\n" +
                "                \"ApproximateReceiveCount\": \"1\",\n" +
                "                \"SentTimestamp\": \"1545082649183\",\n" +
                "                \"SenderId\": \"AIDAIENQZJOLO23YVJ4VO\",\n" +
                "                \"ApproximateFirstReceiveTimestamp\": \"1545082649185\"\n" +
                "            },\n" +
                "            \"messageAttributes\": {\n" +
                "                \"myAttribute\": {\n" +
                "                    \"stringValue\": \"myValue\",\n" +
                "                    \"stringListValues\": [],\n" +
                "                    \"binaryListValues\": [],\n" +
                "                    \"dataType\": \"String\"\n" +
                "                }\n" +
                "            },\n" +
                "            \"md5OfBody\": \"e4e68fb7bd0e697a0ae8f1bb342846b3\",\n" +
                "            \"eventSource\": \"aws:sqs\",\n" +
                "            \"eventSourceARN\": \"arn:aws:sqs:us-east-2:123456789012:my-queue\",\n" +
                "            \"awsRegion\": \"us-east-2\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        try {
            // Step 1: Parse JSON to LinkedHashMap (simulating Lambda's behavior)
            System.out.println("Step 1: Parsing JSON to LinkedHashMap...");
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = objectMapper.readValue(sqsJson, LinkedHashMap.class);

            System.out.println("  ✓ JSON parsed successfully");
            System.out.println("  Map keys: " + inputMap.keySet());

            // Verify Records field exists
            assertTrue("Records field should exist in map", inputMap.containsKey("Records"));

            Object records = inputMap.get("Records");
            assertNotNull("Records should not be null", records);
            assertTrue("Records should be a List", records instanceof List);

            @SuppressWarnings("unchecked")
            List<Object> recordsList = (List<Object>) records;
            assertEquals("Should have 1 record", 1, recordsList.size());

            System.out.println("  ✓ Records field validated: " + recordsList.size() + " records");

            // Step 2: Deserialize using EventDeserializer
            System.out.println("\nStep 2: Deserializing to SQSEvent using EventDeserializer...");
            SQSEvent sqsEvent = EventDeserializer.toSqsEvent(inputMap);

            System.out.println("  ✓ SQSEvent deserialized successfully");

            // Step 3: Validate the deserialized event
            System.out.println("\nStep 3: Validating deserialized SQSEvent...");

            assertNotNull("SQSEvent should not be null", sqsEvent);
            assertNotNull("SQSEvent.getRecords() should not be null", sqsEvent.getRecords());
            assertFalse("Records list should not be empty", sqsEvent.getRecords().isEmpty());
            assertEquals("Should have exactly 1 record", 1, sqsEvent.getRecords().size());

            System.out.println("  ✓ SQSEvent structure validated");

            // Step 4: Validate the message details
            SQSEvent.SQSMessage message = sqsEvent.getRecords().get(0);

            assertNotNull("Message should not be null", message);
            assertEquals("Message ID should match",
                    "059f36b4-87a3-44ab-83d2-661975830a7d",
                    message.getMessageId());

            assertNotNull("Message body should not be null", message.getBody());
            assertTrue("Body should contain task name",
                    message.getBody().contains("task name"));

            assertEquals("Event source should be aws:sqs",
                    "aws:sqs",
                    message.getEventSource());

            assertEquals("AWS region should match",
                    "us-east-2",
                    message.getAwsRegion());

            System.out.println("  ✓ Message details validated:");
            System.out.println("    - Message ID: " + message.getMessageId());
            System.out.println("    - Event Source: " + message.getEventSource());
            System.out.println("    - AWS Region: " + message.getAwsRegion());
            System.out.println("    - Body: " + message.getBody());

            // Step 5: Validate attributes
            assertNotNull("Attributes should not be null", message.getAttributes());
            assertFalse("Attributes should not be empty", message.getAttributes().isEmpty());

            System.out.println("\n  ✓ Attributes validated:");
            message.getAttributes().forEach((key, value) ->
                    System.out.println("    - " + key + ": " + value)
            );

            // Step 6: Validate message attributes
            assertNotNull("Message attributes should not be null", message.getMessageAttributes());
            assertFalse("Message attributes should not be empty", message.getMessageAttributes().isEmpty());
            assertTrue("Should have myAttribute", message.getMessageAttributes().containsKey("myAttribute"));

            SQSEvent.MessageAttribute myAttr = message.getMessageAttributes().get("myAttribute");
            assertNotNull("myAttribute should not be null", myAttr);
            assertEquals("myAttribute value should match", "myValue", myAttr.getStringValue());
            assertEquals("myAttribute dataType should match", "String", myAttr.getDataType());

            System.out.println("\n  ✓ Message attributes validated:");
            System.out.println("    - myAttribute value: " + myAttr.getStringValue());
            System.out.println("    - myAttribute dataType: " + myAttr.getDataType());

            // Step 7: Parse and validate the task data in body
            System.out.println("\nStep 7: Parsing task data from message body...");
            @SuppressWarnings("unchecked")
            Map<String, Object> taskData = objectMapper.readValue(message.getBody(), HashMap.class);

            assertTrue("Task should have 'name' field", taskData.containsKey("name"));
            assertTrue("Task should have 'description' field", taskData.containsKey("description"));

            assertEquals("Task name should match", "task name", taskData.get("name"));
            assertEquals("Task description should match", "task 1 description", taskData.get("description"));

            System.out.println("  ✓ Task data validated:");
            System.out.println("    - name: " + taskData.get("name"));
            System.out.println("    - description: " + taskData.get("description"));

            System.out.println("\n✅ All validations passed! SQS event deserialization works correctly.");

        } catch (Exception e) {
            System.err.println("\n❌ Test failed with error: " + e.getMessage());
            e.printStackTrace();
            fail("SQS event deserialization failed: " + e.getMessage());
        }
    }

    @Test
    public void testDeserializeSQSEvent_RecordsFieldExists() {
        System.out.println("\n=== Test: Verify Records Field Is Not Null ===");

        // Create minimal SQS event structure
        Map<String, Object> inputMap = new HashMap<>();
        List<Map<String, Object>> records = List.of(
                createMinimalSQSMessage("test-message-1", "{\"name\":\"Test Task\"}")
        );
        inputMap.put("Records", records);

        try {
            SQSEvent sqsEvent = EventDeserializer.toSqsEvent(inputMap);

            assertNotNull("SQSEvent should not be null", sqsEvent);
            assertNotNull("Records should not be null", sqsEvent.getRecords());
            assertEquals("Should have 1 record", 1, sqsEvent.getRecords().size());

            System.out.println("✅ Records field properly deserialized");

        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testDeserializeSQSEvent_MultipleRecords() {
        System.out.println("\n=== Test: Deserialize Multiple Records ===");

        Map<String, Object> inputMap = new HashMap<>();
        List<Map<String, Object>> records = List.of(
                createMinimalSQSMessage("msg-1", "{\"name\":\"Task 1\"}"),
                createMinimalSQSMessage("msg-2", "{\"name\":\"Task 2\"}"),
                createMinimalSQSMessage("msg-3", "{\"name\":\"Task 3\"}")
        );
        inputMap.put("Records", records);

        try {
            SQSEvent sqsEvent = EventDeserializer.toSqsEvent(inputMap);

            assertNotNull("SQSEvent should not be null", sqsEvent);
            assertNotNull("Records should not be null", sqsEvent.getRecords());
            assertEquals("Should have 3 records", 3, sqsEvent.getRecords().size());

            System.out.println("✅ Multiple records deserialized correctly: " + sqsEvent.getRecords().size());

        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeserializeSQSEvent_NullRecords_ThrowsException() {
        System.out.println("\n=== Test: Null Records Should Throw Exception ===");

        // Create map without Records field
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("SomeOtherField", "value");

        // This should throw IllegalArgumentException
        EventDeserializer.toSqsEvent(inputMap);

        fail("Should have thrown IllegalArgumentException");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeserializeSQSEvent_InvalidInput_ThrowsException() {
        System.out.println("\n=== Test: Invalid Input Should Throw Exception ===");

        // Pass invalid input
        EventDeserializer.toSqsEvent("invalid-string-input");

        fail("Should have thrown IllegalArgumentException");
    }

    // Helper method to create minimal SQS message structure
    private Map<String, Object> createMinimalSQSMessage(String messageId, String body) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", messageId);
        message.put("body", body);
        message.put("eventSource", "aws:sqs");
        message.put("eventSourceARN", "arn:aws:sqs:us-east-1:123456789012:test-queue");
        message.put("awsRegion", "us-east-1");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("SentTimestamp", String.valueOf(System.currentTimeMillis()));
        message.put("attributes", attributes);

        return message;
    }
}

