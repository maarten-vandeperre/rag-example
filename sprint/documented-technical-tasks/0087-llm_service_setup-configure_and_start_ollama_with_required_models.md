# Configure and start Ollama with required models

## Related User Story

LLM Service Setup

## Objective

Configure and start the Ollama service with the required language models to enable chat functionality and knowledge extraction. Ensure the LLM service is properly integrated with the application and can generate responses for chat queries.

## Scope

- Start Ollama service with proper configuration
- Download and configure the tinyllama model
- Verify LLM service connectivity from the application
- Test model response generation
- Configure model parameters for optimal performance

## Out of Scope

- Training custom models
- Advanced model fine-tuning
- Multi-model configuration
- Model performance optimization

## Clean Architecture Placement

- infrastructure (LLM service configuration)

## Execution Dependencies

None (can run in parallel with other tasks)

## Implementation Details

### Start Ollama Service
- Use podman-compose to start Ollama with the llm profile
- Verify Ollama container is running and healthy
- Check Ollama API is accessible on localhost:11434
- Ensure proper volume mounting for model storage

### Download Required Models
- Download the tinyllama model as configured in application properties
- Verify model is properly installed and available
- Test model loading and response generation
- Check model compatibility with the application

### Configure LLM Integration
- Verify application configuration points to correct Ollama URL
- Test LLM service connectivity from the backend
- Configure appropriate timeout and retry settings
- Ensure proper error handling for LLM service failures

### Test Model Functionality
- Test basic text generation with the model
- Verify model can process chat queries
- Test response quality and coherence
- Check model performance and response times

### Optimize Configuration
- Configure appropriate model parameters (temperature, max tokens)
- Set reasonable timeout values for model responses
- Configure retry logic for transient failures
- Ensure proper resource allocation for model operations

## Files / Modules Impacted

- `docker-compose.dev.yml` (Ollama service configuration)
- `backend/src/main/resources/application-dev.properties` (LLM configuration)
- LLM integration components in the backend
- Chat processing pipeline

## Acceptance Criteria

**Given** Ollama service is started with llm profile
**When** checking the service health
**Then** Ollama should be running and accessible on localhost:11434

**Given** the tinyllama model is downloaded
**When** listing available models in Ollama
**Then** tinyllama should appear in the model list

**Given** the LLM service is configured
**When** the backend tries to connect to Ollama
**Then** the connection should succeed without errors

**Given** a chat query is submitted
**When** the LLM generates a response
**Then** a coherent response should be returned within reasonable time

## Testing Requirements

- Test Ollama service startup and health
- Verify model download and availability
- Test LLM API connectivity from backend
- Test basic text generation functionality
- Verify chat query processing with LLM
- Test error handling for LLM service failures

## Dependencies / Preconditions

- Podman and podman-compose are available
- Docker-compose configuration includes Ollama service
- Sufficient disk space for model storage
- Network connectivity for model download