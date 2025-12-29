# 🧪 LOCAL TESTING GUIDE - JSON LOGGING VALIDATION

## Overview

You can now test your Lambda locally without deploying to AWS. This includes:

- ✅ JSON logging format validation
- ✅ Request ID propagation in logs
- ✅ Multiple log level testing
- ✅ Exception logging
- ✅ Concurrent request handling
- ✅ Integration testing with mock Lambda context

---

## Unit Tests Created

### 1. **ApiHandlerLoggingTest.java**

Tests individual logging functionality:

- JSON format validation
- Required fields (timestamp, level, message)
- Request ID in MDC (Mapped Diagnostic Context)
- Multiple log levels (DEBUG, INFO, WARN, ERROR)
- Exception logging

**Location:** `service/src/test/java/com/project/service/ApiHandlerLoggingTest.java`

### 2. **ApiHandlerIntegrationTest.java**

Integration tests simulating Lambda environment:

- Handler processing with JSON logs
- Request ID propagation
- Concurrent request handling
- Log format consistency
- Error handling and logging

**Location:** `service/src/test/java/com/project/service/ApiHandlerIntegrationTest.java`

---

## Running Tests Locally

### Run All Tests

```bash
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject

# Run all tests with Maven
mvn clean test
```

### Run Specific Test Class

```bash
# Test logging only
mvn test -Dtest=ApiHandlerLoggingTest

# Test integration
mvn test -Dtest=ApiHandlerIntegrationTest

# Run both
mvn test -Dtest=ApiHandler*Test
```

### Run with Verbose Output

```bash
mvn test -X
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
# Report will be at: target/site/jacoco/index.html
```

---

## What the Tests Validate

### ✅ JSON Format

Verifies logs contain proper JSON structure:

```json
{
  "timestamp": "2025-12-27T16:26:10.477Z",
  "level": "INFO",
  "logger": "com.project.service.ApiHandler",
  "message": "Test message",
  "thread": "main",
  "requestId": "test-req-123"
}
```

### ✅ Required Fields

Checks for:

- `timestamp` - ISO 8601 format
- `level` - DEBUG, INFO, WARN, ERROR
- `message` - Log message
- `logger` - Class name
- `requestId` - From MDC

### ✅ Request Tracking

Validates request IDs are properly:

- Set in MDC
- Captured in logs
- Cleared after processing

### ✅ Log Levels

Tests all severity levels work:

- `LOG.debug()` - DEBUG level
- `LOG.info()` - INFO level
- `LOG.warn()` - WARN level
- `LOG.error()` - ERROR level with exceptions

---

## Expected Test Output

### Success

```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Sample Log Output During Tests

```
2025-12-27 16:30:45,123 [main] INFO com.project.service.ApiHandlerLoggingTest - Test message: Testing JSON format
2025-12-27 16:30:45,124 [main] INFO com.project.service.ApiHandlerIntegrationTest - Starting handler test
2025-12-27 16:30:45,125 [main] INFO com.project.service.ApiHandlerIntegrationTest - Handler executed successfully with status: 200
```

---

## Debugging Failed Tests

### If tests fail, check:

1. **Log4j2 Configuration**
    - Verify `service/src/main/resources/log4j2.xml` exists
    - Check `LambdaJsonLayout.json` exists
    - Ensure JSON template is valid

2. **SLF4J Binding**
    - Verify `log4j-slf4j2-impl` is in dependencies
    - Check no conflicting SLF4J implementations

3. **Test Dependencies**
    - JUnit 4.x should be included
    - Check pom.xml has `test` scope dependencies

4. **Run with Debug**
   ```bash
   mvn test -X 2>&1 | grep -i "log4j\|slf4j\|json"
   ```

---

## Before Deploying to Lambda

### Checklist

- [ ] Run `mvn clean test` successfully
- [ ] All 12 tests pass
- [ ] No compilation errors
- [ ] Log output shows JSON format in test output
- [ ] Request IDs appear in logs

### Then Deploy

```bash
# Build for Lambda
mvn clean install -DskipTests

# Redeploy to LocalStack
cd infra/terraform
terraform apply -var-file="terraform.localstack.tfvars" -auto-approve
```

---

## Local vs AWS Logging

### Local Testing

- Tests use console output
- Can capture and inspect logs
- Tests isolated, no external API calls
- Fast feedback loop

### AWS Lambda

- Logs go to CloudWatch
- Full production-grade JSON logging
- Actual request IDs from API Gateway
- Real error handling from external APIs

---

## Next Steps

1. **Run Tests Locally**
   ```bash
   mvn clean test
   ```

2. **Verify JSON Output**
    - Check test console output shows JSON
    - Verify all required fields present

3. **Deploy When Confident**
   ```bash
   mvn clean install
   terraform apply
   ```

4. **Validate in CloudWatch**
   ```bash
   aws logs filter-log-events \
     --log-group-name /aws/lambda/my-token-auth-lambda \
     --endpoint-url http://localhost:4566
   ```

---

## Test Architecture

```
ApiHandlerLoggingTest
├── testJsonLoggingFormat()           - Validates JSON structure
├── testJsonContainsRequiredFields()  - Checks fields present
├── testRequestIdInLogs()             - Validates MDC integration
├── testMultipleLogLevels()           - Tests all log levels
└── testExceptionLogging()            - Tests error logging

ApiHandlerIntegrationTest
├── testHandlerRequestWithJsonLogs()  - Full handler simulation
├── testRequestIdPropagation()        - MDC propagation
├── testConcurrentRequests()          - Thread safety
├── testLogFormatConsistency()        - Format validation
└── testErrorLogging()                - Error handling
```

---

## Tips

- Tests run against log4j2.xml in `src/main/resources`
- Logs are captured to console during test execution
- No external dependencies or AWS calls needed
- Fast execution - all tests complete in < 5 seconds
- Safe to run before every deployment

---

**Your Lambda is now locally testable before deployment!** ✅

