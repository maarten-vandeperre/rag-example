#!/bin/sh
set -eu

OLLAMA_URL="${OLLAMA_URL:-http://localhost:11434}"
OLLAMA_PULL_MODELS="${OLLAMA_PULL_MODELS:-${LLM_MODEL:-tinyllama}}"

model_present() {
  curl -fsS "${OLLAMA_URL}/api/tags" | grep -Eq "\"name\":\"${1}(:[^\"]+)?\""
}

echo "Waiting for Ollama service to be ready at ${OLLAMA_URL}..."
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

  echo "Ensuring model ${model} is available..."
  if model_present "${model}"; then
    echo "Model ${model} already present"
    continue
  fi

  curl -fsS "${OLLAMA_URL}/api/pull" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${model}\",\"stream\":false}" >/tmp/ollama-pull-response.json

  if model_present "${model}"; then
    echo "Model ${model} is ready"
  else
    echo "ERROR: Model ${model} was not found after pull"
    exit 1
  fi
done

echo "Required Ollama models are available"
