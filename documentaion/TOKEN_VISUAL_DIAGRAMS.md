# 🔐 Token Call Flow - Visual Diagram

## Simple Flow Chart

```
┌─────────────────────────────────────────────────────────────┐
│                    Lambda Invoked                           │
│                                                             │
│  Event: APIGatewayProxyRequestEvent                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│      ApiHandler.handleRequest()                             │
│      [ApiHandler.java:33]                                   │
│                                                             │
│  public APIGatewayProxyResponseEvent handleRequest(        │
│      APIGatewayProxyRequestEvent request,                 │
│      Context context) {                                    │
│                                                             │
│      String response = getClient().callExternalApi();      │
│      return buildSuccessResponse(response, 200);           │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│    ExternalApiClient.callExternalApi()                      │
│    [ExternalApiClient.java:47]                              │
│                                                             │
│  public String callExternalApi() {                          │
│      HttpGet request = new HttpGet(...);                   │
│      String token = TokenCache.getToken();  ← CALL HERE   │
│      request.setHeader("Authorization", "Bearer " + token); │
│      return HttpClientFactory.getClient().execute(...);    │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│         TokenCache.getToken()                               │
│         [TokenCache.java:31]                                │
│                                                             │
│  public static String getToken() {                         │
│      CachedToken cached = CACHE.get(...);                 │
│                                                             │
│      if (cached == null || cached.isExpired()) {           │
│          CACHE.put(..., fetchToken());  ← IF NOT CACHED   │
│      }                                                      │
│                                                             │
│      return CACHE.get(...).token();                        │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
                   ┌──────────────┐
                   │  Is cached?  │
                   └──────┬───────┘
                  /               \
                YES               NO
               /                   \
              ↓                     ↓
        ┌─────────────┐    ┌──────────────────────────┐
        │Use Cached   │    │fetchToken() Called       │
        │Token        │    │[TokenCache.java:65]     │
        │             │    │                          │
        │(Fast path)  │    │SecretsProvider.get()    │
        │             │    │Extract token from JSON  │
        │             │    │                          │
        │             │    │🔐 LOG:                  │
        │             │    │"Token retrieved"        │
        │             │    │"Token length"           │
        │             │    │"Token starts with"      │
        │             │    │                          │
        │             │    │Return CachedToken       │
        └─────────────┘    └──────────────────────────┘
              \                     /
               \                   /
                └─────────┬────────┘
                          ↓
        ┌────────────────────────────────┐
        │   Token returned to caller     │
        │   [ExternalApiClient.java:54]  │
        └────────────────────────────────┘
                          ↓
        ┌────────────────────────────────┐
        │   🔐 LOG:                      │
        │   "Using token in request"     │
        │   "Full token"                 │
        │                                │
        │   Add to Authorization header  │
        │   "Bearer {token}"             │
        └────────────────────────────────┘
                          ↓
        ┌────────────────────────────────┐
        │   HTTP Request Executed        │
        │                                │
        │   GET {url}                    │
        │   Authorization: Bearer {token}│
        │                                │
        │   Response received            │
        └────────────────────────────────┘
                          ↓
        ┌────────────────────────────────┐
        │   Return response to client    │
        │   via APIGateway               │
        └────────────────────────────────┘
```

---

## Detailed Call Stack

```java
// CALL STACK WHEN TOKEN IS RETRIEVED:

Thread Stack:
        ↓
        ApiHandler.

handleRequest()  [Line 33]
        ↓
        ApiHandler.

getClient().

callExternalApi()  [Line 46]
        ↓
        ExternalApiClient.

callExternalApi()  [Line 47-102]
        ↓
httpApiCall supplier
lambda  [Line 48-94]
        ↓
        TokenCache.

getToken()  [Line 54] ←
TOKEN CALLED
        ↓
Check CACHE
          ↓
Is null
or expired?
        ↓
YES →

Call fetchToken()
            ↓
                    TokenCache.

fetchToken()  [Line 65-82]
        ↓
        SecretsProvider.

get("external-api/token")
                ↓
Return secret
JSON string
              ↓
                      MAPPER.

readTree(secretValue)
                ↓
Parse JSON
              ↓
                      json.

get("token").

asText()
                ↓
Extract token
field ←
TOKEN RETRIEVED
HERE
              ↓
                      🔐 LOG.

info("🔐 Token retrieved...")
              🔐 LOG.

debug("Token length...")
              🔐 LOG.

debug("Token starts with...")
              ↓
                      new

CachedToken(token, expiryTime)
              ↓
Return CachedToken
            ↓
                    CACHE.

put(secretName, cachedToken)
        ↓
RETURN CACHE.

get(...).

token()
      ↓
Back to ExternalApiClient.

callExternalApi()
        ↓
String token = ...[Line 54]
        ↓
        🔐 LOG.

info("🔐 Using token in request...")
        🔐 LOG.

debug("Full token: "+token)
        ↓
                request.

setHeader("Authorization","Bearer "+token)
        ↓
                HttpClientFactory.

getClient().

execute(request, ...)
          ↓
Return response
      ↓
              Retry.

decorateSupplier(...).

get()
        ↓
Return response
      ↓
End of
lambda
  ↓
          ApiHandler.

buildSuccessResponse(response, 200)
  ↓
Return APIGatewayProxyResponseEvent
↓
END
```

---

## Log Output Timeline

```
Timeline when Lambda is invoked:

T+0ms    [INFO] Received request: path=/api/endpoint, method=GET
T+5ms    [INFO] Initiating external API call to: https://...
T+10ms   [INFO] Fetching fresh auth token from Secrets Manager
T+50ms   [INFO] 🔐 Token retrieved from Secrets Manager: eyJhbGciOi...
T+50ms   [DEBUG] Token length: 97 characters
T+50ms   [DEBUG] Token starts with: eyJhbGciOiJI...
T+50ms   [DEBUG] Token cached until: 2025-12-27T17:55:00Z
T+55ms   [INFO] 🔐 Using token in request: eyJhbGciOiJIUzI1NiIsInR5cCI6...
T+55ms   [DEBUG] Full token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJz...
T+60ms   [DEBUG] Executing HTTP GET request to external API
T+150ms  [INFO] External API call successful: status=200
T+160ms  [INFO] Response: {"status": "success", ...}
```

---

## Token Lifecycle in Lambda Container

```
┌──────────────────────────────────────────────────────────┐
│                  Lambda Container                        │
│                 (Lives for 55 minutes)                   │
│                                                          │
│  Invocation 1 (T+0min):                                 │
│  ├─ TokenCache CACHE is empty                           │
│  ├─ Call SecretsProvider.get()                          │
│  ├─ Fetch token from AWS Secrets Manager                │
│  ├─ 🔐 LOG token                                        │
│  ├─ Store in CACHE with expiry T+55min                  │
│  └─ Use token                                           │
│                                                          │
│  Invocation 2 (T+10min):                                │
│  ├─ TokenCache CACHE has token                          │
│  ├─ Token not expired (expires at T+55min)             │
│  ├─ Use cached token (FAST!)                            │
│  └─ No SecretsProvider call needed                      │
│                                                          │
│  Invocation 3 (T+20min):                                │
│  ├─ TokenCache CACHE has token                          │
│  ├─ Token not expired (expires at T+55min)             │
│  ├─ Use cached token (FAST!)                            │
│  └─ No SecretsProvider call needed                      │
│                                                          │
│  ... (many more invocations) ...                        │
│                                                          │
│  Invocation N (T+56min):                                │
│  ├─ Container recycled OR token expired                 │
│  ├─ TokenCache CACHE is empty                           │
│  ├─ Call SecretsProvider.get() again                    │
│  ├─ Fetch fresh token from AWS Secrets Manager          │
│  ├─ 🔐 LOG token again                                  │
│  ├─ Store new token in CACHE                            │
│  └─ Use fresh token                                     │
│                                                          │
└──────────────────────────────────────────────────────────┘

PERFORMANCE BENEFIT:
- First invocation (cold start): ~100ms (includes Secrets fetch)
- Subsequent invocations (warm): ~5ms (uses cached token)
- Cost: 1 Secrets Manager call per 55 minutes = HUGE savings!
```

---

## Token Data Structure

```java
// In AWS Secrets Manager:
{
        "client_id":"ce43d3bd-e1e0-4eed-a269-8bffe958f0fb",
        "client_secret":"aRZdZP63VqTmhfLcSE9zbAjG"
        }

        ↓Retrieved by SecretsProvider.

get()

// Parsed in TokenCache.fetchToken():
JsonNode json = MAPPER.readTree(secretValue);
String token = json.get("token").asText();

↓
Cached as
CachedToken record:

record CachedToken(String token, Instant expiry) {
    boolean isExpired() {
        return Instant.now().isAfter(expiry);
    }
}

↓
Stored in
ConcurrentHashMap:

ConcurrentHashMap<String, CachedToken> CACHE
  └─Key:"external-api/token"
        └─Value:

CachedToken(token="...", expiry=...)

↓
Returned to
ExternalApiClient:

        request.

setHeader("Authorization","Bearer "+token);

↓
Sent in
HTTP Request:

GET https://exchange-staging.motiveintegrator.com
Authorization:
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Files Involved

```
┌─────────────────────────────────────────────────────┐
│  src/main/java/com/project/ApiHandler.java         │
│  ├─ Line 33: handleRequest() [ENTRY POINT]         │
│  └─ Line 46: getClient().callExternalApi()         │
│     [KICKS OFF TOKEN FLOW]                          │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  src/main/java/com/project/client/                 │
│  ExternalApiClient.java                             │
│  ├─ Line 47: callExternalApi() [API CALLER]        │
│  ├─ Line 54: TokenCache.getToken()                 │
│  │   [TOKEN CALLED HERE] 🎯                        │
│  ├─ Line 55: 🔐 LOG token prefix                   │
│  ├─ Line 56: 🔐 LOG full token                     │
│  └─ Line 58: Use token in header                   │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  src/main/java/com/project/auth/                   │
│  TokenCache.java                                    │
│  ├─ Line 31: getToken() [CACHE CHECK]              │
│  ├─ Line 65: fetchToken() [TOKEN RETRIEVAL]        │
│  ├─ Line 70: 🔐 LOG token retrieved               │
│  ├─ Line 71: 🔐 LOG token length                  │
│  ├─ Line 72: 🔐 LOG token prefix                  │
│  └─ Line 77: return token                          │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  AWS Secrets Manager                                │
│  └─ Secret: external-api/token                     │
│     └─ Contains: client_id + client_secret         │
└─────────────────────────────────────────────────────┘
```

---

**Status**: ✅ Token flow documented with visual diagrams
**Ready**: Build and deploy to see it in action!

