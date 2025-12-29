# QUICK REFERENCE - BUILD & TEST

## 🏗️ BUILD (Command Prompt - cmd.exe)

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

✅ Expected: `BUILD SUCCESS`

---

## 🚀 DEPLOY (PowerShell)

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1"

aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar --endpoint-url http://localhost:4566

Start-Sleep -Seconds 5
```

✅ Expected: Function updated message

---

## 🧪 TEST (PowerShell)

```powershell
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json

Get-Content response.json
```

✅ Expected: JSON response with statusCode

---

## 📊 MONITOR LOGS (PowerShell)

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
```

✅ Expected: Real-time log messages

---

## ✅ SUCCESS INDICATORS

- Build shows: `[INFO] BUILD SUCCESS`
- Response shows: `statusCode: 200`
- Logs show: `Access token` and `API call` messages
- No errors in logs: All `INFO` and `DEBUG` levels

---

## 🔄 COMPLETE WORKFLOW (Copy & Paste All)

**Command Prompt:**

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject && mvn clean install -DskipTests
```

**PowerShell:**

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1";aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar --endpoint-url http://localhost:4566;Start-Sleep -Seconds 5;aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json;Write-Host "Response:";Get-Content response.json;Write-Host "";Write-Host "Recent logs:";aws logs tail /aws/lambda/my-token-auth-lambda --endpoint-url http://localhost:4566
```

---

## 🎯 Status

✅ Code: Production Ready
✅ Build: Ready
✅ Deploy: Ready
✅ Test: Ready

**Go build it! 🚀**

