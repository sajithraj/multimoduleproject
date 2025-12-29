# QUICK REFERENCE - Hardcoded Configuration

## 🎯 What Changed

AppConfig.java now has hardcoded values you can easily edit:

```java
public static final String EXTERNAL_API_URL = "https://exchange-staging.motiveintegrator.com";
public static final String TOKEN_ENDPOINT_URL = "https://exchange-staging.motiveintegrator.com/v1/authorize/token";
public static final String TOKEN_SECRET_NAME = "external-api/token";
public static final String CLIENT_ID = "test-client-id";
public static final String CLIENT_SECRET = "test-client-secret";
```

---

## 🚀 COMPLETE WORKFLOW - Copy & Paste

### Step 1: Update Secrets Manager

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1"

aws secretsmanager update-secret --secret-id external-api/token --secret-string '{"client_id":"YOUR_REAL_ID","client_secret":"YOUR_REAL_SECRET"}' --endpoint-url http://localhost:4566
```

### Step 2: Rebuild

```bash
mvn clean install -DskipTests
```

### Step 3: Deploy

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1"

aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar --endpoint-url http://localhost:4566

Start-Sleep -Seconds 5
```

### Step 4: Test

```powershell
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json

Get-Content response.json
```

### Step 5: Monitor

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
```

---

## 📝 Edit Configuration

Open `AppConfig.java` and modify these lines:

```java
// Line 24-34
public static final String EXTERNAL_API_URL = "YOUR_API_URL";
public static final String TOKEN_ENDPOINT_URL = "YOUR_TOKEN_ENDPOINT";
public static final String TOKEN_SECRET_NAME = "your-secret-name";
public static final String CLIENT_ID = "your-client-id";
public static final String CLIENT_SECRET = "your-client-secret";
```

---

## ✅ Expected Response

After successful test:

```json
{
  "statusCode": 200,
  "status": "success",
  "data": null,
  "error": null
}
```

---

## ⚠️ If You Get Auth Error

"The security token included in the request is invalid"

→ Your Secrets Manager credentials are wrong

→ Update with correct ones:

```powershell
aws secretsmanager update-secret --secret-id external-api/token --secret-string '{"client_id":"CORRECT_ID","client_secret":"CORRECT_SECRET"}' --endpoint-url http://localhost:4566
```

---

**Ready? Follow the workflow above! 🚀**

