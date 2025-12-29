# LocalStack Docker Setup - Complete Installation Guide

## Prerequisites Checklist

Before starting, ensure you have:

- [ ] **Docker Desktop** installed and running
    - Download: https://www.docker.com/products/docker-desktop
    - Windows: Requires Windows 10/11 Pro or Enterprise with WSL 2
    - Mac: Intel or Apple Silicon version
    - Linux: Docker Engine with Docker Compose

- [ ] **AWS CLI v2** installed
    - Download: https://aws.amazon.com/cli/
    - Verify: `aws --version`

- [ ] **Java 21 JDK** installed
    - Download: https://www.oracle.com/java/technologies/downloads/#java21
    - Verify: `java -version`

- [ ] **Maven 3.8+** installed
    - Download: https://maven.apache.org/download.cgi
    - Verify: `mvn --version`

- [ ] **PowerShell 5.1+** (Windows) or **Bash** (Mac/Linux)

---

## Step 1: Verify Prerequisites

### Windows PowerShell:

```powershell
# Check Docker
docker --version
docker ps

# Check AWS CLI
aws --version

# Check Java
java -version

# Check Maven
mvn --version
```

### Mac/Linux Bash:

```bash
# Check Docker
docker --version
docker ps

# Check AWS CLI
aws --version

# Check Java
java -version

# Check Maven
mvn --version
```

If any command fails, install the missing tool from the links above.

---

## Step 2: Start LocalStack

Navigate to your project directory and start LocalStack.

### Windows PowerShell:

```powershell
cd "E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject"

# Start LocalStack and LocalStack UI
docker-compose up -d

# Wait 30-60 seconds for initialization
Start-Sleep -Seconds 30

# Verify containers are running
docker ps
```

### Mac/Linux Bash:

```bash
cd ~/path/to/SetUpProject

# Start LocalStack and LocalStack UI
docker-compose up -d

# Wait for initialization
sleep 30

# Verify containers are running
docker ps
```

**Expected Output:**

```
CONTAINER ID   IMAGE                              STATUS           PORTS
abc123...      localstack/localstack:latest       Up 30 seconds    0.0.0.0:4566->4566/tcp
def456...      localstack/localstack-ui:latest    Up 30 seconds    0.0.0.0:8080->8080/tcp
```

---

## Step 3: Verify LocalStack is Ready

### Test Connection:

**Windows PowerShell:**

```powershell
# Test LocalStack endpoint
Invoke-WebRequest http://localhost:4566 | Select-Object StatusCode

# Should return: StatusCode : 200
```

**Mac/Linux Bash:**

```bash
# Test LocalStack endpoint
curl -s http://localhost:4566 | head -c 50

# Should return: {"services": {"lambda": {...}}}
```

---

## Step 4: Check AWS Resources Created

LocalStack should have automatically created the required resources. Verify:

### Windows PowerShell:

```powershell
# Set AWS credentials (for LocalStack)
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# List secrets
aws secretsmanager list-secrets --endpoint-url http://localhost:4566

# Get the secret
aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

### Mac/Linux Bash:

```bash
# Set AWS credentials (for LocalStack)
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# List secrets
aws secretsmanager list-secrets --endpoint-url http://localhost:4566

# Get the secret
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566
```

**Expected Output:**

```json
{
  "ARN": "arn:aws:secretsmanager:us-east-1:000000000000:secret:external-api/token-abc123",
  "Name": "external-api/token",
  "VersionId": "123abc...",
  "SecretString": "{\"client_id\": \"test-client-id\", \"client_secret\": \"test-client-secret\"}",
  "VersionStages": [
    "AWSCURRENT"
  ],
  "CreatedDate": 1735294245.0
}
```

---

## Step 5: Build Your Lambda JAR

```powershell
# Windows
cd "E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject"
mvn clean install
```

```bash
# Mac/Linux
cd ~/path/to/SetUpProject
mvn clean install
```

**Expected Output:**

```
[INFO] Building jar: .../target/SetUpProject-1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

---

## Step 6: Create Lambda Function in LocalStack

### Windows PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws lambda create-function `
  --function-name my-token-auth-lambda `
  --runtime java21 `
  --role arn:aws:iam::000000000000:role/lambda-execution-role `
  --handler org.example.Main::handleRequest `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --environment Variables="{
    EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
    TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
    CLIENT_ID=test-client-id,
    CLIENT_SECRET=test-client-secret,
    TOKEN_SECRET_NAME=external-api/token
  }" `
  --timeout 60 `
  --memory-size 512 `
  --endpoint-url http://localhost:4566
```

### Mac/Linux Bash:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

aws lambda create-function \
  --function-name my-token-auth-lambda \
  --runtime java21 \
  --role arn:aws:iam::000000000000:role/lambda-execution-role \
  --handler org.example.Main::handleRequest \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --environment Variables="{
    EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
    TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
    CLIENT_ID=test-client-id,
    CLIENT_SECRET=test-client-secret,
    TOKEN_SECRET_NAME=external-api/token
  }" \
  --timeout 60 \
  --memory-size 512 \
  --endpoint-url http://localhost:4566
```

---

## Step 7: Test Your Lambda Function

### Windows PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Invoke Lambda
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

# View response
Get-Content response.json | ConvertFrom-Json | ConvertTo-Json
```

### Mac/Linux Bash:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Invoke Lambda
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json

# View response
cat response.json | jq .
```

---

## Step 8: View CloudWatch Logs

### Windows PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# List log groups
aws logs describe-log-groups --endpoint-url http://localhost:4566

# Get logs
aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --endpoint-url http://localhost:4566
```

### Mac/Linux Bash:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# List log groups
aws logs describe-log-groups --endpoint-url http://localhost:4566

# Get logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow \
  --endpoint-url http://localhost:4566
```

---

## Step 9: Access LocalStack UI

Open your browser and visit:

```
http://localhost:8080
```

**Features:**

- View Lambda functions
- Inspect CloudWatch logs
- Monitor Secrets Manager
- Check IAM roles
- View API Gateway

---

## Troubleshooting

### Issue: "Docker is not running"

**Solution:**

```powershell
# Windows: Start Docker Desktop
# Verify Docker is running
docker ps

# If still not working, restart Docker Desktop
```

### Issue: "Port 4566 already in use"

**Solution:**

```powershell
# Windows: Find process using port 4566
netstat -ano | findstr :4566

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F

# Or use different port in docker-compose.yml
# Change: "4566:4566" to "4567:4566"
```

### Issue: "LocalStack not ready"

**Solution:**

```powershell
# Wait longer for initialization
Start-Sleep -Seconds 60

# Check logs
docker logs localstack-lambda-test

# Restart if needed
docker-compose restart
```

### Issue: "Secret not found"

**Solution:**

```powershell
# Verify secret exists
aws secretsmanager list-secrets --endpoint-url http://localhost:4566

# If missing, manually create it
aws secretsmanager create-secret `
  --name external-api/token `
  --secret-string '{"client_id":"test-client-id","client_secret":"test-client-secret"}' `
  --endpoint-url http://localhost:4566
```

### Issue: "Lambda function creation failed"

**Solution:**

```powershell
# Verify IAM role exists
aws iam get-role `
  --role-name lambda-execution-role `
  --endpoint-url http://localhost:4566

# If missing, create it manually
aws iam create-role `
  --role-name lambda-execution-role `
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}' `
  --endpoint-url http://localhost:4566
```

---

## Common Commands Reference

### AWS CLI Configuration for LocalStack

```powershell
# Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Or configure AWS CLI profile
aws configure --profile localstack
# Enter: test (access key)
# Enter: test (secret key)
# Enter: us-east-1 (region)
# Enter: json (output format)

# Use profile
aws lambda list-functions --profile localstack --endpoint-url http://localhost:4566
```

### Secrets Manager Commands

```powershell
# Create secret
aws secretsmanager create-secret `
  --name my-secret `
  --secret-string '{"key":"value"}' `
  --endpoint-url http://localhost:4566

# Get secret
aws secretsmanager get-secret-value `
  --secret-id my-secret `
  --endpoint-url http://localhost:4566

# Update secret
aws secretsmanager update-secret `
  --secret-id my-secret `
  --secret-string '{"key":"new-value"}' `
  --endpoint-url http://localhost:4566

# Delete secret
aws secretsmanager delete-secret `
  --secret-id my-secret `
  --endpoint-url http://localhost:4566
```

### Lambda Commands

```powershell
# List functions
aws lambda list-functions --endpoint-url http://localhost:4566

# Get function details
aws lambda get-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566

# Invoke function
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

# Update function code
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

# Delete function
aws lambda delete-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566
```

### CloudWatch Logs Commands

```powershell
# List log groups
aws logs describe-log-groups --endpoint-url http://localhost:4566

# Get log streams
aws logs describe-log-streams `
  --log-group-name /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566

# Get log events
aws logs get-log-events `
  --log-group-name /aws/lambda/my-token-auth-lambda `
  --log-stream-name 1 `
  --endpoint-url http://localhost:4566

# Tail logs (follow mode)
aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --endpoint-url http://localhost:4566
```

---

## Stop LocalStack When Done

### Windows PowerShell:

```powershell
cd "E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject"

# Stop containers
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View remaining containers
docker ps
```

### Mac/Linux Bash:

```bash
cd ~/path/to/SetUpProject

# Stop containers
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View remaining containers
docker ps
```

---

## Complete Quick Setup Script

### Windows PowerShell (Save as `localstack-install.ps1`):

```powershell
# LocalStack Installation Script for Windows

Write-Host "LocalStack Installation & Setup" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green
Write-Host ""

# Navigate to project
cd "E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject"

# Set AWS credentials
Write-Host "[1/6] Setting AWS credentials..." -ForegroundColor Cyan
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Start Docker Compose
Write-Host "[2/6] Starting Docker containers..." -ForegroundColor Cyan
docker-compose up -d
Write-Host "Waiting 30 seconds for initialization..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Verify containers
Write-Host "[3/6] Verifying containers..." -ForegroundColor Cyan
docker ps

# Build Maven
Write-Host "[4/6] Building Lambda JAR..." -ForegroundColor Cyan
mvn clean install -q

# Create Lambda function
Write-Host "[5/6] Creating Lambda function..." -ForegroundColor Cyan
aws lambda create-function `
  --function-name my-token-auth-lambda `
  --runtime java21 `
  --role arn:aws:iam::000000000000:role/lambda-execution-role `
  --handler org.example.Main::handleRequest `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --environment Variables="{
    EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
    TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
    CLIENT_ID=test-client-id,
    CLIENT_SECRET=test-client-secret,
    TOKEN_SECRET_NAME=external-api/token
  }" `
  --timeout 60 `
  --memory-size 512 `
  --endpoint-url http://localhost:4566 2>$null

# Test Lambda
Write-Host "[6/6] Testing Lambda function..." -ForegroundColor Cyan
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json | Out-Null

Get-Content response.json

Write-Host ""
Write-Host "✅ Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. View logs: aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566"
Write-Host "  2. LocalStack UI: http://localhost:8080"
Write-Host "  3. Stop: docker-compose down"
Write-Host ""
```

Run it:

```powershell
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
.\localstack-install.ps1
```

---

## Health Check

Verify everything is working:

```powershell
Write-Host "Health Check" -ForegroundColor Green
Write-Host "=============" -ForegroundColor Green
Write-Host ""

$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# 1. Docker Containers
Write-Host "1. Docker Containers:" -ForegroundColor Cyan
docker ps -a --format "table {{.Names}}`t{{.Status}}"
Write-Host ""

# 2. LocalStack Endpoint
Write-Host "2. LocalStack Endpoint:" -ForegroundColor Cyan
$response = Invoke-WebRequest http://localhost:4566 -ErrorAction SilentlyContinue
if ($response.StatusCode -eq 200) {
    Write-Host "✅ LocalStack is running" -ForegroundColor Green
} else {
    Write-Host "❌ LocalStack is not responding" -ForegroundColor Red
}
Write-Host ""

# 3. Secrets Manager
Write-Host "3. Secrets Manager:" -ForegroundColor Cyan
$secrets = aws secretsmanager list-secrets --endpoint-url http://localhost:4566 --query 'SecretList[*].Name' --output text
if ($secrets) {
    Write-Host "✅ Secrets: $secrets" -ForegroundColor Green
} else {
    Write-Host "❌ No secrets found" -ForegroundColor Red
}
Write-Host ""

# 4. Lambda Functions
Write-Host "4. Lambda Functions:" -ForegroundColor Cyan
$functions = aws lambda list-functions --endpoint-url http://localhost:4566 --query 'Functions[*].FunctionName' --output text
if ($functions) {
    Write-Host "✅ Functions: $functions" -ForegroundColor Green
} else {
    Write-Host "ℹ️ No functions found" -ForegroundColor Yellow
}
Write-Host ""

# 5. LocalStack UI
Write-Host "5. LocalStack UI:" -ForegroundColor Cyan
Write-Host "   Open: http://localhost:8080" -ForegroundColor Blue
Write-Host ""
```

---

## Next Steps

Once LocalStack is running:

1. **Update Secrets (if needed)**
   ```powershell
   aws secretsmanager update-secret `
     --secret-id external-api/token `
     --secret-string '{"client_id":"your-id","client_secret":"your-secret"}' `
     --endpoint-url http://localhost:4566
   ```

2. **Test Your Lambda**
   ```powershell
   aws lambda invoke `
     --function-name my-token-auth-lambda `
     --payload '{"test":"data"}' `
     --endpoint-url http://localhost:4566 `
     response.json
   ```

3. **Monitor Logs**
   ```powershell
   aws logs tail /aws/lambda/my-token-auth-lambda `
     --follow `
     --endpoint-url http://localhost:4566
   ```

4. **Use LocalStack UI**
    - Visit http://localhost:8080
    - Monitor all AWS services
    - View logs in real-time

---

**Status:** ✅ Ready for Local Testing
**Date:** December 27, 2025

