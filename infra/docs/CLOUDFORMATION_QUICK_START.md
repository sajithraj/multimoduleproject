# CloudFormation Quick Start

## 🚀 Deploy in 3 Steps

### Step 1: Update the YAML file (optional)

Edit `cloudformation-secrets.yaml`, change these lines:

```yaml
Parameters:
  ClientId:
    Default: 'your-real-client-id'
  
  ClientSecret:
    Default: 'your-real-client-secret'
```

### Step 2: Deploy via CLI

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

### Step 3: Verify

```bash
# Check status
aws cloudformation describe-stacks \
  --stack-name stablecoin-secrets-stack \
  --region us-east-1

# Verify secret created
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --region us-east-1
```

---

## 📝 File: cloudformation-secrets.yaml

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: 'Create Secrets Manager secret and IAM role'

Parameters:
  ClientId:
    Type: String
    Default: 'test-client-id'
  ClientSecret:
    Type: String
    Default: 'test-client-secret'
    NoEcho: true
  SecretName:
    Type: String
    Default: 'external-api/token'
  Environment:
    Type: String
    Default: 'dev'

Resources:
  OAuthSecret:
    Type: AWS::SecretsManager::Secret
    Properties:
      Name: !Ref SecretName
      SecretString: !Sub |
        {
          "client_id": "${ClientId}",
          "client_secret": "${ClientSecret}"
        }

  LambdaExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      RoleName: !Sub 'lambda-execution-role-${Environment}'
      AssumeRolePolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Principal:
              Service: lambda.amazonaws.com
            Action: sts:AssumeRole
      Policies:
        - PolicyName: SecretsManagerAccess
          PolicyDocument:
            Version: '2012-10-17'
            Statement:
              - Effect: Allow
                Action:
                  - secretsmanager:GetSecretValue
                Resource: !GetAtt OAuthSecret.Arn

Outputs:
  SecretArn:
    Value: !GetAtt OAuthSecret.Arn
  LambdaRoleArn:
    Value: !GetAtt LambdaExecutionRole.Arn
```

---

## ✅ What Gets Created

- ✅ AWS Secrets Manager Secret (external-api/token)
- ✅ IAM Role for Lambda (lambda-execution-role-dev)
- ✅ IAM Policy to access the secret

---

## 🗑️ Delete Stack

```bash
aws cloudformation delete-stack --stack-name stablecoin-secrets-stack
```

---

**Status**: ✅ Ready to deploy

