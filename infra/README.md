# Infrastructure Directory Structure

This directory contains all infrastructure-as-code and deployment-related files for the StableCoin Lambda project.

## Directory Organization

```
infra/
├── cloudformation/          # CloudFormation templates
│   └── cloudformation-secrets.yaml
├── terraform/               # Terraform configurations
│   ├── main.tf
│   ├── terraform.tfvars (AWS production)
│   └── terraform.localstack.tfvars (LocalStack development)
├── docker/                  # Docker and LocalStack configurations
│   ├── docker-compose.yml
│   ├── init-aws.sh
│   ├── localstack-helper.bat
│   └── localstack-helper.sh
└── docs/                    # Infrastructure documentation
    ├── IaC_DEPLOYMENT_GUIDE.md
    ├── CLOUDFORMATION_QUICK_START.md
    ├── TERRAFORM_QUICK_START.md
    ├── TERRAFORM_VS_CLOUDFORMATION_LOCALSTACK.md
    ├── TERRAFORM_LOCALSTACK_SETUP.md
    ├── TERRAFORM_LOCALSTACK_ACTION.md
    ├── LOCALSTACK_*.md (various LocalStack guides)
    └── *.md (other documentation)
```

## Quick Start

### Development with LocalStack

```bash
# Start LocalStack
cd infra/docker
docker-compose up -d

# Deploy infrastructure to LocalStack
cd ../terraform
terraform init
terraform plan -var-file=terraform.localstack.tfvars
terraform apply -var-file=terraform.localstack.tfvars
```

### Production Deployment to AWS

```bash
cd infra/terraform
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

### Using CloudFormation

```bash
cd infra/cloudformation
aws cloudformation create-stack \
  --stack-name stablecoin-secrets \
  --template-body file://cloudformation-secrets.yaml \
  --capabilities CAPABILITY_NAMED_IAM
```

## Token Credentials

The following credentials are configured in:

- `terraform/terraform.tfvars` - AWS production
- `terraform/terraform.localstack.tfvars` - LocalStack development
- `../src/main/java/com/project/config/AppConfig.java` - Application config

**Token Secret Name**: `external-api/token`

**Token Values**:

- **Client ID**: `ce43d3bd-e1e0-4eed-a269-8bffe958f0fb`
- **Client Secret**: `aRZdZP63VqTmhfLcSE9zbAjG`

## Documentation

All infrastructure documentation is organized in the `docs/` subdirectory:

- **IaC_DEPLOYMENT_GUIDE.md** - Complete deployment guide for both CloudFormation and Terraform
- **TERRAFORM_LOCALSTACK_SETUP.md** - Detailed Terraform setup with LocalStack
- **LOCALSTACK_INSTALLATION.md** - LocalStack installation and setup guide
- **LOCALSTACK_COMMANDS.md** - AWS CLI commands for LocalStack

## File Descriptions

### CloudFormation

- **cloudformation-secrets.yaml** - Creates Secrets Manager secret and IAM role

### Terraform

- **main.tf** - Terraform configuration with LocalStack endpoint support
- **terraform.tfvars** - AWS production variables
- **terraform.localstack.tfvars** - LocalStack development variables

### Docker

- **docker-compose.yml** - LocalStack container configuration
- **init-aws.sh** - Initialization script for LocalStack resources
- **localstack-helper.bat** - Windows helper script
- **localstack-helper.sh** - Mac/Linux helper script

## Environment-Specific Configurations

### LocalStack Development

- Use `terraform.localstack.tfvars` with Terraform
- LocalStack runs on `http://localhost:4566`
- Use dummy AWS credentials (test/test)

### AWS Production

- Use `terraform.tfvars` with Terraform
- Use CloudFormation with real AWS credentials
- Update credentials before deployment

## Important Notes

1. **Token Credentials**: The credentials stored in Terraform tfvars and AppConfig.java are identical
2. **LocalStack vs AWS**: Switch between environments using different tfvars files
3. **Infrastructure as Code**: All infrastructure is version-controlled and reproducible
4. **No Manual Steps**: Everything is automated via CloudFormation or Terraform

## Next Steps

1. Review the appropriate documentation in `docs/`
2. Choose between CloudFormation or Terraform
3. Deploy to LocalStack for testing
4. Deploy to AWS when ready

---

**Status**: Production Ready
**Last Updated**: December 27, 2025

