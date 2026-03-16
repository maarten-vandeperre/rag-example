#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NEO4J_URI="${NEO4J_URI:-bolt://localhost:7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-dev-password}"
CONSTRAINTS_FILE="${SCRIPT_DIR}/dev-constraints.cypher"
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

printf '=== Initializing Neo4j Development Schema ===\n'
for attempt in $(seq 1 30); do
  if run_cypher_shell 'RETURN 1' >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq 30 ]; then
    printf 'ERROR: Neo4j did not become ready in time.\n'
    exit 1
  fi
  sleep 2
done

run_cypher_shell -f "${CONSTRAINTS_FILE}"
printf 'Neo4j constraints and indexes applied.\n'
