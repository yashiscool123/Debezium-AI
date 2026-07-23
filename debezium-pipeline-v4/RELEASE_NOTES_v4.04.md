# Debezium AI v4.0.4 Release Notes

**Release Date:** July 23, 2026  
**Version:** 4.0.4  
**Previous Version:** 4.0.3

---

## Overview

Debezium AI v4.0.4 introduces inbuilt Data Quality Checks as a first-class pipeline feature, with both rule-based validation and AI-powered anomaly detection. Pipelines can now automatically validate records against NOT_NULL, REGEX, RANGE, UNIQUE, and other rules, while the AI engine uses embeddings and LLMs for semantic anomaly detection, schema drift monitoring, and natural-language validation.

---

## New Features

### Data Quality Checks (Rule-Based)
- **Built-in Rule Engine** — Evaluate records against 14 rule types: NOT_NULL, NOT_EMPTY, REGEX, RANGE, EQUALS, UNIQUE, TYPE_CHECK, MIN_LENGTH, MAX_LENGTH, COMPLETENESS, ACCURACY, CONSISTENCY, TIMELINESS, CUSTOM_SQL
- **Pipeline Integration** — `DataQualityConfig` on every `PipelineDefinition` with `enabled`, `failOnError`, and `mode` (sync/async) settings
- **Per-Rule Configuration** — Each rule supports `scope` (ROW/COLUMN/TABLE/PIPELINE), `column`, `severity` (WARN/ERROR/FATAL), `threshold`, `predicate`, and `expectedValue`
- **Validation** — `PipelineValidator` validates all DQ rule definitions at configuration time

### AI-Powered Data Quality
- **LLM Validation** (`AI_VALIDATE`) — Sends records with a natural-language rule description to the configured LLM (Ollama/OpenAI/Anthropic/Google); LLM returns structured JSON with pass/fail per row
- **Anomaly Detection** (`AI_ANOMALY`) — Embeds each record value using the configured embedding provider and compares against a growing vector store; flags records with cosine similarity < 0.6
- **Schema Drift Detection** (`AI_SCHEMA_DRIFT`) — Computes embedding centroids of baseline vs current records; dissimilarity > 0.2 triggers drift alert
- **Semantic Consistency Check** (`AI_SEMANTIC_CHECK`) — Embeds unique column values and measures pairwise similarity; low average similarity indicates inconsistent data

### REST API (Data Quality)
- `POST /v4/pipelines/{id}/quality/check` — Run all configured DQ rules (built-in + AI) in a single call
- `POST /v4/pipelines/{id}/quality/ai-check?mode=` — Run AI-only checks (anomaly/semantic/drift/all)
- `POST /v4/pipelines/{id}/quality/ai-validate` — Custom LLM validation with user-defined prompt
- `GET /v4/pipelines/{id}/quality/results` — Retrieve stored check results (with pass rate, totals)
- `DELETE /v4/pipelines/{id}/quality/results` — Clear results
- `DELETE /v4/pipelines/{id}/quality/ai-baseline` — Reset embedding baseline

### RBAC
- `DATA_QUALITY_READ` and `DATA_QUALITY_WRITE` permissions added to Permission enum
- All DQ endpoints mapped to appropriate permissions in RBACFilter
- ADMIN and PIPELINE_MANAGER roles include both DQ permissions; PIPELINE_OPERATOR includes READ

---

## Improvements

### Security (v4.0.3 carryover)
- AES-256-GCM encryption for all pipeline secrets and sensitive config fields at rest
- PBKDF2WithHmacSHA256 password hashing (310K iterations, random salt)
- Security headers on all HTTP responses (HSTS, nosniff, X-Frame-Options)

### Service Users & Providers (v4.0.2 carryover)
- Service user support with API key authentication and run-as functionality
- 5 embedding providers: MiniLM, Ollama, OpenAI, VoyageAI, HuggingFace
- 4 LLM providers: Ollama, OpenAI, Anthropic, Google Gemini

---

## Known Issues

- In-memory DataQualityEngine results do not persist across restarts
- AI quality checks require at least one LLM provider configured (Ollama default)
- Embedding-based anomaly detection requires a warm-up phase to build the vector store

---

## What's Next

| Version | Features |
|---|---|
| v4.1.0 | Multi-threaded connector execution, parallel pipeline processing |
| v4.2.0 | UI framework with RBAC, dashboard widgets, real-time monitoring |

---

## Changelog

```
v4.0.4
+ Data Quality Checks as built-in pipeline feature
+ 14 rule types: NOT_NULL, REGEX, RANGE, UNIQUE, EQUALS, TYPE_CHECK, and more
+ DataQualityRuleSpec model with scope, severity, threshold, predicate
+ DataQualityConfig on PipelineDefinition (enabled/failOnError/mode)
+ DataQualityEngine: rule evaluation engine with in-memory result store
+ AI_VALIDATE: LLM-powered record validation with structured JSON response
+ AI_ANOMALY: embedding-based anomaly detection with vector store
+ AI_SCHEMA_DRIFT: embedding centroid comparison for drift detection
+ AI_SEMANTIC_CHECK: pairwise embedding similarity for consistency
+ AIQualityService: LLMService + EmbeddingService + VectorStore integration
+ 8 REST endpoints for rule-based and AI quality checks
+ DATA_QUALITY_READ / DATA_QUALITY_WRITE permissions and RBAC mapping
* PipelineValidator extended to validate all DQ rule definitions
v4.0.3
+ AES-256-GCM encryption for pipeline secrets and sensitive config at rest
+ PBKDF2WithHmacSHA256 password hashing (310K iterations, random salt)
+ Security headers (HSTS, X-Content-Type-Options, X-Frame-Options)
+ Sensitive value masking in export/release JSON output
v4.0.2
+ Service user support (UserType.SERVICE, API keys, run-as)
+ 5 embedding providers (MiniLM, Ollama, OpenAI, VoyageAI, HuggingFace)
+ 4 LLM providers (Ollama, OpenAI, Anthropic, Google Gemini)
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
