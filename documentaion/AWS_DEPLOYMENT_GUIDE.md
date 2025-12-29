# 🚀 AWS Lambda Deployment Guide

## Overview

Once you build with Maven locally, you'll deploy to AWS Lambda using the same Terraform code. This guide explains the
complete process.

---

## Step-by-Step: From Local to AWS

### Phase 1: Local Development (What You Do Now)

```
LocalStack (Docker)
    ↓
1. Run: mvn clean install -DskipTests
   └─ Creates: target/SetUpProject-1.0-SNAPSHOT.jar
    ↓
2. Run: terraform apply -var-file=terraform.localstack.tfvars
   └─ Deploys to LocalStack on port 4566
    ↓
3. Test locally
   └─ aws lambda invoke ... --endpoint-url http://localhost:4566
```

---

### Phase 2: Deploy to AWS (When Ready)

#### Step 1: Update Terraform Variables

Change file: `infra/terraform/terraform.tfvars`

```terraform
# Current (LocalStack)
use_localstack = true
aws_region     = "us-east-1"
environment = "dev-local"

# Change to (AWS Production)
use_localstack = false        # ← Change this to false
aws_region = "us-east-1"  # Your preferred region
environment = "prod"       # Your environment name
client_id = "..." # Your real client ID
client_secret = "..." # Your real client secret
```

#### Step 2: Configure AWS Credentials

Option A - Using AWS CLI:

```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter your default region
# Enter your default output format
```

Option B - Using Environment Variables:

```powershell
$env:AWS_ACCESS_KEY_ID = "your-aws-access-key"
$env:AWS_SECRET_ACCESS_KEY = "your-aws-secret-key"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

#### Step 3: Build JAR (Same as before)

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean install -DskipTests
```

#### Step 4: Deploy to AWS

```bash
cd infra/terraform

# See what will be created
terraform plan -var-file=terraform.tfvars

# Deploy to AWS
terraform apply -var-file=terraform.tfvars
```

#### Step 5: Verify in AWS

```bash
# List Lambda functions
aws lambda list-functions

# Get your function details
aws lambda get-function --function-name my-token-auth-lambda

# View CloudWatch logs
aws logs tail /aws/lambda/my-token-auth-lambda --follow
```

#### Step 6: Test in AWS

```powershell
# Same as LocalStack, but WITHOUT --endpoint-url parameter

aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json

Get-Content response.json
```

---

## Key Differences: LocalStack vs AWS

### LocalStack (Development)

```
Endpoint:        http://localhost:4566
Credentials:     test / test
Region:          us-east-1
Environment:     dev-local
Terraform vars:  terraform.localstack.tfvars
use_localstack:  true

Commands include: --endpoint-url http://localhost:4566
```

### AWS (Production)

```
Endpoint:        AWS API (automatic)
Credentials:     Your real AWS credentials
Region:          Your preferred region
Environment:     prod
Terraform vars:  terraform.tfvars
use_localstack:  false

Commands: No --endpoint-url parameter needed
```

---

## Complete Comparison

| Aspect             | LocalStack                       | AWS                  |
|--------------------|----------------------------------|----------------------|
| **Infrastructure** | Docker container on your machine | Real AWS cloud       |
| **Cost**           | Free                             | Pay per invocation   |
| **Latency**        | Very fast (local)                | Normal (AWS network) |
| **Scaling**        | Not needed                       | Automatic            |
| **Use Case**       | Development & testing            | Production           |
| **Data**           | Test data                        | Real data            |
| **Terraform vars** | terraform.localstack.tfvars      | terraform.tfvars     |
| **Credentials**    | test/test                        | Your AWS credentials |

---

## Terraform Configuration Details

Your `main.tf` supports both LocalStack and AWS dynamically:

### Provider Configuration

```terraform
provider "aws" {
  region = var.aws_region

  # LocalStack endpoints (only if use_localstack = true)
  dynamic "endpoints" {
    for_each = var.use_localstack ? [1] : []
    content {
      secretsmanager = "http://localhost:4566"
      iam            = "http://localhost:4566"
      lambda         = "http://localhost:4566"
      logs           = "http://localhost:4566"
    }
  }

  # Skip validation for LocalStack only
  dynamic "skip_credentials_validation" {
    for_each = var.use_localstack ? [true] : []
    content {
      skip_credentials_validation = true
    }
  }
}
```

**How it works:**

- If `use_localstack = true` → Points to LocalStack on port 4566
- If `use_localstack = false` → Points to real AWS

**Result:** Same Terraform code works for both!

---

## IAM Permissions Needed on AWS

When deploying to AWS, make sure your AWS credentials have these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "lambda:CreateFunction",
        "lambda:UpdateFunctionCode",
        "lambda:GetFunction",
        "lambda:GetFunctionConfiguration"
      ],
      "Resource": "arn:aws:lambda:*:*:function/my-token-auth-lambda"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:GetRole",
        "iam:AttachRolePolicy",
        "iam:PutRolePolicy"
      ],
      "Resource": "arn:aws:iam::*:role/lambda-execution-role-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:CreateSecret",
        "secretsmanager:GetSecretValue",
        "secretsmanager:PutSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:external-api/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:DescribeLogStreams"
      ],
      "Resource": "arn:aws:logs:*:*:log-group:/aws/lambda/*"
    }
  ]
}
```

Or simply use AWS managed policy: `AdministratorAccess` (for testing only)

---

## Deployment Checklist

### Before Deploying to AWS

- [ ] AWS account created
- [ ] AWS credentials configured
- [ ] Real OAuth2 credentials obtained
- [ ] Updated terraform.tfvars with real values
- [ ] Tested locally with LocalStack first
- [ ] JAR built successfully

### During Deployment

- [ ] Ran `terraform plan` to review
- [ ] Reviewed changes before applying
- [ ] Ran `terraform apply`
- [ ] Resources created successfully

### After Deployment

- [ ] Function appears in Lambda console
- [ ] Secrets Manager secret created
- [ ] IAM role created and attached
- [ ] CloudWatch log group created
- [ ] Lambda can be invoked

---

## Troubleshooting AWS Deployment

### Error: "InvalidParameterValueException: The role is invalid or cannot be assumed."

**Fix:** Wait 10-15 seconds after IAM role creation before using it (IAM eventual consistency)

### Error: "AccessDenied: User is not authorized to perform: lambda:CreateFunction"

**Fix:** Your AWS credentials don't have Lambda permissions. Add them to your IAM user.

### Error: "Unable to download jar from S3"

**Fix:** JAR file must be in `target/` directory locally. Run `mvn clean install` first.

### Lambda Still Using Old Code

**Fix:**

1. Rebuild JAR: `mvn clean install -DskipTests`
2. Run Terraform: `terraform apply -var-file=terraform.tfvars`
3. Wait 5 seconds for Lambda to update
4. Test again

---

## Monitoring AWS Lambda

### View Recent Invocations

```bash
aws lambda list-functions
aws lambda get-function-configuration --function-name my-token-auth-lambda
```

### View CloudWatch Logs

```bash
# Real-time logs
aws logs tail /aws/lambda/my-token-auth-lambda --follow

# Last 100 entries
aws logs tail /aws/lambda/my-token-auth-lambda --max-items 100
```

### View Metrics

```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=my-token-auth-lambda \
  --start-time 2025-12-27T00:00:00Z \
  --end-time 2025-12-27T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

---

## Cost Estimation

AWS Lambda Pricing (as of December 2025):

- **Invocations:** $0.20 per 1 million requests
- **Duration:** $0.0000166667 per GB-second
- **Free Tier:** 1 million free requests/month + 400,000 GB-seconds/month

**Example Cost for Your Lambda:**

- 1,000 invocations/day = 30,000/month
- Average duration: 200ms = 0.0003125 GB-seconds
- Total/month: 30,000 × 0.0000166667 × 0.512 GB ≈ $0.26/month

**Very affordable!** ✅

---

## Summary

| Step                     | LocalStack                                                   | AWS                                          |
|--------------------------|--------------------------------------------------------------|----------------------------------------------|
| 1. Build                 | `mvn clean install -DskipTests`                              | Same                                         |
| 2. Configure credentials | test/test                                                    | Your AWS credentials                         |
| 3. Update tfvars         | terraform.localstack.tfvars                                  | terraform.tfvars (use_localstack=false)      |
| 4. Deploy                | `terraform apply -var-file=terraform.localstack.tfvars`      | `terraform apply -var-file=terraform.tfvars` |
| 5. Test                  | `aws lambda invoke ... --endpoint-url http://localhost:4566` | `aws lambda invoke ...` (no endpoint)        |

---

## Next Steps

1. **Test thoroughly locally** with LocalStack
2. **Get AWS credentials** and create AWS account
3. **Update terraform.tfvars** with real values
4. **Deploy to AWS** using same Terraform code
5. **Monitor** with CloudWatch logs

---

**Status**: ✅ Your code is ready for AWS deployment at any time!

The same JAR, same Terraform code, just different configuration!

