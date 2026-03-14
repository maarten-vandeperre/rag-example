#!/bin/bash
set -euo pipefail

printf '=== RAG Application Development Environment Setup ===\n'

JAVA_CMD="java"
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_CMD="${JAVA_HOME}/bin/java"
fi

if command -v "$JAVA_CMD" >/dev/null 2>&1; then
  JAVA_VERSION=$($JAVA_CMD -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
  printf 'Java version detected: %s\n' "$JAVA_VERSION"
  if [ "$JAVA_VERSION" != "17" ]; then
    printf 'ERROR: Java 17 is required, but found Java %s\n' "$JAVA_VERSION"
    exit 1
  fi
else
  printf 'ERROR: Java not found. Please install Java 17.\n'
  exit 1
fi

if command -v node >/dev/null 2>&1; then
  NODE_VERSION=$(node --version | cut -d 'v' -f 2 | cut -d '.' -f 1)
  printf 'Node.js version detected: %s\n' "$NODE_VERSION"
  if [ "$NODE_VERSION" -lt "18" ]; then
    printf 'ERROR: Node.js 18+ is required, but found Node.js %s\n' "$NODE_VERSION"
    exit 1
  fi
else
  printf 'ERROR: Node.js not found. Please install Node.js 18+.\n'
  exit 1
fi

chmod +x gradlew

printf 'Running environment health check...\n'
JAVA_HOME="${JAVA_HOME:-}" ./gradlew healthCheck

printf 'Installing frontend dependencies...\n'
JAVA_HOME="${JAVA_HOME:-}" ./gradlew :frontend:npmInstall

printf '=== Setup Complete ===\n'
printf 'You can now run:\n'
printf '  ./gradlew dev            - Start development environment\n'
printf '  ./gradlew testAll        - Run all tests\n'
printf '  ./gradlew buildWorkspace - Build verified release outputs\n'
