# Quick Reference - After Reorganization

## 📍 Key Locations

### Java Source Code

```
src/main/java/com/project/          ← New package location
├── ApiHandler.java                  ← Lambda handler
├── config/AppConfig.java            ← Contains token credentials
└── (19 Java files total)
```

### Infrastructure

```
infra/                               ← All infrastructure code here
├── cloudformation/                  ← CloudFormation template
├── terraform/                       ← Terraform with credentials
├── docker/                          ← LocalStack Docker setup
├── docs/                            ← All documentation
└── README.md                        ← Infrastructure guide
```

---

## 🔐 Token Credentials

**Same values everywhere:**

```
Client ID:     ce43d3bd-e1e0-4eed-a269-8bffe958f0fb
Client Secret: aRZdZP63VqTmhfLcSE9zbAjG
```

**Locations:**

- AppConfig.java → Java code
- terraform.tfvars → AWS deployment
- terraform.localstack.tfvars → LocalStack development

---

## 🚀 Quick Build & Deploy

### 1. Rebuild Maven

```bash
mvn clean install -DskipTests
```

### 2. Deploy Infrastructure

```bash
cd infra/terraform
terraform init
terraform apply -var-file=terraform.localstack.tfvars
```

### 3. Update Lambda

```bash
aws lambda update-function-code \
  --function-name my-token-auth-lambda \
  --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar \
  --endpoint-url http://localhost:4566
```

### 4. Test Lambda

```bash
aws lambda invoke \
  --function-name my-token-auth-lambda \
  --payload '{}' \
  --endpoint-url http://localhost:4566 \
  response.json
```

---

## 📝 Important Files

| File                                        | Purpose                                    |
|---------------------------------------------|--------------------------------------------|
| pom.xml                                     | Build configuration (groupId: com.project) |
| AppConfig.java                              | Token credentials in Java code             |
| infra/terraform/terraform.tfvars            | AWS deployment credentials                 |
| infra/terraform/terraform.localstack.tfvars | LocalStack credentials                     |
| infra/docker/docker-compose.yml             | LocalStack Docker setup                    |
| infra/docs/                                 | All documentation                          |

---

## ✅ Verification Checklist

- [ ] Ran: `mvn clean install -DskipTests`
- [ ] Build successful with com.project package
- [ ] Ran: `terraform apply -var-file=terraform.localstack.tfvars`
- [ ] Secrets Manager secret created in LocalStack
- [ ] Updated Lambda with new JAR
- [ ] Lambda function invokes successfully

---

## 🎯 Handler Class

**Handler for AWS Lambda:**

```
com.project.ApiHandler
```

(Updated in pom.xml as groupId: com.project)

---

**Status**: Ready to Build and Deploy ✅

