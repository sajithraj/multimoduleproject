# Project Files Reference

Complete reference of all files in the Java Lambda project.

## 📁 Root Directory Files

### Configuration & Build

- **pom.xml** - Maven project configuration
    - Dependencies: Lambda, Powertools v2, HttpClient5, Resilience4j, Log4j2
    - Plugins: Maven Shade for JAR packaging
    - Target: Java 21

### Documentation

- **README.md** - Main project documentation
    - Features overview
    - Setup instructions
    - Configuration guide
    - Token caching details
    - Retry configuration
    - Performance characteristics
    - Error handling
    - Troubleshooting

- **IMPLEMENTATION_SUMMARY.md** - Implementation overview
    - What has been implemented
    - Technology stack
    - Security features
    - Performance characteristics
    - Deployment instructions
    - Customization guide

- **ARCHITECTURE.md** - System architecture documentation
    - System architecture diagram
    - Component details
    - Performance breakdown
    - Thread safety analysis
    - Cold start optimization
    - Resilience patterns
    - Monitoring and observability

- **DEPLOYMENT_GUIDE.md** - Step-by-step deployment guide
    - Prerequisites
    - Quick start (automated and manual)
    - Configuration after deployment
    - API Gateway integration
    - Monitoring setup
    - Troubleshooting
    - Cleanup procedures

- **DEPLOYMENT_CHECKLIST.md** - Pre-deployment verification
    - Code quality checklist
    - Configuration verification
    - AWS prerequisites
    - Build artifacts verification
    - Secrets Manager setup
    - IAM role configuration
    - External API verification
    - Testing checklist
    - Final verification

- **QUICK_START.md** - 5-minute quick start guide
    - Prerequisites
    - Step-by-step deployment (5 minutes)
    - Verification
    - API Gateway setup
    - Monitoring
    - Common issues and solutions

### Deployment Scripts

- **deploy.sh** - Bash deployment script (Linux/macOS)
    - Builds Maven project
    - Creates/updates Secrets Manager secret
    - Creates/validates IAM role
    - Deploys Lambda function
    - Tests function
    - Displays summary

- **deploy.ps1** - PowerShell deployment script (Windows)
    - Same functionality as deploy.sh
    - Windows PowerShell syntax
    - Color-coded output
    - Parameterized configuration

### IAM Configuration

- **trust-policy.json** - Lambda role trust policy
    - Allows Lambda service to assume role
    - Standard AWS Lambda trust relationship

- **secrets-policy.json** - Secrets Manager access policy
    - Allows GetSecretValue action
    - Full Secrets Manager access (can be restricted by secret ARN)

## 📂 Source Code Structure

### Java Source Files

#### `src/main/java/org/example/`

**ApiHandler.java** - Lambda handler (entry point)

- Implements `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
- Main Lambda entry point for API Gateway
- Delegates to ExternalApiClient
- Error handling (500, 502 responses)
- JSON response formatting
- Powertools logging integration

**Main.java** - Configuration documentation

- NOT executed at runtime
- Documents Lambda configuration
- Handler name and runtime
- Environment variables required
- Secret format
- IAM permissions needed
- Cold start optimization notes
- Error handling strategy

#### `src/main/java/org/example/auth/`

**TokenCache.java** - Token caching with Secrets Manager

- 55-minute in-memory cache
- Powertools v2 Parameters provider
- Thread-safe (double-checked locking)
- Lazy initialization of secrets provider
- Automatic expiry detection
- Documentation and error handling

**SecretManagerClient.java** - Direct Secrets Manager client

- DEPRECATED - use TokenCache instead
- Kept for backward compatibility
- Direct AWS SDK integration
- Similar caching pattern

#### `src/main/java/org/example/client/`

**ExternalApiClient.java** - HTTP client with retry

- Makes HTTP GET requests
- Adds Bearer token authentication
- Powertools logging
- Resilience4j retry decorator
- Comprehensive error handling
- JSON response parsing
- Request/response logging

#### `src/main/java/org/example/config/`

**AppConfig.java** - Configuration management

- Environment variable loading
- Required variable validation
- Static initialization
- Getters for optional variables
- Error messages for missing config

**RetryConfigProvider.java** - Retry configuration

- Resilience4j Retry instance
- Max 3 attempts
- Exponential backoff (300ms initial)
- Jitter (50% randomization)
- Specific exception handling
- Network error focused

#### `src/main/java/org/example/exception/`

**ExternalApiException.java** - Custom exception

- Extends RuntimeException
- Constructors for different scenarios
- Used for API-related errors
- Caught by handler and converted to HTTP response

#### `src/main/java/org/example/model/`

**ApiRequest.java** - Request model

- Optional request structure
- Fields: endpoint, method, payload
- Jackson serialization support
- Getters and setters

**ApiResponse.java** - Response model

- Standard response wrapper
- Fields: statusCode, data, error, timestamp
- Jackson serialization support
- Constructors for common scenarios

#### `src/main/java/org/example/util/`

**HttpClientFactory.java** - HTTP client factory

- Lazy initialization
- Connection pooling
    - 10 total connections
    - 5 per route
- Timeout configuration
    - 5 seconds connect
    - 10 seconds socket
- Singleton pattern
- Error handling

### Resource Files

#### `src/main/resources/`

**log4j2.xml** - Logging configuration

- JSON layout via JsonTemplateLayout
- Appenders for console output
- Per-logger configuration
- Log levels by component
- Root logger configuration
- AWS SDK logging suppression

**LambdaJsonLayout.json** - JSON layout template

- Field definitions: timestamp, level, logger, thread, message
- Exception stack trace included
- MDC context support
- CloudWatch Logs compatible format

## 🔗 Dependencies in pom.xml

### AWS Services

- aws-lambda-java-core (1.2.3)
- aws-lambda-java-events (3.11.4)
- powertools-logging (2.5.0)
- powertools-parameters (2.5.0)

### HTTP & Networking

- httpclient5 (5.3)

### Resilience & Retry

- resilience4j-retry (2.2.0)

### Logging & JSON

- jackson-databind (2.17.1)
- log4j-core (2.23.1)
- log4j-layout-template-json (2.23.1)
- log4j-slf4j2-impl (2.23.1)

### Testing

- junit (4.13.2) [scope: test]

### Build Plugins

- maven-shade-plugin (3.5.1)

## 📋 File Responsibilities

| File                      | Responsibility        | Type          |
|---------------------------|-----------------------|---------------|
| ApiHandler.java           | Request handling      | Core          |
| ExternalApiClient.java    | HTTP communication    | Core          |
| TokenCache.java           | Token management      | Core          |
| HttpClientFactory.java    | HTTP client lifecycle | Core          |
| AppConfig.java            | Configuration         | Configuration |
| RetryConfigProvider.java  | Retry policy          | Configuration |
| ExternalApiException.java | Error handling        | Utility       |
| ApiRequest.java           | Data model            | Model         |
| ApiResponse.java          | Data model            | Model         |
| log4j2.xml                | Logging setup         | Configuration |
| LambdaJsonLayout.json     | Log format            | Configuration |
| deploy.sh                 | Linux deployment      | Deployment    |
| deploy.ps1                | Windows deployment    | Deployment    |
| trust-policy.json         | IAM role trust        | Configuration |
| secrets-policy.json       | IAM permissions       | Configuration |

## 🗂️ File Dependencies

```
ApiHandler.java
├── ExternalApiClient.java
├── ExternalApiException.java
├── ApiResponse.java
└── AppConfig.java

ExternalApiClient.java
├── TokenCache.java
├── RetryConfigProvider.java
├── HttpClientFactory.java
├── AppConfig.java
└── ExternalApiException.java

TokenCache.java
├── AppConfig.java
├── ExternalApiException.java
└── Parameters (Powertools)

HttpClientFactory.java
└── No dependencies (self-contained)

RetryConfigProvider.java
└── Resilience4j

AppConfig.java
└── No dependencies (self-contained)

log4j2.xml
└── LambdaJsonLayout.json
```

## 📊 File Statistics

| Category      | Count   | Files                                                                                                                                  |
|---------------|---------|----------------------------------------------------------------------------------------------------------------------------------------|
| Documentation | 7       | README, IMPLEMENTATION_SUMMARY, ARCHITECTURE, DEPLOYMENT_GUIDE, DEPLOYMENT_CHECKLIST, QUICK_START, this file                           |
| Source Code   | 9       | ApiHandler, Main, TokenCache, SecretManagerClient, ExternalApiClient, AppConfig, RetryConfigProvider, ExternalApiException, Models (2) |
| Utilities     | 1       | HttpClientFactory                                                                                                                      |
| Configuration | 4       | pom.xml, log4j2.xml, LambdaJsonLayout.json, AppConfig.java                                                                             |
| Deployment    | 4       | deploy.sh, deploy.ps1, trust-policy.json, secrets-policy.json                                                                          |
| **Total**     | **25+** |                                                                                                                                        |

## 🔄 Information Flow

```
Documentation (7 files)
    ↓
User reads README → Understands features
    ↓
User reads QUICK_START → Gets started in 5 minutes
    ↓
Deploy script (deploy.sh or deploy.ps1)
    ↓
Builds JAR from source code (9 classes)
    ↓
Applies configuration (4 files)
    ↓
Creates IAM roles (2 policy files)
    ↓
Deploys to AWS Lambda
    ↓
Logs output in JSON format (log4j2.xml + LambdaJsonLayout.json)
```

## 📝 Which File to Edit For...

| Need                         | Edit File                                  |
|------------------------------|--------------------------------------------|
| Change token cache duration  | TokenCache.java (TOKEN_EXPIRY_SECONDS)     |
| Change retry attempts        | RetryConfigProvider.java (maxAttempts)     |
| Change log level             | log4j2.xml (Logger level)                  |
| Change HTTP timeout          | HttpClientFactory.java (Timeout.ofSeconds) |
| Change connection pool size  | HttpClientFactory.java (MAX_CONNECTIONS)   |
| Add new error handling       | ExternalApiClient.java or ApiHandler.java  |
| Change response format       | ApiResponse.java or ApiHandler.java        |
| Add new environment variable | AppConfig.java                             |
| Change Lambda handler logic  | ApiHandler.java                            |

## 🚀 Build & Deploy Artifacts

After running `mvn clean package`:

- **SetUpProject-1.0-SNAPSHOT.jar** (25-30MB)
    - Created in: target/
    - Contains: All classes + dependencies
    - Handler: org.example.ApiHandler
    - Used by: deploy.sh / deploy.ps1

## 📚 Documentation Roadmap

```
Start Here
    ↓
README.md (overview)
    ↓
QUICK_START.md (deploy in 5 min)
    ├─ Success? → Go to ARCHITECTURE.md
    └─ Issues? → Go to DEPLOYMENT_CHECKLIST.md
    
DEPLOYMENT_GUIDE.md (detailed steps)
    ↓
ARCHITECTURE.md (understand design)
    ↓
Configuration files (customize)
    ↓
Source code (extend)
```

## ✅ Complete Package

✅ **Source Code**: 9 production-ready Java classes  
✅ **Build Config**: Maven POM with all dependencies  
✅ **Documentation**: 7 comprehensive guides  
✅ **Deployment Scripts**: Bash and PowerShell  
✅ **IAM Configuration**: Trust and permission policies  
✅ **Logging Setup**: JSON format with Log4j2  
✅ **Ready to Deploy**: Everything included

---

**Total Implementation**: 25+ files, complete production-grade Lambda application
**Status**: Ready for deployment
**Version**: 1.0-SNAPSHOT

