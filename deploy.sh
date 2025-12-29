#!/bin/bash

# Build and deployment script for Java Lambda function
# This script builds the JAR, creates Secrets Manager secret, and deploys to Lambda

set -e

PROJECT_NAME="SetUpProject"
FUNCTION_NAME="external-api-lambda"
ROLE_NAME="lambda-execution-role"
REGION="us-east-1"  # Change as needed
EXTERNAL_API_URL="https://api.example.com/endpoint"  # Update this
TOKEN_SECRET_NAME="external-api-token"  # Update this
MEMORY="512"
TIMEOUT="30"

echo "=========================================="
echo "Java Lambda Deployment Script"
echo "=========================================="

# Step 1: Build project
echo ""
echo "Step 1: Building Maven project..."
mvn clean package -DskipTests

JAR_FILE="target/${PROJECT_NAME}-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    exit 1
fi
echo "✓ JAR built successfully: $JAR_FILE"

# Step 2: Create/Update Secrets Manager Secret
echo ""
echo "Step 2: Creating/updating Secrets Manager secret..."
SECRET_JSON='{
  "token": "your_auth_token_here"
}'

aws secretsmanager create-secret \
  --name $TOKEN_SECRET_NAME \
  --region $REGION \
  --secret-string "$SECRET_JSON" \
  2>/dev/null || \
aws secretsmanager update-secret \
  --secret-id $TOKEN_SECRET_NAME \
  --region $REGION \
  --secret-string "$SECRET_JSON"

echo "✓ Secret created/updated: $TOKEN_SECRET_NAME"
echo "  ⚠️  IMPORTANT: Update the token value in AWS Secrets Manager Console!"

# Step 3: Check if IAM role exists
echo ""
echo "Step 3: Checking IAM role..."
if ! aws iam get-role --role-name $ROLE_NAME 2>/dev/null; then
    echo "Creating IAM role: $ROLE_NAME"
    aws iam create-role \
      --role-name $ROLE_NAME \
      --assume-role-policy-document file://trust-policy.json

    echo "Attaching policies..."
    aws iam attach-role-policy \
      --role-name $ROLE_NAME \
      --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

    aws iam put-role-policy \
      --role-name $ROLE_NAME \
      --policy-name secrets-access \
      --policy-document file://secrets-policy.json

    echo "Waiting for role to be available..."
    sleep 10
fi
echo "✓ IAM role ready: $ROLE_NAME"

# Step 4: Get role ARN
ROLE_ARN=$(aws iam get-role --role-name $ROLE_NAME --query 'Role.Arn' --output text)
echo "  Role ARN: $ROLE_ARN"

# Step 5: Create or update Lambda function
echo ""
echo "Step 5: Creating/updating Lambda function..."

if aws lambda get-function --function-name $FUNCTION_NAME --region $REGION 2>/dev/null; then
    echo "Updating existing function: $FUNCTION_NAME"
    aws lambda update-function-code \
      --function-name $FUNCTION_NAME \
      --zip-file fileb://$JAR_FILE \
      --region $REGION

    aws lambda update-function-configuration \
      --function-name $FUNCTION_NAME \
      --runtime java21 \
      --handler org.example.ApiHandler \
      --timeout $TIMEOUT \
      --memory-size $MEMORY \
      --environment "Variables={EXTERNAL_API_URL=$EXTERNAL_API_URL,TOKEN_SECRET_NAME=$TOKEN_SECRET_NAME}" \
      --region $REGION
else
    echo "Creating new function: $FUNCTION_NAME"
    aws lambda create-function \
      --function-name $FUNCTION_NAME \
      --runtime java21 \
      --role $ROLE_ARN \
      --handler org.example.ApiHandler \
      --timeout $TIMEOUT \
      --memory-size $MEMORY \
      --zip-file fileb://$JAR_FILE \
      --environment "Variables={EXTERNAL_API_URL=$EXTERNAL_API_URL,TOKEN_SECRET_NAME=$TOKEN_SECRET_NAME}" \
      --region $REGION \
      --architectures x86_64
fi

echo "✓ Lambda function deployed: $FUNCTION_NAME"

# Step 6: Test the function
echo ""
echo "Step 6: Testing Lambda function..."
TEST_RESPONSE=$(aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --payload '{"httpMethod":"GET","path":"/test","body":null}' \
  response.json)

echo "Response:"
cat response.json
echo ""
echo ""

# Step 7: Display summary
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
echo "Function Details:"
echo "  Name: $FUNCTION_NAME"
echo "  Role: $ROLE_NAME"
echo "  Runtime: java21"
echo "  Handler: org.example.ApiHandler"
echo "  Memory: ${MEMORY}MB"
echo "  Timeout: ${TIMEOUT}s"
echo ""
echo "Environment Variables:"
echo "  EXTERNAL_API_URL: $EXTERNAL_API_URL"
echo "  TOKEN_SECRET_NAME: $TOKEN_SECRET_NAME"
echo ""
echo "Next steps:"
echo "  1. Update token in Secrets Manager:"
echo "     aws secretsmanager update-secret --secret-id $TOKEN_SECRET_NAME --secret-string '{\"token\":\"YOUR_TOKEN\"}'"
echo "  2. Create API Gateway integration"
echo "  3. Test via CloudWatch Logs"
echo ""
echo "View logs:"
echo "  aws logs tail /aws/lambda/$FUNCTION_NAME --follow"
echo ""

