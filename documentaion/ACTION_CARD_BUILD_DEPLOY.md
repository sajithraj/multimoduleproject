# ⚡ ACTION CARD - Build & Deploy

## Do This Every Time You Change Code

### Step 1: Build JAR (2-3 minutes)

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

**Wait for:**

```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXXs
```

---

### Step 2: Deploy to Lambda (30 seconds)

```powershell
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

**Wait for:**

```
aws_lambda_function.token_auth_lambda: Modifications complete...
Apply complete! Resources: 0 added, 1 changed, 0 destroyed.
```

---

### Step 3: Test (10 seconds)

```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"

aws lambda invoke `
  --function-name my-token-auth-lambda `
  --payload '{}' `
  --endpoint-url http://localhost:4566 `
  response.json

Get-Content response.json
```

---

## That's It!

Your code is now deployed to Lambda! ✅

---

## One-Line Build + Deploy

From project root:

```powershell
mvn clean install -DskipTests && cd infra/terraform && terraform apply -var-file=terraform.localstack.tfvars -auto-approve && cd ../../
```

---

## Watch Logs (Optional)

```powershell
aws logs tail /aws/lambda/my-token-auth-lambda --follow --endpoint-url http://localhost:4566
```

---

**Time to Deploy: ~2-3 minutes total**

**Everything Else: Automated by Terraform ✅**

