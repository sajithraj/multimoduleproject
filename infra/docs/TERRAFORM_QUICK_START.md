# Terraform Quick Start

## 🚀 Deploy in 5 Steps

### Step 1: Install Terraform

Download: https://www.terraform.io/downloads.html

Verify:

```bash
terraform --version
```

### Step 2: Update terraform.tfvars

Edit the file and change these values:

```terraform
client_id     = "your-real-client-id"
client_secret = "your-real-client-secret"
```

### Step 3: Initialize

```bash
terraform init
```

Expected: "Terraform has been successfully initialized!"

### Step 4: Plan & Review

```bash
terraform plan
```

This shows what will be created without making changes.

### Step 5: Apply

```bash
terraform apply
```

Type `yes` when prompted.

Expected: "Apply complete! Resources: 4 added"

---

## 📝 Files

### main.tf

```terraform
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  default = "us-east-1"
}

variable "client_id" {
  sensitive = true
  default   = "test-client-id"
}

variable "client_secret" {
  sensitive = true
  default   = "test-client-secret"
}

resource "aws_secretsmanager_secret" "oauth" {
  name = "external-api/token"
}

resource "aws_secretsmanager_secret_version" "oauth" {
  secret_id = aws_secretsmanager_secret.oauth.id
  secret_string = jsonencode({
    client_id     = var.client_id
    client_secret = var.client_secret
  })
}

resource "aws_iam_role" "lambda" {
  name = "lambda-execution-role-dev"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "sts:AssumeRole"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_secrets" {
  name = "secrets-access"
  role = aws_iam_role.lambda.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = aws_secretsmanager_secret.oauth.arn
      }
    ]
  })
}

output "secret_arn" {
  value = aws_secretsmanager_secret.oauth.arn
}

output "role_arn" {
  value = aws_iam_role.lambda.arn
}
```

### terraform.tfvars

```terraform
aws_region    = "us-east-1"
environment   = "dev"
secret_name   = "external-api/token"
client_id     = "your-real-client-id"
client_secret = "your-real-client-secret"
```

---

## ✅ What Gets Created

- ✅ AWS Secrets Manager Secret
- ✅ Secret Version with credentials
- ✅ IAM Role for Lambda
- ✅ IAM Policy for Secrets access

---

## 🔄 Update Credentials

Edit `terraform.tfvars`:

```terraform
client_id     = "new-id"
client_secret = "new-secret"
```

Then apply:

```bash
terraform apply
```

---

## 🗑️ Destroy All

```bash
terraform destroy
```

Type `yes` when prompted.

---

## 📊 State File

Terraform creates `terraform.tfstate` file.

**⚠️ Important**:

- Add to `.gitignore` (contains secrets)
- For team projects, use remote state (S3 backend)

```bash
# Add to .gitignore
echo "terraform.tfstate*" >> .gitignore
```

---

## 🎯 Multiple Environments

Create separate tfvars files:

**terraform.dev.tfvars**

```terraform
environment   = "dev"
client_id     = "dev-id"
client_secret = "dev-secret"
```

**terraform.prod.tfvars**

```terraform
environment   = "prod"
client_id     = "prod-id"
client_secret = "prod-secret"
```

Deploy to different environments:

```bash
# Dev
terraform apply -var-file=terraform.dev.tfvars

# Prod
terraform apply -var-file=terraform.prod.tfvars
```

---

## ✨ Advantages over CloudFormation

✅ Cleaner syntax
✅ Built-in plan/dry-run
✅ State management
✅ Multi-cloud support
✅ Modules & reusability

---

**Status**: ✅ Ready to deploy

