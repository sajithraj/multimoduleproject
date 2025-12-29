# 📚 Service Project Architecture Guide

## Date: December 28, 2025

---

## 🏗️ Architecture Overview

### Request Flow (End-to-End)

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                              │
│  POST /api/auth  (or any configured endpoint)              │
└────────────────────┬────────────────────────────────────────┘
                     │ APIGatewayProxyRequestEvent
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                   ApiHandler.java                           │
│  @Logging(logEvent = true) - Handler Level Only            │
│                                                             │
│  1. Receives request                                        │
│  2. Sets up MDC for request tracking                        │
│  3. Calls callExternalApi()                                 │
│  4. Returns APIGatewayProxyResponseEvent                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│           ExternalApiClient.getInstance()                   │
│  Singleton - No @Logging annotation needed                 │
│                                                             │
│  1. Initializes on first call                               │
│  2. Creates ApigeeSecretsProvider instance                  │
│  3. Prepares HttpClient (java.net.http)                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│      tokenProvider.getValue(null)                           │
│  ApigeeSecretsProvider (from token module)                  │
│                                                             │
│  1. Fetches secret from Secrets Manager                     │
│  2. Applies ApigeeBearerTransformer                         │
│  3. Returns bearer token                                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│            Build HTTP Request                               │
│  java.net.http.HttpRequest                                  │
│                                                             │
│  Headers:                                                   │
│  - Authorization: Bearer {token}                            │
│  - x-dealer-code: Z3DT01                                    │
│  - x-bod-id: 17b1c782-1a09-4588-ac37-9d4534e5f977         │
│  - Content-Type: application/json                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│         HttpClientFactory.getClient()                       │
│  java.net.http.HttpClient with SSL trust-all               │
│                                                             │
│  - Sends HTTP request to external API                       │
│  - Returns HttpResponse<String>                             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│              External API Response                          │
│                                                             │
│  Success (200): Return data to ApiHandler                   │
│  Error (4xx/5xx): Throw ExternalApiException                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│              ApiHandler Response                            │
│                                                             │
│  Success: 200 + JSON body                                   │
│  Error: 502/500 + error JSON                                │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Service Project Structure

```
service/
├── src/main/java/com/project/service/
│   ├── ApiHandler.java                 ← Lambda Entry Point
│   ├── client/
│   │   └── ExternalApiClient.java      ← HTTP Client for External API
│   ├── config/
│   │   └── AppConfig.java              ← Configuration (env vars)
│   ├── exception/
│   │   └── ExternalApiException.java   ← Custom exception
│   └── util/
│       └── HttpClientFactory.java      ← HTTP Client factory
│
└── src/main/resources/
    └── log4j2.xml                      ← Logging configuration
```

---

## 🔧 Component Details

### 1. **ApiHandler.java** - Lambda Entry Point

**Purpose:** Handles API Gateway requests

**Responsibilities:**

- ✅ Receives `APIGatewayProxyRequestEvent`
- ✅ Sets up MDC (Mapped Diagnostic Context) for request tracking
- ✅ Orchestrates the API call
- ✅ Returns `APIGatewayProxyResponseEvent`
- ✅ Handles exceptions and error responses

**Key Code:**

```java

@Logging(logEvent = true)  // ← ONLY HERE (handler level)
@Override
public APIGatewayProxyResponseEvent handleRequest(
        APIGatewayProxyRequestEvent request,
        Context context) {

    // Set up request tracking
    MDC.put("requestId", context.getAwsRequestId());
    MDC.put("path", request.getPath());
    MDC.put("httpMethod", request.getHttpMethod());

    // Call external API
    String response = callExternalApi();

    // Return response
    return buildSuccessResponse(response, 200);
}
```

**Why @Logging only here?**

- ✅ Powertools automatically instruments all methods called within
- ✅ Logs input event and output response
- ✅ No need to annotate private methods or helper classes

---

### 2. **ExternalApiClient.java** - HTTP Client

**Purpose:** Manages HTTP calls to external API with OAuth2 token

**Responsibilities:**

- ✅ Singleton pattern (one instance per Lambda container)
- ✅ Fetches OAuth2 token via `ApigeeSecretsProvider`
- ✅ Builds HTTP request with headers
- ✅ Sends request using `java.net.http.HttpClient`
- ✅ Returns response or throws exception

**Key Code:**

```java
public static ExternalApiClient getInstance() {
    if (instance == null) {
        synchronized (ExternalApiClient.class) {
            if (instance == null) {
                instance = new ExternalApiClient();
                tokenProvider = ApigeeSecretsProvider.get();
            }
        }
    }
    return instance;
}

public String callExternalApi() {
    // Fetch token
    String accessToken = tokenProvider.getValue(null);

    // Build request
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(AppConfig.EXTERNAL_API_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("x-dealer-code", "Z3DT01")
            .header("x-bod-id", "17b1c782-1a09-4588-ac37-9d4534e5f977")
            .GET()
            .build();

    // Send request
    HttpResponse<String> response = HttpClientFactory.getClient().send(request, ...);

    return response.body();
}
```

**Why NO @Logging here?**

- ❌ Not a Lambda handler
- ❌ Already covered by handler's @Logging
- ✅ Uses standard SLF4J logger for specific messages

---

### 3. **HttpClientFactory.java** - HTTP Client Factory

**Purpose:** Creates and manages `java.net.http.HttpClient` with SSL configuration

**Responsibilities:**

- ✅ Singleton HTTP client
- ✅ SSL context with trust-all policy
- ✅ TLS 1.2 protocol
- ✅ 30-second connection timeout

**Key Code:**

```java
public static HttpClient getClient() {
    if (client == null) {
        synchronized (HttpClientFactory.class) {
            if (client == null) {
                SSLContext sslContext = SSLContexts.custom()
                        .setProtocol("TLSv1.2")
                        .loadTrustMaterial(null,
                                (X509Certificate[] chain, String authType) -> true)
                        .build();

                client = HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .connectTimeout(Duration.ofSeconds(30))
                        .build();
            }
        }
    }
    return client;
}
```

**Why trust-all SSL?**

- ✅ Handles self-signed certificates
- ✅ Works with internal corporate CAs
- ✅ Consistent with token module approach

---

### 4. **AppConfig.java** - Configuration

**Purpose:** Centralized configuration from environment variables

**Key Environment Variables:**

```java
public static final String TOKEN_ENDPOINT_URL = getEnv("TOKEN_ENDPOINT_URL");
public static final String TOKEN_SECRET_NAME = getEnv("TOKEN_SECRET_NAME");
public static final String EXTERNAL_API_URL = getEnv("EXTERNAL_API_URL");
```

**Set in Terraform:**

```terraform
environment {
  variables = {
    TOKEN_ENDPOINT_URL = "https://exchange-staging.motiveintegrator.com/v1/authorize/token"
    TOKEN_SECRET_NAME  = "external-api/token"
    EXTERNAL_API_URL   = "https://exchange-staging.motiveintegrator.com/v2/repairorder/..."
    AWS_REGION         = "us-east-1"
    ENVIRONMENT        = "dev"
  }
}
```

---

## 🔍 @Logging Annotation - Best Practices

### ✅ Correct Usage (Handler Level Only)

```java
// ApiHandler.java
@Logging(logEvent = true)  // ← YES! Log input/output events
@Override
public APIGatewayProxyResponseEvent handleRequest(...) {
    // All methods called from here are automatically instrumented
}
```

### ❌ Incorrect Usage (Everywhere)

```java
// ApiHandler.java
@Logging  // ← Redundant if logEvent=true is used below
@Override
public APIGatewayProxyResponseEvent handleRequest(...) {
}

@Logging(logEvent = true)  // ← WRONG! Don't use on private methods
private String callExternalApi() {
}

// ExternalApiClient.java
@Logging(logEvent = true)  // ← WRONG! Not a Lambda handler
public String callExternalApi() {
}
```

### 📊 @Logging Annotation Options

| Option                         | Purpose                         | When to Use                  |
|--------------------------------|---------------------------------|------------------------------|
| `@Logging`                     | Basic logging                   | Handler level (minimal)      |
| `@Logging(logEvent = true)`    | Log input/output events         | **Recommended for handlers** |
| `@Logging(clearState = true)`  | Clear state between invocations | Stateful operations          |
| `@Logging(samplingRate = 0.5)` | Sample 50% of requests          | High-volume debugging        |

### 🎯 Why Handler Level Only?

**Powertools Behavior:**

1. `@Logging` at handler level instruments the **entire execution**
2. All methods called from handler are automatically included
3. Correlation IDs, request IDs propagate automatically
4. Adding `@Logging` to other methods is redundant

**Example:**

```java
// ApiHandler.java
@Logging(logEvent = true)  // ← Sets up logging context
public APIGatewayProxyResponseEvent handleRequest(...) {
    callExternalApi();  // ← Automatically included in logging context
}

private String callExternalApi() {
    LOG.info("Calling API");  // ← This log will have correlation ID
    ExternalApiClient.getInstance().callExternalApi();  // ← Also included
}

// ExternalApiClient.java
public String callExternalApi() {
    LOG.info("Sending request");  // ← Also has correlation ID
}
```

---

## 🐛 Log4j2 Configuration - Fixed

### ❌ Old Configuration (Broken)

```xml

<Configuration packages="software.amazon.lambda.powertools.logging">
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <LambdaJsonLayout/>  <!-- ❌ Error: invalid element -->
        </Console>
    </Appenders>
</Configuration>
```

**Error:**

```
Console contains an invalid element or attribute "LambdaJsonLayout"
```

### ✅ New Configuration (Fixed)

```xml

<Configuration status="WARN">
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <JsonTemplateLayout eventTemplateUri="classpath:LambdaJsonLayout.json"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="ConsoleAppender"/>
        </Root>
        <Logger name="com.project" level="DEBUG" additivity="false">
            <AppenderRef ref="ConsoleAppender"/>
        </Logger>
        <Logger name="software.amazon.awssdk" level="WARN" additivity="false">
            <AppenderRef ref="ConsoleAppender"/>
        </Logger>
    </Loggers>
</Configuration>
```

**What Changed:**

- ✅ `LambdaJsonLayout` → `JsonTemplateLayout` (correct for Powertools v2)
- ✅ Uses `eventTemplateUri="classpath:LambdaJsonLayout.json"`
- ✅ Added `status="WARN"` for Log4j2 internal logging
- ✅ Fixed logger additivity to prevent duplicate logs

---

## 📝 How Everything Works Together

### Scenario: User Calls API

**Step 1: API Gateway receives request**

```json
{
  "httpMethod": "GET",
  "path": "/api/data",
  "headers": {
    "User-Agent": "..."
  }
}
```

**Step 2: ApiHandler receives event**

```java

@Logging(logEvent = true)  // ← Logs incoming event
public APIGatewayProxyResponseEvent handleRequest(...) {
    MDC.put("requestId", context.getAwsRequestId());  // ← Request tracking
    String response = callExternalApi();
}
```

**Step 3: ExternalApiClient fetches token**

```java
// Singleton initialization (first time only)
tokenProvider =ApigeeSecretsProvider.

get();

// Fetch token
String accessToken = tokenProvider.getValue(null);
// ↓
// ApigeeSecretsProvider.getValue()
//   ↓
//   getSecretFromSecretsManager("external-api/token")
//     ↓
//     {"username": "...", "password": "..."}
//   ↓
//   ApigeeBearerTransformer.applyTransformation()
//     ↓
//     Calls OAuth2 endpoint with Basic Auth
//     ↓
//     Returns: "Bearer eyJhbGciOiJSUzI1..."
```

**Step 4: Make HTTP request to external API**

```java
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(EXTERNAL_API_URL))
        .header("Authorization", "Bearer " + accessToken)
        .header("x-dealer-code", "Z3DT01")
        .GET()
        .build();

HttpResponse<String> response = HttpClientFactory.getClient().send(request, ...);
// ↓
// Returns: {"documentId":"DO-73859",...}
```

**Step 5: ApiHandler returns response**

```java
return buildSuccessResponse(response.body(), 200);
// ↓
// APIGatewayProxyResponseEvent {
//   statusCode: 200,
//   body: "{\"documentId\":\"DO-73859\",...}",
//   headers: {"Content-Type": "application/json"}
// }
```

---

## 🎯 Summary

### ✅ What's Working

1. **ApiHandler** - Entry point with proper @Logging
2. **ExternalApiClient** - Manages HTTP calls with OAuth2
3. **HttpClientFactory** - SSL-enabled HTTP client
4. **ApigeeSecretsProvider** - Token fetching (from token module)
5. **Log4j2** - JSON logging for CloudWatch

### 🔧 What Was Fixed

1. ✅ Removed redundant `@Logging` annotations
2. ✅ Fixed Log4j2 configuration (LambdaJsonLayout error)
3. ✅ Cleaned up logging approach

### 📊 Logging Best Practices

- ✅ Use `@Logging(logEvent = true)` **only at handler level**
- ✅ Use standard SLF4J Logger for application logs
- ✅ Use MDC for request correlation
- ❌ Don't use @Logging on private methods or utility classes

---

**Next Steps:**

1. Rebuild: `mvn clean package -DskipTests`
2. Redeploy: `terraform apply`
3. Test: No more Log4j2 errors! ✅

