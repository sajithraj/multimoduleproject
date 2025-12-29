# LocalStack Command Reference Card

Quick copy-paste commands for LocalStack testing.

## Setup & Configuration

### Set AWS Credentials (Required for all commands)

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### Start LocalStack

```powershell
docker-compose up -d
Start-Sleep -Seconds 45  # Wait for initialization
docker ps               # Verify running
```

### Stop LocalStack

```powershell
docker-compose down
```

---

## Secrets Manager Commands

### List All Secrets

```powershell
aws secretsmanager list-secrets --endpoint-url http://localhost:4566
```

### Get Specific Secret

```powershell
aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

### Create a New Secret

```powershell
aws secretsmanager create-secret `
  --name my-secret `
  --secret-string '{"key":"value"}' `
  --endpoint-url http://localhost:4566
```

### Update Secret Value

```powershell
aws secretsmanager update-secret `
  --secret-id external-api/token `
  --secret-string '{"client_id":"new-id","client_secret":"new-secret"}' `
  --endpoint-url http://localhost:4566
```

### Delete a Secret

```powershell
aws secretsmanager delete-secret `
  --secret-id my-secret `
  --force-delete-without-recovery `
  --endpoint-url http://localhost:4566
```

---

## Lambda Commands

### List All Functions

```powershell
aws lambda list-functions --endpoint-url http://localhost:4566
```

### Get Function Details

```powershell
aws lambda get-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566
```

### Invoke Lambda (Test)

```powershell
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

# View response
Get-Content response.json
```

### Invoke with JSON Payload

```powershell
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"path":"/api/endpoint","httpMethod":"GET"}' `
  --endpoint-url http://localhost:4566 `
  response.json
```

### Update Function Code (After JAR rebuild)

```powershell
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566
```

### Update Function Configuration

```powershell
aws lambda update-function-configuration `
  --function-name my-token-auth-lambda `
  --timeout 90 `
  --memory-size 1024 `
  --endpoint-url http://localhost:4566
```

### Delete Function

```powershell
aws lambda delete-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566
```

---

## CloudWatch Logs Commands

### List Log Groups

```powershell
aws logs describe-log-groups --endpoint-url http://localhost:4566
```

### List Log Streams

```powershell
aws logs describe-log-streams `
  --log-group-name /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566
```

### Get Log Events

```powershell
aws logs get-log-events `
  --log-group-name /aws/lambda/my-token-auth-lambda `
  --log-stream-name '<stream-name>' `
  --endpoint-url http://localhost:4566
```

### Tail Logs (Real-time Follow)

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --endpoint-url http://localhost:4566
```

### Tail with Filtering

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --filter-pattern "ERROR" `
  --endpoint-url http://localhost:4566
```

---

## Docker Commands

### Check Container Status

```powershell
docker ps
docker ps -a  # Include stopped containers
```

### View Container Logs

```powershell
docker logs localstack-lambda-test
docker logs -f localstack-lambda-test  # Follow in real-time
docker logs --tail 50 localstack-lambda-test  # Last 50 lines
```

### Restart Container

```powershell
docker-compose restart
```

### Remove Container

```powershell
docker-compose down
```

### Clean Everything (Fresh Start)

```powershell
docker-compose down -v
docker system prune -f
docker-compose up -d
Start-Sleep -Seconds 45
```

### Inspect Container

```powershell
docker inspect localstack-lambda-test
```

---

## Health Checks

### Test LocalStack Endpoint

```powershell
Invoke-WebRequest -Uri "http://localhost:4566/_localstack/health" `
  -UseBasicParsing | Select-Object StatusCode
```

### Verify All Services

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Test each service
Write-Host "Testing Secrets Manager..." 
aws secretsmanager list-secrets --endpoint-url http://localhost:4566 | Out-Null

Write-Host "Testing Lambda..."
aws lambda list-functions --endpoint-url http://localhost:4566 | Out-Null

Write-Host "Testing CloudWatch Logs..."
aws logs describe-log-groups --endpoint-url http://localhost:4566 | Out-Null

Write-Host "✅ All services working!"
```

---

## Common Testing Workflows

### Workflow 1: Test Token Caching

```powershell
# Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# First invocation (fetches token)
Write-Host "First invocation - fetches token..."
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response1.json

# Second invocation (uses cached token)
Write-Host "Second invocation - uses cache..."
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response2.json

# Check logs to see cache behavior
Write-Host "Checking logs..."
aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566 | Select-Object -Last 5
```

### Workflow 2: Update Secrets and Test

```powershell
# Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Update secret with new credentials
Write-Host "Updating secret..."
aws secretsmanager update-secret `
  --secret-id external-api/token `
  --secret-string '{"client_id":"new-id","client_secret":"new-secret"}' `
  --endpoint-url http://localhost:4566

# Clear token cache (force new fetch)
Write-Host "Clear cache by redeploying Lambda..."
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

# Test with new secret
Write-Host "Testing with new secret..."
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

Get-Content response.json
```

### Workflow 3: Monitor and Debug

```powershell
# Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Tail logs in background
Start-Job -ScriptBlock {
  $env:AWS_ACCESS_KEY_ID = "test"
  $env:AWS_SECRET_ACCESS_KEY = "test"
  $env:AWS_DEFAULT_REGION = "us-east-1"
  aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
} -Name LogTail

# Run tests
Write-Host "Running tests..."
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

# Stop log tail
Stop-Job -Name LogTail
```

---

## Troubleshooting Commands

### Check if LocalStack is Healthy

```powershell
$status = docker ps --filter "name=localstack" --format "{{.Status}}"
Write-Host "LocalStack Status: $status"
```

### View All Created Secrets

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws secretsmanager list-secrets `
  --endpoint-url http://localhost:4566 | ConvertFrom-Json | 
  Select-Object -ExpandProperty SecretList | 
  Select-Object Name, CreatedDate
```

### View All Lambda Functions

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws lambda list-functions `
  --endpoint-url http://localhost:4566 | ConvertFrom-Json | 
  Select-Object -ExpandProperty Functions | 
  Select-Object FunctionName, Runtime, Handler
```

### Get Detailed Function Info

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws lambda get-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566 | ConvertFrom-Json | 
  Select-Object -ExpandProperty Configuration
```

---

## Quick Copy-Paste One-Liners

### Invoke Lambda and Show Response

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1";aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 r.json;Get-Content r.json
```

### Show All Secrets

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1";aws secretsmanager list-secrets --endpoint-url http://localhost:4566
```

### Show All Lambda Functions

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1";aws lambda list-functions --endpoint-url http://localhost:4566
```

### Tail Logs

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1";aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
```

---

**Print this page for quick reference! 📋**

