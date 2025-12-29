# 🚀 ACTION CARD - Deploy in 5 Minutes

## Option 1: CloudFormation (Quickest)

### Your Command (Copy & Paste):

```bash
aws cloudformation create-stack \
  --stack-name stablecoin-secrets \
  --template-body file://cloudformation-secrets.yaml \
  --parameters \
    ParameterKey=ClientId,ParameterValue=YOUR_REAL_CLIENT_ID \
    ParameterKey=ClientSecret,ParameterValue=YOUR_REAL_CLIENT_SECRET \
    ParameterKey=SecretName,ParameterValue=external-api/token \
    ParameterKey=Environment,ParameterValue=dev \
  --capabilities CAPABILITY_NAMED_IAM \
  --region us-east-1
```

Replace:

- `YOUR_REAL_CLIENT_ID` - Your actual OAuth2 client ID
- `YOUR_REAL_CLIENT_SECRET` - Your actual OAuth2 client secret

---

## Option 2: Terraform (Best Practices)

### Step 1: Update terraform.tfvars

```terraform
client_id     = "YOUR_REAL_CLIENT_ID"
client_secret = "YOUR_REAL_CLIENT_SECRET"
```

### Step 2: Run These Commands

```bash
terraform init
terraform plan
terraform apply
```

Type `yes` when prompted.

---

## ✅ Verify It Worked

```bash
aws secretsmanager get-secret-value --secret-id external-api/token --region us-east-1
```

Should show your credentials in the response.

---

## 🎯 Timeline

| Step               | Time      |
|--------------------|-----------|
| Choose option      | 1 min     |
| Update credentials | 1 min     |
| Deploy             | 2 min     |
| Verify             | 1 min     |
| **Total**          | **5 min** |

---

## 📊 Files You Have

- `cloudformation-secrets.yaml` ← For CloudFormation
- `main.tf` ← For Terraform
- `terraform.tfvars` ← For Terraform
- Guides and documentation ← For help

---

## ✨ What Gets Created

- AWS Secrets Manager Secret
- IAM Role for Lambda
- IAM Policies for access

---

**Pick one above and deploy!** 🎉

