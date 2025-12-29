# Contributing to Multi-Module Lambda Project

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing to this project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)

---

## 🤝 Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inspiring community for everyone.

### Expected Behavior

- ✅ Be respectful and inclusive
- ✅ Welcome newcomers and help them learn
- ✅ Focus on what is best for the community
- ✅ Show empathy towards other community members

### Unacceptable Behavior

- ❌ Harassment or discriminatory language
- ❌ Trolling or insulting comments
- ❌ Publishing others' private information
- ❌ Any conduct that could be considered inappropriate

---

## 🚀 Getting Started

### Prerequisites

1. **Java 21** or higher
2. **Maven 3.9+**
3. **Git**
4. **AWS CLI** (optional)
5. **Docker** (optional, for LocalStack)

### Setup Development Environment

```bash
# Clone repository
git clone https://github.com/sajithraj/multimoduleproject.git
cd multimoduleproject

# Build project
mvn clean install

# Run tests
mvn test
```

---

## 💻 Development Workflow

### 1. Fork the Repository

```bash
# Fork on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/multimoduleproject.git
cd multimoduleproject

# Add upstream remote
git remote add upstream https://github.com/sajithraj/multimoduleproject.git
```

### 2. Create a Feature Branch

```bash
# Create branch from main
git checkout -b feature/my-amazing-feature

# Or for bug fixes
git checkout -b fix/bug-description
```

### 3. Make Changes

- Write clean, maintainable code
- Follow coding standards
- Add unit tests
- Update documentation

### 4. Commit Changes

```bash
# Stage changes
git add .

# Commit with meaningful message
git commit -m "feat: add amazing feature"
```

#### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat: add SQS batch processing support
fix: resolve token caching issue
docs: update README with deployment steps
test: add integration tests for API Gateway
```

### 5. Push Changes

```bash
# Push to your fork
git push origin feature/my-amazing-feature
```

### 6. Create Pull Request

- Go to GitHub and create a Pull Request
- Fill out the PR template
- Link related issues
- Request review

---

## 📝 Coding Standards

### Java Code Style

#### Naming Conventions

```java
// Classes: PascalCase
public class ApiHandler { }

// Methods: camelCase
public void processRequest() { }

// Constants: UPPER_SNAKE_CASE
private static final String TOKEN_ENDPOINT = "...";

// Variables: camelCase
String accessToken = "...";
```

#### Use Lombok

```java
// ✅ Good - Use Lombok
@Data
@Builder
public class TaskRequest {
    private String eventId;
    private String body;
}

// ❌ Avoid - Manual getters/setters
public class TaskRequest {
    private String eventId;
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}
```

#### Proper Logging

```java
// ✅ Good - Structured logging
log.info("Processing request: eventId={}, type={}", eventId, type);
log.error("Failed to process request: {}", e.getMessage(), e);

// ❌ Avoid - String concatenation
log.info("Processing request: " + eventId + ", type: " + type);
System.out.println("Debug: " + message);
```

#### Error Handling

```java
// ✅ Good - Specific exceptions
try {
    processTask(request);
} catch (ValidationException e) {
    log.error("Validation failed: {}", e.getMessage());
    return buildErrorResponse(400, e.getMessage());
} catch (ExternalApiException e) {
    log.error("API call failed: {}", e.getMessage());
    return buildErrorResponse(502, "External service unavailable");
}

// ❌ Avoid - Catching generic Exception
try {
    processTask(request);
} catch (Exception e) {
    return buildErrorResponse(500, "Error");
}
```

### Code Organization

```
module/
└── src/
    └── main/java/com/project/module/
        ├── handler/     # Lambda handlers
        ├── service/     # Business logic
        ├── model/       # Data models (Lombok)
        ├── util/        # Utilities
        ├── config/      # Configuration
        └── exception/   # Custom exceptions
```

### Documentation

#### Javadoc

```java
/**
 * Processes task requests from multiple event sources.
 * 
 * @param request The task request containing event data
 * @param context Lambda execution context
 * @return TaskResponse containing processing result
 * @throws ValidationException if request is invalid
 */
public TaskResponse processTask(TaskRequest request, Context context) {
    // Implementation
}
```

#### README Updates

- Update module README for new features
- Add usage examples
- Document configuration changes
- Update changelog

---

## 🧪 Testing Requirements

### Unit Tests

```java
@Test
public void testProcessTask_Success() {
    // Arrange
    TaskRequest request = TaskRequest.builder()
        .eventId("test-123")
        .build();
    
    // Act
    TaskResponse response = service.processTask(request, mockContext);
    
    // Assert
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("test-123", response.getTaskId());
}
```

### Test Coverage

- **Minimum:** 80% code coverage
- **Focus areas:**
  - Business logic
  - Error handling
  - Edge cases

### Run Tests

```bash
# All tests
mvn test

# Specific module
mvn test -pl token

# With coverage
mvn test jacoco:report
```

### Integration Tests

```java
@Test
public void testEndToEndFlow() {
    // Test complete flow from request to response
}
```

---

## 🔄 Pull Request Process

### Before Submitting

- [ ] Code follows style guidelines
- [ ] Tests pass (`mvn test`)
- [ ] Code builds (`mvn clean package`)
- [ ] Documentation updated
- [ ] Commit messages follow convention
- [ ] Branch is up to date with main

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests pass locally
```

### Review Process

1. **Automated Checks** - CI/CD pipeline runs
2. **Code Review** - At least one approval required
3. **Testing** - All tests must pass
4. **Documentation** - README updates reviewed
5. **Merge** - Squash and merge to main

---

## 🐛 Reporting Issues

### Before Creating an Issue

1. **Search existing issues** - Check if already reported
2. **Check documentation** - Review README and docs
3. **Try latest version** - Ensure you're up to date

### Bug Report Template

```markdown
## Bug Description
Clear description of the bug

## Steps to Reproduce
1. Step 1
2. Step 2
3. ...

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- OS: [e.g., Windows 11]
- Java Version: [e.g., 21.0.1]
- Maven Version: [e.g., 3.9.6]
- AWS Region: [e.g., us-east-1]

## Logs
```
Relevant log output
```

## Additional Context
Any other information
```

### Feature Request Template

```markdown
## Feature Description
Clear description of the feature

## Use Case
Why is this needed?

## Proposed Solution
How should it work?

## Alternatives Considered
Other approaches you've thought about

## Additional Context
Any other information
```

---

## 🏗️ Module-Specific Guidelines

### Token Module

- Token caching logic must be thread-safe
- Add tests for cache hit/miss scenarios
- Update cache TTL documentation

### Service Module

- HTTP client changes require performance testing
- SSL/TLS updates need security review
- External API mocks for testing

### TaskService Module

- Support all three event sources
- Router pattern must remain clean
- Add tests for each event type

---

## 📚 Resources

### Documentation

- [Main README](README.md)
- [Token Module](token/README.md)
- [Service Module](service/README.md)
- [TaskService Module](taskService/README.md)
- [Infrastructure](infra/README.md)

### External Links

- [AWS Lambda Java](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html)
- [AWS Powertools](https://docs.aws.amazon.com/powertools/java/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Lombok Documentation](https://projectlombok.org/)

---

## 🙏 Recognition

Contributors will be recognized in:
- GitHub contributors list
- Project README
- Release notes

Thank you for contributing! 🎉

---

**Questions?** Open a [GitHub Discussion](https://github.com/sajithraj/multimoduleproject/discussions)

*Last Updated: December 29, 2025*

