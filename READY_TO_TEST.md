# 🚀 TaskService Lambda - READY TO TEST!

## ✅ Deployment Complete!

Your TaskService Lambda is now deployed and ready to test with Postman!

---

## 📍 Quick Start

### 1. Set Environment Variables (Always do this first!)
```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### 2. Test Lambda Immediately
```powershell
# Quick ping test
aws lambda invoke `
  --function-name task-service-dev `
  --payload '{"resource":"/ping","path":"/ping","httpMethod":"GET","headers":{"Accept":"application/json"},"body":null}' `
  --endpoint-url http://localhost:4566 `
  response.json

# View response
cat response.json | jq .
```

---

## 🧪 Test with Postman

### Endpoint for ALL Tests
```
POST http://localhost:4566/2015-03-31/functions/task-service-dev/invocations
```

### Headers
```
Content-Type: application/json
```

---

## 📋 Ready-to-Use Postman Payloads

### Test 1: API Gateway - GET /ping ⭐ START HERE
```json
{
  "resource": "/ping",
  "path": "/ping",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "body": null,
  "requestContext": {
    "requestId": "test-001"
  }
}
```
**Expected:** `{"statusCode":200,"body":"..."}`

---

### Test 2: API Gateway - GET /id/12345
```json
{
  "resource": "/id/{id}",
  "path": "/id/12345",
  "httpMethod": "GET",
  "pathParameters": {
    "id": "12345"
  },
  "headers": {
    "Accept": "application/json"
  }
}
```
**Expected:** `{"statusCode":200,"body":"..."}`

---

### Test 3: API Gateway - POST /tasks
```json
{
  "resource": "/tasks",
  "path": "/tasks",
  "httpMethod": "POST",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "body": "{\"taskName\":\"Process Order\",\"orderId\":\"ORD-001\",\"amount\":99.99}",
  "requestContext": {
    "requestId": "test-003"
  }
}
```
**Expected:** `{"statusCode":200,"body":"..."}`

---

### Test 4: SQS Event - Single Message
```json
{
  "Records": [
    {
      "messageId": "msg-001",
      "body": "{\"orderId\":\"ORD-12345\",\"amount\":150.50,\"customer\":\"John Doe\"}",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev"
    }
  ]
}
```
**Expected:** `{"batchItemFailures":[]}`

---

### Test 5: SQS Event - Batch (3 messages)
```json
{
  "Records": [
    {
      "messageId": "msg-001",
      "body": "{\"orderId\":\"ORD-001\",\"amount\":100}",
      "eventSource": "aws:sqs"
    },
    {
      "messageId": "msg-002",
      "body": "{\"orderId\":\"ORD-002\",\"amount\":200}",
      "eventSource": "aws:sqs"
    },
    {
      "messageId": "msg-003",
      "body": "{\"orderId\":\"ORD-003\",\"amount\":300}",
      "eventSource": "aws:sqs"
    }
  ]
}
```
**Expected:** `{"batchItemFailures":[]}`

---

### Test 6: EventBridge - Scheduled Event
```json
{
  "version": "0",
  "id": "event-001",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "time": "2025-12-29T10:00:00Z",
  "detail": {}
}
```
**Expected:** `"OK"`

---

### Test 7: EventBridge - Custom Event (OrderCreated)
```json
{
  "version": "0",
  "id": "custom-event-001",
  "detail-type": "OrderCreated",
  "source": "com.project.orders",
  "time": "2025-12-29T10:00:00Z",
  "detail": {
    "orderId": "ORD-98765",
    "amount": 250.75
  }
}
```
**Expected:** `"OK"`

---

### Test 8: EventBridge - Payment Event
```json
{
  "version": "0",
  "id": "payment-event-001",
  "detail-type": "PaymentProcessed",
  "source": "com.project.payments",
  "time": "2025-12-29T10:05:00Z",
  "detail": {
    "paymentId": "PAY-54321",
    "orderId": "ORD-98765",
    "amount": 250.75,
    "status": "SUCCESS"
  }
}
```
**Expected:** `"OK"`

---

## 🔍 View Logs

### Tail logs (watch live)
```powershell
aws logs tail /aws/lambda/task-service-dev `
  --follow `
  --format short `
  --endpoint-url http://localhost:4566
```

### Get last 20 log lines
```powershell
aws logs tail /aws/lambda/task-service-dev `
  --format short `
  --endpoint-url http://localhost:4566 `
  | Select-Object -Last 20
```

---

## 🔄 If You Need to Redeploy

### Option 1: Quick Deploy (Recommended)
```powershell
.\quick-deploy.ps1
```

### Option 2: Full Deploy
```powershell
.\deploy-localstack.ps1
```

### Option 3: Manual Deploy
```powershell
# Build
mvn clean package -DskipTests

# Deploy
cd infra\terraform
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

---

## ⚠️ Common Issues

### "The security token included in the request is invalid"
**Solution:** Set environment variables before EVERY command!
```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### Lambda not found
**Solution:** Redeploy
```powershell
.\quick-deploy.ps1
```

### LocalStack not running
**Solution:**
```powershell
cd infra\docker
docker-compose up -d
Start-Sleep -Seconds 30
```

---

## 📊 Verify Deployment

```powershell
# Set credentials
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

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
```

---

## 🎯 Testing Checklist

- [ ] Set environment variables
- [ ] Test API Gateway /ping
- [ ] Test API Gateway /id/{id}
- [ ] Test API Gateway POST /tasks
- [ ] Test SQS single message
- [ ] Test SQS batch messages
- [ ] Test EventBridge scheduled event
- [ ] Test EventBridge custom event
- [ ] View logs to verify routing
- [ ] Test error handling

---

## 🚀 Next: Test in Your Company AWS

Once LocalStack testing is complete:

1. Update Terraform variables
2. Deploy to dev/staging AWS account
3. Test with real API Gateway, SQS, EventBridge
4. Monitor CloudWatch logs
5. Set up alerts

---

**Status:** ✅ DEPLOYED & READY  
**Environment:** LocalStack  
**Date:** December 29, 2025

**👉 NOW TEST WITH POSTMAN!** 🚀

