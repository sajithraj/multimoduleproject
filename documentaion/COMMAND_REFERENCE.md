# 📋 LOCALSTACK DEPLOYMENT - COMMAND REFERENCE

## Commands You Just Ran ✅

```bash
# 1. Build
mvn clean install -DskipTests
→ Result: 24.39 MB JAR in target/

# 2. Deploy Infrastructure
cd infra/terraform
terraform plan -var-file=terraform.localstack.tfvars
terraform apply -var-file=terraform.localstack.tfvars
→ Result: All resources deployed to LocalStack

# 3. Test Lambda
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json
→ Result: Lambda executed successfully
```

---

## Most Useful Commands (Bookmark These!)

### Test Lambda Again (Warm Invocation)

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response_warm.json
```

### Watch Logs Live

```bash
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow \
  --endpoint-url http://localhost:4566
```

### Get Recent Logs

```bash
aws logs tail /aws/lambda/my-token-auth-lambda \
  --endpoint-url http://localhost:4566 \
  --max-items 50
```

### See Response

```bash
cat response.json
```

### Rebuild & Redeploy (After Code Changes)

```bash
mvn clean install -DskipTests && \
cd infra/terraform && \
terraform apply -var-file=terraform.localstack.tfvars -auto-approve && \
cd ../../
```

---

## Useful Info Commands

### Check Lambda Function

```bash
aws lambda get-function-configuration \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566
```

### Check Secret

```bash
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566
```

### List All Functions

```bash
aws lambda list-functions \
  --endpoint-url http://localhost:4566
```

### List Secrets

```bash
aws secretsmanager list-secrets \
  --endpoint-url http://localhost:4566
```

---

## Development Cycle

### When You Make Code Changes

```bash
# Step 1: Build
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests

# Step 2: Deploy
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve

# Step 3: Test
cd ../../
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json

# Step 4: Check Response
cat response.json

# Step 5: View Logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --endpoint-url http://localhost:4566 \
  --max-items 20
```

---

## Deploy to AWS (When Ready)

```bash
# Update Terraform variables
cd infra/terraform

# Edit terraform.tfvars with real AWS credentials

# Deploy to AWS
terraform apply -var-file=terraform.tfvars -auto-approve

# Test on AWS
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response_aws.json

# View AWS logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow
```

---

## Troubleshooting

### Lambda Invocation Failed?

```bash
# Check logs
aws logs tail /aws/lambda/my-token-auth-lambda \
  --endpoint-url http://localhost:4566 \
  --max-items 50

# Check function
aws lambda get-function-configuration \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566
```

### Need to Redeploy?

```bash
# Go to terraform directory
cd infra/terraform

# Reapply
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

### Check LocalStack Status

```bash
docker ps | grep localstack
```

### Restart LocalStack

```bash
docker-compose -f infra/docker/docker-compose.yml down
docker-compose -f infra/docker/docker-compose.yml up -d
```

---

## Key Features to Test

### 1. OAuth2 Token Caching

```bash
# First invocation (fetches token)
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response1.json

# Second invocation (uses cached token - FASTER!)
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response2.json
```

### 2. Automatic Retry

```bash
# Already built in - triggers on API failures
# Watch logs to see retry attempts
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
```

### 3. Structured Logging

```bash
# View JSON formatted logs
aws logs tail /aws/lambda/my-token-auth-lambda --endpoint-url http://localhost:4566 --max-items 50
```

---

## Environment

```
LocalStack: http://localhost:4566
AWS Region: us-east-1
Function: my-token-auth-lambda
Handler: com.project.ApiHandler::handleRequest
Runtime: Java 21
```

---

## Files Modified by Terraform

```
Created:
├─ Lambda Function
├─ IAM Role
├─ IAM Policies
├─ Secrets Manager Secret
└─ CloudWatch Log Group
```

---

## Quick Status Check

```bash
# All in one command
echo "Lambda:" && \
aws lambda list-functions --endpoint-url http://localhost:4566 --query 'Functions[].FunctionName' --output text && \
echo "" && \
echo "Secrets:" && \
aws secretsmanager list-secrets --endpoint-url http://localhost:4566 --query 'SecretList[].Name' --output text && \
echo "" && \
echo "Log Groups:" && \
aws logs describe-log-groups --endpoint-url http://localhost:4566 --query 'logGroups[].logGroupName' --output text
```

---

## Environment Variables in Lambda

```
TOKEN_SECRET_NAME = external-api/token
AWS_REGION = us-east-1
ENVIRONMENT = dev-local
```

Available in your Java code via:

```java
System.getenv("TOKEN_SECRET_NAME")
```

---

## Remember

- ✅ LocalStack must be running (Docker)
- ✅ JAR must be in `target/` folder
- ✅ Always use `--endpoint-url http://localhost:4566` for LocalStack
- ✅ Don't use endpoint URL for real AWS commands
- ✅ Same Terraform code works for both LocalStack and AWS

---

## Need to Reset?

```bash
# Destroy LocalStack infrastructure
cd infra/terraform
terraform destroy -var-file=terraform.localstack.tfvars -auto-approve

# Restart LocalStack
docker-compose -f ../../docker/docker-compose.yml down
docker-compose -f ../../docker/docker-compose.yml up -d

# Redeploy
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

---

**You're all set! Copy & paste these commands as needed.** 📋

