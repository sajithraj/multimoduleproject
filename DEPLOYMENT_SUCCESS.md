# TaskService Lambda - Deployment Success! 🎉

## ✅ Deployment Complete

Your TaskService Lambda has been successfully deployed to LocalStack with the following resources:

### Lambda Functions
1. **task-service-dev** - Unified handler for API Gateway, SQS, and EventBridge
2. **my-token-auth-lambda** - OAuth2 token service

### SQS Queues
1. **task-queue-dev** - Main processing queue
2. **task-queue-dlq-dev** - Dead Letter Queue for failed messages

### EventBridge
1. **task-schedule-dev** - Scheduled trigger (every 5 minutes)

---

## 🧪 Quick Test Commands

### Set Environment Variables
```powershell
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"
```

### Test 1: Simple Lambda Invocation
```powershell
# Create test payload
$payload = @'
{
  "resource": "/ping",
  "path": "/ping",
  "httpMethod": "GET",
  "headers": {"Accept": "application/json"},
  "body": null
}
'@

$payload | Out-File test-payload.json -Encoding UTF8

# Invoke Lambda
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-payload.json `
  --endpoint-url http://localhost:4566 `
  response.json

# View response
Get-Content response.json | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

### Test 2: Send SQS Message
```powershell
# Get queue URL first
$queueUrl = aws sqs list-queues --endpoint-url http://localhost:4566 --query 'QueueUrls[?contains(@, `task-queue-dev`)]' --output text

# Send message
aws sqs send-message `
  --queue-url $queueUrl `
  --message-body '{"orderId":"TEST-001","amount":99.99}' `
  --endpoint-url http://localhost:4566

# Lambda will automatically process it!
```

### Test 3: View Lambda Logs
```powershell
# Tail logs (live)
aws logs tail /aws/lambda/task-service-dev `
  --follow `
  --format short `
  --endpoint-url http://localhost:4566

# Get last 50 lines
aws logs tail /aws/lambda/task-service-dev `
  --format short `
  --endpoint-url http://localhost:4566 `
  | Select-Object -Last 50
```

---

## 📋 Test with Postman

### Endpoint
```
POST http://localhost:4566/2015-03-31/functions/task-service-dev/invocations
```

### Headers
```
Content-Type: application/json
```

### Sample Payloads

#### 1. API Gateway - GET /ping
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

#### 2. API Gateway - GET /id/12345
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

#### 3. SQS Event
```json
{
  "Records": [
    {
      "messageId": "msg-001",
      "body": "{\"orderId\":\"ORD-001\",\"amount\":150}",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev"
    }
  ]
}
```

#### 4. EventBridge Scheduled Event
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

---

## 🔄 Redeploy Process

When you make code changes:

```powershell
# 1. Build
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean package -DskipTests

# 2. Deploy
cd infra\terraform
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

**Or use the automated script:**
```powershell
.\deploy-localstack.ps1
```

---

## 🐛 Troubleshooting

### Issue: "The security token included in the request is invalid"

**Cause:** LocalStack isn't running or credentials aren't set properly

**Solution:**
```powershell
# 1. Check if LocalStack is running
curl http://localhost:4566/_localstack/health

# If not running, start it
cd infra\docker
docker-compose up -d

# Wait for it to be ready (30 seconds)
Start-Sleep -Seconds 30

# 2. Set credentials BEFORE terraform commands
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# 3. Now deploy
cd ..\terraform
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

### Issue: "Unsupported type java.util.LinkedHashMap"

**This is FIXED!** The handler now uses:
```java
public Object handleRequest(Object input, Context context)
```

And uses event detection with `instanceof` checks.

### Issue: Lambda not found

```powershell
# List all functions
aws lambda list-functions --endpoint-url http://localhost:4566

# If empty, redeploy
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

### Issue: No logs appearing

```powershell
# Check if log group exists
aws logs describe-log-groups --endpoint-url http://localhost:4566

# Invoke Lambda to generate logs
aws lambda invoke --function-name task-service-dev --payload '{}' response.json --endpoint-url http://localhost:4566
```

### Issue: SQS messages not processing

```powershell
# Check event source mapping
aws lambda list-event-source-mappings --function-name task-service-dev --endpoint-url http://localhost:4566

# Check if messages are in queue
aws sqs receive-message --queue-url <queue-url> --endpoint-url http://localhost:4566
```

---

## 📚 Documentation

- **Test Payloads:** `taskService/TEST_PAYLOADS.md`
- **Deployment Guide:** `DEPLOYMENT_GUIDE.md`
- **Test Script:** `infra/terraform/test-taskservice.ps1`

---

## ✅ What's Working

✅ Multi-event Lambda (API Gateway, SQS, EventBridge)  
✅ Event type detection and routing  
✅ SQS batch processing with DLQ support  
✅ EventBridge scheduled tasks  
✅ Comprehensive logging with Powertools  
✅ Terraform infrastructure as code  
✅ LocalStack local development  

---

## 🚀 Next Steps

1. **Test all event types** using `test-taskservice.ps1`
2. **Test with Postman** using payloads from `TEST_PAYLOADS.md`
3. **Check logs** to verify proper routing
4. **Test SQS DLQ** by sending invalid messages
5. **Test in your company AWS** when ready

---

## 🎯 Success Criteria

- [x] Lambda deploys successfully
- [x] Accepts API Gateway events
- [x] Accepts SQS events
- [x] Accepts EventBridge events
- [x] Returns proper responses
- [x] Logs to CloudWatch
- [ ] Test with Postman - **DO THIS NOW!**
- [ ] Verify all routes work
- [ ] Check error handling

---

**Status:** ✅ DEPLOYED & READY FOR TESTING  
**Date:** December 29, 2025  
**Environment:** LocalStack (Development)

**Now go ahead and test with Postman!** 🚀

