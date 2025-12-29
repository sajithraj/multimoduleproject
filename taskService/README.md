# Task Service Module

## Overview

The **Task Service** is a Lambda function that handles incoming requests from multiple AWS event sources:

- **API Gateway** - HTTP REST API requests
- **SQS** - Message queue events  
- **EventBridge** - Scheduled and custom events

## Architecture

```
┌─────────────────┐
│   API Gateway   │──┐
└─────────────────┘  │
                     │
┌─────────────────┐  │    ┌──────────────────┐
│      SQS        │──┼───▶│  TaskHandler     │
└─────────────────┘  │    │  (Lambda)        │
                     │    └──────────────────┘
┌─────────────────┐  │            │
│  EventBridge    │──┘            ▼
└─────────────────┘         Business Logic
                           (To be implemented)
```

## Features

✅ **Multi-Source Support** - Handles API Gateway, SQS, and EventBridge events  
✅ **Event Detection** - Automatically detects event source type  
✅ **Structured Logging** - JSON logs with Log4j2 and AWS Powertools  
✅ **Request Parsing** - Converts all event types to common `TaskRequest` format  
✅ **Error Handling** - Proper error responses for each event source  
✅ **Unit Tests** - Comprehensive test coverage for all event types  

## Project Structure

```
taskService/
├── src/
│   ├── main/
│   │   ├── java/com/project/task/
│   │   │   ├── handler/
│   │   │   │   └── TaskHandler.java          # Main Lambda handler
│   │   │   ├── model/
│   │   │   │   ├── EventSourceType.java      # Event source enum
│   │   │   │   ├── TaskRequest.java          # Common request model
│   │   │   │   └── TaskResponse.java         # Response model
│   │   │   └── util/
│   │   │       └── EventParser.java          # Event parsing utility
│   │   └── resources/
│   │       └── log4j2.xml                     # Logging configuration
│   └── test/
│       └── java/com/project/task/
│           └── handler/
│               └── TaskHandlerTest.java       # Comprehensive unit tests
└── pom.xml
```

## Event Types

### 1. API Gateway Events

**Example Request:**
```json
{
  "httpMethod": "POST",
  "path": "/tasks",
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "{\"taskName\":\"Process Order\",\"priority\":\"HIGH\"}"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Task received and queued for processing",
  "taskId": "uuid-generated",
  "data": {
    "requestId": "...",
    "sourceType": "API_GATEWAY",
    "receivedAt": 1735455850000,
    "processedAt": 1735455851000
  }
}
```

### 2. SQS Events

**Example Message:**
```json
{
  "Records": [
    {
      "messageId": "sqs-message-123",
      "body": "{\"orderId\":\"ORD-001\",\"action\":\"PROCESS\"}",
      "attributes": {
        "SentTimestamp": "1735455850000"
      }
    }
  ]
}
```

**Response:** `void` (message acknowledged automatically)

### 3. EventBridge Events

**Example Event:**
```json
{
  "id": "eventbridge-event-123",
  "source": "com.project.tasks",
  "detail-type": "Scheduled Task",
  "detail": {
    "taskType": "daily-report",
    "schedule": "0 9 * * *"
  }
}
```

**Response:** `void` (no response expected)

## Building

```bash
# Build the module
mvn clean package

# Run tests
mvn test

# Skip tests
mvn clean package -DskipTests
```

**Output:** `taskService/target/taskService-1.0-SNAPSHOT.jar` (shaded JAR with all dependencies)

## Testing

### Run All Tests
```bash
mvn test
```

### Test Coverage

- ✅ API Gateway events (POST, GET with query params, path params, empty body)
- ✅ SQS events (single message, multiple messages, with attributes)
- ✅ EventBridge events (scheduled tasks, custom events)
- ✅ Error handling (unknown event types, null context)
- ✅ Integration test (all event types)

**Total Tests:** 15+ test cases

## Deployment

### Local Deployment (LocalStack)

```bash
# Set environment variables
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Create Lambda function
awslocal lambda create-function \
  --function-name task-service \
  --runtime java21 \
  --handler com.project.task.handler.TaskHandler::handleRequest \
  --zip-file fileb://target/taskService-1.0-SNAPSHOT.jar \
  --role arn:aws:iam::000000000000:role/lambda-role \
  --timeout 60 \
  --memory-size 512
```

### AWS Deployment

```bash
# Package
mvn clean package

# Deploy using AWS CLI
aws lambda create-function \
  --function-name task-service \
  --runtime java21 \
  --handler com.project.task.handler.TaskHandler::handleRequest \
  --zip-file fileb://target/taskService-1.0-SNAPSHOT.jar \
  --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
  --timeout 60 \
  --memory-size 512 \
  --environment Variables="{ENVIRONMENT=production}"
```

## Testing the Lambda

### API Gateway Event Test

```bash
awslocal lambda invoke \
  --function-name task-service \
  --payload '{
    "httpMethod": "POST",
    "path": "/tasks",
    "body": "{\"taskName\":\"Test Task\"}"
  }' \
  response.json

cat response.json
```

### SQS Event Test

```bash
awslocal lambda invoke \
  --function-name task-service \
  --payload '{
    "Records": [{
      "messageId": "test-123",
      "body": "{\"orderId\":\"ORD-001\"}"
    }]
  }' \
  response.json
```

### EventBridge Event Test

```bash
awslocal lambda invoke \
  --function-name task-service \
  --payload '{
    "id": "test-event",
    "source": "test.source",
    "detail-type": "Test Event",
    "detail": {"test": true}
  }' \
  response.json
```

## Logging

The service uses **Log4j2** with **JSON logging** for structured CloudWatch logs.

### Log Levels

- **INFO** - Request/response flow, task processing
- **DEBUG** - Detailed event parsing, metadata
- **ERROR** - Exceptions and failures

### Sample Log Output

```json
{
  "instant": {"epochSecond": 1735455850, "nanoOfSecond": 123456789},
  "thread": "main",
  "level": "INFO",
  "loggerName": "com.project.task.handler.TaskHandler",
  "message": "Task handler invoked: functionName=task-service, requestId=abc-123",
  "endOfBatch": false
}
```

## Next Steps

### Business Logic Implementation

The `processTask()` method in `TaskHandler` is currently a placeholder. Implement your business logic there:

```java
private TaskResponse processTask(TaskRequest request, String requestId) {
    // TODO: Implement your business logic here
    // Examples:
    // - Save to database
    // - Call external APIs
    // - Process data transformations
    // - Send notifications
    // - etc.
}
```

### Additional Features to Implement

1. **Database Integration** - Store task data in DynamoDB/RDS
2. **External API Calls** - Integrate with third-party services
3. **Validation** - Add input validation logic
4. **Retry Logic** - Implement retry for failed tasks
5. **Dead Letter Queue** - Handle failed SQS messages
6. **Metrics** - Add CloudWatch metrics
7. **Tracing** - Add X-Ray tracing

## Dependencies

- **AWS Lambda Java Core** 1.2.3
- **AWS Lambda Java Events** 3.11.4
- **AWS Powertools Logging** 2.8.0
- **Jackson** 2.17.1
- **Log4j2** 2.25.3
- **JUnit** 4.13.2 (test)
- **Mockito** 5.8.0 (test)

## Environment Variables

None required currently. Add as needed for your business logic:

```bash
ENVIRONMENT=production
DATABASE_URL=...
API_KEY=...
```

## Contributing

1. Add business logic to `processTask()` method
2. Update tests in `TaskHandlerTest.java`
3. Run tests: `mvn test`
4. Build: `mvn clean package`
5. Deploy and test

## License

Part of SetUpProject - Internal Use

---

**Status:** ✅ **Base Template Complete**  
**Next:** Implement business logic as per requirements

