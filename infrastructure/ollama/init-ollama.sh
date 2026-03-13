#!/bin/sh
set -eu

OLLAMA_URL="${OLLAMA_URL:-http://ollama:11434}"
OLLAMA_PULL_MODELS="${OLLAMA_PULL_MODELS:-tinyllama}"

echo "Waiting for Ollama service to be ready..."
until curl -fsS "${OLLAMA_URL}/api/tags" >/dev/null; do
  sleep 5
done

OLD_IFS=${IFS}
IFS=,
set -- ${OLLAMA_PULL_MODELS}
IFS=${OLD_IFS}

for model in "$@"; do
  model=$(printf '%s' "${model}" | tr -d '[:space:]')
  if [ -z "${model}" ]; then
    continue
  fi

  echo "Pulling model ${model}..."
  curl -fsS "${OLLAMA_URL}/api/pull" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${model}\"}" >/tmp/ollama-pull-response.json
done

echo "Models pulled successfully"
