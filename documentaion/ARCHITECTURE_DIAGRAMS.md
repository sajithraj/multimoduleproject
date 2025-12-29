# System Architecture Diagrams

## 1. High-Level Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    AWS Lambda Function                          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  API Gateway Event Handler                              │  │
│  │  (Main.java / ApiIntegrationExample.java)              │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │  AuthenticatedApiClient                                 │  │
│  │  - Builds HTTP requests                                 │  │
│  │  - Injects Bearer tokens                                │  │
│  │  - Handles retries (max 3)                              │  │
│  │  - Parses responses                                     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │  TokenAuthorizationService                              │  │
│  │  - Orchestrates token lifecycle                         │  │
│  │  - Caches tokens (55 min)                               │  │
│  │  - Requests new tokens when needed                      │  │
│  │  - Validates tokens                                     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│       ┌───────────────┴───────────────┐                         │
│       │                               │                         │
│  ┌────▼──────────────────┐  ┌────────▼──────────────────────┐  │
│  │  TokenCache           │  │  TokenAuthorizationUtil      │  │
│  │  (Lambda Memory)      │  │  (HTTP Client)               │  │
│  │                       │  │                              │  │
│  │  - Fast lookups       │  │  - Form encoding             │  │
│  │  - 55 min expiry      │  │  - JSON parsing              │  │
│  │  - Thread-safe        │  │  - Retry logic               │  │
│  │  - ConcurrentHashMap  │  │  - Error handling            │  │
│  └───────────┬───────────┘  └────────┬───────────────────────┘  │
│              │                       │                          │
│              │ (cached)              │ (fetch)                  │
│              └───────────┬───────────┘                          │
│                          │                                      │
│  ┌───────────────────────▼───────────────────────────────────┐ │
│  │  AppConfig & Secrets Manager Integration                 │ │
│  │  - TOKEN_ENDPOINT_URL                                    │ │
│  │  - CLIENT_ID / CLIENT_SECRET                             │ │
│  │  - Powertools SecretsProvider                            │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                │
└─────────────────────────┬──────────────────────────────────────┘
                          │
              ┌───────────┴────────────┐
              │                        │
          ┌───▼──────────────┐   ┌─────▼──────────────┐
          │ Token Endpoint   │   │ Data API Endpoint  │
          │ /v1/authorize/   │   │ /v1/data/...       │
          │   token          │   │                    │
          └──────────────────┘   └────────────────────┘
```

## 2. Token Authorization Sequence Diagram

```
┌────────┐     ┌──────────────────┐     ┌──────────┐     ┌──────────────┐     ┌───────────┐
│Lambda  │     │TokenAuthService  │     │Token     │     │Secrets       │     │External   │
│Handler │     │                  │     │Cache     │     │Manager       │     │API        │
└────┬───┘     └────────┬─────────┘     └────┬─────┘     └──────┬───────┘     └─────┬─────┘
     │                  │                     │                  │                    │
     │ getAccessToken() │                     │                  │                    │
     ├─────────────────►│                     │                  │                    │
     │                  │ getToken()          │                  │                    │
     │                  ├────────────────────►│                  │                    │
     │                  │                     │                  │                    │
     │                  │  Token exists       │                  │                    │
     │                  │  & valid?           │                  │                    │
     │                  │  ┌─ YES ────────────├─────────────────►│ Return token      │
     │                  │  │                  │                  │                    │
     │                  │  └─ NO (expired)    │                  │                    │
     │                  │                     │                  │                    │
     │                  │ requestNewToken()   │                  │                    │
     │                  ├────────────────────────────────────────┤                    │
     │                  │                     │  getSecret()     │                    │
     │                  │                     │◄─────────────────┤                    │
     │                  │                     │                  │ Retrieve Creds    │
     │                  │◄────────────────────────────────────────┤                    │
     │                  │                     │                  │                    │
     │                  │ requestToken()      │                  │                    │
     │                  ├────────────────────────────────────────────────────────────►│
     │                  │                     │                  │                    │
     │                  │ (retry up to 3x)    │                  │                    │
     │                  │                     │                  │                    │
     │                  │◄────────────────────────────────────────────────────────────┤
     │                  │                     │ TokenAuthResponse                      │
     │                  │                     │                  │                    │
     │                  │ Validate & Parse    │                  │                    │
     │                  │                     │                  │                    │
     │                  │ put(token, expiry)  │                  │                    │
     │                  ├────────────────────►│                  │                    │
     │                  │                     │                  │                    │
     │◄─────────────────┤ token string        │                  │                    │
     │                  │◄────────────────────┤                  │                    │
     │                  │                     │                  │                    │
     │ Token ready      │                     │                  │                    │
     │ (for API call)   │                     │                  │                    │
```

## 3. API Call with Authentication Flow

```
┌────────┐     ┌──────────────────┐     ┌──────────────┐     ┌──────────────┐     ┌───────────┐
│Lambda  │     │Authenticated     │     │Token Auth    │     │HTTP Client   │     │External   │
│Handler │     │ApiClient         │     │Service       │     │(HttpClient5) │     │API        │
└────┬───┘     └────────┬─────────┘     └──────┬───────┘     └──────┬───────┘     └─────┬─────┘
     │                  │                      │                    │                    │
     │ callApi(request) │                      │                    │                    │
     ├─────────────────►│                      │                    │                    │
     │                  │ getAccessToken()     │                    │                    │
     │                  ├─────────────────────►│                    │                    │
     │                  │◄─────────────────────┤ token              │                    │
     │                  │                      │                    │                    │
     │                  │ buildHttpRequest()   │                    │                    │
     │                  │ + Authorization:     │                    │                    │
     │                  │   Bearer {token}     │                    │                    │
     │                  │                      │                    │                    │
     │                  │ executeRequest()     │                    │                    │
     │                  ├────────────────────────────────────────►  │                    │
     │                  │                      │                    │ HTTP Request        │
     │                  │                      │                    ├───────────────────►│
     │                  │                      │                    │                    │
     │                  │  (retry up to 3x     │                    │                    │
     │                  │   on 5xx errors)     │                    │                    │
     │                  │                      │                    │◄───────────────────┤
     │                  │                      │                    │ HTTP Response       │
     │                  │◄────────────────────────────────────────  │                    │
     │                  │                      │                    │                    │
     │                  │ parseResponse()      │                    │                    │
     │                  │ (Jackson ObjectMapper)                    │                    │
     │                  │                      │                    │                    │
     │◄─────────────────┤ ExternalApiResponse  │                    │                    │
     │ (status, data)   │◄─────────────────────┤                    │                    │
     │                  │                      │                    │                    │
```

## 4. Error Handling Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                      API Call Attempt                            │
└──────────────────────┬───────────────────────────────────────────┘
                       │
            ┌──────────▼──────────┐
            │  Execute Request    │
            └──────────┬──────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
    Success        Retry?         Final
    (2xx)         (5xx)          Error
       │              │              │
       │         ┌─────▼──────┐      │
       │         │ Attempt 1  │      │
       │         └─────┬──────┘      │
       │              │              │
       │         ┌─────▼──────┐      │
       │         │ Attempt 2  │      │
       │         └─────┬──────┘      │
       │              │              │
       │         ┌─────▼──────┐      │
       │         │ Attempt 3  │      │
       │         └─────┬──────┘      │
       │              │              │
       │ ┌────────────┼──────────┐   │
       │ │                       │   │
       │ ▼                       ▼   ▼
    Return                  ExternalApiException
    Response                - Token auth failed
    (success)               - API call failed
                            - Invalid response
                            - Network error
                            - Timeout
                            - Max retries exceeded
```

## 5. Token Cache Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│              Token Cache Lifecycle                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
         ┌────────────────────────┐
         │  Token Request Made    │
         │  (via API call)        │
         └────────┬───────────────┘
                  │
                  ▼
         ┌────────────────────────┐
         │  Parse Response        │
         │  (extract JWT token)   │
         └────────┬───────────────┘
                  │
                  ▼
         ┌────────────────────────┐
         │  Store in Cache        │
         │  (ConcurrentHashMap)   │
         └────────┬───────────────┘
                  │
                  ▼
         ┌────────────────────────┐
         │  Set Expiry Timer      │
         │  55 minutes from now   │
         │  (conservative vs 60)  │
         └────────┬───────────────┘
                  │
    ┌─────────────┴────────────┐
    │                          │
    ▼                          ▼
Within Expiry          Expiry Reached
    │                          │
    ├─ Return token ───────┐   │
    │   (0 API calls)      │   │
    │                      │   ├─ Remove from cache
    │  ┌──────────────────►│   │
    │  │                   │   ├─ Request new token
    │  │ Cache hits (fast) │   │
    │  │                   │   └─ Restart cycle
    │  └──────────────────┘
    │
    └─ No latency impact
    └─ Reduces cold start
    └─ Improves performance
```

## 6. Retry Strategy with Exponential Backoff

```
        ┌─────────────────────────┐
        │  API Call Fails         │
        │  (5xx error)            │
        └────────┬────────────────┘
                 │
                 ▼
        ┌─────────────────────────┐
        │  Attempt 1              │
        │  Wait: 1s (initial)     │
        └────────┬────────────────┘
                 │
        ┌────────▼────────┐
        │  Fail again?    │
        │  (5xx)          │
        └────────┬────────┘
                 │ YES
                 ▼
        ┌─────────────────────────┐
        │  Attempt 2              │
        │  Wait: 2s               │
        │  (exponential backoff)  │
        └────────┬────────────────┘
                 │
        ┌────────▼────────┐
        │  Fail again?    │
        │  (5xx)          │
        └────────┬────────┘
                 │ YES
                 ▼
        ┌─────────────────────────┐
        │  Attempt 3              │
        │  Wait: 4s               │
        │  (exponential backoff)  │
        └────────┬────────────────┘
                 │
        ┌────────▼────────┐
        │  Fail again?    │
        └────────┬────────┘
                 │ YES
                 ▼
        ┌─────────────────────────┐
        │  All Retries Exhausted  │
        │  Throw Exception        │
        └─────────────────────────┘

Benefits:
- Prevents thundering herd
- Gives server time to recover
- Max impact: 7 seconds (1+2+4)
- 4xx errors: no retry (fast fail)
```

## 7. Data Model Relationships

```
┌──────────────────────────────────────────────────────────────┐
│                   Request Phase                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────┐                                    │
│  │ TokenAuthRequest    │                                    │
│  ├─────────────────────┤                                    │
│  │ - grantType         │────► "client_credentials"         │
│  │ - clientId          │────► From AppConfig               │
│  │ - clientSecret      │────► From Secrets Manager          │
│  │ - scope (optional)  │────► Custom scope                 │
│  └─────────────────────┘                                    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                         │
                         │ POST (form-encoded)
                         ▼
┌──────────────────────────────────────────────────────────────┐
│              External API Token Endpoint                      │
│        /v1/authorize/token                                   │
└──────────────────────────────────────────────────────────────┘
                         │
                         │ JSON Response
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                   Response Phase                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐                                   │
│  │ TokenAuthResponse    │                                   │
│  ├──────────────────────┤                                   │
│  │ - accessToken        │────► JWT (cached)                │
│  │ - tokenType          │────► "Bearer"                    │
│  │ - expiresIn          │────► 14400 seconds               │
│  └──────────────────────┘                                   │
│          │                                                   │
│          └──► TokenCache (55 min expiry)                   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                         │
                         │ Get token from cache
                         ▼
┌──────────────────────────────────────────────────────────────┐
│              Authenticated API Calls                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐                                   │
│  │ ExternalApiRequest   │                                   │
│  ├──────────────────────┤                                   │
│  │ - endpoint           │────► "/v1/data/..."              │
│  │ - method             │────► GET, POST, PUT               │
│  │ - headers            │────► Custom headers               │
│  │ - body               │────► JSON payload                │
│  │ - queryParams        │────► Query string params          │
│  └──────────────────────┘                                   │
│          │                                                   │
│          └──► + Authorization: Bearer {token}               │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                         │
                         │ HTTP Request with token
                         ▼
┌──────────────────────────────────────────────────────────────┐
│              External API Data Endpoints                      │
│        /v1/data/... (various endpoints)                      │
└──────────────────────────────────────────────────────────────┘
                         │
                         │ JSON Response
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                   Response Phase                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────┐                               │
│  │ ExternalApiResponse      │                               │
│  ├──────────────────────────┤                               │
│  │ - statusCode             │────► HTTP status              │
│  │ - status                 │────► "success" or "error"     │
│  │ - data                   │────► Response payload         │
│  │ - error                  │────► Error message            │
│  │ - errorCode              │────► Error code               │
│  │ - timestamp              │────► Response timestamp       │
│  └──────────────────────────┘                               │
│          │                                                   │
│          └──► Helper methods: isSuccess(), isError()        │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## 8. Cold Start Optimization Strategy

```
┌──────────────────────────────────────────────────────┐
│  Lambda Cold Start Timeline                          │
├──────────────────────────────────────────────────────┤
│                                                      │
│  0-500ms  ┌─────────────────────────────────────┐  │
│           │ 1. Lambda Runtime Initialization    │  │
│           │    (AWS managed)                    │  │
│           └─────────────────────────────────────┘  │
│                                                      │
│  500-1000ms ┌─────────────────────────────────────┐ │
│            │ 2. JVM Startup & Class Loading     │ │
│            │    (static initializers)            │ │
│            └─────────────────────────────────────┘ │
│                                                     │
│  1000-1500ms ┌──────────────────────────────────┐  │
│             │ 3. Lazy Initialization ✓          │  │
│             │    - Powertools init (deferred)   │  │
│             │    - HTTP client (reused)         │  │
│             │    - Token cache (check only)     │  │
│             └──────────────────────────────────┘  │
│                                                     │
│  1500-2000ms ┌──────────────────────────────────┐  │
│             │ 4. Token Authorization (if needed)│  │
│             │    - Check cache (fast)           │  │
│             │    - Or request new (slower)      │  │
│             └──────────────────────────────────┘  │
│                                                     │
│  2000-3000ms ┌──────────────────────────────────┐  │
│             │ 5. API Call Execution            │  │
│             │    - With or without retry        │  │
│             └──────────────────────────────────┘  │
│                                                     │
│  3000-5000ms ┌──────────────────────────────────┐  │
│             │ 6. Response Processing           │  │
│             │    - Parse JSON                  │  │
│             │    - Return to caller            │  │
│             └──────────────────────────────────┘  │
│                                                     │
├──────────────────────────────────────────────────────┤
│  OPTIMIZATION POINTS:                              │
│  ✓ Lazy init reduces startup time                 │
│  ✓ Token cache hit avoids API call                │
│  ✓ Connection reuse improves speed                │
│  ✓ Minimal dependencies loaded                    │
│  ✓ No unnecessary logging at startup              │
└──────────────────────────────────────────────────────┘
```

---

**All diagrams represent the production architecture with Powertools v2.8.0**

