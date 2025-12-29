# 🧪 API GATEWAY TESTING GUIDE

## ✅ Deployment Status

Your infrastructure includes:

- ✅ Lambda Function: `my-token-auth-lambda`
- ✅ API Gateway REST API: `token-auth-api-dev-local`
- ✅ API Gateway Stage: `dev-local`
- ✅ Endpoint: POST `/api/auth`
- ✅ CloudWatch Logs: `/aws/lambda/my-token-auth-lambda`

---

## 🧪 HOW TO TEST (3 Methods)

### Method 1: Using cURL (Recommended for PowerShell)

**Step 1: Set Environment Variables**

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

**Step 2: Get the API Endpoint**

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform

$API_ENDPOINT = terraform output -raw api_gateway_endpoint

Write-Host "API Endpoint: $API_ENDPOINT"
```

**Step 3: Call the API**

```powershell
curl -X POST $API_ENDPOINT `
  -H "Content-Type: application/json" `
  -d '{}'
```

**Expected Response:**

```json
{
  "statusCode": 200,
  "body": "{\"access_token\": \"...\", \"token_type\": \"Bearer\", \"expires_in\": 14400}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

---

### Method 2: Using AWS CLI

```powershell
# Get endpoint
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform

$API_ID = terraform output -raw api_gateway_invoke_url | `
  Select-Object -ExpandProperty {$_.Split('/')[3]}

# Test directly via API Gateway
aws apigateway test-invoke-method `
  --rest-api-id $API_ID `
  --resource-id <resource-id> `
  --http-method POST `
  --endpoint-url http://localhost:4566
```

---

### Method 3: Using Invoke-WebRequest (PowerShell Native)

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform

$endpoint = terraform output -raw api_gateway_endpoint

$body = @{} | ConvertTo-Json

$response = Invoke-WebRequest -Uri $endpoint `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body

Write-Host "Status Code: $($response.StatusCode)"
Write-Host "Response: $($response.Content)"
```

---

## 🔍 STEP-BY-STEP TESTING

### Complete Test Script

Copy and paste this entire script into PowerShell:

```powershell
# ========================================
# COMPLETE API GATEWAY TESTING SCRIPT
# ========================================

# 1. Set environment
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
$env:PATH = "C:\terraform;$env:PATH"

Write-Host "========================================" -ForegroundColor Green
Write-Host "API GATEWAY TESTING" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# 2. Get endpoint
Write-Host "Step 1: Getting API Endpoint..." -ForegroundColor Cyan
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform

$API_ENDPOINT = terraform output -raw api_gateway_endpoint
Write-Host "✅ Endpoint: $API_ENDPOINT" -ForegroundColor Green
Write-Host ""

# 3. Test API
Write-Host "Step 2: Testing API..." -ForegroundColor Cyan
Write-Host ""

$response = curl -X POST $API_ENDPOINT `
  -H "Content-Type: application/json" `
  -d '{}'

Write-Host "Response Received:" -ForegroundColor Yellow
Write-Host $response
Write-Host ""

# 4. Check Logs
Write-Host "Step 3: Checking CloudWatch Logs..." -ForegroundColor Cyan
Write-Host ""

aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566 `
  --since 5m

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ TEST COMPLETE" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
```

---

## 📋 TROUBLESHOOTING

### Issue 1: "404 Not Found"

**Cause:** API Gateway not deployed or endpoint wrong
**Fix:**

```powershell
# Verify endpoint
terraform output api_gateway_endpoint

# Verify API Gateway exists
aws apigateway get-rest-apis --endpoint-url http://localhost:4566
```

### Issue 2: "Connection Refused"

**Cause:** LocalStack not running
**Fix:**

```bash
# Check LocalStack
docker ps | grep localstack

# If not running, start it
cd infra/docker
docker-compose up -d
```

### Issue 3: "Invalid Request"

**Cause:** Missing headers or wrong method
**Fix:**

```powershell
# Ensure headers are set
-H "Content-Type: application/json"

# Ensure POST method
-X POST

# Ensure valid JSON body
-d '{}'
```

### Issue 4: "Token Fetch Error"

**Cause:** Secrets Manager not configured
**Fix:**

```powershell
# Check secrets
aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

---

## ✅ VERIFICATION CHECKLIST

Before testing, verify:

- [ ] LocalStack is running: `docker ps | grep localstack`
- [ ] Lambda deployed:
  `aws lambda get-function --function-name my-token-auth-lambda --endpoint-url http://localhost:4566`
- [ ] API Gateway exists: `aws apigateway get-rest-apis --endpoint-url http://localhost:4566`
- [ ] Secrets Manager has credentials:
  `aws secretsmanager get-secret-value --secret-id external-api/token --endpoint-url http://localhost:4566`
- [ ] CloudWatch logs exist: `aws logs describe-log-groups --endpoint-url http://localhost:4566`

---

## 📊 EXPECTED TEST RESULTS

### Success Response

```
Status Code: 200
Content-Type: application/json
Body: {
  "statusCode": 200,
  "body": "API response data",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

### Error Response (if credentials missing)

```
Status Code: 502
Body: {
  "error": "External API error: Failed to fetch authentication token"
}
```

### Logs Should Show

```
Received request: path=/api/auth, method=POST, requestId=xxx
Fetching fresh auth token from Secrets Manager
Token fetch successful
✅ Access token obtained successfully
Calling external API
```

---

## 🔗 API ENDPOINT DETAILS

| Property     | Value                          |
|--------------|--------------------------------|
| Method       | POST                           |
| Path         | /api/auth                      |
| Base URL     | http://localhost:4566          |
| Full URL     | http://localhost:4566/api/auth |
| Content-Type | application/json               |
| Auth         | None (open)                    |
| CORS         | Enabled (*)                    |

---

## 💡 QUICK TEST COMMAND

The fastest way to test:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"; $env:AWS_SECRET_ACCESS_KEY = "test"; $env:AWS_DEFAULT_REGION = "us-east-1"; cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\terraform; $endpoint = terraform output -raw api_gateway_endpoint; curl -X POST $endpoint -H "Content-Type: application/json" -d '{}'
```

---

## 📚 RELATED DOCUMENTATION

- `API_GATEWAY_CONTRACT.md` - Full API specifications
- `DEPLOYMENT_VERIFICATION.md` - Deployment steps
- `TERRAFORM_FIX_GUIDE.md` - Infrastructure configuration

---

**Ready to test? Run the Complete Test Script above!** ✨

