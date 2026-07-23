# Debezium AI — CDC Pipeline Generator with AI/ML Source-to-Target Mapping

Debezium AI is an intelligent Change Data Capture (CDC) pipeline generator that automates the creation, configuration, deployment, and monitoring of Debezium-based streaming pipelines. It uses AI/ML (embeddings, LLMs, vector search, and feedback learning) to automatically map source database schemas to target schemas, generate transformation chains, and produce ready-to-deploy artifacts for Kubernetes, Docker Compose, Strimzi, Helm, and Terraform.

![Debezium Logo](https://raw.githubusercontent.com/debezium/debezium/main/documentation/modules/ROOT/assets/images/debezium-ui-48x48.png)

**This project builds upon the [Debezium](https://github.com/debezium/debezium) platform — an open-source Change Data Capture platform for reliable CDC pipelines.**

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Build](#build)
  - [Run](#run)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [AI/ML Pipeline Mapping](#aiml-pipeline-mapping)
- [Deployment Formats](#deployment-formats)
- [Monitoring & Observability](#monitoring--observability)
- [Plugin System](#plugin-system)
- [v3 vs v4](#v3-vs-v4)
- [License](#license)

---

## Overview

Debezium AI sits on top of the Debezium CDC platform (v3.7.0-SNAPSHOT) and provides:

- **Declarative pipeline definitions** — Describe your source database and target, and the system generates the full pipeline configuration.
- **AI-powered schema mapping** — Automatically maps source tables/columns to targets using embedding similarity, LLM suggestions, and heuristic rules.
- **Multi-format deployment artifacts** — Generate YAML, Docker Compose, Helm charts, Terraform configs, and shell scripts for production deployments.
- **Full lifecycle management** — Create, validate, deploy, monitor, and audit pipelines through a unified REST API.
- **Plugin-based extensibility** — Add new connectors, transformations, or deployment formats via SPI.

The project has two implementations:

| Version | Architecture | Notable Features |
|---|--------|------|
| **v3** (`debezium-pipeline-generator`) | Single Quarkus JAR | 14 model classes, 7 services, hardcoded connector types, schema introspection, heuristic + embedding mapping |
| **v4** (`debezium-pipeline-v4`) | Multi-module Quarkus app | 5 modules (core/ai/api/monitoring/plugins), full SPI plugin system, feedback learning, vector store, audit logging, API security, multi-tenancy |

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

### REST API (v4)
| Endpoint | Purpose |
|---|---|
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
┌──────────────────────────────────────────────────────────────────┐
│                     Debezium AI Application                       │
├──────────┬──────────┬──────────┬──────────┬──────────────────────┤
│   core   │    ai    │   api    │monitoring│       plugins         │
│          │          │          │          │                      │
│ Models   │Mapping   │REST      │Metrics   │ConnectorPlugin       │
│ Engine   │Engine    │Resources │Collector │TransformerPlugin     │
│ Validator│Embedding │Security  │EventBus  │DeploymentPlugin      │
│ SPI      │Service   │DTO       │Health    │PluginRegistry        │
│          │LLMService│OpenAPI   │Dashboards│                      │
│          │Vector    │          │Audit     │MySQL/Postgres/Mongo   │
│          │Store     │          │          │Strimzi/Docker-Compose │
│          │Feedback  │          │          │Filter/Drop/Rename     │
│          │Trainer   │          │          │                      │
├──────────┴──────────┴──────────┴──────────┴──────────────────────┤
│                    Debezium Core (3.7.0)                          │
│  API │ Config │ Common │ Connectors │ Storage │ Embedded │ Sink   │
├──────────────────────────────────────────────────────────────────┤
│                    JDBC / Kafka Connect / Kubernetes              │
└──────────────────────────────────────────────────────────────────┘
```

### Pipeline Flow
```
1. Define Pipeline ──> 2. Schema Introspection ──> 3. AI Mapping ──> 4. Transform
   (source/target,      (JDBC metadata          (embeddings + LLM    Chain Build
    kafka config)        discovery)               + feedback)         (SMT + KSQL)

5. Validate ──> 6. Generate Artifacts ──> 7. Deploy ──> 8. Monitor
   (config +      (Docker/K8s/Helm/       (via API or    (metrics + health
    connection)     Strimzi/Terraform)      external)      + audit)
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
| **AI Embeddings** | debezium-ai-embeddings-minilm | Custom EmbeddingService (Provider interface) |
| **LLM** | LangChain4j 0.32.1 | LangChain4j 0.35.0 |
| **Vector Store** | Not present | In-memory VectorStore |
| **Feedback Training** | Not present | FeedbackTrainer |
| **Monitoring** | SmallRye Health/Metrics | Dedicated monitoring module |
| **Templating** | — | Apache FreeMarker (core) |
| **Build** | Maven + Quarkus plugin | Maven + Quarkus plugin |
| **Serialization** | Jackson (JSON + YAML) | Jackson (JSON + YAML) |
| **Database Drivers** | PostgreSQL, MySQL, SQL Server, MariaDB, Oracle | PostgreSQL, MySQL, SQL Server, MariaDB, Oracle |

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
    ├── core/                                   # Models, Engine, Validator, SPI
    ├── ai/                                     # Mapping, Embeddings, LLM, Vector Store, Training
    ├── api/                                    # REST Resources, Security, DTO
    ├── monitoring/                             # Metrics, Dashboards, Events, Health, Audit
    └── plugins/                                # Plugin Registry + Built-in Plugins
        ├── connectors/builtin/                 # MySQL, Postgres, MongoDB plugins
        ├── transformers/builtin/               # Filter, Drop, Rename, ContentRouter plugins
        └── deployment/                         # Strimzi, Docker Compose deployment plugins
```

---

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Docker (for integration tests)
- Ollama (optional, for local LLM features)

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

#### Run as native executable

```bash
./mvnw package -Pnative
./target/debezium-pipeline-v4-4.0.0-SNAPSHOT-runner
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
debezium.api.key=<your-api-key>   # Enables API key authentication
debezium.plugins.scan-cdi=true    # Auto-detect plugin CDI beans
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

- **MiniLM** (local, default) — 384-dimensional, runs on CPU
- **Ollama** — local embeddings via Ollama API
- **VoyageAI** — cloud-based embeddings
- **HuggingFace** — HuggingFace inference API

### LLM Providers

- **Ollama** (default) — llama3.1, local inference
- **OpenAI** — GPT models via API

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
| **Architecture** | Single JAR | 5 modules (core/ai/api/monitoring/plugins) |
| **Plugin System** | Hardcoded connector types | SPI + PluginRegistry + CDI scanning |
| **AI Mapping** | Heuristic + embedding similarity | Embeddings + LLM + vector store + feedback training |
| **Monitoring** | SmallRye Health/Metrics | Dedicated module (metrics, dashboards, event bus, audit, health) |
| **API Security** | None | API key authentication |
| **Multi-Tenancy** | None | Tenant-aware filtering |
| **Pipeline Lifecycle** | 8 status states | 11 status states with instance tracking |
| **Response Format** | Raw Map responses | Unified ApiResponse<T> wrapper |
| **OpenAPI** | Basic | Full annotations (Operation, Tag, APIResponse) |
| **Deployment Artifacts** | Hardcoded generator | Plugin-based generation (extensible) |

---

## Credits

This project proudly uses the [Debezium](https://github.com/debezium/debezium) project (v3.7.0-SNAPSHOT) as its foundation. Debezium provides the core CDC engine, connectors for major databases (MySQL, PostgreSQL, MongoDB, Oracle, etc.), and the Pipeline API.

Thanks to the Debezium community for creating a truly excellent CDC platform that enables us to build Debezium AI.

---

## License

Apache License 2.0 — see [LICENSE.txt](./debezium-main/LICENSE.txt) and [LICENSE-3rd-PARTIES.txt](./debezium-main/LICENSE-3rd-PARTIES.txt).

---

Built on [Debezium](https://debezium.io/) — the open-source Change Data Capture platform.
