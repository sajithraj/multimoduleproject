# 🚀 Manual Build & Deploy Workflow

## Overview

You will manually run Maven build, then Terraform will automatically deploy from the target folder.

---

## Workflow

```
1. Run Maven build                    (you do this)
   └─ mvn clean install -DskipTests
   └─ Creates: target/SetUpProject-1.0-SNAPSHOT.jar

2. Run Terraform apply               (detects JAR changes automatically)
   └─ terraform apply -var-file=terraform.localstack.tfvars
   └─ Updates Lambda with new JAR

3. Test Lambda function              (via AWS CLI or Lambda console)
   └─ aws lambda invoke ...
```

---

## Step 1: Build Maven Project

Navigate to project root and build:

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

mvn clean install -DskipTests
```

**Expected Output:**

```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXXs
[INFO] Finished at: 2025-12-27T...
```

**Verifies:**

```
✅ JAR created: target/SetUpProject-1.0-SNAPSHOT.jar
✅ Ready for deployment
```

---

## Step 2: Deploy with Terraform

Navigate to Terraform directory and apply:

```bash
cd infra/terraform

terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

**What Terraform Does:**

1. Detects JAR file has changed (via source_code_hash)
2. Updates Lambda function code
3. Redeploys with new JAR
4. Maintains environment variables
5. Keeps Secrets Manager secret
6. Keeps IAM role & permissions

**Expected Output:**

```
aws_lambda_function.token_auth_lambda: Modifying...
aws_lambda_function.token_auth_lambda: Modifications complete

Apply complete! Resources: 0 added, 1 changed, 0 destroyed.
```

---

## Step 3: Test Lambda Function

Invoke your updated Lambda:

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json

Get-Content response.json
```

---

## Complete Script (One-Liner)

You can combine all steps into one command:

```bash
# Build and Deploy (from project root)
mvn clean install -DskipTests && cd infra/terraform && terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

Or create a script file:

**build-and-deploy.bat** (Windows):

```batch
@echo off
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

echo ===== STEP 1: Maven Build =====
mvn clean install -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

echo.
echo ===== STEP 2: Terraform Deploy =====
cd infra\terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve

echo.
echo ===== COMPLETE =====
echo Lambda function updated with new code!
pause
```

**build-and-deploy.sh** (Mac/Linux):

```bash
#!/bin/bash

cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

echo "===== STEP 1: Maven Build ====="
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "===== STEP 2: Terraform Deploy ====="
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve

echo ""
echo "===== COMPLETE ====="
echo "Lambda function updated with new code!"
```

---

## How Terraform Detects Changes

Terraform uses `source_code_hash` to detect when JAR file changes:

```terraform
source_code_hash = filebase64sha256("${path.module}/../../target/SetUpProject-1.0-SNAPSHOT.jar")
```

**How it works:**

1. Calculates SHA256 hash of JAR file
2. Stores hash in terraform.tfstate
3. On next `terraform apply`, recalculates hash
4. If hash differs, detects change
5. Updates Lambda function automatically

**Result:** You don't need to specify `--force-update` or anything - Terraform just knows!

---

## File Structure

```
Project Root
├── src/
│   └── main/java/com/project/...    ← Your Java code
├── target/
│   ├── SetUpProject-1.0-SNAPSHOT.jar  ← JAR created by Maven (Terraform uses this)
│   └── ... (other build artifacts)
└── infra/
    └── terraform/
        └── main.tf                    ← Terraform reads JAR path from here
```

---

## Important Notes

### ✅ Do This:

- Run `mvn clean install -DskipTests` whenever you change Java code
- Run `terraform apply` after Maven build
- Make sure JAR is in `target/` directory

### ❌ Don't Do This:

- Don't manually upload JAR to Lambda console
- Don't copy JAR to different location
- Don't rename JAR file

### 💡 Tips:

- Always do full build (`clean install`, not just `install`)
- Use `-DskipTests` to build faster (tests still run on CI/CD)
- Terraform handles everything else automatically

---

## Typical Development Cycle

### First Time Setup (Already Done):

```bash
mvn clean install -DskipTests
cd infra/terraform
terraform init
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

### Code Change Cycle (Repeat for each change):

```bash
# Make code changes in src/
# Then:
mvn clean install -DskipTests              # Step 1: Build
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve   # Step 2: Deploy

# Test via AWS CLI or Lambda console
aws lambda invoke ... --endpoint-url http://localhost:4566
```

---

## Verify JAR Deployment

Check that Lambda is using latest JAR:

```bash
# Get Lambda configuration
aws lambda get-function-configuration \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566

# Look for "LastModified" timestamp - should be recent
```

---

## Troubleshooting

### JAR Not Found

```
Error: open target/SetUpProject-1.0-SNAPSHOT.jar: no such file or directory
```

**Fix:** Run `mvn clean install -DskipTests` from project root

### Terraform Says No Changes

```
No changes. Infrastructure is up-to-date.
```

**Reason:** JAR hash hasn't changed, so Lambda doesn't need update
**Fix:** Modify code, rebuild JAR, then apply again

### Lambda Still Using Old Code

```
Invoke test shows old behavior
```

**Fix:**

1. Run Maven build
2. Run `terraform apply`
3. Wait 5 seconds for Lambda to start up
4. Test again

---

## QuickStart Commands

```bash
# Single command build + deploy
mvn clean install -DskipTests && cd infra/terraform && terraform apply -var-file=terraform.localstack.tfvars -auto-approve && cd ../../

# Just build
mvn clean install -DskipTests

# Just deploy (after build)
cd infra/terraform && terraform apply -var-file=terraform.localstack.tfvars -auto-approve

# Just test
aws lambda invoke --function-name my-token-auth-lambda --payload '{}' --endpoint-url http://localhost:4566 response.json
```

---

## Production Deployment

When ready to deploy to AWS:

```bash
# Build
mvn clean install -DskipTests

# Deploy to AWS (same JAR, different Terraform config)
cd infra/terraform
terraform apply -var-file=terraform.tfvars -auto-approve
```

**Note:** Same JAR works for both LocalStack and AWS!

---

**Status:** ✅ Ready for manual builds with automatic Terraform deployment

