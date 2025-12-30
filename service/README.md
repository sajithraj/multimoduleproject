# Service Module

**Shared base module for common service functionality.**

---

## 📋 Overview

This module contains shared utilities, base classes, and common functionality used across all service modules.

---

## 📁 Structure

```
service/
├── pom.xml
├── README.md
└── src/main/java/com/project/service/
    └── (shared utilities)
```

---

## 🔧 Usage

This module is a dependency for:

- `taskService` - Main Lambda function
- `token` - Authentication module (future)

---

## 🚀 Building

```powershell
mvn clean install -pl service
```

---

**Status:** ✅ Active  
**Type:** Shared Library

