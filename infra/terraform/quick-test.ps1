# Quick Test - Verify LinkedHashMap Fix

$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

Write-Host "`n=== Testing API Gateway /ping ===" -ForegroundColor Cyan

$pingPayload = @'
{
  "resource": "/ping",
  "path": "/ping",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json"
  },
  "body": null
}
'@

$pingPayload | Out-File test-ping.json -Encoding UTF8

Write-Host "Invoking Lambda..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-ping.json `
  --endpoint-url http://localhost:4566 `
  response.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

Write-Host "`n=== Checking Logs ===" -ForegroundColor Cyan
aws logs tail /aws/lambda/task-service-dev `
  --endpoint-url http://localhost:4566 `
  --format short `
  | Select-Object -Last 15

