# 🔐 Token Flow in Your Code - Complete Guide

## Token Call Flow Overview

Here's exactly where and how the token is called in your Lambda:

```
APIHandler.handleRequest()
    ↓
    └─→ getClient().callExternalApi()
        ↓
        └─→ ExternalApiClient.callExternalApi()
            ↓
            └─→ TokenCache.getToken()  ← TOKEN IS RETRIEVED HERE
                ↓
                ├─→ Check if cached (if not expired)
                ├─→ If expired: fetchToken()
                │   └─→ SecretsProvider.get(AppConfig.TOKEN_SECRET_NAME)  ← FETCHES FROM SECRETS MANAGER
                │       ↓
                │       └─→ Parses JSON and extracts "token" field
                │           ↓
                │           └─→ 🔐 TOKEN PRINTED TO LOGS HERE
                ↓
            └─→ Token passed to ExternalApiClient as "Bearer {token}"
                ↓
                └─→ 🔐 TOKEN PRINTED WHEN USED HERE
                    ↓
                    └─→ Added to HTTP Authorization header
```

---

## File-by-File Token Usage

### 1. 🎯 TokenCache.java - TOKEN RETRIEVAL

**Location**: `src/main/java/com/project/auth/TokenCache.java`

This is where the token is fetched and cached:

```java
public static String getToken() {
    CachedToken cached = CACHE.get(AppConfig.TOKEN_SECRET_NAME);

    if (cached == null || cached.isExpired()) {
        synchronized (TokenCache.class) {
            cached = CACHE.get(AppConfig.TOKEN_SECRET_NAME);
            if (cached == null || cached.isExpired()) {
                LOG.info("Fetching fresh auth token from Secrets Manager");
                CACHE.put(
                        AppConfig.TOKEN_SECRET_NAME,
                        fetchToken()  // ← TOKEN FETCHED HERE
                );
            }
        }
    }

    return CACHE.get(AppConfig.TOKEN_SECRET_NAME).token();  // ← TOKEN RETURNED HERE
}
```

**Token Retrieval Method**:

```java
private static CachedToken fetchToken() {
    try {
        String secretValue = getSecretsProvider().get(AppConfig.TOKEN_SECRET_NAME);
        JsonNode json = MAPPER.readTree(secretValue);
        String token = json.get("token").asText();  // ← TOKEN EXTRACTED HERE

        // 🔐 DEBUG: Print token for visibility
        LOG.info("🔐 Token retrieved from Secrets Manager: {}", token);
        LOG.debug("Token length: {} characters", token.length());
        LOG.debug("Token starts with: {}",
                token.substring(0, Math.min(10, token.length())) + "...");

        Instant expiryTime = Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS);
        LOG.debug("Token cached until: {}", expiryTime);

        return new CachedToken(token, expiryTime);

    } catch (Exception e) {
        LOG.error("Failed to fetch token from Secrets Manager", e);
        throw new ExternalApiException("Failed to fetch authentication token", e);
    }
}
```

---

### 2. 🌐 ExternalApiClient.java - TOKEN USAGE

**Location**: `src/main/java/com/project/client/ExternalApiClient.java`

This is where the token is used in the API request:

```java
public String callExternalApi() {
    LOG.info("Initiating external API call to: {}", AppConfig.EXTERNAL_API_URL);

    Supplier<String> apiCall = () -> {
        HttpGet request = null;
        try {
            request = new HttpGet(AppConfig.EXTERNAL_API_URL);
            String token = TokenCache.getToken();  // ← TOKEN RETRIEVED HERE

            // 🔐 DEBUG: Print token usage
            LOG.info("🔐 Using token in request: {}",
                    token.substring(0, Math.min(20, token.length())) + "...");
            LOG.debug("Full token: {}", token);  // ← FULL TOKEN LOGGED HERE

            request.setHeader("Authorization", "Bearer " + token);  // ← TOKEN USED HERE
            request.setHeader("Content-Type", "application/json");

            LOG.debug("Executing HTTP GET request to external API");

            return HttpClientFactory.getClient().execute(request, response -> {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();

                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = new String(entity.getContent().readAllBytes());
                    LOG.info("External API call successful: status={}", statusCode);
                    return responseBody;
                } else {
                    String errorBody = new String(entity.getContent().readAllBytes());
                    LOG.error("External API error: status={}, body={}", statusCode, errorBody);
                    throw new ExternalApiException("API returned status: " + statusCode);
                }
            });
        } catch (IOException e) {
            LOG.error("Network error calling external API", e);
            throw new ExternalApiException("Network error during API call", e);
        }
    };

    // Wrap with retry logic
    try {
        return Retry.decorateSupplier(RetryConfigProvider.RETRY, apiCall).get();
    } catch (Exception e) {
        LOG.error("Failed to call external API after retries", e);
        throw new ExternalApiException("External API call failed after retries", e);
    }
}
```

---

### 3. 📤 ApiHandler.java - API CALL INVOCATION

**Location**: `src/main/java/com/project/ApiHandler.java`

This is the Lambda handler that kicks off the token flow:

```java

@Override
public APIGatewayProxyResponseEvent handleRequest(
        APIGatewayProxyRequestEvent request,
        Context context) {

    try {
        MDC.put("requestId", context != null ? context.getAwsRequestId() : "unknown");
        MDC.put("path", request != null ? String.valueOf(request.getPath()) : "unknown");
        MDC.put("httpMethod", request != null ? String.valueOf(request.getHttpMethod()) : "unknown");

        log.info("Received request: path={}, method={}, requestId={}",
                request != null ? request.getPath() : "null",
                request != null ? request.getHttpMethod() : "null",
                context != null ? context.getAwsRequestId() : "null");

        try {
            String response = getClient().callExternalApi();  // ← TOKEN FLOW STARTS HERE
            return buildSuccessResponse(response, 200);

        } catch (ExternalApiException e) {
            log.error("External API error: {}", e.getMessage(), e);
            return buildErrorResponse("External API error: " + e.getMessage(), 502);

        } catch (Exception e) {
            log.error("Unexpected error processing request", e);
            return buildErrorResponse("Internal server error", 500);
        }

    } finally {
        MDC.clear();
    }
}
```

---

## 🔐 Where Token is Printed in Logs

Your token will be printed in **3 places** when Lambda executes:

### 1️⃣ **Token Retrieved from Secrets Manager** (TokenCache.java)

```
[INFO] 🔐 Token retrieved from Secrets Manager: {YOUR_TOKEN_VALUE}
[DEBUG] Token length: 123 characters
[DEBUG] Token starts with: eyJhbGciOiJI...
```

### 2️⃣ **Token Used in API Request** (ExternalApiClient.java)

```
[INFO] 🔐 Using token in request: eyJhbGciOiJIUzI1NiIsInR5cCI...
[DEBUG] Full token: {YOUR_FULL_TOKEN_VALUE}
```

### 3️⃣ **Cache Info** (TokenCache.java)

```
[DEBUG] Token cached until: 2025-12-27T17:55:00Z
```

---

## 📋 Token Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  AWS Secrets Manager                                    │
│  ├─ Secret Name: external-api/token                    │
│  └─ Secret Value: {                                    │
│       "client_id": "ce43d3bd-...",                     │
│       "client_secret": "aRZdZP63VqTm..."              │
│     }                                                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
                        ↓
        Powertools SecretsProvider.get()
                        ↓
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  TokenCache.fetchToken()                                │
│  ├─ Parse JSON from secret                            │
│  ├─ Extract "token" field                             │
│  ├─ 🔐 LOG: Token retrieved                           │
│  └─ Cache in memory (55 minutes)                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  ExternalApiClient.callExternalApi()                    │
│  ├─ Get token from cache                              │
│  ├─ 🔐 LOG: Token in use                              │
│  ├─ Add to Authorization header                       │
│  └─ Execute HTTP request                              │
│                                                         │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  External API (motiveintegrator.com)                    │
│  └─ Receives: Authorization: Bearer {token}           │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 How Token is Generated

Your token is NOT generated in code - it's:

1. **Stored in AWS Secrets Manager**
    - Stored as: `{ "client_id": "...", "client_secret": "..." }`
    - Retrieved by: `TokenCache.fetchToken()`

2. **Retrieved and Cached**
    - Cached for 55 minutes in Lambda memory
    - Thread-safe using double-checked locking

3. **Used in API Requests**
    - Injected as: `Authorization: Bearer {token}`
    - Automatically retried on failure

---

## 📝 Example Log Output

When your Lambda runs, you'll see logs like:

```
[2025-12-27 17:00:00] [INFO] Received request: path=/api/endpoint, method=GET, requestId=abc-123
[2025-12-27 17:00:00] [INFO] Initiating external API call to: https://exchange-staging.motiveintegrator.com
[2025-12-27 17:00:00] [INFO] Fetching fresh auth token from Secrets Manager
[2025-12-27 17:00:00] [INFO] 🔐 Token retrieved from Secrets Manager: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U
[2025-12-27 17:00:00] [DEBUG] Token length: 97 characters
[2025-12-27 17:00:00] [DEBUG] Token starts with: eyJhbGciOiJI...
[2025-12-27 17:00:00] [DEBUG] Token cached until: 2025-12-27T17:55:00Z
[2025-12-27 17:00:00] [INFO] 🔐 Using token in request: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI...
[2025-12-27 17:00:00] [DEBUG] Full token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U
[2025-12-27 17:00:00] [DEBUG] Executing HTTP GET request to external API
[2025-12-27 17:00:01] [INFO] External API call successful: status=200
```

---

## ✅ Token Flow Summary

| Component         | Purpose           | Token Visible?    |
|-------------------|-------------------|-------------------|
| ApiHandler        | Entry point       | No                |
| ExternalApiClient | Makes API call    | ✅ YES - Logged    |
| TokenCache        | Gets/caches token | ✅ YES - Logged    |
| Secrets Manager   | Stores token      | ✅ YES - Retrieved |

---

## 🔐 Changes Made for Visibility

I've added logging to two files:

### 1. TokenCache.java (fetchToken method)

Added three logging statements:

```java
LOG.info("🔐 Token retrieved from Secrets Manager: {}",token);
LOG.

debug("Token length: {} characters",token.length());
        LOG.

debug("Token starts with: {}",token.substring(0, Math.min(10, token.length()))+"...");
```

### 2. ExternalApiClient.java (callExternalApi method)

Added two logging statements:

```java
LOG.info("🔐 Using token in request: {}",token.substring(0, Math.min(20, token.length()))+"...");
        LOG.

debug("Full token: {}",token);
```

These logs will print to CloudWatch when your Lambda runs!

---

## 🚀 Next Steps

1. **Rebuild JAR**
   ```bash
   mvn clean install -DskipTests
   ```

2. **Deploy to LocalStack**
   ```bash
   cd infra/terraform
   terraform apply -var-file=terraform.localstack.tfvars
   ```

3. **Check Logs** - You'll see the token printed:
   ```bash
   aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
   ```

---

**Status**: ✅ Token flow documented and logging added

