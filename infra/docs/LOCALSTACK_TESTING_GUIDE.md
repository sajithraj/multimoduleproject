# LocalStack Testing Setup

Complete guide to test your Lambda function locally with LocalStack.

## What is LocalStack?

LocalStack is a fully functional local AWS cloud stack that runs in Docker. It simulates AWS services including:

- AWS Lambda
- AWS Secrets Manager
- AWS CloudWatch Logs
- AWS IAM
- AWS API Gateway
- AWS CloudFormation

---

## Prerequisites

- Docker Desktop installed
- Docker Compose installed
- AWS CLI v2 installed
- Java 21 JDK
- Maven

---

## Setup Instructions

### 1. Start LocalStack

```bash
cd SetUpProject
docker-compose up -d
```

**Wait for initialization** (about 30-60 seconds):

```bash
# Check status
docker logs localstack-lambda-test

# Should see: "Ready."
```

### 2. Verify AWS Resources Created

```bash
# Set AWS credentials
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Verify secret
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566

# Output should show:
# {
#   "client_id": "test-client-id",
#   "client_secret": "test-client-secret"
# }
```

### 3. Build the Lambda JAR

```bash
cd SetUpProject
mvn clean install

# JAR location: target/SetUpProject-1.0-SNAPSHOT.jar
```

### 4. Create Lambda Function

```bash
aws lambda create-function \
  --function-name my-token-auth-lambda \
  --runtime java21 \
  --role arn:aws:iam::000000000000:role/lambda-execution-role \
  --handler org.example.Main::handleRequest \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --environment Variables="{
    EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
    TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
    CLIENT_ID=test-client-id,
    CLIENT_SECRET=test-client-secret,
    TOKEN_SECRET_NAME=external-api/token
  }" \
  --timeout 60 \
  --memory-size 512 \
  --endpoint-url http://localhost:4566
```

### 5. Test Lambda Invocation

```bash
# Synchronous invocation
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{"action": "test"}' \
  --endpoint-url http://localhost:4566 \
  response.json

# View response
cat response.json
```

---

## Testing Scenarios

### Scenario 1: Token Authorization

**Create test file** `test-token-auth.json`:

```json
{
  "action": "get_token"
}
```

**Run test:**

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload file://test-token-auth.json \
  --endpoint-url http://localhost:4566 \
  response.json

cat response.json
```

### Scenario 2: API Call with Authentication

**Create test file** `test-api-call.json`:

```json
{
  "action": "call_api",
  "endpoint": "/v1/data/list",
  "method": "GET"
}
```

**Run test:**

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload file://test-api-call.json \
  --endpoint-url http://localhost:4566 \
  response.json

cat response.json
```

### Scenario 3: POST with Body

**Create test file** `test-post.json`:

```json
{
  "action": "call_api",
  "endpoint": "/v1/data/create",
  "method": "POST",
  "body": {
    "name": "Test Data",
    "value": 123
  }
}
```

---

## Viewing Logs

### CloudWatch Logs in LocalStack

```bash
# List log groups
aws logs describe-log-groups \
  --endpoint-url http://localhost:4566

# View logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow \
  --endpoint-url http://localhost:4566
```

### Docker Logs

```bash
# Real-time logs
docker logs -f localstack-lambda-test

# With timestamps
docker logs -f --timestamps localstack-lambda-test
```

---

## LocalStack UI

Open browser and visit:

```
http://localhost:8080
```

Features:

- View Lambda functions
- Inspect logs
- Monitor Secrets Manager
- Check API Gateway
- View metrics

---

## AWS CLI Commands Reference

### Secrets Manager

```bash
# Create secret
aws secretsmanager create-secret \
  --name external-api/token \
  --secret-string '{"client_id":"id","client_secret":"secret"}' \
  --endpoint-url http://localhost:4566

# Get secret
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566

# Update secret
aws secretsmanager update-secret \
  --secret-id external-api/token \
  --secret-string '{"client_id":"new-id","client_secret":"new-secret"}' \
  --endpoint-url http://localhost:4566

# List secrets
aws secretsmanager list-secrets \
  --endpoint-url http://localhost:4566
```

### Lambda

```bash
# List functions
aws lambda list-functions \
  --endpoint-url http://localhost:4566

# Get function details
aws lambda get-function \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566

# Update function code
aws lambda update-function-code \
  --function-name my-token-auth-lambda \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --endpoint-url http://localhost:4566

# Get function logs
aws lambda get-function-code-signing-config \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566
```

### CloudWatch Logs

```bash
# List log groups
aws logs describe-log-groups \
  --endpoint-url http://localhost:4566

# List log streams
aws logs describe-log-streams \
  --log-group-name /aws/lambda/my-token-auth-lambda \
  --endpoint-url http://localhost:4566

# Get log events
aws logs get-log-events \
  --log-group-name /aws/lambda/my-token-auth-lambda \
  --log-stream-name 1 \
  --endpoint-url http://localhost:4566
```

---

## Troubleshooting

### LocalStack Not Starting

```bash
# Check Docker running
docker ps

# View logs
docker logs localstack-lambda-test

# Restart
docker-compose restart
```

### Connection Refused

```bash
# Verify LocalStack is running
curl http://localhost:4566

# Check network
docker network ls
docker network inspect lambda-network
```

### Secret Not Found

```bash
# Verify secret exists
aws secretsmanager list-secrets \
  --endpoint-url http://localhost:4566

# Check secret content
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566
```

### Lambda Function Fails

```bash
# Check logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow \
  --endpoint-url http://localhost:4566

# Get function details
aws lambda get-function \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566
```

---

## Complete Testing Workflow

```bash
# 1. Start LocalStack
docker-compose up -d

# 2. Wait for init (30-60 seconds)
sleep 60

# 3. Build JAR
mvn clean install

# 4. Create Lambda function
aws lambda create-function \
  --function-name my-token-auth-lambda \
  --runtime java21 \
  --role arn:aws:iam::000000000000:role/lambda-execution-role \
  --handler org.example.Main::handleRequest \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --environment Variables="{
    EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
    TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
    CLIENT_ID=test-client-id,
    CLIENT_SECRET=test-client-secret,
    TOKEN_SECRET_NAME=external-api/token
  }" \
  --timeout 60 \
  --memory-size 512 \
  --endpoint-url http://localhost:4566

# 5. Invoke Lambda
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json

# 6. View response
cat response.json

# 7. View logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow \
  --endpoint-url http://localhost:4566

# 8. Stop LocalStack
docker-compose down
```

---

## Environment Variables

Configure in Lambda or docker-compose:

```bash
EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com
TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token
CLIENT_ID=test-client-id
CLIENT_SECRET=test-client-secret
TOKEN_SECRET_NAME=external-api/token
```

---

## Network Configuration

The docker-compose creates a custom network `lambda-network` that allows:

- LocalStack container to communicate with Docker socket
- LocalStack UI to connect to LocalStack
- Local machine to access both services

### Accessing from your code:

**From LocalStack container:**

```
http://localstack:4566
```

**From your machine:**

```
http://localhost:4566
```

---

## Cleanup

```bash
# Stop containers
docker-compose down

# Remove all containers and volumes
docker-compose down -v

# Remove images (optional)
docker rmi localstack/localstack:latest
docker rmi localstack/localstack-ui:latest
```

---

## Notes

1. **Real API Calls**: The Lambda function will attempt real HTTP calls to the external API endpoint. For testing token
   authorization without external API, mock the HTTP client.

2. **Secrets Security**: LocalStack credentials are for local testing only. Production uses real AWS Secrets Manager.

3. **Java 21 Runtime**: Ensure your JAR is compiled for Java 21. Lambda supports java21 runtime.

4. **Memory & Timeout**: Configured as 512MB and 60 seconds. Adjust as needed.

5. **Concurrent Testing**: Can create multiple Lambda functions for different test scenarios.

---

**Status**: Ready for Local Testing 🚀

