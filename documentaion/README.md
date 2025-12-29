# Production Grade Java Lambda Application

Complete implementation of a Java Lambda function that calls external APIs with authentication token caching, retry
logic, and JSON logging.

## Features

✅ **Powertools v2** - Latest logging and parameter management  
✅ **Token Caching** - 55-minute in-container cache for 60-minute auth tokens  
✅ **Secrets Manager Integration** - Secure token retrieval using Powertools Parameters  
✅ **Retry Logic** - Exponential backoff with jitter (no circuit breaker)  
✅ **JSON Logging** - CloudWatch Logs compatible structured logging  
✅ **Cold Start Optimization** - Lazy initialization of expensive resources  
✅ **Production Grade** - Error handling, validation, thread-safe implementation  
✅ **API Gateway Integration** - APIGatewayProxyEvent/Response compatibility

## Project Structure

```
src/main/java/org/example/
├── ApiHandler.java                 # Lambda handler for API Gateway
├── Main.java                       # Configuration documentation
├── auth/
│   ├── TokenCache.java            # 55-min token caching with Secrets Manager
│   └── SecretManagerClient.java   # Deprecated - use TokenCache
├── client/
│   └── ExternalApiClient.java     # HTTP client with retry logic
├── config/
│   ├── AppConfig.java             # Environment variable configuration
│   └── RetryConfigProvider.java   # Retry configuration with exponential backoff
├── exception/
│   └── ExternalApiException.java  # Custom exception class
├── model/
│   ├── ApiRequest.java            # Request model
│   └── ApiResponse.java           # Response model
└── util/
    └── HttpClientFactory.java     # Lazy-initialized HTTP client with pooling

src/main/resources/
├── log4j2.xml                      # Logging configuration
└── LambdaJsonLayout.json          # JSON layout template
```

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.8+
- AWS CLI configured with appropriate credentials

### Build

```bash
mvn clean package
```

This creates a shaded JAR (`SetUpProject-1.0-SNAPSHOT.jar`) containing all dependencies.

### AWS Lambda Deployment

1. **Create IAM Role**
   ```bash
   aws iam create-role \
     --role-name lambda-execution-role \
     --assume-role-policy-document file://trust-policy.json
   ```

2. **Attach Policies**
   ```bash
   # CloudWatch Logs
   aws iam attach-role-policy \
     --role-name lambda-execution-role \
     --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

   # Secrets Manager
   aws iam put-role-policy \
     --role-name lambda-execution-role \
     --policy-name secrets-access \
     --policy-document file://secrets-policy.json
   ```

3. **Create Lambda Function**
   ```bash
   aws lambda create-function \
     --function-name external-api-lambda \
     --runtime java21 \
     --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-execution-role \
     --handler org.example.ApiHandler \
     --timeout 30 \
     --memory-size 512 \
     --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
     --environment Variables="{EXTERNAL_API_URL=https://api.example.com/endpoint,TOKEN_SECRET_NAME=my-api-token}"
   ```

4. **Create API Gateway Integration**
   ```bash
   # Create REST API
   aws apigateway create-rest-api --name external-api

   # Integrate with Lambda
   # (Use AWS Console for simplicity or awscli-local for detailed setup)
   ```

## Configuration

### Environment Variables

| Variable            | Description                 | Required |
|---------------------|-----------------------------|----------|
| `EXTERNAL_API_URL`  | External API endpoint URL   | Yes      |
| `TOKEN_SECRET_NAME` | Secrets Manager secret name | Yes      |

### Secrets Manager Format

Store in AWS Secrets Manager as JSON:

```json
{
  "token": "your_bearer_token_here"
}
```

### Logging Configuration

Edit `src/main/resources/log4j2.xml` to adjust:

- Log level (currently INFO)
- Log appenders
- Logger-specific configurations

## Token Caching

### Cache Behavior

- **Duration**: 55 minutes (conservative vs 60-minute token lifetime)
- **Storage**: Lambda container memory
- **Scope**: Per Lambda execution environment
- **Thread-safe**: Yes (double-checked locking)
- **Expiry**: Automatic, checked on each request

### Cache Logic Flow

```
Request arrives
    ↓
Check if cached token exists AND not expired
    ├─ YES → Return cached token
    └─ NO → Fetch from Secrets Manager (synced)
            ↓
        Cache with expiry
            ↓
        Return token
```

## Retry Configuration

### Policy Details

- **Max Attempts**: 3 (1 initial + 2 retries)
- **Initial Wait**: 300ms
- **Backoff**: Exponential (2.0 multiplier)
- **Jitter**: 50% randomization
- **Max Wait**: 2000ms

### Retried Exceptions

- `java.io.IOException`
- `java.net.SocketException`
- `java.net.SocketTimeoutException`
- `java.net.ConnectException`

### Ignored Exceptions

- `IllegalArgumentException` (configuration errors)

## Performance Characteristics

### Cold Start

- **First Invocation**: 2-3 seconds
    - JVM startup: ~1-1.5s
    - Dependencies initialization: ~500-800ms
    - First Secrets Manager call: ~500-1000ms

### Warm Start

- **Subsequent Invocations** (cached token): 50-100ms
- **Token Refresh** (every 55 min): 200-300ms
- **API Call**: Depends on external API latency + 300-600ms retry overhead if needed

### Optimization Strategies Applied

1. **Lazy Initialization**: HTTP client, retry config, secrets provider
2. **Connection Pooling**: 10 total connections, 5 per route
3. **Token Caching**: 55-minute in-memory cache
4. **Container Reuse**: Lambda keeps warm between invocations

## Error Handling

### API Errors

```
External API Call
    ├─ Success (2xx) → Return response
    ├─ Error (4xx/5xx) → Retry (if transient)
    │                  → Fail after max attempts
    └─ Network Error → Retry with backoff
```

### Logging

All errors logged with full context in JSON format:

```json
{
  "timestamp": "2024-12-27T10:30:45.123Z",
  "level": "ERROR",
  "logger": "org.example.client.ExternalApiClient",
  "message": "Failed to call external API after retries",
  "exception": "...",
  "thread": "main"
}
```

## API Gateway Integration

### Request Format

```
GET /api/resource HTTP/1.1
Authorization: Bearer {Lambda retrieves this}
```

### Response Format (Success)

```json
{
  "statusCode": 200,
  "body": "{external API response}",
  "headers": {
    "Content-Type": "application/json"
  }
}
```

### Response Format (Error)

```json
{
  "statusCode": 502,
  "body": {
    "error": "External API error: ...",
    "timestamp": 1735288245000
  },
  "headers": {
    "Content-Type": "application/json"
  }
}
```

## Testing

### Local Testing

```bash
# Build project
mvn clean package

# Test with mocked Lambda context
mvn test
```

### Lambda Testing

```bash
# Create test event
aws lambda invoke \
  --function-name external-api-lambda \
  --payload '{"httpMethod":"GET","path":"/test"}' \
  response.json

# View response
cat response.json
```

## Monitoring

### CloudWatch Logs

Logs are automatically sent to CloudWatch in JSON format:

```bash
# Tail logs
aws logs tail /aws/lambda/external-api-lambda --follow

# Search for errors
aws logs filter-log-events \
  --log-group-name /aws/lambda/external-api-lambda \
  --filter-pattern "ERROR"
```

### CloudWatch Metrics

- Duration (execution time)
- Invocations (total calls)
- Errors (failed invocations)
- Throttles (rate limit exceeded)

### Custom Metrics

Add to `src/main/java/org/example/` as needed:

```java
// Example: CloudWatch metrics for cache hits/misses
MetricUnit.Count("TokenCacheHit",1);
MetricUnit.

Milliseconds("TokenFetchDuration",fetchTime);
```

## Troubleshooting

### Token Not Cached

**Symptom**: Multiple Secrets Manager calls per invocation
**Solution**: Check token expiry (55 minutes), verify cache isn't cleared

### Retries Not Working

**Symptom**: API calls fail on first error
**Solution**: Verify exception types match `RetryConfigProvider`

### Cold Start Too Long

**Symptom**: First invocation takes 5+ seconds
**Solution**:

- Increase Lambda memory (more vCPU = faster JVM)
- Use Lambda SnapStart (if using Java 11+)
- Consider reserved concurrency

### Secrets Manager Access Denied

**Symptom**: `UnauthorizedException` from Secrets Manager
**Solution**:

1. Verify IAM role has `secretsmanager:GetSecretValue` permission
2. Check secret name matches environment variable
3. Verify secret exists in same region

### JSON Logs Not Appearing

**Symptom**: Logs in plaintext format
**Solution**:

1. Check `LambdaJsonLayout.json` is in `src/main/resources/`
2. Verify log4j2.xml references correct layout
3. Rebuild and redeploy

## Security Considerations

1. **Secrets Manager**: Never hardcode tokens, always use Secrets Manager
2. **IAM Roles**: Use least privilege principle
3. **Logging**: Sensitive data is NOT logged
4. **HTTPS**: External API calls should use HTTPS
5. **Timeout**: Set Lambda timeout appropriately (30-60 seconds)

## Dependencies

| Dependency                 | Version | Purpose            |
|----------------------------|---------|--------------------|
| aws-lambda-java-core       | 1.2.3   | Lambda runtime     |
| aws-lambda-java-events     | 3.11.4  | API Gateway events |
| powertools-logging         | 2.5.0   | JSON logging       |
| powertools-parameters      | 2.5.0   | Secrets Manager    |
| httpclient5                | 5.3     | HTTP requests      |
| resilience4j-retry         | 2.2.0   | Retry logic        |
| jackson-databind           | 2.17.1  | JSON handling      |
| log4j-core                 | 2.23.1  | Logging framework  |
| log4j-layout-template-json | 2.23.1  | JSON layout        |

## Next Steps

1. **Update Configuration**: Set `EXTERNAL_API_URL` and `TOKEN_SECRET_NAME`
2. **Deploy**: Follow AWS Lambda Deployment section
3. **Test**: Use Lambda Testing section
4. **Monitor**: Check CloudWatch Logs and metrics
5. **Optimize**: Adjust memory/timeout based on performance

## Support

For issues or questions:

- Check logs in CloudWatch
- Review IAM permissions
- Verify environment variables
- Test with AWS Lambda Console

## License

Proprietary - BlockChain StableCoin Project

