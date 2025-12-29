# Configuration Guide - Hardcoded Values for Testing

## 📝 Current Configuration

Your `AppConfig.java` now has **hardcoded test values** that you can easily modify:

```java
// External API Configuration
public static final String EXTERNAL_API_URL = "https://exchange-staging.motiveintegrator.com";
public static final String TOKEN_ENDPOINT_URL = "https://exchange-staging.motiveintegrator.com/v1/authorize/token";

// Secrets Manager Configuration
public static final String TOKEN_SECRET_NAME = "external-api/token";

// Credentials
public static final String CLIENT_ID = "test-client-id";
public static final String CLIENT_SECRET = "test-client-secret";
```

---

## 🔧 How to Use

### 1. Edit Configuration Values

Open `AppConfig.java` and modify the values:

```java
// Example: Change the API URL
public static final String EXTERNAL_API_URL = "https://your-api.com";

// Example: Change secret name
public static final String TOKEN_SECRET_NAME = "your-secret-name";
```

### 2. Rebuild

```bash
mvn clean install -DskipTests
```

### 3. Deploy & Test

```powershell
aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar --endpoint-url http://localhost:4566
```

---

## 🔐 Important: Secrets Manager Integration

The configuration loads values from `AppConfig.java`, but **Secrets Manager integration** still fetches credentials
from:

- **Secret Name**: `external-api/token`
- **Secret Content**: `{"client_id": "...", "client_secret": "..."}`

### Update Secrets in LocalStack

```powershell
$env:AWS_ACCESS_KEY_ID="test";$env:AWS_SECRET_ACCESS_KEY="test";$env:AWS_DEFAULT_REGION="us-east-1"

aws secretsmanager update-secret `
  --secret-id external-api/token `
  --secret-string '{"client_id":"your-client-id","client_secret":"your-client-secret"}' `
  --endpoint-url http://localhost:4566
```

---

## 📋 Configuration Values Explained

| Config             | Purpose                         | Default Value                                                    | Where to Change                  |
|--------------------|---------------------------------|------------------------------------------------------------------|----------------------------------|
| EXTERNAL_API_URL   | Base URL for API calls          | https://exchange-staging.motiveintegrator.com                    | AppConfig.java                   |
| TOKEN_ENDPOINT_URL | Token authorization endpoint    | https://exchange-staging.motiveintegrator.com/v1/authorize/token | AppConfig.java                   |
| TOKEN_SECRET_NAME  | Secrets Manager secret name     | external-api/token                                               | AppConfig.java                   |
| CLIENT_ID          | OAuth2 client ID (fallback)     | test-client-id                                                   | AppConfig.java + Secrets Manager |
| CLIENT_SECRET      | OAuth2 client secret (fallback) | test-client-secret                                               | AppConfig.java + Secrets Manager |

---

## 🔄 Migration Path: From Hardcoded to Environment Variables

When ready to move to environment variables:

### Step 1: Update AppConfig.java

```java
static {
    // Load from environment variables
    EXTERNAL_API_URL = getRequiredEnv("EXTERNAL_API_URL");
    TOKEN_SECRET_NAME = getRequiredEnv("TOKEN_SECRET_NAME");
    TOKEN_ENDPOINT_URL = getRequiredEnv("TOKEN_ENDPOINT_URL");
    CLIENT_ID = getRequiredEnv("CLIENT_ID");
    CLIENT_SECRET = getRequiredEnv("CLIENT_SECRET");

    LOG.info("Configuration loaded from environment variables");
}
```

### Step 2: Set Environment Variables in Lambda

In AWS Lambda console or via AWS CLI:

```bash
aws lambda update-function-configuration \
  --function-name my-token-auth-lambda \
  --environment Variables="{
    EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
    TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
    TOKEN_SECRET_NAME=external-api/token,
    CLIENT_ID=your-client-id,
    CLIENT_SECRET=your-client-secret
  }"
```

### Step 3: Rebuild & Deploy

```bash
mvn clean install -DskipTests
aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar
```

---

## 🧪 Testing with Different Configurations

### Test 1: Change API URL

```java
// In AppConfig.java
public static final String EXTERNAL_API_URL = "http://localhost:3000"; // Local mock server
```

### Test 2: Change Secret Name

```java
// In AppConfig.java
public static final String TOKEN_SECRET_NAME = "my-custom-secret";
```

Then update LocalStack:

```powershell
aws secretsmanager update-secret --secret-id my-custom-secret --secret-string '...' --endpoint-url http://localhost:4566
```

---

## 📌 Best Practices

✅ **For Development**: Use hardcoded values in AppConfig.java (easier to iterate)
✅ **For Staging**: Use environment variables set in Lambda function
✅ **For Production**: Use AWS Secrets Manager for all credentials

---

## 🚀 Quick Test Workflow

1. **Edit AppConfig.java** with your test values
2. **Rebuild**: `mvn clean install -DskipTests`
3. **Deploy**: `aws lambda update-function-code ...`
4. **Test**: `aws lambda invoke ...`
5. **Monitor**: `aws logs tail ... --follow`

---

## 💾 Current Values in Your Configuration

```
External API URL:     https://exchange-staging.motiveintegrator.com
Token Endpoint:       https://exchange-staging.motiveintegrator.com/v1/authorize/token
Token Secret Name:    external-api/token
Client ID:            test-client-id (fallback, actual from Secrets Manager)
Client Secret:        test-client-secret (fallback, actual from Secrets Manager)
```

---

## ⚠️ Important Notes

1. **Secrets Manager takes precedence**: Even though CLIENT_ID and CLIENT_SECRET are in AppConfig, the actual values
   come from Secrets Manager
2. **Update both places**: When testing, update the secret in Secrets Manager
3. **Logging**: Check logs to see which configuration is being used

---

## 🎯 Next Steps

1. ✅ Edit `AppConfig.java` with your values
2. ✅ Update Secrets Manager with credentials:
   ```powershell
   aws secretsmanager update-secret --secret-id external-api/token --secret-string '{"client_id":"YOUR_ID","client_secret":"YOUR_SECRET"}' --endpoint-url http://localhost:4566
   ```
3. ✅ Rebuild: `mvn clean install -DskipTests`
4. ✅ Deploy: `aws lambda update-function-code ...`
5. ✅ Test: `aws lambda invoke ...`

---

**Status**: ✅ Ready for testing with hardcoded configuration
**Next**: Update Secrets Manager with real credentials

