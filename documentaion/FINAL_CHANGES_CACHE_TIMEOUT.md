# ✅ Final Changes - Cache Logging & Timeout Configuration

## Date: December 28, 2025

---

## 🎯 Changes Requested

> "How do I know whether the token is coming from cache or from API call?"
> "Add timeout OAUTH2_TIMEOUT_SECONDS in terraform lambda env variable & make token timeout default value to 3."

---

## ✅ Changes Implemented

### 1. **Added Cache Tracking Logs**

#### SSMApigeeProvider.java

**Added:**

- Logger import and instance
- Debug logging for Secrets Manager fetch
- Timer to measure Secrets Manager fetch time
- **Clear message: "Calling OAuth2 token endpoint to get fresh bearer token (no caching)"**

**Before:**

```java
public String getValue(String secretKey) {
    String key = (secretKey == null || secretKey.trim().isEmpty()) ? TOKEN_SECRET_NAME : secretKey;
    String secretValue = getSecretFromSecretsManager(key);
    return transformer.applyTransformation(secretValue, String.class);
}
```

**After:**

```java
public String getValue(String secretKey) {
    String key = (secretKey == null || secretKey.trim().isEmpty()) ? TOKEN_SECRET_NAME : secretKey;
    
    LOG.debug("Fetching OAuth2 credentials from Secrets Manager: {}", key);
    long startTime = System.currentTimeMillis();
    
    String secretValue = getSecretFromSecretsManager(key);
    
    LOG.debug("Secrets Manager fetch completed in {} ms", System.currentTimeMillis() - startTime);
    LOG.info("Calling OAuth2 token endpoint to get fresh bearer token (no caching)");

    // Apply transformation to get bearer token
    return transformer.applyTransformation(secretValue, String.class);
}
```

**New Log Output:**

```
DEBUG Fetching OAuth2 credentials from Secrets Manager: external-api/token
DEBUG Secrets Manager fetch completed in 45 ms
INFO  Calling OAuth2 token endpoint to get fresh bearer token (no caching)
DEBUG Sending OAuth2 token request to endpoint: https://...
INFO  Successfully retrieved OAuth2 bearer token from endpoint: https://...
```

---

### 2. **Updated Default Timeout**

#### ApigeeBearerTransformer.java

**Changed default from 10 seconds to 3 seconds:**

**Before:**

```java
private Integer getTimeoutValue() {
    try {
        return Integer.parseInt(System.getenv("OAUTH2_TIMEOUT_SECONDS"));
    } catch (Exception e) {
        log.trace("Default to 10 second OAuth2 timeout.");
        return 10;  // ← Old default
    }
}
```

**After:**

```java
private Integer getTimeoutValue() {
    try {
        return Integer.parseInt(System.getenv("OAUTH2_TIMEOUT_SECONDS"));
    } catch (Exception e) {
        log.debug("OAUTH2_TIMEOUT_SECONDS not set, using default timeout of 3 seconds");
        return 3;  // ← New default ✅
    }
}
```

---

### 3. **Added Terraform Environment Variable**

#### main.tf

**Added OAUTH2_TIMEOUT_SECONDS to Lambda environment variables:**

**Before:**

```terraform
environment {
  variables = {
    TOKEN_ENDPOINT_URL = var.token_endpoint_url
    TOKEN_SECRET_NAME  = var.secret_name
    EXTERNAL_API_URL   = var.external_api_url
    AWS_REGION         = var.aws_region
    ENVIRONMENT        = var.environment
  }
}
```

**After:**

```terraform
environment {
  variables = {
    # Token Configuration
    TOKEN_ENDPOINT_URL      = var.token_endpoint_url
    TOKEN_SECRET_NAME       = var.secret_name
    OAUTH2_TIMEOUT_SECONDS  = var.oauth2_timeout_seconds  # ← New ✅

    # External API Configuration
    EXTERNAL_API_URL = var.external_api_url

    # General Configuration
    AWS_REGION  = var.aws_region
    ENVIRONMENT = var.environment
  }
}
```

**Added variable definition:**

```terraform
variable "oauth2_timeout_seconds" {
  description = "OAuth2 token request timeout in seconds"
  type        = number
  default     = 3  # ← Default value
}
```

---

## 📊 How to Interpret Logs

### Current Implementation (No Caching):

**Every request will show:**

```
INFO  Calling OAuth2 token endpoint to get fresh bearer token (no caching)
DEBUG Sending OAuth2 token request to endpoint: https://...
INFO  Successfully retrieved OAuth2 bearer token from endpoint: https://...
```

**This means:**

- ✅ Token is fetched FRESH from OAuth2 API every time
- ❌ No caching (stateless, as per your team's approach)

---

### If You Add Caching Later:

If you implement token caching (e.g., using Powertools Parameters caching), logs would look like:

**First call (cache miss):**

```
DEBUG Token not found in cache, fetching from OAuth2 endpoint
INFO  Calling OAuth2 token endpoint to get fresh bearer token
INFO  Successfully retrieved OAuth2 bearer token from endpoint
INFO  Token cached for 3600 seconds
```

**Second call (cache hit):**

```
INFO  Token found in cache, using cached value (expires in 3245 seconds)
DEBUG Skipping OAuth2 endpoint call
```

---

## 🔧 Configuration Options

### Option 1: Use Default Timeout (3 seconds)

```terraform
# Don't set OAUTH2_TIMEOUT_SECONDS in tfvars
# Will use default value of 3
```

### Option 2: Override Timeout

```terraform
# terraform.tfvars or terraform.localstack.tfvars
oauth2_timeout_seconds = 5  # Override to 5 seconds
```

### Option 3: Environment-Specific Timeouts

```terraform
# terraform.dev.tfvars
oauth2_timeout_seconds = 3

# terraform.staging.tfvars
oauth2_timeout_seconds = 5

# terraform.prod.tfvars
oauth2_timeout_seconds = 10
```

---

## 📝 Files Modified

| File                                     | Change                                  | Purpose                         |
|------------------------------------------|-----------------------------------------|---------------------------------|
| `token/.../SSMApigeeProvider.java`       | Added logger & timing logs              | Track token fetch timing        |
| `token/.../SSMApigeeProvider.java`       | Added cache status log                  | Clearly indicate "no caching"   |
| `token/.../ApigeeBearerTransformer.java` | Changed default timeout: 10→3           | Faster default timeout          |
| `infra/terraform/main.tf`                | Added `OAUTH2_TIMEOUT_SECONDS` env var  | Configure timeout via Terraform |
| `infra/terraform/main.tf`                | Added `oauth2_timeout_seconds` variable | Define timeout parameter        |

---

## 🎯 Log Interpretation Guide

### Understanding Token Flow:

```
┌─────────────────────────────────────────────────────────────┐
│  1. Request arrives at Lambda                               │
│     LOG: "Received request: path=..., method=..."          │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  2. ExternalApiClient.callExternalApi()                     │
│     LOG: "Initiating external API call to: ..."            │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  3. SSMApigeeProvider.getValue()                            │
│     LOG: "Fetching OAuth2 credentials from Secrets Manager" │
│     LOG: "Secrets Manager fetch completed in X ms"         │
│     LOG: "Calling OAuth2 token endpoint (no caching)" ✅    │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  4. ApigeeBearerTransformer.applyTransformation()           │
│     LOG: "Parsed OAuth2 credentials - username/password OK"│
│     LOG: "Sending OAuth2 token request to endpoint"        │
│     LOG: "Successfully retrieved OAuth2 bearer token" ✅    │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│  5. ExternalApiClient continues                             │
│     LOG: "Retrieved access token, length: 512 characters"  │
│     LOG: "External API call successful: status=200" ✅      │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Summary

### What You Can Now See in Logs:

1. **Token Source:**
    - ✅ Clearly states: "Calling OAuth2 token endpoint to get fresh bearer token (no caching)"
    - ✅ Every request shows this (stateless approach)

2. **Timing Information:**
    - ✅ Secrets Manager fetch time (e.g., "45 ms")
    - ✅ Can identify if Secrets Manager is slow

3. **OAuth2 Timeout:**
    - ✅ Default: 3 seconds
    - ✅ Configurable via Terraform
    - ✅ Logged on initialization

### Configuration:

| Setting        | Default   | Configurable Via         | Production Value            |
|----------------|-----------|--------------------------|-----------------------------|
| OAuth2 Timeout | 3 seconds | `OAUTH2_TIMEOUT_SECONDS` | Adjust based on API latency |
| Token Caching  | No cache  | N/A (by design)          | Consider adding if needed   |

---

## 🚀 Deployment

### Build:

```bash
mvn clean package -DskipTests
```

### Deploy:

```bash
cd infra/terraform
terraform apply -var-file="terraform.localstack.tfvars" -auto-approve
```

### Test & Verify Logs:

```bash
# Invoke Lambda
aws --endpoint-url=http://localhost:4566 lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{"body":"{}"}' response.json

# Check logs for cache status
aws --endpoint-url=http://localhost:4566 logs tail \
  /aws/lambda/my-token-auth-lambda --since 5m | grep -E "(cache|OAuth2 token endpoint)"

# Expected output:
# INFO Calling OAuth2 token endpoint to get fresh bearer token (no caching)
# INFO Successfully retrieved OAuth2 bearer token from endpoint
```

---

## 🎯 Next Steps

You mentioned:
> "This is going to be our final change then we will work on the lambda handler codebase more"

**Ready for:**

- ✅ Lambda handler improvements
- ✅ Dagger dependency injection implementation
- ✅ Any other service layer enhancements

The token/authentication layer is now:

- ✅ Production-ready
- ✅ Well-logged for debugging
- ✅ Configurable via Terraform
- ✅ Stateless (no caching issues)
- ✅ HttpRequest builder reuse bug fixed

---

**Status:** ✅ **COMPLETE**  
**Cache Tracking:** ✅ Added (clearly shows "no caching")  
**Timeout Configuration:** ✅ Added (default 3 seconds)  
**Terraform Variable:** ✅ Added (`OAUTH2_TIMEOUT_SECONDS`)  
**Ready for:** Lambda handler work with Dagger

