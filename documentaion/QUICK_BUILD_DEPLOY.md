# ⚡ Quick Build & Deploy Commands

## Your Workflow

```
mvn clean install -DskipTests
    ↓
cd infra/terraform
    ↓
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
    ↓
Test Lambda
```

---

## Quick Commands

### Build JAR (from project root)

```bash
mvn clean install -DskipTests
```

### Deploy to LocalStack (after build)

```bash
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

### Deploy to AWS (after build)

```bash
cd infra/terraform
terraform apply -var-file=terraform.tfvars -auto-approve
```

### Test Lambda

```powershell
# Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"

# Invoke
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json

# View result
Get-Content response.json
```

### Watch Logs

```bash
aws logs tail /aws/lambda/my-token-auth-lambda \
  --follow \
  --endpoint-url http://localhost:4566
```

---

## One-Liner (Build + Deploy)

From project root:

```bash
mvn clean install -DskipTests && cd infra/terraform && terraform apply -var-file=terraform.localstack.tfvars -auto-approve && cd ../../
```

---

## What Terraform Does

1. ✅ Detects JAR has changed
2. ✅ Updates Lambda function code
3. ✅ Keeps all configuration same
4. ✅ Maintains environment variables
5. ✅ Preserves IAM permissions

**Result:** New code deployed in seconds!

---

## File Terraform Uses

```
target/SetUpProject-1.0-SNAPSHOT.jar
     ↑
  Created by Maven
     
infra/terraform/main.tf
     ↑
  Deploys to Lambda
```

---

## Typical Development Loop

```
1. Edit Java code
   ↓
2. Run: mvn clean install -DskipTests
   ↓
3. Run: cd infra/terraform && terraform apply -var-file=terraform.localstack.tfvars -auto-approve
   ↓
4. Test: aws lambda invoke ...
   ↓
5. View logs: aws logs tail ...
   ↓
6. Repeat from step 1 for next change
```

---

**Status:** ✅ Terraform reads JAR from target/ folder automatically

