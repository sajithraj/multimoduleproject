# Infrastructure - Terraform Deployment

Infrastructure as Code (IaC) for deploying AWS Lambda functions using Terraform with LocalStack support.

## 📋 Overview

This directory contains Terraform configurations to deploy:

- Lambda functions (Service and TaskService)
- Secrets Manager secrets
- IAM roles and policies
- API Gateway endpoints
- CloudWatch Log Groups

---

## 🏗️ Infrastructure Components

```
AWS Resources
│
├── Lambda Functions
│   ├── my-token-auth-lambda (Service Module)
│   └── task-service (TaskService Module)
│
├── Secrets Manager
│   └── external-api/token (OAuth2 credentials)
│
├── IAM Roles
│   └── lambda-execution-role
│
├── CloudWatch Log Groups
│   ├── /aws/lambda/my-token-auth-lambda
│   └── /aws/lambda/task-service
│
└── API Gateway (optional)
    └── REST API endpoints
```

---

## 📦 Files

| File | Purpose |
|------|---------|
| `main.tf` | Main Terraform configuration |
| `variables.tf` | Input variables |
| `outputs.tf` | Output values |
| `deploy.ps1` | PowerShell deployment script |
| `deploy.sh` | Bash deployment script |
| `deploy-localstack.ps1` | LocalStack deployment |

---

## 🔧 Prerequisites

### Required
- **Terraform 1.0+**
- **AWS CLI** configured with credentials
- **Maven** (to build JARs)

### Optional (LocalStack)
- **Docker**
- **LocalStack** (for local testing)

---

## 🚀 Deployment

### Step 1: Build Lambda Functions

```bash
# Navigate to project root
cd ..

# Build all modules
mvn clean package

# Verify JARs created
ls -la service/target/service-1.0-SNAPSHOT.jar
ls -la taskService/target/taskService-1.0-SNAPSHOT.jar
```

### Step 2: Initialize Terraform

```bash
cd infra/terraform
terraform init
```

### Step 3: Review Plan

```bash
terraform plan
```

### Step 4: Deploy to AWS

```bash
terraform apply
```

### Step 5: Verify Deployment

```bash
# List Lambda functions
aws lambda list-functions

# Get function details
aws lambda get-function --function-name my-token-auth-lambda
aws lambda get-function --function-name task-service
```

---

## 🐳 LocalStack Deployment

### Step 1: Start LocalStack

```bash
# Using Docker
docker run -d -p 4566:4566 localstack/localstack

# Or using docker-compose
docker-compose up -d
```

### Step 2: Deploy to LocalStack

```powershell
# PowerShell
.\deploy-localstack.ps1
```

```bash
# Bash
./deploy-localstack.sh
```

### Step 3: Test LocalStack Lambda

```bash
# Set LocalStack endpoint
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Invoke Lambda
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json

cat response.json
```

---

## 🔧 Configuration

### Required Variables

```hcl
# variables.tf

variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "service_jar_path" {
  description = "Path to service JAR file"
  type        = string
  default     = "../../service/target/service-1.0-SNAPSHOT.jar"
}

variable "taskservice_jar_path" {
  description = "Path to taskService JAR file"
  type        = string
  default     = "../../taskService/target/taskService-1.0-SNAPSHOT.jar"
}
```

### Override Variables

Create `terraform.tfvars`:

```hcl
aws_region = "us-east-1"
environment = "production"
```

---

## 📝 Environment Variables

### Lambda Environment Variables

Configured in Terraform:

```hcl
environment {
  variables = {
    # Service Lambda
    TOKEN_ENDPOINT_URL = var.token_endpoint_url
    TOKEN_SECRET_NAME = "external-api/token"
    EXTERNAL_API_URL = var.external_api_url
    OAUTH2_TIMEOUT_SECONDS = "3"
    POWERTOOLS_SERVICE_NAME = "api-service"
    POWERTOOLS_LOG_LEVEL = "INFO"
    
    # TaskService Lambda
    POWERTOOLS_SERVICE_NAME = "task-service"
    POWERTOOLS_LOG_LEVEL = "INFO"
    POWERTOOLS_LOGGER_LOG_EVENT = "true"
  }
}
```

---

## 🔐 Secrets Manager

### Create Secret

```bash
# AWS
aws secretsmanager create-secret \
  --name external-api/token \
  --secret-string '{
    "username":"your-client-id",
    "password":"your-client-secret"
  }'

# LocalStack
awslocal secretsmanager create-secret \
  --name external-api/token \
  --secret-string '{
    "username":"test-client-id",
    "password":"test-client-secret"
  }'
```

### Update Secret

```bash
aws secretsmanager update-secret \
  --secret-id external-api/token \
  --secret-string '{
    "username":"new-client-id",
    "password":"new-client-secret"
  }'
```

---

## 📊 Lambda Configuration

### Service Lambda

```hcl
resource "aws_lambda_function" "service" {
  function_name = "my-token-auth-lambda"
  runtime       = "java21"
  handler       = "com.project.service.ApiHandler::handleRequest"
  memory_size   = 512
  timeout       = 60
  
  filename         = var.service_jar_path
  source_code_hash = filebase64sha256(var.service_jar_path)
  
  role = aws_iam_role.lambda_execution_role.arn
  
  environment {
    variables = {
      TOKEN_ENDPOINT_URL = var.token_endpoint_url
      TOKEN_SECRET_NAME = "external-api/token"
      EXTERNAL_API_URL = var.external_api_url
    }
  }
}
```

### TaskService Lambda

```hcl
resource "aws_lambda_function" "taskservice" {
  function_name = "task-service"
  runtime       = "java21"
  handler       = "com.project.task.handler.UnifiedTaskHandler::handleRequest"
  memory_size   = 512
  timeout       = 60
  
  filename         = var.taskservice_jar_path
  source_code_hash = filebase64sha256(var.taskservice_jar_path)
  
  role = aws_iam_role.lambda_execution_role.arn
}
```

---

## 🧪 Testing

### Test Service Lambda

```bash
# AWS
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json

# LocalStack
awslocal lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json
```

### Test TaskService Lambda

```bash
# API Gateway event
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "httpMethod": "POST",
    "path": "/tasks",
    "body": "{\"taskName\":\"Test Task\"}"
  }' \
  response.json

# SQS event
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "Records": [{
      "messageId": "msg-123",
      "body": "{\"orderId\":\"ORD-001\"}"
    }]
  }' \
  response.json
```

---

## 📈 Monitoring

### CloudWatch Logs

```bash
# View logs
aws logs tail /aws/lambda/my-token-auth-lambda --follow

# Filter logs
aws logs filter-log-events \
  --log-group-name /aws/lambda/my-token-auth-lambda \
  --filter-pattern "ERROR"
```

### Lambda Metrics

```bash
# Get metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=my-token-auth-lambda \
  --start-time 2025-12-29T00:00:00Z \
  --end-time 2025-12-29T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

---

## 🔄 Updates

### Update Lambda Code

```bash
# Build new JAR
cd ../..
mvn clean package -pl service

# Update Lambda
cd infra/terraform
terraform apply -target=aws_lambda_function.service
```

### Update Environment Variables

```bash
# Edit variables.tf or terraform.tfvars

# Apply changes
terraform apply
```

---

## 🗑️ Cleanup

### Destroy All Resources

```bash
terraform destroy
```

### Destroy Specific Resource

```bash
# Destroy Lambda only
terraform destroy -target=aws_lambda_function.service

# Destroy Secret only
terraform destroy -target=aws_secretsmanager_secret.external_api_token
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. JAR Not Found
**Problem:** `Error: no such file or directory`  
**Solution:** Build JAR files first: `mvn clean package`

#### 2. Insufficient IAM Permissions
**Problem:** Access denied errors  
**Solution:** Add required permissions to execution role

#### 3. Lambda Timeout
**Problem:** Function times out  
**Solution:** Increase timeout in `main.tf`

```hcl
timeout = 90  # Increase from 60
```

#### 4. LocalStack Connection Failed
**Problem:** Cannot connect to LocalStack  
**Solution:** Verify Docker container is running

```bash
docker ps | grep localstack
```

---

## 📚 References

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Lambda Terraform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function)
- [LocalStack Documentation](https://docs.localstack.cloud/)

---

## 🔄 Changelog

### Version 1.0.0 (2025-12-29)
- ✅ Terraform configuration for Lambda
- ✅ LocalStack support
- ✅ Secrets Manager integration
- ✅ Deployment scripts

---

**Infrastructure as Code with Terraform**

[← Back to Main README](../README.md)

