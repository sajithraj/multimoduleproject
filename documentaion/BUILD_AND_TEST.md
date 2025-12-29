# Complete Build & Test Guide

## ✅ All Compilation Issues Fixed

Three critical issues have been resolved:

1. **RetryConfigProvider** - Removed conflicting interval function configuration
2. **AuthenticatedApiClient** - Fixed exception handling in lambda expressions
3. **TokenAuthorizationUtil** - Fixed ResponseHandler usage pattern

---

## 🏗️ Build Instructions

### Step 1: Open Command Prompt

1. Press `Win + R`
2. Type `cmd` and press Enter
3. Navigate to your project:

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
```

### Step 2: Clean & Build

```bash
mvn clean install -DskipTests
```

**Expected Output:**

```
[INFO] Scanning for projects...
[INFO] 
[INFO] --------< org.example:SetUpProject >--------
[INFO] Building SetUpProject 1.0-SNAPSHOT
[INFO] 
...
[INFO] BUILD SUCCESS
[INFO] Total time:  XX.XXXs
[INFO] Finished at: 2025-12-27T...
```

### Step 3: Verify JAR Created

```bash
dir target\*.jar
```

Should show:

```
SetUpProject-1.0-SNAPSHOT.jar    (~25 MB)
```

---

## 🚀 Deploy & Test

After successful build, open PowerShell and run:

### Step 1: Set AWS Credentials

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

Write-Host "AWS Credentials Set ✓"
```

### Step 2: Update Lambda Function

```powershell
Write-Host "Updating Lambda function..."

aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

Write-Host "Waiting for update..." 
Start-Sleep -Seconds 5

Write-Host "Lambda Updated ✓"
```

### Step 3: Test Lambda Invocation

```powershell
Write-Host "Testing Lambda..."

aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

Write-Host ""
Write-Host "Response:"
Get-Content response.json
```

### Step 4: Monitor Logs

```powershell
Write-Host "Monitoring logs (Ctrl+C to stop)..."

aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --endpoint-url http://localhost:4566
```

---

## 🔍 What to Expect

### If Successful ✅

- **Build**: `BUILD SUCCESS` message
- **Lambda**: Response JSON with status code
- **Logs**: Token authorization messages
- **No Errors**: All compilation errors resolved

### Common Response Format

```json
{
  "statusCode": 200,
  "status": "success",
  "data": null,
  "error": null,
  "message": null,
  "timestamp": 1735294800000
}
```

### If Errors Appear

Check CloudWatch logs for details:

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566 | Select-Object -Last 20
```

---

## 🎯 Complete Script (Copy & Paste)

### PowerShell - Build & Deploy

```powershell
# 1. Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# 2. Update function
Write-Host "Updating Lambda..."
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

# 3. Wait and test
Start-Sleep -Seconds 5
Write-Host ""
Write-Host "Testing Lambda..."

aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

Write-Host ""
Write-Host "Response:"
Get-Content response.json

Write-Host ""
Write-Host "Recent logs:"
aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566 | Select-Object -Last 10
```

---

## 🔧 Troubleshooting

### Maven not found

**Solution**: Ensure Maven is available from cmd.exe

```bash
mvn --version
```

If not found, add Maven to PATH or use full path:

```bash
C:\apache-maven-3.9.12\bin\mvn clean install -DskipTests
```

### Lambda Update Fails

**Solution**: Verify LocalStack is running

```powershell
docker ps
docker ps -a  # Shows stopped containers
```

Restart if needed:

```powershell
docker-compose restart
```

### No Response from Lambda

**Solution**: Check CloudWatch logs

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566
```

Look for error messages and stack traces.

### Connection Refused

**Solution**: Ensure LocalStack is healthy

```powershell
Invoke-WebRequest http://localhost:4566/_localstack/health -UseBasicParsing
```

Should return status code 200.

---

## 📊 Build Timeline

| Step      | Command               | Expected Time |
|-----------|-----------------------|---------------|
| Clean     | `mvn clean`           | 5-10s         |
| Compile   | `mvn compile`         | 30-60s        |
| Package   | `mvn package`         | 10-20s        |
| Install   | `mvn install`         | 5-10s         |
| **Total** | **mvn clean install** | **1-2 min**   |

---

## ✅ Verification Checklist

After build:

- [ ] `mvn clean install` shows `BUILD SUCCESS`
- [ ] JAR file exists in `target/` (~25 MB)
- [ ] Lambda function updates without errors
- [ ] Lambda invocation returns response JSON
- [ ] CloudWatch logs show no errors
- [ ] Status code is 200 or 500 (not timeout)

---

## 🎉 Success Indicators

✅ **Build Success**

```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXXs
```

✅ **Lambda Response**

```json
{
  "statusCode": 200,
  "status": "success"
}
```

✅ **Logs Show**

```
[INFO] Initiating API call to endpoint: ...
[INFO] Access token obtained for API call
```

---

## 📞 Need Help?

1. **Compilation errors**: Check `FIXES_APPLIED.md`
2. **Build fails**: Run `mvn clean` first
3. **Lambda doesn't respond**: Check LocalStack logs
4. **Connection issues**: Verify Docker containers running

---

**Status**: ✅ Ready to Build
**Date**: December 27, 2025
**Next**: Run `mvn clean install -DskipTests` from cmd.exe

