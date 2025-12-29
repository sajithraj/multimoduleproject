# 🚀 NEXT STEPS - Using Hardcoded Configuration

## ✅ What's Done

Your `AppConfig.java` now uses **hardcoded test values** instead of environment variables. You can edit these values
directly in the file for testing.

---

## 🎯 IMMEDIATE ACTION PLAN

### Step 1: Update Secrets Manager (LocalStack)

Update the secret with your actual credentials:

```powershell
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

aws secretsmanager update-secret `
  --secret-id external-api/token `
  --secret-string '{"client_id":"YOUR_ACTUAL_CLIENT_ID","client_secret":"YOUR_ACTUAL_CLIENT_SECRET"}' `
  --endpoint-url http://localhost:4566
```

Replace:

- `YOUR_ACTUAL_CLIENT_ID` with your real client ID
- `YOUR_ACTUAL_CLIENT_SECRET` with your real client secret

### Step 2: Verify Secret Updated

```powershell
aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

Should show your new credentials.

### Step 3: Rebuild JAR

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

Expected: `BUILD SUCCESS`

### Step 4: Deploy

```powershell
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

Start-Sleep -Seconds 5
```

### Step 5: Test

```powershell
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

Get-Content response.json
```

### Step 6: Monitor Logs

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --endpoint-url http://localhost:4566
```

---

## 📝 Modify Configuration Values

If you need to change any configuration values:

### 1. Edit AppConfig.java

```java
// Line 24-34 in AppConfig.java
public static final String EXTERNAL_API_URL = "https://your-api.com";
public static final String TOKEN_ENDPOINT_URL = "https://your-api.com/v1/authorize/token";
public static final String TOKEN_SECRET_NAME = "your-secret-name";
```

### 2. Rebuild & Deploy

```bash
mvn clean install -DskipTests
```

```powershell
aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar --endpoint-url http://localhost:4566
```

### 3. Test Again

```powershell
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json
Get-Content response.json
```

---

## 🔑 Key Configuration Values to Know

**From AppConfig.java** (you control these):

- `EXTERNAL_API_URL` - Your API endpoint
- `TOKEN_ENDPOINT_URL` - Your token authorization endpoint
- `TOKEN_SECRET_NAME` - Name of the secret in Secrets Manager

**From Secrets Manager** (you update via CLI):

- `client_id` - OAuth2 client ID
- `client_secret` - OAuth2 client secret

---

## ✨ Troubleshooting

### Error: "The security token included in the request is invalid"

This means the credentials in Secrets Manager are wrong. Fix:

```powershell
aws secretsmanager update-secret `
  --secret-id external-api/token `
  --secret-string '{"client_id":"CORRECT_ID","client_secret":"CORRECT_SECRET"}' `
  --endpoint-url http://localhost:4566
```

### Error: "No secret found"

The secret name in Secrets Manager doesn't match `TOKEN_SECRET_NAME`.

**Check**:

```powershell
aws secretsmanager list-secrets --endpoint-url http://localhost:4566
```

**Fix**: Update `TOKEN_SECRET_NAME` in AppConfig.java to match.

### Lambda not updating

Make sure to wait 5 seconds after update:

```powershell
Start-Sleep -Seconds 5
```

---

## 📊 Configuration Checklist

- [ ] Updated Secrets Manager with real credentials
- [ ] Verified secret with `get-secret-value`
- [ ] Rebuilt JAR with `mvn clean install`
- [ ] Deployed with `aws lambda update-function-code`
- [ ] Waited 5 seconds
- [ ] Tested with `aws lambda invoke`
- [ ] Checked logs with `aws logs tail`

---

## 🎯 Summary

| Step          | Command                                | Status          |
|---------------|----------------------------------------|-----------------|
| Update Secret | `aws secretsmanager update-secret ...` | ⏳ Do this next  |
| Rebuild       | `mvn clean install`                    | ⏳ After secrets |
| Deploy        | `aws lambda update-function-code ...`  | ⏳ After rebuild |
| Test          | `aws lambda invoke ...`                | ⏳ After deploy  |
| Monitor       | `aws logs tail ...`                    | ⏳ After test    |

---

**Ready to test?** Follow the steps above! 🚀

