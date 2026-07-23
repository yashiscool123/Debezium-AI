# Debezium AI — Deployment Guide

This guide covers production deployment of Debezium AI across on-premise and cloud environments including Azure, AWS, and GCP.

---

## Table of Contents

- [Quick Reference](#quick-reference)
- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [On-Premise Deployment](#on-premise-deployment)
- [Docker Compose Deployment](#docker-compose-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Azure Deployment](#azure-deployment)
- [AWS Deployment](#aws-deployment)
- [GCP Deployment](#gcp-deployment)
- [Multi-Region Deployment](#multi-region-deployment)
- [High Availability](#high-availability)
- [Scaling](#scaling)
- [Security Hardening](#security-hardening)
- [Backup and Recovery](#backup-and-recovery)
- [Monitoring & Alerting](#monitoring--alerting)
- [Troubleshooting](#troubleshooting)

---

## Quick Reference

| Deployment Type | Best For | Time to Deploy | Complexity |
|---|---|---|---|
| Local / Dev | Development | 5 min | Low |
| Docker Compose | Staging / Small Production | 10 min | Low |
| Kubernetes | Production | 30 min | Medium |
| Azure AKS | Enterprise Cloud | 45 min | Medium-High |
| AWS EKS | Enterprise Cloud | 45 min | Medium-High |
| GCP GKE | Enterprise Cloud | 45 min | Medium-High |

---

## Architecture Overview

```
                         ┌─────────────────┐
                         │   Load Balancer  │
                         │  (HAProxy / ALB) │
                         └────────┬────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
              ┌─────┴─────┐ ┌─────┴─────┐ ┌─────┴─────┐
              │ Debezium  │ │ Debezium  │ │ Debezium  │  (Horizontal Scaling)
              │ AI Node 1 │ │ AI Node 2 │ │ AI Node N │
              └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
                    │             │             │
                    └─────────────┼─────────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
              ┌─────┴─────┐ ┌─────┴─────┐ ┌─────┴─────┐
              │ PostgreSQL │ │  MongoDB  │ │    Redis   │
              │ (Metadata) │ │ (History) │ │  (Cache)   │
              └───────────┘ └───────────┘ └───────────┘
                    │             │             │
                    └─────────────┼─────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │     Kafka / Kafka Connect  │
                    │     (CDC Event Streaming)  │
                    └───────────────────────────┘
```

---

## Prerequisites

### Required
- Java 21 JDK
- Docker and Docker Compose (for containerized deployment)
- Kubernetes cluster (for K8s deployment)
- kubectl CLI configured (for K8s deployment)
- Helm v3+ (for Helm-based installation)

### Recommended for Production
- PostgreSQL 16+ (metadata storage)
- MongoDB 7+ (job history and configurations)
- Kafka 3.6+ / Confluent Platform 7.6+ (CDC streaming)
- Prometheus + Grafana (monitoring)
- Redis (caching and rate limiting)
- Vault / AWS Secrets Manager (secrets management)
- Cert-Manager (TLS certificates)

---

## On-Premise Deployment

### Manual Installation

```bash
# 1. Build the project
cd debezium-pipeline-v4
./mvnw clean install -DskipITs

# 2. Configure
cp application.properties application-production.properties
# Edit production properties with your settings

# 3. Run
java -jar api/target/debezium-pipeline-v4-api-4.0.1-runner.jar \
  -Dquarkus.http.port=8080 \
  -Dquarkus.profile=prod
```

### As a Systemd Service

```ini
[Unit]
Description=Debezium AI
After=network.target postgresql.service

[Service]
Type=simple
User=debezium
Group=debezium
WorkingDirectory=/opt/debezium-ai
ExecStart=/usr/bin/java -Xms512m -Xmx4g -jar /opt/debezium-ai/api/target/debezium-pipeline-v4-api-4.0.1-runner.jar
Restart=on-failure
RestartSec=10
EnvironmentFile=/opt/debezium-ai/application-production.properties

[Install]
WantedBy=multi-user.target
```

---

## Docker Compose Deployment

### Quick Start

```bash
cd debezium-pipeline-v4/installer

# Create .env file (copy from .env.example)
cp .env.example .env
# Edit .env with your settings

# Start all services
docker compose up -d

# Start with production profile
docker compose --profile production up -d

# Start with monitoring
docker compose --profile full up -d
```

### Verify Deployment

```bash
# Check all services
docker compose ps

# Check application health
curl http://localhost:8080/q/health

# Check logs
docker compose logs -f debezium-ai
```

---

## Kubernetes Deployment

### Helm Chart Installation

```bash
# Add Debezium AI Helm repository
helm repo add debezium-ai https://yashiscool123.github.io/Debezium-AI/charts
helm repo update

# Install with default values
helm install debezium-ai debezium-ai/debezium-ai

# Install with custom values
helm install debezium-ai debezium-ai/debezium-ai \
  --namespace debezium-ai \
  --create-namespace \
  --set ingress.enabled=true \
  --set ingress.host=debezium-ai.example.com \
  --set storage.type=postgresql \
  --set auth.type=oidc
```

### Manual Kubernetes Deployment

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: debezium-ai
---
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: debezium-ai
  namespace: debezium-ai
spec:
  replicas: 3
  selector:
    matchLabels:
      app: debezium-ai
  template:
    metadata:
      labels:
        app: debezium-ai
    spec:
      containers:
      - name: debezium-ai
        image: debezium-ai:4.0.1
        ports:
        - containerPort: 8080
        env:
        - name: DEBEZIUM_API_KEY
          valueFrom:
            secretKeyRef:
              name: debezium-ai-secrets
              key: api-key
        resources:
          requests:
            cpu: "500m"
            memory: "1Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
---
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: debezium-ai
  namespace: debezium-ai
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: debezium-ai
```

Apply with:
```bash
kubectl apply -f namespace.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

---

## Azure Deployment

### Azure Kubernetes Service (AKS)

```bash
# 1. Login to Azure
az login

# 2. Create resource group
az group create --name debezium-ai-rg --location eastus

# 3. Create AKS cluster
az aks create \
  --resource-group debezium-ai-rg \
  --name debezium-ai-cluster \
  --node-count 3 \
  --enable-addons monitoring \
  --generate-ssh-keys

# 4. Get credentials
az aks get-credentials --resource-group debezium-ai-rg --name debezium-ai-cluster

# 5. Deploy with Helm
helm install debezium-ai ./charts/debezium-ai \
  --set ingress.enabled=true \
  --set ingress.className=azure-application-gateway \
  --set storage.type=azure-cosmos

# 6. Configure Azure Database for PostgreSQL
az postgres flexible-server create \
  --name debezium-ai-pg \
  --resource-group debezium-ai-rg \
  --sku-name Standard_D2s_v3
```

### Azure Container Instances

```bash
az container create \
  --resource-group debezium-ai-rg \
  --name debezium-ai \
  --image debezium-ai:4.0.1 \
  --ports 8080 \
  --environment-variables \
    DEBEZIUM_API_KEY=your-key \
    DEBEZIUM_STORAGE_TYPE=postgresql
```

### Azure App Service

```bash
az webapp create \
  --resource-group debezium-ai-rg \
  --plan debezium-ai-plan \
  --name debezium-ai-app \
  --runtime "JAVA|21-java21"
```

---

## AWS Deployment

### Amazon EKS (Elastic Kubernetes Service)

```bash
# 1. Install eksctl and configure AWS CLI
aws configure

# 2. Create EKS cluster
eksctl create cluster \
  --name debezium-ai \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.large \
  --nodes 3 \
  --nodes-min 3 \
  --nodes-max 10

# 3. Deploy with Helm
helm install debezium-ai ./charts/debezium-ai \
  --set ingress.enabled=true \
  --set ingress.className=alb \
  --set storage.type=dynamodb

# 4. Create RDS PostgreSQL
aws rds create-db-instance \
  --db-instance-identifier debezium-ai \
  --db-instance-class db.t3.large \
  --engine postgres \
  --master-username debezium \
  --master-user-password your-password \
  --allocated-storage 100
```

### ECS Fargate

```bash
# Register task definition
aws ecs register-task-definition \
  --family debezium-ai \
  --network-mode awsvpc \
  --task-role-arn arn:aws:iam::account:role/ecsTaskRole \
  --execution-role-arn arn:aws:iam::account:role/ecsTaskExecutionRole \
  --cpu "1024" \
  --memory "2048" \
  --container-definitions '[
    {
      "name": "debezium-ai",
      "image": "debezium-ai:4.0.1",
      "portMappings": [{"containerPort": 8080}],
      "environment": [
        {"name": "DEBEZIUM_API_KEY", "value": "your-key"}
      ]
    }
  ]'
```

### Elastic Beanstalk

```bash
# Initialize EB application
eb init debezium-ai --platform java-21

# Create environment
eb create debezium-ai-prod \
  --instance-type t3.large \
  --envvars DEBEZIUM_API_KEY=your-key
```

---

## GCP Deployment

### Google Kubernetes Engine (GKE)

```bash
# 1. Authenticate with GCP
gcloud auth login
gcloud config set project your-project-id

# 2. Create GKE cluster
gcloud container clusters create debezium-ai \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type e2-standard-2 \
  --enable-autoscaling \
  --min-nodes 3 \
  --max-nodes 10

# 3. Get credentials
gcloud container clusters get-credentials debezium-ai --zone us-central1-a

# 4. Deploy
helm install debezium-ai ./charts/debezium-ai \
  --set ingress.enabled=true \
  --set ingress.className=gce \
  --set storage.type=cloud-sql

# 5. Create Cloud SQL PostgreSQL
gcloud sql instances create debezium-ai-db \
  --database-version POSTGRES_16 \
  --cpu 4 \
  --memory 16GB \
  --region us-central1
```

### Cloud Run

```bash
# Build container
gcloud builds submit --tag gcr.io/your-project/debezium-ai:4.0.1

# Deploy to Cloud Run
gcloud run deploy debezium-ai \
  --image gcr.io/your-project/debezium-ai:4.0.1 \
  --platform managed \
  --region us-central1 \
  --memory 4Gi \
  --cpu 2 \
  --concurrency 80 \
  --timeout 3600 \
  --set-env-vars "DEBEZIUM_API_KEY=your-key"
```

### Compute Engine

```bash
# Create VM instance
gcloud compute instances create debezium-ai-vm \
  --zone us-central1-a \
  --machine-type e2-standard-4 \
  --image-family ubuntu-2204-lts \
  --image-project ubuntu-os-cloud \
  --tags debezium-ai

# SSH and deploy
gcloud compute ssh debezium-ai-vm
# Follow manual installation steps on the VM
```

---

## Multi-Region Deployment

### Active-Active Configuration

For global deployments with active-active architecture:

```yaml
# regions-config.yaml
regions:
  - name: us-east
    replicas: 2
    storage: postgresql-us-east
    kafka-cluster: kafka-us-east
  - name: eu-west
    replicas: 2
    storage: postgresql-eu-west
    kafka-cluster: kafka-eu-west
  - name: ap-southeast
    replicas: 2
    storage: postgresql-ap-southeast
    kafka-cluster: kafka-ap-southeast

global:
  dns:
    type: global-accelerator
    ssl: true
  replication:
    type: kafka-mirrormaker
    interval: 100ms
```

---

## High Availability

### Database HA

**PostgreSQL:**
- Use Patroni for automatic failover
- Streaming replication with 2+ standby nodes
- Automated backups with WAL archiving

```yaml
# patroni-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: patroni-config
data:
  POSTGRES_REPLICATION_MODE: "master"
  POSTGRES_REPLICATION_USER: "replicator"
  POSTGRES_REPLICATION_PASSWORD: "replication-password"
```

### Application HA

- Deploy 3+ replicas (odd number for quorum)
- Use pod anti-affinity to spread across nodes
- Configure pod disruption budgets

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: debezium-ai-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: debezium-ai
```

---

## Scaling

### Vertical Scaling

Increase resources per pod/container:
```yaml
resources:
  requests:
    cpu: "2"
    memory: "4Gi"
  limits:
    cpu: "4"
    memory: "8Gi"
```

### Horizontal Scaling

```bash
# Scale Kubernetes deployment
kubectl scale deployment debezium-ai --replicas=6

# Enable HPA
kubectl autoscale deployment debezium-ai \
  --cpu-percent=70 \
  --min=3 \
  --max=10
```

---

## Security Hardening

### Network Security

```bash
# Restrict to internal VPC
kubectl annotate service debezium-ai \
  service.beta.kubernetes.io/azure-load-balancer-internal="true"

# Network policies
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: debezium-ai-network-policy
spec:
  podSelector:
    matchLabels:
      app: debezium-ai
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
EOF
```

### Secrets Management

```bash
# Kubernetes Secrets
kubectl create secret generic debezium-ai-secrets \
  --from-literal=api-key=your-secure-key \
  --from-literal=db-password=your-db-password

# Azure Key Vault
az keyvault secret set --vault-name debezium-ai-kv --name "api-key" --value "your-key"

# AWS Secrets Manager
aws secretsmanager create-secret --name debezium-ai/api-key --secret-string "your-key"
```

### TLS Configuration

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.0/cert-manager.yaml

# Create certificate
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: debezium-ai-tls
spec:
  secretName: debezium-ai-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - debezium-ai.yourdomain.com
EOF
```

---

## Backup and Recovery

### Automated Backup Script

```bash
#!/bin/bash
# backup.sh — Automated backup for Debezium AI

BACKUP_DIR="/backups/debezium-ai"
DATE=$(date +%Y%m%d-%H%M%S)
RETENTION_DAYS=30

# Backup PostgreSQL
pg_dump -h $DB_HOST -U $DB_USER -d debezium_ai \
  | gzip > "$BACKUP_DIR/postgres-$DATE.sql.gz"

# Backup MongoDB
mongodump --uri="$MONGO_URI" \
  --gzip --archive="$BACKUP_DIR/mongodb-$DATE.gz"

# Backup configurations
curl -X POST "http://localhost:8080/v4/export/environment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d '{"sourceEnvironment": "PROD", "targetEnvironment": "BACKUP"}' \
  > "$BACKUP_DIR/pipelines-$DATE.json"

# Cleanup old backups
find $BACKUP_DIR -type f -mtime +$RETENTION_DAYS -delete

# Upload to cloud (optional)
# aws s3 sync $BACKUP_DIR s3://debezium-ai-backups/
# az storage blob upload-batch -d backups --account-name debeziumai ...
```

### Recovery

```bash
# Restore PostgreSQL
gunzip -c postgres-20250401-120000.sql.gz | psql -h $DB_HOST -U $DB_USER debezium_ai

# Restore MongoDB
mongorestore --uri="$MONGO_URI" --gzip --archive="mongodb-20250401-120000.gz"

# Restore pipelines
curl -X POST "http://localhost:8080/v4/export/import" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d @pipelines-20250401-120000.json
```

---

## Monitoring & Alerting

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
  - static_configs:
    - targets: ['alertmanager:9093']

rule_files:
  - 'alerts.yml'

scrape_configs:
  - job_name: 'debezium-ai'
    metrics_path: '/q/metrics'
    static_configs:
    - targets: ['debezium-ai:8080']
```

### Alert Rules

```yaml
# alerts.yml
groups:
  - name: debezium-ai-alerts
    rules:
    - alert: PipelineFailureRate
      expr: rate(pipeline_errors_total[5m]) > 0.1
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "High pipeline failure rate"

    - alert: PipelineLatencyHigh
      expr: pipeline_latency_seconds > 30
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "Pipeline latency exceeds 30 seconds"

    - alert: ServiceDown
      expr: up{job="debezium-ai"} == 0
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "Debezium AI service is down"

    - alert: HighMemoryUsage
      expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.85
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "JVM memory usage > 85%"
```

### Grafana Dashboard

Import the pre-built dashboard JSON from `installer/grafana-dashboard.json`.

---

## Troubleshooting

### Common Deployment Issues

| Issue | Likely Cause | Solution |
|---|---|---|
| Pod CrashLoopBackOff | Missing environment variables | Check ConfigMap and Secrets |
| Database connection refused | Wrong host/port/credentials | Verify database settings |
| OOMKilled | Insufficient memory | Increase memory limits |
| ImagePullBackOff | Container registry auth | Configure image pull secrets |
| API returns 401 | Invalid API key | Check DEBEZIUM_API_KEY env var |
| SSO not working | Wrong OIDC configuration | Verify issuer, client ID, and secret |

### Debugging

```bash
# Check pod logs
kubectl logs -f deployment/debezium-ai

# Debug with increased logging
kubectl set env deployment/debezium-ai QUARKUS_LOG_LEVEL=DEBUG

# Port forward for local access
kubectl port-forward service/debezium-ai 8080:8080

# Check events
kubectl get events --sort-by='.lastTimestamp'

# Exec into container
kubectl exec -it deployment/debezium-ai -- /bin/sh
```

---

## Rollback

```bash
# Kubernetes rollback
kubectl rollout undo deployment/debezium-ai

# Helm rollback
helm rollback debezium-ai 1

# Docker Compose rollback
docker compose down
docker compose up -d
```

---

## Appendix

### Port Reference

| Port | Service | Protocol |
|---|---|---|
| 8080 | Debezium AI API | HTTP |
| 8083 | Kafka Connect | HTTP |
| 9092 | Kafka | TCP |
| 5432 | PostgreSQL | TCP |
| 27017 | MongoDB | TCP |
| 9090 | Prometheus | HTTP |
| 3000 | Grafana | HTTP |

### Environment Variables Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `DEBEZIUM_API_KEY` | Yes | - | API authentication key |
| `DEBEZIUM_AUTH_TYPE` | No | local | Authentication type (local/oidc/google/github) |
| `DEBEZIUM_STORAGE_TYPE` | No | memory | Storage backend (memory/postgresql/mongodb) |
| `QUARKUS_HTTP_PORT` | No | 8080 | HTTP listen port |
| `JAVA_OPTS` | No | -Xms256m -Xmx1g | JVM options |
| `DB_HOST` | Depends | localhost | Database host |
| `DB_PORT` | Depends | varies | Database port |
| `DB_USER` | Depends | debezium | Database user |
| `DB_PASSWORD` | Depends | - | Database password |

---

*For additional help, visit https://github.com/yashiscool123/Debezium-AI/issues*
