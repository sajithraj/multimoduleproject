# Quick Deploy to LocalStack
# Run this script when you get credential errors

Write-Host "`n=== Quick LocalStack Deploy ===" -ForegroundColor Cyan

# 1. Set credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

# 2. Check LocalStack
Write-Host "`nChecking LocalStack..." -ForegroundColor Yellow
try {
    curl http://localhost:4566/_localstack/health -UseBasicParsing -TimeoutSec 3 | Out-Null
    Write-Host "✓ LocalStack is running" -ForegroundColor Green
} catch {
    Write-Host "✗ LocalStack not running!" -ForegroundColor Red
    Write-Host "Starting LocalStack..." -ForegroundColor Yellow
    cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject\infra\docker
    docker-compose up -d
    Write-Host "Waiting 30 seconds for LocalStack..." -ForegroundColor Cyan
    Start-Sleep -Seconds 30
    cd ..\..
}

# 3. Build
Write-Host "`nBuilding..." -ForegroundColor Yellow
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
mvn clean package -DskipTests -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Build complete" -ForegroundColor Green

# 4. Deploy
Write-Host "`nDeploying..." -ForegroundColor Yellow
cd infra\terraform
terraform apply -var="use_localstack=true" -var="environment=dev" -auto-approve

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ SUCCESS!" -ForegroundColor Green
    Write-Host "`nTest with:" -ForegroundColor Yellow
    Write-Host "  .\test-taskservice.ps1" -ForegroundColor Cyan
} else {
    Write-Host "`n✗ Deploy failed" -ForegroundColor Red
}

