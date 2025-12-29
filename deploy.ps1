# Build and deployment script for Java Lambda function (Windows PowerShell)
# This script builds the JAR, creates Secrets Manager secret, and deploys to Lambda

param(
    [string]$Region = "us-east-1",
    [string]$ExternalApiUrl = "https://api.example.com/endpoint",
    [string]$TokenSecretName = "external-api-token",
    [string]$Memory = "512",
    [string]$Timeout = "30"
)

$ProjectName = "SetUpProject"
$FunctionName = "external-api-lambda"
$RoleName = "lambda-execution-role"
$JarFile = "target\${ProjectName}-1.0-SNAPSHOT.jar"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Java Lambda Deployment Script (Windows)" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Step 1: Build project
Write-Host ""
Write-Host "Step 1: Building Maven project..." -ForegroundColor Yellow
mvn clean package -DskipTests

if (-not (Test-Path $JarFile)) {
    Write-Host "Error: JAR file not found: $JarFile" -ForegroundColor Red
    exit 1
}
Write-Host "✓ JAR built successfully: $JarFile" -ForegroundColor Green

# Step 2: Create/Update Secrets Manager Secret
Write-Host ""
Write-Host "Step 2: Creating/updating Secrets Manager secret..." -ForegroundColor Yellow

$SecretJson = @{
    token = "your_auth_token_here"
} | ConvertTo-Json

$SecretExists = $false
try {
    aws secretsmanager describe-secret `
        --secret-id $TokenSecretName `
        --region $Region `
        --query 'ARN' `
        --output text | Out-Null
    $SecretExists = $true
} catch {
    $SecretExists = $false
}

if ($SecretExists) {
    aws secretsmanager update-secret `
        --secret-id $TokenSecretName `
        --region $Region `
        --secret-string $SecretJson
} else {
    aws secretsmanager create-secret `
        --name $TokenSecretName `
        --region $Region `
        --secret-string $SecretJson
}

Write-Host "✓ Secret created/updated: $TokenSecretName" -ForegroundColor Green
Write-Host "  ⚠️  IMPORTANT: Update the token value in AWS Secrets Manager Console!" -ForegroundColor Yellow

# Step 3: Check if IAM role exists
Write-Host ""
Write-Host "Step 3: Checking IAM role..." -ForegroundColor Yellow

$RoleExists = $false
try {
    aws iam get-role --role-name $RoleName | Out-Null
    $RoleExists = $true
} catch {
    $RoleExists = $false
}

if (-not $RoleExists) {
    Write-Host "Creating IAM role: $RoleName" -ForegroundColor Yellow

    $TrustPolicy = Get-Content "trust-policy.json" -Raw
    aws iam create-role `
        --role-name $RoleName `
        --assume-role-policy-document $TrustPolicy

    Write-Host "Attaching policies..." -ForegroundColor Yellow
    aws iam attach-role-policy `
        --role-name $RoleName `
        --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

    $SecretsPolicy = Get-Content "secrets-policy.json" -Raw
    aws iam put-role-policy `
        --role-name $RoleName `
        --policy-name secrets-access `
        --policy-document $SecretsPolicy

    Write-Host "Waiting for role to be available..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
}

Write-Host "✓ IAM role ready: $RoleName" -ForegroundColor Green

# Step 4: Get role ARN
$RoleArn = (aws iam get-role --role-name $RoleName --query 'Role.Arn' --output text)
Write-Host "  Role ARN: $RoleArn" -ForegroundColor Gray

# Step 5: Create or update Lambda function
Write-Host ""
Write-Host "Step 5: Creating/updating Lambda function..." -ForegroundColor Yellow

$FunctionExists = $false
try {
    aws lambda get-function --function-name $FunctionName --region $Region | Out-Null
    $FunctionExists = $true
} catch {
    $FunctionExists = $false
}

if ($FunctionExists) {
    Write-Host "Updating existing function: $FunctionName" -ForegroundColor Yellow

    aws lambda update-function-code `
        --function-name $FunctionName `
        --zip-file fileb://$JarFile `
        --region $Region

    aws lambda update-function-configuration `
        --function-name $FunctionName `
        --runtime java21 `
        --handler org.example.ApiHandler `
        --timeout $Timeout `
        --memory-size $Memory `
        --environment "Variables={EXTERNAL_API_URL=$ExternalApiUrl,TOKEN_SECRET_NAME=$TokenSecretName}" `
        --region $Region
} else {
    Write-Host "Creating new function: $FunctionName" -ForegroundColor Yellow

    aws lambda create-function `
        --function-name $FunctionName `
        --runtime java21 `
        --role $RoleArn `
        --handler org.example.ApiHandler `
        --timeout $Timeout `
        --memory-size $Memory `
        --zip-file fileb://$JarFile `
        --environment "Variables={EXTERNAL_API_URL=$ExternalApiUrl,TOKEN_SECRET_NAME=$TokenSecretName}" `
        --region $Region `
        --architectures x86_64
}

Write-Host "✓ Lambda function deployed: $FunctionName" -ForegroundColor Green

# Step 6: Test the function
Write-Host ""
Write-Host "Step 6: Testing Lambda function..." -ForegroundColor Yellow

$TestPayload = @{
    httpMethod = "GET"
    path       = "/test"
    body       = $null
} | ConvertTo-Json

aws lambda invoke `
    --function-name $FunctionName `
    --region $Region `
    --payload $TestPayload `
    response.json | Out-Null

Write-Host "Response:" -ForegroundColor Yellow
Get-Content response.json | Write-Host
Write-Host ""

# Step 7: Display summary
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Function Details:" -ForegroundColor Cyan
Write-Host "  Name: $FunctionName"
Write-Host "  Role: $RoleName"
Write-Host "  Runtime: java21"
Write-Host "  Handler: org.example.ApiHandler"
Write-Host "  Memory: ${Memory}MB"
Write-Host "  Timeout: ${Timeout}s"
Write-Host ""
Write-Host "Environment Variables:" -ForegroundColor Cyan
Write-Host "  EXTERNAL_API_URL: $ExternalApiUrl"
Write-Host "  TOKEN_SECRET_NAME: $TokenSecretName"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Update token in Secrets Manager:"
Write-Host "     aws secretsmanager update-secret --secret-id $TokenSecretName --secret-string '{""token"":""YOUR_TOKEN""}'"
Write-Host "  2. Create API Gateway integration"
Write-Host "  3. Test via CloudWatch Logs"
Write-Host ""
Write-Host "View logs:" -ForegroundColor Cyan
Write-Host "  aws logs tail /aws/lambda/$FunctionName --follow"
Write-Host ""

