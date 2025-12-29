# 📮 TESTING LAMBDA WITH POSTMAN

## ✅ How to Test in Postman

### Step 1: Create New Request in Postman

1. **Open Postman**
2. **Click "+" to create new request**
3. **Name it:** `Test Lambda OAuth Token`

---

### Step 2: Configure Request

**Method:** POST

**URL (Copy exactly):**

```
http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations
```

**Headers Tab** - Add these:

```
Content-Type: application/json
```

**Body Tab:**

- Select **raw**
- Select **JSON** from dropdown
- Paste this:

```json
{}
```

---

### Step 3: Send Request

**Click "Send"**

---

## 📊 Expected Response

### Status Code:

```
200 OK
```

### Response Body (Full JSON):

```json
{
  "statusCode": 200,
  "body": "{\"access_token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"token_type\": \"Bearer\", \"expires_in\": 14400}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

---

## 🔍 Understanding the Response

The response is wrapped in API Gateway format:

| Part              | Meaning                           |
|-------------------|-----------------------------------|
| `statusCode: 200` | HTTP status - success             |
| `body`            | The actual response (JSON string) |
| `headers`         | HTTP response headers             |

---

## 💡 Extract the Actual Token

To see just the token, parse the `body` field (it's a JSON string):

**The body contains:**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 14400
}
```

---

## 🧪 Complete Postman Setup

### Request Details:

```
Name:     Test Lambda OAuth Token
Method:   POST
URL:      http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations

Headers:
  Content-Type: application/json

Body (Raw JSON):
  {}
```

### Expected Response:

```
Status: 200 OK
Body: (see above JSON)
Response Time: ~1-2 seconds (first time)
                <100ms (cached, warm invocation)
```

---

## 📝 Step-by-Step Screenshots Description

1. **Postman Main Screen**
    - Method dropdown: Select "POST"
    - URL bar: Paste the invocation URL
    - Headers: Add Content-Type: application/json
    - Body: Select "raw" → "JSON" → paste {}

2. **Click "Send"**
    - Watch for response in lower panel

3. **View Response**
    - Status code shows "200 OK"
    - Body shows the full JSON response
    - Check response time at bottom

---

## 🔄 Compare with Local

When you run your local version:

- **Same response structure?** ✅ Should match
- **Token format same?** ✅ Should be similar
- **Status code same?** ✅ Should be 200
- **Headers present?** ✅ Should include Content-Type and CORS

---

## ❓ Troubleshooting in Postman

### Issue: Connection Refused

- ✅ Make sure LocalStack is running: `docker ps | grep localstack`
- ✅ Check URL is exactly: `http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations`

### Issue: 404 Not Found

- ✅ Lambda might not exist - run:
  `aws lambda get-function --function-name my-token-auth-lambda --endpoint-url http://localhost:4566`

### Issue: Empty Response

- ✅ Check Lambda logs: `aws logs tail /aws/lambda/my-token-auth-lambda --endpoint-url http://localhost:4566 --since 5m`

### Issue: Token Error

- ✅ Check Secrets Manager:
  `aws secretsmanager get-secret-value --secret-id external-api/token --endpoint-url http://localhost:4566`

---

## 💾 Save in Postman

1. Click "Save" button
2. Collection: Create new "Lambda Tests"
3. Name: "Test Lambda OAuth Token"
4. Save for reuse later!

---

## 🚀 Quick Copy-Paste Setup

In Postman:

- **Method:** POST
- **URL:** `http://localhost:4566/2015-03-31/functions/my-token-auth-lambda/invocations`
- **Header:** `Content-Type: application/json`
- **Body:** `{}`
- **Click Send**

Done! ✨

---

## 📊 Response Time Metrics

| Call                       | Time         | Notes                    |
|----------------------------|--------------|--------------------------|
| First call (cold start)    | ~2-3 seconds | Token fetched from OAuth |
| Second call (warm, cached) | <100ms       | Token from cache         |
| Subsequent calls           | <100ms       | All cached               |

You'll see the time difference in Postman's response time indicator!

---

## 📸 What You'll See in Postman

**Left Panel:**

- Request name: Test Lambda OAuth Token
- Method: POST
- URL field filled

**Center Panel:**

- Tabs: Params, Authorization, Headers, Body, etc.
- Body tab shows: `{}`

**Right Panel:**

- Response body showing full JSON
- Status: 200 OK
- Response time
- Headers showing Content-Type: application/json

---

## ✅ That's It!

Just follow the setup above and click "Send" in Postman. You'll see the complete response with token, type, expiration,
and headers.

Perfect for comparing with your local version! 🎉

