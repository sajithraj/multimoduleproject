#!/bin/bash

# LocalStack Initialization Script
# Creates necessary AWS resources for testing

set -e

echo "============================================"
echo "LocalStack Initialization Started"
echo "============================================"

# Set AWS credentials
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# LocalStack endpoint
ENDPOINT_URL="http://localhost:4566"

echo "Creating Secrets Manager secret..."
aws secretsmanager create-secret \
  --name external-api/token \
  --description "External API Credentials" \
  --secret-string '{"client_id":"test-client-id","client_secret":"test-client-secret"}' \
  --endpoint-url=$ENDPOINT_URL \
  --region us-east-1 || echo "Secret already exists"

echo "✓ Secret created: external-api/token"

echo ""
echo "Creating IAM Role for Lambda..."
aws iam create-role \
  --role-name lambda-execution-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "lambda.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }' \
  --endpoint-url=$ENDPOINT_URL \
  --region us-east-1 || echo "Role already exists"

echo "✓ IAM Role created: lambda-execution-role"

echo ""
echo "Attaching policy to role..."
aws iam put-role-policy \
  --role-name lambda-execution-role \
  --policy-name lambda-policy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "secretsmanager:GetSecretValue",
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ],
        "Resource": "*"
      }
    ]
  }' \
  --endpoint-url=$ENDPOINT_URL \
  --region us-east-1 || echo "Policy already attached"

echo "✓ Policy attached to role"

echo ""
echo "============================================"
echo "LocalStack Initialization Complete!"
echo "============================================"
echo ""
echo "Resources Created:"
echo "  • Secret: external-api/token"
echo "  • IAM Role: lambda-execution-role"
echo ""
echo "Endpoint: http://localhost:4566"
echo "LocalStack UI: http://localhost:8080"
echo ""

