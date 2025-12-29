# Terraform + LocalStack Quick Start

## 🎯 Easy LocalStack Integration

Your Terraform is now configured to work seamlessly with LocalStack!

---

## 🚀 Deploy to LocalStack (For Development)

### Step 1: Start LocalStack

```bash
docker-compose up -d
```

### Step 2: Initialize Terraform

```bash
terraform init
```

### Step 3: Plan Against LocalStack

```bash
terraform plan -var-file=terraform.localstack.tfvars
```

Expected: Shows 4 resources to be created

### Step 4: Apply to LocalStack

```bash
terraform apply -var-file=terraform.localstack.tfvars
```

Type `yes` when prompted.

### Step 5: Verify

```powershell
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

---

## 🌍 Deploy to AWS Production

### Step 1: Update terraform.tfvars

```terraform
client_id     = "your-real-client-id"
client_secret = "your-real-client-secret"
```

### Step 2: Plan Against AWS

```bash
terraform plan -var-file=terraform.tfvars
```

### Step 3: Apply to AWS

```bash
terraform apply -var-file=terraform.tfvars
```

### Step 4: Verify

```bash
aws secretsmanager get-secret-value --secret-id external-api/token
```

---

## 📋 File Guide

| File                          | Purpose                                                   |
|-------------------------------|-----------------------------------------------------------|
| `main.tf`                     | Terraform configuration (updated with LocalStack support) |
| `terraform.tfvars`            | AWS production variables                                  |
| `terraform.localstack.tfvars` | LocalStack development variables                          |

---

## 🔄 Switching Between LocalStack and AWS

### To use LocalStack:

```bash
terraform apply -var-file=terraform.localstack.tfvars
```

### To use AWS:

```bash
terraform apply -var-file=terraform.tfvars
```

**That's it!** No code changes needed.

---

## ✨ How It Works

Your `main.tf` now has:

```terraform
variable "use_localstack" {
  default = false
}

dynamic "endpoints" {
  for_each = var.use_localstack ? [1] : []
  # LocalStack endpoints only when use_localstack = true
}
```

**LocalStack tfvars**:

```terraform
use_localstack = true  # ← Enables LocalStack endpoints
```

**AWS tfvars**:

```terraform
use_localstack = false  # ← Uses real AWS
```

---

## 🎯 Local Development Workflow

```
1. Start LocalStack        (docker-compose up -d)
2. Deploy infrastructure   (terraform apply -var-file=terraform.localstack.tfvars)
3. Test your Lambda        (aws lambda invoke ...)
4. Debug and iterate       (check logs, update code)
5. When done, clean up     (terraform destroy -var-file=terraform.localstack.tfvars)
```

---

## 🚀 Production Deployment Workflow

```
1. Update AWS credentials  (update terraform.tfvars)
2. Plan deployment         (terraform plan -var-file=terraform.tfvars)
3. Deploy to AWS           (terraform apply -var-file=terraform.tfvars)
4. Verify in AWS Console   (check Secrets Manager, IAM roles)
```

---

## ✅ Advantages of This Setup

✅ **Easy switching** - One command to switch between LocalStack and AWS
✅ **Same code** - No code changes needed
✅ **Safe testing** - Test everything locally first
✅ **Clear separation** - Different tfvars for different environments
✅ **Professional** - Production-ready infrastructure as code

---

## 🎉 You're All Set!

Your Terraform is now optimized for:

- ✅ LocalStack development
- ✅ AWS production deployment
- ✅ Easy environment switching
- ✅ Professional IaC practices

**Start testing with LocalStack!** 🚀

