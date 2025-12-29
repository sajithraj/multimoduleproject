# ⚠️ PACKAGE NAME FIXES NEEDED

## Issue Found

The files were moved but package declarations were not updated correctly.

## Fix Required

### Token Module Files Need Fixing:

**TokenService.java** (in token/src/main/java/com/project/token/service/)

- Current: `package com.project.service;`
- Should be: `package com.project.token.service;`
- Update imports: `com.project.config.*` → `com.project.token.config.*`
- Update imports: `com.project.util.*` → `com.project.token.util.*`
- Update imports: `com.project.exception.*` → `com.project.token.exception.*`

**TokenCache.java** (in token/src/main/java/com/project/token/auth/)

- Current: `package com.project.auth;`
- Should be: `package com.project.token.auth;`
- Update imports: `com.project.service.TokenService` → `com.project.token.service.TokenService`
- Update imports: `com.project.exception.*` → `com.project.token.exception.*`

**TokenAuthorizationService.java** (in token/src/main/java/com/project/token/service/)

- Current: `package com.project.service;`
- Should be: `package com.project.token.service;`
- Update imports: `com.project.exception.*` → `com.project.token.exception.*`

**TokenResponse.java** (in token/src/main/java/com/project/token/dto/)

- Current: Check package name
- Should be: `package com.project.token.dto;`

### Service Module Files Need Fixing:

**ApiHandler.java** (in service/src/main/java/com/project/service/)

- Current: Check package name
- Should be: `package com.project.service;` ✅ (this is correct)
- Update imports from old packages to new ones
- Update: `com.project.client.*` → `com.project.service.client.*`
- Update: `com.project.config.*` → `com.project.service.config.*`

All other service files - similar pattern.

## Action Required

I need to fix all package names and imports. Shall I do it now?

