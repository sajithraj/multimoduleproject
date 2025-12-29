# 📋 QUICK REFERENCE - MULTI-MODULE MIGRATION

## Current State

✅ All directories created
✅ All pom.xml files created
✅ Ready for file migration

---

## 3 SIMPLE STEPS

### Step 1: Replace Parent POM

```powershell
cd E:\Development\dev_apps\BlockChain\StableCoin\Lambda\SetUpProject
del pom.xml
ren pom.xml.new pom.xml
```

### Step 2: Move Token Files

Copy from: `src/main/java/com/project/services/token/`
Copy to: `token/src/main/java/com/project/token/`

**Files to move:**

- TokenService.java
- TokenCache.java
- TokenAuthorizationService.java
- dto/TokenResponse.java

**Update package in each file:**
Change: `package com.project.services.token;`
To: `package com.project.token;`

### Step 3: Move Service Files

Copy from: `src/main/java/com/project/`
Copy to: `service/src/main/java/com/project/service/`

**Files to move:**

- ApiHandler.java → service/src/main/java/com/project/service/
- client/ExternalApiClient.java → service/src/main/java/com/project/service/client/
- client/AuthenticatedApiClient.java → service/src/main/java/com/project/service/client/
- client/dto/ExternalApiRequest.java → service/src/main/java/com/project/service/client/dto/
- client/dto/ExternalApiResponse.java → service/src/main/java/com/project/service/client/dto/
- config/AppConfig.java → service/src/main/java/com/project/service/config/
- config/RetryConfigProvider.java → service/src/main/java/com/project/service/config/
- util/HttpClientFactory.java → service/src/main/java/com/project/service/util/
- exception/ExternalApiException.java → service/src/main/java/com/project/service/

**Update packages in each file:**

```java
package com.project;          → package com.project.service;
package com.project.client;   → package com.project.service.client;
package com.project.config;   → package com.project.service.config;
package com.project.util;     → package com.project.service.util;
```

---

## ✅ Verification

After moving all files:

```bash
mvn clean install -DskipTests
```

Expected output:

```
[INFO] SetUpProject - Parent POM ................... SUCCESS
[INFO] SetUpProject - Token Module ................ SUCCESS
[INFO] SetUpProject - Service Module ............. SUCCESS
[INFO] BUILD SUCCESS
```

---

## 📁 Final Structure Example

```
SetUpProject/
├── pom.xml (parent)
├── token/
│   ├── pom.xml
│   └── src/main/java/com/project/token/
│       ├── TokenService.java
│       ├── TokenCache.java
│       ├── TokenAuthorizationService.java
│       ├── config/TokenConfig.java
│       └── dto/TokenResponse.java
└── service/
    ├── pom.xml
    └── src/main/java/com/project/service/
        ├── ApiHandler.java
        ├── client/
        ├── config/
        ├── util/
        └── exception/
```

---

## 📚 See Also

- `MULTI_MODULE_MIGRATION_GUIDE.md` - Detailed step-by-step with imports
- `MULTI_MODULE_READY.md` - Full status overview

---

## 💡 Tips

- IDE Refactor Feature: Right-click class → Refactor → Move (auto-updates imports!)
- Move one file, build, fix imports, then next file
- Keep old src/ directory as backup until fully migrated
- All imports from token module to service: `com.project.token.*`
- All imports from service config/util: `com.project.service.config.*`, `com.project.service.util.*`

---

**Everything is prepared! Just need to move files now.** 🚀


