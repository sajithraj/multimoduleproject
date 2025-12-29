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
    }
  }

  # LocalStack credentials (dummy values)
  skip_credentials_validation = var.use_localstack
  skip_metadata_api_check     = var.use_localstack
  skip_requesting_account_id  = var.use_localstack
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

# ==============================================
# API GATEWAY RESOURCES (COMMENTED - USE LAMBDA DIRECT)
# ==============================================
# Note: LocalStack API Gateway service has limitations
# Testing Lambda directly via aws lambda invoke is more reliable
# API Gateway resources can be uncommented for AWS production deployment

# resource "aws_api_gateway_rest_api" "token_api" {
#   name        = "token-auth-api-${var.environment}"
#   description = "API Gateway for OAuth2 Token Authorization"
#
#   tags = {
#     Environment = var.environment
#     Application = "StableCoinLambda"
#     ManagedBy   = "Terraform"
#   }
# }
#
# resource "aws_api_gateway_resource" "api_resource" {
#   rest_api_id = aws_api_gateway_rest_api.token_api.id
#   parent_id   = aws_api_gateway_rest_api.token_api.root_resource_id
#   path_part   = "api"
# }
#
# resource "aws_api_gateway_resource" "auth_resource" {
#   rest_api_id = aws_api_gateway_rest_api.token_api.id
#   parent_id   = aws_api_gateway_resource.api_resource.id
#   path_part   = "auth"
# }
#
# resource "aws_api_gateway_method" "post_method" {
#   rest_api_id      = aws_api_gateway_rest_api.token_api.id
#   resource_id      = aws_api_gateway_resource.auth_resource.id
#   http_method      = "POST"
#   authorization    = "NONE"
#   api_key_required = false
# }
#
# resource "aws_api_gateway_integration" "lambda_integration" {
#   rest_api_id             = aws_api_gateway_rest_api.token_api.id
#   resource_id             = aws_api_gateway_resource.auth_resource.id
#   http_method             = aws_api_gateway_method.post_method.http_method
#   type                    = "AWS_PROXY"
#   integration_http_method = "POST"
#   uri                     = aws_lambda_function.token_auth_lambda.invoke_arn
# }
#
# resource "aws_lambda_permission" "api_gateway_invoke" {
#   statement_id  = "AllowAPIGatewayInvoke"
#   action        = "lambda:InvokeFunction"
#   function_name = aws_lambda_function.token_auth_lambda.function_name
#   principal     = "apigateway.amazonaws.com"
#   source_arn    = "${aws_api_gateway_rest_api.token_api.execution_arn}/*/*"
# }
#
# resource "aws_api_gateway_deployment" "api_deployment" {
#   rest_api_id = aws_api_gateway_rest_api.token_api.id
#
#   depends_on = [
#     aws_api_gateway_integration.lambda_integration,
#     aws_lambda_permission.api_gateway_invoke
#   ]
# }
#
# resource "aws_api_gateway_stage" "api_stage" {
#   deployment_id = aws_api_gateway_deployment.api_deployment.id
#   rest_api_id   = aws_api_gateway_rest_api.token_api.id
#   stage_name    = var.environment
#
#   tags = {
#     Environment = var.environment
#     Application = "StableCoinLambda"
#     ManagedBy   = "Terraform"
#   }
# }

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

