# Deploy to LocalStack - Complete Script
# This script builds the JARs and deploys everything to LocalStack

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LocalStack Deployment Script" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Set credentials FIRST
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Step 0: Check and start LocalStack
Write-Host "Step 0: Checking LocalStack..." -ForegroundColor Yellow

try {
    $health = Invoke-WebRequest -Uri "http://localhost:4566/_localstack/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ LocalStack is running`n" -ForegroundColor Green
} catch {
    Write-Host "LocalStack is not running. Starting it..." -ForegroundColor Yellow

    $dockerPath = "E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\docker"

    if (Test-Path $dockerPath) {
        Push-Location $dockerPath
        docker-compose up -d
        Pop-Location

        Write-Host "Waiting for LocalStack to be ready (30 seconds)..." -ForegroundColor Cyan
        Start-Sleep -Seconds 30

        try {
            $health = Invoke-WebRequest -Uri "http://localhost:4566/_localstack/health" -UseBasicParsing -ErrorAction Stop
            Write-Host "✓ LocalStack started successfully`n" -ForegroundColor Green
        } catch {
            Write-Host "✗ LocalStack failed to start!" -ForegroundColor Red
            Write-Host "Please manually start: cd infra\docker && docker-compose up -d" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host "✗ Docker compose file not found!" -ForegroundColor Red
        exit 1
    }
}

# Step 1: Build JARs
Write-Host "Step 1: Building JAR files..." -ForegroundColor Yellow
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n✗ Build failed! Cannot continue." -ForegroundColor Red
    exit 1
}

Write-Host "✓ Build successful`n" -ForegroundColor Green

# Verify JARs exist
$serviceJar = ".\service\target\service-1.0-SNAPSHOT.jar"
$taskServiceJar = ".\taskService\target\taskService-1.0-SNAPSHOT.jar"

if (-not (Test-Path $serviceJar)) {
    Write-Host "✗ Service JAR not found: $serviceJar" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $taskServiceJar)) {
    Write-Host "✗ TaskService JAR not found: $taskServiceJar" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Service JAR: $(Get-Item $serviceJar | Select-Object -ExpandProperty Length) bytes" -ForegroundColor Green
Write-Host "✓ TaskService JAR: $(Get-Item $taskServiceJar | Select-Object -ExpandProperty Length) bytes`n" -ForegroundColor Green

# Step 2: Deploy with Terraform
Write-Host "Step 3: Deploying to LocalStack with Terraform..." -ForegroundColor Yellow

cd infra\terraform

# Set LocalStack credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# Initialize Terraform (if needed)
if (-not (Test-Path ".terraform")) {
    Write-Host "Initializing Terraform..." -ForegroundColor Cyan
    terraform init
}

# Apply Terraform
Write-Host "Applying Terraform configuration..." -ForegroundColor Cyan
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n✗ Terraform apply failed!" -ForegroundColor Red
    exit 1
}

Write-Host "`n✓ Deployment successful!`n" -ForegroundColor Green

# Step 4: Verify Deployment
Write-Host "Step 4: Verifying deployment..." -ForegroundColor Yellow

Write-Host "`nListing Lambda functions:" -ForegroundColor Cyan
aws lambda list-functions --endpoint-url http://localhost:4566 --query 'Functions[].FunctionName' --output table

Write-Host "`nListing SQS queues:" -ForegroundColor Cyan
aws sqs list-queues --endpoint-url http://localhost:4566

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Run tests: .\test-taskservice.ps1" -ForegroundColor Gray
Write-Host "  2. View logs: aws logs tail /aws/lambda/task-service-dev --follow --endpoint-url http://localhost:4566" -ForegroundColor Gray
Write-Host "  3. Send SQS message: aws sqs send-message --queue-url <queue-url> --message-body '{\"test\":\"data\"}' --endpoint-url http://localhost:4566`n" -ForegroundColor Gray

# Get Terraform outputs
Write-Host "Terraform Outputs:" -ForegroundColor Yellow
terraform output -json | ConvertFrom-Json | ConvertTo-Json -Depth 5

