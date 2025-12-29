# TaskService Lambda - Testing Script
# Execute this in PowerShell

# Set LocalStack credentials
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TaskService Lambda - Test Suite" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check if LocalStack is running
Write-Host "1. Checking LocalStack status..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:4566/_localstack/health" -UseBasicParsing -ErrorAction Stop
    Write-Host "   ✓ LocalStack is running" -ForegroundColor Green
} catch {
    Write-Host "   ✗ LocalStack is NOT running!" -ForegroundColor Red
    Write-Host "   Please start LocalStack first: docker-compose up -d" -ForegroundColor Red
    exit 1
}

# List Lambda functions
Write-Host "`n2. Listing Lambda functions..." -ForegroundColor Yellow
aws lambda list-functions --endpoint-url http://localhost:4566 --query 'Functions[].FunctionName' --output table

# Test 1: API Gateway - GET /ping
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 1: API Gateway - GET /ping" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$apiGatewayPing = @'
{
  "resource": "/ping",
  "path": "/ping",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "queryStringParameters": null,
  "pathParameters": null,
  "body": null,
  "isBase64Encoded": false,
  "requestContext": {
    "requestId": "test-request-id-001",
    "accountId": "123456789012",
    "stage": "dev"
  }
}
'@

$apiGatewayPing | Out-File -FilePath "test-api-ping.json" -Encoding UTF8

Write-Host "Invoking Lambda with API Gateway /ping event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-api-ping.json `
  --endpoint-url http://localhost:4566 `
  response-ping.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-ping.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Test 2: API Gateway - GET /id/{id}
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 2: API Gateway - GET /id/12345" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$apiGatewayGetId = @'
{
  "resource": "/id/{id}",
  "path": "/id/12345",
  "httpMethod": "GET",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "queryStringParameters": null,
  "pathParameters": {
    "id": "12345"
  },
  "body": null,
  "isBase64Encoded": false,
  "requestContext": {
    "requestId": "test-request-id-002",
    "accountId": "123456789012",
    "stage": "dev"
  }
}
'@

$apiGatewayGetId | Out-File -FilePath "test-api-get-id.json" -Encoding UTF8

Write-Host "Invoking Lambda with API Gateway /id/{id} event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-api-get-id.json `
  --endpoint-url http://localhost:4566 `
  response-get-id.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-get-id.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Test 3: API Gateway - POST /tasks
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 3: API Gateway - POST /tasks" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$apiGatewayPostTasks = @'
{
  "resource": "/tasks",
  "path": "/tasks",
  "httpMethod": "POST",
  "headers": {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
  "queryStringParameters": null,
  "pathParameters": null,
  "body": "{\"taskName\":\"Process Order\",\"taskData\":{\"orderId\":\"ORD-001\",\"amount\":99.99}}",
  "isBase64Encoded": false,
  "requestContext": {
    "requestId": "test-request-id-003",
    "accountId": "123456789012",
    "stage": "dev"
  }
}
'@

$apiGatewayPostTasks | Out-File -FilePath "test-api-post-tasks.json" -Encoding UTF8

Write-Host "Invoking Lambda with API Gateway POST /tasks event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-api-post-tasks.json `
  --endpoint-url http://localhost:4566 `
  response-post-tasks.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-post-tasks.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Test 4: SQS Event - Single Message
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 4: SQS Event - Single Message" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$sqsSingleMessage = @'
{
  "Records": [
    {
      "messageId": "msg-001",
      "receiptHandle": "receipt-001",
      "body": "{\"orderId\":\"ORD-12345\",\"amount\":150.50,\"customer\":\"John Doe\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1735470000000"
      },
      "messageAttributes": {},
      "md5OfBody": "test-md5",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev",
      "awsRegion": "us-east-1"
    }
  ]
}
'@

$sqsSingleMessage | Out-File -FilePath "test-sqs-single.json" -Encoding UTF8

Write-Host "Invoking Lambda with SQS single message event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-sqs-single.json `
  --endpoint-url http://localhost:4566 `
  response-sqs-single.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-sqs-single.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Test 5: SQS Event - Batch Messages
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 5: SQS Event - Batch (3 messages)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$sqsBatch = @'
{
  "Records": [
    {
      "messageId": "msg-001",
      "receiptHandle": "receipt-001",
      "body": "{\"orderId\":\"ORD-001\",\"amount\":100}",
      "attributes": {"ApproximateReceiveCount": "1"},
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev"
    },
    {
      "messageId": "msg-002",
      "receiptHandle": "receipt-002",
      "body": "{\"orderId\":\"ORD-002\",\"amount\":200}",
      "attributes": {"ApproximateReceiveCount": "1"},
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev"
    },
    {
      "messageId": "msg-003",
      "receiptHandle": "receipt-003",
      "body": "{\"orderId\":\"ORD-003\",\"amount\":300}",
      "attributes": {"ApproximateReceiveCount": "1"},
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:task-queue-dev"
    }
  ]
}
'@

$sqsBatch | Out-File -FilePath "test-sqs-batch.json" -Encoding UTF8

Write-Host "Invoking Lambda with SQS batch event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-sqs-batch.json `
  --endpoint-url http://localhost:4566 `
  response-sqs-batch.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-sqs-batch.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Test 6: EventBridge - Scheduled Event
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 6: EventBridge - Scheduled Event" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$ebScheduled = @'
{
  "version": "0",
  "id": "event-001",
  "detail-type": "Scheduled Event",
  "source": "aws.events",
  "account": "123456789012",
  "time": "2025-12-29T10:00:00Z",
  "region": "us-east-1",
  "resources": ["arn:aws:events:us-east-1:123456789012:rule/task-schedule-dev"],
  "detail": {}
}
'@

$ebScheduled | Out-File -FilePath "test-eb-scheduled.json" -Encoding UTF8

Write-Host "Invoking Lambda with EventBridge scheduled event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-eb-scheduled.json `
  --endpoint-url http://localhost:4566 `
  response-eb-scheduled.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-eb-scheduled.json

# Test 7: EventBridge - Custom Event (OrderCreated)
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test 7: EventBridge - Custom Event (OrderCreated)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$ebOrderCreated = @'
{
  "version": "0",
  "id": "custom-event-001",
  "detail-type": "OrderCreated",
  "source": "com.project.orders",
  "account": "123456789012",
  "time": "2025-12-29T10:00:00Z",
  "region": "us-east-1",
  "resources": [],
  "detail": {
    "orderId": "ORD-98765",
    "customerId": "CUST-12345",
    "amount": 250.75
  }
}
'@

$ebOrderCreated | Out-File -FilePath "test-eb-order.json" -Encoding UTF8

Write-Host "Invoking Lambda with EventBridge OrderCreated event..." -ForegroundColor Yellow
aws lambda invoke `
  --function-name task-service-dev `
  --payload file://test-eb-order.json `
  --endpoint-url http://localhost:4566 `
  response-eb-order.json

Write-Host "`nResponse:" -ForegroundColor Green
Get-Content response-eb-order.json

# Check Lambda Logs
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Checking Lambda Logs (last 20 lines)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

aws logs tail /aws/lambda/task-service-dev `
  --endpoint-url http://localhost:4566 `
  --format short `
  | Select-Object -Last 20

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "All Tests Completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host "`nTest files created:" -ForegroundColor Yellow
Write-Host "  - test-api-ping.json" -ForegroundColor Gray
Write-Host "  - test-api-get-id.json" -ForegroundColor Gray
Write-Host "  - test-api-post-tasks.json" -ForegroundColor Gray
Write-Host "  - test-sqs-single.json" -ForegroundColor Gray
Write-Host "  - test-sqs-batch.json" -ForegroundColor Gray
Write-Host "  - test-eb-scheduled.json" -ForegroundColor Gray
Write-Host "  - test-eb-order.json" -ForegroundColor Gray

Write-Host "`nResponse files created:" -ForegroundColor Yellow
Write-Host "  - response-ping.json" -ForegroundColor Gray
Write-Host "  - response-get-id.json" -ForegroundColor Gray
Write-Host "  - response-post-tasks.json" -ForegroundColor Gray
Write-Host "  - response-sqs-single.json" -ForegroundColor Gray
Write-Host "  - response-sqs-batch.json" -ForegroundColor Gray
Write-Host "  - response-eb-scheduled.json" -ForegroundColor Gray
Write-Host "  - response-eb-order.json" -ForegroundColor Gray

Write-Host "`n✓ Testing complete! Check the response files for results." -ForegroundColor Green

