# EventBridge Event Samples - All Use ScheduledEvent Class

## 📝 Important Note

**All EventBridge events** (scheduled, EC2, S3, custom, etc.) arrive at Lambda as `ScheduledEvent` objects.
The differentiation happens based on the `source` and `detail-type` fields, not the Java class type.

---

## 1. Scheduled Task Event (CloudWatch Events)

**Source:** `aws.events`  
**Detail-Type:** `Scheduled Event`  
**Use Case:** Cron/rate expressions

```json
{
  "id": "cdc73f9d-aea9-11e3-9d5a-835b769c0d9c",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "account": "123456789012",
  "time": "2025-12-29T10:00:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:events:us-east-1:123456789012:rule/my-scheduled-rule"
  ],
  "detail": {}
}
```

**Test in Lambda Console:**
```json
{
  "id": "scheduled-event-123",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "time": "2025-12-29T10:00:00Z",
  "region": "us-east-1",
  "detail": {}
}
```

---

## 2. EC2 Instance State Change Event

**Source:** `aws.ec2`  
**Detail-Type:** `EC2 Instance State-change Notification`  
**Use Case:** React to EC2 instance state changes

```json
{
  "id": "7bf73129-1428-4cd3-a780-95db273d1602",
  "detail-type": "EC2 Instance State-change Notification",
  "source": "aws.ec2",
  "account": "123456789012",
  "time": "2025-12-29T10:15:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:ec2:us-east-1:123456789012:instance/i-1234567890abcdef0"
  ],
  "detail": {
    "instance-id": "i-1234567890abcdef0",
    "state": "running"
  }
}
```

**Test in Lambda Console:**
```json
{
  "id": "ec2-event-456",
  "detail-type": "EC2 Instance State-change Notification",
  "source": "aws.ec2",
  "time": "2025-12-29T10:15:00Z",
  "region": "us-east-1",
  "detail": {
    "instance-id": "i-1234567890abcdef0",
    "state": "running"
  }
}
```

---

## 3. S3 Event (via EventBridge)

**Source:** `aws.s3`  
**Detail-Type:** `Object Created`  
**Use Case:** React to S3 object creation

```json
{
  "id": "c7f012e8-7f73-4b67-9e23-8c0c8c8e8e8e",
  "detail-type": "Object Created",
  "source": "aws.s3",
  "account": "123456789012",
  "time": "2025-12-29T10:30:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:s3:::my-bucket"
  ],
  "detail": {
    "version": "0",
    "bucket": {
      "name": "my-bucket"
    },
    "object": {
      "key": "uploads/file.txt",
      "size": 1024
    }
  }
}
```

**Test in Lambda Console:**
```json
{
  "id": "s3-event-789",
  "detail-type": "Object Created",
  "source": "aws.s3",
  "time": "2025-12-29T10:30:00Z",
  "region": "us-east-1",
  "detail": {
    "bucket": {
      "name": "my-bucket"
    },
    "object": {
      "key": "uploads/file.txt"
    }
  }
}
```

---

## 4. Custom Business Event (OrderCreated)

**Source:** `com.project.orders`  
**Detail-Type:** `OrderCreated`  
**Use Case:** Custom application events

```json
{
  "id": "custom-event-001",
  "detail-type": "OrderCreated",
  "source": "com.project.orders",
  "account": "123456789012",
  "time": "2025-12-29T11:00:00Z",
  "region": "us-east-1",
  "resources": [],
  "detail": {
    "orderId": "ORD-12345",
    "customerId": "CUST-67890",
    "amount": 99.99,
    "status": "pending"
  }
}
```

**Test in Lambda Console:**
```json
{
  "id": "order-event-001",
  "detail-type": "OrderCreated",
  "source": "com.project.orders",
  "time": "2025-12-29T11:00:00Z",
  "region": "us-east-1",
  "detail": {
    "orderId": "ORD-12345",
    "customerId": "CUST-67890",
    "amount": 99.99
  }
}
```

---

## 5. Custom Payment Event

**Source:** `com.project.payments`  
**Detail-Type:** `PaymentProcessed`  
**Use Case:** Payment workflow

```json
{
  "id": "payment-event-002",
  "detail-type": "PaymentProcessed",
  "source": "com.project.payments",
  "time": "2025-12-29T11:15:00Z",
  "region": "us-east-1",
  "detail": {
    "paymentId": "PAY-99999",
    "orderId": "ORD-12345",
    "amount": 99.99,
    "status": "completed"
  }
}
```

**Test in Lambda Console:**
```json
{
  "id": "payment-event-002",
  "detail-type": "PaymentProcessed",
  "source": "com.project.payments",
  "time": "2025-12-29T11:15:00Z",
  "region": "us-east-1",
  "detail": {
    "paymentId": "PAY-99999",
    "status": "completed"
  }
}
```

---

## 6. CloudWatch Alarm State Change

**Source:** `aws.cloudwatch`  
**Detail-Type:** `CloudWatch Alarm State Change`  
**Use Case:** React to alarm state changes

```json
{
  "id": "alarm-event-003",
  "detail-type": "CloudWatch Alarm State Change",
  "source": "aws.cloudwatch",
  "time": "2025-12-29T11:30:00Z",
  "region": "us-east-1",
  "detail": {
    "alarmName": "HighCPUAlarm",
    "state": {
      "value": "ALARM",
      "reason": "Threshold Crossed"
    }
  }
}
```

---

## 🔍 Key Observations

### All Events Have Same Structure

```
✅ id (string)
✅ detail-type (string)
✅ source (string)
✅ time (string - ISO 8601)
✅ region (string)
✅ detail (object - varies by event type)
```

### How Lambda Receives Them

**Java Class:** All events → `ScheduledEvent` class  
**Differentiation:** Based on `source` and `detail-type` fields

```java
// All of these are ScheduledEvent objects:
ScheduledEvent scheduledTask;     // source = "aws.events"
ScheduledEvent ec2Event;          // source = "aws.ec2"
ScheduledEvent s3Event;           // source = "aws.s3"
ScheduledEvent customEvent;       // source = "com.project.*"
```

---

## 📊 How EventBridgeHandler Differentiates

```java
private EventBridgeEventType detectEventType(ScheduledEvent event) {
    String source = event.getSource();
    String detailType = event.getDetailType();

    // Scheduled tasks
    if ("aws.events".equals(source) && "Scheduled Event".equals(detailType)) {
        return EventBridgeEventType.SCHEDULED_TASK;
    }

    // Custom business events
    if (source != null && source.startsWith("com.project")) {
        return EventBridgeEventType.CUSTOM_BUSINESS_EVENT;
    }

    // AWS system events (EC2, S3, CloudWatch, etc.)
    if (source != null && source.startsWith("aws.")) {
        return EventBridgeEventType.SYSTEM_EVENT;
    }

    // Default
    return EventBridgeEventType.CUSTOM_BUSINESS_EVENT;
}
```

---

## 🧪 Testing Each Event Type

### 1. Test Scheduled Task
```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "id": "test-1",
    "detail-type": "Scheduled Event",
    "source": "aws.events",
    "time": "2025-12-29T10:00:00Z",
    "region": "us-east-1",
    "detail": {}
  }' \
  response.json
```

**Expected Logs:**
```
Detected EventBridge invocation
EventBridge type: Scheduled Task (CloudWatch Events)
Processing scheduled task
```

---

### 2. Test EC2 Event
```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "id": "test-2",
    "detail-type": "EC2 Instance State-change Notification",
    "source": "aws.ec2",
    "time": "2025-12-29T10:15:00Z",
    "region": "us-east-1",
    "detail": {
      "instance-id": "i-1234567890abcdef0",
      "state": "running"
    }
  }' \
  response.json
```

**Expected Logs:**
```
Detected EventBridge invocation: source=aws.ec2, detailType=EC2 Instance State-change Notification
EventBridge type: AWS System Event (aws.ec2)
Processing system event: source=aws.ec2
```

---

### 3. Test Custom Order Event
```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "id": "test-3",
    "detail-type": "OrderCreated",
    "source": "com.project.orders",
    "time": "2025-12-29T11:00:00Z",
    "region": "us-east-1",
    "detail": {
      "orderId": "ORD-12345",
      "amount": 99.99
    }
  }' \
  response.json
```

**Expected Logs:**
```
Detected EventBridge invocation: source=com.project.orders, detailType=OrderCreated
EventBridge type: Custom Business Event
Processing order event: detailType=OrderCreated
```

---

## ✅ Conclusion

**Why InvocationTypeDetector is Correct:**

1. ✅ **All EventBridge events** are `ScheduledEvent` objects
2. ✅ **Single `instanceof` check** catches all EventBridge events
3. ✅ **EventBridgeHandler** does the fine-grained detection
4. ✅ **No unsupported event errors** for EC2, S3, or custom events

**The Flow:**
```
Event Arrives
    ↓
InvocationTypeDetector.detect()
    ↓
if (input instanceof ScheduledEvent)  ← Catches ALL EventBridge events
    ↓
Return InvocationType.EVENT_BRIDGE
    ↓
EventRouter routes to EventBridgeHandler
    ↓
EventBridgeHandler.detectEventType()  ← Differentiates here
    ↓
Routes to specific handler (scheduled/custom/system)
```

---

**The current implementation is correct!** 🎉

All EventBridge events (scheduled, EC2, S3, custom) will be detected as `ScheduledEvent` by Java/AWS SDK, and then differentiated based on `source` field in `EventBridgeHandler`.

---

**Created:** December 29, 2025  
**Use these samples to test your Lambda function!**

