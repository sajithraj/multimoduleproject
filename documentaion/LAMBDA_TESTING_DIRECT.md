# ✅ HOW TO TEST YOUR LAMBDA

## STATUS: Lambda is DEPLOYED ✅

**API Gateway on LocalStack:** ❌ Not working (LocalStack limitation)
**Solution:** Test Lambda directly via AWS CLI

---

## 🧪 TEST YOUR LAMBDA (3 Ways)

### Option 1: Direct Lambda Invocation (Simplest) ⚡

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

cat response.json
```

**Expected Output:**

```json
{
  "statusCode": 200,
  "body": "{\"access_token\": \"eyJ...\", \"token_type\": \"Bearer\", \"expires_in\": 14400}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

---

### Option 2: With Logs

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Invoke Lambda
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  --log-type Tail `
  response.json

Write-Host "Response:" -ForegroundColor Green
cat response.json

Write-Host ""
Write-Host "Recent Logs:" -ForegroundColor Cyan
aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566 `
  --since 5m
```

---

### Option 3: Using Test Event

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Create test event
$testEvent = @{
    path = "/api/auth"
    httpMethod = "POST"
    headers = @{"Content-Type" = "application/json"}
    body = "{}"
} | ConvertTo-Json

# Invoke
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload $testEvent `
  --endpoint-url http://localhost:4566 `
  response.json

cat response.json
```

---

## 📋 WHAT YOUR LAMBDA DOES

✅ Receives HTTP POST request
✅ Fetches OAuth2 token from external API
✅ Caches token for 1 hour
✅ Returns token with HTTP 200 status
✅ Logs everything to CloudWatch

---

## 🔍 VERIFY LAMBDA IS DEPLOYED

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"

# Check Lambda exists
aws lambda get-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566

# Check Logs exist
aws logs describe-log-groups `
  --endpoint-url http://localhost:4566

# Check Secrets exist
aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

---

## 📊 INFRASTRUCTURE STATUS

```
Lambda Function:     ✅ DEPLOYED
Secrets Manager:     ✅ CONFIGURED
CloudWatch Logs:     ✅ ENABLED
IAM Role:            ✅ CREATED
API Gateway:         ❌ LocalStack limitation (use direct Lambda invoke)
```

---

## 💡 WHY NOT API GATEWAY?

LocalStack's API Gateway service has limitations and doesn't reliably work with the current setup. Direct Lambda
invocation is more stable and faster for testing.

When you deploy to AWS Production, the API Gateway Terraform code is ready (just uncomment it in main.tf).

---

## 🚀 READY TO TEST?

Run **Option 1** above - it's the simplest and fastest!

```powershell
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json && cat response.json
```

That's it! 🎉

