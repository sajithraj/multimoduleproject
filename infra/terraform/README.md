# Terraform Configuration

**Infrastructure as Code for Task Service AWS resources.**

---

## 📋 Overview

Terraform configuration to provision:
- AWS Lambda Function
- API Gateway REST API
- SQS Queues (Main + DLQ)
- IAM Roles & Policies
- CloudWatch Log Groups
- Event Source Mappings

---

## 🚀 Quick Start

### LocalStack Deployment

```powershell
# Initialize
terraform init

# Deploy
terraform apply -var="use_localstack=true" -auto-approve

# Get outputs
terraform output
```

### AWS Deployment

```powershell
# Initialize
terraform init

# Plan
terraform plan

# Deploy
terraform apply

# Get outputs
terraform output
```

---

## 📁 Files

| File | Purpose |
|------|---------|
| `main.tf` | Main Terraform configuration |
| `terraform.tfvars` | AWS environment variables |
| `terraform.localstack.tfvars` | LocalStack variables |
| `terraform.tfstate` | State file (auto-generated) |
| `.terraform.lock.hcl` | Dependency lock file |

---

## 🔧 Configuration

### Variables

**terraform.tfvars (AWS):**
```hcl
use_localstack = false
aws_region = "us-east-1"
environment = "prod"
lambda_timeout = 30
lambda_memory = 512
```

**terraform.localstack.tfvars (LocalStack):**
```hcl
use_localstack = true
aws_region = "us-east-1"
environment = "dev"
```

### Resources Created

**Lambda Function:**
- Name: `task-service-{env}`
- Runtime: `java21`
- Handler: `com.project.task.handler.UnifiedTaskHandler::handleRequest`
- Memory: 512 MB
- Timeout: 30 seconds

**API Gateway:**
- Type: REST API
- Stage: `dev`
- CORS: Enabled
- Endpoints: 6 (GET, POST, PUT, DELETE)

**SQS:**
- Main Queue: `task-queue`
- DLQ: `task-queue-dlq`
- Max Receive Count: 3
- Visibility Timeout: 30 seconds

**IAM:**
- Lambda Execution Role
- Policies: CloudWatch Logs, SQS, EventBridge

---

## 📊 Outputs

```hcl
output "lambda_function_name" {
  value = aws_lambda_function.task_service.function_name
}

output "api_gateway_invoke_url" {
  value = "${aws_api_gateway_deployment.main.invoke_url}${aws_api_gateway_stage.dev.stage_name}"
}

output "sqs_queue_url" {
  value = aws_sqs_queue.task_queue.url
}

output "sqs_dlq_url" {
  value = aws_sqs_queue.task_dlq.url
}
```

**View outputs:**
```powershell
terraform output
terraform output -json
```

---


## 🔄 Terraform Commands

### Basic Operations

```powershell
# Initialize
terraform init

# Format
terraform fmt

# Validate
terraform validate

# Plan
terraform plan

# Apply
terraform apply

# Destroy
terraform destroy
```

### With Variables

```powershell
# LocalStack
terraform apply -var="use_localstack=true" -auto-approve

# AWS with specific region
terraform apply -var="aws_region=us-west-2"

# Using var file
terraform apply -var-file="terraform.localstack.tfvars"
```

### State Management

```powershell
# List resources
terraform state list

# Show resource
terraform state show aws_lambda_function.task_service

# Refresh state
terraform refresh

# Import existing resource
terraform import aws_lambda_function.task_service task-service-dev
```

---

## 🐛 Troubleshooting

### Issue: Provider initialization failed

```powershell
# Clean and reinitialize
Remove-Item -Recurse -Force .terraform
terraform init
```

### Issue: State lock error

```powershell
# Force unlock (use carefully)
terraform force-unlock <LOCK_ID>
```

### Issue: Resource already exists

```powershell
# Import existing resource
terraform import <resource_type>.<name> <resource_id>

# Or destroy and recreate
terraform destroy -target=<resource>
terraform apply
```

### Issue: LocalStack connection refused

```powershell
# Check LocalStack is running
docker ps | Select-String localstack

# Restart if needed
cd ../docker
docker-compose restart
```

---

## 🧹 Cleanup

### Destroy All Resources

```powershell
# LocalStack
terraform destroy -var="use_localstack=true" -auto-approve

# AWS
terraform destroy
```

### Clean Terraform Files

```powershell
# Remove state files
Remove-Item terraform.tfstate*

# Remove lock file
Remove-Item .terraform.lock.hcl

# Remove terraform directory
Remove-Item -Recurse .terraform
```

---

## 📚 Best Practices

1. **Always run `terraform plan` before `apply`**
2. **Use variables for environment-specific values**
3. **Keep state files secure (never commit to Git)**
4. **Use remote state for team collaboration**
5. **Tag all resources for cost tracking**
6. **Use workspaces for multiple environments**

---

## 🔐 Security Notes

**State Files:**
- `.tfstate` contains sensitive data
- Added to `.gitignore`
- Use remote state backend for production

**Credentials:**
- Never hardcode AWS credentials
- Use environment variables or AWS CLI profiles
- For LocalStack, any dummy credentials work

---

## 📖 References

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Terraform LocalStack Setup](https://docs.localstack.cloud/user-guide/integrations/terraform/)
- [AWS Lambda with Terraform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function)

---

**Status:** ✅ Production Ready  
**Terraform Version:** >= 1.5  
**AWS Provider Version:** >= 5.0

