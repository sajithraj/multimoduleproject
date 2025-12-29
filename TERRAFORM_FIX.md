# Terraform LocalStack Credentials Fix

## Problem
When deploying to LocalStack, Terraform was showing this error:
```
Error: creating SQS Queue: The security token included in the request is invalid.
Error: creating EventBridge Rule: The security token included in the request is invalid.
```

## Root Cause

Two issues:

1. **Missing Endpoints:** The Terraform provider configuration was missing SQS and EventBridge endpoints
2. **Credentials Not Set:** Terraform wasn't using the static "test" credentials needed for LocalStack

## Solution Applied

### Updated `infra/terraform/main.tf`

**Before:**
```terraform
provider "aws" {
  region = var.aws_region

  dynamic "endpoints" {
    for_each = var.use_localstack ? [1] : []
    content {
      secretsmanager = "http://localhost:4566"
      iam            = "http://localhost:4566"
      lambda         = "http://localhost:4566"
      logs           = "http://localhost:4566"
    }
  }

  skip_credentials_validation = var.use_localstack
  skip_metadata_api_check     = var.use_localstack
  skip_requesting_account_id  = var.use_localstack
}
```

**After:**
```terraform
provider "aws" {
  region = var.aws_region

  dynamic "endpoints" {
    for_each = var.use_localstack ? [1] : []
    content {
      secretsmanager = "http://localhost:4566"
      iam            = "http://localhost:4566"
      lambda         = "http://localhost:4566"
      logs           = "http://localhost:4566"
      sqs            = "http://localhost:4566"              # ← ADDED
      cloudwatch     = "http://localhost:4566"              # ← ADDED
      cloudwatchevents = "http://localhost:4566"           # ← ADDED
      events         = "http://localhost:4566"              # ← ADDED
      apigateway     = "http://localhost:4566"              # ← ADDED
    }
  }

  skip_credentials_validation = var.use_localstack
  skip_metadata_api_check     = var.use_localstack
  skip_requesting_account_id  = var.use_localstack
  
  # For LocalStack, use static credentials
  access_key = var.use_localstack ? "test" : null          # ← ADDED
  secret_key = var.use_localstack ? "test" : null          # ← ADDED
}
```

## Changes Made

✅ Added SQS endpoint  
✅ Added CloudWatch endpoint  
✅ Added CloudWatch Events endpoint  
✅ Added EventBridge endpoint  
✅ Added API Gateway endpoint  
✅ Added static credentials (access_key/secret_key)  

## Why This Fixes the Issue

### 1. Endpoints
Each AWS service needs its endpoint configured to point to LocalStack (port 4566). Without these, Terraform tries to connect to real AWS, which fails.

### 2. Static Credentials
LocalStack accepts any credentials, but Terraform needs them explicitly set in the provider block when `use_localstack = true`.

## Deployment Commands

### After This Fix
```powershell
cd infra\terraform

# Set environment variables (still needed for AWS CLI)
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Re-initialize to pick up provider changes
terraform init -upgrade

# Apply
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve
```

### Or Use the Script
```powershell
.\deploy-localstack.ps1
```

## Resources That Will Now Deploy

✅ **Lambda Functions**
- task-service-dev
- my-token-auth-lambda

✅ **SQS Queues** (Previously Failing ❌)
- task-queue-dev
- task-queue-dlq-dev

✅ **EventBridge Rules** (Previously Failing ❌)
- task-schedule-dev (cron: rate(5 minutes))

✅ **CloudWatch Log Groups**
- /aws/lambda/task-service-dev
- /aws/lambda/my-token-auth-lambda

✅ **Secrets Manager**
- external-api/token

✅ **IAM Roles & Policies**
- lambda_execution_role

## Verification

After deployment, verify all resources:

```powershell
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"

# Check Lambda
aws lambda list-functions --endpoint-url http://localhost:4566

# Check SQS
aws sqs list-queues --endpoint-url http://localhost:4566

# Check EventBridge
aws events list-rules --endpoint-url http://localhost:4566

# Check Secrets
aws secretsmanager list-secrets --endpoint-url http://localhost:4566
```

## Status

✅ **FIXED** - All resources should now deploy successfully to LocalStack

---

**Date:** December 29, 2025  
**Issue:** Terraform LocalStack credentials error  
**Resolution:** Added missing endpoints and static credentials to provider configuration

