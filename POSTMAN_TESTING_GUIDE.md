# 🎯 Postman Testing Guide - LocalStack TaskService

## ✅ Setup Complete!

Your Lambda functions are deployed and ready to test. I've created a **Postman collection** for you.

---

## 📦 Import Postman Collection

**File Location:**  
`E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\TaskService-LocalStack.postman_collection.json`

**Steps:**

1. Open Postman
2. Click **Import** button
3. Select the `TaskService-LocalStack.postman_collection.json` file
4. The collection will be imported with all endpoints ready to use!

---

## 🔗 Direct URLs for Postman

### 1. API Gateway Endpoints

#### ✅ Ping (Health Check)

```
GET http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/ping
```

#### ✅ Get All Tasks

```
GET http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task
```

#### ✅ Get Task by ID

```
GET http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task/task-1
```

#### ✅ Create Task

```
POST http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task

Headers:
Content-Type: application/json

Body (raw JSON):
{
    "name": "Test Task from Postman",
    "description": "Testing task creation",
    "status": "TODO"
}
```

#### ✅ Update Task

```
PUT http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task/task-1

Headers:
Content-Type: application/json

Body (raw JSON):
{
    "name": "Updated Task Name",
    "description": "Updated description",
    "status": "IN_PROGRESS"
}
```

#### ✅ Delete Task

```
DELETE http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task/task-3
```

---

### 2. SQS Testing (Automatic Lambda Trigger)

**IMPORTANT:** For SQS testing, use AWS CLI from PowerShell (Postman has encoding issues with AWS API)

```powershell
# Send SQS message (Lambda will be triggered automatically)
aws sqs send-message `
  --queue-url "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/task-queue-dev-local" `
  --message-body '{"name":"Task from SQS","description":"Testing SQS","status":"TODO"}' `
  --endpoint-url http://localhost:4566 `
  --region us-east-1
```

---

## 📋 Testing Checklist

### ✅ API Gateway Tests (Use Postman)

1. **Test Ping:**
    - URL: `GET http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/ping`
    - Expected: `200 OK` with service status

2. **Get All Tasks:**
    - URL: `GET http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task`
    - Expected: List of 3 tasks (task-1, task-2, task-3)

3. **Get Task by ID:**
    - URL: `GET http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task/task-1`
    - Expected: Single task object

4. **Create Task:**
    - URL: `POST http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task`
    - Body: `{"name":"My Task","description":"Test","status":"TODO"}`
    - Expected: `201 Created` with new task including auto-generated ID

5. **Update Task:**
    - URL: `PUT http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task/task-1`
    - Body: `{"name":"Updated","status":"IN_PROGRESS"}`
    - Expected: `200 OK` with updated task

6. **Delete Task:**
    - URL: `DELETE http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task/task-3`
    - Expected: `200 OK` with deleted task info

---

### ✅ SQS Tests (Use PowerShell)

**Send message to SQS queue:**

```powershell
aws sqs send-message `
  --queue-url "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/task-queue-dev-local" `
  --message-body '{"name":"SQS Task","description":"From SQS Queue","status":"TODO"}' `
  --endpoint-url http://localhost:4566 `
  --region us-east-1
```

**Expected:**

- Message sent confirmation
- Lambda automatically triggered
- Check logs to see task creation

---

## 📊 View Lambda Logs

```powershell
# Follow logs in real-time
aws logs tail /aws/lambda/task-service-dev-local `
  --follow `
  --endpoint-url http://localhost:4566 `
  --region us-east-1

# View last 5 minutes
aws logs tail /aws/lambda/task-service-dev-local `
  --since 5m `
  --endpoint-url http://localhost:4566 `
  --region us-east-1
```

---

## 🎯 Quick Test from PowerShell

```powershell
# Test API Gateway - Ping
curl -X GET "http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/ping"

# Test API Gateway - Get All Tasks
curl -X GET "http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task"

# Test API Gateway - Create Task
curl -X POST "http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/task" `
  -H "Content-Type: application/json" `
  -d '{"name":"PowerShell Task","description":"Test","status":"TODO"}'

# Test SQS (triggers Lambda)
aws sqs send-message `
  --queue-url "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/task-queue-dev-local" `
  --message-body '{"name":"SQS Task","description":"Auto-triggered","status":"TODO"}' `
  --endpoint-url http://localhost:4566 `
  --region us-east-1
```

---

## 🐛 Troubleshooting

### Issue: "Connection refused"

**Solution:** Make sure LocalStack is running

```powershell
docker ps | Select-String localstack
```

### Issue: "API not found"

**Solution:** Redeploy Terraform

```powershell
cd infra/terraform
terraform apply -var-file="terraform.localstack.tfvars" -auto-approve
```

### Issue: "No logs appearing"

**Solution:** Check if log group exists

```powershell
aws logs describe-log-groups --endpoint-url http://localhost:4566 --region us-east-1
```

---

## ✅ Summary

### What Works:

- ✅ API Gateway endpoints (test via Postman)
- ✅ SQS integration (test via AWS CLI)
- ✅ EventBridge scheduled tasks (triggers every 5 minutes)
- ✅ Lambda Function URL (for API Gateway-style requests)
- ✅ CloudWatch Logs

### Key URLs:

- **API Gateway Base:** `http://localhost:4566/restapis/4chxakhroa/dev-local/_user_request_/`
- **SQS Queue:** `http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/task-queue-dev-local`
- **Lambda Function:** `task-service-dev-local`

### Postman Collection:

📁 **File:** `TaskService-LocalStack.postman_collection.json`  
🚀 **Import it** and start testing immediately!

---

**Your LocalStack environment is fully deployed and ready for testing!** 🎉

**Recommendation:** Use the Postman collection for API Gateway tests - it's already configured with all endpoints!

