# 📋 AWS Deployment Quick Card

## When You're Ready to Deploy to AWS

### Step 1: Get AWS Account

```
Go to: aws.amazon.com
Create account
Get Access Key ID and Secret Access Key
```

### Step 2: Update Terraform Variables

Edit: `infra/terraform/terraform.tfvars`

Change from:

```terraform
use_localstack = true
environment    = "dev-local"
```

To:

```terraform
use_localstack = false
environment    = "prod"
client_id      = "your-real-client-id"
client_secret  = "your-real-client-secret"
```

### Step 3: Configure AWS CLI

```powershell
aws configure

# When prompted, enter:
# AWS Access Key ID: [your key]
# AWS Secret Access Key: [your secret]
# Default region: us-east-1
# Default output format: json
```

### Step 4: Build & Deploy

```bash
mvn clean install -DskipTests
cd infra/terraform
terraform apply -var-file=terraform.tfvars -auto-approve
```

### Step 5: Test on AWS

```powershell
# No --endpoint-url needed!

aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json

Get-Content response.json
```

---

## That's It!

Your Lambda is now running on AWS! 🚀

---

## Remember

✅ Same JAR
✅ Same Terraform code
✅ Same Lambda function
✅ Only tfvars + credentials change

---

## Cost

~ $0.26/month for typical usage (very affordable!)

---

## To Go Back to LocalStack

```bash
cd infra/terraform
terraform apply -var-file=terraform.localstack.tfvars -auto-approve
```

---

**Status:** Ready to deploy to AWS anytime!

