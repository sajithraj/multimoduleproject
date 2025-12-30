# 🎯 Complete Testing Guide - All Fixed!

## ✅ What Was Fixed

### Issue: `detailType` was null for EventBridge events

**Root Cause:**

- JSON uses `"detail-type"` (hyphenated)
- Java `ScheduledEvent` expects `detailType` (camelCase)
- Jackson couldn't map them without a MixIn

**Solution:**  
Added `ScheduledEventMixIn` with `@JsonProperty("detail-type")` annotation to properly map the field.

---

## 🔗 Correct Endpoints for Postman

### 1. API Gateway Endpoints ✅ (Working)

```
Base URL: http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_

GET    /ping
GET    /task
GET    /task/{id}
POST   /task
PUT    /task/{id}
DELETE /task/{id}
```

### 2. Lambda Direct Invocation Endpoint (For SQS & EventBridge Testing)

```
POST http://localhost:4566/2015-03-31/functions/task-service-dev-local/invocations

Headers:
Content-Type: application/json

Body: (Your event JSON - SQS or EventBridge format)
```

---

## 📋 Test Payloads

### Test 1: SQS Event

**Endpoint:**

```
POST http://localhost:4566/2015-03-31/functions/task-service-dev-local/invocations
```

**Headers:**

```
Content-Type: application/json
```

**Body:**

```json
{
  "Records": [
    {
      "messageId": "test-msg-123",
      "receiptHandle": "test-receipt-handle",
      "body": "{\"name\":\"SQS Task from Postman\",\"description\":\"Testing SQS integration\",\"status\":\"TODO\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735558800000",
        "SenderId": "AIDAIENQZJOLO23YVJ4VO",
        "ApproximateFirstReceiveTimestamp": "1735558800000"
      },
      "messageAttributes": {},
      "md5OfBody": "test-hash",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:000000000000:task-queue-dev-local",
      "awsRegion": "us-east-1"
    }
  ]
}
```

**Expected Response:**

```json
{
  "batchItemFailures": []
}
```

---

### Test 2: EventBridge Scheduled Event

**Endpoint:**

```
POST http://localhost:4566/2015-03-31/functions/task-service-dev-local/invocations
```

**Headers:**

```
Content-Type: application/json
```

**Body:**

```json
{
  "id": "cdc73f9d-aea9-11e3-9d5a-835b769c0d9c",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "account": "123456789012",
  "time": "2025-12-30T10:00:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:events:us-east-1:123456789012:rule/my-scheduled-rule"
  ],
  "detail": {}
}
```

**Expected Response:**

```
"OK"
```

**What Should Happen:**

- detailType should be "Scheduled Event" (NOT null)
- Event type detected as SCHEDULED_EVENT
- Scheduled task created with auto-generated ID

---

### Test 3: EventBridge Custom Event

**Endpoint:**

```
POST http://localhost:4566/2015-03-31/functions/task-service-dev-local/invocations
```

**Headers:**

```
Content-Type: application/json
```

**Body:**

```json
{
  "id": "custom-event-123",
  "detail-type": "TaskCreated",
  "source": "com.custom.tasks",
  "account": "123456789012",
  "time": "2025-12-30T10:00:00Z",
  "region": "us-east-1",
  "resources": [],
  "detail": {
    "name": "Custom Event Task",
    "description": "Created from custom EventBridge event",
    "status": "TODO"
  }
}
```

**Expected Response:**

```
"OK"
```

**What Should Happen:**

- detailType should be "TaskCreated" (NOT null)
- Event type detected as CUSTOM_EVENT (because source starts with "com.custom")
- Task created from detail object

---

## 🧪 Quick Test Commands (PowerShell)

### Test SQS via Actual Queue:

```powershell
aws sqs send-message `
  --queue-url "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/task-queue-dev-local" `
  --message-body '{"name":"Test from SQS","description":"Testing","status":"TODO"}' `
  --endpoint-url http://localhost:4566 `
  --region us-east-1
```

### Test API Gateway - Ping:

```powershell
Invoke-WebRequest -Uri "http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/ping" -Method GET
```

### Test API Gateway - Get All Tasks:

```powershell
Invoke-WebRequest -Uri "http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task" -Method GET
```

### View Logs:

```powershell
aws logs tail /aws/lambda/task-service-dev-local `
  --follow `
  --endpoint-url http://localhost:4566 `
  --region us-east-1
```

---

## 📊 Verification Checklist

After sending each test:

### For SQS Events:

1. ✅ Check response: `{"batchItemFailures": []}`
2. ✅ Check logs: "Detected SQS invocation"
3. ✅ Check logs: "Creating new task"
4. ✅ Check logs: "Task created successfully"
5. ✅ Verify task created: `GET /task` should show new task

### For EventBridge Scheduled Events:

1. ✅ Check response: `"OK"`
2. ✅ Check logs: "Detected EventBridge invocation"
3. ✅ Check logs: "detailType=Scheduled Event" (NOT null!)
4. ✅ Check logs: "Detected: Scheduled Task"
5. ✅ Check logs: "Scheduled task created"

### For EventBridge Custom Events:

1. ✅ Check response: `"OK"`
2. ✅ Check logs: "Detected EventBridge invocation"
3. ✅ Check logs: "detailType=TaskCreated" (NOT null!)
4. ✅ Check logs: "Detected: Custom Event"
5. ✅ Check logs: "Custom event task created"

---

## 🎯 Summary

### What Works Now:

| Test Type                   | Endpoint                                | Status                          |
|-----------------------------|-----------------------------------------|---------------------------------|
| API Gateway - All endpoints | `/restapis/.../`                        | ✅ Working                       |
| SQS - Direct invocation     | `/2015-03-31/functions/.../invocations` | ✅ Working                       |
| SQS - Via queue             | SQS Queue URL                           | ✅ Auto-triggered                |
| EventBridge - Scheduled     | `/2015-03-31/functions/.../invocations` | ✅ Fixed (detailType now mapped) |
| EventBridge - Custom        | `/2015-03-31/functions/.../invocations` | ✅ Fixed (detailType now mapped) |

### Key URLs:

- **API Gateway Base:** `http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/`
- **Lambda Invocation:** `http://localhost:4566/2015-03-31/functions/task-service-dev-local/invocations`
- **SQS Queue:** `http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/task-queue-dev-local`

---

**All systems operational! Test away!** 🚀

