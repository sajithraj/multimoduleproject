# TaskService - Template Pattern Implementation Guide

## 🎯 Overview

The TaskService has been enhanced with a **template pattern** architecture that provides:

✅ **Multiple API Gateway Endpoints** - /ping, /get, /id/{id}, /post  
✅ **EventBridge Event Types** - Scheduled tasks, custom events, system events  
✅ **Clean Routing** - Separate routers for API and EventBridge  
✅ **Easy Extension** - Add new endpoints/events easily  
✅ **Production-Ready** - Professional error handling  

---

## 🏗️ Architecture

```
UnifiedTaskHandler (Entry Point)
         │
         ▼
   EventRouter (Detects source)
         │
         ├─── API Gateway ──▶ ApiGatewayRouter ──▶ TaskService
         │                         │                    │
         │                         ├─── /ping          ├─── processPing()
         │                         ├─── /get           ├─── processGetAll()
         │                         ├─── /id/{id}       ├─── processGetById()
         │                         └─── /post          └─── processPost()
         │
         ├─── SQS ──▶ TaskService.processSqsMessage()
         │
         └─── EventBridge ──▶ EventBridgeHandler ──▶ TaskService
                                    │                    │
                                    ├─── Scheduled      ├─── processScheduledTask()
                                    ├─── Order Events   ├─── processOrderEvent()
                                    ├─── Payment Events ├─── processPaymentEvent()
                                    ├─── User Events    ├─── processUserEvent()
                                    └─── System Events  └─── processSystemEvent()
```

---

## 📦 Components

### 1. ApiGatewayRouter
**Location:** `router/ApiGatewayRouter.java`

**Purpose:** Routes API Gateway requests to specific endpoint handlers

**Supported Endpoints:**
```java
GET  /ping        -> processPing()       // Health check
GET  /get         -> processGetAll()     // Get all resources
GET  /id/{id}     -> processGetById()    // Get resource by ID
POST /post        -> processPost()       // Create resource
```

**Features:**
- ✅ Method validation (GET, POST)
- ✅ Path parameter extraction
- ✅ 404 handling with helpful error messages
- ✅ CORS headers included
- ✅ Consistent error responses

**Usage Example:**
```java
// In EventRouter
APIGatewayProxyResponseEvent response = API_ROUTER.route(event, context);
```

---

### 2. EventBridgeHandler
**Location:** `handler/EventBridgeHandler.java`

**Purpose:** Handles both scheduled and custom EventBridge events

**Supported Event Types:**

#### A. Scheduled Tasks
**Source:** `aws.events`  
**Detail-Type:** `Scheduled Event`  
**Use Case:** Cron jobs, periodic tasks, cleanup jobs

```json
{
  "source": "aws.events",
  "detail-type": "Scheduled Event",
  "detail": {}
}
```

#### B. Custom Business Events
**Source:** `com.project.*`  
**Detail-Type:** Custom (e.g., `OrderCreated`, `PaymentProcessed`)  
**Use Case:** Application events, workflow triggers

```json
{
  "source": "com.project.orders",
  "detail-type": "OrderCreated",
  "detail": {
    "orderId": "ORD-123",
    "customerId": "CUST-456"
  }
}
```

#### C. AWS System Events
**Source:** `aws.*`  
**Detail-Type:** Varies  
**Use Case:** EC2 state changes, S3 events, CloudWatch alarms

```json
{
  "source": "aws.ec2",
  "detail-type": "EC2 Instance State-change Notification",
  "detail": {
    "instance-id": "i-1234567890abcdef0"
  }
}
```

---

### 3. EventBridgeEventType Enum
**Location:** `model/EventBridgeEventType.java`

```java
public enum EventBridgeEventType {
    SCHEDULED_TASK,           // Cron/rate expressions
    CUSTOM_BUSINESS_EVENT,    // Application events
    SYSTEM_EVENT              // AWS system events
}
```

---

## 🚀 Adding New Endpoints

### Add New API Gateway Endpoint

**Step 1:** Add route in `ApiGatewayRouter.java`

```java
return switch (path) {
    case "/ping" -> handlePing(event, context);
    case "/get" -> handleGet(event, context);
    case "/post" -> handlePost(event, context);
    case "/users" -> handleGetUsers(event, context);  // NEW
    // ...
};
```

**Step 2:** Create handler method

```java
private APIGatewayProxyResponseEvent handleGetUsers(
        APIGatewayProxyRequestEvent event,
        Context context) {
    
    log.info("Handling GET /users request");
    return taskService.processGetUsers(event, context);
}
```

**Step 3:** Implement in `TaskService.java`

```java
public APIGatewayProxyResponseEvent processGetUsers(
        APIGatewayProxyRequestEvent event,
        Context context) {
    
    log.info("Processing GET /users");
    
    // Your business logic here
    List<User> users = fetchUsers();
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("users", users);
    
    return buildApiResponse(200, response);
}
```

---

## 🎯 Adding New EventBridge Event Types

### Add New Custom Event Handler

**Step 1:** Add event type detection in `EventBridgeHandler.java`

```java
private String handleCustomEvent(ScheduledEvent event, Context context) {
    String detailType = event.getDetailType();
    
    if (detailType.contains("Order")) {
        taskService.processOrderEvent(event, context);
    } else if (detailType.contains("Inventory")) {  // NEW
        taskService.processInventoryEvent(event, context);
    }
    // ...
}
```

**Step 2:** Implement in `TaskService.java`

```java
public void processInventoryEvent(ScheduledEvent event, Context context) {
    log.info("Processing inventory event: detailType={}", event.getDetailType());
    
    Map<String, Object> detail = event.getDetail();
    
    // Your business logic here
    String productId = (String) detail.get("productId");
    int quantity = (int) detail.get("quantity");
    
    updateInventory(productId, quantity);
    
    log.info("Inventory event processed successfully");
}
```

---

## 📝 Request/Response Examples

### 1. Health Check (/ping)

**Request:**
```bash
curl -X GET https://api-gateway-url/ping
```

**Response:**
```json
{
  "status": "healthy",
  "service": "task-service",
  "timestamp": 1735455850000,
  "requestId": "abc-123-def",
  "version": "1.0.0"
}
```

---

### 2. Get All Resources (/get)

**Request:**
```bash
curl -X GET https://api-gateway-url/get
```

**Response:**
```json
{
  "success": true,
  "message": "Retrieved all resources",
  "data": [
    {
      "id": "res-1",
      "name": "Resource 1",
      "status": "active",
      "createdAt": 1735455850000
    }
  ],
  "count": 3
}
```

---

### 3. Get Resource by ID (/id/{id})

**Request:**
```bash
curl -X GET https://api-gateway-url/id/res-123
```

**Response:**
```json
{
  "success": true,
  "message": "Resource retrieved successfully",
  "data": {
    "id": "res-123",
    "name": "Resource res-123",
    "status": "active",
    "createdAt": 1735455850000
  }
}
```

---

### 4. Create Resource (POST /post)

**Request:**
```bash
curl -X POST https://api-gateway-url/post \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Resource",
    "type": "document"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Resource created successfully",
  "taskId": "uuid-generated",
  "data": {
    "id": "uuid-generated",
    "name": "New Resource",
    "status": "active",
    "createdAt": 1735455850000
  }
}
```

---

### 5. Scheduled Task Event

**Event:**
```json
{
  "id": "event-123",
  "source": "aws.events",
  "detail-type": "Scheduled Event",
  "time": "2025-12-29T09:00:00Z",
  "detail": {}
}
```

**Processing:**
- Detects as `SCHEDULED_TASK`
- Calls `TaskService.processScheduledTask()`
- Returns `"OK"`

---

### 6. Custom Order Event

**Event:**
```json
{
  "id": "event-456",
  "source": "com.project.orders",
  "detail-type": "OrderCreated",
  "detail": {
    "orderId": "ORD-123",
    "customerId": "CUST-456",
    "amount": 99.99
  }
}
```

**Processing:**
- Detects as `CUSTOM_BUSINESS_EVENT`
- Routes to `processOrderEvent()` (contains "Order")
- Returns `"OK"`

---

## 🔧 Configuration

### API Gateway Setup

```yaml
Resources:
  /ping:
    GET: Lambda integration
  
  /get:
    GET: Lambda integration
  
  /id/{id}:
    GET: Lambda integration
    PathParameter: id
  
  /post:
    POST: Lambda integration
```

### EventBridge Rules

#### Scheduled Task
```json
{
  "schedule": "rate(1 hour)",
  "target": {
    "arn": "arn:aws:lambda:...:function:task-service"
  }
}
```

#### Custom Event Pattern
```json
{
  "source": ["com.project.orders"],
  "detail-type": ["OrderCreated", "OrderUpdated"]
}
```

---

## 🧪 Testing

### Test /ping Endpoint

```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "httpMethod": "GET",
    "path": "/ping"
  }' \
  response.json
```

### Test /get Endpoint

```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "httpMethod": "GET",
    "path": "/get"
  }' \
  response.json
```

### Test /id/{id} Endpoint

```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "httpMethod": "GET",
    "path": "/id/res-123",
    "pathParameters": {"id": "res-123"}
  }' \
  response.json
```

### Test Scheduled Event

```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "source": "aws.events",
    "detail-type": "Scheduled Event",
    "detail": {}
  }' \
  response.json
```

### Test Custom Event

```bash
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "source": "com.project.orders",
    "detail-type": "OrderCreated",
    "detail": {"orderId": "ORD-123"}
  }' \
  response.json
```

---

## 📊 Error Handling

### API Gateway Errors

#### 400 - Bad Request
```json
{
  "success": false,
  "error": "Request body is required"
}
```

#### 404 - Not Found
```json
{
  "error": "Not Found",
  "message": "No route found for GET /unknown",
  "availableRoutes": [
    "GET /ping",
    "GET /get",
    "GET /id/{id}",
    "POST /post"
  ]
}
```

#### 405 - Method Not Allowed
```json
{
  "success": false,
  "error": "Method not allowed. Use GET."
}
```

#### 500 - Internal Server Error
```json
{
  "success": false,
  "error": "Internal server error"
}
```

---

## 🎨 Template Pattern Benefits

### Easy to Extend
- ✅ Add new API endpoint in 3 steps
- ✅ Add new EventBridge handler in 2 steps
- ✅ Clear structure for new developers

### Maintainable
- ✅ Separation of concerns
- ✅ Each router handles one responsibility
- ✅ TaskService focuses on business logic

### Testable
- ✅ Mock routers independently
- ✅ Test endpoints individually
- ✅ Test event types separately

### Production-Ready
- ✅ Comprehensive error handling
- ✅ Structured logging
- ✅ CORS support
- ✅ Method validation

---

## 🚀 Next Steps

### Implement Your Business Logic

Replace placeholder methods in `TaskService.java`:

```java
// Example: Implement processGetAll()
public APIGatewayProxyResponseEvent processGetAll(...) {
    // 1. Query database
    List<Resource> resources = dynamoDbClient.scan(...);
    
    // 2. Transform data
    List<ResourceDTO> dtos = resources.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
    
    // 3. Return response
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("data", dtos);
    response.put("count", dtos.size());
    
    return buildApiResponse(200, response);
}
```

---

## 📚 Summary

**Template Features:**
- ✅ Multiple API Gateway endpoints
- ✅ EventBridge scheduled + custom events
- ✅ Clean router pattern
- ✅ Easy to extend
- ✅ Production-ready

**File Structure:**
```
taskService/
├── handler/
│   ├── UnifiedTaskHandler.java        # Entry point
│   └── EventBridgeHandler.java        # NEW - EventBridge routing
├── router/
│   ├── EventRouter.java                # Main router
│   └── ApiGatewayRouter.java           # NEW - API routing
├── model/
│   └── EventBridgeEventType.java       # NEW - Event types
└── service/
    └── TaskService.java                 # UPDATED - All handlers
```

**Ready to Use!** 🎉

[← Back to TaskService README](README.md)

