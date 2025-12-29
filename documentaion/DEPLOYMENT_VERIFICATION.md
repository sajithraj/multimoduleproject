# ✅ DEPLOYMENT VERIFICATION CHECKLIST

## Pre-Deployment Verification

Run this checklist before deploying to ensure everything is in place.

### 1. Code Compilation ✅

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

**Check:**

- [ ] Build completes successfully
- [ ] JAR file created: `target/SetUpProject-1.0-SNAPSHOT.jar`
- [ ] JAR size: ~2-3 MB
- [ ] No compilation errors

**Command to verify:**

```bash
dir target\*.jar
```

---

### 2. Terraform Configuration ✅

**Check files exist:**

- [ ] `infra/terraform/main.tf` - Updated with API Gateway
- [ ] `infra/terraform/terraform.localstack.tfvars` - Configuration values
- [ ] `infra/terraform/.terraform` - Initialized (after init)

**Command to verify:**

```bash
cd infra/terraform
terraform validate
```

---

### 3. Java Source Code ✅

**Check new files exist:**

- [ ] `src/main/java/com/project/services/token/TokenService.java`
- [ ] `src/main/java/com/project/services/token/TokenCache.java`
- [ ] `src/main/java/com/project/services/token/TokenAuthorizationService.java`
- [ ] `src/main/java/com/project/services/token/dto/TokenResponse.java`
- [ ] `src/main/java/com/project/services/api/dto/ExternalApiRequest.java`
- [ ] `src/main/java/com/project/services/api/dto/ExternalApiResponse.java`

**Command to verify:**

```powershell
ls -Recurse src/main/java/com/project/services/
```

---

### 4. Documentation ✅

**Check files exist:**

- [ ] `DOCUMENTATION_INDEX.md`
- [ ] `README_START_HERE.md`
- [ ] `IMPLEMENTATION_COMPLETE.md`
- [ ] `COMPLETE_IMPLEMENTATION_GUIDE.md`
- [ ] `NEW_STRUCTURE_GUIDE.md`
- [ ] `API_GATEWAY_CONTRACT.md`
- [ ] `FRESH_DEPLOYMENT_SUMMARY.md`
- [ ] `FINAL_SUMMARY.md`
- [ ] `CHANGES_SUMMARY.md`

**Command to verify:**

```powershell
ls *.md
```

---

### 5. Docker & LocalStack ✅

**Check Docker:**

- [ ] Docker is running
- [ ] LocalStack container running
- [ ] Port 4566 accessible

**Commands to verify:**

```bash
docker ps
curl http://localhost:4566/health
```

---

### 6. AWS CLI & Terraform ✅

**Check tools:**

- [ ] AWS CLI installed and configured
- [ ] Terraform installed and in PATH
- [ ] Both can access LocalStack

**Commands to verify:**

```bash
aws --version
terraform --version
aws s3 ls --endpoint-url http://localhost:4566
```

---

### 7. Environment Variables ✅

**Check set in current session:**

- [ ] `AWS_ACCESS_KEY_ID=test`
- [ ] `AWS_SECRET_ACCESS_KEY=test`
- [ ] `AWS_DEFAULT_REGION=us-east-1`

**Command to verify:**

```powershell
$env:AWS_ACCESS_KEY_ID
$env:AWS_SECRET_ACCESS_KEY
$env:AWS_DEFAULT_REGION
```

---

## Deployment Steps

### Step 1: Build

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

**Verify:** JAR file created without errors

---

### Step 2: Deploy Infrastructure

```bash
cd infra/terraform

# Initialize (if needed)
terraform init

# Plan (review changes)
terraform plan -var-file=terraform.localstack.tfvars

# Apply
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

**Verify:** All 7 resources created successfully

---

### Step 3: Verify Deployment

```bash
# Check resources created
aws lambda list-functions --endpoint-url http://localhost:4566
aws apigateway get-rest-apis --endpoint-url http://localhost:4566
aws secretsmanager list-secrets --endpoint-url http://localhost:4566
```

**Verify:** Lambda, API Gateway, Secrets Manager all accessible

---

### Step 4: Test Lambda Directly

```bash
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

cat response.json
```

**Verify:** Response received (check for errors in logs)

---

### Step 5: Test via API Gateway

```bash
# Get API Gateway endpoint from Terraform output
$API_URL = terraform output -raw api_gateway_endpoint

# Test
curl -X POST $API_URL `
  -H "Content-Type: application/json" `
  -d '{}'
```

**Verify:** HTTP response received with proper status code

---

### Step 6: Check Logs

```bash
aws logs tail /aws/lambda/my-token-auth-lambda `
  --endpoint-url http://localhost:4566 `
  --since 10m
```

**Verify:** Logs show successful execution or indicate errors

---

## Troubleshooting

### Build Fails

**Error:** Compilation failure
**Solution:**

1. Check Java files for syntax errors
2. Ensure all imports are correct
3. Run `mvn clean install` again

### Terraform Apply Fails

**Error:** Resource already exists
**Solution:**

1. Run `terraform destroy` to clean up
2. Delete `terraform.tfstate*` files
3. Run `terraform init` and `apply` again

### Lambda Invocation Fails

**Error:** Function not found
**Solution:**

1. Check Lambda function name in deployment
2. Verify endpoint URL format
3. Check AWS credentials are set

### API Gateway Endpoint Not Working

**Error:** 404 or connection refused
**Solution:**

1. Verify API Gateway deployment
2. Check endpoint URL from Terraform output
3. Verify resource path is `/api/auth`

### Logs Show Errors

**Error:** Token fetch failed, connection timeout
**Solution:**

1. Verify Secrets Manager has credentials
2. Check external API endpoint is accessible
3. Review error message for specific issue

---

## Post-Deployment Verification

### ✅ Verify All Resources Created

```powershell
Write-Host "=== VERIFICATION ===" -ForegroundColor Green

# 1. Check Lambda
aws lambda get-function `
  --function-name my-token-auth-lambda `
  --endpoint-url http://localhost:4566 `
  --query 'Configuration.FunctionName'

# 2. Check API Gateway
aws apigateway get-rest-apis `
  --endpoint-url http://localhost:4566 `
  --query 'items[0].name'

# 3. Check Secrets Manager
aws secretsmanager list-secrets `
  --endpoint-url http://localhost:4566 `
  --query 'SecretList[0].Name'

# 4. Check CloudWatch Logs
aws logs describe-log-groups `
  --endpoint-url http://localhost:4566 `
  --query 'logGroups[0].logGroupName'
```

---

## Success Criteria

- ✅ JAR builds without errors
- ✅ Terraform deploys 7 resources
- ✅ Lambda function accessible
- ✅ API Gateway endpoint responsive
- ✅ Secrets Manager stores credentials
- ✅ CloudWatch logs show execution
- ✅ No errors in logs
- ✅ Token caching works
- ✅ API calls succeed

---

## Rollback (If Needed)

```bash
# Destroy all resources
terraform destroy -var-file=terraform.localstack.tfvars -auto-approve

# Clean up state
rm terraform.tfstate*
rm -r .terraform
```

---

## Performance Metrics

After deployment, check these metrics:

| Metric            | Expected      |
|-------------------|---------------|
| Lambda Invocation | < 2 seconds   |
| Warm Invocation   | < 1.2 seconds |
| API Call          | < 2 seconds   |
| Token Cache Hit   | < 100ms       |
| Cold Start        | ~3 seconds    |

---

## Documentation Reference

For detailed information, see:

- `API_GATEWAY_CONTRACT.md` - API endpoints
- `COMPLETE_IMPLEMENTATION_GUIDE.md` - Implementation details
- `NEW_STRUCTURE_GUIDE.md` - Folder structure
- `TROUBLESHOOTING.md` - Common issues (if available)

---

## Ready for Production?

Before going to production AWS:

1. [ ] Update credentials in Secrets Manager
2. [ ] Change token endpoint to production
3. [ ] Update API endpoint to production
4. [ ] Enable API key authentication
5. [ ] Enable request logging
6. [ ] Set up CloudWatch alarms
7. [ ] Update Lambda timeout based on API latency
8. [ ] Test load scenarios
9. [ ] Set up CI/CD pipeline
10. [ ] Document runbook

---

**Deployment Ready: ✅ YES**

Next: Run `mvn clean install` then `terraform apply` 🚀

