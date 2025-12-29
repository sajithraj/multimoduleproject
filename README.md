# AWS Lambda Multi-Module Project

A production-ready, multi-module Maven project for AWS Lambda functions with OAuth2 token management, external API integration, and multi-source task processing.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)](https://maven.apache.org/)
[![AWS Lambda](https://img.shields.io/badge/AWS-Lambda-orange.svg)](https://aws.amazon.com/lambda/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Building](#building)
- [Deployment](#deployment)
- [Testing](#testing)
- [Documentation](#documentation)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Contributing](#contributing)

---

## 🎯 Overview

This project implements a serverless architecture on AWS Lambda with three specialized modules:

1. **Token Module** - OAuth2 token management with AWS Powertools v2 caching
2. **Service Module** - External API integration with token-based authentication
3. **TaskService Module** - Multi-source event processing (API Gateway, SQS, EventBridge)

### Key Features

✅ **AWS Powertools v2** - Advanced Lambda utilities with caching  
✅ **OAuth2 Token Caching** - 55-minute TTL with automatic refresh  
✅ **Multi-Module Architecture** - Clean separation of concerns  
✅ **Lombok Integration** - Reduced boilerplate code  
✅ **JSON Structured Logging** - Log4j2 with CloudWatch integration  
✅ **SSL/TLS Support** - Custom certificate handling  
✅ **Comprehensive Testing** - Unit and integration tests  
✅ **Infrastructure as Code** - Terraform for deployment  
✅ **LocalStack Compatible** - Local development and testing  

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS Cloud                                │
│                                                                   │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐  │
│  │ API Gateway  │      │     SQS      │      │ EventBridge  │  │
│  └──────┬───────┘      └──────┬───────┘      └──────┬───────┘  │
│         │                     │                     │            │
│         └──────────┬──────────┴──────────┬──────────┘            │
│                    │                     │                        │
│         ┌──────────▼──────────┐ ┌───────▼───────────┐           │
│         │  Service Lambda     │ │ TaskService       │           │
│         │  (External API)     │ │ Lambda            │           │
│         └──────────┬──────────┘ └───────────────────┘           │
│                    │                                              │
│         ┌──────────▼──────────┐                                  │
│         │  Token Module       │                                  │
│         │  (OAuth2 Caching)   │                                  │
│         └──────────┬──────────┘                                  │
│                    │                                              │
│         ┌──────────▼──────────┐                                  │
│         │  Secrets Manager    │                                  │
│         │  (Credentials)      │                                  │
│         └─────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Modules

### 1. Token Module
**Purpose:** OAuth2 token lifecycle management

**Features:**
- AWS Powertools v2 parameter store integration
- Token caching with 55-minute TTL
- Automatic token refresh
- Secrets Manager integration
- SSL certificate handling

**Technology:** Java 21, AWS Powertools 2.8.0, Lombok

[📖 Token Module Documentation](token/README.md)

---

### 2. Service Module
**Purpose:** External API integration with authenticated requests

**Features:**
- Token-based authentication
- HTTP client with connection pooling
- SSL/TLS support
- Error handling and retry logic
- JSON structured logging

**Technology:** Java 21, Apache HttpClient 5, Log4j2

[📖 Service Module Documentation](service/README.md)

---

### 3. TaskService Module
**Purpose:** Multi-source event processing

**Features:**
- API Gateway request handling
- SQS message processing
- EventBridge event handling
- Router pattern architecture
- Lombok models with builder pattern

**Technology:** Java 21, AWS Lambda Events, Lombok

[📖 TaskService Module Documentation](taskService/README.md)

---

## 🔧 Prerequisites

### Required
- **Java 21** or higher
- **Maven 3.9+**
- **AWS CLI** (for deployment)
- **Git**

### Optional (for local development)
- **Docker** (for LocalStack)
- **Terraform 1.0+** (for infrastructure)
- **LocalStack** (for local AWS simulation)

### AWS Permissions Required
- Lambda function management
- Secrets Manager access
- CloudWatch Logs
- IAM role management

---

## 🚀 Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/sajithraj/multimoduleproject.git
cd multimoduleproject
```

### 2. Build All Modules
```bash
mvn clean package
```

### 3. Deploy to LocalStack (Optional)
```bash
# Start LocalStack
docker run -d -p 4566:4566 localstack/localstack

# Deploy infrastructure
cd infra/terraform
terraform init
terraform apply -var="use_localstack=true"
```

### 4. Deploy to AWS
```bash
cd infra/terraform
terraform init
terraform apply
```

---

## 🔨 Building

### Build All Modules
```bash
mvn clean package
```

### Build Specific Module
```bash
mvn clean package -pl token
mvn clean package -pl service
mvn clean package -pl taskService
```

### Skip Tests
```bash
mvn clean package -DskipTests
```

### Build with Tests
```bash
mvn clean install
```

### Build Artifacts
- `token/target/token-1.0-SNAPSHOT.jar`
- `service/target/service-1.0-SNAPSHOT.jar`
- `taskService/target/taskService-1.0-SNAPSHOT.jar`

---

## 🚢 Deployment

### AWS Lambda Deployment

#### Using Terraform (Recommended)
```bash
cd infra/terraform

# Initialize
terraform init

# Plan
terraform plan

# Apply
terraform apply
```

#### Using AWS CLI
```bash
# Deploy service Lambda
aws lambda update-function-code \
  --function-name my-token-auth-lambda \
  --zip-file fileb://service/target/service-1.0-SNAPSHOT.jar

# Deploy taskService Lambda
aws lambda update-function-code \
  --function-name task-service \
  --zip-file fileb://taskService/target/taskService-1.0-SNAPSHOT.jar
```

### Environment Variables

#### Service Lambda
```bash
TOKEN_ENDPOINT_URL=https://api.example.com/oauth/token
TOKEN_SECRET_NAME=external-api/token
EXTERNAL_API_URL=https://api.example.com/v1/resource
OAUTH2_TIMEOUT_SECONDS=3
POWERTOOLS_SERVICE_NAME=api-service
POWERTOOLS_LOG_LEVEL=INFO
```

#### TaskService Lambda
```bash
POWERTOOLS_SERVICE_NAME=task-service
POWERTOOLS_LOG_LEVEL=INFO
POWERTOOLS_LOGGER_LOG_EVENT=true
```

[📖 Full Deployment Guide](infra/README.md)

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Module Tests
```bash
mvn test -pl token
mvn test -pl service
mvn test -pl taskService
```

### Integration Tests
```bash
# Start LocalStack
docker run -d -p 4566:4566 localstack/localstack

# Run integration tests
mvn verify
```

### Manual Testing

#### Test Service Lambda
```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  response.json

cat response.json
```

#### Test TaskService Lambda
```bash
# API Gateway event
aws lambda invoke \
  --function-name task-service \
  --payload '{
    "httpMethod": "POST",
    "path": "/tasks",
    "body": "{\"taskName\":\"Test\"}"
  }' \
  response.json
```

---

## 📚 Documentation

### Module Documentation
- [Token Module](token/README.md) - OAuth2 token management
- [Service Module](service/README.md) - External API integration
- [TaskService Module](taskService/README.md) - Multi-source processing

### Infrastructure
- [Terraform Infrastructure](infra/README.md) - IaC deployment guide

### Guides
- [Quick Start Guide](#quick-start) - Get started quickly
- [Deployment Guide](infra/README.md#deployment) - AWS deployment
- [Testing Guide](#testing) - Run tests locally

---

## 📂 Project Structure

```
multimoduleproject/
├── pom.xml                          # Parent POM
├── README.md                        # This file
├── .gitignore                       # Git exclusions
│
├── token/                           # OAuth2 Token Module
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/
│       │   └── com/project/token/
│       │       ├── provider/
│       │       └── transformer/
│       └── test/java/
│
├── service/                         # External API Service Module
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/
│       │   └── com/project/service/
│       │       ├── client/
│       │       ├── config/
│       │       ├── exception/
│       │       └── util/
│       └── test/java/
│
├── taskService/                     # Multi-Source Task Service
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/
│       │   └── com/project/task/
│       │       ├── handler/
│       │       ├── router/
│       │       ├── service/
│       │       ├── model/
│       │       └── util/
│       └── test/java/
│
└── infra/                           # Infrastructure as Code
    ├── README.md
    └── terraform/
        ├── main.tf
        ├── variables.tf
        ├── outputs.tf
        └── *.ps1 (deployment scripts)
```

---

## 🛠️ Tech Stack

### Core Technologies
- **Java 21** - Programming language
- **Maven** - Build and dependency management
- **AWS Lambda** - Serverless compute
- **Lombok** - Boilerplate reduction

### AWS Services
- **AWS Lambda** - Function execution
- **AWS Secrets Manager** - Credential storage
- **AWS Powertools** - Lambda utilities
- **CloudWatch Logs** - Logging and monitoring
- **API Gateway** - HTTP endpoints
- **SQS** - Message queuing
- **EventBridge** - Event routing

### Libraries & Frameworks
- **AWS Powertools Java 2.8.0** - Lambda utilities
- **Apache HttpClient 5** - HTTP communication
- **Jackson 2.17.1** - JSON processing
- **Log4j2 2.25.3** - Structured logging
- **JUnit 4** - Unit testing
- **Mockito 5** - Mocking framework

### Development Tools
- **Terraform** - Infrastructure as code
- **LocalStack** - Local AWS simulation
- **Docker** - Containerization
- **Git** - Version control

---

## 👥 Contributing

### Development Workflow
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Use Lombok for boilerplate reduction
- Write unit tests for new features
- Document public APIs with Javadoc
- Use meaningful commit messages

### Testing Requirements
- Unit tests for business logic
- Integration tests for AWS interactions
- Minimum 80% code coverage

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Support

For issues, questions, or contributions:
- **Issues:** [GitHub Issues](https://github.com/sajithraj/multimoduleproject/issues)
- **Discussions:** [GitHub Discussions](https://github.com/sajithraj/multimoduleproject/discussions)

---

## 📝 Changelog

### Version 1.0.0 (2025-12-29)
- ✅ Initial release with 3 modules
- ✅ OAuth2 token caching with Powertools v2
- ✅ Multi-source task processing
- ✅ Terraform infrastructure
- ✅ Comprehensive documentation

---

## 🙏 Acknowledgments

- AWS Powertools Java team
- AWS Lambda documentation
- Open source community

---

**Built with ❤️ using Java 21, AWS Lambda, and AWS Powertools**

*Last Updated: December 29, 2025*

