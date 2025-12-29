# 🚀 TERRAFORM + LOCALSTACK - ACTION CARD

## ⚡ Deploy Secrets to LocalStack in 3 Commands

### Command 1: Initialize Terraform

```bash
terraform init
```

### Command 2: Plan Deployment

```bash
terraform plan -var-file=terraform.localstack.tfvars
```

Review the output to see what will be created.

### Command 3: Deploy to LocalStack

```bash
terraform apply -var-file=terraform.localstack.tfvars
```

Type `yes` when prompted.

---

## ✅ Verify It Worked

```powershell
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

aws secretsmanager get-secret-value `
  --secret-id external-api/token `
  --endpoint-url http://localhost:4566
```

Should show your test credentials.

---

## 🔄 Later: Deploy to AWS

Just change the tfvars file:

```bash
# Update your real credentials first in terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

**Same code, different credentials!** 🎉

---

## 📊 Files You Have

- `main.tf` - Supports both LocalStack and AWS
- `terraform.localstack.tfvars` - LocalStack settings
- `terraform.tfvars` - AWS settings

---

## 🎯 Why Terraform + LocalStack?

✅ One command to switch environments
✅ Dry-run with `terraform plan`
✅ Easy to test locally
✅ Professional IaC workflow
✅ No code changes needed

---

**Ready? Run the 3 commands above!** 🚀

