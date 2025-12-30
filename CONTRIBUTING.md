# Contributing to Task Service

Thank you for your interest in contributing to the Task Service project! This document provides guidelines and
instructions for contributing.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Issue Reporting](#issue-reporting)

---

## 🤝 Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Our Standards

**Positive behavior includes:**

- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community

**Unacceptable behavior includes:**

- Harassment, trolling, or derogatory comments
- Public or private harassment
- Publishing others' private information
- Other conduct which could reasonably be considered inappropriate

---

## 🚀 Getting Started

### Prerequisites

Before contributing, ensure you have:

- Java 21+ installed
- Maven 3.9+ installed
- Docker (for LocalStack testing)
- Git configured
- IDE (IntelliJ IDEA recommended)

### Fork and Clone

1. **Fork the repository** on GitHub
2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/SetUpProject.git
   cd SetUpProject
   ```

3. **Add upstream remote:**
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/SetUpProject.git
   ```

4. **Verify remotes:**
   ```bash
   git remote -v
   ```

---

## 💻 Development Setup

### 1. Build the Project

```powershell
# Full build
mvn clean install

# Verify tests pass
mvn test
```

### 2. Start LocalStack

```powershell
cd infra/docker
docker-compose up -d
```

### 3. Deploy to LocalStack

```powershell
cd infra/terraform
terraform init
terraform apply -var="use_localstack=true" -auto-approve
```

### 4. Verify Setup

```powershell
# Check Lambda
aws lambda list-functions --endpoint-url http://localhost:4566

# Test API
.\test-api.ps1
```

---

## 🎯 How to Contribute

### Types of Contributions

We welcome various types of contributions:

1. **Bug Fixes** - Fix issues and improve stability
2. **Features** - Add new functionality
3. **Documentation** - Improve or add documentation
4. **Tests** - Add or improve test coverage
5. **Performance** - Optimize code performance
6. **Refactoring** - Improve code quality

### Contribution Workflow

1. **Find or Create an Issue**
    - Check existing issues
    - Create new issue if needed
    - Discuss approach before starting

2. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/bug-description
   ```

3. **Make Changes**
    - Write clean, readable code
    - Follow coding standards
    - Add/update tests
    - Update documentation

4. **Test Your Changes**
   ```powershell
   # Run all tests
   mvn clean test
   
   # Run specific tests
   mvn test -Dtest=YourTest
   
   # Verify LocalStack integration
   .\test-api.ps1
   ```

5. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat: add amazing feature"
   ```

6. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **Create Pull Request**
    - Go to GitHub
    - Click "New Pull Request"
    - Fill in the template
    - Wait for review

---

## 📝 Coding Standards

### Java Code Style

**Follow these principles:**

1. **Use Lombok** - Reduce boilerplate
   ```java
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class Task {
       private String id;
       private String name;
   }
   ```

2. **Use MapStruct** - Type-safe mapping
   ```java
   @Mapper(componentModel = "default")
   public interface TaskMapper {
       TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);
       Task toEntity(TaskRequestDTO dto);
   }
   ```

3. **Proper Logging**
   ```java
   @Slf4j
   public class MyService {
       public void process() {
           log.info("Processing started");
           log.debug("Debug details: {}", data);
           log.error("Error occurred: {}", e.getMessage(), e);
       }
   }
   ```

4. **Input Validation**
   ```java
   public class TaskRequestDTO {
       @NotNull(message = "Name is required")
       @NotEmpty(message = "Name cannot be empty")
       private String name;
   }
   ```

5. **Error Handling**
   ```java
   try {
       // business logic
   } catch (SpecificException e) {
       log.error("Specific error: {}", e.getMessage());
       throw new CustomException("User-friendly message", e);
   } catch (Exception e) {
       log.error("Unexpected error", e);
       throw new RuntimeException("Processing failed", e);
   }
   ```

### Code Formatting

- **Indentation:** 4 spaces
- **Line Length:** Max 120 characters
- **Braces:** K&R style
- **Imports:** Organize and remove unused
- **Naming:**
    - Classes: `PascalCase`
    - Methods/Variables: `camelCase`
    - Constants: `UPPER_SNAKE_CASE`
    - Packages: `lowercase`

### Package Structure

```
com.project.task/
├── handler/      # Lambda handlers
├── router/       # Event routers
├── service/      # Business logic
├── model/        # Domain models
├── dto/          # Data transfer objects
├── mapper/       # MapStruct mappers
├── data/         # Data layer
└── util/         # Utilities
```

---

## 🧪 Testing Guidelines

### Test Structure

**Use AAA pattern:**

```java
@Test
public void testFeature() {
    // Arrange
    Task task = Task.builder().name("Test").build();
    
    // Act
    String result = service.process(task);
    
    // Assert
    assertNotNull(result);
    assertEquals("expected", result);
}
```

### Test Categories

1. **Unit Tests** - Test individual components
   ```java
   @Test
   public void testTaskMapper() {
       TaskRequestDTO dto = new TaskRequestDTO();
       dto.setName("Test");
       
       Task task = TaskMapper.INSTANCE.toEntity(dto);
       
       assertEquals("Test", task.getName());
   }
   ```

2. **Integration Tests** - Test multiple components
   ```java
   @Test
   public void testApiGateway_CompleteFlow() {
       APIGatewayProxyRequestEvent request = createRequest();
       Object response = handler.handleRequest(request, context);
       // assertions
   }
   ```

3. **End-to-End Tests** - Test full flow with LocalStack

### Test Coverage

**Minimum Requirements:**

- **Unit Tests:** 80% coverage
- **Integration Tests:** All major flows
- **Edge Cases:** Error conditions, null checks, validation

**Running Tests:**

```powershell
# All tests
mvn test

# With coverage
mvn clean verify

# Specific test
mvn test -Dtest=ApiGatewayIntegrationTest

# Integration tests only
mvn verify -Pintegration-tests
```

### Test Naming

```java
// Good
testApiGateway_CreateTask_Success()

testSQS_InvalidMessage_ThrowsException()

// Bad
test1()

testStuff()
```

---

## 🔄 Pull Request Process

### Before Submitting

**Checklist:**

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] No merge conflicts with main branch
- [ ] Code follows style guidelines
- [ ] Commit messages follow conventions

### PR Template

When creating a PR, include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe testing done

## Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] No breaking changes (or documented)
```

### Review Process

1. **Automated Checks** - CI/CD pipeline runs
2. **Code Review** - At least 1 approval required
3. **Testing** - Reviewer tests changes
4. **Approval** - Maintainer approves
5. **Merge** - Squash and merge to main

### After Merge

- Delete your feature branch
- Pull latest main branch
- Update your fork

```bash
git checkout main
git pull upstream main
git push origin main
```

---

## 📝 Commit Message Guidelines

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat:** New feature
- **fix:** Bug fix
- **docs:** Documentation changes
- **style:** Formatting, missing semi-colons, etc.
- **refactor:** Code restructuring
- **perf:** Performance improvements
- **test:** Adding/updating tests
- **chore:** Maintenance tasks

### Examples

**Good:**

```
feat(api): add task update endpoint

- Added PUT /task/{id} endpoint
- Implemented validation
- Added integration tests

Closes #123
```

```
fix(sqs): resolve message deserialization issue

Fixed Records field mapping by adding Jackson MixIn.
This improves performance by 60% compared to string manipulation.

Fixes #456
```

**Bad:**

```
updated stuff
```

```
fix bug
```

### Rules

1. Use imperative mood ("add" not "added")
2. Don't capitalize first letter
3. No period at the end of subject
4. Limit subject to 50 characters
5. Wrap body at 72 characters
6. Reference issues in footer

---

## 🐛 Issue Reporting

### Before Creating an Issue

1. **Search existing issues** - Avoid duplicates
2. **Check documentation** - Issue might be documented
3. **Test with latest version** - Issue might be fixed

### Bug Report Template

```markdown
## Bug Description
Clear description of the bug

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- Java version:
- Maven version:
- OS:
- LocalStack version:

## Logs/Screenshots
Relevant logs or screenshots

## Additional Context
Any other relevant information
```

### Feature Request Template

```markdown
## Feature Description
Clear description of the feature

## Use Case
Why is this feature needed?

## Proposed Solution
How should it work?

## Alternatives Considered
Other approaches considered

## Additional Context
Any other relevant information
```

---

## 🎯 Development Best Practices

### 1. Keep Changes Focused

- One feature/fix per PR
- Small, reviewable commits
- Clear commit messages

### 2. Write Tests First (TDD)

```java
@Test
public void testNewFeature() {
    // Write test first
    // Then implement feature
}
```

### 3. Document Your Code

```java
/**
 * Processes task creation from API Gateway request.
 * 
 * @param request API Gateway request event
 * @param context Lambda context
 * @return APIGatewayProxyResponseEvent with task data
 * @throws IllegalArgumentException if request validation fails
 */
public APIGatewayProxyResponseEvent processCreateTask(
    APIGatewayProxyRequestEvent request, 
    Context context
) {
    // implementation
}
```

### 4. Handle Errors Gracefully

```java
try{
        return processTask(task);
}catch(
ValidationException e){
        log.

warn("Validation failed: {}",e.getMessage());
        return

createErrorResponse(400,"Invalid input");
}catch(
Exception e){
        log.

error("Unexpected error",e);
    return

createErrorResponse(500,"Internal server error");
}
```

### 5. Performance Considerations

- Use appropriate data structures
- Avoid unnecessary object creation
- Use streaming for large collections
- Profile before optimizing

---

## 📚 Resources

### Project Documentation

- [Main README](README.md)
- [TaskService README](taskService/README.md)
- [API Documentation](taskService/README.md#api-documentation)

### External Resources

- [AWS Lambda Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [Java Coding Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
- [Effective Java](https://www.oreilly.com/library/view/effective-java/9780134686097/)
- [LocalStack Documentation](https://docs.localstack.cloud/)

---

## 🙏 Thank You!

Thank you for contributing to Task Service! Your efforts help make this project better for everyone.

**Questions?** Feel free to ask in issues or discussions.

**Happy Coding!** 🚀

