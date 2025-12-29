# Architecture Documentation

Comprehensive architecture overview of the Java Lambda application.

## System Architecture

```
┌─────────────────┐
│   API Gateway   │
└────────┬────────┘
         │ APIGatewayProxyRequestEvent
         ▼
┌─────────────────────────────────────────────────────────────┐
│              AWS Lambda (Java 21)                            │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         ApiHandler (RequestHandler)                  │  │
│  │  - Receives APIGatewayProxyRequestEvent              │  │
│  │  - Delegates to ExternalApiClient                    │  │
│  │  - Returns APIGatewayProxyResponseEvent              │  │
│  │  - Handles errors (502, 500)                         │  │
│  └──────────┬───────────────────────────────────────────┘  │
│             │                                               │
│  ┌──────────▼───────────────────────────────────────────┐  │
│  │     ExternalApiClient                                │  │
│  │  - Makes HTTP requests to external API               │  │
│  │  - Adds Bearer token from TokenCache                 │  │
│  │  - Wraps calls with Retry logic                      │  │
│  │  - Handles response parsing (JSON)                   │  │
│  └──────────┬───────────────────────────────────────────┘  │
│             │                                               │
│  ┌──────────▼────────────────┐                              │
│  │    TokenCache             │                              │
│  │  - 55-min in-memory cache │                              │
│  │  - Thread-safe            │                              │
│  │  - Double-checked locking │                              │
│  └──────────┬────────────────┘                              │
│             │                                               │
│             │ Lazy fetch if expired                         │
│             ▼                                               │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  HttpClientFactory                                  │  │
│  │  - Connection pooling (10 total, 5 per route)       │  │
│  │  - Timeout configuration                            │  │
│  │  - Lazy initialization                              │  │
│  └─────────────────────────────────────────────────────┘  │
│             │                                               │
│  ┌──────────▼───────────────┬──────────────────────────┐  │
│  │ RetryConfigProvider      │  AppConfig               │  │
│  │  - Max 3 attempts        │  - Environment variables │  │
│  │  - Exponential backoff   │  - Configuration loading │  │
│  │  - 300ms initial wait    │  - Validation           │  │
│  └──────────────────────────┴──────────────────────────┘  │
│             │                                               │
└─────────────┼────────────────────────────────────────────────┘
              │
    ┌─────────┴──────────┬─────────────────┐
    ▼                    ▼                 ▼
┌──────────────┐  ┌──────────────────┐  ┌─────────────────┐
│   External   │  │  Secrets Manager │  │  CloudWatch     │
│     API      │  │  - Token storage │  │  - JSON Logs    │
│              │  │  - Encrypted     │  │  - Metrics      │
└──────────────┘  └──────────────────┘  └─────────────────┘
```

## Component Details

### 1. ApiHandler (Entry Point)

**Responsibility**: Lambda handler and request/response orchestration

**Key Features**:

- Implements `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
- Lazy-initializes `ExternalApiClient`
- Comprehensive error handling (500, 502 errors)
- JSON response formatting
- Powertools logging integration

**Data Flow**:

```
APIGatewayProxyRequestEvent
    ↓
Validate environment variables
    ↓
Get/initialize ExternalApiClient
    ↓
Call external API
    ↓
Handle response (success/error)
    ↓
APIGatewayProxyResponseEvent
```

### 2. ExternalApiClient

**Responsibility**: HTTP communication with retry logic

**Key Features**:

- Constructs HTTP GET request with Authorization header
- Integrates with TokenCache for token retrieval
- Wraps API call with Resilience4j Retry decorator
- Comprehensive error handling and logging
- Connection timeout handling (5s connect, 10s socket)

**Retry Flow**:

```
Initial API Call
    ├─ Success → Return response
    └─ Network/IO Error
         ├─ Retry 1 (wait 300ms)
         ├─ Retry 2 (wait 600ms)
         └─ Fail → Throw ExternalApiException
```

### 3. TokenCache

**Responsibility**: Authentication token lifecycle management

**Key Features**:

- 55-minute in-memory cache (conservative vs 60-minute token lifetime)
- Powertools v2 Parameters provider for Secrets Manager
- Thread-safe double-checked locking pattern
- Lazy initialization of secrets provider
- Automatic expiry detection

**Cache Lifecycle**:

```
Runtime Start
    ↓
Cache = empty
    ↓
First API Call
    ↓
Token not found in cache
    ↓
Fetch from Secrets Manager via Powertools
    ↓
Parse JSON: { "token": "..." }
    ↓
Cache with expiry = now + 55 minutes
    ↓
Subsequent calls (within 55 min)
    ├─ Token found and not expired → Return
    └─ Token expired → Refresh
```

### 4. HttpClientFactory

**Responsibility**: HTTP client initialization and connection management

**Key Features**:

- Lazy initialization (reduces cold start)
- Connection pooling
    - Max 10 total connections
    - Max 5 connections per route
- Timeout configuration
    - Connect timeout: 5 seconds
    - Socket timeout: 10 seconds
- Automatic retry disabled (handled by Resilience4j)

**Benefits**:

- Connection reuse across invocations (warm starts)
- Reduced latency on warm requests
- Prevents connection exhaustion

### 5. RetryConfigProvider

**Responsibility**: Centralized retry policy configuration

**Key Features**:

- Max 3 total attempts (1 initial + 2 retries)
- Exponential backoff with jitter
    - Initial: 300ms
    - Multiplier: 2.0
    - Jitter: 50%
- Specific exception handling
    - Retries: IO/Network exceptions
    - Ignores: Configuration/Validation errors

**Retry Timing**:

```
Attempt 1: T+0ms → Fails
Attempt 2: T+300ms → Fails
Attempt 3: T+600-1200ms → Success/Fail
```

### 6. AppConfig

**Responsibility**: Environment variable management

**Key Features**:

- Centralized configuration loading
- Validation on startup
- Throws exception if required variables missing
- Static initialization (fast lookup)

**Required Variables**:

- `EXTERNAL_API_URL`: Target API endpoint
- `TOKEN_SECRET_NAME`: Secrets Manager secret name

### 7. Powertools Integration

#### Logging (v2)

- JSON-formatted logs via Log4j2
- Compatible with CloudWatch Logs
- Supports structured logging with context

#### Parameters (v2)

- Secrets Manager integration
- Built-in caching support
- Automatic decryption

### 8. Exception Handling

```
ExternalApiException (Custom Runtime Exception)
    ├─ Network errors (IO, Socket, Connection)
    ├─ API errors (non-2xx responses)
    ├─ Token fetch failures
    └─ Configuration errors

Handling Strategy:
    ├─ Network/IO → Retry with backoff
    ├─ API errors → Log and return error response
    ├─ Token failures → Fail immediately
    └─ Configuration → Fail on startup
```

## Performance Characteristics

### Cold Start Breakdown

```
Event: Lambda cold start invocation

1. JVM Startup              ~1.0-1.5s
   └─ Class loading
   └─ Bytecode JIT compilation
   └─ Memory allocation

2. Dependency Initialization ~0.5-0.8s
   └─ Log4j2 setup
   └─ Jackson ObjectMapper init
   └─ AWS SDK client setup

3. Application Init         ~0.1-0.2s
   └─ AppConfig static init
   └─ RetryConfigProvider init
   └─ Handler instantiation

4. First Request            ~0.5-1.0s
   └─ HttpClientFactory init (lazy)
   └─ SecretsProvider init (lazy)
   └─ Secrets Manager API call
   └─ Token parsing and cache

5. External API Call        Variable
   └─ Network latency to external API
   └─ API processing time

Total Cold Start: 2-3 seconds
```

### Warm Start Breakdown

```
Event: Lambda warm invocation (same container)

1. Handler invocation       ~5-10ms
   └─ JVM already running
   └─ Classes already loaded

2. Token cache lookup       <1ms
   └─ HashMap lookup
   └─ Expiry check

3. External API Call        Variable
   └─ Network latency
   └─ API processing time
   └─ Connection reused from pool

Total Warm Start: 50-100ms (plus external API latency)
```

### Token Refresh Scenario

```
Event: Token expired (after 55 minutes)

1. Cache check              <1ms
   └─ Token found but expired

2. Secrets Manager fetch    ~200-300ms
   └─ Lock acquisition
   └─ AWS API call
   └─ JSON parsing

3. Cache update             <1ms
4. External API call        Variable

Total with Refresh: ~200-300ms overhead
```

## Data Models

### ApiRequest (Optional)

```json
{
  "endpoint": "string",
  "method": "string",
  "payload": {}
}
```

### ApiResponse (Standard)

```json
{
  "statusCode": 200,
  "data": {},
  "error": null,
  "timestamp": 1735288245000
}
```

### Token Secret (Secrets Manager Format)

```json
{
  "token": "bearer_token_here"
}
```

## Thread Safety

### TokenCache

- **Thread-Safe**: Yes
- **Pattern**: Double-checked locking
- **Protection**: synchronized block around cache initialization
- **Impact**: Minimal contention on warm starts

### HttpClientFactory

- **Thread-Safe**: Yes
- **Pattern**: Double-checked locking
- **Protection**: synchronized block around client creation
- **Impact**: Single client instance shared across all threads

### ExternalApiClient

- **Thread-Safe**: Yes (stateless)
- **Impact**: Safe for concurrent invocations

### RetryConfigProvider

- **Thread-Safe**: Yes (immutable static final)
- **Impact**: No contention

## Cold Start Optimization Strategies

### 1. Lazy Initialization

```
Expensive resources initialized on first use:
├─ HttpClientFactory (connection pooling setup)
├─ SecretsManagerProvider (AWS SDK init)
└─ ExternalApiClient (optional)

Impact: ~500ms reduction in cold start
```

### 2. Static Configuration

```
Fast lookup at runtime:
├─ RetryConfigProvider (cached Retry instance)
├─ AppConfig (env vars read once)
└─ Handler implementation

Impact: Negligible startup overhead
```

### 3. Connection Pooling

```
Reduces cold start for subsequent API calls:
├─ Connection reused across invocations
├─ TLS handshake cached
└─ DNS lookup cached

Impact: ~50-100ms per subsequent call
```

### 4. Token Caching

```
Avoids Secrets Manager calls:
├─ 55-minute cache (conservative)
├─ In-memory storage
└─ Automatic expiry

Impact: ~200-300ms per 55-minute period
```

## Resilience Patterns

### Retry Logic (Exponential Backoff with Jitter)

```
Transient Failure
    ├─ Immediate retry (0-300ms)
    ├─ Second retry (300-1200ms)
    └─ Final retry (600-2400ms)

Only retries network/IO exceptions
Does NOT retry business logic errors
```

### Token Expiry Handling

```
Token Expiry
    ├─ Detected on cache check
    ├─ Synchronized refresh (prevents thundering herd)
    └─ Blocking for duration of refresh (~200ms)
```

### Error Handling

```
Error Flow
    ├─ Validation errors → Fail fast
    ├─ Network errors → Retry with backoff
    ├─ API errors → Log and return error response
    └─ Unknown errors → Generic error response
```

## Security Architecture

### Secrets Management

- Tokens stored in AWS Secrets Manager
- Retrieved via Powertools v2 Parameters
- Never logged or displayed
- Cached only in Lambda container memory
- Auto-rotated every 55 minutes

### Network Security

- External API calls over HTTPS
- No hardcoded credentials
- IAM role-based access control

### Logging Security

- JSON structured logs
- No sensitive data in logs
- CloudWatch Logs encryption at rest

### Access Control

- Lambda execution role with minimal permissions
- Secrets Manager read-only access
- CloudWatch Logs write access

## Deployment Architecture

### AWS Services

```
┌─────────────────┐
│  API Gateway    │ - REST API endpoint
└─────────────────┘
         │
┌─────────────────┐
│  Lambda (Java)  │ - Function execution
└─────────────────┘
         │
    ┌────┴────┬──────────┐
    ▼         ▼          ▼
┌────────┐ ┌──────────┐ ┌────────────┐
│Secrets │ │ External │ │ CloudWatch │
│Manager │ │   API    │ │   Logs     │
└────────┘ └──────────┘ └────────────┘
```

## Scalability

### Concurrency

- Lambda manages auto-scaling
- Each invocation gets isolated container
- No shared state issues (token cached per container)
- HTTP connection pool shared within container

### Performance at Scale

- First concurrent invocation: cold start (2-3s)
- Subsequent concurrent invocations: warm start (50-100ms)
- Token cache independent per container
- No central bottlenecks

## Monitoring and Observability

### Metrics

- Duration (execution time)
- Errors (failed invocations)
- Throttles (rate limits)
- Invocations (total calls)

### Logs

- JSON-formatted structured logs
- CloudWatch Logs integration
- Searchable fields:
    - timestamp
    - level (ERROR, WARN, INFO)
    - logger (component name)
    - message (log text)
    - exception (stack trace if present)

### Debugging

- Request IDs for tracing
- Detailed error messages
- Retry event logging
- Performance metrics

## Future Enhancements

### Possible Improvements

1. **Metrics Publishing**: Add CloudWatch custom metrics
2. **Distributed Tracing**: X-Ray integration
3. **Circuit Breaker**: Fallback patterns
4. **Caching**: Response caching beyond token
5. **Rate Limiting**: Request throttling
6. **Authentication**: API key management
7. **Versioning**: Multiple API versions support

