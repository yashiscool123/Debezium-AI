# Debezium AI — User Guide

## Welcome to Debezium AI

Debezium AI helps you create, manage, and monitor Change Data Capture (CDC) pipelines using AI-powered schema mapping. This guide walks you through everything from initial setup to advanced usage.

---

## Getting Started

### Accessing the Application

Once deployed, access the application at:

- **Web UI:** `http://localhost:8080/v4`
- **API Docs:** `http://localhost:8080/v4/openapi`
- **Health Check:** `http://localhost:8080/q/health`

### Default Credentials

| Role | Username | Password |
|---|---|---|
| Super Admin | `admin` | Set during installation |

> Change the default password on first login.

### Quick Start Tutorial

**Step 1: Login**

```bash
curl -X POST http://localhost:8080/v4/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your-password"}'
```

You'll receive a session token. Use it as a Bearer token for subsequent API calls.

**Step 2: Create a Pipeline**

```bash
curl -X POST http://localhost:8080/v4/pipelines \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <session-token>" \
  -d '{
    "name": "My First Pipeline",
    "description": "CDC from MySQL to Kafka",
    "source": {
      "connector": {
        "type": "mysql",
        "host": "localhost",
        "port": 3306,
        "user": "root",
        "password": "secret"
      }
    },
    "target": {
      "connector": {
        "type": "kafka",
        "bootstrapServers": "localhost:9092"
      }
    }
  }'
```

**Step 3: Introspect Schema**

```bash
curl -X POST http://localhost:8080/v4/schema/introspect \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <session-token>" \
  -d '{ "connection": { "type": "mysql", "host": "localhost", "user": "root", "password": "secret" } }'
```

**Step 4: Get AI Mapping Suggestions**

```bash
curl -X POST http://localhost:8080/v4/mappings/suggest \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <session-token>" \
  -d '{
    "sourceTables": [{"schema": "public", "table": "orders", "columns": [...]}],
    "targetTables": [{"schema": "public", "table": "order_events", "columns": [...]}]
  }'
```

**Step 5: Deploy**

```bash
curl -X POST http://localhost:8080/v4/pipelines/{id}/deploy \
  -H "Authorization: Bearer <session-token>"
```

**Step 6: Generate Deployment Artifacts**

```bash
curl -X POST "http://localhost:8080/v4/deployments/generate?format=strimzi" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <session-token>" \
  -d '{ "id": "pipeline-id", "name": "my-pipeline" }'
```

---

## User Interface

### Dashboard

The main dashboard shows:
- **Pipeline Status** — Overview of all pipelines and their current state
- **Recent Activity** — Latest pipeline runs and events
- **System Health** — Service status and resource usage
- **Quick Actions** — Create, deploy, or monitor pipelines

### Pipeline Builder

The pipeline builder guides you through creating a new pipeline:

1. **Basic Info** — Name, description, version, tags
2. **Source Configuration** — Database type, connection details, schema selection
3. **Target Configuration** — Sink type, topic configuration
4. **Schema Mapping** — AI-suggested table/column mappings (accept, reject, or modify)
5. **Transformations** — Add SMT transforms, KSQL or Flink SQL
6. **Deployment Settings** — Platform, namespace, resources, scaling
7. **Monitoring** — Alert rules, dashboards, health checks
8. **Review & Deploy** — Review all settings and deploy

### Mapping Designer

The mapping designer provides:
- **Visual drag-and-drop** — Map source columns to target columns
- **AI Suggestions** — Get smart mapping recommendations
- **Confidence Scores** — See how confident the AI is about each mapping
- **Bulk Accept/Reject** — Approve or reject all suggestions at once
- **Custom Mappings** — Define transformations between columns

### Monitoring Dashboards

Real-time monitoring with:
- **Pipeline Metrics** — Event rates, latency, throughput
- **Alert Rules** — Configure thresholds and notification channels
- **Health Checks** — System component status
- **Audit Log** — Complete action history

---

## Feature Guides

### Role-Based Access Control

| Role | Description |
|---|---|
| **Super Admin** | Full system access including tenant management |
| **Admin** | Full access within their tenant |
| **Pipeline Manager** | Create, edit, deploy, and delete pipelines |
| **Pipeline Operator** | Start, stop, and monitor pipelines |
| **Pipeline Viewer** | View-only access |
| **Connector Admin** | Manage connector configurations |
| **Data Engineer** | AI mapping, schema introspection, transforms |
| **Auditor** | Read-only audit log access |
| **Developer** | API access for integrations |

### AI Mapping

The AI mapping engine considers:
- Column name similarity (Levenshtein distance)
- Data type compatibility (numeric, string, temporal, boolean)
- Primary key alignment
- Nullable field matching
- Semantic similarity (via embeddings)
- LLM suggestions (via Ollama or OpenAI)

**Tips for better mapping results:**
- Use descriptive column names (e.g., `customer_email` instead of `ce`)
- Ensure your target schema has a similar structure
- Provide explicit mappings for critical columns
- Review and accept/reject AI suggestions to improve future results

### Pipeline Export/Import

Export pipelines for environment promotion:

```bash
# Export specific pipelines
curl -X POST http://localhost:8080/v4/export/pipelines \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "pipelineIds": ["id1", "id2"],
    "sourceEnvironment": "DEV",
    "targetEnvironment": "QA"
  }'

# Create a release
curl -X POST http://localhost:8080/v4/export/release \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "pipelineIds": ["id1", "id2"],
    "version": "1.0.0",
    "releaseNotes": "Initial release"
  }'

# Import
curl -X POST http://localhost:8080/v4/export/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "json": "{...exported JSON...}",
    "targetEnvironment": "PROD"
  }'
```

### SSO / Authentication

To configure SSO:

1. **Keycloak / OIDC** — Set up in `application.properties`:
```properties
debezium.auth.type=oidc
debezium.auth.oidc.issuer=https://keycloak.example.com/auth/realms/debezium
debezium.auth.oidc.client-id=debezium-ai
debezium.auth.oidc.client-secret=your-secret
```

2. **Google SSO** — Configure OAuth 2.0 credentials in Google Cloud Console

3. **GitHub SSO** — Register OAuth app in GitHub Developer Settings

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|---|---|
| Connection refused | Check if the service is running on the expected port |
| Authentication failed | Verify your API key or session token |
| Pipeline validation fails | Check connector configuration and database connectivity |
| AI mapping returns empty | Ensure source/target schemas are introspected first |
| Deployment fails | Check Kubernetes/Docker permissions and connectivity |

### Logs

```bash
# Application logs
docker logs debezium-ai

# Kafka Connect logs
docker logs debezium-ai-connect

# Query audit log via API
curl -H "Authorization: Bearer <token>" http://localhost:8080/v4/audit
```

---

## Best Practices

1. **Use descriptive names** for pipelines, connectors, and mappings
2. **Test in DEV first** before promoting to QA/PROD
3. **Review AI mappings** before deploying to production
4. **Set up monitoring alerts** for pipeline failures
5. **Export configurations** regularly for backup
6. **Use RBAC** to limit access to sensitive pipelines
7. **Enable audit logging** for compliance requirements
8. **Regularly update** the Debezium core version

---

## API Reference

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v4/auth/login` | Username/password login |
| POST | `/v4/auth/logout` | Logout |
| GET | `/v4/auth/session` | Validate current session |
| POST | `/v4/auth/sso/{provider}` | SSO login |
| POST | `/v4/auth/register` | Register new user |

### Pipelines

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v4/pipelines` | Create pipeline |
| GET | `/v4/pipelines` | List all pipelines |
| GET | `/v4/pipelines/{id}` | Get pipeline by ID |
| PUT | `/v4/pipelines/{id}` | Update pipeline |
| DELETE | `/v4/pipelines/{id}` | Delete pipeline |
| POST | `/v4/pipelines/{id}/deploy` | Deploy pipeline |
| POST | `/v4/pipelines/{id}/duplicate` | Duplicate pipeline |
| POST | `/v4/pipelines/validate` | Validate pipeline config |

### AI Mapping

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v4/mappings/suggest` | AI-suggested mappings |
| POST | `/v4/mappings/embed` | Generate embeddings |
| POST | `/v4/mappings/similarity` | Text similarity |

### Export/Import

| Method | Endpoint | Description |
|---|---|---|
| POST | `/v4/export/pipelines` | Export pipelines |
| POST | `/v4/export/release` | Create release package |
| POST | `/v4/export/environment` | Export all pipelines for environment |
| POST | `/v4/export/import` | Import pipelines |

### Connectors & Deployments

| Method | Endpoint | Description |
|---|---|---|
| GET | `/v4/connectors` | List available connectors |
| GET | `/v4/connectors/{name}` | Connector details |
| POST | `/v4/deployments/generate` | Generate deployment artifacts |
| GET | `/v4/deployments/formats` | List deployment formats |

### Monitoring

| Method | Endpoint | Description |
|---|---|---|
| GET | `/v4/metrics` | All pipeline metrics |
| GET | `/v4/metrics/summary` | Metric summary |
| POST | `/v4/metrics/alerts` | Evaluate alert rules |

### Users & Roles

| Method | Endpoint | Description |
|---|---|---|
| GET | `/v4/users` | List all users |
| GET | `/v4/users/roles` | List all roles |
| GET | `/v4/users/permissions` | List all permissions |
| POST | `/v4/users` | Create user |
| PUT | `/v4/users/{username}` | Update user |
| DELETE | `/v4/users/{username}` | Delete user |

---

## Support

For issues and feature requests, please visit:
- **GitHub Issues:** https://github.com/yashiscool123/Debezium-AI/issues
- **Documentation:** https://github.com/yashiscool123/Debezium-AI
