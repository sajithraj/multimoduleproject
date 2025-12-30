# Docker Configuration

**LocalStack setup for local AWS development and testing.**

---

## 📋 Overview

This directory contains Docker configuration for running LocalStack, which provides a local AWS cloud stack for
development and testing without incurring AWS costs.

---

## 🚀 Quick Start

### Start LocalStack

```powershell
# Navigate to docker directory
cd infra/docker

# Start LocalStack container
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Stop LocalStack

```powershell
# Stop container
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## 📁 Files

| File                    | Purpose                                             |
|-------------------------|-----------------------------------------------------|
| `docker-compose.yml`    | Docker Compose configuration for LocalStack         |
| `init-aws.sh`           | Initialization script for AWS resources             |
| `localstack-helper.sh`  | Helper script for LocalStack operations (Linux/Mac) |
| `localstack-helper.bat` | Helper script for LocalStack operations (Windows)   |

---

## 🔧 Configuration

### docker-compose.yml

**Services:**

- LocalStack container
- Port 4566 exposed for AWS API
- Configured AWS services: Lambda, Secrets Manager, CloudWatch Logs, IAM, API Gateway, SQS, EventBridge

**Environment Variables:**

```yaml
- DEBUG=0
- DOCKER_HOST=unix:///var/run/docker.sock
- LAMBDA_EXECUTOR=docker
- AWS_DEFAULT_REGION=us-east-1
- AWS_ACCESS_KEY_ID=test
- AWS_SECRET_ACCESS_KEY=test
```

**Enabled Services:**

- Lambda
- Secrets Manager
- CloudWatch Logs
- IAM
- API Gateway
- SQS
- EventBridge
- CloudWatch

---

## 🧪 Health Check

LocalStack includes a health check endpoint to verify services are ready:

```powershell
# Check health status
curl http://localhost:4566/_localstack/health

# Or using Invoke-WebRequest
Invoke-WebRequest -Uri "http://localhost:4566/_localstack/health" | Select-Object -ExpandProperty Content
```

---

## 🔍 Troubleshooting

### Container Won't Start

```powershell
# Check Docker is running
docker ps

# View logs
docker-compose logs

# Restart container
docker-compose restart
```

### Port 4566 Already in Use

```powershell
# Find process using port
netstat -ano | findstr :4566

# Kill process or change port in docker-compose.yml
```

### Services Not Available

```powershell
# Wait for health check to pass
docker-compose logs -f

# Check health endpoint
curl http://localhost:4566/_localstack/health
```

### Lambda Execution Issues

Ensure Docker socket is mounted correctly:

- Windows: Docker Desktop must be running
- Linux/Mac: /var/run/docker.sock must be accessible

---

## 📊 Accessing LocalStack

### AWS CLI Configuration

```powershell
# Set environment variables
$env:AWS_ACCESS_KEY_ID="test"
$env:AWS_SECRET_ACCESS_KEY="test"
$env:AWS_DEFAULT_REGION="us-east-1"

# Use with AWS CLI
aws --endpoint-url=http://localhost:4566 lambda list-functions
aws --endpoint-url=http://localhost:4566 sqs list-queues
aws --endpoint-url=http://localhost:4566 apigateway get-rest-apis
```

### Terraform Configuration

See `../terraform/README.md` for Terraform setup with LocalStack.

---

## 🧹 Cleanup

### Remove All Resources

```powershell
# Stop and remove containers
docker-compose down

# Remove containers, networks, and volumes
docker-compose down -v

# Remove images (optional)
docker rmi localstack/localstack:latest
```

---

## 📚 LocalStack Features

**Free Tier Includes:**

- Lambda
- API Gateway
- SQS
- EventBridge
- CloudWatch Logs
- IAM
- Secrets Manager
- And more...

**Pro Features (Not Required):**

- Advanced Lambda features
- EKS
- RDS
- And more...

---

## 🔗 References

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [AWS CLI with LocalStack](https://docs.localstack.cloud/user-guide/integrations/aws-cli/)

---

## ⚙️ System Requirements

- Docker Desktop (Windows/Mac) or Docker Engine (Linux)
- Docker Compose
- 2GB+ RAM allocated to Docker
- Port 4566 available

---

**Status:** ✅ Production Ready  
**LocalStack Version:** Latest  
**Docker Compose Version:** >= 2.0

