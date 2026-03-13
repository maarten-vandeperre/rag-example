# Create LLM Service container setup

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create LLM service container setup using Ollama for local language model hosting to generate answers from document context.

## Scope

- Create Ollama container configuration for local LLM hosting
- Configure model downloading and management
- Set up LLM service communication endpoints
- Configure resource allocation for model inference

## Out of Scope

- LLM model fine-tuning
- GPU acceleration setup
- Model performance optimization
- Production LLM scaling

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_podman_compose_configuration.md

## Implementation Details

Create Ollama LLM setup with:

1. **Ollama service configuration** in docker-compose.yml:
```yaml
ollama:
  image: ollama/ollama:latest
  ports:
    - "11434:11434"
  environment:
    OLLAMA_HOST: 0.0.0.0
  volumes:
    - ollama-models:/root/.ollama
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 60s
```

2. **Model initialization script** (init-ollama.sh):
```bash
#!/bin/bash
echo "Waiting for Ollama service to be ready..."
until curl -f http://ollama:11434/api/tags; do
  echo "Waiting for Ollama..."
  sleep 5
done

echo "Pulling required models..."
# Pull a lightweight model suitable for Q&A
curl -X POST http://ollama:11434/api/pull \
  -H "Content-Type: application/json" \
  -d '{"name": "llama2:7b-chat"}'

# Alternative smaller model for development
curl -X POST http://ollama:11434/api/pull \
  -H "Content-Type: application/json" \
  -d '{"name": "mistral:7b-instruct"}'

echo "Models pulled successfully"
```

3. **LLM service wrapper** (if needed for API compatibility):
```yaml
llm-api:
  build: ./infrastructure/llm-api
  depends_on:
    - ollama
  environment:
    OLLAMA_URL: http://ollama:11434
    DEFAULT_MODEL: llama2:7b-chat
  ports:
    - "8081:8080"
```

4. **Environment configuration**:
- OLLAMA_URL=http://ollama:11434
- DEFAULT_LLM_MODEL=llama2:7b-chat
- LLM_TIMEOUT_SECONDS=20
- LLM_MAX_TOKENS=2048
- LLM_TEMPERATURE=0.1

5. **Resource allocation**:
```yaml
deploy:
  resources:
    limits:
      memory: 8G
      cpus: '4.0'
    reservations:
      memory: 4G
      cpus: '2.0'
```

Alternative configuration for external LLM APIs:
```yaml
# For OpenAI API usage instead of local Ollama
environment:
  LLM_PROVIDER: openai
  OPENAI_API_KEY: ${OPENAI_API_KEY}
  OPENAI_MODEL: gpt-3.5-turbo
```

Model management:
- Automatic model downloading on first startup
- Model persistence across container restarts
- Model version management
- Fallback model configuration

## Files / Modules Impacted

- docker-compose.yml (ollama service)
- infrastructure/ollama/init-ollama.sh
- infrastructure/llm-api/Dockerfile (if wrapper needed)
- infrastructure/llm-api/app.py (if wrapper needed)
- .env.example (LLM configuration)

## Acceptance Criteria

Given Ollama container is started
When health check is performed
Then Ollama should be accessible on port 11434

Given model initialization script runs
When required models are pulled
Then models should be available for inference

Given LLM service receives a request
When text generation is requested
Then appropriate response should be generated within timeout

Given container is restarted
When models are checked
Then downloaded models should be preserved

## Testing Requirements

- Test Ollama container startup and health
- Test model downloading and availability
- Test text generation functionality
- Test response time and timeout handling
- Test model persistence across restarts

## Dependencies / Preconditions

- Podman Compose configuration must exist
- Sufficient system resources (RAM/CPU) for LLM inference
- Internet connectivity for model downloading
- Ollama image must be available