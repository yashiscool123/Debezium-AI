# Debezium AI v4.0.3 Release Notes

**Release Date:** July 23, 2026  
**Version:** 4.0.3  
**Previous Version:** 4.0.2

---

## Overview

Debezium AI v4.0.3 adds comprehensive encryption for sensitive data both in transit and at rest. All pipeline configuration secrets (database passwords, connection strings, API tokens) are now encrypted using AES-256-GCM before storage and automatically decrypted during deployment. Password hashing is upgraded to PBKDF2WithHmacSHA256 with random salts. HTTP security headers and TLS configuration are enforced.

---

## New Features

### Encryption at Rest
- **AES-256-GCM Encryption** — All pipeline secrets and connector config values marked as sensitive are encrypted before storage using AES-256-GCM with a random 12-byte IV per encryption
- **PBKDF2 Key Derivation** — Encryption master key is derived from a configurable master password using PBKDF2 with 310,000 iterations
- **Automatic Encrypt/Decrypt** — PipelineEngine automatically encrypts secrets on `create()` and `update()`, and decrypts on `deploy()`
- **Bulk Re-Encryption** — `POST /v4/pipelines/encrypt-all` endpoint to re-encrypt all existing pipeline secrets

### Encryption in Transit
- **Strict-Transport-Security** — HSTS header with configurable max-age on all HTTPS responses
- **Security Headers** — X-Content-Type-Options: nosniff, X-Frame-Options: DENY, X-XSS-Protection, Cache-Control headers on every response
- **TLS Redirect** — `quarkus.http.insecure-requests=redirect` to auto-redirect HTTP to HTTPS

### Password Security
- **PBKDF2WithHmacSHA256** — Replaced single-round SHA-256 with 310,000-iteration PBKDF2 with random per-password salt
- **Constant-Time Verification** — Password comparison uses `MessageDigest.isEqual` to prevent timing attacks
- **Registration Hashing** — `POST /v4/auth/register` now hashes passwords before storing

### Export Security
- **Secrets Masking** — All sensitive values are masked (`ab****cd`) during JSON export and release packaging

### Service User & API Key Support (v4.0.2 carryover)
- **Dedicated Service Users** — `UserType.SERVICE` with `PIPELINE_RUN_AS` permission
- **API Key Authentication** — Generate, validate, and revoke API keys for service users
- **Run-as Support** — `POST /v4/pipelines/{id}/run-as/{serviceUserId}` endpoint

### Embedding & LLM Providers (v4.0.2 carryover)
- **5 Embedding Providers** — MiniLM, Ollama, OpenAI, VoyageAI, HuggingFace via LangChain4j
- **4 LLM Providers** — Ollama, OpenAI, Anthropic, Google Gemini with configurable models

---

## Improvements

### Security
- AES-256-GCM encryption for all connector secrets and sensitive config fields at rest
- PBKDF2WithHmacSHA256 password hashing (310K iterations, random salt)
- Security headers on all HTTP responses (HSTS, nosniff, X-Frame-Options)
- Sensitive field auto-detection via pattern matching (password, token, secret, api.key, etc.)
- Secret values masked in export JSON output

### Configuration
- `debezium.encryption.master-key` — Configure the master encryption key
- `debezium.encryption.salt` — Optional pre-configured salt for key derivation
- `debezium.security.hsts.enabled` — Toggle HSTS header
- `debezium.security.hsts.max-age` — HSTS max-age in seconds

---

## Fixed Issues

- Passwords sent to `POST /v4/auth/register` are now properly hashed instead of stored in plaintext
- Pipeline export no longer leaks database credentials in JSON output
- Password hashing uses cryptographic-grade PBKDF2 instead of single-round SHA-256
- HTTP responses now include proper security headers

---

## Known Issues

- In-memory storage does not persist across restarts (encryption key must be reconfigured)
- Multi-threaded connector execution is planned for v4.1.0

---

## What's Next

| Version | Features |
|---|---|
| v4.1.0 | Multi-threaded connector execution, parallel pipeline processing |
| v4.2.0 | UI framework with RBAC, dashboard widgets, real-time monitoring |

---

## Changelog

```
v4.0.3
+ AES-256-GCM encryption for pipeline secrets and sensitive config at rest
+ PBKDF2WithHmacSHA256 password hashing (310K iterations, random salt)
+ Security headers (HSTS, X-Content-Type-Options, X-Frame-Options)
+ Sensitive value masking in export/release JSON output
+ POST /v4/pipelines/encrypt-all endpoint for bulk re-encryption
+ application.properties with encryption and security config defaults
+ CryptoUtil utility class for AES-256-GCM encrypt/decrypt
+ SecretManager CDI bean for automated encrypt/decrypt/mask
* AuthenticationService password hashing upgraded from SHA-256 to PBKDF2
* AuthResource register endpoint now hashes passwords before storage
v4.0.2
+ Service user support (UserType.SERVICE, API keys, run-as)
+ 5 embedding providers (MiniLM, Ollama, OpenAI, VoyageAI, HuggingFace)
+ 4 LLM providers (Ollama, OpenAI, Anthropic, Google Gemini)
+ EmbeddingConfig/LLMConfig records with provider factory
+ ProviderRegistry for property-based service creation
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
