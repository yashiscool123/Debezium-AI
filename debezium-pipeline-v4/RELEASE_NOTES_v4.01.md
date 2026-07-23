# Debezium AI v4.0.1 Release Notes

**Release Date:** July 23, 2026  
**Version:** 4.0.1-SNAPSHOT  
**Previous Version:** 4.0.0-SNAPSHOT

---

## Overview

Debezium AI v4.0.1 is a significant enhancement over v4.0.0, adding enterprise-grade security, multi-environment release management, NoSQL storage, one-click deployment, and comprehensive documentation. This release transforms Debezium AI from a pipeline generator into a full-featured CDC pipeline management platform.

---

## New Features

### Authentication & Authorization
- **Username/Password Authentication** — Built-in authentication service with session management
- **SSO/OIDC Support** — Integration with Keycloak, Google, GitHub, and Azure AD
- **Role-Based Access Control (RBAC)** — 9 predefined roles with granular permissions
- **API Key Authentication** — Backward-compatible API key support
- **Session Management** — Token-based sessions with expiry and tracking

### Multi-Environment Release Management
- **Pipeline Export/Import** — Export pipelines as JSON packages for environment promotion
- **Release Packaging** — Bundle pipelines with metadata and release notes
- **Environment Overrides** — Automatic configuration adjustment per target environment
- **DEV → QA → PROD Workflow** — Full lifecycle pipeline promotion

### NoSQL Storage Layer
- **Generic NoSqlStore Interface** — Pluggable storage abstraction
- **Job History Store** — Track all pipeline runs with status, metrics, and errors
- **Configuration Store** — Indexed configuration storage with field-based lookups
- **MongoDB Support** — First-class MongoDB job history store implementation

### One-Click Setup & Installer
- **Interactive Setup Wizard (bash)** — Step-by-step configuration for Linux/Mac
- **PowerShell Installer** — Windows-native setup script
- **Docker Compose Profile** — Single-command deployment with optional services
- **Dockerfile (Multi-stage)** — Optimized production container image
- **Prometheus + Grafana** — Pre-configured monitoring stack

### Documentation
- **Developer Guide** — Architecture, module structure, SPI, coding standards
- **User Guide** — Getting started, feature guides, API reference, best practices
- **Deployment Guide** — Comprehensive cloud deployment (Azure, AWS, GCP)
- **Release Notes** — This document

### AI/ML Enhancements
- **OIDCProvider** — SSO authentication for AI mapping service integration
- **Enhanced MappingEngine** — Improved embedding-based column matching

---

## Improvements

### Security
- Added RBACFilter for API endpoint authorization
- Added password hashing with SHA-256
- Added session validation on every request
- Added public endpoint whitelisting

### API
- New `/v4/auth/*` endpoints for authentication
- New `/v4/users/*` endpoints for user management
- New `/v4/export/*` endpoints for pipeline export/import
- New `/v4/audit` endpoint for audit log access

### Performance
- In-memory stores use ConcurrentHashMap for thread safety
- Parallel pipeline import with batch processing
- Optimized session lookup with cached validation

### Developer Experience
- Comprehensive API reference in OpenAPI format
- Type-safe builder patterns for all model records
- Consistent error responses with ApiResponse wrapper

---

## Fixed Issues

- Session expiry now properly invalidates tokens
- Pipeline deployment now returns proper HTTP status codes
- SSO authentication handles token exchange errors gracefully
- Environment variable substitution in deployment configurations
- CRLF/LF line ending normalization for cross-platform compatibility

---

## Breaking Changes

1. **Authentication Required** — All API endpoints now require authentication (Bearer token or API key)
   - Exception: `/v4/auth/login`, `/v4/health`, `/q/health`, `/q/metrics`, `/v4/openapi`
2. **API Response Format** — All endpoints now return `ApiResponse<T>` wrapper
3. **Session Token** — Use `Authorization: Bearer <session-id>` header for API calls

### Migration from v4.0.0

```bash
# Old
curl http://localhost:8080/v4/pipelines

# New (v4.0.1)
SESSION=$(curl -s -X POST http://localhost:8080/v4/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-pass"}' | jq -r '.data.sessionId')

curl -H "Authorization: Bearer $SESSION" http://localhost:8080/v4/pipelines
```

---

## Known Issues

- SSO providers require manual configuration in `application.properties`
- In-memory storage does not persist across restarts
- Multi-threaded connector execution is planned for v4.1.0
- Cloud installer scripts require provider CLI tools (az, aws, gcloud)

---

## Deprecations

- API key via `X-API-Key` header still works but is deprecated in favor of Bearer tokens
- The v3 API endpoints (`/api/v1/*`) remain available but are deprecated

---

## Contributors

A full list of contributors is available in the GitHub repository.

---

## What's Next

The following features are planned for future releases:

| Version | Features |
|---|---|
| v4.1.0 | Multi-threaded connector execution, parallel pipeline processing |
| v4.2.0 | UI framework with RBAC, dashboard widgets, real-time monitoring |
| v4.3.0 | Cloud-native installer scripts, Terraform modules, marketplace listings |
| v5.0.0 | Distributed architecture, streaming-first execution engine |

---

## Changelog

```
v4.0.1
+ Authentication service with username/password and SSO
+ Role-based access control with 9 roles and permissions
+ Pipeline export/import for multi-environment releases
+ NoSQL storage with job history and configuration stores
+ One-click setup wizard (bash and PowerShell)
+ Docker Compose deployment with full service stack
+ Multi-stage Dockerfile for production builds
+ Comprehensive developer, user, and deployment documentation
+ RBACFilter for API endpoint authorization
+ OIDC SSO provider (Keycloak, Google, GitHub, Azure AD)
* Enhanced AI mapping with improved embedding matching
* Security: password hashing, session management, input validation
* Performance: ConcurrentHashMap stores, parallel operations
```

---

## Quick Start

```bash
# Build and run
cd debezium-pipeline-v4
./mvnw clean install -Dquick -DskipITs
./mvnw quarkus:dev

# Access
# http://localhost:8080/v4
# http://localhost:8080/v4/openapi
```
