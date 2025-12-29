# API Integration Specifications

## External API Specifications

### Token Authorization Endpoint

**Endpoint:** `POST /v1/authorize/token`

**Base URL:** `https://exchange-staging.motiveintegrator.com`

**Full URL:** `https://exchange-staging.motiveintegrator.com/v1/authorize/token`

---

## 1. Token Authorization Request

### Request Details

| Property           | Value                             |
|--------------------|-----------------------------------|
| **Method**         | POST                              |
| **Content-Type**   | application/x-www-form-urlencoded |
| **Authentication** | None (credentials in body)        |
| **Timeout**        | 5 seconds                         |

### Request Body Format

**Form-Encoded Parameters:**

```
grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET
```

### Request Parameters

| Parameter     | Type   | Required | Description                  | Example            |
|---------------|--------|----------|------------------------------|--------------------|
| grant_type    | String | Yes      | OAuth2 grant type            | client_credentials |
| client_id     | String | Yes      | API client identifier        | abc123xyz          |
| client_secret | String | Yes      | API client secret            | secret_key_value   |
| scope         | String | No       | Optional scope specification | read write         |

### Curl Example

```bash
curl -X POST \
  'https://exchange-staging.motiveintegrator.com/v1/authorize/token' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data 'grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET'
```

### Java Implementation

```java
TokenAuthRequest tokenRequest = new TokenAuthRequest(
        "client_credentials",     // grant_type
        "YOUR_CLIENT_ID",         // clientId
        "YOUR_CLIENT_SECRET"      // clientSecret
);

TokenAuthResponse response = TokenAuthorizationUtil.requestToken(
        "https://exchange-staging.motiveintegrator.com/v1/authorize/token",
        tokenRequest
);
```

---

## 2. Token Authorization Response

### Success Response (HTTP 200)

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "token_type": "Bearer",
  "expires_in": 14400
}
```

### Response Fields

| Field            | Type         | Description                                 |
|------------------|--------------|---------------------------------------------|
| **access_token** | String (JWT) | Bearer token for API authentication         |
| **token_type**   | String       | Token type (always "Bearer")                |
| **expires_in**   | Integer      | Token lifetime in seconds (14400 = 4 hours) |

### Token Lifetime

- **Nominal Lifetime:** 14400 seconds (4 hours)
- **Cached Until:** 3300 seconds (55 minutes) - Conservative to avoid expiry mid-request
- **Cache Expiry Strategy:** Check on every request, refresh if expired

### Error Response Examples

#### 401 Unauthorized (Invalid Credentials)

```json
{
  "error": "invalid_client",
  "error_description": "Client authentication failed"
}
```

#### 400 Bad Request (Missing Parameters)

```json
{
  "error": "invalid_request",
  "error_description": "Missing required parameter: client_id"
}
```

#### 500 Server Error (Retry Eligible)

```json
{
  "error": "server_error",
  "error_description": "Internal server error"
}
```

---

## 3. Using the Token for API Calls

### Bearer Token Format

Once obtained, use the token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Request Headers

```http
GET /v1/data/endpoint HTTP/1.1
Host: exchange-staging.motiveintegrator.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
Accept: application/json
```

### Java Implementation

```java
// Token is automatically added by AuthenticatedApiClient
ExternalApiRequest request = new ExternalApiRequest("/v1/data/list", "GET");
ExternalApiResponse response = AuthenticatedApiClient.callApi(request);
```

---

## 4. Token Caching Strategy

### Cache Management

| Aspect            | Value                       | Reason                           |
|-------------------|-----------------------------|----------------------------------|
| **Storage**       | Lambda Container Memory     | Fast access, no network          |
| **Duration**      | 55 minutes                  | Conservative vs 60-minute actual |
| **Max Reuse**     | Multiple requests in 55 min | Reduce token endpoint calls      |
| **Expiry Check**  | Before every API call       | Ensure valid token               |
| **Thread Safety** | Double-checked locking      | Safe for concurrent access       |

### Cache Behavior

```
Request 1 (T=0min)
├─ Token cache empty
├─ Request new token from API
├─ Store in cache (expires at T=55min)
└─ Return token

Request 2 (T=5min)
├─ Token cache has valid token
├─ Return cached token (no API call)
└─ Latency: <10ms

Request 3 (T=60min)
├─ Token cache expired
├─ Request new token from API
├─ Store in cache (expires at T=115min)
└─ Return token
```

---

## 5. Retry Policy

### Retry Trigger Conditions

**Retryable Errors (5xx):**

- 500 Internal Server Error
- 502 Bad Gateway
- 503 Service Unavailable
- 504 Gateway Timeout

**Non-Retryable Errors (4xx):**

- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found

### Retry Configuration

| Parameter            | Value                  |
|----------------------|------------------------|
| **Max Attempts**     | 3                      |
| **Backoff Strategy** | Exponential            |
| **Initial Delay**    | 1 second               |
| **Delay Multiplier** | 2x                     |
| **Max Total Time**   | ~7 seconds (1 + 2 + 4) |

### Retry Sequence

```
Attempt 1
├─ Execute request
├─ 5xx error?
│  ├─ YES → Wait 1s, retry
│  └─ NO → Return response

Attempt 2
├─ Wait 1s
├─ Execute request
├─ 5xx error?
│  ├─ YES → Wait 2s, retry
│  └─ NO → Return response

Attempt 3
├─ Wait 2s
├─ Execute request
├─ 5xx error?
│  ├─ YES → Throw exception
│  └─ NO → Return response
```

---

## 6. Error Handling

### Error Scenarios

| Scenario            | HTTP Status | Action                      |
|---------------------|-------------|-----------------------------|
| Invalid credentials | 401         | Fail immediately (no retry) |
| Missing parameters  | 400         | Fail immediately (no retry) |
| Server error        | 500         | Retry up to 3 times         |
| Service unavailable | 503         | Retry up to 3 times         |
| Network timeout     | N/A         | Retry up to 3 times         |
| Connection refused  | N/A         | Retry up to 3 times         |

### Exception Types

```java
ExternalApiException
├─
Token authorization
failed
├─
API call

failed(after 3retries)
├─
Invalid response
format
├─Network/
timeout error
└─
Configuration error
```

### Error Response Handling

```java
try{
ExternalApiResponse response = AuthenticatedApiClient.callApi(request);
    
    if(response.

isSuccess()){
// Process response
Object data = response.getData();
    }else{
            // Handle error
            LOG.

error("API Error: {} - {}",
      response.getErrorCode(), 
            response.

getError());
        }

        }catch(
ExternalApiException e){
        // All retries exhausted
        LOG.

error("API integration failed",e);
// Handle exception
}
```

---

## 7. Response Parsing

### Token Response Parsing

```java
// Raw response from API
String jsonResponse = "{\"access_token\": \"eyJ...\", \"token_type\": \"Bearer\", \"expires_in\": 14400}";

// Parsed by TokenAuthResponse
TokenAuthResponse response = MAPPER.readValue(jsonResponse, TokenAuthResponse.class);

// Validation
if(response.

isValid()){
String token = response.getAccessToken();
String type = response.getTokenType();     // "Bearer"
long expiry = response.getExpiresIn();     // 14400
}
```

### Generic API Response Parsing

```java
// Raw response from API
String jsonResponse = "{\"status\": \"success\", \"data\": {...}}";

// Parsed by ExternalApiResponse
ExternalApiResponse response = MAPPER.readValue(jsonResponse, ExternalApiResponse.class);

// Usage
if(response.

isSuccess()){
Object data = response.getData();  // Could be Map, List, or Object
}
```

---

## 8. Configuration

### Environment Variables

```bash
# Required
EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com
TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token
CLIENT_ID=your_client_id_here
CLIENT_SECRET=your_client_secret_here
TOKEN_SECRET_NAME=external-api/token
```

### Secrets Manager Setup

**Secret Name:** `external-api/token`

**Secret Format:**

```json
{
  "client_id": "your_client_id",
  "client_secret": "your_client_secret"
}
```

---

## 9. Timing Specifications

### Request Timeouts

| Component             | Timeout    |
|-----------------------|------------|
| Connection timeout    | 5 seconds  |
| Read timeout          | 10 seconds |
| Write timeout         | 10 seconds |
| Total request timeout | 30 seconds |

### Response Times

| Operation              | Expected        |
|------------------------|-----------------|
| Token API (success)    | 1-2 seconds     |
| Token API (with retry) | 7-15 seconds    |
| Data API call          | 500ms-2 seconds |
| Cache lookup           | <10ms           |

---

## 10. Security Specifications

### TLS/HTTPS

- **Minimum Version:** TLS 1.2
- **Certificate Validation:** Enabled
- **Cipher Suites:** Strong ciphers only

### Authentication

- **Method:** OAuth2 Client Credentials Grant
- **Token Format:** JWT
- **Token Storage:** Lambda container memory
- **Credential Storage:** AWS Secrets Manager

### Data Protection

- **Secrets in logs:** Masked (***)
- **Token validation:** JWT format check
- **Expiry enforcement:** Before every use

---

## 11. Example Integration

### Full Flow Example

```java
// 1. Get token (cached or fresh)
String token = TokenAuthorizationService.getAccessToken();

// 2. Create API request
ExternalApiRequest request = new ExternalApiRequest(
        "/v1/data/list",  // endpoint
        "GET"             // method
);

// 3. Optional: Add custom headers
request.

addHeader("X-Request-ID","req-"+System.currentTimeMillis());

// 4. Execute call (token added automatically)
ExternalApiResponse response = AuthenticatedApiClient.callApi(request);

// 5. Handle response
if(response.

isSuccess()){
Object data = response.getData();
int statusCode = response.getStatusCode();
long timestamp = response.getTimestamp();
    
    LOG.

info("API call successful: {}",data);
    
}else if(response.

isError()){
String error = response.getError();
String errorCode = response.getErrorCode();
    
    LOG.

error("API call failed: {} - {}",errorCode, error);
}
```

---

## 12. API Endpoint Reference

### Token Endpoint

- **URL:** `https://exchange-staging.motiveintegrator.com/v1/authorize/token`
- **Method:** POST
- **Auth:** None (credentials in body)
- **Response:** TokenAuthResponse (JWT token)
- **Cache:** 55 minutes
- **Retry:** Yes (5xx only)

### Data Endpoints (Examples)

- **URL:** `https://exchange-staging.motiveintegrator.com/v1/data/...`
- **Method:** GET, POST, PUT
- **Auth:** Bearer token (automatic)
- **Response:** Varies by endpoint
- **Retry:** Yes (5xx only)

---

## 13. Testing the Integration

### Manual Testing with Curl

```bash
# 1. Get token
curl -X POST \
  'https://exchange-staging.motiveintegrator.com/v1/authorize/token' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data 'grant_type=client_credentials&client_id=YOUR_ID&client_secret=YOUR_SECRET'

# 2. Use token for API call
curl -X GET \
  'https://exchange-staging.motiveintegrator.com/v1/data/list' \
  --header 'Authorization: Bearer YOUR_TOKEN_HERE'
```

### Unit Testing

```java

@Test
public void testTokenAuthorization() {
    // Request token
    String token = TokenAuthorizationService.getAccessToken();

    // Verify format
    assertTrue(TokenAuthorizationService.isValidToken(token));

    // Verify starts with "eyJ" (JWT format)
    assertTrue(token.startsWith("eyJ"));
}

@Test
public void testApiCallWithAuth() {
    // Create request
    ExternalApiRequest request = new ExternalApiRequest("/v1/data", "GET");

    // Execute with automatic auth
    ExternalApiResponse response = AuthenticatedApiClient.callApi(request);

    // Verify success
    assertTrue(response.isSuccess());
    assertNotNull(response.getData());
}
```

---

**Last Updated:** December 27, 2025
**API Version:** v1
**Status:** Production Ready

