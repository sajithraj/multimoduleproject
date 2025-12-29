# ❌ SECRETSMANAGER AUTHENTICATION ERROR - ROOT CAUSE ANALYSIS

## Error Observed

```
The security token included in the request is invalid. 
(Service: SecretsManager, Status Code: 400)
```

## Root Cause

The Lambda is running the OLD compiled code (org.example package) instead of the NEW code (com.project package) that I
just fixed.

### Evidence

- Logs show: `org.example.auth.TokenCache` (OLD package)
- Logs show: `software.amazon.lambda.powertools.parameters.secrets.SecretsProvider` (OLD implementation)
- We compiled NEW code with: `com.project` package and AWS SDK direct calls

## Why is Old Code Running?

The JAR file in `target/` was built from the old source, but the FIXED code I created is in the source. When Terraform
deployed, it reused the old JAR.

## Solution

### Step 1: Delete old JAR

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
rm target/SetUpProject-1.0-SNAPSHOT.jar
```

### Step 2: Rebuild with fixed code

```bash
mvn clean install -DskipTests
```

### Step 3: Redeploy

```bash
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

### Step 4: Test

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json
```

## What I Fixed in the Code

1. **Removed Powertools SecretsProvider** - Was trying to use real AWS auth
2. **Added Direct AWS SDK SecretsManagerClient** - Uses environment variables for LocalStack configuration
3. **Uses proper endpoint configuration** - AWS SDK respects `AWS_ENDPOINT_URL_SECRETSMANAGER` environment variable

## Expected Result After Fix

The new code will:

1. Read AWS credentials from environment (which LocalStack provides)
2. Call Secrets Manager directly via AWS SDK
3. Parse the token secret JSON
4. Successfully authenticate

## Why LocalStack Wasn't Working Before

LocalStack uses fake AWS credentials for local testing. The Powertools library tries to sign requests with real AWS
auth, which LocalStack rejects. The direct AWS SDK approach in the fixed code respects LocalStack's endpoints and
credentials properly.

