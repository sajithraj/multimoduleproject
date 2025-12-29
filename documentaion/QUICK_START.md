# Quick Start Guide

Get your Java Lambda function deployed in 5 minutes.

## 📋 Before You Start

You need:

- AWS Account with permissions
- AWS CLI v2 installed: `aws --version`
- Maven installed: `mvn --version`
- Java 21 JDK installed: `java -version`

## ⚡ 5-Minute Quick Start

### 1. Build the Project (1 min)

```bash
cd SetUpProject
mvn clean package
```

Expected output: `SetUpProject-1.0-SNAPSHOT.jar` created in `target/` folder

### 2. Set AWS Region (30 sec)

```bash
# Choose your region (default: us-east-1)
export AWS_REGION=us-east-1
export AWS_DEFAULT_REGION=us-east-1

# Verify AWS CLI is configured
aws sts get-caller-identity
```

### 3. Create Secrets (1 min)

```bash
# Store your API token in AWS Secrets Manager
aws secretsmanager create-secret \
  --name external-api-token \
  --secret-string '{"token":"YOUR_API_TOKEN_HERE"}'

# Verify it was created
aws secretsmanager describe-secret --secret-id external-api-token
```

### 4. Deploy (2-3 min)

Choose your OS:

**Linux/macOS (Bash)**

```bash
chmod +x deploy.sh
./deploy.sh
```

**Windows (PowerShell)**

```powershell
.\deploy.ps1
```

The script will:

- Create IAM role with required permissions
- Deploy Lambda function
- Test the function
- Display the function ARN and endpoint

### 5. Update Configuration (30 sec)

After deployment, update the external API endpoint:

```bash
# Set the actual external API URL
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --environment Variables="{
    EXTERNAL_API_URL=https://your-api.example.com/endpoint,
    TOKEN_SECRET_NAME=external-api-token
  }"
```

## ✅ Verify Deployment

```bash
# Test the Lambda function
aws lambda invoke \
  --function-name external-api-lambda \
  --payload '{"httpMethod":"GET","path":"/test"}' \
  response.json

# View the response
cat response.json

# Check logs
aws logs tail /aws/lambda/external-api-lambda --follow
```

Expected response (HTTP 200):

```json
{
  "statusCode": 200,
  "body": "{...external API response...}",
  "headers": {
    "Content-Type": "application/json"
  }
}
```

## 🔗 Create API Gateway (Optional)

To make your Lambda callable from HTTP:

```bash
# Create REST API
API_ID=$(aws apigateway create-rest-api \
  --name external-api-lambda \
  --query 'id' \
  --output text)

# Get root resource
ROOT=$(aws apigateway get-resources \
  --rest-api-id $API_ID \
  --query 'items[0].id' \
  --output text)

# Create resource
RESOURCE=$(aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $ROOT \
  --path-part '{proxy+}' \
  --query 'id' \
  --output text)

# Create method
aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE \
  --http-method ANY \
  --authorization-type NONE

# Get Lambda ARN
LAMBDA_ARN=$(aws lambda get-function-arn \
  --function-name external-api-lambda \
  --query 'FunctionArn' \
  --output text)

# Integrate with Lambda
aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $RESOURCE \
  --http-method ANY \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri "arn:aws:apigateway:$AWS_REGION:lambda:path/2015-03-31/functions/$LAMBDA_ARN/invocations"

# Grant permission
aws lambda add-permission \
  --function-name external-api-lambda \
  --statement-id AllowAPIGateway \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com

# Deploy API
DEPLOY=$(aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name prod \
  --query 'id' \
  --output text)

echo "API Endpoint: https://$API_ID.execute-api.$AWS_REGION.amazonaws.com/prod"
```

## 📊 Monitor Your Lambda

```bash
# View real-time logs
aws logs tail /aws/lambda/external-api-lambda --follow

# Search for errors
aws logs filter-log-events \
  --log-group-name /aws/lambda/external-api-lambda \
  --filter-pattern "ERROR"

# View metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=external-api-lambda \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum
```

## 🚨 Common Issues

### "Access Denied" when creating role

**Solution**: You need AWS IAM permissions. Use an admin user or role.

### Secret not found

**Solution**: Verify secret name matches environment variable:

```bash
aws secretsmanager list-secrets --query 'SecretList[].Name'
```

### Lambda timeout

**Solution**: Increase timeout and memory:

```bash
aws lambda update-function-configuration \
  --function-name external-api-lambda \
  --timeout 60 \
  --memory-size 1024
```

### JAR not found after build

**Solution**: Check Maven build completed successfully:

```bash
ls -lh target/SetUpProject-1.0-SNAPSHOT.jar
```

## 📚 Learn More

- **Full README**: `cat README.md`
- **Architecture Details**: `cat ARCHITECTURE.md`
- **Deployment Guide**: `cat DEPLOYMENT_GUIDE.md`
- **Checklist**: `cat DEPLOYMENT_CHECKLIST.md`

## 🎯 Next Steps

1. ✅ Review logs in CloudWatch
2. ✅ Test token caching (wait for second invocation)
3. ✅ Create API Gateway integration
4. ✅ Set up CloudWatch alarms
5. ✅ Configure production token secret

## 💡 Tips

**Cold Start Too Slow?**
→ Increase Lambda memory from 512 to 1024 MB

**Want Faster Deployments?**
→ Update code:
`aws lambda update-function-code --function-name external-api-lambda --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar`

**Need to View Live Logs?**
→ Use: `aws logs tail /aws/lambda/external-api-lambda --follow`

**Forgot Function Name?**
→ Check: `aws lambda list-functions --query 'Functions[].FunctionName'`

## 🔒 Security Reminder

1. **Never hardcode tokens** - Always use Secrets Manager ✓
2. **Keep secret safe** - Use strong random tokens ✓
3. **Review IAM permissions** - Only needed permissions ✓
4. **Rotate tokens regularly** - Every 90 days recommended ✓

---

**Congratulations!** Your Lambda is now running! 🎉

For detailed instructions, see **DEPLOYMENT_GUIDE.md**

