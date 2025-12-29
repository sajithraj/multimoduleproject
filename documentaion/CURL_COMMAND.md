# 🔧 CURL COMMAND FOR TESTING LAMBDA

## ✅ Copy & Paste This Command

### For PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"; $env:AWS_SECRET_ACCESS_KEY = "test"; $env:AWS_DEFAULT_REGION = "us-east-1"; curl -X POST http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations -H "Content-Type: application/json" -d '{}'
```

---

## 📋 Or Run Step-by-Step (Better):

### Step 1: Set Environment Variables

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### Step 2: Call Lambda

```powershell
curl -X POST http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations `
  -H "Content-Type: application/json" `
  -d '{}'
```

---

## 🎯 Expected Output:

```json
{
  "statusCode": 200,
  "body": "{\"access_token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"token_type\": \"Bearer\", \"expires_in\": 14400}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

---

## 🔍 To See Pretty Output:

### PowerShell (with formatting):

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

$response = curl -X POST http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations `
  -H "Content-Type: application/json" `
  -d '{}' | ConvertFrom-Json

Write-Host "Status Code: $($response.statusCode)" -ForegroundColor Green
Write-Host "Response Body:" -ForegroundColor Yellow
Write-Host $response.body
Write-Host "Headers:" -ForegroundColor Cyan
$response.headers | ConvertTo-Json
```

---

## ✅ Simpler Alternative Using Invoke-WebRequest:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

$response = Invoke-WebRequest -Uri "http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body '{}'

Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
Write-Host "Response:" -ForegroundColor Yellow
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

---

## 🚀 FASTEST WAY (Copy & Run):

Just copy this entire block and paste in PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"; $env:AWS_SECRET_ACCESS_KEY = "test"; $env:AWS_DEFAULT_REGION = "us-east-1"; curl -X POST http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations -H "Content-Type: application/json" -d '{}' | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

---

## 📊 What Each Parameter Means:

| Parameter                             | Meaning                     |
|---------------------------------------|-----------------------------|
| `-X POST`                             | HTTP method is POST         |
| `http://localhost:4566/...`           | LocalStack endpoint         |
| `2015-03-31/functions/...`            | AWS Lambda API version path |
| `my-token-auth-lambda`                | Your function name          |
| `/invocations`                        | Invoke the function         |
| `-H "Content-Type: application/json"` | Request header              |
| `-d '{}'`                             | Request body (empty JSON)   |

---

## ❓ If You Get Connection Error:

1. Check LocalStack is running:

```powershell
docker ps | grep localstack
```

2. If not running, start it:

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\docker
docker-compose up -d
```

---

## 🎉 That's It!

Just run the curl command above and you'll see your full Lambda response in PowerShell!

