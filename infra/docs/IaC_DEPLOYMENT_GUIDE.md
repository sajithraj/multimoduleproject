# Infrastructure as Code Setup - CloudFormation & Terraform

Complete guide to deploy Secrets Manager secret using CloudFormation or Terraform.

---

## 🎯 Quick Choice

| If You                               | Use            |
|--------------------------------------|----------------|
| Want simple AWS setup                | CloudFormation |
| Want flexibility & repeatability     | Terraform      |
| Want to manage multiple environments | Terraform      |
| Just need quick setup                | CloudFormation |

---

## ☁️ OPTION 1: CloudFormation (Easiest)

### File: `cloudformation-secrets.yaml`

#### Step 1: Update Parameters (Optional)

Edit the file to change default values:

```yaml
Parameters:
  ClientId:
    Default: 'your-real-client-id'  # Change this
  
  ClientSecret:
    Default: 'your-real-client-secret'  # Change this
```

#### Step 2: Deploy Stack via AWS Console

1. Go to AWS CloudFormation Console
2. Click "Create Stack"
3. Choose "Upload a template file"
4. Select `cloudformation-secrets.yaml`
5. Click "Next"
6. Enter Stack Name: `stablecoin-secrets-stack`
7. Enter Parameters:
    - ClientId: Your actual client ID
    - ClientSecret: Your actual client secret
    - SecretName: `external-api/token`
    - Environment: `dev` (or staging/prod)
8. Click "Next" → "Next" → "Create Stack"

Wait for status to show **CREATE_COMPLETE**.

#### Step 3: Deploy Stack via CLI

```bash
aws cloudformation create-stack \
  --stack-name stablecoin-secrets-stack \
  --template-body file://cloudformation-secrets.yaml \
  --parameters \
    ParameterKey=ClientId,ParameterValue=your-real-client-id \
    ParameterKey=ClientSecret,ParameterValue=your-real-client-secret \
    ParameterKey=SecretName,ParameterValue=external-api/token \
    ParameterKey=Environment,ParameterValue=dev \
  --capabilities CAPABILITY_NAMED_IAM \
  --region us-east-1
```

#### Step 4: Verify

```bash
# Check stack status
aws cloudformation describe-stacks \
  --stack-name stablecoin-secrets-stack \
  --region us-east-1

# Check secret was created
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --region us-east-1
```

---

## 🏗️ OPTION 2: Terraform (Recommended for Production)

### Files: `main.tf` and `terraform.tfvars`

#### Step 1: Install Terraform

Download: https://www.terraform.io/downloads.html

Verify installation:

```bash
terraform --version
```

#### Step 2: Update Variables

Edit `terraform.tfvars`:

```terraform
aws_region    = "us-east-1"
environment   = "dev"
secret_name   = "external-api/token"
client_id     = "your-real-client-id"      # ← UPDATE THIS
client_secret = "your-real-client-secret"  # ← UPDATE THIS
```

#### Step 3: Initialize Terraform

```bash
cd path/to/project
terraform init
```

Expected output:

```
Terraform has been successfully initialized!
```

#### Step 4: Plan Deployment

```bash
terraform plan
```

Review the output to see what will be created:

- AWS Secrets Manager Secret
- IAM Role for Lambda
- IAM Policies

#### Step 5: Apply Deployment

```bash
terraform apply
```

Type `yes` when prompted.

Expected output:

```
Apply complete! Resources: 4 added, 0 changed, 0 destroyed.
```

#### Step 6: Verify

```bash
# Check secret was created
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --region us-east-1

# Check IAM role was created
aws iam get-role \
  --role-name lambda-execution-role-dev \
  --region us-east-1
```

---

## 🔄 Managing Multiple Environments with Terraform

### Staging Environment

Create `terraform.staging.tfvars`:

```terraform
aws_region    = "us-east-1"
environment   = "staging"
secret_name   = "external-api/token-staging"
client_id     = "staging-client-id"
client_secret = "staging-client-secret"
```

Deploy:

```bash
terraform apply -var-file=terraform.staging.tfvars
```

### Production Environment

Create `terraform.prod.tfvars`:

```terraform
aws_region    = "us-east-1"
environment   = "prod"
secret_name   = "external-api/token-prod"
client_id     = "prod-client-id"
client_secret = "prod-client-secret"
```

Deploy:

```bash
terraform apply -var-file=terraform.prod.tfvars
```

---

## 🔐 Updating Secrets

### CloudFormation

Update the stack:

```bash
aws cloudformation update-stack \
  --stack-name stablecoin-secrets-stack \
  --template-body file://cloudformation-secrets.yaml \
  --parameters \
    ParameterKey=ClientId,ParameterValue=new-client-id \
    ParameterKey=ClientSecret,ParameterValue=new-client-secret \
    UsePreviousValue=true \
  --capabilities CAPABILITY_NAMED_IAM
```

### Terraform

1. Update `terraform.tfvars`:

```terraform
client_id     = "new-client-id"
client_secret = "new-client-secret"
```

2. Apply changes:

```bash
terraform apply
```

3. Or update directly:

```bash
terraform apply -var="client_id=new-id" -var="client_secret=new-secret"
```

---

## 🗑️ Cleanup

### Remove CloudFormation Stack

```bash
aws cloudformation delete-stack \
  --stack-name stablecoin-secrets-stack
```

### Remove Terraform Resources

```bash
terraform destroy
```

Type `yes` when prompted.

---

## 📊 What Gets Created

### CloudFormation / Terraform Create:

1. **Secrets Manager Secret**
    - Name: `external-api/token`
    - Content: `{"client_id": "...", "client_secret": "..."}`
    - Tags: Environment, Application, Purpose

2. **IAM Role**
    - Name: `lambda-execution-role-dev` (or staging/prod)
    - Permissions: Can read Secrets Manager secret
    - Permissions: Can write to CloudWatch Logs

3. **IAM Policies**
    - SecretsManager:GetSecretValue
    - SecretsManager:DescribeSecret
    - CloudWatch Logs permissions

---

## ✅ Verification Checklist

- [ ] CloudFormation/Terraform deployed successfully
- [ ] Secrets Manager secret created
- [ ] Can retrieve secret via CLI:
  ```bash
  aws secretsmanager get-secret-value --secret-id external-api/token
  ```
- [ ] IAM role created
- [ ] Lambda can access the secret

---

## 📋 Comparison

| Feature               | CloudFormation    | Terraform    |
|-----------------------|-------------------|--------------|
| Learning curve        | Easy              | Moderate     |
| Setup time            | 5 minutes         | 10 minutes   |
| Multiple environments | Manual            | Automated    |
| State management      | AWS               | Local/Remote |
| Dry-run               | Yes (Change Sets) | Yes (Plan)   |
| Rollback              | Easy              | Manual       |
| Reusability           | Low               | High         |
| Version control       | Good              | Excellent    |

---

## 🚀 Quick Start

**Choose one:**

### CloudFormation (Fast)

```bash
aws cloudformation create-stack \
  --stack-name stablecoin-secrets \
  --template-body file://cloudformation-secrets.yaml \
  --parameters \
    ParameterKey=ClientId,ParameterValue=YOUR_ID \
    ParameterKey=ClientSecret,ParameterValue=YOUR_SECRET \
  --capabilities CAPABILITY_NAMED_IAM
```

### Terraform (Flexible)

```bash
# Edit terraform.tfvars first
terraform init
terraform plan
terraform apply
```

---

## 📞 Troubleshooting

### Stack Creation Failed

**CloudFormation:**

```bash
aws cloudformation describe-stack-events \
  --stack-name stablecoin-secrets-stack
```

**Terraform:**

```bash
terraform plan -detailed-exit-code
```

### Cannot access secret

Verify IAM policy:

```bash
aws iam get-role-policy \
  --role-name lambda-execution-role-dev \
  --policy-name SecretsManagerAccess
```

### Permission Denied

Add proper IAM permissions to your user:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:*",
        "iam:CreateRole",
        "iam:PutRolePolicy",
        "iam:AttachRolePolicy"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## 📚 Additional Resources

- [CloudFormation Secrets Manager](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-secretsmanager-secret.html)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)

---

**Status**: ✅ Ready to deploy
**Next**: Choose CloudFormation or Terraform and deploy!

