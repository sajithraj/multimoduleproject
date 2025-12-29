# ApigeeSecretsProvider - Powertools v1 to v2 Migration Comparison

## Date: December 28, 2025

## Overview

Successfully migrated `SSMApigeeProvider` from AWS Lambda Powertools v1 to v2, creating `ApigeeSecretsProvider` without
any manual caching (ConcurrentHashMap) implementation.

---

## Key Differences Between Old and New Implementation

### **Old Implementation (Powertools v1 - SSMApigeeProvider)**

```java
public class SSMApigeeProvider extends BaseProvider {
    private final SecretsManagerClient client;

    public SSMApigeeProvider(CacheManager cacheManager,
                             SecretsManagerClient client,
                             TransformationManager transformationManager) {
        super(cacheManager);  // ← Caching handled by BaseProvider
        this.client = client;
        super.defaultMaxAge(3600, ChronoUnit.SECONDS);
        super.setTransformationManager(transformationManager);
        super.withTransformation(ApigeeBearerTransformer.class);
    }
}
```

**Key Features:**

- ✅ Extends `BaseProvider` (Powertools v1)
- ✅ Uses `CacheManager` from Powertools v1
- ✅ Uses `TransformationManager` from Powertools v1
- ✅ Caching automatically handled by framework
- ✅ No manual cache implementation (no ConcurrentHashMap)

---

### **New Implementation (Powertools v2 - ApigeeSecretsProvider)**

```java
public class ApigeeSecretsProvider {
    private final SecretsManagerClient client;
    private final ApigeeBearerTransformer transformer;

    private ApigeeSecretsProvider(Builder builder) {
        this.client = builder.client != null ? builder.client : createDefaultClient();
        this.transformer = builder.transformer != null ? builder.transformer : new ApigeeBearerTransformer();
        // NO cache implementation here - caller handles caching if needed
    }

    public String getValue(String secretKey) {
        String key = (secretKey == null || secretKey.trim().isEmpty()) ? TOKEN_SECRET_NAME : secretKey;
        String secretValue = getSecretFromSecretsManager(key);
        return transformer.applyTransformation(secretValue, String.class);
    }
}
```

**Key Features:**

- ✅ No extension of BaseProvider (removed in v2)
- ✅ Direct SecretsManager client usage
- ✅ Direct ApigeeBearerTransformer usage (no TransformationManager)
- ✅ **No manual caching implementation (no ConcurrentHashMap)**
- ✅ Simple, stateless design - caching should be handled at higher level if needed

---

## What Was REMOVED from the Initial (Incorrect) Implementation

### ❌ Removed: Manual Cache Implementation

```java
// REMOVED - This was NOT in the original SSMApigeeProvider
private final ConcurrentHashMap<String, CachedValue> cache;
private final long defaultMaxAgeSeconds;

private static class CachedValue {
    private final String value;
    private final Instant expiry;
    // ...
}
```

### ❌ Removed: Cache-related Methods

```java
// REMOVED - These were NOT in the original SSMApigeeProvider
public String getValue(String secretKey, int maxAgeSeconds) { ...}

public void clearCache(String secretKey) { ...}

public void clearAllCache() { ...}
```

---

## What Was KEPT from the Original Implementation

### ✅ Kept: Core Functionality

1. **SecretsManager Integration**: Direct integration with AWS Secrets Manager
2. **ApigeeBearerTransformer**: Token transformation logic
3. **Builder Pattern**: Flexible configuration via builder
4. **Environment Variables**:
    - `TOKEN_ENDPOINT_URL` (OAuth2 token endpoint)
    - `TOKEN_SECRET_NAME` (Secrets Manager secret name)
5. **resetToDefaults()**: No-op method kept for API compatibility

### ✅ Kept: Client Configuration

```java
private static SecretsManagerClient createDefaultClient() {
    return SecretsManagerClient.builder()
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .region(Region.of(
                    System.getenv().getOrDefault(
                            SdkSystemSetting.AWS_REGION.environmentVariable(),
                            "us-east-1"
                    )))
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .putAdvancedOption(
                            SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                            "powertools-parameters")
                    .build())
            .build();
}
```

---

## Migration Highlights

| Aspect             | Powertools v1                    | Powertools v2                    |
|--------------------|----------------------------------|----------------------------------|
| **Base Class**     | Extends `BaseProvider`           | Standalone class                 |
| **Cache Handling** | Framework (`CacheManager`)       | Caller's responsibility          |
| **Transformer**    | `TransformationManager`          | Direct `ApigeeBearerTransformer` |
| **Manual Cache**   | ❌ None                           | ❌ None (correctly removed)       |
| **State**          | Stateful (cache in BaseProvider) | Stateless                        |
| **Complexity**     | Framework-dependent              | Simple, direct                   |

---

## Usage Example

```java
// Create provider
ApigeeSecretsProvider provider = ApigeeSecretsProvider.builder()
                .build();

// Get bearer token (no internal caching)
String bearerToken = provider.getValue(null); // Uses TOKEN_SECRET_NAME from env

// OR with specific secret name
String bearerToken = provider.getValue("my-custom-secret");
```

---

## Environment Variables Required

```properties
TOKEN_ENDPOINT_URL=https://your-oauth2-endpoint.com/token
TOKEN_SECRET_NAME=my-oauth2-credentials
AWS_REGION=us-east-1
OAUTH2_TIMEOUT_SECONDS=10  # Optional, defaults to 10
```

---

## Summary of Changes

### ✅ What We Fixed

1. **Removed manual caching** (ConcurrentHashMap, CachedValue class) - This was NOT in the original
2. **Removed cache-related methods** (clearCache, clearAllCache, getValue with maxAge)
3. **Simplified to stateless design** - matching the original's reliance on framework caching
4. **Fixed UserAgentConfigurator import** - removed internal API dependency

### ✅ What We Preserved

1. **Core functionality** - fetch from Secrets Manager + transform via ApigeeBearerTransformer
2. **Builder pattern** - flexible configuration
3. **Environment variables** - TOKEN_ENDPOINT_URL and TOKEN_SECRET_NAME
4. **Client configuration** - same SecretsManager client setup

---

## Conclusion

The `ApigeeSecretsProvider` now correctly mirrors the original `SSMApigeeProvider` design:

- ✅ **No manual caching** (was incorrectly added initially)
- ✅ **Simple, stateless design** (like the original)
- ✅ **Powertools v2 compatible** (no v1 dependencies)
- ✅ **Caller handles caching** (if needed, at higher level)

This matches your team's implementation approach with Powertools v2.8.0! 🎉

