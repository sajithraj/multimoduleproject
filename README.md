# Task Service - AWS Lambda Multi-Event Handler

**A production-grade AWS Lambda service that handles multiple event sources (API Gateway, SQS, EventBridge) with unified routing and processing.**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![LocalStack](https://img.shields.io/badge/LocalStack-3.0+-green.svg)](https://localstack.cloud/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [API Documentation](#api-documentation)
- [Event Sources](#event-sources)
- [Monitoring](#monitoring)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

This project demonstrates a **unified Lambda handler** that processes events from multiple AWS sources:

- **API Gateway** - REST API endpoints for CRUD operations
- **SQS** - Asynchronous message queue processing with DLQ support
- **EventBridge** - Scheduled tasks and custom business events

The service manages tasks with full CRUD operations, automatic event routing, and comprehensive error handling.

### Key Highlights

- ✅ **Unified Handler** - Single Lambda handles all event types
- ✅ **Type-Safe Deserialization** - Efficient event parsing with Jackson MixIns
- ✅ **DLQ Support** - Failed message handling with retry logic
- ✅ **Production-Ready** - Proper logging, validation, error handling
- ✅ **LocalStack Compatible** - Full local development environment
- ✅ **Comprehensive Tests** - 31 tests covering all scenarios
- ✅ **MapStruct Integration** - Type-safe DTO mapping
- ✅ **Lombok** - Clean, concise code

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Lambda Function                       │
│                   (task-service-dev)                         │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │          UnifiedTaskHandler                            │ │
│  │  (Request Handler - Detects Event Type)               │ │
│  └─────┬──────────────────┬────────────────┬─────────────┘ │
│        │                  │                │               │
│        ▼                  ▼                ▼               │
│  ┌─────────┐      ┌──────────┐    ┌──────────────┐       │
│  │ API GW  │      │   SQS    │    │ EventBridge  │       │
│  │ Router  │      │ Router   │    │   Handler    │       │
│  └────┬────┘      └─────┬────┘    └───────┬──────┘       │
│       │                 │                  │               │
│       ▼                 ▼                  ▼               │
│  ┌─────────────────────────────────────────────────────┐  │
│  │            Service Layer                            │  │
│  │  - ApiGatewayTaskService                           │  │
│  │  - SQSTaskService                                  │  │
│  │  - EventBridgeTaskService                          │  │
│  └─────────────────┬───────────────────────────────────┘  │
│                    │                                       │
│                    ▼                                       │
│  ┌─────────────────────────────────────────────────────┐  │
│  │            Data Layer (In-Memory Store)            │  │
│  │  - TaskData (Thread-safe ConcurrentHashMap)        │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

External Event Sources:
┌──────────────┐    ┌──────────┐    ┌──────────────┐
│ API Gateway  │    │   SQS    │    │ EventBridge  │
│  (REST API)  │    │ (Queue)  │    │ (Scheduled)  │
└──────┬───────┘    └─────┬────┘    └──────┬───────┘
       │                  │                 │
       └──────────────────┴─────────────────┘
                          │
                          ▼
                   Lambda Function
```

### Event Flow

1. **API Gateway** → REST endpoints → CRUD operations on tasks
2. **SQS** → Batch processing → Partial failure handling → DLQ routing
3. **EventBridge** → Scheduled/Custom events → Task creation

---

## ✨ Features

### API Gateway Integration
- ✅ GET /ping - Health check
- ✅ GET /task - List all tasks
- ✅ GET /task/{id} - Get task by ID
- ✅ POST /task - Create new task
- ✅ PUT /task/{id} - Update task
- ✅ DELETE /task/{id} - Delete task
- ✅ CORS enabled
- ✅ Standard response format

### SQS Integration
- ✅ Batch message processing (up to 10 messages)
- ✅ Partial batch failure support (`ReportBatchItemFailures`)
- ✅ Dead Letter Queue (DLQ) for failed messages
- ✅ Automatic retry with max receive count
- ✅ TaskRequestDTO validation
- ✅ Efficient deserialization with Jackson MixIn

### EventBridge Integration
- ✅ Scheduled events (cron/rate expressions)
- ✅ Custom business events
- ✅ Task creation from event detail
- ✅ Flexible event routing

### Technical Features
- ✅ **Unified Event Routing** - Single handler for all event types
- ✅ **Type-Safe DTOs** - MapStruct for object mapping
- ✅ **Lombok** - Clean code with annotations
- ✅ **Jackson Optimizations** - MixIn for performance
- ✅ **Comprehensive Logging** - Log4j2 with structured logging
- ✅ **Input Validation** - javax.validation annotations
- ✅ **Error Handling** - Proper HTTP status codes
- ✅ **Thread-Safe** - ConcurrentHashMap for data store

---

## 📁 Project Structure

```
SetUpProject/
├── pom.xml                          # Parent POM
├── README.md                        # This file
├── CONTRIBUTING.md                  # Contribution guidelines
│
├── taskService/                     # Main Lambda service
│   ├── pom.xml
│   ├── README.md                    # Module-specific documentation
│   └── src/
│       ├── main/java/com/project/task/
│       │   ├── handler/            # Event handlers
│       │   │   ├── UnifiedTaskHandler.java
│       │   │   └── EventBridgeHandler.java
│       │   ├── router/             # Event routers
│       │   │   ├── EventRouter.java
│       │   │   ├── ApiGatewayRouter.java
│       │   │   └── SQSRouter.java
│       │   ├── service/            # Business logic
│       │   │   ├── ApiGatewayTaskService.java
│       │   │   ├── SQSTaskService.java
│       │   │   └── EventBridgeTaskService.java
│       │   ├── model/              # Domain models
│       │   │   └── Task.java
│       │   ├── dto/                # Data transfer objects
│       │   │   └── TaskRequestDTO.java
│       │   ├── mapper/             # MapStruct mappers
│       │   │   └── TaskMapper.java
│       │   ├── data/               # Data layer
│       │   │   └── TaskData.java
│       │   └── util/               # Utilities
│       │       └── EventDeserializer.java
│       └── test/                   # Comprehensive test suite
│
├── service/                         # Base service module (shared)
│   ├── pom.xml
│   └── README.md
│
├── token/                           # Token/Auth module (future)
│   ├── pom.xml
│   └── README.md
│
├── infra/                          # Infrastructure as Code
│   ├── terraform/
│   │   ├── main.tf                 # Terraform configuration
│   │   ├── terraform.tfvars        # AWS variables
│   │   ├── terraform.localstack.tfvars
│   │   └── scripts/               # Helper scripts
│   └── docker/
│       └── docker-compose.yml      # LocalStack setup
│
└── scripts/                        # Utility scripts
    ├── test-api.ps1
    ├── deploy-localstack.ps1
    └── quick-deploy.ps1
```

---

## 🔧 Prerequisites

### Required
- **Java 21+** - OpenJDK or Amazon Corretto
- **Maven 3.9+** - Build tool
- **Docker** - For LocalStack
- **AWS CLI v2** - For AWS/LocalStack interaction

### Optional
- **Terraform 1.5+** - For infrastructure deployment
- **Postman** - For API testing
- **IntelliJ IDEA** - Recommended IDE

### Installation

**Java:**
```powershell
# Download from: https://adoptium.net/
java -version
```

**Maven:**
```powershell
# Download from: https://maven.apache.org/
mvn -version
```

**Docker:**
```powershell
# Download from: https://www.docker.com/
docker --version
```

**AWS CLI:**
```powershell
# Download from: https://aws.amazon.com/cli/
aws --version
```

---

## 🚀 Quick Start

### 1. Clone and Build

```powershell
# Clone repository
git clone <repository-url>
cd SetUpProject

# Build all modules
mvn clean install
```

### 2. Start LocalStack

```powershell
# Start LocalStack with Docker
cd infra/docker
docker-compose up -d

# Verify LocalStack is running
docker ps | Select-String localstack
```

### 3. Deploy to LocalStack

```powershell
# Deploy with Terraform
cd infra/terraform
terraform init
terraform apply -var="use_localstack=true" -auto-approve

# Get API Gateway URL
$apiUrl = (Get-Content terraform.tfstate -Raw | ConvertFrom-Json).outputs.api_gateway_invoke_url.value
Write-Host "API URL: $apiUrl"
```

### 4. Test the API

```powershell
# Health check
Invoke-RestMethod -Uri "$apiUrl/ping" -Method GET

# Get all tasks
Invoke-RestMethod -Uri "$apiUrl/task" -Method GET | ConvertTo-Json

# Create a task
$body = @{
    name = "My First Task"
    description = "Testing the API"
    status = "TODO"
} | ConvertTo-Json

Invoke-RestMethod -Uri "$apiUrl/task" -Method POST -Body $body -ContentType "application/json"
```

---

## 💻 Development

### Build Commands

```powershell
# Clean build
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Build specific module
mvn clean package -pl taskService -am

# Run tests
mvn test

# Run specific test
mvn test -Dtest=ApiGatewayIntegrationTest
```

### Project Properties

Key dependencies and versions are defined in parent `pom.xml`:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <lombok.version>1.18.30</lombok.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <jackson.version>2.17.1</jackson.version>
</properties>
```

### Module Dependencies

- **taskService** - Main Lambda function (depends on service)
- **service** - Shared base module
- **token** - Authentication module (future)

---

## 🧪 Testing

### Test Coverage

```
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
```

### Test Categories

**API Gateway Tests:**
- Health check (GET /ping)
- CRUD operations (GET, POST, PUT, DELETE)
- Error handling (404, 400, 500)
- Query parameters
- Path parameters

**SQS Tests:**
- Single message processing
- Batch processing (multiple messages)
- Partial batch failures
- DLQ routing
- Large payloads
- Invalid messages

**EventBridge Tests:**
- Scheduled events
- Custom business events
- System events

### Running Tests

```powershell
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ApiGatewayIntegrationTest

# Run tests with coverage
mvn clean verify

# Run integration tests only
mvn verify -Pintegration-tests
```

### Test with LocalStack

```powershell
# Use helper script
.\test-api.ps1

# Or test manually
$apiUrl = "http://localhost:4566/restapis/{api-id}/dev/_user_request_"

# Test API Gateway
Invoke-RestMethod -Uri "$apiUrl/ping"

# Test SQS (send message to queue)
aws sqs send-message --queue-url http://localhost:4566/000000000000/task-queue --message-body '{"name":"Test","status":"TODO"}' --endpoint-url http://localhost:4566 --region us-east-1

# Test EventBridge (invoke Lambda directly)
$event = '{"id":"test-123","source":"aws.events","detail-type":"Scheduled Event","time":"2025-12-30T10:00:00Z","detail":{}}'
aws lambda invoke --function-name task-service-dev --payload $event --endpoint-url http://localhost:4566 --region us-east-1 response.json
```

---

## 🚢 Deployment

### LocalStack (Development)

**Using Terraform:**
```powershell
cd infra/terraform
terraform apply -var="use_localstack=true" -auto-approve
```

**Using Script:**
```powershell
.\deploy-localstack.ps1
```

### AWS (Production)

**1. Update Terraform Variables:**
```hcl
# infra/terraform/terraform.tfvars
use_localstack = false
aws_region = "us-east-1"
environment = "prod"
```

**2. Deploy:**
```powershell
cd infra/terraform
terraform init
terraform plan
terraform apply
```

**3. Verify:**
```powershell
aws lambda list-functions --region us-east-1
aws apigateway get-rest-apis --region us-east-1
```

---

## 📚 API Documentation

### Base URL
```
LocalStack: http://localhost:4566/restapis/{api-id}/dev/_user_request_
AWS: https://{api-id}.execute-api.{region}.amazonaws.com/dev
```

### Endpoints

#### 1. Health Check
```http
GET /ping
```

**Response:**
```json
{
  "service": "task-service",
  "requestId": "uuid",
  "version": "1.0.0",
  "status": "healthy",
  "timestamp": 1735555200000,
  "message": "GET /ping successfully invoked"
}
```

#### 2. Get All Tasks
```http
GET /task
```

**Response:**
```json
{
  "service": "task-service",
  "status": "success",
  "data": [
    {
      "id": "task-1",
      "name": "Sample Task",
      "description": "Task description",
      "status": "TODO",
      "createdAt": 1735555200000,
      "updatedAt": 1735555200000
    }
  ],
  "count": 1
}
```

#### 3. Get Task by ID
```http
GET /task/{id}
```

**Response (200):**
```json
{
  "service": "task-service",
  "status": "success",
  "data": {
    "id": "task-1",
    "name": "Sample Task",
    "status": "TODO"
  }
}
```

**Response (404):**
```json
{
  "service": "task-service",
  "status": "error",
  "error": "Task not found with id: task-1"
}
```

#### 4. Create Task
```http
POST /task
Content-Type: application/json

{
  "name": "New Task",
  "description": "Task description",
  "status": "TODO"
}
```

**Response (201):**
```json
{
  "service": "task-service",
  "status": "success",
  "data": {
    "id": "generated-uuid",
    "name": "New Task",
    "status": "TODO",
    "createdAt": 1735555200000,
    "updatedAt": 1735555200000
  }
}
```

#### 5. Update Task
```http
PUT /task/{id}
Content-Type: application/json

{
  "name": "Updated Task",
  "status": "COMPLETED"
}
```

#### 6. Delete Task
```http
DELETE /task/{id}
```

**Response (200):**
```json
{
  "service": "task-service",
  "status": "success",
  "data": {
    "id": "task-1",
    "deleted": true
  }
}
```

For complete API documentation with examples, see [taskService/README.md](taskService/README.md)

---

## 📨 Event Sources

### API Gateway

REST API with 6 endpoints for full CRUD operations on tasks.

**Example:**
```powershell
$apiUrl = "YOUR_API_URL"
Invoke-RestMethod -Uri "$apiUrl/task" -Method GET
```

### SQS

Asynchronous message processing with batch support and DLQ.

**Message Format:**
```json
{
  "name": "Task from SQS",
  "description": "Description",
  "status": "TODO"
}
```

**Send Message:**
```powershell
aws sqs send-message \
  --queue-url http://localhost:4566/000000000000/task-queue \
  --message-body '{"name":"SQS Task","status":"TODO"}' \
  --endpoint-url http://localhost:4566
```

### EventBridge

**Scheduled Events:**
```json
{
  "id": "scheduled-123",
  "source": "aws.events",
  "detail-type": "Scheduled Event",
  "detail": {}
}
```
Creates task with name: `"scheduled event scheduled-123"`

**Custom Events:**
```json
{
  "source": "com.project.orders",
  "detail-type": "OrderCompleted",
  "detail": {
    "name": "Process Order",
    "description": "Order processing",
    "status": "TODO"
  }
}
```
Creates task from detail fields.

---

## 📊 Monitoring

### View Lambda Logs

```powershell
# LocalStack
aws logs tail /aws/lambda/task-service-dev \
  --follow \
  --endpoint-url http://localhost:4566

# AWS
aws logs tail /aws/lambda/task-service-prod --follow
```

### Check SQS Queue Status

```powershell
# Main queue
aws sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/task-queue \
  --attribute-names ApproximateNumberOfMessages \
  --endpoint-url http://localhost:4566

# DLQ
aws sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/task-queue-dlq \
  --attribute-names ApproximateNumberOfMessages \
  --endpoint-url http://localhost:4566
```

### Health Check

```powershell
# API Gateway health
$apiUrl = "YOUR_API_URL"
Invoke-RestMethod -Uri "$apiUrl/ping"
```

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Quick Contribution Guide

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Write/update tests
5. Ensure all tests pass (`mvn test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

---

## 🐛 Troubleshooting

### Common Issues

#### Issue: LocalStack not starting
```powershell
# Check Docker
docker ps

# Restart LocalStack
cd infra/docker
docker-compose down
docker-compose up -d
```

#### Issue: Lambda not found
```powershell
# Check Lambda exists
aws lambda list-functions --endpoint-url http://localhost:4566

# Redeploy
cd infra/terraform
terraform apply -var="use_localstack=true" -auto-approve
```

#### Issue: SQS messages not processing
```powershell
# Check event source mapping
aws lambda list-event-source-mappings \
  --function-name task-service-dev \
  --endpoint-url http://localhost:4566

# Recreate mapping
cd infra/terraform
.\setup-dlq.ps1
```

#### Issue: API Gateway 403 errors
```powershell
# Get correct API URL
cd infra/terraform
$apiUrl = (Get-Content terraform.tfstate -Raw | ConvertFrom-Json).outputs.api_gateway_invoke_url.value
Write-Host $apiUrl
```

For more troubleshooting, check module-specific READMEs.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/your-repo/issues)
- **Documentation:** See module READMEs for detailed documentation
- **Examples:** Check `taskService/README.md` for complete examples

---

## 🎯 Next Steps

1. ✅ Complete API Gateway, SQS, and EventBridge integration
2. ⏳ Add DynamoDB for persistent storage
3. ⏳ Implement authentication with token module
4. ⏳ Add CloudWatch metrics and alarms
5. ⏳ Create CI/CD pipeline
6. ⏳ Add API documentation with OpenAPI/Swagger

---

**Built with ❤️ using Java, AWS Lambda, and LocalStack**

