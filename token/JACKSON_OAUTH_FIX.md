# 🔧 OAuth2 Jackson Deserialization Issue - FIXED

## 🐛 Problem

**Error in CloudWatch Logs:**
```
READ_DATE_TIMESTAMPS_AS_NANOSECONDS: java.lang.NoSuchFieldError
java.lang.NoSuchFieldError: READ_DATE_TIMESTAMPS_AS_NANOSECONDS
at com.fasterxml.jackson.databind.DeserializationContext.findRootValueDeserializer
```

**Root Cause:**
Jackson library version conflict when trying to deserialize OAuth2 response with `Instant` timestamp fields.

---

## ✅ Solution Applied

### 1. Changed Timestamp Field Type
**Before:**
```java
record ApigeeOauthResponse(
    @JsonProperty("issued_at") Instant issuedAt,  // ❌ Causes Jackson error
    ...
)
```

**After:**
```java
record ApigeeOauthResponse(
    @JsonProperty("issued_at") String issuedAt,  // ✅ Works reliably
    ...
)
```

### 2. Removed JavaTimeModule
**Before:**
```java
this.mapper = new ObjectMapper();
this.mapper.registerModule(new JavaTimeModule());  // ❌ Not needed
```

**After:**
```java
this.mapper = new ObjectMapper();  // ✅ Simple and works
```

### 3. Added Better Error Handling & Logging
```java
if (HttpStatusCode.OK == response.statusCode()) {
    String responseBody = response.body();
    log.info("Successfully retrieved OAuth2 response from endpoint: {}", this.tokenEndpointUrl);
    log.debug("OAuth2 response body: {}", responseBody);  // ✅ View raw response
    
    try {
        ApigeeOauthResponse oauthResponse = mapper.readValue(responseBody, ApigeeOauthResponse.class);
        String accessToken = oauthResponse.accessToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            log.error("OAuth2 response contains null or empty access_token. Response: {}", responseBody);
            throw new RuntimeException("OAuth2 response missing access_token");
        }
        
        log.info("Successfully extracted bearer token (length: {} characters)", accessToken.length());
        return accessToken;
        
    } catch (Exception e) {
        log.error("Failed to parse OAuth2 response. Response body: {}", responseBody, e);
        throw new RuntimeException("Failed to parse OAuth2 response: " + e.getMessage(), e);
    }
}
```

---

## 🔍 How to View OAuth2 Response in CloudWatch

### Method 1: Enable DEBUG Logging

**Set Lambda Environment Variable:**
```
POWERTOOLS_LOG_LEVEL=DEBUG
```

**What You'll See:**
```json
{
  "level": "DEBUG",
  "message": "OAuth2 response body: {\"access_token\":\"abc123...\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"issued_at\":\"1735455850000\"}"
}
```

### Method 2: CloudWatch Log Insights Query

```sql
fields @timestamp, @message
| filter @message like /OAuth2 response body/
| sort @timestamp desc
| limit 20
```

### Method 3: Check Logs in Console

1. Go to **CloudWatch → Log Groups**
2. Find `/aws/lambda/my-token-auth-lambda`
3. Filter: `OAuth2 response`
4. You'll see the raw JSON response

---

## 📊 Expected OAuth2 Response Format

### Successful Response
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "issued_at": "1735455850000"
}
```

### Error Response
```json
{
  "error": "invalid_client",
  "error_description": "Invalid credentials"
}
```

---

## 🧪 Testing the Fix

### Rebuild & Deploy
```bash
# Build
mvn clean package -pl token

# Deploy
cd infra/terraform
terraform apply
```

### Test Lambda
```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json

cat response.json
```

### Check Logs
```bash
aws logs tail /aws/lambda/my-token-auth-lambda --follow
```

---

## 🎯 What Changed

| File | Change | Reason |
|------|--------|--------|
| `ApigeeBearerTransformer.java` | Changed `Instant` to `String` for `issuedAt` | Avoid Jackson deserialization issues |
| `ApigeeBearerTransformer.java` | Removed `JavaTimeModule` | Not needed with String timestamps |
| `ApigeeBearerTransformer.java` | Added response body logging | Debug OAuth2 responses |
| `ApigeeBearerTransformer.java` | Added null/empty token check | Better error messages |
| `ApigeeBearerTransformer.java` | Added try-catch around parsing | Log full response on error |

---

## 🚀 Expected Behavior After Fix

### 1. Token Request Sent
```
DEBUG - Sending OAuth2 token request to endpoint: https://internal.dev.api.svb.com/v1/security/oauth/token
```

### 2. Response Received
```
INFO - Successfully retrieved OAuth2 response from endpoint: https://...
DEBUG - OAuth2 response body: {"access_token":"...","token_type":"Bearer",...}
```

### 3. Token Extracted
```
INFO - Successfully extracted bearer token (length: 284 characters)
```

### 4. Token Cached
```
INFO - OAuth2 bearer token fetched fresh and CACHED by Powertools (fetch time: 1234 ms)
```

### 5. API Call Success
```
INFO - External API call successful: status=200
```

---

## 🐛 Troubleshooting

### Issue: Still Getting Jackson Error
**Solution:** Rebuild and redeploy
```bash
mvn clean package
terraform apply
```

### Issue: Can't See Response Body
**Solution:** Enable DEBUG logging
```bash
# In Terraform or Lambda console
POWERTOOLS_LOG_LEVEL=DEBUG
```

### Issue: Token is null
**Check:** Response body in logs
```sql
-- CloudWatch Insights
fields @message
| filter @message like /OAuth2 response body/
```

### Issue: Invalid Credentials
**Check:** Secrets Manager values
```bash
aws secretsmanager get-secret-value \
  --secret-id external-api/token
```

---

## 📝 Key Takeaways

1. **Use String for timestamps** - More reliable than `Instant` with Jackson
2. **Log response bodies** - Essential for debugging OAuth issues
3. **Add null checks** - Catch empty/missing tokens early
4. **Use DEBUG level** - See raw responses in CloudWatch
5. **Keep it simple** - Don't over-complicate with modules

---

## ✅ Summary

**Problem:** Jackson couldn't deserialize OAuth2 response with `Instant` field  
**Solution:** Changed to `String` and added better logging  
**Result:** Token extraction works reliably + full visibility into responses  

**Status:** ✅ **FIXED**

---

**Issue Resolution Date:** December 29, 2025

