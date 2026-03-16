#!/bin/bash
set -euo pipefail

NEO4J_HTTP_URL="${NEO4J_HTTP_URL:-http://localhost:7474}"
NEO4J_URI="${NEO4J_URI:-bolt://localhost:7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-dev-password}"
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

  return 1
}

printf '=== Neo4j Troubleshooting ===\n'
printf 'HTTP endpoint: %s\n' "${NEO4J_HTTP_URL}"
printf 'Bolt endpoint: %s\n' "${NEO4J_URI}"

if curl -fsS "${NEO4J_HTTP_URL}" >/dev/null 2>&1; then
  printf 'Neo4j Browser endpoint reachable\n'
else
  printf 'Neo4j Browser endpoint unreachable\n'
fi

if run_cypher_shell 'SHOW DATABASES' >/dev/null 2>&1; then
  printf 'Bolt authentication successful\n'
else
  printf 'Bolt authentication failed or container runtime unavailable\n'
fi
