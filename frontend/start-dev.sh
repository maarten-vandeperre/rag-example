#!/bin/bash
set -euo pipefail

printf '=== Starting RAG Frontend in Development Mode ===\n'

if ! curl -fsS http://localhost:8081/q/health >/dev/null 2>&1; then
  printf 'WARNING: Backend is not running at http://localhost:8081\n'
  printf 'Start it with ./gradlew :backend:dev\n\n'
fi

if ! curl -fsS http://localhost:8180/health/ready >/dev/null 2>&1; then
  printf 'WARNING: Keycloak is not running at http://localhost:8180\n'
  printf 'Start development services first: ./start-dev-services.sh\n\n'
fi

if [ ! -d node_modules ]; then
  npm install
fi

NODE_ENV=development BROWSER=true npm start
