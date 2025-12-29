# ✅ Lambda Function Added to Terraform

## What Was Added

I've added **complete Lambda function configuration** to your `infra/terraform/main.tf` file!

---

## Lambda Function Configuration

### Resource: aws_lambda_function

```terraform
resource "aws_lambda_function" "token_auth_lambda" {
  filename      = "../../../target/SetUpProject-1.0-SNAPSHOT.jar"
  function_name = "my-token-auth-lambda"
  role          = aws_iam_role.lambda_execution_role.arn
  handler       = "com.project.ApiHandler::handleRequest"
  runtime       = "java21"
  timeout       = 60
  memory_size   = 512

  environment {
    variables = {
      TOKEN_SECRET_NAME = var.secret_name          # ← Secret name from tfvars
      AWS_REGION = var.aws_region           # ← Region from tfvars
      ENVIRONMENT = var.environment          # ← Environment from tfvars
    }
  }

  tags = {
    Environment = var.environment
    Application = "StableCoinLambda"
    ManagedBy   = "Terraform"
  }

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic_execution,
    aws_iam_role_policy.secrets_manager_access
  ]
}
```

---

## Environment Variables Set

Your Lambda now has these environment variables automatically set by Terraform:

| Variable            | Value                | Source                  |
|---------------------|----------------------|-------------------------|
| `TOKEN_SECRET_NAME` | `external-api/token` | From `terraform.tfvars` |
| `AWS_REGION`        | `us-east-1`          | From `terraform.tfvars` |
| `ENVIRONMENT`       | `dev-local` or `dev` | From `terraform.tfvars` |

---

## CloudWatch Log Group

Also created:

```terraform
resource "aws_cloudwatch_log_group" "lambda_log_group" {
  name              = "/aws/lambda/my-token-auth-lambda"
  retention_in_days = 14
}
```

**Logs will be automatically sent here!**

---

## Complete Resource Chain

```
Terraform deploys in this order:
    ↓
1. Secrets Manager Secret
   └─ Contains OAuth2 credentials
    ↓
2. IAM Role
   └─ Trust policy for Lambda service
    ↓
3. IAM Policies
   ├─ CloudWatch Logs access
   └─ Secrets Manager GetSecretValue access
    ↓
4. Lambda Function
   ├─ Uses JAR from target/
   ├─ Sets environment variables
   ├─ Attached to IAM role
   └─ Handler: com.project.ApiHandler::handleRequest
    ↓
5. CloudWatch Log Group
   └─ Receives Lambda logs
```

---

## Outputs Available

After `terraform apply`, you'll get:

```terraform
secret_arn = ARN of the secret
secret_name             = "external-api/token"
lambda_role_arn = ARN of execution role
lambda_role_name = "lambda-execution-role-dev"
lambda_function_arn = ARN of Lambda function
lambda_function_name = "my-token-auth-lambda"
lambda_log_group_name = "/aws/lambda/my-token-auth-lambda"
deployment_summary = Complete summary of all resources
```

---

## Next: Deploy

```bash
# Navigate to terraform directory
cd infra/terraform

# Initialize (first time only)
terraform init

# Deploy with LocalStack
terraform apply -var-file=terraform.localstack.tfvars

# Or deploy to AWS
terraform apply -var-file=terraform.tfvars
```

---

## ✅ Now Complete

Your Terraform main.tf now:

- ✅ Creates Secrets Manager secret with credentials
- ✅ Creates IAM role for Lambda
- ✅ Creates IAM policies for Secrets Manager access
- ✅ **Creates Lambda function (NEW!)**
- ✅ Creates CloudWatch Log Group
- ✅ Sets TOKEN_SECRET_NAME environment variable
- ✅ Sets AWS_REGION environment variable
- ✅ Sets ENVIRONMENT environment variable

---

**Your infrastructure is now 100% defined in Terraform!** 🎉

