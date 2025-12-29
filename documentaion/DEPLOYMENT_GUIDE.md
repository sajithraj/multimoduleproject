# AWS Lambda Deployment Guide

Complete step-by-step guide to deploy the Java Lambda function to AWS.

## Prerequisites

- AWS Account with appropriate permissions
- AWS CLI v2 installed and configured
- Maven 3.8+
- Java 21 JDK
- Unix/Linux shell (for deploy.sh) OR PowerShell (for deploy.ps1)

## Quick Start (Automated)

### Option 1: Bash (Linux/macOS)

```bash
# Make script executable
chmod +x deploy.sh

# Run deployment with default settings
./deploy.sh

# Or with custom settings
./deploy.sh \
  --region us-west-2 \
  --external-api-url "https://api.yourdomain.com/v1/endpoint" \
  --token-secret-name "my-custom-token-secret"
```

### Option 2: PowerShell (Windows)

```powershell
# Run deployment with default settings
.\deploy.ps1

# Or with custom settings
.\deploy.ps1 `
  -Region us-west-2 `
  -ExternalApiUrl "https://api.yourdomain.com/v1/endpoint" `
  -TokenSecretName "my-custom-token-secret"
```

## Manual Step-by-Step Deployment

If you prefer manual deployment, follow these steps:

### Step 1: Build the Application

```bash
mvn clean package
```

This creates `target/SetUpProject-1.0-SNAPSHOT.jar` with all dependencies included.

### Step 2: Create IAM Role

```bash
# Create role with trust policy
aws iam create-role \
  --role-name lambda-execution-role \
  --assume-role-policy-document file://trust-policy.json

# Attach CloudWatch Logs policy
aws iam attach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Attach Secrets Manager policy
aws iam put-role-policy \
  --role-name lambda-execution-role \
  --policy-name secrets-access \
  --policy-document file://secrets-policy.json
```

### Step 3: Create Secrets Manager Secret

```bash
aws secretsmanager create-secret \
  --name external-api-token \
  --secret-string '{
    "token": "your_actual_bearer_token_here"
  }'
```

**Important**: Update the token with your actual API token.

### Step 4: Get Role ARN

```bash
ROLE_ARN=$(aws iam get-role \
  --role-name lambda-execution-role \
  --query 'Role.Arn' \
  --output text)

echo "Role ARN: $ROLE_ARN"
```

### Step 5: Create Lambda Function

```bash
aws lambda create-function \
  --function-name external-api-lambda \
  --runtime java21 \
  --role $ROLE_ARN \
  --handler org.example.ApiHandler \
  --timeout 30 \
  --memory-size 512 \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --environment Variables="{
    EXTERNAL_API_URL=https://api.example.com/endpoint,
    TOKEN_SECRET_NAME=external-api-token
  }" \
  --architectures x86_64
```

### Step 6: Test the Function

```bash
aws lambda invoke \
  --function-name external-api-lambda \
  --payload '{"httpMethod":"GET","path":"/test","body":null}' \
  response.json

cat response.json
```

## Configuration After Deployment

### Update Environment Variables

If you need to change the external API URL or secret name:

```bash
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --environment Variables="{
    EXTERNAL_API_URL=https://new-api.example.com/endpoint,
    TOKEN_SECRET_NAME=updated-secret-name
  }"
```

### Update Token Secret

To update the authentication token:

```bash
aws secretsmanager update-secret \
  --secret-id external-api-token \
  --secret-string '{
    "token": "new_token_value_here"
  }'
```

The new token will be used on the next Lambda invocation (within 55 minutes).

### Adjust Memory and Timeout

```bash
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --memory-size 1024 \
  --timeout 60
```

## Integration with API Gateway

### Create REST API

```bash
REST_API_ID=$(aws apigateway create-rest-api \
  --name external-api \
  --description "Lambda-based external API wrapper" \
  --query 'id' \
  --output text)

echo "REST API ID: $REST_API_ID"
```

### Create Resource

```bash
ROOT_RESOURCE_ID=$(aws apigateway get-resources \
  --rest-api-id $REST_API_ID \
  --query 'items[0].id' \
  --output text)

RESOURCE_ID=$(aws apigateway create-resource \
  --rest-api-id $REST_API_ID \
  --parent-id $ROOT_RESOURCE_ID \
  --path-part "{proxy+}" \
  --query 'id' \
  --output text)

echo "Resource ID: $RESOURCE_ID"
```

### Create Method

```bash
aws apigateway put-method \
  --rest-api-id $REST_API_ID \
  --resource-id $RESOURCE_ID \
  --http-method ANY \
  --authorization-type NONE
```

### Integrate with Lambda

```bash
LAMBDA_ARN=$(aws lambda get-function \
  --function-name external-api-lambda \
  --query 'Configuration.FunctionArn' \
  --output text)

aws apigateway put-integration \
  --rest-api-id $REST_API_ID \
  --resource-id $RESOURCE_ID \
  --http-method ANY \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri "arn:aws:apigateway:${AWS_REGION}:lambda:path/2015-03-31/functions/${LAMBDA_ARN}/invocations"
```

### Grant API Gateway Permission to Invoke Lambda

```bash
aws lambda add-permission \
  --function-name external-api-lambda \
  --statement-id AllowAPIGatewayInvoke \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:${AWS_REGION}:${AWS_ACCOUNT_ID}:${REST_API_ID}/*/*"
```

### Deploy API

```bash
DEPLOYMENT_ID=$(aws apigateway create-deployment \
  --rest-api-id $REST_API_ID \
  --stage-name prod \
  --query 'id' \
  --output text)

echo "API Endpoint: https://${REST_API_ID}.execute-api.${AWS_REGION}.amazonaws.com/prod"
```

## Monitoring

### View CloudWatch Logs

```bash
# Tail logs in real-time
aws logs tail /aws/lambda/external-api-lambda --follow

# View specific duration
aws logs filter-log-events \
  --log-group-name /aws/lambda/external-api-lambda \
  --start-time $(date -d '1 hour ago' +%s)000
```

### Search for Errors

```bash
aws logs filter-log-events \
  --log-group-name /aws/lambda/external-api-lambda \
  --filter-pattern "ERROR"
```

### View Metrics

```bash
# Duration (ms)
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=external-api-lambda \
  --start-time 2024-12-27T00:00:00Z \
  --end-time 2024-12-27T23:59:59Z \
  --period 3600 \
  --statistics Average,Maximum,Minimum

# Errors
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Errors \
  --dimensions Name=FunctionName,Value=external-api-lambda \
  --start-time 2024-12-27T00:00:00Z \
  --end-time 2024-12-27T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

## Troubleshooting

### Lambda Invocation Fails with "Access Denied"

**Cause**: IAM role doesn't have Secrets Manager permissions

**Solution**:

```bash
# Verify policy is attached
aws iam get-role-policy \
  --role-name lambda-execution-role \
  --policy-name secrets-access

# Re-apply if missing
aws iam put-role-policy \
  --role-name lambda-execution-role \
  --policy-name secrets-access \
  --policy-document file://secrets-policy.json
```

### Cannot Find Secrets Manager Secret

**Cause**: Secret name mismatch or wrong region

**Solution**:

```bash
# List all secrets
aws secretsmanager list-secrets

# Update Lambda environment variable
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --environment Variables="{TOKEN_SECRET_NAME=correct-secret-name}"
```

### High Cold Start Time (5+ seconds)

**Cause**: Default memory is too low, JVM startup is slow

**Solution**:

```bash
# Increase memory (also increases vCPU)
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --memory-size 1024  # Default is 512

# Consider using Lambda SnapStart for faster startups (requires Java 11+)
```

### Token Not Being Cached (Multiple Secrets Manager Calls)

**Cause**: Token cached for 55 minutes but may be cleared between container reuses

**Solution**:

- This is expected behavior when Lambda container is reclaimed
- Performance should improve after first invocation in same container
- Monitor via CloudWatch metrics

### Timeout Errors

**Cause**: External API taking too long, or network issues

**Solution**:

```bash
# Increase timeout to 60 seconds
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --timeout 60

# Check external API availability
curl -v https://your-api.example.com/endpoint
```

## Cleanup

### Delete Lambda Function

```bash
aws lambda delete-function --function-name external-api-lambda
```

### Delete IAM Role

```bash
# Detach policies
aws iam detach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role-policy \
  --role-name lambda-execution-role \
  --policy-name secrets-access

# Delete role
aws iam delete-role --role-name lambda-execution-role
```

### Delete Secrets Manager Secret

```bash
# Schedule deletion (30-day recovery window)
aws secretsmanager delete-secret \
  --secret-id external-api-token \
  --recovery-window-in-days 30

# Or immediate deletion (not recommended)
aws secretsmanager delete-secret \
  --secret-id external-api-token \
  --force-delete-without-recovery
```

## Cost Optimization

### Reserved Concurrency

If you expect consistent traffic, consider reserved concurrency:

```bash
aws lambda put-function-concurrency \
  --function-name external-api-lambda \
  --reserved-concurrent-executions 100
```

### Provisioned Concurrency

For predictable, sustained traffic:

```bash
aws lambda put-provisioned-concurrency-config \
  --function-name external-api-lambda \
  --provisioned-concurrent-executions 50
```

## Security Best Practices

1. **Never store tokens in code** - Always use Secrets Manager
2. **Principle of least privilege** - Restrict IAM permissions to only needed actions
3. **Enable VPC** - If accessing private resources, deploy Lambda in VPC
4. **Use HTTPS** - All external API calls should use HTTPS
5. **Rotate tokens regularly** - Update secret every 90 days
6. **Monitor logs** - Review CloudWatch Logs for suspicious activity
7. **Version control** - Keep source code in private Git repository

## Performance Tuning

### Cold Start Optimization

- Increase Lambda memory (512-1024MB is optimal for Java)
- Use Lambda SnapStart (available for Java 11+)
- Consider keeping function warm with scheduled invocations

### Warm Start Optimization

- Token caching (55 minutes): Reduces Secrets Manager calls by ~99%
- HTTP connection pooling: Reuses connections across invocations
- Container reuse: Multiple invocations share same JVM

### Expected Performance

| Scenario                     | Duration                                  |
|------------------------------|-------------------------------------------|
| Cold start                   | 2-3 seconds                               |
| Warm start (cached token)    | 50-100ms                                  |
| Token refresh (every 55 min) | 200-300ms                                 |
| External API call            | Depends on API latency + 10-30ms overhead |

## Additional Resources

- [AWS Lambda Java Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html)
- [AWS Lambda Python Examples](https://docs.aws.amazon.com/lambda/latest/dg/lambda-python.html)
- [Powertools for AWS Lambda (Java)](https://docs.powertools.aws.dev/lambda/java/latest/)
- [AWS Secrets Manager User Guide](https://docs.aws.amazon.com/secretsmanager/latest/userguide/)
- [API Gateway with Lambda Proxy Integration](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html)

