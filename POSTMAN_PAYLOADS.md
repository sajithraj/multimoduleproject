# Postman Test Payloads - Copy & Paste Ready

Use these payloads in Postman with:
- **Method:** POST
- **URL:** `http://localhost:4566/2015-03-31/functions/task-service-dev/invocations`
- **Headers:** `Content-Type: application/json`
- **Body:** Raw JSON (copy from below)

---

## API Gateway Events

### 1. GET /ping (Health Check)
```json
{
  "resource": "/ping",
  "path": "/ping",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json",
    "User-Agent": "PostmanRuntime/7.32.0"
  },
  "multiValueHeaders": {
    "Accept": ["application/json"],
    "Content-Type": ["application/json"]
  },
  "queryStringParameters": null,
  "multiValueQueryStringParameters": null,
  "pathParameters": null,
  "stageVariables": null,
  "requestContext": {
    "requestId": "postman-test-001",
    "accountId": "123456789012",
    "stage": "dev",
    "requestTime": "29/Dec/2025:10:00:00 +0000",
    "requestTimeEpoch": 1735470000000,
    "identity": {
      "sourceIp": "127.0.0.1",
      "userAgent": "PostmanRuntime/7.32.0"
    },
    "protocol": "HTTP/1.1",
    "httpMethod": "GET",
    "path": "/ping",
    "resourcePath": "/ping"
  },
  "body": null,
  "isBase64Encoded": false
}
```

---

### 2. GET /id/{id} (Get Resource by ID)
```json
{
  "resource": "/id/{id}",
  "path": "/id/12345",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json",
    "User-Agent": "PostmanRuntime/7.32.0"
  },
  "multiValueHeaders": {
    "Accept": ["application/json"]
  },
  "queryStringParameters": null,
  "multiValueQueryStringParameters": null,
  "pathParameters": {
    "id": "12345"
  },
  "stageVariables": null,
  "requestContext": {
    "requestId": "postman-test-002",
    "accountId": "123456789012",
    "stage": "dev",
    "requestTime": "29/Dec/2025:10:01:00 +0000",
    "requestTimeEpoch": 1735470060000,
    "identity": {
      "sourceIp": "127.0.0.1",
      "userAgent": "PostmanRuntime/7.32.0"
    },
    "protocol": "HTTP/1.1",
    "httpMethod": "GET",
    "path": "/id/12345",
    "resourcePath": "/id/{id}"
  },
  "body": null,
  "isBase64Encoded": false
}
```

---

### 3. GET /id/ABC-999 (Different ID Format)
```json
{
  "resource": "/id/{id}",
  "path": "/id/ABC-999",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "queryStringParameters": null,
  "pathParameters": {
    "id": "ABC-999"
  },
  "requestContext": {
    "requestId": "postman-test-003",
    "accountId": "123456789012",
    "stage": "dev"
  },
  "body": null,
  "isBase64Encoded": false
}
```

---

### 4. GET /tasks (List Tasks)
```json
{
  "resource": "/tasks",
  "path": "/tasks",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "queryStringParameters": {
    "limit": "10",
    "status": "active",
    "page": "1"
  },
  "multiValueQueryStringParameters": {
    "limit": ["10"],
    "status": ["active"],
    "page": ["1"]
  },
  "pathParameters": null,
  "requestContext": {
    "requestId": "postman-test-004",
    "accountId": "123456789012",
    "stage": "dev"
  },
  "body": null,
  "isBase64Encoded": false
}
```

---

### 5. POST /tasks (Create Task)
```json
{
  "resource": "/tasks",
  "path": "/tasks",
  "httpMethod": "POST",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json",
    "User-Agent": "PostmanRuntime/7.32.0"
  },
  "queryStringParameters": null,
  "pathParameters": null,
  "requestContext": {
    "requestId": "postman-test-005",
    "accountId": "123456789012",
    "stage": "dev",
    "requestTime": "29/Dec/2025:10:05:00 +0000",
    "requestTimeEpoch": 1735470300000,
    "identity": {
      "sourceIp": "127.0.0.1",
      "userAgent": "PostmanRuntime/7.32.0"
    }
  },
  "body": "{\"taskName\":\"Process Order\",\"taskData\":{\"orderId\":\"ORD-001\",\"amount\":99.99,\"currency\":\"USD\",\"items\":[{\"productId\":\"PROD-123\",\"quantity\":2,\"price\":49.99}]}}",
  "isBase64Encoded": false
}
```

---

### 6. POST /tasks (Create Task - Complex)
```json
{
  "resource": "/tasks",
  "path": "/tasks",
  "httpMethod": "POST",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json",
    "X-Request-ID": "custom-request-id-001"
  },
  "queryStringParameters": null,
  "pathParameters": null,
  "requestContext": {
    "requestId": "postman-test-006",
    "accountId": "123456789012",
    "stage": "dev"
  },
  "body": "{\"taskName\":\"Send Notification\",\"taskType\":\"EMAIL\",\"recipient\":\"user@example.com\",\"subject\":\"Order Confirmation\",\"message\":\"Your order has been confirmed\",\"priority\":\"HIGH\",\"metadata\":{\"templateId\":\"order-confirmation\",\"language\":\"en\"}}",
  "isBase64Encoded": false
}
```

---

### 7. PUT /tasks/{id} (Update Task - Not Implemented Yet)
```json
{
  "resource": "/tasks/{id}",
  "path": "/tasks/TASK-12345",
  "httpMethod": "PUT",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "pathParameters": {
    "id": "TASK-12345"
  },
  "requestContext": {
    "requestId": "postman-test-007",
    "accountId": "123456789012",
    "stage": "dev"
  },
  "body": "{\"status\":\"COMPLETED\",\"result\":\"Success\"}",
  "isBase64Encoded": false
}
```

---

## SQS Events

### 8. SQS - Single Message (Order)
```json
{
  "Records": [
    {
      "messageId": "msg-001",
      "receiptHandle": "AQEBwJnKyrHigUMZj6rYigCgxlaS3SLy0a...",
      "body": "{\"orderId\":\"ORD-12345\",\"amount\":150.50,\"currency\":\"USD\",\"customer\":\"John Doe\",\"email\":\"john@example.com\",\"items\":[{\"productId\":\"PROD-001\",\"quantity\":2,\"price\":75.25}]}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470000000",
        "SenderId": "AIDAIT2UOQQY3AUEKVGXU",
        "ApproximateFirstReceiveTimestamp": "1735470001000"
      },
      "messageAttributes": {
        "Priority": {
          "stringValue": "HIGH",
          "dataType": "String"
        },
        "Source": {
          "stringValue": "OrderService",
          "dataType": "String"
        }
      },
      "md5OfBody": "e4e68fb7bd0e697a0ae8f1bb342846b3",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    }
  ]
}
```

---

### 9. SQS - Single Message (Simple)
```json
{
  "Records": [
    {
      "messageId": "msg-002",
      "receiptHandle": "receipt-002",
      "body": "{\"taskId\":\"TASK-001\",\"action\":\"process\",\"timestamp\":\"2025-12-29T10:00:00Z\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470000000"
      },
      "messageAttributes": {},
      "md5OfBody": "test-md5-002",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    }
  ]
}
```

---

### 10. SQS - Batch Messages (3 Orders)
```json
{
  "Records": [
    {
      "messageId": "msg-batch-001",
      "receiptHandle": "receipt-batch-001",
      "body": "{\"orderId\":\"ORD-001\",\"amount\":100.00,\"status\":\"NEW\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470000000"
      },
      "messageAttributes": {},
      "md5OfBody": "md5-001",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    },
    {
      "messageId": "msg-batch-002",
      "receiptHandle": "receipt-batch-002",
      "body": "{\"orderId\":\"ORD-002\",\"amount\":200.00,\"status\":\"NEW\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470001000"
      },
      "messageAttributes": {},
      "md5OfBody": "md5-002",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    },
    {
      "messageId": "msg-batch-003",
      "receiptHandle": "receipt-batch-003",
      "body": "{\"orderId\":\"ORD-003\",\"amount\":300.00,\"status\":\"NEW\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470002000"
      },
      "messageAttributes": {},
      "md5OfBody": "md5-003",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    }
  ]
}
```

---

### 11. SQS - Batch with Large Payload
```json
{
  "Records": [
    {
      "messageId": "msg-large-001",
      "receiptHandle": "receipt-large-001",
      "body": "{\"orderId\":\"ORD-LARGE-001\",\"customerName\":\"Jane Smith\",\"email\":\"jane@example.com\",\"phone\":\"+1-555-0123\",\"address\":{\"street\":\"123 Main St\",\"city\":\"New York\",\"state\":\"NY\",\"zip\":\"10001\",\"country\":\"USA\"},\"items\":[{\"productId\":\"PROD-001\",\"name\":\"Laptop\",\"quantity\":1,\"price\":1200.00},{\"productId\":\"PROD-002\",\"name\":\"Mouse\",\"quantity\":2,\"price\":25.00},{\"productId\":\"PROD-003\",\"name\":\"Keyboard\",\"quantity\":1,\"price\":75.00}],\"totalAmount\":1325.00,\"currency\":\"USD\",\"paymentMethod\":\"CREDIT_CARD\",\"status\":\"PENDING\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470000000"
      },
      "messageAttributes": {
        "OrderType": {
          "stringValue": "RETAIL",
          "dataType": "String"
        },
        "Priority": {
          "stringValue": "NORMAL",
          "dataType": "String"
        }
      },
      "md5OfBody": "large-md5",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    },
    {
      "messageId": "msg-large-002",
      "receiptHandle": "receipt-large-002",
      "body": "{\"orderId\":\"ORD-LARGE-002\",\"customerName\":\"Bob Johnson\",\"items\":[{\"productId\":\"PROD-004\",\"quantity\":5,\"price\":10.00}],\"totalAmount\":50.00}",
      "attributes": {
        "ApproximateReceiveCount": "1"
      },
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev"
    }
  ]
}
```

---

## EventBridge Events

### 12. EventBridge - Scheduled Event (Cron Job)
```json
{
  "version": "0",
  "id": "scheduled-event-001",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "account": "123456789012",
  "time": "2025-12-29T10:00:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:events:us-east-1:123456789012:rule/task-schedule-dev"
  ],
  "detail": {}
}
```

---

### 13. EventBridge - Custom Event (OrderCreated)
```json
{
  "version": "0",
  "id": "order-event-001",
  "detail-type": "OrderCreated",
  "source": "com.project.orders",
  "account": "123456789012",
  "time": "2025-12-29T10:00:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:dynamodb:us-east-1:123456789012:table/Orders"
  ],
  "detail": {
    "orderId": "ORD-98765",
    "customerId": "CUST-12345",
    "customerName": "Alice Brown",
    "amount": 250.75,
    "currency": "USD",
    "status": "CREATED",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Wireless Headphones",
        "quantity": 1,
        "price": 199.99
      },
      {
        "productId": "PROD-002",
        "productName": "Phone Case",
        "quantity": 2,
        "price": 25.38
      }
    ],
    "shippingAddress": {
      "street": "456 Oak Avenue",
      "city": "San Francisco",
      "state": "CA",
      "zip": "94102"
    },
    "createdAt": "2025-12-29T10:00:00Z"
  }
}
```

---

### 14. EventBridge - Payment Event
```json
{
  "version": "0",
  "id": "payment-event-001",
  "detail-type": "PaymentProcessed",
  "source": "com.project.payments",
  "account": "123456789012",
  "time": "2025-12-29T10:05:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:dynamodb:us-east-1:123456789012:table/Payments"
  ],
  "detail": {
    "paymentId": "PAY-54321",
    "orderId": "ORD-98765",
    "customerId": "CUST-12345",
    "amount": 250.75,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "cardType": "VISA",
    "last4Digits": "4242",
    "status": "SUCCESS",
    "transactionId": "TXN-ABC123",
    "processedAt": "2025-12-29T10:05:00Z",
    "metadata": {
      "gateway": "Stripe",
      "gatewayTransactionId": "ch_3ABC123",
      "ipAddress": "192.168.1.1"
    }
  }
}
```

---

### 15. EventBridge - User Event
```json
{
  "version": "0",
  "id": "user-event-001",
  "detail-type": "UserRegistered",
  "source": "com.project.users",
  "account": "123456789012",
  "time": "2025-12-29T10:10:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:cognito-idp:us-east-1:123456789012:userpool/us-east-1_ABC123"
  ],
  "detail": {
    "userId": "USER-99999",
    "email": "newuser@example.com",
    "username": "newuser123",
    "firstName": "New",
    "lastName": "User",
    "registeredAt": "2025-12-29T10:10:00Z",
    "emailVerified": false,
    "subscriptionPlan": "FREE"
  }
}
```

---

### 16. EventBridge - AWS System Event (EC2)
```json
{
  "version": "0",
  "id": "ec2-event-001",
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
    "state": "running",
    "previous-state": "pending"
  }
}
```

---

### 17. EventBridge - AWS System Event (S3)
```json
{
  "version": "0",
  "id": "s3-event-001",
  "detail-type": "Object Created",
  "source": "aws.s3",
  "account": "123456789012",
  "time": "2025-12-29T10:20:00Z",
  "region": "us-east-1",
  "resources": [
    "arn:aws:s3:::my-bucket"
  ],
  "detail": {
    "bucket": {
      "name": "my-bucket"
    },
    "object": {
      "key": "uploads/document.pdf",
      "size": 1048576,
      "etag": "d41d8cd98f00b204e9800998ecf8427e"
    },
    "request-id": "C3D13FE58DE4C810",
    "requester": "123456789012"
  }
}
```

---

### 18. EventBridge - Custom Event (Inventory Low)
```json
{
  "version": "0",
  "id": "inventory-event-001",
  "detail-type": "InventoryLowAlert",
  "source": "com.project.inventory",
  "account": "123456789012",
  "time": "2025-12-29T10:25:00Z",
  "region": "us-east-1",
  "resources": [],
  "detail": {
    "productId": "PROD-001",
    "productName": "Wireless Headphones",
    "currentStock": 5,
    "minimumStock": 20,
    "reorderQuantity": 50,
    "supplier": "TechSupplier Inc",
    "alertLevel": "WARNING"
  }
}
```

---

## Expected Responses

### API Gateway Events
- **Status Code:** 200
- **Response Format:**
```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "{\"message\":\"...\",\"data\":{...}}"
}
```

### SQS Events
- **Response Format:**
```json
{
  "batchItemFailures": []
}
```
- **Note:** Empty array means all messages processed successfully
- If failures occur, you'll see:
```json
{
  "batchItemFailures": [
    {"itemIdentifier": "msg-002"}
  ]
}
```

### EventBridge Events
- **Response Format:**
```json
"OK"
```
- **Note:** Simple string response

---

## Testing Order (Recommended)

1. ✅ **Test 1** - API Gateway GET /ping (simplest)
2. ✅ **Test 2** - API Gateway GET /id/12345
3. ✅ **Test 4** - API Gateway GET /tasks (with query params)
4. ✅ **Test 5** - API Gateway POST /tasks
5. ✅ **Test 8** - SQS Single Message
6. ✅ **Test 10** - SQS Batch (3 messages)
7. ✅ **Test 12** - EventBridge Scheduled
8. ✅ **Test 13** - EventBridge OrderCreated
9. ✅ **Test 14** - EventBridge Payment
10. ✅ **Test 11** - SQS Large Payload

---

## Tips for Postman

### Create Collection
1. Create new Collection: "TaskService Lambda Tests"
2. Add all 18 requests above
3. Set Collection Variables:
   - `{{lambda_url}}` = `http://localhost:4566/2015-03-31/functions/task-service-dev/invocations`

### Pre-request Script (Optional)
```javascript
// Add timestamp
pm.environment.set("timestamp", new Date().toISOString());
```

### Tests Script (Add to verify responses)
```javascript
// For API Gateway
if (pm.response.json().statusCode) {
    pm.test("Status code is 200", function () {
        pm.expect(pm.response.json().statusCode).to.eql(200);
    });
}

// For SQS
if (pm.response.json().batchItemFailures !== undefined) {
    pm.test("No batch failures", function () {
        pm.expect(pm.response.json().batchItemFailures).to.be.an('array').that.is.empty;
    });
}

// For EventBridge
if (typeof pm.response.json() === 'string') {
    pm.test("Returns OK", function () {
        pm.expect(pm.response.json()).to.eql("OK");
    });
}
```

---

**Total Test Cases:** 18  
**Coverage:** API Gateway (7), SQS (4), EventBridge (7)  
**Ready to Copy & Paste into Postman!** ✅

