#!/bin/bash
# ===============================================================
# Debezium AI — One-Click Setup & Configuration Wizard
# ===============================================================
# This script automates the installation and configuration of
# Debezium AI across on-premise and cloud environments.
# ===============================================================

set -e

VERSION="4.0.1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  Debezium AI v${VERSION} — Setup Wizard${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# -----------------------------------------------
# Detect OS and Prerequisites
# -----------------------------------------------
detect_os() {
    echo -e "${YELLOW}[1/8] Detecting environment...${NC}"
    case "$(uname -s)" in
        Linux*)     OS=linux;;
        Darwin*)    OS=macos;;
        CYGWIN*|MINGW*|MSYS*) OS=windows;;
        *)          OS=unknown;;
    esac
    echo "  OS: $OS"

    # Check Java
    if command -v java &>/dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        echo -e "  Java: ${GREEN}found (version $JAVA_VER)${NC}"
        if [ "$JAVA_VER" -lt 21 ]; then
            echo -e "  ${RED}Error: Java 21+ required${NC}"
            exit 1
        fi
    else
        echo -e "  ${YELLOW}Java not found. Will install Java 21.${NC}"
        INSTALL_JAVA=true
    fi

    # Check Maven
    if command -v mvn &>/dev/null; then
        echo -e "  Maven: ${GREEN}found${NC}"
    else
        echo -e "  ${YELLOW}Maven not found. Will use Maven wrapper.${NC}"
    fi

    # Check Docker
    if command -v docker &>/dev/null; then
        echo -e "  Docker: ${GREEN}found${NC}"
        HAS_DOCKER=true
    else
        echo -e "  Docker: ${YELLOW}not found (optional)${NC}"
    fi
}

# -----------------------------------------------
# Choose Installation Mode
# -----------------------------------------------
choose_mode() {
    echo ""
    echo -e "${YELLOW}[2/8] Choose installation mode:${NC}"
    echo "  1) Developer — Local development setup"
    echo "  2) Production — Production deployment"
    echo "  3) Docker — Docker-based deployment"
    echo "  4) Kubernetes — Kubernetes cluster deployment"
    echo "  5) Cloud — Deploy to Azure/AWS/GCP"
    read -p "  Enter choice [1-5] (default: 1): " MODE
    MODE=${MODE:-1}
    echo "  Selected mode: $MODE"
}

# -----------------------------------------------
# Configure Database
# -----------------------------------------------
configure_database() {
    echo ""
    echo -e "${YELLOW}[3/8] Configure storage backend:${NC}"
    echo "  1) PostgreSQL (recommended for production)"
    echo "  2) MongoDB (recommended for NoSQL)"
    echo "  3) In-Memory (development only)"
    echo "  4) Azure Cosmos DB"
    echo "  5) DynamoDB (AWS)"
    read -p "  Enter choice [1-5] (default: 1): " DB_CHOICE
    DB_CHOICE=${DB_CHOICE:-1}

    case $DB_CHOICE in
        1|postgres)
            STORE_TYPE="postgresql"
            read -p "  PostgreSQL host [localhost]: " PG_HOST
            PG_HOST=${PG_HOST:-localhost}
            read -p "  PostgreSQL port [5432]: " PG_PORT
            PG_PORT=${PG_PORT:-5432}
            read -p "  PostgreSQL database [debezium_ai]: " PG_DB
            PG_DB=${PG_DB:-debezium_ai}
            read -p "  PostgreSQL username [debezium]: " PG_USER
            PG_USER=${PG_USER:-debezium}
            read -s -p "  PostgreSQL password: " PG_PASS
            echo ""
            ;;
        2|mongodb)
            STORE_TYPE="mongodb"
            read -p "  MongoDB URI [mongodb://localhost:27017]: " MONGO_URI
            MONGO_URI=${MONGO_URI:-mongodb://localhost:27017}
            read -p "  MongoDB database [debezium_ai]: " MONGO_DB
            MONGO_DB=${MONGO_DB:-debezium_ai}
            ;;
        3|memory)
            STORE_TYPE="memory"
            echo "  Using in-memory storage (data will not persist)"
            ;;
        4|cosmos)
            STORE_TYPE="cosmos"
            read -p "  Cosmos DB URI: " COSMOS_URI
            read -p "  Cosmos DB key: " COSMOS_KEY
            ;;
        5|dynamodb)
            STORE_TYPE="dynamodb"
            read -p "  AWS Region [us-east-1]: " AWS_REGION
            AWS_REGION=${AWS_REGION:-us-east-1}
            read -p "  AWS Access Key ID: " AWS_KEY
            read -s -p "  AWS Secret Key: " AWS_SECRET
            echo ""
            ;;
    esac
}

# -----------------------------------------------
# Configure SSO / Authentication
# -----------------------------------------------
configure_auth() {
    echo ""
    echo -e "${YELLOW}[4/8] Configure authentication:${NC}"
    echo "  1) Local username/password (default)"
    echo "  2) Keycloak / OIDC"
    echo "  3) Google SSO"
    echo "  4) GitHub SSO"
    echo "  5) Azure AD"
    read -p "  Enter choice [1-5] (default: 1): " AUTH_CHOICE
    AUTH_CHOICE=${AUTH_CHOICE:-1}

    case $AUTH_CHOICE in
        1) AUTH_TYPE="local";;
        2)
            AUTH_TYPE="oidc"
            read -p "  Keycloak issuer URL: " OIDC_ISSUER
            read -p "  Client ID: " OIDC_CLIENT_ID
            read -p "  Client Secret: " OIDC_CLIENT_SECRET
            ;;
        3)
            AUTH_TYPE="google"
            read -p "  Google Client ID: " GOOGLE_CLIENT_ID
            read -p "  Google Client Secret: " GOOGLE_CLIENT_SECRET
            ;;
        4)
            AUTH_TYPE="github"
            read -p "  GitHub Client ID: " GITHUB_CLIENT_ID
            read -p "  GitHub Client Secret: " GITHUB_CLIENT_SECRET
            ;;
        5)
            AUTH_TYPE="azure"
            read -p "  Azure AD Tenant ID: " AZURE_TENANT_ID
            read -p "  Azure AD Client ID: " AZURE_CLIENT_ID
            read -p "  Azure AD Client Secret: " AZURE_CLIENT_SECRET
            ;;
    esac
}

# -----------------------------------------------
# Configure Deployment Target
# -----------------------------------------------
configure_deployment() {
    echo ""
    echo -e "${YELLOW}[5/8] Configure deployment target:${NC}"
    echo "  1) Local / On-Premise"
    echo "  2) Docker Compose"
    echo "  3) Kubernetes (Minikube / K8s)"
    echo "  4) Azure Kubernetes Service (AKS)"
    echo "  5) Amazon EKS"
    echo "  6) Google GKE"
    read -p "  Enter choice [1-6] (default: 1): " DEPLOY_CHOICE
    DEPLOY_CHOICE=${DEPLOY_CHOICE:-1}

    case $DEPLOY_CHOICE in
        1) DEPLOY_TARGET="local";;
        2) DEPLOY_TARGET="docker";;
        3) DEPLOY_TARGET="kubernetes";;
        4) DEPLOY_TARGET="aks";;
        5) DEPLOY_TARGET="eks";;
        6) DEPLOY_TARGET="gke";;
    esac
}

# -----------------------------------------------
# Configure AI / LLM
# -----------------------------------------------
configure_ai() {
    echo ""
    echo -e "${YELLOW}[6/8] Configure AI/ML capabilities:${NC}"
    echo "  1) Local MiniLM (default, no external dependencies)"
    echo "  2) Ollama (local LLM)"
    echo "  3) OpenAI API"
    echo "  4) Disable AI features"
    read -p "  Enter choice [1-4] (default: 1): " AI_CHOICE
    AI_CHOICE=${AI_CHOICE:-1}

    case $AI_CHOICE in
        1)
            AI_PROVIDER="minilm"
            echo "  Using local MiniLM embeddings"
            ;;
        2)
            AI_PROVIDER="ollama"
            read -p "  Ollama URL [http://localhost:11434]: " OLLAMA_URL
            OLLAMA_URL=${OLLAMA_URL:-http://localhost:11434}
            read-p "  Ollama model [llama3.1]: " OLLAMA_MODEL
            OLLAMA_MODEL=${OLLAMA_MODEL:-llama3.1}
            ;;
        3)
            AI_PROVIDER="openai"
            read -p "  OpenAI API Key: " OPENAI_KEY
            read -p "  OpenAI Model [gpt-4]: " OPENAI_MODEL
            OPENAI_MODEL=${OPENAI_MODEL:-gpt-4}
            ;;
        4)
            AI_PROVIDER="none"
            echo "  AI features disabled"
            ;;
    esac
}

# -----------------------------------------------
# Configure Admin User
# -----------------------------------------------
configure_admin() {
    echo ""
    echo -e "${YELLOW}[7/8] Create admin user:${NC}"
    read -p "  Admin username [admin]: " ADMIN_USER
    ADMIN_USER=${ADMIN_USER:-admin}
    read -s -p "  Admin password: " ADMIN_PASS
    echo ""
    read -s -p "  Confirm password: " ADMIN_PASS2
    echo ""
    if [ "$ADMIN_PASS" != "$ADMIN_PASS2" ]; then
        echo -e "${RED}  Passwords do not match${NC}"
        exit 1
    fi
    read -p "  Admin email [admin@debezium.ai]: " ADMIN_EMAIL
    ADMIN_EMAIL=${ADMIN_EMAIL:-admin@debezium.ai}
}

# -----------------------------------------------
# Generate Configuration
# -----------------------------------------------
generate_config() {
    echo ""
    echo -e "${YELLOW}[8/8] Generating configuration...${NC}"

    CONFIG_FILE="$PROJECT_DIR/application.properties"

    cat > "$CONFIG_FILE" << EOF
# ==========================================
# Debezium AI v${VERSION} — Generated Configuration
# Generated: $(date)
# ==========================================

# Server
quarkus.http.port=8080
quarkus.http.root-path=/

# Storage
debezium.storage.type=${STORE_TYPE:-memory}

# Authentication
debezium.auth.type=${AUTH_TYPE:-local}
debezium.api.key=$(openssl rand -hex 32 2>/dev/null || date +%s | md5sum | head -c 32)

# AI/ML
debezium.ai.embeddings.provider=${AI_PROVIDER:-minilm}

# Deployment
debezium.deployment.target=${DEPLOY_TARGET:-local}

# Monitoring
quarkus.smallrye-health.path=/q/health
quarkus.smallrye-metrics.path=/q/metrics
EOF

    if [ "$STORE_TYPE" = "postgresql" ]; then
        cat >> "$CONFIG_FILE" << EOF

# PostgreSQL Store
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${PG_USER:-debezium}
quarkus.datasource.password=${PG_PASS:-debezium}
quarkus.datasource.jdbc.url=jdbc:postgresql://${PG_HOST:-localhost}:${PG_PORT:-5432}/${PG_DB:-debezium_ai}
EOF
    fi

    if [ "$STORE_TYPE" = "mongodb" ]; then
        cat >> "$CONFIG_FILE" << EOF

# MongoDB Store
debezium.storage.mongodb.uri=${MONGO_URI:-mongodb://localhost:27017}
debezium.storage.mongodb.database=${MONGO_DB:-debezium_ai}
EOF
    fi

    if [ "$AI_PROVIDER" = "ollama" ]; then
        cat >> "$CONFIG_FILE" << EOF

# Ollama LLM
debezium.ai.llm.provider=ollama
debezium.ai.llm.ollama.url=${OLLAMA_URL:-http://localhost:11434}
debezium.ai.llm.ollama.model=${OLLAMA_MODEL:-llama3.1}
EOF
    fi

    if [ "$AI_PROVIDER" = "openai" ]; then
        cat >> "$CONFIG_FILE" << EOF

# OpenAI LLM
debezium.ai.llm.provider=openai
debezium.ai.llm.openai.key=${OPENAI_KEY}
debezium.ai.llm.openai.model=${OPENAI_MODEL:-gpt-4}
EOF
    fi

    echo -e "${GREEN}  Configuration generated: $CONFIG_FILE${NC}"
    echo -e "${GREEN}  API Key: $(grep debezium.api.key $CONFIG_FILE | cut -d= -f2)${NC}"
}

# -----------------------------------------------
# Build and Start
# -----------------------------------------------
build_and_start() {
    echo ""
    echo -e "${YELLOW}Building Debezium AI v${VERSION}...${NC}"

    cd "$PROJECT_DIR"

    if [ -f "./mvnw" ]; then
        MVN_CMD="./mvnw"
    else
        MVN_CMD="mvn"
    fi

    echo "  Running: $MVN_CMD clean install -Dquick -DskipITs"
    $MVN_CMD clean install -Dquick -DskipITs

    echo ""
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}  Debezium AI v${VERSION} Setup Complete!${NC}"
    echo -e "${GREEN}================================================${NC}"
    echo ""
    echo "  Configuration: $CONFIG_FILE"
    echo ""
    echo "  Start commands:"
    echo "    cd $PROJECT_DIR"
    echo "    ./mvnw quarkus:dev"
    echo ""
    echo "  Access the application:"
    echo "    http://localhost:8080/v4"
    echo "    API Docs: http://localhost:8080/v4/openapi"
    echo "    Health:   http://localhost:8080/q/health"
    echo ""
    echo -e "${YELLOW}  Default login: admin / <your-password>${NC}"
    echo ""
}

# -----------------------------------------------
# Main
# -----------------------------------------------
main() {
    detect_os
    choose_mode
    configure_database
    configure_auth
    configure_deployment
    configure_ai
    configure_admin
    generate_config
    build_and_start
}

main "$@"
