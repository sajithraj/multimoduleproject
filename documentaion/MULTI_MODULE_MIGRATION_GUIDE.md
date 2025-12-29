# 📋 MULTI-MODULE MIGRATION GUIDE

## ✅ Structure Created

The following skeleton has been created for you:

```
SetUpProject/
├── pom.xml.new              ← PARENT POM (replace current pom.xml with this)
├── token/
│   ├── pom.xml              ✅ CREATED
│   └── src/main/java/com/project/token/
│       ├── config/
│       ├── dto/
│       ├── exception/
│       └── (files to move here)
└── service/
    ├── pom.xml              ✅ CREATED
    └── src/main/java/com/project/service/
        ├── client/
        │   └── dto/
        ├── config/
        ├── util/
        └── (files to move here)
```

---

## 📂 FILES TO MOVE

### TOKEN MODULE

Move these files from `src/main/java/com/project/services/token/` to `token/src/main/java/com/project/token/`:

```
FROM: src/main/java/com/project/services/token/
TO:   token/src/main/java/com/project/token/

Files:
├── TokenService.java                    → token/src/main/java/com/project/token/
├── TokenCache.java                      → token/src/main/java/com/project/token/
├── TokenAuthorizationService.java       → token/src/main/java/com/project/token/
├── dto/TokenResponse.java               → token/src/main/java/com/project/token/dto/
└── (create) TokenException.java         → token/src/main/java/com/project/token/exception/
```

**Config file for token module (create new):**

```
token/src/main/java/com/project/token/config/TokenConfig.java

Content:
- TOKEN_ENDPOINT_URL constant
- TOKEN_SECRET_NAME constant
- Shared token configuration
```

---

### SERVICE MODULE

Move these files from current structure to `service/src/main/java/com/project/service/`:

```
FROM: src/main/java/com/project/
TO:   service/src/main/java/com/project/service/

Files to move:
├── ApiHandler.java                      → service/src/main/java/com/project/service/
├── client/ExternalApiClient.java        → service/src/main/java/com/project/service/client/
├── client/AuthenticatedApiClient.java   → service/src/main/java/com/project/service/client/
├── client/dto/ExternalApiRequest.java   → service/src/main/java/com/project/service/client/dto/
├── client/dto/ExternalApiResponse.java  → service/src/main/java/com/project/service/client/dto/
├── config/AppConfig.java                → service/src/main/java/com/project/service/config/
├── config/RetryConfigProvider.java      → service/src/main/java/com/project/service/config/
└── util/HttpClientFactory.java          → service/src/main/java/com/project/service/util/
```

**Exception file:**

```
service/src/main/java/com/project/service/
├── exception/ExternalApiException.java

OR keep in shared location if needed for both modules
```

---

## 🔧 STEPS TO DO MIGRATION

### Step 1: Update Parent POM

```bash
# Delete old pom.xml
del pom.xml

# Rename new parent pom
ren pom.xml pom.xml
```

### Step 2: Move Token Files

Copy these files from `src/main/java/com/project/services/token/` to `token/src/main/java/com/project/token/`:

- TokenService.java
- TokenCache.java
- TokenAuthorizationService.java
- TokenResponse.java (from dto folder)

### Step 3: Move Service Files

Copy these files from `src/main/java/com/project/` to `service/src/main/java/com/project/service/`:

- ApiHandler.java
- ExternalApiClient.java (from client folder)
- AuthenticatedApiClient.java (from client folder)
- ExternalApiRequest.java (from client/dto)
- ExternalApiResponse.java (from client/dto)
- AppConfig.java (from config folder)
- RetryConfigProvider.java (from config folder)
- HttpClientFactory.java (from util folder)

### Step 4: Create Shared Exception (Optional)

Can be in either module or created in a shared location:

- ExternalApiException.java

---

## 📝 IMPORTANT: Update Package Names!

When moving files, update the package statements at the top:

### Token Module Files

Change from:

```java
package com.project.services.token;
package com.project.services.token.dto;
```

Change to:

```java
package com.project.token;
package com.project.token.dto;
```

### Service Module Files

Change from:

```java
package com.project;
package com.project.client;
package com.project.config;
package com.project.util;
```

Change to:

```java
package com.project.service;
package com.project.service.client;
package com.project.service.client.dto;
package com.project.service.config;
package com.project.service.util;
```

---

## 🔄 Update Import Statements

After moving files, you'll need to update imports:

### In Service Module (imports from Token Module)

```java
// Change from

import com.project.services.token.TokenAuthorizationService;
import com.project.services.token.dto.TokenResponse;

// Change to
import com.project.token.TokenAuthorizationService;
import com.project.token.dto.TokenResponse;
```

### In Service Config

```java
// Change from

import com.project.config.RetryConfigProvider;
import com.project.config.AppConfig;

// Change to
import com.project.service.config.RetryConfigProvider;
import com.project.service.config.AppConfig;
```

---

## 🧪 Testing After Migration

Once files are moved and organized:

```bash
# Clean build
mvn clean install -DskipTests

# Should build both modules: token and service
# Output JAR will be in: service/target/service-1.0-SNAPSHOT.jar
```

---

## 📋 CHECKLIST

- [ ] Copy pom.xml.new → pom.xml
- [ ] Move token files to token/src/main/java/com/project/token/
- [ ] Move service files to service/src/main/java/com/project/service/
- [ ] Update package statements in all files
- [ ] Update import statements
- [ ] Delete old src/ directory (after moving all files)
- [ ] Run: mvn clean install -DskipTests
- [ ] Verify build succeeds
- [ ] Test with curl: `aws lambda invoke ...`

---

## ✅ Final Structure

After migration:

```
SetUpProject/
├── pom.xml                          (parent POM)
├── token/
│   ├── pom.xml
│   └── src/main/java/com/project/token/
│       ├── TokenService.java
│       ├── TokenCache.java
│       ├── TokenAuthorizationService.java
│       ├── config/TokenConfig.java
│       ├── dto/TokenResponse.java
│       └── exception/TokenException.java
├── service/
│   ├── pom.xml
│   └── src/main/java/com/project/service/
│       ├── ApiHandler.java
│       ├── client/
│       │   ├── ExternalApiClient.java
│       │   ├── AuthenticatedApiClient.java
│       │   └── dto/
│       │       ├── ExternalApiRequest.java
│       │       └── ExternalApiResponse.java
│       ├── config/
│       │   ├── AppConfig.java
│       │   └── RetryConfigProvider.java
│       ├── util/HttpClientFactory.java
│       └── exception/ExternalApiException.java
└── infra/
    ├── terraform/
    ├── docker/
    └── ...
```

---

## 💡 Tips

- Move one file at a time and update imports
- Use IDE's refactor → move class feature if available (auto-updates imports)
- Run `mvn clean install` after each major move to catch errors early
- Keep old src/ directory until fully migrated for reference

---

**Take your time with the migration - no rush!** 🚀

