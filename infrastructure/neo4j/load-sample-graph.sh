#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NEO4J_URI="${NEO4J_URI:-bolt://localhost:7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-dev-password}"
DATA_FILE="${SCRIPT_DIR}/sample-dev-data.cypher"
NEO4J_CONTAINER="${NEO4J_CONTAINER:-rag-neo4j-dev}"

run_cypher_shell() {
  if command -v cypher-shell >/dev/null 2>&1; then
    cypher-shell -a "${NEO4J_URI}" -u "${NEO4J_USER}" -p "${NEO4J_PASSWORD}" "$@"
    return
  fi

  if command -v podman >/dev/null 2>&1; then
    podman exec -i "${NEO4J_CONTAINER}" cypher-shell -u "${NEO4J_USER}" -p "${NEO4J_PASSWORD}" "$@"
    return
  fi

  if command -v docker >/dev/null 2>&1; then
    docker exec -i "${NEO4J_CONTAINER}" cypher-shell -u "${NEO4J_USER}" -p "${NEO4J_PASSWORD}" "$@"
    return
  fi

  printf 'ERROR: cypher-shell is not available locally and no container runtime was found for fallback.\n'
  exit 1
}

run_cypher_shell -f "${DATA_FILE}"
printf 'Neo4j sample graph loaded.\n'
