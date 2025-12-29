# Business Logic - Token Authorization & API Integration

## Overview

This Lambda application implements a complete token-based authentication flow for secure API communication. It combines
AWS Powertools for logging, AWS Secrets Manager for credential storage, and a caching mechanism to minimize API calls
and reduce cold start latency.

## Architecture

### Component Structure

```
├── client/
│   ├── AuthenticatedApiClient.java      # Main HTTP client with auth
│   ├── ExternalApiClient.java           # Legacy client (deprecated)
│   ├── dto/
│   │   ├── TokenAuthRequest.java        # Token request model
│   │   ├── TokenAuthResponse.java       # Token response model
│   │   ├── ExternalApiRequest.java      # Generic API request
│   │   └── ExternalApiResponse.java     # Generic API response
│   └── util/
│       └── TokenAuthorizationUtil.java  # Token utility functions
├── service/
│   └── TokenAuthorizationService.java   # Business logic orchestration
├── auth/
│   └── TokenCache.java                  # Container-level caching
└── config/
    └── AppConfig.java                   # Environment configuration
```

## Token Authorization Flow

### Step 1: Request Access Token

**Endpoint:** `POST /v1/authorize/token`

**Request Format:**

```http
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET
```

**Request Model:** `TokenAuthRequest`

- `grantType`: OAuth2 grant type (e.g., "client_credentials")
- `clientId`: API client identifier
- `clientSecret`: API client secret (sensitive)
- `scope`: Optional scope specification

### Step 2: Receive Token Response

**Response Format:**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "expires_in": 14400
}
```

**Response Model:** `TokenAuthResponse`

- `accessToken`: JWT token for API authentication
- `tokenType`: Token type (Bearer)
- `expiresIn`: Token lifetime in seconds (14400 = 4 hours)

### Step 3: Cache Token in Lambda Container

**Cache Manager:** `TokenCache`

- **Expiry:** 55 minutes (conservative vs 60-minute actual lifetime)
- **Storage:** Lambda container memory (ConcurrentHashMap)
- **Thread-Safety:** Double-checked locking pattern
- **Benefits:**
    - Eliminates API calls for token validation
    - Reduces cold start impact
    - Improves performance

### Step 4: Use Token for API Calls

**Token Injection:** Automatically added as Bearer token

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI...
```

## Class Responsibilities

### TokenAuthorizationService

**Purpose:** High-level business logic orchestration

**Key Methods:**

- `getAccessToken()`: Get token (cached or fresh)
- `requestNewToken()`: Request from external API
- `buildTokenRequest()`: Construct request with credentials
- `isValidToken(token)`: Validate JWT format

**Flow:**

1. Check if token exists in cache and is not expired
2. If valid cached token, return immediately
3. If expired or missing, request new token from API
4. Store new token in cache
5. Return token for subsequent API calls

### TokenAuthorizationUtil

**Purpose:** Low-level HTTP request/response handling

**Key Methods:**

- `requestToken(url, request)`: Execute token request with retry
- `buildFormParams(request)`: Convert request to URL-encoded form
- `readResponseBody(entity)`: Parse HTTP response

**Features:**

- Automatic retry with exponential backoff
- Response validation
- Error handling and propagation
- Structured logging

### TokenCache

**Purpose:** Container-level token storage and lifecycle management

**Key Methods:**

- `getToken()`: Retrieve cached token with expiry check
- `clearCache()`: Manual token invalidation
- `fetchToken()`: Retrieve from Secrets Manager via Powertools

**Caching Strategy:**

- Key: Secret name
- Value: `CachedToken` record with token and expiry
- Expiry: 55 minutes (conservative)
- Thread-Safety: Synchronized double-checked locking

### AuthenticatedApiClient

**Purpose:** Execute authenticated API calls to external service

**Key Methods:**

- `callApi(request)`: Execute API request with auth and retry
- `buildHttpRequest(method, url, request, token)`: Construct HTTP request
- `executeRequest(httpRequest)`: Execute and parse response

**Features:**

- Automatic token retrieval and injection
- Support for GET, POST, PUT methods
- JSON request/response handling
- Automatic retry on failure

## Environment Variables Required

```bash
# External API Configuration
EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com

# Token Endpoint
TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token

# Client Credentials (from Secrets Manager)
CLIENT_ID=your_client_id
CLIENT_SECRET=your_client_secret

# Secrets Manager Configuration
TOKEN_SECRET_NAME=external-api/token
```

## Retry Configuration

**Strategy:** Exponential backoff
**Max Attempts:** 3 (configurable via RetryConfigProvider)
**Initial Delay:** Automatic calculation
**Max Delay:** Automatic calculation

**Trigger Conditions:**

- Network timeouts
- 5xx server errors
- Connection refused

**Non-Retryable:**

- 4xx client errors
- Invalid credentials
- Invalid request format

## Error Handling

### Exception Hierarchy

```
Exception
├── ExternalApiException
│   ├── Token authorization failed
│   ├── API call failed
│   ├── Invalid response format
│   └── Network/timeout errors
```

### Error Scenarios

| Scenario                    | Handling           | Result                                 |
|-----------------------------|--------------------|----------------------------------------|
| Expired token               | Request new token  | Automatic retry                        |
| Network timeout             | Retry with backoff | ExternalApiException after max retries |
| Invalid credentials         | No retry           | Immediate ExternalApiException         |
| Invalid response            | Log and fail       | ExternalApiException with details      |
| Secrets Manager unavailable | Fail fast          | ExternalApiException                   |

## Performance Optimization

### Cold Start Reduction

1. **Lazy Initialization**
    - Powertools provider initialized on first use
    - Token cache checked before API call
    - HTTP client reused across invocations

2. **Container-Level Caching**
    - Token cached for 55 minutes
    - No need to call token API on subsequent requests
    - Reduces token endpoint load

3. **Connection Reuse**
    - HTTP client pooling (HttpClientFactory)
    - Persistent connections
    - Connection timeout: 5 seconds

### Request Optimization

1. **Minimal Retry Overhead**
    - Exponential backoff prevents thundering herd
    - Max 3 attempts
    - Fast fail on client errors

2. **Response Parsing**
    - Jackson ObjectMapper (lightweight)
    - Only parse necessary fields
    - Raw response passthrough option

## Example Usage

### Token Authorization Only

```java
// Get access token (cached or fresh)
String accessToken = TokenAuthorizationService.getAccessToken();

// Use token for HTTP headers
headers.

put("Authorization","Bearer "+accessToken);
```

### Complete API Flow

```java
// Build API request
ExternalApiRequest apiRequest = new ExternalApiRequest(
                "/v1/data/endpoint",
                "GET"
        );

// Call API with automatic authentication and retry
ExternalApiResponse response = AuthenticatedApiClient.callApi(apiRequest);

// Check response
if(response.

isSuccess()){
Object data = response.getData();
// Process data
}else{
String error = response.getError();
// Handle error
}
```

### Token Refresh (Manual)

```java
// Clear cached token to force refresh on next request
TokenAuthorizationService.clearCachedToken();

// Next call will request new token
String freshToken = TokenAuthorizationService.getAccessToken();
```

## Security Considerations

### Secrets Management

- Client credentials stored in AWS Secrets Manager
- Retrieved via Powertools SecretsProvider
- Not exposed in logs (masked with ***)

### Token Security

- JWT validation on response
- Token type verification (Bearer)
- Expiry validation before use

### Network Security

- HTTPS only (enforced by client configuration)
- TLS 1.2+ (via HttpClient5)
- Certificate validation enabled

## Testing

### Unit Test Example

```java

@Test
public void testTokenAuthorizationFlow() {
    // Request token
    String token = TokenAuthorizationService.getAccessToken();

    // Verify format (JWT)
    assertTrue(TokenAuthorizationService.isValidToken(token));

    // Call API with token
    ExternalApiRequest request = new ExternalApiRequest("/endpoint", "GET");
    ExternalApiResponse response = AuthenticatedApiClient.callApi(request);

    // Verify response
    assertTrue(response.isSuccess());
}
```

## Monitoring & Logging

### Structured Logging

- Powertools JSON format
- Correlation IDs (AWS Lambda)
- Log levels: INFO, DEBUG, WARN, ERROR

### Key Events Logged

1. **Token Authorization**
    - Request initiated
    - Token obtained (success/failure)
    - Cache hit/miss

2. **API Calls**
    - Endpoint called
    - Request details (method, path)
    - Response status and body
    - Retry attempts

3. **Errors**
    - Exception type
    - Stack trace
    - Remediation hints

## Future Enhancements

- [ ] Token refresh strategy (before expiry)
- [ ] Circuit breaker pattern (optional)
- [ ] Token encryption in cache
- [ ] Multiple credential support
- [ ] Custom token validation hooks
- [ ] Rate limiting per token

