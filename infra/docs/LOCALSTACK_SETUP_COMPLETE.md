# LocalStack Setup - Installation & Testing Complete ✅

## Current Status

### ✅ LocalStack Running

- **Container**: localstack-lambda-test
- **Status**: Healthy (Up 10+ minutes)
- **Endpoint**: http://localhost:4566
- **Port**: 4566 (default)

### ✅ Services Configured

- Lambda
- Secrets Manager
- CloudWatch Logs
- IAM
- API Gateway

### ✅ Secrets Manager Setup

**Secret Name**: `external-api/token`
**Secret Content**:

```json
{
  "client_id": "test-client-id",
  "client_secret": "test-client-secret"
}
```

### ✅ Lambda Function Created

**Function Name**: `my-token-auth-lambda`
**Handler**: `org.example.ApiHandler`
**Runtime**: `java21`
**Memory**: `512 MB`
**Timeout**: `60 seconds`

**Environment Variables**:

```
EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com
TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token
CLIENT_ID=test-client-id
CLIENT_SECRET=test-client-secret
TOKEN_SECRET_NAME=external-api/token
```

### ✅ JAR File Ready

- **Location**: `target/SetUpProject-1.0-SNAPSHOT.jar`
- **Size**: ~25 MB (with all dependencies)
- **Status**: Deployed to Lambda

---

## Quick Reference Commands

### Set AWS Credentials (Local Testing)

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### Test Secrets Manager

```powershell
# List secrets
aws secretsmanager list-secrets --endpoint-url http://localhost:4566

# Get specific secret
aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

### Test Lambda Function

```powershell
# Invoke Lambda
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{"path":"/test","httpMethod":"GET"}' `
  --endpoint-url http://localhost:4566 `
  response.json

# View response
Get-Content response.json
```

### View Lambda Logs

```powershell
# List log groups
aws logs describe-log-groups --endpoint-url http://localhost:4566

# Get log streams
aws logs describe-log-streams `
  --log-group-name /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566

# Get log events
aws logs get-log-events `
  --log-group-name /aws/lambda/my-token-auth-lambda `
  --log-stream-name '<stream-name>' `
  --endpoint-url http://localhost:4566

# Tail logs in real-time
aws logs tail /aws/lambda/my-token-auth-lambda `
  --follow `
  --endpoint-url http://localhost:4566
```

### Manage Lambda Function

```powershell
# List all functions
aws lambda list-functions --endpoint-url http://localhost:4566

# Get function details
aws lambda get-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566

# Update function code
aws lambda update-function-code `
  --function-name my-token-auth-lambda `
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar `
  --endpoint-url http://localhost:4566

# Delete function
aws lambda delete-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566
```

---

## Docker Commands

### View Container Status

```powershell
docker ps
docker logs localstack-lambda-test
```

### Stop LocalStack

```powershell
docker-compose down
```

### Restart LocalStack

```powershell
docker-compose restart
```

### Clean Everything

```powershell
docker-compose down -v
docker system prune -f
docker-compose up -d
```

---

## What's Next?

### Option 1: Test with Real API (Current Setup)

The Lambda is configured to call the real external API:

- Token Endpoint: `https://exchange-staging.motiveintegrator.com/v1/authorize/token`
- Data Endpoint: `https://exchange-staging.motiveintegrator.com/v1/data/...`

To test, update the secrets with real credentials:

```powershell
aws secretsmanager update-secret `
  --secret-id external-api/token `
  --secret-string '{"client_id":"your-real-id","client_secret":"your-real-secret"}' `
  --endpoint-url http://localhost:4566
```

Then invoke the Lambda:

```powershell
aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json
```

### Option 2: Mock the External API

Create a mock API server locally and update EXTERNAL_API_URL.

### Option 3: Deploy to AWS

When ready for AWS Lambda:

```bash
# Build
mvn clean install

# Deploy
aws lambda create-function \
  --function-name my-token-auth-lambda \
  --runtime java21 \
  --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-execution-role \
  --handler org.example.ApiHandler \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --environment Variables="{...}"
```

---

## Project Features Available

### ✅ Token Caching

- Automatic token caching for 55 minutes
- Reduces external API calls
- Thread-safe implementation
- Lazy initialization for cold start optimization

### ✅ Retry Logic

- Exponential backoff with jitter
- Max 3 attempts
- Handles network errors and timeouts
- Smart error detection

### ✅ Secrets Manager Integration

- Automatic credential retrieval
- Support for JSON secrets
- Powertools v2.8.0 integration
- Secure credential handling

### ✅ JSON Logging

- Powertools v2.8.0 structured logging
- CloudWatch-compatible format
- Multiple log levels
- Error tracking and debugging

### ✅ Cold Start Optimization

- Lazy initialization of expensive resources
- HTTP connection pooling
- Minimal dependencies loading
- Optimized for Lambda execution

---

## File Locations

```
Project Root: E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\

Key Files:
- docker-compose.yml         # LocalStack configuration
- init-aws.sh               # Resource initialization script
- localstack-helper.bat     # Windows helper script
- localstack-helper.sh      # Mac/Linux helper script

Source Code:
- src/main/java/org/example/
  ├── ApiHandler.java       # Lambda handler (entry point)
  ├── client/               # HTTP client classes
  ├── service/              # Business logic services
  ├── auth/                 # Token cache implementation
  └── config/               # Configuration classes

Built Artifacts:
- target/SetUpProject-1.0-SNAPSHOT.jar  # Shaded JAR (deployed to Lambda)
```

---

## Troubleshooting

### Issue: Lambda not responding

**Solution**: Check handler name matches - should be `org.example.ApiHandler`

### Issue: Secrets not found

**Solution**: Verify secret name is `external-api/token`

### Issue: Cold start too long

**Solution**: Increase Lambda memory (more CPU) or use reserved concurrency

### Issue: Network errors calling external API

**Solution**: Verify URL is correct and endpoint is accessible from your network

---

## Next Steps

1. **Update Secrets** with your actual credentials
2. **Test Lambda** with sample payloads
3. **Monitor Logs** for any errors
4. **Deploy to AWS** when ready

---

## Documentation Files

- `LOCALSTACK_QUICK_START.md` - Quick reference
- `LOCALSTACK_TESTING_GUIDE.md` - Detailed guide
- `LOCALSTACK_INSTALLATION.md` - Setup instructions

---

**Status**: ✅ LocalStack Ready for Testing
**Date**: December 27, 2025
**Environment**: Development (LocalStack)

