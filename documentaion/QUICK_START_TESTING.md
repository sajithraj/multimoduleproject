# 🚀 Quick Start - Lambda Testing Commands

## ⚠️ IMPORTANT: LocalStack Endpoint Required

**The error you're getting:**

```
An error occurred (UnrecognizedClientException) when calling the Invoke operation: 
The security token included in the request is invalid.
```

**Root Cause:** You're missing `--endpoint-url=http://localhost:4566`

---

## ✅ CORRECT Commands for LocalStack

### Setup Environment Variables:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### Test 1: First Call (Cache Miss)

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  r1.json

# View response
Get-Content r1.json | ConvertFrom-Json | ConvertTo-Json
```

### Test 2: Second Call (Cache Hit)

```powershell
# Wait a moment
Start-Sleep -Seconds 2

aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  r2.json

# View response
Get-Content r2.json | ConvertFrom-Json | ConvertTo-Json
```

### Test 3: View Logs (Check for Cache)

```powershell
aws --endpoint-url=http://localhost:4566 logs tail `
  /aws/lambda/my-token-auth-lambda `
  --since 5m `
  --format short | Select-String -Pattern "CACHE"
```

---

## 📊 What to Expect

### First Call (Cache Miss):

```
Logs will show:
✅ "No cached token found, fetching fresh token from OAuth2 endpoint"
✅ "Fresh OAuth2 token fetched and CACHED"
⏱️ Duration: ~4000-5000 ms (cold start)
```

### Second Call (Cache Hit):

```
Logs will show:
✅ "OAuth2 bearer token retrieved from CACHE (age: X seconds, remaining TTL: Y seconds)"
⏱️ Duration: ~100-500 ms (96% faster!)
```

---

## 🔧 Complete Test Script

```powershell
# Set environment
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

Write-Host "`n=== Test 1: Cache Miss (First Call) ===" -ForegroundColor Cyan
$start1 = Get-Date
aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  r1.json 2>&1 | Out-Null
$duration1 = ((Get-Date) - $start1).TotalMilliseconds
Write-Host "Duration: $duration1 ms" -ForegroundColor Yellow
Get-Content r1.json | ConvertFrom-Json | Select-Object statusCode | Format-Table

Start-Sleep -Seconds 2

Write-Host "`n=== Test 2: Cache Hit (Second Call) ===" -ForegroundColor Cyan
$start2 = Get-Date
aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  r2.json 2>&1 | Out-Null
$duration2 = ((Get-Date) - $start2).TotalMilliseconds
Write-Host "Duration: $duration2 ms" -ForegroundColor Yellow
Get-Content r2.json | ConvertFrom-Json | Select-Object statusCode | Format-Table

Write-Host "`n=== Performance Comparison ===" -ForegroundColor Green
$improvement = [math]::Round((($duration1 - $duration2) / $duration1) * 100, 2)
Write-Host "First call:  $duration1 ms"
Write-Host "Second call: $duration2 ms"
Write-Host "Improvement: $improvement% faster!" -ForegroundColor Green

Write-Host "`n=== Checking Logs for Cache Evidence ===" -ForegroundColor Cyan
aws --endpoint-url=http://localhost:4566 logs tail `
  /aws/lambda/my-token-auth-lambda `
  --since 5m `
  --format short | Select-String -Pattern "CACHE|cached" | Select-Object -First 5
```

---

## ❌ Common Mistakes

### Mistake 1: Missing Endpoint URL

```powershell
# ❌ WRONG - Tries to call real AWS
aws lambda invoke --function-name my-token-auth-lambda ...

# ✅ CORRECT - Calls LocalStack
aws --endpoint-url=http://localhost:4566 lambda invoke --function-name my-token-auth-lambda ...
```

### Mistake 2: Wrong Payload Format

```powershell
# ❌ WRONG - PowerShell escaping issues
--payload '{"body":"{}"}'

# ✅ CORRECT - Escape quotes properly
--payload '{\"body\":\"{}\"}'
```

### Mistake 3: Not Setting Environment Variables

```powershell
# Must set these EVERY PowerShell session:
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

---

## 🎯 Expected Results

### Call 1 (Cache Miss):

```json
{
  "statusCode": 200,
  "body": "[{\"documentId\":\"DO-73859\",...}]"
}
```

**Duration:** ~4000-5000 ms

### Call 2 (Cache Hit):

```json
{
  "statusCode": 200,
  "body": "[{\"documentId\":\"DO-73859\",...}]"
}
```

**Duration:** ~100-500 ms (96% faster!)

### Logs (Cache Evidence):

```
INFO SSMApigeeProvider initialized with token caching enabled (TTL: 3300 seconds / 55 minutes)
INFO No cached token found, fetching fresh token from OAuth2 endpoint
INFO Fresh OAuth2 token fetched and CACHED - Secrets: 173 ms, Token API: 1500 ms, Total: 1673 ms

[Second Call]
INFO OAuth2 bearer token retrieved from CACHE (age: 30 seconds, remaining TTL: 3270 seconds)
```

---

## 🔍 Troubleshooting

### Issue: "UnrecognizedClientException"

**Solution:** Add `--endpoint-url=http://localhost:4566`

### Issue: "ResourceNotFoundException"

**Solution:** Lambda not deployed. Run:

```powershell
cd infra\terraform
terraform apply -var-file="terraform.localstack.tfvars" -auto-approve
```

### Issue: "Connection refused"

**Solution:** LocalStack not running. Start it first.

### Issue: Cache not working

**Solution:** Check logs:

```powershell
aws --endpoint-url=http://localhost:4566 logs tail `
  /aws/lambda/my-token-auth-lambda --since 10m
```

---

## 📚 More Information

See `CACHING_COMPLETE_GUIDE.md` for:

- How caching works in detail
- Lambda container lifecycle
- Multi-tenant safety
- Performance expectations
- Architecture diagrams

---

**Bottom Line:** Always include `--endpoint-url=http://localhost:4566` when testing with LocalStack! 🎯

