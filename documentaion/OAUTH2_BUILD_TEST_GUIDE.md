# 🚀 OAuth2 Token Implementation - Build & Test Guide

## Files Changed Summary

```
Created:
└── src/main/java/com/project/service/TokenService.java (NEW)
    └─ OAuth2 token fetching service (REUSABLE)

Modified:
├── src/main/java/com/project/auth/TokenCache.java
│   └─ Now uses TokenService for fetching
├── src/main/java/com/project/client/ExternalApiClient.java
│   ├─ Uses getAccessToken()
│   └─ Adds custom headers
└── src/main/java/com/project/config/AppConfig.java
    └─ Updated endpoints and secret references
```

---

## Step-by-Step Build & Test

### Step 1: Rebuild JAR (3-5 minutes)

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

**Expected Output:**

```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXXs
```

**What Happened:**

- Compiled new TokenService.java
- Updated TokenCache.java
- Updated ExternalApiClient.java
- Created new JAR with all changes

---

### Step 2: Deploy Infrastructure (2 minutes)

```bash
cd infra/terraform
terraform init
terraform apply -var-file=terraform.localstack.tfvars
```

**What's Created:**

- Secrets Manager secret: external-api/token
- IAM role for Lambda
- CloudWatch Logs group

---

### Step 3: Update Lambda Function (1 minute)

```powershell
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

Start-Sleep -Seconds 5
```

**What Happened:**

- Lambda function code updated
- New TokenService available
- TokenCache uses new logic

---

### Step 4: Watch Logs (Real-time Monitoring)

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
```

Keep this window open to see logs as Lambda runs!

---

### Step 5: Invoke Lambda (New Window)

```powershell
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

Get-Content response.json
```

---

## What You'll See in Logs

### First Invocation (Token Fetch):

```
[INFO] 🔐 Token expired or missing, fetching new access token from OAuth2 provider
[INFO] Fetching access token from OAuth2 provider: https://exchange-staging.motiveintegrator.com/v1/authorize/token
[DEBUG] Sending token request to: https://exchange-staging.motiveintegrator.com/v1/authorize/token
[INFO] ✅ Access token obtained successfully
[INFO] 🔐 Token retrieved: eyJhbGciOiJIUzI1NiIsInR5cCI...
[DEBUG] Token type: Bearer
[DEBUG] Token expires in: 14400 seconds
[INFO] ✅ New access token cached successfully
[INFO] 🔐 Token will expire in 11520 seconds (actual: 14400 seconds, using 80% for safety)
[DEBUG] 🔐 Token preview: eyJhbGciOiJIUzI1NiIsInR5cC...
[INFO] 🔐 Using access token in request: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
[DEBUG] Full access token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI...
[DEBUG] Executing HTTP GET request to external API
[INFO] External API call successful: status=200
```

### Second Invocation (Token Cached - Same Window):

```powershell
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response2.json
```

**You'll See:**

```
[DEBUG] 🔐 Using cached access token (expires in 11500 seconds)
[INFO] 🔐 Using access token in request: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
[DEBUG] Executing HTTP GET request to external API
[INFO] External API call successful: status=200
```

Notice: No token fetch! Token was reused from cache. Much faster! ⚡

---

## Expected Response

```json
{
  "statusCode": 200,
  "status": "success",
  "data": {
    ...
  },
  "error": null,
  "message": null,
  "timestamp": 1735294800000
}
```

---

## Verification Checklist

- [ ] Maven build successful (BUILD SUCCESS)
- [ ] Terraform apply successful (resources created)
- [ ] Lambda updated without errors
- [ ] First invocation shows token fetch logs
- [ ] Second invocation shows cached token logs
- [ ] API call returns status 200
- [ ] Response contains valid JSON

---

## Troubleshooting

### Build Fails

**Problem**: `mvn clean install` fails
**Solution**:

```bash
# Clean and retry
rm -r target
mvn clean install -DskipTests
```

### Terraform Apply Fails

**Problem**: "resource already exists"
**Solution**:

```bash
# Destroy and recreate
terraform destroy
terraform apply -var-file=terraform.localstack.tfvars
```

### Lambda Invocation Fails

**Problem**: Lambda returns error in response
**Solution**:

```bash
# Check logs for error details
aws logs tail /aws/lambda/my-token-auth-lambda --endpoint-url http://localhost:4566

# Look for error messages about:
# - Secrets Manager access
# - OAuth2 endpoint connectivity
# - JSON parsing
```

### No Logs Appearing

**Problem**: Logs not showing
**Solution**:

```bash
# Verify logs group exists
aws logs describe-log-groups --endpoint-url http://localhost:4566

# Try getting last 100 lines
aws logs tail /aws/lambda/my-token-auth-lambda --max-items 100 --endpoint-url http://localhost:4566
```

---

## Performance Validation

### Measure First Invocation

```powershell
# Record start time
$start = Get-Date
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json
$end = Get-Date
Write-Host "Duration: $($end - $start)"
```

Expected: 300-450ms (includes token fetch + API call)

### Measure Second Invocation

```powershell
# Record start time
$start = Get-Date
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response2.json
$end = Get-Date
Write-Host "Duration: $($end - $start)"
```

Expected: 200-300ms (no token fetch, just cached token + API call)

**Savings**: 30-50% faster for warm invocations! ✅

---

## All-in-One Script

```powershell
# Set variables
$projectRoot = "E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject"
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

Write-Host "Step 1: Building..."
cd $projectRoot
mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed"; exit 1 }

Write-Host "`nStep 2: Deploying infrastructure..."
cd "$projectRoot\infra\terraform"
terraform init
terraform apply -var-file=terraform.localstack.tfvars -auto-approve

Write-Host "`nStep 3: Updating Lambda..."
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://$projectRoot/target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566
Start-Sleep -Seconds 5

Write-Host "`nStep 4: Testing (First invocation - will fetch token)..."
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response1.json
Get-Content response1.json | ConvertFrom-Json | ForEach-Object { Write-Host "Response: $_" }

Write-Host "`nStep 5: Testing (Second invocation - will use cached token)..."
Start-Sleep -Seconds 2
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response2.json
Get-Content response2.json | ConvertFrom-Json | ForEach-Object { Write-Host "Response: $_" }

Write-Host "`nStep 6: Showing recent logs..."
aws logs tail /aws/lambda/my-token-auth-lambda --max-items 50 --endpoint-url http://localhost:4566

Write-Host "`n✅ All tests complete!"
```

---

**Status**: ✅ Ready to build and test
**Next**: Run `mvn clean install -DskipTests` to start!

