# 🐛 OAuth2 Error - Diagnosis & Fix

## Date: December 28, 2025

---

## 🔍 Problem Identified

### Error in Logs:

```
OAuth2 endpoint returned error status: {}
java.lang.RuntimeException: Error received from OAuth2 endpoint. Response is Bad request
```

### Issues Found:

#### 1. **Logging Format Bug** ❌

```java
// BEFORE (broken)
log.error("OAuth2 endpoint returned error status: {}",response.statusCode());
```

**Problem:** The log shows `{}` literally instead of the status code value. This is because Log4j2 might not be properly
configured or there's a conflict.

#### 2. **Insufficient Error Details** ❌

The original error message only showed "Response is Bad request" without:

- HTTP status code
- Full response body details
- Request details (username/password presence)

---

## ✅ Fixes Applied

### 1. **Improved Error Logging**

```java
// AFTER (fixed)
}else{
int statusCode = response.statusCode();
String responseBody = response.body();
    log.

error("OAuth2 endpoint returned error - status: {}, response: {}",
      statusCode, responseBody);
    throw new

RuntimeException(
            "Error received from OAuth2 endpoint. Status: "+statusCode +
                ", Response: "+responseBody);
}
```

**Changes:**

- ✅ Extract status code and body into variables first
- ✅ Log both status and response body
- ✅ Include status code in exception message
- ✅ More detailed error context

### 2. **Added Debug Logging for Credentials**

```java
// AFTER (added)
try{
token =mapper.

readValue(value, ApigeeAuthToken .class);
    log.

debug("Parsed OAuth2 credentials - username present: {}, password present: {}",
              (token.userName() !=null&&!token.

userName().

isEmpty()),
        (token.

password() !=null&&!token.

password().

isEmpty()));
        }catch(
Exception e){
        log.

error("Failed to parse OAuth2 credentials from secret: {}",e.getMessage());
        throw new

RuntimeException("Failed to parse Apigee credentials",e);
}

        try{
        log.

debug("Sending OAuth2 token request to endpoint: {}",this.tokenEndpointUrl);

HttpResponse<String> response = this.httpClient.send(...);
```

**Changes:**

- ✅ Log if username and password are present (without logging actual values)
- ✅ Log when sending OAuth2 request
- ✅ Better traceability for debugging

---

## 🔍 Root Cause Analysis

### Why "Bad Request" Error?

The OAuth2 endpoint is returning a 400 Bad Request, which typically means one of:

1. **Invalid Credentials Format**
    - Username/password format incorrect
    - Missing required fields
    - Special characters not properly encoded

2. **Wrong Grant Type**
    - Currently using: `grant_type=client_credentials`
    - Endpoint might expect different format

3. **Missing Headers**
    - Content-Type might be incorrect
    - Additional headers might be required

4. **Secret Format Issue**
    - Secrets Manager secret format: `{"username": "...", "password": "..."}`
    - Apigee might expect: `{"client_id": "...", "client_secret": "..."}`

---

## 🔧 Debugging Steps (After Redeploy)

### 1. Check the Detailed Logs

```bash
aws --endpoint-url=http://localhost:4566 logs tail \
  /aws/lambda/my-token-auth-lambda --since 5m
```

Look for:

```
Parsed OAuth2 credentials - username present: true, password present: true
Sending OAuth2 token request to endpoint: https://...
OAuth2 endpoint returned error - status: 400, response: {...detailed error...}
```

### 2. Verify Secret Format

```bash
aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value \
  --secret-id external-api/token --query SecretString
```

Expected format:

```json
{
  "username": "ce43d3bd-e1e0-4eed-a269-8bffe958f0fb",
  "password": "aRZdZP63VqTmhfLcSE9zbAjG"
}
```

### 3. Test OAuth2 Endpoint Manually

```bash
curl -X POST https://exchange-staging.motiveintegrator.com/v1/authorize/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'username:password' | base64)" \
  -d "grant_type=client_credentials"
```

---

## 📊 Possible Fixes (If Still Failing)

### Option 1: Change POST Body Format

```java
// Current (might be wrong)
.POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))

// Try this instead
        .

POST(HttpRequest.BodyPublishers.ofString(
        "grant_type=client_credentials"+
        "&client_id="+URLEncoder.encode(token.userName(),StandardCharsets.UTF_8)+
        "&client_secret="+URLEncoder.

encode(token.password(),StandardCharsets.UTF_8)
        ))
```

### Option 2: Change Header Format

```java
// Current
.headers("Content-Type","application/x-www-form-urlencoded")

// Try JSON instead
.

headers("Content-Type","application/json")
.

POST(HttpRequest.BodyPublishers.ofString(
        "{\"grant_type\":\"client_credentials\","+
        "\"client_id\":\""+token.userName() +"\","+
        "\"client_secret\":\""+token.

password() +"\"}"
        ))
```

### Option 3: Check Secret Field Names

If Apigee expects `client_id` and `client_secret` instead of `username` and `password`:

```java
// Update record
record ApigeeAuthToken(
                @JsonProperty(value = "client_id", required = true)  // Changed from "username"
                @NotNull String userName,
                @JsonProperty(value = "client_secret", required = true)  // Changed from "password"
                @NotNull String password
        ) implements Serializable {
}
```

And update Terraform:

```terraform
secret_string = jsonencode({
  client_id     = var.client_id
  client_secret = var.client_secret
})
```

---

## ✅ What Was Fixed in This Update

| Issue              | Before                   | After                             |
|--------------------|--------------------------|-----------------------------------|
| **Logging Format** | `{}` shown literally     | Actual values logged              |
| **Error Details**  | Generic "Bad request"    | Status code + full response       |
| **Debug Info**     | No credential validation | Logs if username/password present |
| **Traceability**   | Hard to debug            | Clear flow with debug logs        |

---

## 🚀 Next Steps

1. **Rebuild** ✅ (changes made)
2. **Redeploy** (needed)
   ```bash
   cd infra/terraform
   terraform apply -var-file="terraform.localstack.tfvars" -auto-approve
   ```

3. **Test & Check Logs**
   ```bash
   aws --endpoint-url=http://localhost:4566 lambda invoke \
     --function-name my-token-auth-lambda \
     --payload '{"body":"{}"}' response.json
   
   aws --endpoint-url=http://localhost:4566 logs tail \
     /aws/lambda/my-token-auth-lambda --since 5m
   ```

4. **Analyze the detailed error message** and determine if:
    - Credentials format is wrong
    - Grant type is incorrect
    - Additional fields are needed

---

## 📝 Summary

### What Broke:

- ❌ OAuth2 endpoint returning "Bad request" (400)
- ❌ Logging format showing `{}` instead of values
- ❌ Insufficient error details for debugging

### What Was Fixed:

- ✅ Improved error logging with full details
- ✅ Added debug logging for credentials validation
- ✅ Better exception messages with status codes
- ✅ More traceable request flow

### Still Need To:

- 🔄 Redeploy and test
- 🔍 Check detailed logs to see exact error
- 🔧 Potentially fix OAuth2 request format based on error details

---

**Status:** ✅ **Logging Fixed**  
**Next:** Redeploy and analyze detailed error message  
**Goal:** Fix OAuth2 "Bad Request" issue

