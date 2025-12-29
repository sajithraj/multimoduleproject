# LocalStack Testing Environment - Quick Start

Complete Docker environment to test your Lambda function locally with AWS Secrets Manager and Secrets Manager
integration.

---

## 🚀 Quick Start (5 minutes)

### 1. Start Everything

**On Windows:**

```powershell
.\localstack-helper.bat full
```

**On Mac/Linux:**

```bash
chmod +x localstack-helper.sh
./localstack-helper.sh full
```

This will:

- ✅ Start LocalStack Docker container
- ✅ Build your Lambda JAR
- ✅ Create Lambda function with environment variables
- ✅ Setup Secrets Manager with credentials

### 2. Test Your Lambda

**Invoke Lambda (Windows):**

```powershell
.\localstack-helper.bat invoke '{}'
```

**Invoke Lambda (Mac/Linux):**

```bash
./localstack-helper.sh invoke '{}'
```

### 3. View Logs

**Windows:**

```powershell
.\localstack-helper.bat logs
```

**Mac/Linux:**

```bash
./localstack-helper.sh logs
```

---

## 📋 Files Included

| File                            | Purpose                                 |
|---------------------------------|-----------------------------------------|
| **docker-compose.yml**          | LocalStack Docker configuration         |
| **init-aws.sh**                 | Initialization script for AWS resources |
| **localstack-helper.sh**        | Helper script for Mac/Linux             |
| **localstack-helper.bat**       | Helper script for Windows               |
| **LOCALSTACK_TESTING_GUIDE.md** | Detailed testing documentation          |

---

## 🐳 What's in the Docker Environment

### LocalStack Container

- **Services:** Lambda, Secrets Manager, CloudWatch Logs, IAM, API Gateway
- **Port:** 4566 (gateway)
- **Data:** Stored in `/tmp/localstack`

### LocalStack UI (Optional)

- **URL:** http://localhost:8080
- **Features:** Visual monitoring of AWS services

### Pre-configured Resources

**Secrets Manager Entry:**

```
Name: external-api/token
Value: {"client_id": "test-client-id", "client_secret": "test-client-secret"}
```

**IAM Role:**

```
Name: lambda-execution-role
Permissions: SecretsManager access, CloudWatch Logs
```

---

## 📖 Detailed Commands

### Helper Script Commands (Windows)

```batch
localstack-helper.bat start      # Start LocalStack only
localstack-helper.bat stop       # Stop LocalStack
localstack-helper.bat status     # Check if running
localstack-helper.bat build      # Build JAR with Maven
localstack-helper.bat setup      # Create/update Lambda function
localstack-helper.bat invoke     # Invoke Lambda (interactive)
localstack-helper.bat logs       # Follow CloudWatch logs
localstack-helper.bat secrets    # View all secrets
localstack-helper.bat full       # Complete setup
localstack-helper.bat clean      # Remove all containers
```

### Helper Script Commands (Mac/Linux)

```bash
./localstack-helper.sh start
./localstack-helper.sh stop
./localstack-helper.sh status
./localstack-helper.sh build
./localstack-helper.sh setup
./localstack-helper.sh invoke '{}'
./localstack-helper.sh logs
./localstack-helper.sh secrets
./localstack-helper.sh full
./localstack-helper.sh clean
```

### Manual AWS CLI Commands

**List secrets:**

```bash
aws secretsmanager list-secrets --endpoint-url http://localhost:4566
```

**Get secret:**

```bash
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566
```

**Update secret:**

```bash
aws secretsmanager update-secret \
  --secret-id external-api/token \
  --secret-string '{"client_id":"new-id","client_secret":"new-secret"}' \
  --endpoint-url http://localhost:4566
```

**List Lambda functions:**

```bash
aws lambda list-functions --endpoint-url http://localhost:4566
```

**Invoke Lambda:**

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json
```

---

## 🧪 Testing Scenarios

### Test 1: Token Authorization

```powershell
# Windows
.\localstack-helper.bat invoke '{}'
```

**Expected Response:**

```json
{
  "statusCode": 200,
  "data": {
    "token": "eyJ...",
    "cached": true
  }
}
```

### Test 2: Multiple Invocations (Cache Test)

```powershell
# First invocation - fetches token
.\localstack-helper.bat invoke '{}'

# Second invocation - uses cached token
.\localstack-helper.bat invoke '{}'

# Check logs to see cache hit
.\localstack-helper.bat logs
```

Look for log messages:

- First: "Requesting new access token"
- Second: "Using cached access token"

### Test 3: API Call

```powershell
.\localstack-helper.bat invoke '{"action": "api_call", "endpoint": "/v1/data"}'
```

---

## 🔍 Viewing Logs

### Real-time Logs

```powershell
.\localstack-helper.bat logs
```

### Docker Logs

```powershell
docker logs -f localstack-lambda-test
```

### Sample Log Output

```json
{
  "timestamp": "2025-12-27T10:30:45.123Z",
  "level": "INFO",
  "message": "Access token successfully obtained",
  "logger": "TokenAuthorizationService",
  "correlation_id": "aws-lambda-correlation-id"
}
```

---

## 🔐 Secrets Management

### Update Secret Value

```bash
# Update with new credentials
aws secretsmanager update-secret \
  --secret-id external-api/token \
  --secret-string '{"client_id":"prod-id","client_secret":"prod-secret"}' \
  --endpoint-url http://localhost:4566
```

### View All Secrets

```bash
aws secretsmanager list-secrets \
  --endpoint-url http://localhost:4566 | jq .
```

### Test Secret Retrieval

```bash
# This is what your Lambda does internally
aws secretsmanager get-secret-value \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566
```

---

## 🐛 Troubleshooting

### LocalStack Won't Start

```powershell
# Check Docker is running
docker ps

# Check Docker Compose
docker-compose ps

# View logs
docker logs localstack-lambda-test

# Restart
docker-compose restart
```

### Port Already in Use

```powershell
# Find what's using port 4566
netstat -ano | findstr :4566

# Kill process (Windows)
taskkill /PID <PID> /F

# Or change port in docker-compose.yml
```

### Secret Not Found

```bash
# Verify secret exists
aws secretsmanager list-secrets --endpoint-url http://localhost:4566

# Get secret details
aws secretsmanager describe-secret \
  --secret-id external-api/token \
  --endpoint-url http://localhost:4566
```

### Lambda Function Not Found

```bash
# List all functions
aws lambda list-functions --endpoint-url http://localhost:4566

# Get function details
aws lambda get-function \
  --function-name my-token-auth-lambda \
  --endpoint-url http://localhost:4566
```

### Memory/CPU Issues

Increase Docker resources:

1. Open Docker Desktop Settings
2. Go to Resources
3. Increase Memory to 4GB+
4. Increase CPUs to 2+

---

## 📊 Environment Variables

The Lambda function is configured with:

```
EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com
TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token
CLIENT_ID=test-client-id
CLIENT_SECRET=test-client-secret
TOKEN_SECRET_NAME=external-api/token
```

To modify:

1. Edit `localstack-helper.bat` (Windows) or `localstack-helper.sh` (Mac/Linux)
2. Update the environment variables section
3. Run `localstack-helper.bat setup` to update Lambda

---

## 🌐 LocalStack UI

Open browser and visit:

```
http://localhost:8080
```

Features:

- View Lambda functions and code
- Inspect CloudWatch logs
- Monitor Secrets Manager
- Check API Gateway
- View metrics and events

---

## 💾 Data Persistence

LocalStack data is stored in `/tmp/localstack`. To preserve data between restarts:

```bash
# The volume is configured in docker-compose.yml
docker volume ls
```

To clear all data:

```powershell
.\localstack-helper.bat clean
```

---

## 🔗 Integration with Real AWS

### Test Locally First

```powershell
# 1. Start LocalStack
.\localstack-helper.bat full

# 2. Run all tests
.\localstack-helper.bat invoke '{}'

# 3. Check logs
.\localstack-helper.bat logs

# 4. Stop when done
.\localstack-helper.bat stop
```

### Deploy to Real AWS

```powershell
# 1. Build JAR
mvn clean install

# 2. Deploy to AWS Lambda
aws lambda create-function ...
```

---

## 📚 Complete Workflow

```powershell
# 1. Start full setup
.\localstack-helper.bat full

# 2. Verify resources created
.\localstack-helper.bat secrets

# 3. Test Lambda invocation
.\localstack-helper.bat invoke '{}'

# 4. Check logs
.\localstack-helper.bat logs

# 5. Update code
# ... (Edit your code)

# 6. Rebuild and update
mvn clean install
.\localstack-helper.bat setup

# 7. Test again
.\localstack-helper.bat invoke '{}'

# 8. Cleanup
.\localstack-helper.bat stop
```

---

## ✅ Checklist

- [ ] Docker Desktop installed and running
- [ ] AWS CLI v2 installed
- [ ] Java 21 JDK installed
- [ ] Maven installed
- [ ] In project root directory
- [ ] Run full setup: `.\localstack-helper.bat full`
- [ ] Lambda invocation successful
- [ ] Logs visible in CloudWatch
- [ ] Secrets Manager accessible

---

## 📞 Support

If you encounter issues:

1. **Check LocalStack logs:** `docker logs localstack-lambda-test`
2. **Check Docker:** `docker ps`, `docker-compose ps`
3. **Check AWS CLI:** `aws --version`
4. **Restart everything:** `.\localstack-helper.bat clean && .\localstack-helper.bat full`

---

## 🎓 Learning Resources

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

---

**Status:** ✅ Ready for Testing
**Last Updated:** December 27, 2025

