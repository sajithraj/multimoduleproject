# 🎯 COMPLETE GUIDE: Lambda Token Caching Implementation

## Date: December 28, 2025

---

## 📚 Table of Contents

1. [How Token Caching Works](#how-token-caching-works)
2. [Lambda Serverless Architecture](#lambda-serverless-architecture)
3. [Multi-Tenant Cache Safety](#multi-tenant-cache-safety)
4. [Cache Lifecycle](#cache-lifecycle)
5. [Testing Guide](#testing-guide)
6. [Troubleshooting](#troubleshooting)

---

## 🔍 How Token Caching Works

### The Implementation

```java
public class SSMApigeeProvider {
    
    // Static cache - survives Lambda warm invocations
    private static final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    
    // Cache TTL: 55 minutes
    private static final long CACHE_TTL_MILLIS = 3300 * 1000L;
    
    public String getValue(String secretKey) {
        String key = (secretKey == null || secretKey.trim().isEmpty()) 
                ? TOKEN_SECRET_NAME : secretKey;
        
        // Step 1: Check cache
        CachedToken cachedToken = tokenCache.get(key);
        
        // Step 2: If found and not expired, return cached token
        if (cachedToken != null && !cachedToken.isExpired()) {
            return cachedToken.token;  // ← CACHE HIT! No API call!
        }
        
        // Step 3: Cache miss - fetch fresh token
        String secretValue = getSecretFromSecretsManager(key);
        String token = transformer.applyTransformation(secretValue, String.class);
        
        // Step 4: Cache the token
        tokenCache.put(key, new CachedToken(token, System.currentTimeMillis()));
        
        return token;
    }
    
    private static class CachedToken {
        final String token;
        final long cachedAt;
        
        boolean isExpired() {
            return (System.currentTimeMillis() - cachedAt) > CACHE_TTL_MILLIS;
        }
    }
}
```

### Key Points:

1. **`static` keyword** - The cache survives across invocations in the same container
2. **`ConcurrentHashMap`** - Thread-safe for concurrent requests
3. **Cache key** - Secret name (e.g., "external-api/token")
4. **Cache value** - Bearer token + timestamp
5. **TTL check** - Every access checks if expired

---

## 🏗️ Lambda Serverless Architecture

### How Lambda Containers Work:

```
┌────────────────────────────────────────────────────────────────────────┐
│  AWS Lambda Service                                                    │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │  Container 1 (Instance A)                                        │ │
│  │  ┌────────────────────────────────────────────────────────────┐ │ │
│  │  │  JVM Instance                                              │ │ │
│  │  │  ├─ SSMApigeeProvider (singleton)                         │ │ │
│  │  │  │  └─ static tokenCache (HashMap)                        │ │ │
│  │  │  │     ├─ "external-api/token" → CachedToken(token1)     │ │ │
│  │  │  │     └─ "another-secret" → CachedToken(token2)         │ │ │
│  │  │  └─ Handles Requests 1, 2, 3, 4...                       │ │ │
│  │  └────────────────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │  Container 2 (Instance B) - Separate instance!                  │ │
│  │  ┌────────────────────────────────────────────────────────────┐ │ │
│  │  │  JVM Instance                                              │ │ │
│  │  │  ├─ SSMApigeeProvider (different singleton)               │ │ │
│  │  │  │  └─ static tokenCache (different HashMap)              │ │ │
│  │  │  │     └─ Initially EMPTY!                                │ │ │
│  │  │  └─ Handles Requests 5, 6, 7...                           │ │ │
│  │  └────────────────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Important Concepts:

#### 1. **Container Lifecycle**

```
COLD START (New Container)
├─ Load Lambda code (JAR)
├─ Initialize JVM
├─ Initialize static variables (tokenCache = new ConcurrentHashMap<>())
├─ Execute request
└─ Container stays WARM for ~15 minutes of inactivity

WARM INVOCATION (Reuse Container)
├─ Skip initialization (JVM already running)
├─ Static variables still in memory (tokenCache still has data!)
├─ Execute request
└─ Cache persists!

COLD START (New Container after idle)
├─ Previous container was killed
├─ New JVM instance
├─ tokenCache starts EMPTY again
└─ Must fetch fresh token
```

#### 2. **Why `static` is Critical**

```java
// ❌ WITHOUT static - cache lost between invocations
private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
// This would be recreated on EVERY invocation!

// ✅ WITH static - cache persists in warm container
private static final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
// This persists across invocations in the same container!
```

**Why it works:**

- `static` variables belong to the **class**, not the instance
- Lambda reuses the **same JVM** for warm invocations
- Static variables remain in memory between invocations

#### 3. **Container Isolation**

```
Request Flow:

Request 1 → Container A → Cache Miss → Fetch Token → Cache it
Request 2 → Container A → Cache Hit! ✅ (same container)
Request 3 → Container A → Cache Hit! ✅ (same container)

High Traffic - AWS Scales Out:

Request 4 → Container B → Cache Miss! (new container, empty cache)
Request 5 → Container A → Cache Hit! ✅ (original container still alive)
Request 6 → Container B → Cache Hit! ✅ (container B now has cache)
```

**Each container has its own cache** - they don't share!

---

## 🔐 Multi-Tenant Cache Safety

### Your Question:

> "If other lambda uses with diff secret key will that make any prob?"

### Answer: **NO - It's Safe!** ✅

Here's why:

#### Cache Key Structure:

```java
String key = (secretKey == null || secretKey.trim().isEmpty())
        ? TOKEN_SECRET_NAME : secretKey;

tokenCache.

put(key, new CachedToken(token, timestamp));
```

**Cache stores:**

```
Key                          → Value
─────────────────────────────────────────────────────────────────
"external-api/token"         → CachedToken("Bearer eyJhbGci...")
"another-api/credentials"    → CachedToken("Bearer xyzAbc123...")
"service-b/oauth"            → CachedToken("Bearer qwerty789...")
```

**Each secret name = separate cache entry!**

#### Example Scenario:

```java
// Lambda Function 1 (Service A)
System.setenv("TOKEN_SECRET_NAME","service-a/token");
provider.

getValue(null);  // Uses "service-a/token" as cache key

// Lambda Function 2 (Service B)
System.

setenv("TOKEN_SECRET_NAME","service-b/token");
provider.

getValue(null);  // Uses "service-b/token" as cache key

// NO CONFLICT! Different keys = different cache entries
```

#### Thread Safety:

```java
ConcurrentHashMap<String, CachedToken> tokenCache
```

**Benefits:**

- ✅ Multiple threads can read simultaneously
- ✅ Writes are atomic (no corruption)
- ✅ No race conditions
- ✅ Safe for high concurrency

---

## ⏰ Cache Lifecycle

### Detailed Timeline:

```
┌─────────────────────────────────────────────────────────────────────┐
│  Container Lifecycle                                                │
└─────────────────────────────────────────────────────────────────────┘

00:00:00 - Container Cold Start (Request 1)
├─ JVM starts
├─ static tokenCache = new ConcurrentHashMap<>() (EMPTY)
├─ Request arrives
├─ tokenCache.get("external-api/token") → null (cache miss)
├─ Fetch token from OAuth2 API (1500 ms)
├─ tokenCache.put("external-api/token", CachedToken(token, 00:00:00))
└─ Response: 4368 ms

00:00:30 - Request 2 (30 seconds later, SAME container)
├─ JVM still running (warm)
├─ tokenCache still has data ✅
├─ tokenCache.get("external-api/token") → CachedToken found!
├─ Check: isExpired()? → No (only 30 seconds old)
├─ Return cached token (no OAuth2 API call!)
└─ Response: ~100 ms (96% faster!)

00:10:00 - Request 3 (10 minutes later, SAME container)
├─ tokenCache.get("external-api/token") → CachedToken found!
├─ Check: isExpired()? → No (only 10 minutes old, TTL is 55 min)
├─ Return cached token
└─ Response: ~100 ms

00:54:00 - Request 4 (54 minutes later, SAME container)
├─ tokenCache.get("external-api/token") → CachedToken found!
├─ Check: isExpired()? → No (54 minutes < 55 minute TTL)
├─ Return cached token
└─ Response: ~100 ms

00:56:00 - Request 5 (56 minutes later, SAME container)
├─ tokenCache.get("external-api/token") → CachedToken found!
├─ Check: isExpired()? → YES! (56 minutes > 55 minute TTL)
├─ Fetch NEW token from OAuth2 API (1500 ms)
├─ tokenCache.put("external-api/token", CachedToken(newToken, 00:56:00))
└─ Response: ~2500 ms (cache refresh)

01:00:00 - Request 6 (4 minutes later, SAME container)
├─ tokenCache.get("external-api/token") → NEW CachedToken found!
├─ Check: isExpired()? → No (only 4 minutes old)
├─ Return cached token
└─ Response: ~100 ms

[15 minutes of no requests]

01:15:00 - Container KILLED (idle timeout)
├─ JVM shuts down
├─ tokenCache is garbage collected
└─ Container removed from pool

01:16:00 - Request 7 (NEW container)
├─ COLD START again
├─ New JVM, tokenCache starts EMPTY
├─ Must fetch fresh token
└─ Response: ~4368 ms
```

### Key Insights:

1. **Cache persists ONLY within a container**
2. **Container can stay alive for hours** (if there's traffic)
3. **Cache survives for 55 minutes** (then refreshes)
4. **Container dies after ~15 min idle** (AWS kills it)
5. **Each new container starts with EMPTY cache**

---

## 🧪 Testing Guide

### Setup LocalStack First:

```powershell
# Set environment variables
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Test connection
aws --endpoint-url=http://localhost:4566 lambda list-functions
```

### Test 1: Cache Miss (First Call)

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  response1.json

Get-Content response1.json | ConvertFrom-Json
```

**Expected:**

- Duration: ~4000-5000 ms (cold start)
- Logs: "No cached token found, fetching fresh token"

### Test 2: Cache Hit (Second Call)

```powershell
Start-Sleep -Seconds 2

aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  response2.json

Get-Content response2.json | ConvertFrom-Json
```

**Expected:**

- Duration: ~100-500 ms (96% faster!)
- Logs: "OAuth2 bearer token retrieved from CACHE (age: X seconds)"

### Test 3: Multiple Rapid Calls (Cache Persistence)

```powershell
# Test cache persistence with 10 rapid calls
for ($i=1; $i -le 10; $i++) {
    Write-Host "Call $i..." -NoNewline
    $start = Get-Date
    
    aws --endpoint-url=http://localhost:4566 lambda invoke `
      --function-name my-token-auth-lambda `
      --payload '{"body":"{}"}' `
      response_$i.json 2>&1 | Out-Null
    
    $duration = (Get-Date) - $start
    Write-Host " Duration: $($duration.TotalMilliseconds) ms"
    
    Start-Sleep -Milliseconds 500
}
```

**Expected:**

- Call 1: ~4000 ms (cold start or cache miss)
- Calls 2-10: ~100-500 ms each (all cache hits!)

### Test 4: Check Logs

```powershell
aws --endpoint-url=http://localhost:4566 logs tail `
  /aws/lambda/my-token-auth-lambda `
  --since 5m `
  --format short
```

**Look for:**

```
INFO OAuth2 bearer token retrieved from CACHE (age: 30 seconds, remaining TTL: 3270 seconds)
```

---

## 🐛 Troubleshooting

### Issue 1: "UnrecognizedClientException"

**Error:**

```
An error occurred (UnrecognizedClientException) when calling the Invoke operation: 
The security token included in the request is invalid.
```

**Cause:** Missing `--endpoint-url=http://localhost:4566` for LocalStack

**Fix:**

```powershell
# ❌ WRONG - tries to call real AWS
aws lambda invoke --function-name my-token-auth-lambda ...

# ✅ CORRECT - calls LocalStack
aws --endpoint-url=http://localhost:4566 lambda invoke --function-name my-token-auth-lambda ...
```

### Issue 2: Cache Not Working

**Symptoms:**

- Every call shows "No cached token found"
- All calls take 2-4 seconds

**Possible Causes:**

1. **Container is being recycled**
   ```
   Solution: Check if Lambda memory/timeout is too low
   ```

2. **Cache is expiring too quickly**
   ```java
   // Check CACHE_TTL_MILLIS value
   private static final long CACHE_TTL_MILLIS = 3300 * 1000L; // Should be 3300000
   ```

3. **Different cache keys**
   ```
   Solution: Check logs for actual cache key being used
   ```

### Issue 3: Cache Shared Between Different Secrets?

**Concern:**
> "Will different secrets interfere with each other?"

**Answer:** NO! Each secret has its own cache entry.

**Verification:**

```java
// Check logs - cache key should match secret name
LOG.info("Cache key: {}", key);

// Example:
// Cache key: external-api/token
// Cache key: service-b/credentials
```

---

## 📊 Performance Expectations

### Baseline (No Caching):

| Call Type       | Duration     | Why?                             |
|-----------------|--------------|----------------------------------|
| Cold Start      | 4000-5000 ms | Load JVM + classes + fetch token |
| Warm (no cache) | 1200-2500 ms | Connection pooling helps         |

### With Token Caching:

| Call Type            | Duration       | Why?              |
|----------------------|----------------|-------------------|
| Cold Start           | 4000-5000 ms   | Same (first time) |
| Warm - Cache Hit     | **100-500 ms** | No OAuth2 call! ✅ |
| Warm - Cache Expired | 1200-2500 ms   | Refresh token     |

**Improvement: 80-96% faster on cache hits!**

---

## 🎯 Quick Commands Reference

### Deploy:

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

# Build
mvn clean package -DskipTests

# Deploy
cd infra\terraform
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
terraform apply -var-file="terraform.localstack.tfvars" -auto-approve
```

### Test:

```powershell
# Single invocation
aws --endpoint-url=http://localhost:4566 lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"body":"{}"}' `
  response.json

# View response
Get-Content response.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# View logs
aws --endpoint-url=http://localhost:4566 logs tail `
  /aws/lambda/my-token-auth-lambda `
  --since 5m
```

### Monitor Cache:

```powershell
# Look for cache hit/miss in logs
aws --endpoint-url=http://localhost:4566 logs tail `
  /aws/lambda/my-token-auth-lambda `
  --since 5m `
  --format short | Select-String -Pattern "CACHE"
```

---

## ✅ Summary

### How It Works:

1. **Static `ConcurrentHashMap`** stores tokens in memory
2. **Cache survives** across warm invocations
3. **Each container** has its own cache (isolated)
4. **Each secret name** has its own cache entry (no conflicts)
5. **55-minute TTL** with automatic expiration check
6. **Thread-safe** for concurrent requests

### When Cache is Lost:

- Container dies (idle >15 min)
- Lambda function redeployed
- Container manually killed by AWS

### When Cache is Kept:

- Warm invocations (most common)
- High traffic (container stays alive)
- Within TTL period (55 minutes)

### Is It Safe?

- ✅ Thread-safe (`ConcurrentHashMap`)
- ✅ Multi-tenant safe (different cache keys)
- ✅ Auto-expiring (time-based TTL)
- ✅ Container-isolated (no cross-pollution)

---

**Your caching implementation is PRODUCTION-READY!** 🚀

