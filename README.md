# Debezium AI — CDC Pipeline Generator with AI/ML Source-to-Target Mapping

![Debezium Logo](https://raw.githubusercontent.com/debezium/debezium/main/documentation/modules/ROOT/assets/images/debezium-ui-48x48.png)

**Debezium AI** is an intelligent Change Data Capture (CDC) pipeline management platform that automates the creation, configuration, deployment, and monitoring of Debezium-based streaming pipelines. It uses AI/ML (embeddings, LLMs, vector search, and feedback learning) to automatically map source database schemas to target schemas, generate transformation chains, and produce ready-to-deploy artifacts for Kubernetes, Docker Compose, Strimzi, Helm, and Terraform.

This project builds upon the [Debezium](https://github.com/debezium/debezium) platform — an open-source Change Data Capture platform for reliable CDC pipelines.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Quick Install](#quick-install)
  - [Build](#build)
  - [Run](#run)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Authentication & RBAC](#authentication--rbac)
- [Pipeline Export/Import & Releases](#pipeline-exportimport--releases)
- [NoSQL Storage](#nosql-storage)
- [AI/ML Pipeline Mapping](#aiml-pipeline-mapping)
- [Deployment Formats](#deployment-formats)
- [Installer & Deployment](#installer--deployment)
- [Monitoring & Observability](#monitoring--observability)
- [Plugin System](#plugin-system)
- [v3 vs v4](#v3-vs-v4)
- [Documentation](#documentation)
- [License](#license)

---

## Overview

Debezium AI sits on top of the Debezium CDC platform (v3.7.0-SNAPSHOT) and provides:

- **Declarative pipeline definitions** — Describe your source database and target, and the system generates the full pipeline configuration.
- **AI-powered schema mapping** — Automatically maps source tables/columns to targets using embedding similarity, LLM suggestions, and heuristic rules.
- **Multi-format deployment artifacts** — Generate YAML, Docker Compose, Helm charts, Terraform configs, and shell scripts for production deployments.
- **Full lifecycle management** — Create, validate, deploy, monitor, and audit pipelines through a unified REST API.
- **Plugin-based extensibility** — Add new connectors, transformations, or deployment formats via SPI.
- **Enterprise security** — Username/password login, SSO/OIDC (Keycloak, Google, GitHub, Azure AD), and role-based access control with 9 granular roles.
- **Multi-environment releases** — Export pipelines as JSON packages and promote across DEV, QA, and PROD environments.
- **NoSQL storage** — Pluggable storage backend for job history, configurations, and pipeline metadata.
- **One-click installer** — Interactive setup wizard for Windows (PowerShell), Linux/Mac (bash), and Docker Compose.

The project has two implementations:

| Version | Architecture | Notable Features |
|---|--------|------|
| **v3** (`debezium-pipeline-generator`) | Single Quarkus JAR | 14 model classes, 7 services, hardcoded connector types, schema introspection, heuristic + embedding mapping |
| **v4** (`debezium-pipeline-v4`) | Multi-module Quarkus app | 5+ modules, full SPI plugin system, auth/SSO/RBAC, export/import, NoSQL storage, installer, monitoring module |

---

## Features

### Pipeline Generation
- Define source connectors (MySQL, PostgreSQL, MongoDB, SQL Server, Oracle, MariaDB, JDBC, and more)
- Define sink connectors (JDBC, Elasticsearch, S3, Snowflake, BigQuery, Redis, Kafka, and more)
- Automatic topic naming strategy generation (6 strategies)
- Transformation chain builder with 14 SMT types + KSQL/Flink SQL generation
- Deployment artifact generation for 5 formats
- Pipeline validation with detailed error/warning reporting
- Pipeline duplication and version tracking

### AI/ML Mapping
- **Embedding-based column mapping** — Uses MiniLM (384-dim) or any configurable embedding provider to semantically match source and target columns
- **LLM-powered suggestions** — LangChain4j integration with OpenAI or Ollama (llama3.1) for schema mapping, transformation generation, and KSQL query generation
- **Vector store** — In-memory vector database with cosine similarity search and metadata filtering
- **Feedback training** — Learns from user accept/reject decisions, adjusts weight factors (name/type/nullable/pk) over time
- **Schema introspection** — JDBC-based discovery of tables, columns, data types, primary keys, indexes, and foreign keys
- **Type compatibility scoring** — Numeric, string, temporal, and boolean type groups with cross-type compatibility

### Authentication & RBAC
- **Username/Password Login** — Built-in authentication service with session management
- **SSO / OIDC** — Keycloak, Google, GitHub, and Azure AD integration
- **9 RBAC Roles** — SUPER_ADMIN, ADMIN, PIPELINE_MANAGER, PIPELINE_OPERATOR, PIPELINE_VIEWER, CONNECTOR_ADMIN, DATA_ENGINEER, AUDITOR, DEVELOPER
- **Granular Permissions** — Per-endpoint authorization via `RBACFilter`
- **Session Management** — Token-based sessions with expiry and tracking

### Pipeline Export/Import & Releases
- **Pipeline Export** — Export pipelines as JSON packages for environment promotion
- **Release Packaging** — Bundle pipelines with metadata and release notes
- **Environment Overrides** — Automatic configuration adjustment (DEV → QA → PROD)
- **Import** — Restore pipelines from exported packages into target environments

### NoSQL Storage
- **Pluggable NoSqlStore Interface** — Swap storage backends without code changes
- **Job History Store** — Track pipeline runs with status, metrics, errors, and duration
- **Configuration Store** — Indexed configuration storage with field-based lookups
- **MongoDB Implementation** — First-class MongoDB support for production deployments

### One-Click Installer
- **Interactive Setup Wizard (bash)** — Step-by-step configuration for Linux/Mac
- **PowerShell Installer** — Windows-native setup with auto-configuration
- **Docker Compose Profiles** — Single-command deployment (dev, full, streaming, monitoring)
- **Production Dockerfile** — Multi-stage build for optimized container images
- **Prometheus + Grafana** — Pre-configured monitoring stack in Docker Compose

### REST API (v4)
| Endpoint | Purpose |
|---|---|
| `POST /v4/auth/login` | Login with username/password |
| `POST /v4/auth/logout` | Logout and invalidate session |
| `GET /v4/auth/session` | Validate current session |
| `POST /v4/auth/sso/{provider}` | SSO login via OIDC provider |
| `POST /v4/auth/register` | Register new user |
| `GET /v4/users` | List all users |
| `GET /v4/users/roles` | List roles with permissions |
| `GET /v4/users/permissions` | List all permissions |
| `POST /v4/users` | Create user |
| `PUT /v4/users/{username}` | Update user |
| `DELETE /v4/users/{username}` | Delete user |
| `POST /v4/pipelines` | Create pipeline |
| `GET /v4/pipelines` | List pipelines (with tenant filter) |
| `GET /v4/pipelines/{id}` | Get pipeline |
| `PUT /v4/pipelines/{id}` | Update pipeline |
| `DELETE /v4/pipelines/{id}` | Delete pipeline |
| `POST /v4/pipelines/{id}/deploy` | Deploy pipeline |
| `GET /v4/pipelines/{id}/instance` | Get deployment instance status |
| `POST /v4/pipelines/validate` | Validate pipeline config |
| `POST /v4/pipelines/{id}/duplicate` | Duplicate pipeline |
| `POST /v4/mappings/suggest` | AI-suggested table/column mappings |
| `POST /v4/mappings/embed` | Generate embedding for field/text |
| `POST /v4/mappings/similarity` | Cosine similarity between texts |
| `GET /v4/connectors` | List connector plugins |
| `GET /v4/connectors/{name}` | Connector details + config schema |
| `POST /v4/deployments/generate` | Generate deployment artifacts |
| `GET /v4/deployments/formats` | List deployment formats |
| `POST /v4/export/pipelines` | Export pipelines as JSON |
| `POST /v4/export/release` | Create release package |
| `POST /v4/export/environment` | Export all pipelines for environment |
| `POST /v4/export/import` | Import pipelines from JSON |
| `GET /v4/metrics` | Pipeline metrics |
| `GET /v4/metrics/summary` | Metric summary + recent alerts |
| `POST /v4/metrics/alerts` | Evaluate alert rules |
| `GET /v4/health` | Health check |

### Monitoring & Observability
- **MetricsCollector** — In-memory metric store with min/max/sum/count tracking and alert rule evaluation
- **HealthIndicator** — Pluggable health checks with overall UP/DEGRADED status
- **EventBus** — Pub/sub event bus with time-based event log (5,000 event history)
- **DashboardManager** — CRUD for monitoring dashboards with 9 widget types
- **AuditLogger** — In-memory audit trail for actions on pipelines and entities (10,000 entry history)

### Security & Multi-Tenancy
- API key authentication (`X-API-Key` or Bearer token)
- Tenant-aware pipeline isolation
- Built-in alert rules with INFO / WARNING / CRITICAL severity

### Deployment Formats
| Format | Output |
|---|---|
| **Docker Compose** | ZooKeeper, Kafka, Schema Registry, Debezium Connect |
| **Kubernetes YAML** | Namespace, Deployment, Service |
| **Helm** | Strimzi Kafka operator, Kafka cluster, ZooKeeper, Connect |
| **Strimzi CRD** | `KafkaConnector` custom resource |
| **Terraform** | Kubernetes + Helm providers, Strimzi + Connect installation |
| **Shell Script** | Interactive deploy.sh supporting all formats |

---

## Architecture

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Debezium AI v4.0.1                                 │
├───────────────────────────────────────────────────────────────────────────┤
│  Auth / SSO / RBAC │  Pipelines │  Mappings │  Export/Import  │  Users   │
├──────────┬──────────┬──────────┬─────────────┬───────────────┬────────────┤
│   core   │    ai    │   api    │  monitoring  │   plugins     │   nosql    │
│          │          │          │              │               │            │
│ AuthSvc  │Mapping   │REST      │Metrics       │ConnectorPlugin│NoSqlStore  │
│ UserSvc  │Engine    │Resources │Collector     │TransformerPln │JobHistory  │
│ RBACFlt  │Embedding │DTO       │EventBus      │DeploymentPln  │ConfigStore │
│ Pipeline │LLMService│OpenAPI   │Health        │PluginRegistry │MongoDBImpl │
│ Export   │Vector    │Auth      │Dashboards    │               │            │
│ Release  │Store     │Export    │Audit         │MySQL/Postgres │            │
│ NoSqlInt │Feedback  │          │              │Docker/Strimzi │            │
├──────────┴──────────┴──────────┴──────────────┴───────────────┴────────────┤
│                         Debezium Core (3.7.0)                               │
│  API │ Config │ Common │ Connectors │ Storage │ Embedded │ Sink              │
├────────────────────────────────────────────────────────────────────────────┤
│                     JDBC / Kafka Connect / Kubernetes                       │
└────────────────────────────────────────────────────────────────────────────┘
```

### Pipeline Flow
```
1. Authenticate ──> 2. Define Pipeline ──> 3. Schema Introspection ──> 4. AI Mapping
   (login / SSO,    (source/target,      (JDBC metadata            (embeddings + LLM
    RBAC check)      kafka config)        discovery)                 + feedback)

5. Transform ──> 6. Validate ──> 7. Export/Release ──> 8. Generate Artifacts
   Chain Build    (config,         (JSON package,        (YAML, Helm,
   (SMT + KSQL)    schema,          environment           Terraform, Scripts)
                   connectivity)    promotion)

9. Deploy ──> 10. Monitor ──> 11. Job History
   (K8s, Docker    (metrics,        (NoSqlStore -
    Compose,        alerts,          persisted run
    Strimzi)        dashboards)      records)
```

---

## Tech Stack

| Component | v3 | v4 |
|---|---|---|
| **Language** | Java 21 | Java 21 |
| **Framework** | Quarkus 3.x | Quarkus 3.33.1.1 |
| **REST** | RESTEasy Reactive + Jackson | RESTEasy Reactive + Jackson |
| **OpenAPI** | SmallRye OpenAPI | SmallRye OpenAPI (annotated) |
| **DI** | Quarkus Arc (CDI) | Quarkus Arc (CDI) |
| **Debezium** | 3.7.0-SNAPSHOT | 3.7.0-SNAPSHOT |
| **AI Embeddings** | debezium-ai-embeddings-minilm | EmbeddingService + 5 providers: MiniLM, Ollama, OpenAI, VoyageAI, HuggingFace |
| **LLM** | LangChain4j 0.32.1 | LangChain4j 0.35.0 + 4 providers: Ollama, OpenAI, Anthropic, Google Gemini |
| **Vector Store** | Not present | In-memory VectorStore |
| **Feedback Training** | Not present | FeedbackTrainer |
| **Monitoring** | SmallRye Health/Metrics | Dedicated monitoring module |
| **Templating** | — | Apache FreeMarker (core) |
| **Build** | Maven + Quarkus plugin | Maven + Quarkus plugin |
| **Serialization** | Jackson (JSON + YAML) | Jackson (JSON + YAML) |
| **Database Drivers** | PostgreSQL, MySQL, SQL Server, MariaDB, Oracle | PostgreSQL, MySQL, SQL Server, MariaDB, Oracle |
| **Auth / SSO** | — | AuthService + OIDC (Keycloak, Google, GitHub, Azure AD) |
| **RBAC** | — | 9 roles, Permission-based RBACFilter |
| **Export/Import** | — | JSON export/import + release packaging |
| **NoSQL Storage** | — | Pluggable NoSqlStore (MongoDB impl) |
| **Installer** | — | Bash + PowerShell + Docker Compose profiles |

---

## Project Structure

```
Debezium AI/
├── debezium-main/                              # Debezium 3.7.0-SNAPSHOT (forked)
│   ├── debezium-core/                          # CDC engine core
│   ├── debezium-api/                           # Public API
│   ├── debezium-connector-mysql/               # MySQL connector
│   ├── debezium-connector-postgres/            # PostgreSQL connector
│   ├── debezium-connector-mongodb/             # MongoDB connector
│   ├── debezium-connector-sqlserver/           # SQL Server connector
│   ├── debezium-connector-oracle/              # Oracle connector
│   ├── debezium-connector-mariadb/             # MariaDB connector
│   ├── debezium-connector-jdbc/                # JDBC connector
│   ├── debezium-connector-binlog/              # MySQL/MariaDB binlog base
│   ├── debezium-ai/                            # AI embedding modules
│   │   ├── debezium-ai-embeddings/             # Core embeddings API
│   │   ├── debezium-ai-embeddings-minilm/      # MiniLM local embeddings
│   │   ├── debezium-ai-embeddings-ollama/      # Ollama embeddings
│   │   ├── debezium-ai-embeddings-hugging-face/ # HuggingFace embeddings
│   │   ├── debezium-ai-embeddings-voyage-ai/   # VoyageAI embeddings
│   │   └── debezium-ai-docling/                # Document processing
│   ├── debezium-storage/                       # Storage backends (Kafka, File, JDBC, Redis, S3, etc.)
│   ├── debezium-sink/                          # Sink framework
│   ├── debezium-embedded/                      # Embedded engine
│   ├── debezium-server/                        # Debezium Server
│   ├── debezium-scripting/                     # KSQL/Flink scripting
│   ├── debezium-ddl-parser/                    # DDL parser (MySQL, MariaDB, Oracle)
│   ├── debezium-testing/                       # Testcontainers + system test infrastructure
│   └── support/                                # Checkstyle, IDE configs, ArchUnit, RevAPI
│
├── debezium-pipeline-generator/                # v3 — Quarkus monolith
│   └── src/main/java/io/debezium/pipeline/generator/
│       ├── PipelineGeneratorApplication.java
│       ├── api/PipelineGeneratorResource.java  # REST endpoints
│       ├── model/                              # 14 model records
│       ├── service/                            # 7 service classes
│       └── ai/EmbeddingService.java            # AI embedding service
│
└── debezium-pipeline-v4/                       # v4 — Multi-module Quarkus app
    ├── pom.xml                                 # Parent POM
    ├── core/                                   # Models, Engine, Validator, SPI, Auth, Export
    │   ├── src/main/java/.../core/auth/        # AuthService, UserService, RBACFilter
    │   ├── src/main/java/.../core/export/      # ExportService, ReleaseService
    │   └── src/main/java/.../core/nosql/       # NoSqlStore interface
    ├── ai/                                     # Mapping, Embeddings, LLM, Vector Store, Training
    ├── api/                                    # REST Resources, Security, DTO
    ├── monitoring/                             # Metrics, Dashboards, Events, Health, Audit
    ├── plugins/                                # Plugin Registry + Built-in Plugins
    │   ├── connectors/builtin/                 # MySQL, Postgres, MongoDB plugins
    │   ├── transformers/builtin/               # Filter, Drop, Rename, ContentRouter plugins
    │   └── deployment/                         # Strimzi, Docker Compose deployment plugins
    └── nosql/                                  # NoSQL storage implementations
        └── mongodb/                            # MongoDB NoSqlStore, JobHistoryStore, ConfigStore
```

---

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Docker (for integration tests)
- Ollama (optional, for local LLM features)
- **SSO (optional)** — Keycloak, or OIDC provider credentials for Google/GitHub/Azure AD

### Quick Install

Use the interactive installer to set up the full stack (Quarkus app + Debezium Connect + Prometheus + Grafana):

**Linux / Mac:**
```bash
curl -fsSL https://raw.githubusercontent.com/your-org/debezium-ai/main/install.sh | bash
```

**Windows (PowerShell):**
```powershell
irm https://raw.githubusercontent.com/your-org/debezium-ai/main/install.ps1 | iex
```

**Docker Compose (all platforms):**
```bash
# Full stack (default)
docker compose --profile full up -d

# Development only (no monitoring)
docker compose --profile dev up -d

# Streaming + monitoring
docker compose --profile streaming up -d

# Monitoring stack only
docker compose --profile monitoring up -d
```

The installer walks through configuration options: database connection, Debezium Connect URL, Ollama endpoint, admin credentials, SSO provider, and export directory.

### Build

#### Build entire project (including Debezium core)

```bash
cd debezium-main
./mvnw clean install -Dquick -DskipITs
```

#### Build v3 pipeline generator

```bash
cd debezium-pipeline-generator
./mvnw clean install -Dquick
```

#### Build v4 pipeline generator

```bash
cd debezium-pipeline-v4
./mvnw clean install -Dquick
```

> `-Dquick` skips tests, code formatting, and checks for fastest iteration.  
> Use `./mvnw clean install` for a full build including tests.

### Run

#### Run v3 (Quarkus dev mode)

```bash
cd debezium-pipeline-generator
./mvnw quarkus:dev
```

Access at: `http://localhost:8080/api/v1`

#### Run v4 (Quarkus dev mode)

```bash
cd debezium-pipeline-v4
./mvnw quarkus:dev
```

Access at: `http://localhost:8080/v4`

#### Run with Docker Compose (production-like)

```bash
# Start all services (Quarkus, Debezium Connect, Prometheus, Grafana)
docker compose --profile full up -d

# View logs
docker compose logs -f

# Stop
docker compose down
```

#### Run as native executable

```bash
./mvnw package -Pnative
./target/debezium-pipeline-v4-4.0.1-runner
```

### Accessing the Application

Once running, the application is available at:

| Interface | URL | Description |
|---|---|---|
| **REST API** | `http://localhost:8080/v4` | All pipeline, mapping, deployment, monitoring, and auth endpoints |
| **Swagger UI** | `http://localhost:8080/v4/openapi` | Interactive API docs — try endpoints directly |
| **Health Check** | `http://localhost:8080/q/health` | Service health and readiness |
| **Prometheus** | `http://localhost:9090` | Metrics (requires `--profile monitoring` or `--profile full`) |
| **Grafana** | `http://localhost:3000` | Dashboards (admin/admin, requires monitoring profile) |

All API access requires authentication. Login first to get a session token:

```bash
curl -X POST http://localhost:8080/v4/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "<your-password>"}'
# Response includes "sessionId" — use as Bearer token for all subsequent calls
```

### Working with Pipelines

**List all pipelines:**
```bash
curl http://localhost:8080/v4/pipelines \
  -H "Authorization: Bearer <session-id>"
```

**Filter by tenant or service user:**
```bash
curl "http://localhost:8080/v4/pipelines?tenant=default" \
  -H "Authorization: Bearer <session-id>"

curl "http://localhost:8080/v4/pipelines?serviceUser=my-svc-user" \
  -H "Authorization: Bearer <session-id>"
```

**Get pipeline details + deployment status:**
```bash
curl http://localhost:8080/v4/pipelines/{id} \
  -H "Authorization: Bearer <session-id>"

curl http://localhost:8080/v4/pipelines/{id}/instance \
  -H "Authorization: Bearer <session-id>"
```

**Pipeline status lifecycle:**
```
DRAFT → VALIDATING → VALID/INVALID → DEPLOYING → DEPLOYED → RUNNING
                                                              ↓
                                                         DEGRADED
                                                              ↓
                                                         FAILED/STOPPED
                                                                  ↓
                                                            ARCHIVED
```

---

## Configuration

### Application Properties (v3)

```properties
quarkus.http.port=8080
quarkus.http.root-path=/api/v1
debezium.ai.embeddings.provider=minilm
debezium.ai.embeddings.minilm.model-path=models/minilm-l6-v2
debezium.ai.llm.provider=ollama
debezium.ai.llm.ollama.url=http://localhost:11434
debezium.ai.llm.ollama.model=llama3.1
debezium.vector.db.type=pgvector
debezium.kafka-connect.url=http://localhost:8083
debezium.schema-registry.url=http://localhost:8081
```

### Application Properties (v4)

```properties
quarkus.http.port=8080
debezium.api.key=<your-api-key>         # Enables API key authentication
debezium.plugins.scan-cdi=true          # Auto-detect plugin CDI beans

# --- Authentication ---
debezium.auth.enabled=true              # Enable auth/SSO/RBAC
debezium.auth.jwt.secret=<jwt-secret>   # JWT signing secret
debezium.auth.session.ttl=3600          # Session TTL in seconds

# --- SSO / OIDC ---
debezium.auth.sso.keycloak.url=http://localhost:8081
debezium.auth.sso.keycloak.realm=debezium
debezium.auth.sso.keycloak.client-id=debezium-ai
debezium.auth.sso.keycloak.client-secret=<secret>
debezium.auth.sso.google.client-id=<google-client-id>
debezium.auth.sso.github.client-id=<github-client-id>
debezium.auth.sso.azure.client-id=<azure-client-id>

# --- RBAC Default Admin ---
debezium.auth.admin.username=admin
debezium.auth.admin.password=<admin-password>

# --- Export / Release ---
debezium.export.directory=./exports     # Export output directory
debezium.export.release.prefix=release  # Release package prefix

# --- NoSQL Storage ---
debezium.nosql.type=mongodb             # Storage backend type
debezium.nosql.mongodb.uri=mongodb://localhost:27017
debezium.nosql.mongodb.database=debezium_ai
debezium.nosql.mongodb.job-collection=job_history
debezium.nosql.mongodb.config-collection=configurations
```

---

## AI/ML Pipeline Mapping

### How It Works

1. **Schema Introspection** — The system connects to source and target databases via JDBC and discovers all tables, columns, data types, primary keys, indexes, and foreign keys.

2. **Column Similarity Scoring** — Each source column is compared against each target column using:
   - **Name similarity** — Levenshtein distance on column names
   - **Type compatibility** — Numeric, string, temporal, and boolean type groups
   - **Nullable compatibility** — Whether both are nullable
   - **Primary key match** — Whether both are primary keys
   - **Semantic similarity** — Optional: column names + types are embedded as vectors and compared via cosine similarity

3. **Table Matching** — Unmapped tables are auto-matched by:
   - Table name similarity
   - Column count similarity
   - Column name overlap

4. **LLM Enhancement** — The schema context can be sent to an LLM (Ollama/OpenAI) which returns structured JSON mapping suggestions. These are merged with heuristic results.

5. **Confidence Scoring** — Each mapping receives a 0–1 confidence score. Mappings above a configurable threshold are auto-enabled.

6. **Feedback Learning** — User accept/reject decisions are recorded. The FeedbackTrainer adjusts weight factors for name/type/nullable/pk similarity over time.

### Embedding Providers

| Provider | Default Model | Dimensions | Location | Config Key |
|---|---|---|---|---|
| **MiniLM** | `all-MiniLM-L6-v2` | 384 | Local (CPU) | `minilm` |
| **Ollama** | `nomic-embed-text` | 768 | Local | `ollama` |
| **OpenAI** | `text-embedding-ada-002` | 1536 | Cloud | `openai` |
| **VoyageAI** | `voyage-code-2` | 1024 | Cloud | `voyageai` |
| **HuggingFace** | `bert-base-uncased` | 768 | Cloud | `huggingface` |

**Configuration example (OpenAI):**
```properties
debezium.ai.embeddings.provider=openai
debezium.ai.embeddings.model=text-embedding-3-small
debezium.ai.embeddings.api-key=${OPENAI_API_KEY}
```

### LLM Providers

| Provider | Default Model | Location | Config Key |
|---|---|---|---|
| **Ollama** | `llama3.1` | Local | `ollama` |
| **OpenAI** | `gpt-4o` | Cloud | `openai` |
| **Anthropic** | `claude-3-opus-20240229` | Cloud | `anthropic` |
| **Google Gemini** | `gemini-1.5-pro` | Cloud | `google` |

**Configuration examples:**
```properties
# Ollama (default)
debezium.ai.llm.provider=ollama
debezium.ai.llm.model=llama3.1
debezium.ai.llm.base-url=http://localhost:11434

# OpenAI
debezium.ai.llm.provider=openai
debezium.ai.llm.model=gpt-4o
debezium.ai.llm.api-key=${OPENAI_API_KEY}

# Anthropic Claude
debezium.ai.llm.provider=anthropic
debezium.ai.llm.model=claude-3-opus-20240229
debezium.ai.llm.api-key=${ANTHROPIC_API_KEY}

# Google Gemini
debezium.ai.llm.provider=google
debezium.ai.llm.model=gemini-1.5-pro
debezium.ai.llm.api-key=${GOOGLE_API_KEY}
```

---

## Deployment Formats

### Docker Compose

```yaml
services:
  zookeeper:    # confluentinc/cp-zookeeper:7.6.0
  kafka:        # confluentinc/cp-kafka:7.6.0
  debezium-connect:  # debezium/connect:3.7
```

### Strimzi KafkaConnector CRD

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnector
spec:
  class: io.debezium.connector.mysql.MySqlConnector
  tasksMax: 1
  config:
    database.hostname: ${DB_HOST}
    topic.prefix: debezium
```

Also supports: **Kubernetes YAML**, **Helm charts**, **Terraform configs**.

---

## Monitoring & Observability

The v4 monitoring module provides production-grade observability:

- **MetricsCollector** — Track counters and gauges per pipeline. Evaluate alert rules (GREATER_THAN, LESS_THAN, EQUAL_TO, CHANGED) with INFO/WARNING/CRITICAL severity.
- **HealthIndicator** — Register health checks, get overall UP/DEGRADED/DOWN status.
- **EventBus** — Pub/sub for internal events with 5,000-event history.
- **DashboardManager** — Create monitoring dashboards with widgets (COUNTER, TIME_SERIES, TABLE, PIE_CHART, BAR_CHART, HEAT_MAP, TOPOLOGY, LOG_VIEWER, STATUS_INDICATOR).
- **AuditLogger** — Track all user actions with entity type, user ID, and details (10,000-entry history).
- **Job History** — Pipeline run records persisted via NoSqlStore (MongoDB) for long-term analysis.
- **Prometheus + Grafana** — Pre-configured monitoring stack in Docker Compose (`--profile monitoring` or `--profile full`).

---

## Installer & Deployment

Debezium AI includes an interactive installer for one-click setup across platforms:

### Installer Options

| Platform | Script | Features |
|---|---|---|
| **Linux / Mac** | `install.sh` (bash) | Step-by-step wizard, Docker Compose profiles, auto-detection |
| **Windows** | `install.ps1` (PowerShell) | Native Windows setup with admin prompt |
| **All Platforms** | `docker-compose.yml` | Pre-configured profiles (dev, full, streaming, monitoring) |

### Docker Compose Profiles

```bash
# All services (Quarkus + Connect + Prometheus + Grafana)
docker compose --profile full up -d

# Quarkus app only (lightweight dev)
docker compose --profile dev up -d

# Debezium Connect + streaming setup
docker compose --profile streaming up -d

# Prometheus + Grafana only
docker compose --profile monitoring up -d
```

### Production Dockerfile

The multi-stage `Dockerfile` builds an optimized container image using Quarkus native compilation:

```bash
# Build the container image
docker build -t debezium-ai:4.0.1 -f debezium-pipeline-v4/Dockerfile .

# Run with environment overrides
docker run -p 8080:8080 \
  -e DEBEZIUM_AUTH_ENABLED=true \
  -e DEBEZIUM_AUTH_ADMIN_PASSWORD=<password> \
  -e DEBEZIUM_NOSQL_MONGODB_URI=mongodb://host.docker.internal:27017 \
  debezium-ai:4.0.1
```

---

## Plugin System

The v4 plugin system is built on four SPI interfaces:

```java
PipelinePlugin     // Base interface — name, version, lifecycle hooks
ConnectorPlugin    // getConnectorClass(), getRequiredConfig(), getConfigSchema()
TransformerPlugin  // transform(source, target, config) — field transformations
DeploymentPlugin   // generate(pipelineConfig) — deployment artifact generation
```

### Built-in Plugins

| Type | Plugins |
|---|---|
| **Connectors** | MySQL, PostgreSQL, MongoDB (with config schemas) |
| **Transformers** | Filter, Drop Fields, Rename Field, Content-Based Router |
| **Deployment** | Strimzi KafkaConnector CRD, Docker Compose |

Register custom plugins via CDI (`@ApplicationScoped`) or programmatically via `PluginRegistry`.

---

## v3 vs v4

| Feature | v3 | v4 |
|---|---|---|
| **Architecture** | Single JAR | 7 modules (core/ai/api/monitoring/plugins/nosql/installer) |
| **Plugin System** | Hardcoded connector types | SPI + PluginRegistry + CDI scanning |
| **AI Mapping** | Heuristic + embedding similarity | Embeddings + LLM + vector store + feedback training |
| **Monitoring** | SmallRye Health/Metrics | Dedicated module (metrics, dashboards, event bus, audit, health) |
| **API Security** | None | API key + username/password login + SSO/OIDC |
| **RBAC** | None | 9 granular roles with permission-based access control |
| **Multi-Tenancy** | None | Tenant-aware filtering |
| **Pipeline Lifecycle** | 8 status states | 11 status states with instance tracking |
| **Export/Import** | None | JSON export/import + environment promotion + release packaging |
| **NoSQL Storage** | None | Pluggable NoSqlStore with MongoDB implementation |
| **Installer** | None | Interactive bash + PowerShell + Docker Compose profiles |
| **Response Format** | Raw Map responses | Unified ApiResponse<T> wrapper |
| **OpenAPI** | Basic | Full annotations (Operation, Tag, APIResponse) |
| **Deployment Artifacts** | Hardcoded generator | Plugin-based generation (extensible) |

---

## Documentation

| Guide | Description |
|---|---|
| [Developer Guide](./DEVELOPER_GUIDE.md) | Architecture, build setup, coding conventions, module walkthrough |
| [User Guide](./USER_GUIDE.md) | REST API, authentication, pipeline lifecycle, configuration reference |
| [AGENTS.md](./AGENTS.md) | Guidelines for AI coding assistants and agents contributing to the project |

---

## Credits

This project proudly uses the [Debezium](https://github.com/debezium/debezium) project (v3.7.0-SNAPSHOT) as its foundation. Debezium provides the core CDC engine, connectors for major databases (MySQL, PostgreSQL, MongoDB, Oracle, etc.), and the Pipeline API.

Thanks to the Debezium community for creating a truly excellent CDC platform that enables us to build Debezium AI.

---

## License

Apache License 2.0 — see [LICENSE.txt](./debezium-main/LICENSE.txt) and [LICENSE-3rd-PARTIES.txt](./debezium-main/LICENSE-3rd-PARTIES.txt).

---

Built on [Debezium](https://debezium.io/) — the open-source Change Data Capture platform.
