# Infrastructure

**Infrastructure as Code (IaC) for Task Service deployment.**

---

## 📋 Overview

This directory contains all infrastructure configuration:
- **Terraform** - AWS resource provisioning
- **Docker** - LocalStack for local development

---

## 📁 Structure

```
infra/
├── README.md                    # This file
├── terraform/
│   ├── main.tf                  # Terraform configuration
│   ├── terraform.tfvars         # AWS variables
│   ├── terraform.localstack.tfvars  # LocalStack variables
│   └── scripts/                 # Helper scripts
│       ├── setup-dlq.ps1
│       ├── check-sqs-queue.ps1
│       ├── view-dlq.ps1
│       └── test-sqs-failures.ps1
└── docker/
    ├── docker-compose.yml       # LocalStack setup
    ├── init-aws.sh              # Initialization script
    └── localstack-helper.*      # Helper scripts
```

---

## 🚀 Quick Start

### LocalStack Deployment

**1. Start LocalStack:**
```powershell
cd docker
docker-compose up -d
```

**2. Deploy Infrastructure:**
```powershell
cd terraform
terraform init
terraform apply -var="use_localstack=true" -auto-approve
```

**3. Verify:**
```powershell
# Check Lambda
aws lambda list-functions --endpoint-url http://localhost:4566

# Check API Gateway
$apiUrl = (Get-Content terraform.tfstate -Raw | ConvertFrom-Json).outputs.api_gateway_invoke_url.value
Write-Host $apiUrl
```

### AWS Deployment

**1. Configure Variables:**
```hcl
# terraform/terraform.tfvars
use_localstack = false
aws_region = "us-east-1"
environment = "prod"
```

**2. Deploy:**
```powershell
cd terraform
terraform init
terraform plan
terraform apply
```

---

## 🐳 Docker (LocalStack)

See [docker/README.md](docker/README.md) for details.

**Quick Commands:**
```powershell
# Start
docker-compose up -d

# Stop
docker-compose down

# View logs
docker-compose logs -f

# Restart
docker-compose restart
```

---

## 🏗️ Terraform

See [terraform/README.md](terraform/README.md) for details.

**Resources Created:**
- Lambda Function (`task-service-dev`)
- API Gateway (REST API)
- SQS Queue (`task-queue`)
- SQS DLQ (`task-queue-dlq`)
- IAM Roles & Policies
- CloudWatch Log Groups

**Key Files:**
- `main.tf` - Resource definitions
- `terraform.tfvars` - AWS configuration
- `terraform.localstack.tfvars` - LocalStack configuration

---

## 🔧 Helper Scripts

Located in `terraform/scripts/`:

| Script | Purpose |
|--------|---------|
| `setup-dlq.ps1` | Configure SQS DLQ and event source mapping |
| `check-sqs-queue.ps1` | View queue status and messages |
| `view-dlq.ps1` | View failed messages in DLQ |
| `test-sqs-failures.ps1` | Test SQS failure scenarios |

**Usage:**
```powershell
cd terraform/scripts
.\setup-dlq.ps1
.\check-sqs-queue.ps1
```

---

## 📊 Outputs

After deployment, Terraform provides:

```hcl
outputs = {
  lambda_function_name = "task-service-dev"
  api_gateway_invoke_url = "http://localhost:4566/restapis/{id}/dev/_user_request_"
  sqs_queue_url = "http://localhost:4566/000000000000/task-queue"
  sqs_dlq_url = "http://localhost:4566/000000000000/task-queue-dlq"
}
```

**Access outputs:**
```powershell
cd terraform
terraform output
```

---

## 🧹 Cleanup

### LocalStack
```powershell
# Destroy infrastructure
cd terraform
terraform destroy -var="use_localstack=true" -auto-approve

# Stop Docker
cd ../docker
docker-compose down
```

### AWS
```powershell
cd terraform
terraform destroy
```

---

## 🔍 Troubleshooting

### Issue: LocalStack not starting
```powershell
cd docker
docker-compose down
docker-compose up -d
docker-compose logs -f
```

### Issue: Terraform fails
```powershell
# Reinitialize
terraform init -upgrade

# Check state
terraform state list

# Refresh
terraform refresh -var="use_localstack=true"
```

### Issue: Resources not created
```powershell
# Check LocalStack
docker ps | Select-String localstack

# Verify endpoint
aws lambda list-functions --endpoint-url http://localhost:4566
```

---

## 📚 Documentation

- [Parent README](../README.md) - Project overview
- [Terraform README](terraform/README.md) - Terraform details
- [Docker README](docker/README.md) - Docker/LocalStack details
- [Quick Reference](../QUICK_REFERENCE.md) - Testing commands

---

**Status:** ✅ Production Ready  
**Last Updated:** December 30, 2025

