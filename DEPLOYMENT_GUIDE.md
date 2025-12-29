# TaskService Lambda - Quick Deployment & Testing Guide

## Prerequisites

✅ Java 21 installed  
✅ Maven 3.8+ installed  
✅ Docker Desktop running  
✅ LocalStack running (docker-compose up -d)  
✅ AWS CLI installed  
✅ Terraform installed  

---

## Step 1: Build the Project

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

# Build all modules
mvn clean package -DskipTests

# Verify JARs are created
ls taskService\target\taskService-1.0-SNAPSHOT.jar
ls service\target\service-1.0-SNAPSHOT.jar
```

**Expected Output:**
```
✓ taskService-1.0-SNAPSHOT.jar (15-20 MB)
✓ service-1.0-SNAPSHOT.jar (15-20 MB)
```

---

## Step 2: Start LocalStack

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\docker

# Start LocalStack
docker-compose up -d

# Verify it's running
docker ps
curl http://localhost:4566/_localstack/health
```

**Expected Output:**
```json
{
  "services": {
    "lambda": "running",
    "logs": "running",
    "secretsmanager": "running",
    "sqs": "running",
    "events": "running"
  }
}
```

---

## Step 3: Deploy with Terraform

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform

# Set LocalStack credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Initialize Terraform
terraform init

# Plan deployment
terraform plan -var="use_localstack=true" -var="environment=dev"

# Apply deployment
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

**Expected Output:**
```
Apply complete! Resources: 15 added, 0 changed, 0 destroyed.

Outputs:

task_service_function_name = "task-service-dev"
task_service_function_url = "http://localhost:4566/..."
task_queue_url = "http://localhost:4566/000000000000/task-queue-dev"
```

---

## Step 4: Verify Deployment

```powershell
# List Lambda functions
aws lambda list-functions --endpoint-url http://localhost:4566 --query 'Functions[].FunctionName'

# Expected output:
# [
#   "my-token-auth-lambda",
#   "task-service-dev"
# ]

# List SQS queues
aws sqs list-queues --endpoint-url http://localhost:4566

# Expected output:
# {
#   "QueueUrls": [
#     "http://localhost:4566/000000000000/task-queue-dev",
#     "http://localhost:4566/000000000000/task-queue-dlq-dev"
#   ]
# }

# List EventBridge rules
aws events list-rules --endpoint-url http://localhost:4566

# Expected output shows:
# - task-schedule-dev (scheduled every 5 minutes)
```

---

## Step 5: Run Automated Tests

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform

# Run the test script
.\test-taskservice.ps1
```

**This script will test:**
1. ✅ API Gateway - GET /ping
2. ✅ API Gateway - GET /id/{id}
3. ✅ API Gateway - POST /tasks
4. ✅ SQS - Single message
5. ✅ SQS - Batch messages (3)
6. ✅ EventBridge - Scheduled event
7. ✅ EventBridge - Custom event (OrderCreated)

---

## Step 6: Manual Testing with Postman

### Import Test Payloads

1. Open Postman
2. Create new Collection: "TaskService Lambda Tests"
3. Import payloads from: `taskService/TEST_PAYLOADS.md`

### Test API Gateway Events

**Endpoint:** 
```
POST http://localhost:4566/2015-03-31/functions/task-service-dev/invocations
```

**Headers:**
```
Content-Type: application/json
Accept: application/json
```

**Body (GET /ping):**
```json
{
  "resource": "/ping",
  "path": "/ping",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "queryStringParameters": null,
  "pathParameters": null,
  "body": null,
  "isBase64Encoded": false,
  "requestContext": {
    "requestId": "test-request-id-001",
    "accountId": "123456789012",
    "stage": "dev"
  }
}
```

**Expected Response:**
```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "{\"message\":\"Pong!\",\"timestamp\":\"2025-12-29T...\"}"
}
```

---

## Step 7: Test SQS Integration

### Send Message to SQS

```powershell
aws sqs send-message `
  --queue-url http://localhost:4566/000000000000/task-queue-dev `
  --message-body '{"orderId":"TEST-001","amount":99.99,"customer":"Test User"}' `
  --endpoint-url http://localhost:4566
```

### Check if Lambda Processed It

```powershell
# Check Lambda logs
aws logs tail /aws/lambda/task-service-dev --follow --endpoint-url http://localhost:4566
```

**Expected Log Output:**
```
Processing SQS message: orderId=TEST-001
SQS message processed successfully
```

---

## Step 8: View Lambda Logs

### Tail Logs (Live)

```powershell
aws logs tail /aws/lambda/task-service-dev `
  --follow `
  --format short `
  --endpoint-url http://localhost:4566
```

### Get Last 50 Log Lines

```powershell
aws logs tail /aws/lambda/task-service-dev `
  --format short `
  --endpoint-url http://localhost:4566 `
  | Select-Object -Last 50
```

### Filter Logs by Pattern

```powershell
aws logs tail /aws/lambda/task-service-dev `
  --filter-pattern "ERROR" `
  --endpoint-url http://localhost:4566
```

---

## Common Issues & Solutions

### Issue 1: "Unsupported type java.util.LinkedHashMap"

**Cause:** Lambda can't deserialize the input object  
**Solution:** 
- Handler signature must be: `Object handleRequest(Object input, Context context)`
- Use `instanceof` to detect event type
- Don't try to cast directly to specific event types in signature

**Fixed in:** `UnifiedTaskHandler.java` - uses Object input and EventRouter for detection

---

### Issue 2: Lambda Not Found

```powershell
# Check if Lambda exists
aws lambda get-function `
  --function-name task-service-dev `
  --endpoint-url http://localhost:4566

# If not found, redeploy
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

---

### Issue 3: No Response from Lambda

```powershell
# Check Lambda configuration
aws lambda get-function-configuration `
  --function-name task-service-dev `
  --endpoint-url http://localhost:4566

# Check logs for errors
aws logs tail /aws/lambda/task-service-dev `
  --endpoint-url http://localhost:4566 `
  | Select-String "ERROR"
```

---

### Issue 4: SQS Messages Not Processing

```powershell
# Check event source mapping
aws lambda list-event-source-mappings `
  --function-name task-service-dev `
  --endpoint-url http://localhost:4566

# Check if messages are in queue
aws sqs receive-message `
  --queue-url http://localhost:4566/000000000000/task-queue-dev `
  --endpoint-url http://localhost:4566

# Check DLQ for failed messages
aws sqs receive-message `
  --queue-url http://localhost:4566/000000000000/task-queue-dlq-dev `
  --endpoint-url http://localhost:4566
```

---

### Issue 5: LocalStack Not Running

```powershell
# Check Docker containers
docker ps

# If not running, start it
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\docker
docker-compose up -d

# Check logs
docker-compose logs -f localstack
```

---

## Testing Checklist

- [ ] Build completed successfully (JAR files created)
- [ ] LocalStack is running
- [ ] Terraform apply completed without errors
- [ ] Lambda function exists in LocalStack
- [ ] SQS queues created
- [ ] EventBridge rule created
- [ ] API Gateway /ping returns 200 OK
- [ ] API Gateway /id/{id} returns resource
- [ ] API Gateway POST /tasks creates task
- [ ] SQS message triggers Lambda
- [ ] EventBridge scheduled event works
- [ ] EventBridge custom event works
- [ ] Logs are visible in CloudWatch

---

## Quick Reference Commands

### Deploy Everything
```powershell
mvn clean package -DskipTests && `
cd infra\terraform && `
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve && `
cd ..\..
```

### Test Everything
```powershell
cd infra\terraform
.\test-taskservice.ps1
```

### View Logs
```powershell
aws logs tail /aws/lambda/task-service-dev --follow --endpoint-url http://localhost:4566
```

### Send Test SQS Message
```powershell
aws sqs send-message --queue-url http://localhost:4566/000000000000/task-queue-dev --message-body '{"test":"data"}' --endpoint-url http://localhost:4566
```

### Cleanup
```powershell
cd infra\terraform
terraform destroy -var="use_localstack=true" -var="environment=dev" -auto-approve
```

---

## Next Steps

1. ✅ Deploy to LocalStack and test
2. ✅ Test all event types (API Gateway, SQS, EventBridge)
3. ✅ Verify logging and error handling
4. 🔜 Test in your company's AWS account
5. 🔜 Add authentication/authorization
6. 🔜 Add DynamoDB for state management
7. 🔜 Add monitoring and alerting
8. 🔜 Add CI/CD pipeline

---

**Last Updated:** December 29, 2025  
**Version:** 1.0  
**Status:** Ready for Testing ✅

