#!/bin/sh
# Debezium AI — root Maven wrapper (delegates to debezium-main/mvnw)
# ---------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$SCRIPT_DIR/debezium-main/mvnw" "$@"