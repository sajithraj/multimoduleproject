# Service Module - External API Integration

AWS Lambda function for authenticated external API integration with OAuth2 token management and SSL/TLS support.

## 📋 Overview

The Service module provides a production-ready Lambda function that:

- Calls external APIs with OAuth2 bearer token authentication
- Manages token lifecycle using the Token module
- Handles SSL/TLS connections with custom certificates
- Implements connection pooling for performance
- Provides structured JSON logging

---

## 🎯 Features

✅ **Token-Based Authentication** - OAuth2 bearer tokens  
✅ **Token Caching** - Reuses tokens from cache  
✅ **SSL/TLS Support** - Custom certificate handling  
✅ **Connection Pooling** - Optimized HTTP connections  
✅ **Error Handling** - Retry logic and fallbacks  
✅ **JSON Logging** - Structured CloudWatch logs  
✅ **API Gateway Integration** - REST API endpoints  

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                  API Gateway                         │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│             Service Lambda                          │
│                                                       │
│  ┌──────────────────────────────────────────────┐  │
│  │         ApiHandler                           │  │
│  │  - Request validation                        │  │
│  │  - Response formatting                       │  │
│  └───────────────────┬──────────────────────────┘  │
│                      │                               │
│  ┌───────────────────▼──────────────────────────┐  │
│  │     ExternalApiClient                        │  │
│  │  - Token management                          │  │
│  │  - HTTP client                               │  │
│  │  - SSL/TLS handling                          │  │
│  └───────────────────┬──────────────────────────┘  │
│                      │                               │
│  ┌───────────────────▼──────────────────────────┐  │
│  │     SSMApigeeProvider (Token Module)        │  │
│  │  - Token caching                             │  │
│  │  - Token refresh                             │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────┘
                      │
        ┌─────────────▼─────────────┐
        │   External API            │
        │   (Third-party service)   │
        └───────────────────────────┘
```

---

## 📦 Components

### 1. ApiHandler
**Purpose:** Lambda request handler

**Responsibilities:**
- Parse API Gateway events
- Validate requests
- Call external API
- Format responses
- Handle errors

```java
public class ApiHandler implements RequestHandler<
    APIGatewayProxyRequestEvent, 
    APIGatewayProxyResponseEvent> {
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
        APIGatewayProxyRequestEvent event, 
        Context context) {
        // Process request
    }
}
```

---

### 2. ExternalApiClient
**Purpose:** HTTP client for external API calls

**Features:**
- Token-based authentication
- Connection pooling
- SSL/TLS support
- Retry logic
- Error handling

```java
public class ExternalApiClient {
    public String callExternalApi(String url) {
        String token = tokenProvider.getToken(secretName);
        // Make authenticated request
    }
}
```

---

### 3. HttpClientFactory
**Purpose:** Configure HTTP client with SSL support

**Features:**
- Custom SSL context
- Certificate loading
- Connection pooling
- Timeout configuration

```java
public class HttpClientFactory {
    public static HttpClient createClient() {
        SSLContext sslContext = loadSSLContext();
        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

---

## 🔧 Configuration

### Environment Variables

```bash
# Required
TOKEN_ENDPOINT_URL=https://api.example.com/oauth/token
TOKEN_SECRET_NAME=external-api/token
EXTERNAL_API_URL=https://api.example.com/v1/resource
AWS_REGION=us-east-1

# Optional
OAUTH2_TIMEOUT_SECONDS=3
POWERTOOLS_SERVICE_NAME=api-service
POWERTOOLS_LOG_LEVEL=INFO
```

### Lambda Configuration

```
Handler: com.project.service.ApiHandler::handleRequest
Runtime: java21
Memory: 512 MB
Timeout: 60 seconds
```

---

## 🚀 Usage

### API Gateway Request

```bash
curl -X GET https://api-id.execute-api.region.amazonaws.com/prod/api \
  -H "Content-Type: application/json"
```

### Lambda Direct Invoke

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json
```

### Response Format

```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  },
  "body": "{\"status\":\"success\",\"data\":{...}}"
}
```

---

## 🧪 Testing

### Run Tests
```bash
mvn test -pl service
```

### Integration Test
```bash
# Deploy to LocalStack
cd infra/terraform
terraform apply -var="use_localstack=true"

# Test Lambda
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json
```

### Manual Testing

```bash
# Test with actual API Gateway
curl https://your-api-gateway-url/prod/api
```

---

## 🔐 Security

### IAM Permissions Required

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:external-api/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### SSL/TLS Configuration

- Custom truststore with root certificates
- Certificate validation enabled
- TLS 1.2+ required
- Certificate located at: `src/main/resources/svb_root_ssl_cert.pem`

---

## ⚡ Performance

### Connection Pooling

```java
HttpClient client = HttpClient.newBuilder()
    .version(Version.HTTP_1_1)
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

**Benefits:**
- Reuses connections
- Reduces latency
- Improves throughput

### Metrics

| Metric | Cold Start | Warm Start (Cached) | Warm Start (No Cache) |
|--------|------------|---------------------|----------------------|
| Duration | ~4000ms | ~500ms | ~2500ms |
| Token Fetch | 2000ms | 5ms | 2000ms |
| API Call | 1500ms | 1500ms | 1500ms |

---

## 📊 Logging

### Log Examples

```json
{
  "instant": {"epochSecond": 1735455850},
  "level": "INFO",
  "loggerName": "com.project.service.ApiHandler",
  "message": "Received request: path=/api, method=GET, requestId=abc-123"
}

{
  "level": "INFO",
  "loggerName": "com.project.service.client.ExternalApiClient",
  "message": "External API call successful: status=200"
}

{
  "level": "ERROR",
  "loggerName": "com.project.service.ApiHandler",
  "message": "External API error: Connection timeout"
}
```

---

## 🔄 Request Flow

```
API Gateway Request
     │
     ▼
ApiHandler
     │
     ├─── Parse Request
     │
     ▼
ExternalApiClient
     │
     ├─── Get Token (from cache or fetch)
     │
     ├─── Build HTTP Request
     │
     ├─── Add Bearer Token
     │
     ▼
External API Call
     │
     ▼
Parse Response
     │
     ▼
Format API Gateway Response
```

---

## 🛠️ Dependencies

```xml
<!-- Token Module -->
<dependency>
    <groupId>com.project</groupId>
    <artifactId>token</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- AWS Lambda -->
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-lambda-java-core</artifactId>
    <version>1.2.3</version>
</dependency>

<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-lambda-java-events</artifactId>
    <version>3.11.4</version>
</dependency>

<!-- HTTP Client -->
<!-- Uses Java 21 built-in HttpClient -->

<!-- Logging -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.25.3</version>
</dependency>
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. SSL Handshake Failed
**Problem:** Certificate validation error  
**Solution:** Check certificate file exists and is valid

```bash
# Verify certificate file
ls -la src/main/resources/svb_root_ssl_cert.pem
```

#### 2. Token Expired
**Problem:** 401 Unauthorized  
**Solution:** Token cache automatically refreshes

#### 3. API Timeout
**Problem:** External API not responding  
**Solution:** Increase Lambda timeout

```bash
aws lambda update-function-configuration \
  --function-name my-token-auth-lambda \
  --timeout 90
```

#### 4. Connection Refused
**Problem:** Cannot reach external API  
**Solution:** Check VPC configuration and security groups

---

## 🔄 Error Handling

### Error Response Format

```json
{
  "statusCode": 500,
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "{
    \"success\": false,
    \"message\": \"Internal server error\",
    \"error\": \"Connection timeout\"
  }"
}
```

### HTTP Status Codes

| Code | Description | Handler |
|------|-------------|---------|
| 200 | Success | Return API response |
| 400 | Bad Request | Validation error |
| 401 | Unauthorized | Token issue |
| 500 | Server Error | Exception occurred |
| 502 | Bad Gateway | External API error |
| 504 | Gateway Timeout | API timeout |

---

## 📈 Monitoring

### CloudWatch Metrics

- **Invocations** - Total Lambda invocations
- **Duration** - Execution time
- **Errors** - Failed invocations
- **Throttles** - Rate limiting

### Custom Metrics

```java
// Log execution time
long start = System.currentTimeMillis();
// ... execute
long duration = System.currentTimeMillis() - start;
log.info("External API call completed in {}ms", duration);
```

---

## 🔄 Changelog

### Version 1.0.0 (2025-12-29)
- ✅ Token-based authentication
- ✅ SSL/TLS support
- ✅ Connection pooling
- ✅ JSON structured logging
- ✅ Error handling

---

**Built with ❤️ using Java 21, AWS Lambda, and Token Module**

[← Back to Main README](../README.md)

