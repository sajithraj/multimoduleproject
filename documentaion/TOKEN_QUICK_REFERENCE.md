# 🔐 Token Flow - Quick Reference

## Where Token is Called

### Main Flow:

```
ApiHandler.handleRequest()  (Line 33 in ApiHandler.java)
    ↓
    getClient().callExternalApi()  (Line 46 in ApiHandler.java)
    ↓
    ExternalApiClient.callExternalApi()  (Line 47 in ExternalApiClient.java)
    ↓
    TokenCache.getToken()  (Line 54 in ExternalApiClient.java) ← TOKEN CALLED HERE
```

---

## Where Token is Retrieved

**File**: `src/main/java/com/project/auth/TokenCache.java`  
**Method**: `fetchToken()`  
**Lines**: 65-77

```java
private static CachedToken fetchToken() {
    try {
        String secretValue = getSecretsProvider().get(AppConfig.TOKEN_SECRET_NAME);  // ← FETCHES FROM SECRETS MANAGER
        JsonNode json = MAPPER.readTree(secretValue);
        String token = json.get("token").asText();  // ← TOKEN EXTRACTED

        // 🔐 TOKEN PRINTED HERE (3 logs):
        LOG.info("🔐 Token retrieved from Secrets Manager: {}", token);
        LOG.debug("Token length: {} characters", token.length());
        LOG.debug("Token starts with: {}", token.substring(0, Math.min(10, token.length())) + "...");

        Instant expiryTime = Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS);
        LOG.debug("Token cached until: {}", expiryTime);

        return new CachedToken(token, expiryTime);
    }
}
```

---

## Where Token is Used

**File**: `src/main/java/com/project/client/ExternalApiClient.java`  
**Method**: `callExternalApi()`  
**Lines**: 51-59

```java
try{
request =new

HttpGet(AppConfig.EXTERNAL_API_URL);

String token = TokenCache.getToken();  // ← GET TOKEN

// 🔐 TOKEN PRINTED HERE (2 logs):
    LOG.

info("🔐 Using token in request: {}",token.substring(0, Math.min(20, token.length()))+"...");
        LOG.

debug("Full token: {}",token);  // ← FULL TOKEN LOGGED
    
    request.

setHeader("Authorization","Bearer "+token);  // ← TOKEN USED HERE
    request.

setHeader("Content-Type","application/json");
```

---

## 📝 Log Output You'll See

When Lambda executes, check CloudWatch logs for these messages:

### Token Retrieval (TokenCache.java):

```
[INFO] 🔐 Token retrieved from Secrets Manager: eyJhbGciOiJIUzI1NiIsInR5cCI...
[DEBUG] Token length: 97 characters
[DEBUG] Token starts with: eyJhbGciOiJI...
[DEBUG] Token cached until: 2025-12-27T17:55:00Z
```

### Token Usage (ExternalApiClient.java):

```
[INFO] 🔐 Using token in request: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
[DEBUG] Full token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI...
```

---

## 🔑 How to Find Token in Code

### Search for "🔐" in logs:

```bash
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566 | grep "🔐"
```

### Or search for "Token retrieved":

```bash
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566 | grep "retrieved"
```

### Or search for "Full token":

```bash
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566 | grep "Full token"
```

---

## 🎯 Token Call Chain Summary

| Step | Location                     | Action                                        |
|------|------------------------------|-----------------------------------------------|
| 1    | ApiHandler.java:46           | `callExternalApi()` invoked                   |
| 2    | ExternalApiClient.java:54    | `TokenCache.getToken()` called                |
| 3    | TokenCache.java:31           | Check if cached and not expired               |
| 4    | TokenCache.java:65           | If expired: `fetchToken()` called             |
| 5    | TokenCache.java:66           | `SecretsProvider.get()` calls Secrets Manager |
| 6    | TokenCache.java:70-72        | 🔐 TOKEN PRINTED (3 log statements)           |
| 7    | ExternalApiClient.java:54    | Token returned and stored                     |
| 8    | ExternalApiClient.java:55-56 | 🔐 TOKEN PRINTED (2 log statements)           |
| 9    | ExternalApiClient.java:58    | Token added to Authorization header           |
| 10   | ExternalApiClient.java:60    | HTTP request executed                         |

---

## 🚀 To See Token in Action

### Build & Deploy:

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
cd infra/terraform
terraform init
terraform apply -var-file=terraform.localstack.tfvars
aws lambda update-function-code --function-name my-token-auth-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar --endpoint-url http://localhost:4566
Start-Sleep -Seconds 5
```

### Invoke & Watch Logs:

```bash
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

# In one PowerShell window (logs):
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566

# In another PowerShell window (invoke):
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json
```

---

## ✅ Changes Made

### TokenCache.java (Lines 69-72)

Added 3 log statements to print token when retrieved from Secrets Manager:

```java
LOG.info("🔐 Token retrieved from Secrets Manager: {}",token);
LOG.

debug("Token length: {} characters",token.length());
        LOG.

debug("Token starts with: {}",token.substring(0, Math.min(10, token.length()))+"...");
```

### ExternalApiClient.java (Lines 55-56)

Added 2 log statements to print token when used in API request:

```java
LOG.info("🔐 Using token in request: {}",token.substring(0, Math.min(20, token.length()))+"...");
        LOG.

debug("Full token: {}",token);
```

---

**Status**: ✅ Token flow documented and logging added
**Ready**: Build and deploy to see token in logs

