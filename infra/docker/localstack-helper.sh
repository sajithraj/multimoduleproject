#!/bin/bash

# LocalStack Helper Script
# Automates LocalStack setup and testing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ENDPOINT_URL="http://localhost:4566"
LAMBDA_FUNCTION_NAME="my-token-auth-lambda"
LAMBDA_HANDLER="org.example.Main::handleRequest"
LAMBDA_RUNTIME="java21"
LAMBDA_MEMORY=512
LAMBDA_TIMEOUT=60
IAM_ROLE="lambda-execution-role"
IAM_ROLE_ARN="arn:aws:iam::000000000000:role/$IAM_ROLE"
SECRET_NAME="external-api/token"

# AWS Credentials
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Start LocalStack
start_localstack() {
    log_info "Starting LocalStack..."
    docker-compose up -d

    log_info "Waiting for LocalStack to be ready..."
    sleep 30

    # Check if ready
    if curl -s $ENDPOINT_URL | grep -q "not"; then
        log_success "LocalStack is ready!"
    else
        log_warning "LocalStack may still be initializing..."
    fi
}

# Stop LocalStack
stop_localstack() {
    log_info "Stopping LocalStack..."
    docker-compose down
    log_success "LocalStack stopped"
}

# Check if LocalStack is running
check_localstack() {
    if ! curl -s $ENDPOINT_URL > /dev/null 2>&1; then
        log_error "LocalStack is not running at $ENDPOINT_URL"
        exit 1
    fi
    log_success "LocalStack is running"
}

# Build JAR
build_jar() {
    log_info "Building JAR with Maven..."
    mvn clean install -q
    log_success "JAR built successfully"
}

# Create/Update Lambda function
create_lambda() {
    log_info "Creating Lambda function..."

    # Check if function exists
    if aws lambda get-function \
        --function-name $LAMBDA_FUNCTION_NAME \
        --endpoint-url $ENDPOINT_URL > /dev/null 2>&1; then

        log_warning "Function already exists, updating..."
        aws lambda update-function-code \
            --function-name $LAMBDA_FUNCTION_NAME \
            --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
            --endpoint-url $ENDPOINT_URL > /dev/null
    else
        log_info "Creating new function..."
        aws lambda create-function \
            --function-name $LAMBDA_FUNCTION_NAME \
            --runtime $LAMBDA_RUNTIME \
            --role $IAM_ROLE_ARN \
            --handler $LAMBDA_HANDLER \
            --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
            --environment Variables="{
                EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com,
                TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token,
                CLIENT_ID=test-client-id,
                CLIENT_SECRET=test-client-secret,
                TOKEN_SECRET_NAME=$SECRET_NAME
            }" \
            --timeout $LAMBDA_TIMEOUT \
            --memory-size $LAMBDA_MEMORY \
            --endpoint-url $ENDPOINT_URL > /dev/null
    fi

    log_success "Lambda function created/updated"
}

# Invoke Lambda
invoke_lambda() {
    local payload="${1:-'{}'}"

    log_info "Invoking Lambda function..."
    log_info "Payload: $payload"

    aws lambda invoke \
        --function-name $LAMBDA_FUNCTION_NAME \
        --payload "$payload" \
        --endpoint-url $ENDPOINT_URL \
        response.json > /dev/null

    log_success "Lambda invoked successfully"

    echo ""
    log_info "Response:"
    cat response.json | jq . 2>/dev/null || cat response.json
    echo ""
}

# View logs
view_logs() {
    log_info "Fetching CloudWatch logs..."
    aws logs tail /aws/lambda/$LAMBDA_FUNCTION_NAME \
        --follow \
        --endpoint-url $ENDPOINT_URL
}

# View secrets
view_secrets() {
    log_info "Listing Secrets..."
    aws secretsmanager list-secrets \
        --endpoint-url $ENDPOINT_URL | jq .

    echo ""
    log_info "Getting secret: $SECRET_NAME"
    aws secretsmanager get-secret-value \
        --secret-id $SECRET_NAME \
        --endpoint-url $ENDPOINT_URL | jq .
}

# Full setup
full_setup() {
    log_info "Starting full setup..."
    echo ""

    start_localstack
    check_localstack

    echo ""
    build_jar

    echo ""
    create_lambda

    echo ""
    log_success "Setup complete!"
    echo ""
    log_info "Next steps:"
    echo "  1. Test token auth: ./localstack-helper.sh invoke '{\"action\":\"token\"}'"
    echo "  2. View logs: ./localstack-helper.sh logs"
    echo "  3. View secrets: ./localstack-helper.sh secrets"
    echo "  4. Stop: ./localstack-helper.sh stop"
}

# Usage
usage() {
    cat << EOF
LocalStack Helper Script

Usage: $0 <command> [args]

Commands:
  start          Start LocalStack
  stop           Stop LocalStack
  status         Check LocalStack status
  build          Build JAR with Maven
  setup          Create/update Lambda function
  invoke <payload>  Invoke Lambda (default: {})
  logs           View CloudWatch logs (follow mode)
  secrets        View Secrets Manager
  full           Complete setup (start + build + create)
  clean          Stop and remove all containers
  help           Show this help message

Examples:
  $0 full                          # Full setup
  $0 invoke '{}'                   # Invoke with empty payload
  $0 invoke '{"action":"test"}'    # Invoke with custom payload
  $0 logs                          # Follow logs
  $0 secrets                       # View secrets

EOF
}

# Main
case "${1:-help}" in
    start)
        start_localstack
        ;;
    stop)
        stop_localstack
        ;;
    status)
        check_localstack
        ;;
    build)
        build_jar
        ;;
    setup)
        check_localstack
        create_lambda
        ;;
    invoke)
        check_localstack
        invoke_lambda "${2:-{}}"
        ;;
    logs)
        check_localstack
        view_logs
        ;;
    secrets)
        check_localstack
        view_secrets
        ;;
    full)
        full_setup
        ;;
    clean)
        docker-compose down -v
        log_success "Cleaned up all containers and volumes"
        ;;
    help)
        usage
        ;;
    *)
        log_error "Unknown command: $1"
        echo ""
        usage
        exit 1
        ;;
esac

