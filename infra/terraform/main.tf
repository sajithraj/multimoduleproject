terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

variable "use_localstack" {
  description = "Use LocalStack for local development (true) or real AWS (false)"
  type        = bool
  default     = false
}

provider "aws" {
  region = var.aws_region

  # LocalStack configuration (used when use_localstack = true)
  dynamic "endpoints" {
    for_each = var.use_localstack ? [1] : []
    content {
      secretsmanager = "http://localhost:4566"
      iam            = "http://localhost:4566"
      lambda         = "http://localhost:4566"
      logs           = "http://localhost:4566"
      sqs            = "http://localhost:4566"
      cloudwatch     = "http://localhost:4566"
      events         = "http://localhost:4566"
      apigateway     = "http://localhost:4566"
    }
  }

  # LocalStack credentials (dummy values)
  skip_credentials_validation = var.use_localstack
  skip_metadata_api_check     = var.use_localstack
  skip_requesting_account_id  = var.use_localstack

  # For LocalStack, use static credentials
  access_key = var.use_localstack ? "test" : null
  secret_key = var.use_localstack ? "test" : null
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "dev-local", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, dev-local, staging, or prod."
  }
}

variable "secret_name" {
  description = "Name of the Secrets Manager secret"
  type        = string
  default     = "external-api/token"
}

variable "token_endpoint_url" {
  description = "OAuth2 Token Endpoint URL"
  type        = string
  default     = "https://exchange-staging.motiveintegrator.com/v1/authorize/token"
}

variable "external_api_url" {
  description = "External API Endpoint URL"
  type        = string
  default     = "https://exchange-staging.motiveintegrator.com/v2/repairorder/mix-mockservice/roNum/73859"
}

variable "oauth2_timeout_seconds" {
  description = "OAuth2 token request timeout in seconds"
  type        = number
  default     = 3
}

variable "client_id" {
  description = "OAuth2 Username (mapped to client_id internally)"
  type        = string
  sensitive   = true
  default     = "test-client-id"
}

variable "client_secret" {
  description = "OAuth2 Password (mapped to client_secret internally)"
  type        = string
  sensitive   = true
  default     = "test-client-secret"
}

# Secrets Manager Secret
resource "aws_secretsmanager_secret" "oauth_credentials" {
  name                    = var.secret_name
  description             = "OAuth2 credentials for external API integration"
  recovery_window_in_days = 7

  tags = {
    Environment = var.environment
    Application = "StableCoinLambda"
    Purpose     = "OAuth2Credentials"
    ManagedBy   = "Terraform"
  }
}

# Secrets Manager Secret Version (actual secret data)
resource "aws_secretsmanager_secret_version" "oauth_credentials" {
  secret_id = aws_secretsmanager_secret.oauth_credentials.id
  secret_string = jsonencode({
    username = var.client_id
    password = var.client_secret
  })
}

# IAM Role for Lambda to access Secrets Manager
resource "aws_iam_role" "lambda_execution_role" {
  name = "lambda-execution-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Environment = var.environment
    Application = "StableCoinLambda"
    ManagedBy   = "Terraform"
  }
}

# IAM Policy for Lambda to access CloudWatch Logs
resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# IAM Policy for Lambda to access Secrets Manager
resource "aws_iam_role_policy" "secrets_manager_access" {
  name = "secrets-manager-access"
  role = aws_iam_role.lambda_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = aws_secretsmanager_secret.oauth_credentials.arn
      }
    ]
  })
}

# Lambda Function
resource "aws_lambda_function" "token_auth_lambda" {
  filename         = "${path.module}/../../service/target/service-1.0-SNAPSHOT.jar"
  function_name    = "my-token-auth-lambda"
  role             = aws_iam_role.lambda_execution_role.arn
  handler          = "com.project.service.ApiHandler::handleRequest"
  runtime          = "java21"
  timeout          = 60
  memory_size      = 512
  source_code_hash = filebase64sha256("${path.module}/../../service/target/service-1.0-SNAPSHOT.jar")

  environment {
    variables = {
      # Token Configuration
      TOKEN_ENDPOINT_URL     = var.token_endpoint_url
      TOKEN_SECRET_NAME      = var.secret_name
      OAUTH2_TIMEOUT_SECONDS = var.oauth2_timeout_seconds

      # External API Configuration
      EXTERNAL_API_URL = var.external_api_url

      # General Configuration
      LAMBDA_AWS_REGION = var.aws_region
      ENVIRONMENT       = var.environment

      # AWS Lambda Powertools Configuration
      POWERTOOLS_SERVICE_NAME     = "setup-project"
      POWERTOOLS_LOG_LEVEL        = "INFO"
      POWERTOOLS_LOGGER_LOG_EVENT = "true"
    }
  }

  tags = {
    Environment = var.environment
    Application = "StableCoinLambda"
    ManagedBy   = "Terraform"
  }

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic_execution,
    aws_iam_role_policy.secrets_manager_access
  ]
}

# Lambda Log Group
resource "aws_cloudwatch_log_group" "lambda_log_group" {
  name              = "/aws/lambda/my-token-auth-lambda"
  retention_in_days = 14

  tags = {
    Environment = var.environment
    Application = "StableCoinLambda"
    ManagedBy   = "Terraform"
  }
}



# ========================================
# TaskService Lambda (Unified Handler)
# ========================================

# CloudWatch Log Group for TaskService
resource "aws_cloudwatch_log_group" "task_service_log_group" {
  name              = "/aws/lambda/task-service-${var.environment}"
  retention_in_days = 7
}

# TaskService Lambda Function
resource "aws_lambda_function" "task_service_lambda" {

  filename         = "${path.module}/../../taskService/target/taskService-1.0-SNAPSHOT.jar"
  function_name    = "task-service-${var.environment}"
  role             = aws_iam_role.lambda_execution_role.arn
  handler          = "com.project.task.handler.UnifiedTaskHandler::handleRequest"
  runtime          = "java21"
  timeout          = 60
  memory_size      = 512
  source_code_hash = filebase64sha256("${path.module}/../../taskService/target/taskService-1.0-SNAPSHOT.jar")

  environment {
    variables = {
      ENVIRONMENT              = var.environment
      POWERTOOLS_SERVICE_NAME  = "task-service"
      POWERTOOLS_LOG_LEVEL     = "INFO"
      POWERTOOLS_LOGGER_LOG_EVENT = "true"
    }
  }

  depends_on = [
    aws_cloudwatch_log_group.task_service_log_group,
    aws_iam_role_policy_attachment.lambda_basic_execution
  ]
}

# SQS Queue for TaskService
resource "aws_sqs_queue" "task_queue" {
  name                       = "task-queue-${var.environment}"
  delay_seconds              = 0
  max_message_size           = 262144
  message_retention_seconds  = 345600  # 4 days
  receive_wait_time_seconds  = 0
  visibility_timeout_seconds = 90      # 3x Lambda timeout

  # Dead Letter Queue configuration
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.task_dlq.arn
    maxReceiveCount     = 3
  })
}

# Dead Letter Queue for failed messages
resource "aws_sqs_queue" "task_dlq" {
  name                      = "task-queue-dlq-${var.environment}"
  message_retention_seconds = 1209600  # 14 days
}

# Lambda permission for SQS to invoke
resource "aws_lambda_permission" "allow_sqs_invoke" {
  statement_id  = "AllowSQSInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.task_service_lambda.function_name
  principal     = "sqs.amazonaws.com"
  source_arn    = aws_sqs_queue.task_queue.arn
}

# SQS Event Source Mapping
resource "aws_lambda_event_source_mapping" "task_queue_trigger" {
  event_source_arn = aws_sqs_queue.task_queue.arn
  function_name    = aws_lambda_function.task_service_lambda.arn
  batch_size       = 10

  # Enable batch item failures for DLQ support
  function_response_types = ["ReportBatchItemFailures"]

  depends_on = [
    aws_lambda_permission.allow_sqs_invoke
  ]
}

# EventBridge Rule for Scheduled Tasks (cron)
resource "aws_cloudwatch_event_rule" "task_schedule_rule" {
  name                = "task-schedule-${var.environment}"
  description         = "Trigger TaskService Lambda every 5 minutes"
  schedule_expression = "rate(5 minutes)"
}

# EventBridge target
resource "aws_cloudwatch_event_target" "task_schedule_target" {
  rule      = aws_cloudwatch_event_rule.task_schedule_rule.name
  target_id = "TaskServiceLambda"
  arn       = aws_lambda_function.task_service_lambda.arn
}

# Lambda permission for EventBridge
resource "aws_lambda_permission" "allow_eventbridge_invoke" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.task_service_lambda.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.task_schedule_rule.arn
}

# Lambda Function URL for HTTP/REST testing (like API Gateway)
resource "aws_lambda_function_url" "task_service_url" {
  function_name      = aws_lambda_function.task_service_lambda.function_name
  authorization_type = "NONE"  # For testing only - use AWS_IAM in production

  cors {
    allow_credentials = true
    allow_origins     = ["*"]
    allow_methods     = ["*"]
    allow_headers     = ["*"]
    max_age           = 86400
  }
}

# ========================================
# Outputs for TaskService
# ========================================
# Outputs
output "secret_arn" {
  description = "ARN of the created secret"
  value       = aws_secretsmanager_secret.oauth_credentials.arn
}

output "secret_name" {
  description = "Name of the created secret"
  value       = aws_secretsmanager_secret.oauth_credentials.name
}

output "lambda_role_arn" {
  description = "ARN of the Lambda execution role"
  value       = aws_iam_role.lambda_execution_role.arn
}

output "lambda_role_name" {
  description = "Name of the Lambda execution role"
  value       = aws_iam_role.lambda_execution_role.name
}

output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.token_auth_lambda.arn
}

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.token_auth_lambda.function_name
}

output "lambda_log_group_name" {
  description = "CloudWatch Log Group name for Lambda"
  value       = aws_cloudwatch_log_group.lambda_log_group.name
}


output "deployment_summary" {
  description = "Summary of deployed resources"
  value = {
    secret_name          = aws_secretsmanager_secret.oauth_credentials.name
    lambda_function_name = aws_lambda_function.token_auth_lambda.function_name
    lambda_role_name     = aws_iam_role.lambda_execution_role.name
    log_group_name       = aws_cloudwatch_log_group.lambda_log_group.name
    environment          = var.environment
    region               = var.aws_region
  }
}

output "task_service_function_name" {
  description = "TaskService Lambda function name"
  value       = aws_lambda_function.task_service_lambda.function_name
}

output "task_service_function_arn" {
  description = "TaskService Lambda function ARN"
  value       = aws_lambda_function.task_service_lambda.arn
}

output "task_service_function_url" {
  description = "TaskService Lambda Function URL for HTTP testing"
  value       = aws_lambda_function_url.task_service_url.function_url
}

output "task_queue_url" {
  description = "SQS Queue URL for TaskService"
  value       = aws_sqs_queue.task_queue.url
}

output "task_queue_arn" {
  description = "SQS Queue ARN"
  value       = aws_sqs_queue.task_queue.arn
}

output "task_dlq_url" {
  description = "Dead Letter Queue URL"
  value       = aws_sqs_queue.task_dlq.url
}

output "eventbridge_rule_name" {
  description = "EventBridge rule name for scheduled tasks"
  value       = aws_cloudwatch_event_rule.task_schedule_rule.name
}

output "task_service_test_commands" {
  description = "Commands to test TaskService"
  value = {
    # Test with Postman/curl using Function URL
    http_test = "curl -X POST ${aws_lambda_function_url.task_service_url.function_url} -H 'Content-Type: application/json' -d '{\"test\":\"data\"}'"

    # Send message to SQS
    sqs_test = "aws sqs send-message --queue-url ${aws_sqs_queue.task_queue.url} --message-body '{\"orderId\":\"12345\"}' --endpoint-url=http://localhost:4566"

    # Invoke Lambda directly
    direct_test = "aws lambda invoke --function-name ${aws_lambda_function.task_service_lambda.function_name} --payload '{\"test\":\"data\"}' response.json --endpoint-url=http://localhost:4566"

    # Check logs
    logs_test = "aws logs tail /aws/lambda/${aws_lambda_function.task_service_lambda.function_name} --follow --endpoint-url=http://localhost:4566"
  }
}
