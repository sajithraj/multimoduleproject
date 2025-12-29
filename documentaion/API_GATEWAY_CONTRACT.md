# 📡 API GATEWAY CONTRACT

## Endpoint Details

### Base URL

```
LocalStack: http://localhost:4566
AWS: https://<api-id>.execute-api.<region>.amazonaws.com
```

### POST /api/auth

**Purpose:** Authenticate and fetch OAuth2 token, then call external API

**Request:**

```http
POST /api/auth HTTP/1.1
Host: localhost:4566
Content-Type: application/json

{}
```

**Headers:**

- `Content-Type: application/json` (required)
- `Authorization: (optional, not needed for now)`
- Any custom headers will be logged

**Body:**

- `{}` (empty JSON object is fine for now)

**Query Parameters:**

- None required currently
- Can be extended for filtering/options

---

## Response Examples

### Success Response (200 OK)

```json
{
  "statusCode": 200,
  "body": "{\"data\": \"API response\"}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

### Error Response (5xx)

```json
{
  "statusCode": 502,
  "body": "{\"error\": \"External API error: Connection timeout\"}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

### Internal Error (500)

```json
{
  "statusCode": 500,
  "body": "{\"error\": \"Internal server error\"}",
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*"
  }
}
```

---

## Status Codes

| Code | Meaning             | When                               |
|------|---------------------|------------------------------------|
| 200  | OK                  | Successful API call                |
| 400  | Bad Request         | Invalid request format             |
| 401  | Unauthorized        | Invalid credentials                |
| 403  | Forbidden           | Access denied                      |
| 502  | Bad Gateway         | External API error (after retries) |
| 503  | Service Unavailable | Service down (after retries)       |
| 500  | Internal Error      | Lambda error                       |
| 504  | Gateway Timeout     | Timeout (after retries)            |

---

## CORS Headers

All responses include:

```
Access-Control-Allow-Origin: *
Content-Type: application/json
```

This allows requests from any origin (suitable for development).

---

## Flow Diagram

```
Client Request
    ↓
API Gateway (POST /api/auth)
    ↓
Lambda Handler (ApiHandler)
    ├─ Extract request metadata
    ├─ Call external API
    │  ├─ Get token (cached or fresh)
    │  ├─ Add Authorization header
    │  └─ Retry on failure (up to 3 times)
    │
    └─ Build HTTP Response
       ├─ statusCode: 200/500/502
       ├─ body: JSON data
       └─ headers: Content-Type, CORS
```

---

## Testing

### Using cURL

```bash
# Basic request
curl -X POST http://localhost:4566/api/auth \
  -H "Content-Type: application/json" \
  -d '{}'

# With verbose output
curl -v -X POST http://localhost:4566/api/auth \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

# With custom timeout
curl -X POST http://localhost:4566/api/auth \
  --max-time 30 \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Using PowerShell

```powershell
$uri = "http://localhost:4566/api/auth"
$body = @{} | ConvertTo-Json

$response = Invoke-WebRequest -Uri $uri `
  -Method POST `
  -Body $body `
  -ContentType "application/json"

Write-Host $response.StatusCode
Write-Host $response.Content | ConvertFrom-Json
```

### Using Python

```python
import requests
import json

url = "http://localhost:4566/api/auth"
headers = {"Content-Type": "application/json"}
payload = {}

response = requests.post(url, headers=headers, json=payload)
print(f"Status: {response.status_code}")
print(f"Response: {response.json()}")
```

---

## Environment Variables (Lambda)

The Lambda function uses these environment variables:

| Variable            | Value              | Purpose                                |
|---------------------|--------------------|----------------------------------------|
| `TOKEN_SECRET_NAME` | external-api/token | Secrets Manager secret for credentials |
| `AWS_REGION`        | us-east-1          | AWS region                             |
| `ENVIRONMENT`       | dev-local          | Environment name                       |

---

## Monitoring & Logging

### CloudWatch Logs

```bash
# View logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --endpoint-url http://localhost:4566 \
  --follow

# Search for errors
aws logs tail /aws/lambda/my-token-auth-lambda \
  --endpoint-url http://localhost:4566 \
  --filter-pattern "ERROR"
```

### Request Tracking

Every request includes:

- Request ID: `context.getAwsRequestId()`
- Request Path: `event.getPath()`
- HTTP Method: `event.getHttpMethod()`
- Timestamp: Automatic from Powertools

---

## Rate Limiting (Future)

Currently: No rate limiting
Future: Can add via API Gateway throttling:

```terraform
resource "aws_api_gateway_stage" "api_stage" {
  throttle_settings {
    burst_limit = 5000
    rate_limit  = 2000
  }
}
```

---

## Authentication (Future)

Currently: Open (no API key required)
Future: Can add via:

1. **API Key:**

```terraform
api_key_required = true
```

2. **OAuth2:**

```terraform
authorization = "AWS_IAM"
```

3. **Cognito:**

```terraform
authorizer_id = aws_api_gateway_authorizer.cognito.id
```

---

## Deployment Checklist

- ✅ API Gateway created
- ✅ /api/auth resource created
- ✅ POST method configured
- ✅ Lambda integration active
- ✅ Lambda permissions granted
- ✅ API deployed
- ✅ CloudWatch logs enabled
- ✅ CORS headers configured

---

## Troubleshooting

### 404 Not Found

**Issue:** Endpoint not found
**Solution:** Check API Gateway deployment, verify path `/api/auth`

### 502 Bad Gateway

**Issue:** Lambda error or external API failure
**Solution:** Check CloudWatch logs, verify token credentials

### Timeout

**Issue:** Request takes >60 seconds
**Solution:** Increase Lambda timeout, check external API performance

### CORS Error

**Issue:** Cross-origin request blocked
**Solution:** CORS headers are enabled, check browser console

---

## Next Steps

1. **Test the endpoint** using cURL or Postman
2. **Monitor logs** in CloudWatch
3. **Add authentication** when ready
4. **Deploy to AWS** production environment
5. **Add more services** following the same pattern

---

## Support

For issues:

1. Check `COMPLETE_IMPLEMENTATION_GUIDE.md`
2. Review CloudWatch logs
3. Test endpoint with cURL
4. Verify Secrets Manager credentials

